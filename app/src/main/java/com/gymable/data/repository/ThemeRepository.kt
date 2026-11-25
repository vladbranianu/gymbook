package com.gymable.data.repository

import android.content.Context
import com.gymable.ui.theme.AccentOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("gymable_theme", Context.MODE_PRIVATE)

    private val _accent = MutableStateFlow(loadAccent())
    val accent: StateFlow<AccentOption> = _accent.asStateFlow()

    fun setAccent(option: AccentOption) {
        prefs.edit().putInt("accent_index", option.ordinal).apply()
        _accent.value = option
    }

    private fun loadAccent(): AccentOption {
        val index = prefs.getInt("accent_index", 0)
        return AccentOption.values().getOrElse(index) { AccentOption.MINT_GREEN }
    }
}
