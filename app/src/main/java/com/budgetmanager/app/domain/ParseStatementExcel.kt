package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.StatementLine
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

sealed interface StatementExcelResult {
    data class Success(val lines: List<StatementLine>, val skippedRowCount: Int) : StatementExcelResult
    data class Failure(val reason: String) : StatementExcelResult
}

/**
 * Reads a bank/credit-card statement spreadsheet into [StatementLine]s - the same best-effort,
 * keyword-matched column layout [ParseStatementCsv] uses (headers vary bank to bank), just fed a
 * cell grid read from a workbook instead of CSV text. Two real formats exist in the wild:
 *
 * - Legacy binary **.xls** (BIFF8 / OLE2, magic bytes `D0 CF 11 E0`) - read with Apache POI's
 *   HSSF reader. This is the format Indian bank/card exports actually use in practice (confirmed
 *   against a real sample), not the newer XLSX.
 * - Modern **.xlsx** (OOXML, a zip file starting `PK`) - read with a small hand-rolled zip+XML
 *   reader (`xl/sharedStrings.xml` + the first `xl/worksheets/sheetN.xml`) rather than
 *   `poi-ooxml`, whose `javax.xml.stream`/xmlbeans dependency chain is not Android-friendly.
 *   Covers the common single-sheet export case; anything fancier (multiple sheets, merged cells,
 *   formulas) is out of scope, same spirit as the CSV reader's own documented trade-offs.
 */
object ParseStatementExcel {

