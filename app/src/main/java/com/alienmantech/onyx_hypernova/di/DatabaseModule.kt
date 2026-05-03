package com.alienmantech.onyx_hypernova.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.alienmantech.onyx_hypernova.data.db.MIGRATION_1_2
import com.alienmantech.onyx_hypernova.data.db.MIGRATION_2_3
import com.alienmantech.onyx_hypernova.data.db.RankItDatabase
import com.alienmantech.onyx_hypernova.data.db.RankedItemDao
import com.alienmantech.onyx_hypernova.data.db.RankedListDao
import com.alienmantech.onyx_hypernova.data.db.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RankItDatabase =
        Room.databaseBuilder(context, RankItDatabase::class.java, "rankit.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideListDao(db: RankItDatabase): RankedListDao = db.rankedListDao()

    @Provides
    fun provideItemDao(db: RankItDatabase): RankedItemDao = db.rankedItemDao()

    @Provides
    fun provideTagDao(db: RankItDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
