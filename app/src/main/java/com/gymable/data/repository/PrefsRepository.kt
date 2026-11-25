package com.gymable.data.repository

import android.content.Context
import com.gymable.ui.theme.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("gymable_prefs", Context.MODE_PRIVATE)

    private val _unitSystem = MutableStateFlow(loadUnit())
    val unitSystem: StateFlow<UnitSystem> = _unitSystem.asStateFlow()

    fun setUnitSystem(unit: UnitSystem) {
        prefs.edit().putInt("unit_system", unit.ordinal).apply()
        _unitSystem.value = unit
    }

    private fun loadUnit(): UnitSystem {
        val index = prefs.getInt("unit_system", 0)
        return UnitSystem.values().getOrElse(index) { UnitSystem.KG }
    }

    fun addWorkoutSessionId(id: Long) {
        val current = prefs.getStringSet(KEY_WORKOUT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(id.toString())
        prefs.edit().putStringSet(KEY_WORKOUT_IDS, current).apply()
    }

    fun clearWorkoutSession() {
        prefs.edit().remove(KEY_WORKOUT_IDS).apply()
    }

    fun getWorkoutSessionIds(): List<Long> =
        prefs.getStringSet(KEY_WORKOUT_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.sorted()
            ?: emptyList()

    // Donation tier — only increases, never decreases
    private val _maxDonationTier = MutableStateFlow(prefs.getInt(KEY_MAX_DONATION_TIER, 0))
    val maxDonationTierFlow: StateFlow<Int> = _maxDonationTier.asStateFlow()

    val maxDonationTier: Int get() = _maxDonationTier.value

    fun setMaxDonationTier(tier: Int) {
        if (tier > _maxDonationTier.value) {
            prefs.edit().putInt(KEY_MAX_DONATION_TIER, tier).apply()
            _maxDonationTier.value = tier
        }
    }

    var donationNudgeShown: Boolean
        get() = prefs.getBoolean(KEY_DONATION_NUDGE_SHOWN, false)
        set(value) { prefs.edit().putBoolean(KEY_DONATION_NUDGE_SHOWN, value).apply() }

    fun incrementCompletedWorkouts(): Int {
        val next = prefs.getInt(KEY_COMPLETED_WORKOUTS, 0) + 1
        prefs.edit().putInt(KEY_COMPLETED_WORKOUTS, next).apply()
        return next
    }

    var reviewRequested: Boolean
        get() = prefs.getBoolean(KEY_REVIEW_REQUESTED, false)
        set(value) { prefs.edit().putBoolean(KEY_REVIEW_REQUESTED, value).apply() }

    companion object {
        private const val KEY_WORKOUT_IDS = "workout_session_ids"
        private const val KEY_MAX_DONATION_TIER = "max_donation_tier"
        private const val KEY_DONATION_NUDGE_SHOWN = "donation_nudge_shown"
        private const val KEY_COMPLETED_WORKOUTS = "completed_workouts"
        private const val KEY_REVIEW_REQUESTED = "review_requested"
    }
}
