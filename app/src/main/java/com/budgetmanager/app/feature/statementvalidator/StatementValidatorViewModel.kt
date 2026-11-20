package com.budgetmanager.app.feature.statementvalidator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.StatementLine
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.domain.MatchStatement
import com.budgetmanager.app.domain.StatementMatchResult
import com.budgetmanager.app.domain.ParseStatementCsv
import com.budgetmanager.app.domain.StatementCsvResult
import com.budgetmanager.app.domain.ParseStatementExcel
import com.budgetmanager.app.domain.StatementExcelResult
import com.budgetmanager.app.domain.ParseStatementPdf
import com.budgetmanager.app.domain.StatementPdfResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private const val DATE_TOLERANCE_DAYS = 3L
private val dateLabelFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

/** What [ParseStatementCsv]/[ParseStatementExcel]/[ParseStatementPdf] all ultimately agree on -
 *  the three parsers have their own result types (different failure shapes: PDF alone can ask for
 *  a password), so the ViewModel normalises each into this before building a report. */
private sealed interface ParsedStatement {
    data class Lines(val lines: List<StatementLine>, val skippedRowCount: Int) : ParsedStatement
    data class Error(val message: String) : ParsedStatement
    data object NeedsPassword : ParsedStatement
}

/**
 * A validator, not a second way to add transactions - compares an imported statement against
 * what the SMS pipeline actually caught (06-backlog.md, "Credit card / bank statement import").
 * Nothing here is ever saved on its own; the one real action (a missing line) hands off to Add
 * Transaction pre-filled, same as accepting a message does. Deliberately a one-off session: the
 * comparison lives only as long as this screen does, nothing is persisted.
 *
 * Three file formats are supported, detected from the file's own bytes rather than trusted to its
 * extension (SAF display names aren't always reliable): CSV/plain text, legacy binary .xls and
 * modern .xlsx (both via [ParseStatementExcel]), and PDF (via [ParseStatementPdf], which may need
 * a password - see [pendingPdf]).
 */
