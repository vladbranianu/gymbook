package com.gymable.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("gymable_prefs", Context.MODE_PRIVATE)

    val isComplete: Boolean
        get() = prefs.getBoolean("onboarding_done", false)

    fun markComplete() {
        prefs.edit().putBoolean("onboarding_done", true).apply()
    }
}
