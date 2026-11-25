package com.gymable.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymDatabase::class.java
    )

    @Test
    fun userDataSurvivesAllMigrationsFromOldestSupportedSchema() {
        val name = "migration-test"

        helper.createDatabase(name, OLDEST_SUPPORTED_SCHEMA).apply {
            execSQL("INSERT INTO gyms (id, name) VALUES (1, 'Iron Paradise')")
            execSQL("INSERT INTO muscle_groups (id, name, gymId) VALUES (1, 'Chest', 1)")
            execSQL("INSERT INTO exercises (id, name, muscleGroupId, isUnilateral) VALUES (1, 'Bench Press', 1, 0)")
            execSQL("INSERT INTO exercises (id, name, muscleGroupId, isUnilateral) VALUES (2, 'One-Arm Row', 1, 1)")
            execSQL("INSERT INTO workout_sessions (id, exerciseId, date, rating) VALUES (1, 1, 1700000000000, 8)")
            execSQL("INSERT INTO workout_sets (id, sessionId, setNumber, reps, weight) VALUES (1, 1, 1, 8, 80.0)")
            execSQL("INSERT INTO workout_sets (id, sessionId, setNumber, reps, weight) VALUES (2, 1, 2, 6, 85.0)")
            execSQL("INSERT INTO records (id, exerciseId, maxReps, maxWeight) VALUES (1, 1, 8, 85.0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(name, GymDatabase.VERSION, true, *ALL_MIGRATIONS)

        assertEquals(1, db.count("gyms"))
        assertEquals(1, db.count("muscle_groups"))
        assertEquals(2, db.count("exercises"))
        assertEquals(1, db.count("workout_sessions"))
        assertEquals(2, db.count("workout_sets"))
        assertEquals(1, db.count("records"))
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
}
