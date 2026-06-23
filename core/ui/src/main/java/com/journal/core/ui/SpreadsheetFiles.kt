package com.journal.core.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

data class SpreadsheetDocument(
    val rows: List<List<String>>,
    val fileName: String
)

fun readSpreadsheetDocument(context: Context, uri: Uri): SpreadsheetDocument {
    val fileName = context.displayName(uri)
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("Не удалось открыть файл")
    return SpreadsheetDocument(
        rows = parseSpreadsheetRows(bytes, fileName),
        fileName = fileName
    )
}

fun parseSpreadsheetRows(bytes: ByteArray, fileName: String): List<List<String>> {
    val lowerName = fileName.lowercase()
    return if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xlsm") || bytes.isZipFile()) {
        parseXlsxRows(bytes)
    } else {
        parseCsvRows(bytes.toString(Charsets.UTF_8))
    }
}

fun shareXlsxFile(
    context: Context,
    rows: List<List<String>>,
    fileName: String,
    sheetName: String = "Sheet1",
    chooserTitle: String = "Export"
) {
    shareBytesFile(
        context = context,
        bytes = buildSimpleXlsx(rows, sheetName),
        fileName = fileName,
        mimeType = XLSX_MIME_TYPE,
        chooserTitle = chooserTitle
    )
}

fun buildSimpleXlsx(rows: List<List<String>>, sheetName: String = "Sheet1"): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.writeEntry("[Content_Types].xml", xlsxContentTypes())
        zip.writeEntry("_rels/.rels", xlsxRootRels())
        zip.writeEntry("xl/_rels/workbook.xml.rels", xlsxWorkbookRels())
        zip.writeEntry("xl/workbook.xml", xlsxWorkbook(sheetName))
        zip.writeEntry("xl/styles.xml", xlsxStyles())
        zip.writeEntry("xl/worksheets/sheet1.xml", xlsxSheet(rows))
    }
    return output.toByteArray()
}

private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index).orEmpty().ifBlank { "import.xlsx" }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "import.xlsx"
}

private fun ByteArray.isZipFile(): Boolean =
    size >= 4 && this[0] == 0x50.toByte() && this[1] == 0x4B.toByte()

private fun parseCsvRows(text: String): List<List<String>> {
    val delimiter = detectDelimiter(text)
    val rows = mutableListOf<MutableList<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var inQuotes = false
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char == '"' && inQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                cell.append('"')
                index++
            }
            char == '"' -> inQuotes = !inQuotes
            char == delimiter && !inQuotes -> {
                row.add(cell.toString().trim())
                cell.clear()
            }
            (char == '\n' || char == '\r') && !inQuotes -> {
                if (char == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                row.add(cell.toString().trim())
                if (row.any { it.isNotBlank() }) rows.add(row)
                row = mutableListOf()
                cell.clear()
            }
            else -> cell.append(char)
        }
        index++
    }
    row.add(cell.toString().trim())
    if (row.any { it.isNotBlank() }) rows.add(row)
    return rows
}

private fun detectDelimiter(text: String): Char {
    val sample = text.lineSequence().take(5).joinToString("\n")
    return if (sample.count { it == ';' } > sample.count { it == ',' }) ';' else ','
}

private fun parseXlsxRows(bytes: ByteArray): List<List<String>> {
    val entries = mutableMapOf<String, ByteArray>()
    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        generateSequence { zip.nextEntry }.forEach { entry ->
            if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
            zip.closeEntry()
        }
    }
    val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
    val sheet = entries.keys
        .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
        .sorted()
        .firstNotNullOfOrNull { entries[it] }
        ?: return emptyList()
    return parseWorksheetRows(sheet, sharedStrings)
}

private fun parseSharedStrings(bytes: ByteArray): List<String> {
    val document = xmlDocument(bytes)
    val items = document.getElementsByTagName("si")
    return List(items.length) { index ->
        val item = items.item(index) as Element
        item.getElementsByTagName("t").asElements().joinToString("") { it.textContent.orEmpty() }
    }
}

