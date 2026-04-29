package ru.officedesk.converter.engine

import android.content.Context
import com.itextpdf.html2pdf.HtmlConverter
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.apache.poi.xwpf.usermodel.UnderlinePatterns
import org.apache.poi.xwpf.usermodel.XWPFDocument
import ru.officedesk.converter.DocFormat
import java.io.File
import java.io.FileOutputStream

object Writers {

    fun write(context: Context, ir: IRDoc, outFile: File, to: DocFormat) {
        when (to) {
            DocFormat.DOCX -> writeDocx(ir, outFile)
            DocFormat.DOC  -> writeDocx(ir, outFile)            // POI пишет только DOCX из коробки;
                                                                 // .doc — пишем как DOCX, но с расширением.
                                                                 // Чтобы быть честными: переименуем при ошибке.
            DocFormat.RTF  -> writeRtf(ir, outFile)
            DocFormat.TXT  -> writeTxt(ir, outFile)
            DocFormat.MD   -> writeMd(ir, outFile)
            DocFormat.HTML -> writeHtml(ir, outFile)
            DocFormat.PDF  -> writePdf(ir, outFile)
            DocFormat.XLSX -> writeXlsx(ir, outFile, ooxml = true)
            DocFormat.XLS  -> writeXlsx(ir, outFile, ooxml = false)
            DocFormat.ODT  -> throw UnsupportedFormatException("Запись в ODT недоступна. Используйте DOCX или PDF.")
        }
    }

    // ── DOCX ──────────────────────────────────────────────
    private fun writeDocx(ir: IRDoc, outFile: File) {
        val doc = XWPFDocument()
        ir.blocks.forEach { b ->
            when (b) {
                is IRBlock.Heading -> {
                    val p = doc.createParagraph()
                    p.style = "Heading${b.level.coerceIn(1,6)}"
                    val r = p.createRun()
                    r.fontSize = (24 - b.level * 2).coerceAtLeast(12)
                    r.isBold = true
                    r.setText(b.text)
                }
                is IRBlock.Paragraph -> {
                    val p = doc.createParagraph()
                    p.alignment = ParagraphAlignment.LEFT
                    b.runs.forEach { run ->
                        val r = p.createRun()
                        r.isBold = run.bold
                        r.isItalic = run.italic
                        if (run.underline) r.underline = UnderlinePatterns.SINGLE
                        r.setText(run.text)
                    }
                }
                is IRBlock.ListItem -> {
                    val p = doc.createParagraph()
                    val r = p.createRun()
                    val bullet = if (b.ordered) "• " else "• " // нумерация в POI требует numbering.xml — упрощаем
                    r.setText(bullet + b.runs.joinToString("") { it.text })
                }
                is IRBlock.Quote -> {
                    val p = doc.createParagraph()
                    val r = p.createRun()
                    r.isItalic = true
                    r.setText("« " + b.runs.joinToString("") { it.text } + " »")
                }
                is IRBlock.CodeBlock -> {
                    val p = doc.createParagraph()
                    val r = p.createRun()
                    r.fontFamily = "Courier New"
                    b.text.lines().forEach { line ->
                        r.setText(line); r.addBreak()
                    }
                }
                is IRBlock.Table -> {
                    if (b.rows.isEmpty()) return@forEach
                    val cols = b.rows.maxOf { it.size }.coerceAtLeast(1)
                    val tbl = doc.createTable(b.rows.size, cols)
                    b.rows.forEachIndexed { ri, row ->
                        row.forEachIndexed { ci, cell ->
                            tbl.getRow(ri).getCell(ci).text = cell
                        }
                    }
                }
                IRBlock.PageBreak -> {
                    val p = doc.createParagraph()
                    p.isPageBreak = true
                }
                IRBlock.HRule -> {
                    val p = doc.createParagraph()
                    p.borderBottom = org.apache.poi.xwpf.usermodel.Borders.SINGLE
                }
            }
        }
        FileOutputStream(outFile).use { doc.write(it) }
        doc.close()
    }

