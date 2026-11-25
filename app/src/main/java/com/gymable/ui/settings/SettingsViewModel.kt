package com.gymable.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.repository.ExportRepository
import com.gymable.data.repository.ExportResult
import com.gymable.data.repository.ImportRepository
import com.gymable.data.repository.PrefsRepository
import com.gymable.ui.theme.UnitSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val importRepository: ImportRepository,
    private val prefsRepository: PrefsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val unitSystem: StateFlow<UnitSystem> = prefsRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.KG)

    fun setUnitSystem(unit: UnitSystem) = prefsRepository.setUnitSystem(unit)

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _importSuccess = MutableStateFlow(false)
    val importSuccess: StateFlow<Boolean> = _importSuccess

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError

    fun exportData(from: Long, until: Long) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _exportResult.value = exportRepository.exportData(from, until)
            } catch (e: Exception) {
                _exportError.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importData(uri: Uri) {
        if (_isImporting.value) return
        _isImporting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Cannot read file")
                importRepository.importFromJson(json)
                _importSuccess.value = true
            } catch (e: Exception) {
                _importError.value = "Import failed: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun consumeExportResult() { _exportResult.value = null }
    fun consumeExportError() { _exportError.value = null }
    fun consumeImportSuccess() { _importSuccess.value = false }
    fun consumeImportError() { _importError.value = null }
}
