package com.gymable.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymable.data.repository.PrefsRepository
import com.gymable.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val prefsRepository: PrefsRepository
) : ViewModel() {

    val selectedAccent: StateFlow<AccentOption> = themeRepository.accent
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentOption.MINT_GREEN)

    val unitSystem: StateFlow<UnitSystem> = prefsRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.Eagerly, UnitSystem.KG)

    val maxDonationTier: StateFlow<Int> = prefsRepository.maxDonationTierFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setAccent(option: AccentOption) {
        themeRepository.setAccent(option)
    }

    fun setUnitSystem(unit: UnitSystem) {
        prefsRepository.setUnitSystem(unit)
    }
}
