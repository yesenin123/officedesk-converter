package ru.officedesk.converter

/**
 * Форматы конвертера.
 * Только широко распространённые форматы (требование ТЗ + RuStore).
 */
enum class DocFormat(
    val ext: String,
    val displayName: String,
    val mime: String,
    val canRead: Boolean,
    val canWrite: Boolean,
) {
    DOCX("docx", "Word Document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", true, true),
    DOC ("doc",  "Word 97–2003", "application/msword", true, true),
    RTF ("rtf",  "Rich Text",    "application/rtf", true, true),
    ODT ("odt",  "OpenDocument", "application/vnd.oasis.opendocument.text", true, false),
    TXT ("txt",  "Plain Text",   "text/plain", true, true),
    PDF ("pdf",  "PDF",          "application/pdf", true, true),
    HTML("html", "Web Page",     "text/html", true, true),
    EPUB("epub", "eBook",        "application/epub+zip", true, true),
    MD  ("md",   "Markdown",     "text/markdown", true, true),
    XLSX("xlsx", "Excel Workbook","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true, true),
    XLS ("xls",  "Excel 97–2003","application/vnd.ms-excel", true, true);

    companion object {
        fun fromExt(name: String): DocFormat? {
            val ext = name.substringAfterLast('.', "").lowercase()
            return values().firstOrNull { it.ext == ext } ?: when (ext) {
                "markdown" -> MD
                "htm" -> HTML
                "docm" -> DOCX
                else -> null
            }
        }
        val readable get() = values().filter { it.canRead }
        val writable get() = values().filter { it.canWrite }
    }
}
