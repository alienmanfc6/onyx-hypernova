package com.alienmantech.onyx_hypernova.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ranked_items ADD COLUMN color TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS item_tag_cross_ref (itemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY(itemId, tagId), FOREIGN KEY(itemId) REFERENCES ranked_items(id) ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_item_tag_cross_ref_tagId ON item_tag_cross_ref (tagId)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ranked_items ADD COLUMN badgeId TEXT")
    }
}
