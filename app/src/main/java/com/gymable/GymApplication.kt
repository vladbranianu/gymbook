package com.gymable

import android.app.Application
import com.gymable.data.DataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymApplication : Application() {

    @Inject lateinit var dataSeeder: DataSeeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.SEED_DATABASE) {
            appScope.launch { dataSeeder.seed() }
        }
    }
}