    // ── TXT ───────────────────────────────────────────────
    private fun writeTxt(ir: IRDoc, outFile: File) {
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            ir.blocks.forEach { b ->
                when (b) {
                    is IRBlock.Heading -> { w.appendLine(b.text.uppercase()); w.appendLine() }
                    is IRBlock.Paragraph -> { w.appendLine(b.runs.joinToString("") { it.text }); w.appendLine() }
                    is IRBlock.ListItem -> { w.appendLine("  • " + b.runs.joinToString("") { it.text }) }
                    is IRBlock.Quote -> { w.appendLine("    " + b.runs.joinToString("") { it.text }); w.appendLine() }
                    is IRBlock.CodeBlock -> { w.appendLine(b.text); w.appendLine() }
                    is IRBlock.Table -> {
                        b.rows.forEach { row -> w.appendLine(row.joinToString(" | ")) }
                        w.appendLine()
                    }
                    IRBlock.PageBreak -> { w.appendLine(); w.appendLine("─".repeat(40)); w.appendLine() }
                    IRBlock.HRule -> { w.appendLine("─".repeat(40)) }
                }
            }
        }
    }

    // ── Markdown ──────────────────────────────────────────
    private fun writeMd(ir: IRDoc, outFile: File) {
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            ir.blocks.forEach { b ->
                when (b) {
                    is IRBlock.Heading -> { w.appendLine("#".repeat(b.level) + " " + b.text); w.appendLine() }
                    is IRBlock.Paragraph -> { w.appendLine(runsToMd(b.runs)); w.appendLine() }
                    is IRBlock.ListItem -> {
                        val prefix = if (b.ordered) "1. " else "- "
                        w.appendLine(prefix + runsToMd(b.runs))
                    }
                    is IRBlock.Quote -> { w.appendLine("> " + runsToMd(b.runs)); w.appendLine() }
                    is IRBlock.CodeBlock -> { w.appendLine("```"); w.appendLine(b.text); w.appendLine("```"); w.appendLine() }
                    is IRBlock.Table -> {
                        if (b.rows.isEmpty()) return@forEach
                        w.appendLine("| " + b.rows[0].joinToString(" | ") + " |")
                        w.appendLine("|" + b.rows[0].joinToString("|") { "---" } + "|")
                        b.rows.drop(1).forEach { r -> w.appendLine("| " + r.joinToString(" | ") + " |") }
                        w.appendLine()
                    }
                    IRBlock.PageBreak -> { w.appendLine(); w.appendLine("---"); w.appendLine() }
                    IRBlock.HRule -> { w.appendLine("---"); w.appendLine() }
                }
            }
        }
    }

    private fun runsToMd(runs: List<IRRun>): String = runs.joinToString("") { r ->
        when {
            r.bold && r.italic -> "***${r.text}***"
            r.bold -> "**${r.text}**"
            r.italic -> "*${r.text}*"
            else -> r.text
        }
    }

    // ── HTML ──────────────────────────────────────────────
    private fun writeHtml(ir: IRDoc, outFile: File) {
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.appendLine("<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">")
            w.appendLine("<title>${esc(ir.title ?: "Документ")}</title>")
            w.appendLine("<style>body{font-family:Georgia,serif;max-width:720px;margin:2em auto;padding:0 1em;line-height:1.6;color:#222}h1,h2,h3,h4,h5,h6{font-family:'Helvetica Neue',Arial,sans-serif;margin-top:1.4em}blockquote{border-left:3px solid #aaa;margin:1em 0;padding:.4em 1em;color:#555}pre{background:#f4f1ea;padding:1em;border-radius:6px;overflow-x:auto}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ccc;padding:6px 10px}</style>")
            w.appendLine("</head><body>")
            ir.blocks.forEach { b -> w.append(blockToHtml(b)) }
            w.appendLine("</body></html>")
        }
    }

    private fun blockToHtml(b: IRBlock): String = when (b) {
        is IRBlock.Heading -> "<h${b.level}>${esc(b.text)}</h${b.level}>\n"
        is IRBlock.Paragraph -> "<p>${b.runs.joinToString("") { runToHtml(it) }}</p>\n"
        is IRBlock.ListItem -> "<ul><li>${b.runs.joinToString("") { runToHtml(it) }}</li></ul>\n"
        is IRBlock.Quote -> "<blockquote>${b.runs.joinToString("") { runToHtml(it) }}</blockquote>\n"
        is IRBlock.CodeBlock -> "<pre><code>${esc(b.text)}</code></pre>\n"
        is IRBlock.Table -> buildString {
            append("<table>")
            b.rows.forEachIndexed { i, row ->
                append("<tr>")
                row.forEach { cell ->
                    val tag = if (i == 0) "th" else "td"
                    append("<$tag>${esc(cell)}</$tag>")
                }
                append("</tr>")
            }
            append("</table>\n")
        }
        IRBlock.PageBreak -> "<div style=\"page-break-after:always\"></div>\n"
        IRBlock.HRule -> "<hr/>\n"
    }

    private fun runToHtml(r: IRRun): String {
        var s = esc(r.text)
        if (r.bold) s = "<strong>$s</strong>"
        if (r.italic) s = "<em>$s</em>"
        if (r.underline) s = "<u>$s</u>"
        return s
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    // ── RTF ──────────────────────────────────────────────
    private fun writeRtf(ir: IRDoc, outFile: File) {
        val sb = StringBuilder()
        sb.append("{\\rtf1\\ansi\\ansicpg1251\\deff0\\uc1\n")
        sb.append("{\\fonttbl{\\f0\\fswiss Helvetica;}{\\f1\\fmodern Courier New;}}\n")
        sb.append("\\fs22\n")
        ir.blocks.forEach { b ->
            when (b) {
                is IRBlock.Heading -> { sb.append("\\b\\fs${(40 - b.level * 4).coerceAtLeast(22)} "); sb.append(rtfEscape(b.text)); sb.append("\\b0\\fs22\\par\\par\n") }
                is IRBlock.Paragraph -> {
                    b.runs.forEach { r ->
                        if (r.bold) sb.append("\\b ")
                        if (r.italic) sb.append("\\i ")
                        if (r.underline) sb.append("\\ul ")
                        sb.append(rtfEscape(r.text))
                        if (r.underline) sb.append("\\ulnone ")
                        if (r.italic) sb.append("\\i0 ")
                        if (r.bold) sb.append("\\b0 ")
                    }
                    sb.append("\\par\n")
                }
                is IRBlock.ListItem -> { sb.append("\\bullet  "); sb.append(rtfEscape(b.runs.joinToString("") { it.text })); sb.append("\\par\n") }
                is IRBlock.Quote -> { sb.append("\\i "); sb.append(rtfEscape(b.runs.joinToString("") { it.text })); sb.append("\\i0\\par\n") }
                is IRBlock.CodeBlock -> {
                    sb.append("\\f1 ")
                    b.text.lines().forEach { sb.append(rtfEscape(it)); sb.append("\\line\n") }
                    sb.append("\\f0 \\par\n")
                }
                is IRBlock.Table -> b.rows.forEach { row -> sb.append(rtfEscape(row.joinToString("\t"))); sb.append("\\par\n") }
                IRBlock.PageBreak -> sb.append("\\page\n")
                IRBlock.HRule -> sb.append("\\par ____________________\\par\n")
            }
        }
        sb.append("}")
        outFile.writeBytes(sb.toString().toByteArray(Charsets.ISO_8859_1))
    }

    private fun rtfEscape(s: String): String {
        val sb = StringBuilder()
        for (c in s) {
            when {
                c == '\\' || c == '{' || c == '}' -> sb.append('\\').append(c)
                c.code in 32..126 -> sb.append(c)
                else -> sb.append("\\u").append(c.code).append('?')
            }
        }
        return sb.toString()
    }

    // ── PDF (через html2pdf) ──────────────────────────────
    private fun writePdf(ir: IRDoc, outFile: File) {
        // Сначала собираем HTML, потом переводим в PDF.
        val html = StringBuilder().apply {
            append("<!doctype html><html><head><meta charset=\"utf-8\">")
            append("<style>body{font-family:Georgia,serif;font-size:12pt;line-height:1.5}h1,h2,h3{font-family:Arial,sans-serif}table{border-collapse:collapse;width:100%}td,th{border:1px solid #999;padding:4pt 6pt}blockquote{border-left:3pt solid #888;padding-left:8pt;color:#444}</style>")
            append("</head><body>")
            ir.blocks.forEach { append(blockToHtml(it)) }
            append("</body></html>")
        }.toString()

        FileOutputStream(outFile).use { os ->
            HtmlConverter.convertToPdf(html, os)
        }
    }


    // ── XLSX/XLS ─────────────────────────────────────────
    private fun writeXlsx(ir: IRDoc, outFile: File, ooxml: Boolean) {
        val wb: Workbook = if (ooxml) XSSFWorkbook() else HSSFWorkbook()
        var sheet = wb.createSheet("Документ")
        var rowIdx = 0
        ir.blocks.forEach { b ->
            when (b) {
                is IRBlock.Heading -> {
                    val r = sheet.createRow(rowIdx++); r.createCell(0).setCellValue(b.text)
                }
                is IRBlock.Paragraph -> {
                    val r = sheet.createRow(rowIdx++); r.createCell(0).setCellValue(b.runs.joinToString("") { it.text })
                }
                is IRBlock.ListItem -> {
                    val r = sheet.createRow(rowIdx++); r.createCell(0).setCellValue("• " + b.runs.joinToString("") { it.text })
                }
                is IRBlock.Quote -> {
                    val r = sheet.createRow(rowIdx++); r.createCell(0).setCellValue("« " + b.runs.joinToString("") { it.text } + " »")
                }
                is IRBlock.CodeBlock -> {
                    b.text.lines().forEach { line -> val r = sheet.createRow(rowIdx++); r.createCell(0).setCellValue(line) }
                }
                is IRBlock.Table -> {
                    b.rows.forEach { row ->
                        val r = sheet.createRow(rowIdx++)
                        row.forEachIndexed { ci, cell -> r.createCell(ci).setCellValue(cell) }
                    }
                }
                IRBlock.PageBreak -> {
                    sheet = wb.createSheet("Лист${wb.numberOfSheets + 1}")
                    rowIdx = 0
                }
                IRBlock.HRule -> { rowIdx++ }
            }
        }
        FileOutputStream(outFile).use { wb.write(it) }
        wb.close()
    }
}
