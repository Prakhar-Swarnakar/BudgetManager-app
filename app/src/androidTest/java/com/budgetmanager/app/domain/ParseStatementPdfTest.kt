package com.budgetmanager.app.domain

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Runs on a phone/emulator, not as a plain JVM unit test. PDFBox-Android's text-extraction
 * classes (`LegacyPDFStreamEngine`, `PDFTextStripper`) throw `ExceptionInInitializerError` on a
 * plain JVM - their static init reaches [PDFBoxResourceLoader], which loads standard-font
 * resources from real Android assets and has no JVM-only fallback. Confirmed directly: the CSV
 * and Excel parser tests for this same feature run fine as plain unit tests; only the PDF ones
 * needed this move.
 */
@RunWith(AndroidJUnit4::class)
class ParseStatementPdfTest {

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun date_description_amount_directionMarker_linesAreReadCorrectly() {
        val pdf = buildMinimalPdf(
            listOf(
                "13 Aug 26 UPI-SWIGGY 557.00 D",
                "30 Aug 26 UPI- 111.00 C",
                "This line has no transaction shape at all"
            )
        )

        val result = ParseStatementPdf(pdf) as StatementPdfResult.Success
        assertEquals(2, result.lines.size)
        assertEquals(LocalDate.of(2026, 8, 13), result.lines[0].date)
        assertEquals("UPI-SWIGGY", result.lines[0].description)
        assertEquals(Money.ofRupees(557), result.lines[0].amount)
        assertEquals(MessageDirection.DEBIT, result.lines[0].direction)
        assertEquals(MessageDirection.CREDIT, result.lines[1].direction)
    }

    @Test
    fun aLineWithDigitsEmbeddedInTheDescription_isStillReadCorrectly() {
        val pdf = buildMinimalPdf(
            listOf("30 Aug 26 PAYMENT RECEIVED 000DP016242085043DvbV8j 70515.00 C")
        )

        val result = ParseStatementPdf(pdf) as StatementPdfResult.Success
        assertEquals(1, result.lines.size)
        assertEquals("PAYMENT RECEIVED 000DP016242085043DvbV8j", result.lines[0].description)
        assertEquals(Money.ofRupees(70515), result.lines[0].amount)
    }

    @Test
    fun aPdfWithNoTransactionShapedLines_failsClearly() {
        val pdf = buildMinimalPdf(listOf("Thank you for banking with us."))
        assertTrue(ParseStatementPdf(pdf) is StatementPdfResult.Failure)
    }

    @Test
    fun anEncryptedPdf_withoutAPassword_asksForOne_thenUnlocksWithTheRightOne() {
        val plain = buildMinimalPdf(listOf("13 Aug 26 UPI-SWIGGY 557.00 D"))
        val encrypted = encrypt(plain, ownerPassword = "owner123", userPassword = "secret")

        assertEquals(StatementPdfResult.PasswordRequired, ParseStatementPdf(encrypted))

        val wrongAttempt = ParseStatementPdf(encrypted, password = "nope") as StatementPdfResult.Failure
        assertTrue(wrongAttempt.reason.isNotBlank())

        val success = ParseStatementPdf(encrypted, password = "secret") as StatementPdfResult.Success
        assertEquals(1, success.lines.size)
        assertEquals(Money.ofRupees(557), success.lines[0].amount)
    }

    private fun encrypt(bytes: ByteArray, ownerPassword: String, userPassword: String): ByteArray {
        PDDocument.load(ByteArrayInputStream(bytes)).use { document ->
            val policy = StandardProtectionPolicy(ownerPassword, userPassword, AccessPermission())
            policy.encryptionKeyLength = 128
            document.protect(policy)
            val out = ByteArrayOutputStream()
            document.save(out)
            return out.toByteArray()
        }
    }

    /** Hand-builds a minimal, byte-offset-accurate single-page PDF with one text line per string
     *  in [lines], rather than going through PDFBox's own content-stream/font-writing APIs - this
     *  keeps the fixture independent of exactly which fonts [PDFBoxResourceLoader] has loaded, and
     *  reading the bytes back exercises exactly the real parsing path [ParseStatementPdf] uses
     *  against a real downloaded statement. */
    private fun buildMinimalPdf(lines: List<String>): ByteArray {
        fun escape(text: String) = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

        val content = buildString {
            var y = 750
            for (line in lines) {
                append("BT /F1 10 Tf 50 $y Td (${escape(line)}) Tj ET\n")
                y -= 20
            }
        }
        val contentBytes = content.toByteArray(StandardCharsets.ISO_8859_1)

        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        )

        val out = ByteArrayOutputStream()
        fun writeAscii(s: String) = out.write(s.toByteArray(StandardCharsets.ISO_8859_1))

        writeAscii("%PDF-1.4\n")
        val offsets = IntArray(6)

        for (i in 1..4) {
            offsets[i] = out.size()
            writeAscii("$i 0 obj\n${objects[i - 1]}\nendobj\n")
        }

        offsets[5] = out.size()
        writeAscii("5 0 obj\n<< /Length ${contentBytes.size} >>\nstream\n")
        out.write(contentBytes)
        writeAscii("\nendstream\nendobj\n")

        val xrefOffset = out.size()
        writeAscii("xref\n0 6\n0000000000 65535 f \n")
        for (i in 1..5) {
            writeAscii("%010d 00000 n \n".format(offsets[i]))
        }
        writeAscii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF")

        return out.toByteArray()
    }
}
