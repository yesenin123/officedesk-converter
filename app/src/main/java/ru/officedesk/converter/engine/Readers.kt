package ru.officedesk.converter.engine

import android.content.Context
import android.net.Uri
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.data.MutableDataSet
import nl.siegmann.epublib.epub.EpubReader
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import ru.officedesk.converter.DocFormat
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object Readers {

    fun read(context: Context, uri: Uri, from: DocFormat, progress: (Int) -> Unit = {}): IRDoc {
        val cr = context.contentResolver
        return cr.openInputStream(uri)?.use { input ->
            when (from) {
                DocFormat.DOCX -> readDocx(input)
                DocFormat.DOC  -> readDoc(input)
                DocFormat.RTF  -> readRtf(input)
                DocFormat.ODT  -> readOdt(input)
                DocFormat.TXT  -> readTxt(input)
                DocFormat.MD   -> readMd(input)
                DocFormat.HTML -> readHtml(input)
                DocFormat.EPUB -> readEpub(input)
                DocFormat.PDF  -> readPdf(input)
                DocFormat.XLSX -> readXlsx(input)
                DocFormat.XLS  -> readXls(input)
            }
        } ?: throw UnsupportedFormatException("Не удалось открыть файл")
    }

    // ── DOCX ──────────────────────────────────────────────
    private fun readDocx(input: InputStream): IRDoc {
        val doc = XWPFDocument(input)
        val blocks = mutableListOf<IRBlock>()
        doc.bodyElements.forEach { el ->
            when (el) {
                is org.apache.poi.xwpf.usermodel.XWPFParagraph -> {
                    val style = el.style?.lowercase() ?: ""
                    val text = el.text ?: ""
                    if (text.isBlank()) return@forEach
                    val runs = el.runs.map { r ->
                        IRRun(
                            (try { r.text() } catch (_: Throwable) { null }) ?: "",
                            r.isBold,
                            r.isItalic,
                            try { r.underline.name != "NONE" } catch (_: Throwable) { false }
                        )
                    }.ifEmpty { listOf(IRRun(text)) }

                    val level = Regex("heading\\s*([1-6])").find(style)?.groupValues?.get(1)?.toIntOrNull()
                    val numLvl = try { el.numIlvl?.toInt() } catch (_: Throwable) { null }
                    if (level != null) {
                        blocks.add(IRBlock.Heading(level, runs.joinToString("") { it.text }))
                    } else if (numLvl != null) {
                        blocks.add(IRBlock.ListItem(ordered = true, level = numLvl, runs = runs))
                    } else {
                        blocks.add(IRBlock.Paragraph(runs))
                    }
                }
                is XWPFTable -> {
                    val rows = el.rows.map { row -> row.tableCells.map { it.text } }
                    blocks.add(IRBlock.Table(rows))
                }
            }
        }
        return IRDoc(title = doc.properties?.coreProperties?.title, blocks = blocks)
    }

    // ── DOC (старый Word) ────────────────────────────────
    private fun readDoc(input: InputStream): IRDoc {
        val doc = HWPFDocument(input)
        val text = WordExtractor(doc).text
        return plainTextToIR(text)
    }

    // ── RTF ──────────────────────────────────────────────
    private fun readRtf(input: InputStream): IRDoc {
        // Простой парсер: убираем управляющие последовательности RTF, оставляем текст.
        val raw = input.readBytes().toString(Charsets.ISO_8859_1)
        val cleaned = stripRtf(raw)
        return plainTextToIR(cleaned)
    }

    private fun stripRtf(rtf: String): String {
        // 1. Группы заголовков и таблицы шрифтов/цветов — отбрасываем их содержимое.
        val sb = StringBuilder()
        var i = 0
        var depth = 0
        var skip = 0
        while (i < rtf.length) {
            val c = rtf[i]
            when {
                c == '\\' && i + 1 < rtf.length -> {
                    // управляющее слово
                    val end = rtf.indexOfAny(charArrayOf(' ', '\\', '{', '}', '\r', '\n', ';'), i + 1).let { if (it < 0) rtf.length else it }
                    val word = rtf.substring(i + 1, end)
                    when {
                        word.startsWith("u") && word.length > 1 && word[1].isDigit() || (word.startsWith("u-") && word.length > 2) -> {
                            // \uNNNN — Unicode
                            val numStr = word.drop(1)
                            val n = numStr.takeWhile { it == '-' || it.isDigit() }.toIntOrNull()
                            if (n != null) sb.append(n.toChar())
                        }
                        word == "par" || word == "line" -> sb.append('\n')
                        word == "tab" -> sb.append('\t')
                        word.startsWith("'") && word.length >= 3 -> {
                            val hex = word.substring(1, 3)
                            try { sb.append(hex.toInt(16).toChar()) } catch (_: Exception) {}
                        }
                        word == "fonttbl" || word == "colortbl" || word == "stylesheet" || word == "info" || word == "pict" -> {
                            // пропускаем содержимое до конца группы
                            skip++
                        }
                    }
                    i = end
                    if (i < rtf.length && rtf[i] == ' ') i++
                }
                c == '{' -> { depth++; if (skip > 0) skip++; i++ }
                c == '}' -> { depth--; if (skip > 0) { skip--; if (skip == 0) {} }; i++ }
                else -> {
                    if (skip == 0 && c.code >= 32) sb.append(c)
                    if (c == '\n') sb.append('\n')
                    i++
                }
            }
        }
        return sb.toString().replace(Regex("\\n{3,}"), "\n\n").trim()
    }

    // ── ODT (zip с content.xml) ───────────────────────────
    private fun readOdt(input: InputStream): IRDoc {
        val zis = ZipInputStream(input)
        var contentXml: String? = null
        while (true) {
            val e = zis.nextEntry ?: break
            if (e.name == "content.xml") {
                contentXml = zis.readBytes().toString(Charsets.UTF_8)
                break
            }
        }
        if (contentXml == null) throw UnsupportedFormatException("ODT повреждён")
        // ODT использует пространство имён text:p, text:h
        val blocks = mutableListOf<IRBlock>()
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val xml = factory.newDocumentBuilder().parse(contentXml.byteInputStream())
        val all = xml.getElementsByTagName("*")
        for (i in 0 until all.length) {
            val n = all.item(i)
            val ln = n.localName ?: continue
            val text = n.textContent?.trim() ?: continue
            if (text.isEmpty()) continue
            when (ln) {
                "h" -> {
                    val level = (n.attributes?.getNamedItemNS("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "outline-level")
                        ?.nodeValue?.toIntOrNull() ?: 1).coerceIn(1, 6)
                    blocks.add(IRBlock.Heading(level, text))
                }
                "p" -> blocks.add(IRBlock.Paragraph(listOf(IRRun(text))))
            }
        }
        return IRDoc(blocks = blocks)
    }

    // ── TXT ──────────────────────────────────────────────
    private fun readTxt(input: InputStream): IRDoc =
        plainTextToIR(BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText())

    // ── MD ──────────────────────────────────────────────
    private fun readMd(input: InputStream): IRDoc {
        val text = input.bufferedReader(Charsets.UTF_8).readText()
        // Markdown -> HTML -> через тот же парсер, что и HTML
        val parser = Parser.builder(MutableDataSet()).build()
        val doc: Document = parser.parse(text)
        val html = com.vladsch.flexmark.html.HtmlRenderer.builder().build().render(doc)
        return readHtmlString(html)
    }

    // ── HTML ─────────────────────────────────────────────
    private fun readHtml(input: InputStream): IRDoc {
        val html = input.bufferedReader(Charsets.UTF_8).readText()
        return readHtmlString(html)
    }

    private fun readHtmlString(html: String): IRDoc {
        // Минималистичный HTML-парсер: преобразуем в Markdown, потом извлекаем структуру.
        val md = FlexmarkHtmlConverter.builder().build().convert(html)
        return parseMarkdownStructure(md)
    }

    private fun parseMarkdownStructure(md: String): IRDoc {
        val blocks = mutableListOf<IRBlock>()
        val lines = md.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> { i++ }
                line.startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    val text = line.drop(level).trim().trimStart('#').trim()
                    blocks.add(IRBlock.Heading(level, text))
                    i++
                }
                line.startsWith("> ") -> {
                    blocks.add(IRBlock.Quote(listOf(IRRun(line.removePrefix("> ")))))
                    i++
                }
                line.startsWith("```") -> {
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        sb.appendLine(lines[i]); i++
                    }
                    if (i < lines.size) i++
                    blocks.add(IRBlock.CodeBlock(sb.toString().trimEnd()))
                }
                Regex("^[-*+] ").containsMatchIn(line) -> {
                    blocks.add(IRBlock.ListItem(false, 0, parseInline(line.substring(2))))
                    i++
                }
                Regex("^\\d+\\. ").containsMatchIn(line) -> {
                    blocks.add(IRBlock.ListItem(true, 0, parseInline(line.substringAfter(". "))))
                    i++
                }
                line.matches(Regex("^[-*_]{3,}\\s*$")) -> { blocks.add(IRBlock.HRule); i++ }
                else -> {
                    val sb = StringBuilder(line)
                    i++
                    while (i < lines.size && lines[i].isNotBlank() && !lines[i].startsWith("#") && !lines[i].startsWith("```")) {
                        sb.append(' ').append(lines[i].trim()); i++
                    }
                    blocks.add(IRBlock.Paragraph(parseInline(sb.toString())))
                }
            }
        }
        return IRDoc(blocks = blocks)
    }

    private fun parseInline(s: String): List<IRRun> {
        // очень простой инлайн-парсер: **жирный** *курсив*
        val runs = mutableListOf<IRRun>()
        val regex = Regex("(\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|_([^_]+)_)")
        var last = 0
        for (m in regex.findAll(s)) {
            if (m.range.first > last) runs.add(IRRun(s.substring(last, m.range.first)))
            when {
                m.value.startsWith("**") -> runs.add(IRRun(m.groupValues[2], bold = true))
                m.value.startsWith("*")  -> runs.add(IRRun(m.groupValues[3], italic = true))
                m.value.startsWith("_")  -> runs.add(IRRun(m.groupValues[4], italic = true))
            }
            last = m.range.last + 1
        }
        if (last < s.length) runs.add(IRRun(s.substring(last)))
        if (runs.isEmpty() && s.isNotEmpty()) runs.add(IRRun(s))
        return runs
    }

    // ── EPUB ─────────────────────────────────────────────
    private fun readEpub(input: InputStream): IRDoc {
        val book = EpubReader().readEpub(input)
        val blocks = mutableListOf<IRBlock>()
        book.contents.forEach { res ->
            try {
                val html = res.inputStream.bufferedReader().readText()
                val sub = readHtmlString(html)
                blocks.addAll(sub.blocks)
                blocks.add(IRBlock.PageBreak)
            } catch (_: Throwable) { /* пропустить главу */ }
        }
        return IRDoc(title = book.title, blocks = blocks)
    }

    // ── PDF ──────────────────────────────────────────────
    private fun readPdf(input: InputStream): IRDoc {
        // iText умеет читать; для текста используем PdfTextExtractor
        val reader = com.itextpdf.kernel.pdf.PdfReader(input)
        val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(reader)
        val sb = StringBuilder()
        for (p in 1..pdfDoc.numberOfPages) {
            val page = pdfDoc.getPage(p)
            val text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page)
            sb.appendLine(text); sb.appendLine()
        }
        pdfDoc.close()
        return plainTextToIR(sb.toString())
    }

    // ── XLSX / XLS ───────────────────────────────────────
    private fun readXlsx(input: InputStream): IRDoc = readWorkbook(input, ooxml = true)
    private fun readXls(input: InputStream): IRDoc = readWorkbook(input, ooxml = false)

    private fun readWorkbook(input: InputStream, ooxml: Boolean): IRDoc {
        val wb = if (ooxml) org.apache.poi.xssf.usermodel.XSSFWorkbook(input) else HSSFWorkbook(input)
        val blocks = mutableListOf<IRBlock>()
        for (sheetIdx in 0 until wb.numberOfSheets) {
            val sheet = wb.getSheetAt(sheetIdx)
            blocks.add(IRBlock.Heading(2, sheet.sheetName))
            val rows = mutableListOf<List<String>>()
            for (row in sheet) {
                val cells = mutableListOf<String>()
                val last = row.lastCellNum.toInt().coerceAtLeast(0)
                for (c in 0 until last) {
                    val cell = row.getCell(c)
                    cells.add(cell?.toString() ?: "")
                }
                rows.add(cells)
            }
            if (rows.isNotEmpty()) blocks.add(IRBlock.Table(rows))
            blocks.add(IRBlock.PageBreak)
        }
        wb.close()
        return IRDoc(blocks = blocks)
    }

    // ── helpers ──────────────────────────────────────────
    private fun plainTextToIR(text: String): IRDoc {
        val blocks = text.split(Regex("\\r?\\n\\s*\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { IRBlock.Paragraph(listOf(IRRun(it))) as IRBlock }
        return IRDoc(blocks = blocks)
    }
}