@HiltViewModel
class StatementValidatorViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(StatementValidatorUiState())
    val uiState: StateFlow<StatementValidatorUiState> = internalState.asStateFlow()

    /** The bytes of a password-protected PDF waiting on [onPasswordSubmitted] - cleared on
     *  success, cancel, or picking a different file. */
    private var pendingPdf: Pair<String, ByteArray>? = null

    fun onFileSelected(fileName: String, bytes: ByteArray) {
        pendingPdf = null
        internalState.update { it.copy(isProcessing = true, errorMessage = null, passwordPrompt = null) }
        viewModelScope.launch {
            when (val parsed = parseBytes(fileName, bytes)) {
                is ParsedStatement.Error -> internalState.update {
                    it.copy(isProcessing = false, errorMessage = parsed.message)
                }
                ParsedStatement.NeedsPassword -> {
                    pendingPdf = fileName to bytes
                    internalState.update {
                        it.copy(isProcessing = false, passwordPrompt = PasswordPrompt(fileName))
                    }
                }
                is ParsedStatement.Lines -> finishWithLines(fileName, parsed.lines, parsed.skippedRowCount)
            }
        }
    }

    fun onPasswordSubmitted(password: String) {
        val (fileName, bytes) = pendingPdf ?: return
        internalState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            when (val result = ParseStatementPdf(bytes, password)) {
                is StatementPdfResult.Success -> {
                    pendingPdf = null
                    internalState.update { it.copy(passwordPrompt = null) }
                    finishWithLines(fileName, result.lines, result.skippedRowCount)
                }
                is StatementPdfResult.Failure -> internalState.update {
                    it.copy(isProcessing = false, passwordPrompt = PasswordPrompt(fileName, error = result.reason))
                }
                StatementPdfResult.PasswordRequired -> internalState.update {
                    it.copy(isProcessing = false, passwordPrompt = PasswordPrompt(fileName, error = "That password didn't open the file."))
                }
            }
        }
    }

    fun onPasswordPromptCancelled() {
        pendingPdf = null
        internalState.update { it.copy(isProcessing = false, passwordPrompt = null) }
    }

    /** The IO failure path (permission denied, stream closed mid-read, …) - distinct from a
     *  parse failure, which always produces a specific reason instead. */
    fun onFileReadFailed() {
        internalState.update { it.copy(isProcessing = false, errorMessage = "Couldn't read that file. Try again.") }
    }

    private suspend fun finishWithLines(fileName: String, lines: List<StatementLine>, skippedRowCount: Int) {
        if (lines.isEmpty()) {
            internalState.update {
                it.copy(isProcessing = false, errorMessage = "No readable transaction rows found.")
            }
            return
        }
        val zone = ZoneId.systemDefault()
        val minDate = lines.minOf { it.date }.minusDays(DATE_TOLERANCE_DAYS)
        val maxDate = lines.maxOf { it.date }.plusDays(DATE_TOLERANCE_DAYS)
        val messagesInRange = messageRepository.observeAll().first().filter { message ->
            val d = message.receivedAt.atZone(zone).toLocalDate()
            !d.isBefore(minDate) && !d.isAfter(maxDate)
        }
        val result = MatchStatement(lines, messagesInRange, DATE_TOLERANCE_DAYS)
        internalState.update {
            it.copy(isProcessing = false, step = buildReport(fileName, lines, skippedRowCount, result))
        }
    }

    private fun parseBytes(fileName: String, bytes: ByteArray): ParsedStatement = when (sniffFormat(fileName, bytes)) {
        FileFormat.PDF -> when (val result = ParseStatementPdf(bytes)) {
            is StatementPdfResult.Success -> ParsedStatement.Lines(result.lines, result.skippedRowCount)
            is StatementPdfResult.Failure -> ParsedStatement.Error(result.reason)
            StatementPdfResult.PasswordRequired -> ParsedStatement.NeedsPassword
        }
        FileFormat.EXCEL -> when (val result = ParseStatementExcel(bytes)) {
            is StatementExcelResult.Success -> ParsedStatement.Lines(result.lines, result.skippedRowCount)
            is StatementExcelResult.Failure -> ParsedStatement.Error(result.reason)
        }
        FileFormat.CSV -> when (val result = ParseStatementCsv(decodeText(bytes))) {
            is StatementCsvResult.Success -> ParsedStatement.Lines(result.lines, result.skippedRowCount)
            is StatementCsvResult.Failure -> ParsedStatement.Error(result.reason)
        }
    }

    private enum class FileFormat { PDF, EXCEL, CSV }

    /** The file's own bytes decide the format, not its extension or the MIME type SAF reports -
     *  both are unreliable (a bank's "export" button sometimes mislabels one for the other). */
    private fun sniffFormat(fileName: String, bytes: ByteArray): FileFormat = when {
        bytes.size >= 4 && bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte() -> FileFormat.PDF
        bytes.size >= 4 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() && bytes[3] == 0xE0.toByte() -> FileFormat.EXCEL
        bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() &&
            fileName.substringAfterLast('.', "").lowercase(Locale.ENGLISH) != "csv" -> FileFormat.EXCEL
        else -> FileFormat.CSV
    }

    /** UTF-8 first (the common case, and safe even when the bytes are plain ASCII), with a BOM
     *  stripped if a bank's export includes one. ISO-8859-1 can't actually fail to decode
     *  anything, so it's a reasonable fallback for the rare non-UTF-8 export rather than crashing
     *  on garbled rupee symbols. */
    private fun decodeText(bytes: ByteArray): String {
        val hasUtf8Bom = bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        val body = if (hasUtf8Bom) bytes.copyOfRange(3, bytes.size) else bytes
        return try {
            body.toString(Charsets.UTF_8)
        } catch (_: Exception) {
            body.toString(Charset.forName("ISO-8859-1"))
        }
    }

    fun onAddRequested(line: StatementLineUi) {
        internalState.update { it.copy(navigateToAddTransactionForLine = line) }
    }

    fun onNavigationHandled() {
        internalState.update { it.copy(navigateToAddTransactionForLine = null) }
    }

    fun onReset() {
        pendingPdf = null
        internalState.update { StatementValidatorUiState() }
    }

    fun onDismissError() {
        internalState.update { it.copy(errorMessage = null) }
    }

    private fun buildReport(
        fileName: String,
        lines: List<StatementLine>,
        skippedRowCount: Int,
        result: StatementMatchResult
    ): StatementValidatorStep.Report {
        val minDate = lines.minOf { it.date }
        val maxDate = lines.maxOf { it.date }
        val matchedCount = lines.size - result.missing.size
        val difference = result.statementDebitTotal - result.inAppDebitTotal
        return StatementValidatorStep.Report(
            fileName = fileName,
            dateRangeLabel = "${minDate.format(dateLabelFormatter)} - ${maxDate.format(dateLabelFormatter)}",
            coveragePercent = result.coveragePercent,
            matchedCount = matchedCount,
            lineCount = lines.size,
            skippedRowCount = skippedRowCount,
            statementTotalText = result.statementDebitTotal.formatted(),
            inAppTotalText = result.inAppDebitTotal.formatted(),
            differenceText = difference.formatted(),
            needsReview = result.needsReview.map {
                StatementRowUi(
                    description = it.message.merchant ?: it.message.body.take(60),
                    dateLabel = it.line.date.format(dateLabelFormatter),
                    amountText = it.line.amount.formatted()
                )
            },
            missing = result.missing.map { line ->
                StatementLineUi(
                    description = line.description.ifBlank { "(no description)" },
                    dateLabel = line.date.format(dateLabelFormatter),
                    amountText = line.amount.formatted(),
                    amountPaise = line.amount.paise,
                    dateEpochDay = line.date.toEpochDay()
                )
            },
            rejectedButOnStatement = result.rejectedButOnStatement.map {
                StatementRowUi(
                    description = it.message.merchant ?: it.message.body.take(60),
                    dateLabel = it.line.date.format(dateLabelFormatter),
                    amountText = it.line.amount.formatted()
                )
            },
            amountMismatches = result.amountMismatches.map {
                StatementMismatchUi(
                    description = it.message.merchant ?: it.message.body.take(60),
                    dateLabel = it.line.date.format(dateLabelFormatter),
                    statementAmountText = it.line.amount.formatted(),
                    ourAmountText = it.message.parsedAmount?.formatted() ?: "Amount unknown"
                )
            },
            possibleFalsePositives = result.possibleFalsePositives.map { message ->
                StatementRowUi(
                    description = message.merchant ?: message.body.take(60),
                    dateLabel = message.receivedAt.atZone(ZoneId.systemDefault()).toLocalDate().format(dateLabelFormatter),
                    amountText = message.parsedAmount?.formatted() ?: "Amount unknown"
                )
            }
        )
    }
}
