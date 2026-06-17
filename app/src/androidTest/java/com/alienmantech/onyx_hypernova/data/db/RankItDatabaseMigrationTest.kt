package com.alienmantech.onyx_hypernova.data.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RankItDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RankItDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate1To2_addsNullableColorColumn() {
        createDatabaseAtVersion(1)

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).apply {
            query("SELECT color FROM ranked_items WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            close()
        }
    }

    @Test
    fun migrate2To3_createsTagTables() {
        createDatabaseAtVersion(2)

        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).apply {
            query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'tags'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'item_tag_cross_ref'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
            close()
        }
    }

    @Test
    fun migrate3To4_addsLastUsedAtAndBackfillsFromListUpdates() {
        createDatabaseAtVersion(3)

        helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4).apply {
            query("SELECT lastUsedAt FROM tags WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(4_321L, cursor.getLong(0))
            }
            close()
        }
    }

    @Test
    fun migrate4To5_addsNullableBadgeIdColumn() {
        createDatabaseAtVersion(4)

        helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5).apply {
            query("SELECT badgeId FROM ranked_items WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
            }
            close()
        }
    }

    @Test
    fun migrate1To5_preservesLegacyDataAcrossFullUpgradePath() {
        createDatabaseAtVersion(1)

        helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        ).apply {
            query("SELECT name, position, color, badgeId FROM ranked_items WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Millennium Force", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertTrue(cursor.isNull(2))
                assertTrue(cursor.isNull(3))
            }
            close()
        }
    }

    private fun createDatabaseAtVersion(version: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(TEST_DB)
        val path = context.getDatabasePath(TEST_DB)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).apply {
            when (version) {
                1 -> createVersion1Schema(this)
                2 -> createVersion2Schema(this)
                3 -> createVersion3Schema(this)
                4 -> createVersion4Schema(this)
                else -> error("Unsupported test schema version: $version")
            }
            this.version = version
            close()
        }
    }

    private fun createVersion1Schema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ranked_lists (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ranked_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                listId INTEGER NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL,
                FOREIGN KEY(listId) REFERENCES ranked_lists(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_ranked_items_listId ON ranked_items (listId)")
        db.execSQL(
            """
            INSERT INTO ranked_lists (id, name, createdAt, updatedAt)
            VALUES (1, 'Favorites', 1000, 4321)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO ranked_items (id, listId, name, position)
            VALUES (1, 1, 'Millennium Force', 0)
            """.trimIndent()
        )
    }

    private fun createVersion2Schema(db: SQLiteDatabase) {
        createVersion1Schema(db)
        db.execSQL("ALTER TABLE ranked_items ADD COLUMN color TEXT")
    }

    private fun createVersion3Schema(db: SQLiteDatabase) {
        createVersion2Schema(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS item_tag_cross_ref (
                itemId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY(itemId, tagId),
                FOREIGN KEY(itemId) REFERENCES ranked_items(id) ON DELETE CASCADE,
                FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_item_tag_cross_ref_tagId ON item_tag_cross_ref (tagId)")
        db.execSQL("INSERT INTO tags (id, name) VALUES (1, 'steel')")
        db.execSQL("INSERT INTO item_tag_cross_ref (itemId, tagId) VALUES (1, 1)")
    }

    private fun createVersion4Schema(db: SQLiteDatabase) {
        createVersion3Schema(db)
        db.execSQL("ALTER TABLE tags ADD COLUMN lastUsedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE tags
            SET lastUsedAt = COALESCE((
                SELECT MAX(l.updatedAt)
                FROM item_tag_cross_ref x
                INNER JOIN ranked_items i ON i.id = x.itemId
                INNER JOIN ranked_lists l ON l.id = i.listId
                WHERE x.tagId = tags.id
            ), 0)
            """.trimIndent()
        )
        db.execSQL("UPDATE tags SET lastUsedAt = 4321 WHERE id = 1")
    }

    private companion object {
        const val TEST_DB = "migration-rankit-test"
    }
}
