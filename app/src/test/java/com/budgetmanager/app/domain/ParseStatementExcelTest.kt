package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ParseStatementExcelTest {

    // ---- legacy binary .xls, via a real POI-written workbook ----

    @Test
    fun `legacy xls with a real date cell and debit-credit columns is read correctly`() {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        val dateStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.creationHelper.createDataFormat().getFormat("d-m-yyyy")
        }

        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Date")
        header.createCell(1).setCellValue("Narration")
        header.createCell(2).setCellValue("Debit")
        header.createCell(3).setCellValue("Credit")

        val row1 = sheet.createRow(1)
        row1.createCell(0).apply { setCellValue(LocalDateTime.of(2026, 9, 5, 0, 0)); cellStyle = dateStyle }
        row1.createCell(1).setCellValue("INVESCO SIP")
        row1.createCell(2).setCellValue(3000.0)

        val row2 = sheet.createRow(2)
        row2.createCell(0).apply { setCellValue(LocalDateTime.of(2026, 9, 20, 0, 0)); cellStyle = dateStyle }
        row2.createCell(1).setCellValue("Salary")
        row2.createCell(3).setCellValue(45000.0)

        val bytes = ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }

        val result = ParseStatementExcel(bytes) as StatementExcelResult.Success
        assertEquals(2, result.lines.size)
        assertEquals(LocalDate.of(2026, 9, 5), result.lines[0].date)
        assertEquals("INVESCO SIP", result.lines[0].description)
        assertEquals(Money.ofRupees(3000), result.lines[0].amount)
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals(LocalDate.of(2026, 9, 20), result.lines[1].date)
        assertEquals(Money.ofRupees(45000), result.lines[1].amount)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun `an xls with only unrelated columns fails clearly`() {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Sheet1")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Name")
        header.createCell(1).setCellValue("Phone")

        val bytes = ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }

        assertTrue(ParseStatementExcel(bytes) is StatementExcelResult.Failure)
    }

    // ---- modern .xlsx, via a hand-built minimal OOXML zip ----

    @Test
    fun `xlsx with shared strings and date serials is read correctly`() {
        val excelEpoch = LocalDate.of(1899, 12, 30)
        val serial1 = ChronoUnit.DAYS.between(excelEpoch, LocalDate.of(2026, 9, 5))
        val serial2 = ChronoUnit.DAYS.between(excelEpoch, LocalDate.of(2026, 9, 20))

        val sharedStrings = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="6" uniqueCount="6">
            <si><t>Date</t></si>
            <si><t>Narration</t></si>
            <si><t>Debit</t></si>
            <si><t>Credit</t></si>
            <si><t>INVESCO SIP</t></si>
            <si><t>Salary</t></si>
            </sst>
        """.trimIndent()

        val sheet = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <sheetData>
            <row r="1">
            <c r="A1" t="s"><v>0</v></c>
            <c r="B1" t="s"><v>1</v></c>
            <c r="C1" t="s"><v>2</v></c>
            <c r="D1" t="s"><v>3</v></c>
            </row>
            <row r="2">
            <c r="A2"><v>$serial1</v></c>
            <c r="B2" t="s"><v>4</v></c>
            <c r="C2"><v>3000</v></c>
            </row>
            <row r="3">
            <c r="A3"><v>$serial2</v></c>
            <c r="B3" t="s"><v>5</v></c>
            <c r="D3"><v>45000</v></c>
            </row>
            </sheetData>
            </worksheet>
        """.trimIndent()

        val bytes = buildZip(
            "xl/sharedStrings.xml" to sharedStrings,
            "xl/worksheets/sheet1.xml" to sheet
        )

        val result = ParseStatementExcel(bytes) as StatementExcelResult.Success
        assertEquals(2, result.lines.size)
        assertEquals(LocalDate.of(2026, 9, 5), result.lines[0].date)
        assertEquals("INVESCO SIP", result.lines[0].description)
        assertEquals(Money.ofRupees(3000), result.lines[0].amount)
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals(LocalDate.of(2026, 9, 20), result.lines[1].date)
        assertEquals(Money.ofRupees(45000), result.lines[1].amount)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun `an unrecognised file format fails clearly`() {
        assertTrue(ParseStatementExcel("not a spreadsheet".toByteArray()) is StatementExcelResult.Failure)
    }

    private fun buildZip(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
