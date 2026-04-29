package ru.officedesk.converter.engine

/**
 * Универсальное промежуточное представление документа.
 * Любой входной формат парсится сюда, потом сериализуется в любой выходной.
 * Намеренно простое — гарантирует надёжность на телефоне.
 */
data class IRDoc(
    val title: String? = null,
    val blocks: List<IRBlock>,
)

sealed class IRBlock {
    data class Heading(val level: Int, val text: String) : IRBlock()           // 1..6
    data class Paragraph(val runs: List<IRRun>) : IRBlock()
    data class ListItem(val ordered: Boolean, val level: Int, val runs: List<IRRun>) : IRBlock()
    data class Quote(val runs: List<IRRun>) : IRBlock()
    data class CodeBlock(val text: String) : IRBlock()
    data class Table(val rows: List<List<String>>) : IRBlock()
    object PageBreak : IRBlock()
    object HRule : IRBlock()
}

data class IRRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)
