package com.budgetmanager.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.backup.BackupSerializer
import com.budgetmanager.app.data.backup.BackupValidationResult
import com.budgetmanager.app.data.repository.BackupRepository
import com.budgetmanager.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state not backed by a repository - lives only as long as this screen does. */
private data class TransientState(
    val pendingImport: PendingImportUi? = null,
    val importError: String? = null,
    val importSuccessMessage: String? = null,
    val exportError: String? = null
)

/** The settings-repository-backed flags, combined in one intermediate step since there are more
 *  of them than kotlinx.coroutines' direct combine() overload (5) handles. */
private data class SettingsFlags(
    val newSpendsAlertsEnabled: Boolean,
    val eightyPercentAlertsEnabled: Boolean,
    val overBudgetAlertsEnabled: Boolean,
    val trendsMonthsShown: Int,
    val lastExportAtMillis: Long?
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientState())

    private val flags = combine(
        settingsRepository.observeNewSpendsAlertsEnabled(),
        settingsRepository.observeEightyPercentAlertsEnabled(),
        settingsRepository.observeOverBudgetAlertsEnabled(),
        settingsRepository.observeTrendsMonthsShown(),
        settingsRepository.observeLastExportAt()
    ) { newSpends, eightyPercent, overBudget, trendsMonthsShown, lastExportAt ->
        SettingsFlags(newSpends, eightyPercent, overBudget, trendsMonthsShown, lastExportAt)
    }

    val uiState: StateFlow<SettingsUiState> = combine(flags, transientState) { flags, transient ->
        SettingsUiState(
            isLoading = false,
            newSpendsAlertsEnabled = flags.newSpendsAlertsEnabled,
            eightyPercentAlertsEnabled = flags.eightyPercentAlertsEnabled,
            overBudgetAlertsEnabled = flags.overBudgetAlertsEnabled,
            trendsMonthsShown = flags.trendsMonthsShown,
            lastExportAtMillis = flags.lastExportAtMillis,
            pendingImport = transient.pendingImport,
            importError = transient.importError,
            importSuccessMessage = transient.importSuccessMessage,
            exportError = transient.exportError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onNewSpendsAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNewSpendsAlertsEnabled(enabled) }
    }

    fun onEightyPercentAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEightyPercentAlertsEnabled(enabled) }
    }

    fun onOverBudgetAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOverBudgetAlertsEnabled(enabled) }
    }

    fun onTrendsMonthsShownChanged(months: Int) {
        viewModelScope.launch { settingsRepository.setTrendsMonthsShown(months) }
    }

    /** Called by the screen once it has a destination Uri from the system file picker - the
     *  ViewModel builds the JSON, the screen (which owns the ContentResolver) writes it. */
    suspend fun buildExportJson(): String = BackupSerializer.serialize(backupRepository.buildBackup())

    fun onExportCompleted() {
        viewModelScope.launch { settingsRepository.setLastExportAt(System.currentTimeMillis()) }
    }

    fun onExportFailed() {
        transientState.update { it.copy(exportError = "Couldn't write the backup file. Try again.") }
    }

    /** Validates the file the user picked - malformed, missing a field, or from a newer app
     *  version are all rejected here, before anything in the database changes. */
    fun onImportFileSelected(rawJson: String) {
        when (val result = BackupSerializer.validate(rawJson)) {
            is BackupValidationResult.Valid -> transientState.update {
                val bundle = result.bundle
                it.copy(
                    pendingImport = PendingImportUi(
                        bundle = bundle,
                        categoryCount = bundle.categories.size,
                        transactionCount = bundle.transactions.size,
                        smsMessageCount = bundle.smsMessages.size
                    ),
                    importError = null
                )
            }
            is BackupValidationResult.Invalid -> transientState.update {
                it.copy(importError = result.reason, pendingImport = null)
            }
        }
    }

    fun onImportReadFailed() {
        transientState.update { it.copy(importError = "Couldn't read that file. Try again.") }
    }

    fun onConfirmImport() {
        val bundle = transientState.value.pendingImport?.bundle ?: return
        viewModelScope.launch {
            backupRepository.restore(bundle)
            transientState.update {
                it.copy(pendingImport = null, importSuccessMessage = "Import complete. Your data has been replaced.")
            }
        }
    }

    fun onCancelImport() {
        transientState.update { it.copy(pendingImport = null) }
    }

    fun onDismissImportError() {
        transientState.update { it.copy(importError = null) }
    }

    fun onDismissImportSuccess() {
        transientState.update { it.copy(importSuccessMessage = null) }
    }

    fun onDismissExportError() {
        transientState.update { it.copy(exportError = null) }
    }
}