private fun parseWorksheetRows(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
    val document = xmlDocument(bytes)
    val rowNodes = document.getElementsByTagName("row")
    val rows = mutableListOf<List<String>>()
    rowNodes.asElements().forEach { rowElement ->
        val values = mutableListOf<String>()
        rowElement.getElementsByTagName("c").asElements().forEach { cell ->
            val columnIndex = columnIndexFromReference(cell.getAttribute("r")).coerceAtLeast(values.size)
            while (values.size < columnIndex) values.add("")
            values.add(cellText(cell, sharedStrings).trim())
        }
        if (values.any { it.isNotBlank() }) rows.add(values)
    }
    return rows
}

private fun cellText(cell: Element, sharedStrings: List<String>): String {
    val type = cell.getAttribute("t")
    if (type == "inlineStr") {
        return cell.getElementsByTagName("t").asElements().joinToString("") { it.textContent.orEmpty() }
    }
    val raw = cell.getElementsByTagName("v").item(0)?.textContent.orEmpty()
    return if (type == "s") sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw
}

private fun xmlDocument(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = false
}.newDocumentBuilder().parse(ByteArrayInputStream(bytes))

private fun org.w3c.dom.NodeList.asElements(): List<Element> =
    List(length) { index -> item(index) }.filterIsInstance<Element>()

private fun columnIndexFromReference(reference: String): Int {
    val letters = reference.takeWhile { it.isLetter() }
    if (letters.isBlank()) return 0
    return letters.fold(0) { acc, char -> acc * 26 + (char.uppercaseChar() - 'A' + 1) } - 1
}

private fun ZipOutputStream.writeEntry(name: String, content: String) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

private fun xlsxSheet(rows: List<List<String>>): String = buildString {
    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
    append("<sheetFormatPr defaultRowHeight=\"18\"/>")
    append("<sheetData>")
    rows.forEachIndexed { rowIndex, row ->
        val excelRow = rowIndex + 1
        append("<row r=\"").append(excelRow).append("\">")
        row.forEachIndexed { columnIndex, value ->
            val cell = "${columnName(columnIndex + 1)}$excelRow"
            val style = if (rowIndex == 0) 1 else 0
            append("<c r=\"").append(cell).append("\" t=\"inlineStr\" s=\"").append(style).append("\"><is><t>")
            append(value.xmlEscape())
            append("</t></is></c>")
        }
        append("</row>")
    }
    append("</sheetData>")
    append("<pageMargins left=\"0.7\" right=\"0.7\" top=\"0.75\" bottom=\"0.75\" header=\"0.3\" footer=\"0.3\"/>")
    append("</worksheet>")
}

private fun xlsxContentTypes(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    </Types>
""".trimIndent()

private fun xlsxRootRels(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
    </Relationships>
""".trimIndent()

private fun xlsxWorkbookRels(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    </Relationships>
""".trimIndent()

private fun xlsxWorkbook(sheetName: String): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets><sheet name="${sheetName.take(31).xmlEscape()}" sheetId="1" r:id="rId1"/></sheets>
    </workbook>
""".trimIndent()

private fun xlsxStyles(): String = """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
        <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
        <borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border><border><left style="thin"/><right style="thin"/><top style="thin"/><bottom style="thin"/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1"/></cellXfs>
        <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
    </styleSheet>
""".trimIndent()

private fun columnName(index: Int): String {
    var value = index
    val result = StringBuilder()
    while (value > 0) {
        value--
        result.insert(0, ('A'.code + value % 26).toChar())
        value /= 26
    }
    return result.toString()
}

private fun String.xmlEscape(): String = buildString {
    this@xmlEscape.forEach { char ->
        append(
            when (char) {
                '<' -> "&lt;"
                '>' -> "&gt;"
                '&' -> "&amp;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> char.toString()
            }
        )
    }
}
