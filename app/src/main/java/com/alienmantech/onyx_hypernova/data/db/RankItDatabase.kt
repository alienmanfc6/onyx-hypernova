package com.alienmantech.onyx_hypernova.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RankedListEntity::class, RankedItemEntity::class, TagEntity::class, ItemTagCrossRef::class],
    version = 3,
    exportSchema = true
)
abstract class RankItDatabase : RoomDatabase() {
    abstract fun rankedListDao(): RankedListDao
    abstract fun rankedItemDao(): RankedItemDao
    abstract fun tagDao(): TagDao
}
