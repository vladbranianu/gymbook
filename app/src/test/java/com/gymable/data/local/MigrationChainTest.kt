package com.gymable.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationChainTest {

    @Test
    fun migrationsFormAnUnbrokenChainToTheCurrentVersion() {
        val byStart = ALL_MIGRATIONS.groupBy { it.startVersion }
        var version = OLDEST_SUPPORTED_SCHEMA
        while (version < GymDatabase.VERSION) {
            val step = byStart[version]
            assertTrue("No migration starting at version $version", step != null)
            assertEquals("Multiple migrations start at version $version", 1, step!!.size)
            assertEquals("Migration from $version must advance exactly one version", version + 1, step[0].endVersion)
            version = step[0].endVersion
        }
        assertEquals(GymDatabase.VERSION, version)
    }

    @Test
    fun noMigrationGoesBackwardsOrBelowTheOldestSupportedSchema() {
        ALL_MIGRATIONS.forEach {
            assertTrue("Migration ${it.startVersion}->${it.endVersion} is not an upgrade", it.endVersion > it.startVersion)
            assertTrue("Migration starts below OLDEST_SUPPORTED_SCHEMA", it.startVersion >= OLDEST_SUPPORTED_SCHEMA)
        }
    }

    @Test
    fun destructiveFallbackOnlyCoversVersionsBeforeMigrationsExisted() {
        assertTrue(PRE_MIGRATION_SCHEMA_VERSIONS.all { it < OLDEST_SUPPORTED_SCHEMA })
        assertTrue(OLDEST_SUPPORTED_SCHEMA !in PRE_MIGRATION_SCHEMA_VERSIONS)
    }
}
