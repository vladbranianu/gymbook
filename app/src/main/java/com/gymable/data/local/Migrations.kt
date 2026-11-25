package com.gymable.data.local

import androidx.room.migration.Migration

/**
 * First schema version that has an exported schema file (app/schemas/.../7.json) and is
 * guaranteed to upgrade without losing user data.
 */
const val OLDEST_SUPPORTED_SCHEMA = 7

/**
 * Schema versions that shipped before migrations existed. Upgrading from these still wipes the
 * database, exactly as before. Any other missing migration path now fails loudly instead of
 * silently deleting the user's workout history.
 */
val PRE_MIGRATION_SCHEMA_VERSIONS: IntArray = (1 until OLDEST_SUPPORTED_SCHEMA).toList().toIntArray()

/**
 * Every migration from OLDEST_SUPPORTED_SCHEMA up to GymDatabase.VERSION, one per version step.
 *
 * When changing an entity:
 *  1. Bump GymDatabase.VERSION by one.
 *  2. Add a Migration(N, N + 1) here and append it to this array.
 *  3. Build once so Room exports app/schemas/.../<N+1>.json, and commit that file.
 *  4. Run MigrationTest (androidTest) and MigrationChainTest (unit test).
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()