    private val xlsMagic = byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte())
    private val xlsxMagic = byteArrayOf(0x50, 0x4B) // "PK"

    operator fun invoke(bytes: ByteArray): StatementExcelResult {
        val rows = try {
            when {
                bytes.size >= 4 && bytes.copyOfRange(0, 4).contentEquals(xlsMagic) -> readXls(bytes)
                bytes.size >= 2 && bytes.copyOfRange(0, 2).contentEquals(xlsxMagic) -> readXlsx(bytes)
                else -> return StatementExcelResult.Failure(
                    "That doesn't look like an Excel file (.xls or .xlsx)."
                )
            }
        } catch (e: Exception) {
            return StatementExcelResult.Failure("Couldn't read that spreadsheet: ${e.message ?: "unknown error"}.")
        }

        if (rows.isEmpty()) return StatementExcelResult.Failure("That file has no rows to read.")

        val headerIndex = rows.take(ParseStatementCsv.HEADER_SEARCH_WINDOW).indexOfFirst {
            ParseStatementCsv.looksLikeHeaderRow(it.map { c -> c.trim().lowercase(Locale.ENGLISH) })
        }
        if (headerIndex < 0) {
            return StatementExcelResult.Failure(
                "Couldn't find a Date and Amount column in this file's header row."
            )
        }

        val header = rows[headerIndex].map { it.trim().lowercase(Locale.ENGLISH) }
        val columns = ParseStatementCsv.detectColumns(header)
        if (columns.date < 0) return StatementExcelResult.Failure("Couldn't find a Date column in this file.")
        if (columns.debit < 0 && columns.credit < 0 && columns.amount < 0) {
            return StatementExcelResult.Failure("Couldn't find a Debit/Credit or Amount column in this file.")
        }

        val lines = mutableListOf<StatementLine>()
        var skipped = 0
        for (row in rows.drop(headerIndex + 1)) {
            if (row.all { it.isBlank() }) continue
            val line = ParseStatementCsv.parseRow(row, columns)
            if (line != null) lines += line else skipped++
        }

        if (lines.isEmpty()) {
            return StatementExcelResult.Failure("Found a header row, but no readable transaction rows after it.")
        }
        return StatementExcelResult.Success(lines, skipped)
    }

    // ---- legacy binary .xls, via POI HSSF ----

    private val xlsDateFormat = DateTimeFormatter.ofPattern("d-M-yyyy", Locale.ENGLISH)

    private fun readXls(bytes: ByteArray): List<List<String>> {
        HSSFWorkbook(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val lastColumn = (sheet.firstRowNum..sheet.lastRowNum)
                .mapNotNull { sheet.getRow(it)?.lastCellNum?.toInt() }
                .maxOrNull() ?: 0
            return (sheet.firstRowNum..sheet.lastRowNum).map { rowIndex ->
                val row = sheet.getRow(rowIndex)
                (0 until lastColumn).map { cellIndex ->
                    cellToString(row?.getCell(cellIndex))
                }
            }
        }
    }

    private fun cellToString(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) {
                cell.localDateTimeCellValue.toLocalDate().format(xlsDateFormat)
            } else {
                BigDecimal(cell.numericCellValue).setScale(2, RoundingMode.HALF_UP).toPlainString()
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> try {
                cell.stringCellValue
            } catch (_: Exception) {
                try {
                    BigDecimal(cell.numericCellValue).setScale(2, RoundingMode.HALF_UP).toPlainString()
                } catch (_: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }

    // ---- modern .xlsx, hand-rolled zip+XML reader ----

    private fun readXlsx(bytes: ByteArray): List<List<String>> {
        var sharedStringsXml: ByteArray? = null
        val worksheetCandidates = sortedMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == "xl/sharedStrings.xml" -> sharedStringsXml = zip.readBytes()
                    name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) -> worksheetCandidates[name] = zip.readBytes()
                    else -> Unit
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheetXml = worksheetCandidates["xl/worksheets/sheet1.xml"]
            ?: worksheetCandidates.values.firstOrNull()
            ?: throw IllegalStateException("No worksheet found in this .xlsx file.")

        val sharedStrings = sharedStringsXml?.let { parseSharedStrings(it) } ?: emptyList()
        return parseSheet(sheetXml, sharedStrings)
    }

    private fun parseXml(bytes: ByteArray): Document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(bytes))

    private fun parseSharedStrings(xml: ByteArray): List<String> {
        val doc = parseXml(xml)
        val siNodes = doc.getElementsByTagName("si")
        return (0 until siNodes.length).map { i ->
            val si = siNodes.item(i) as Element
            val tNodes = si.getElementsByTagName("t")
            (0 until tNodes.length).joinToString("") { tNodes.item(it).textContent ?: "" }
        }
    }

    private val columnLetterRegex = Regex("^([A-Z]+)")

    private fun columnIndex(cellRef: String): Int {
        val letters = columnLetterRegex.find(cellRef)?.groupValues?.get(1) ?: return 0
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }

    private fun parseSheet(xml: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val doc = parseXml(xml)
        val rowNodes = doc.getElementsByTagName("row")
        return (0 until rowNodes.length).map { r ->
            val rowElement = rowNodes.item(r) as Element
            val cellNodes = rowElement.getElementsByTagName("c")
            val cells = mutableMapOf<Int, String>()
            var maxIndex = -1
            for (c in 0 until cellNodes.length) {
                val cellElement = cellNodes.item(c) as Element
                val ref = cellElement.getAttribute("r")
                val index = if (ref.isNotEmpty()) columnIndex(ref) else c
                maxIndex = maxOf(maxIndex, index)
                cells[index] = resolveCellValue(cellElement, sharedStrings)
            }
            (0..maxIndex).map { i -> cells[i] ?: "" }
        }
    }

    private fun resolveCellValue(cellElement: Element, sharedStrings: List<String>): String {
        val type = cellElement.getAttribute("t")
        if (type == "inlineStr") {
            val isNodes = cellElement.getElementsByTagName("is")
            if (isNodes.length > 0) {
                val tNodes = (isNodes.item(0) as Element).getElementsByTagName("t")
                return (0 until tNodes.length).joinToString("") { tNodes.item(it).textContent ?: "" }
            }
        }
        val vNodes = cellElement.getElementsByTagName("v")
        val raw = if (vNodes.length > 0) vNodes.item(0).textContent ?: "" else ""
        return when (type) {
            "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
            "str" -> raw
            else -> raw // numeric (including date serials, handled by ParseStatementCsv.parseDate's fallback)
        }
    }
}
