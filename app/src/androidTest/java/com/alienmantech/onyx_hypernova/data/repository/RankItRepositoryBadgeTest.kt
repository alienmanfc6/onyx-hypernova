package com.alienmantech.onyx_hypernova.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alienmantech.onyx_hypernova.data.db.RankItDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RankItRepositoryBadgeTest {
    private lateinit var db: RankItDatabase
    private lateinit var repo: RankItRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RankItDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RankItRepository(db, db.rankedListDao(), db.rankedItemDao(), db.tagDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun setBadgeForItem_replacesAndClearsBadge() = runBlocking {
        val listId = repo.createList("Coasters")
        repo.addItem(listId, "Mystic", 0)
        val itemId = db.rankedItemDao().getItemsForListOnce(listId).single().id
        val initialUpdatedAt = db.rankedListDao().getListById(listId)?.updatedAt ?: error("Missing list")

        Thread.sleep(5)
        repo.setBadgeForItem(itemId, "night")
        assertEquals("night", db.rankedItemDao().getItemById(itemId)?.badgeId)
        val updatedAtAfterBadge = db.rankedListDao().getListById(listId)?.updatedAt ?: error("Missing list")
        check(updatedAtAfterBadge > initialUpdatedAt)

        repo.setBadgeForItem(itemId, "trimless")
        assertEquals("trimless", db.rankedItemDao().getItemById(itemId)?.badgeId)

        repo.setBadgeForItem(itemId, null)
        assertNull(db.rankedItemDao().getItemById(itemId)?.badgeId)
    }

    @Test
    fun exportAndImportBackup_preservesBadgeId() = runBlocking {
        val listId = repo.createList("Favorites")
        repo.addItem(listId, "Orion", 0, tags = listOf("b&m"))
        val itemId = db.rankedItemDao().getItemsForListOnce(listId).single().id
        repo.setBadgeForItem(itemId, "night")

        val backup = repo.exportBackup()
        assertEquals("night", backup.lists.single().items.single().badgeId)

        repo.importBackup(backup)

        val restoredList = db.rankedListDao().getAllListsOnce().single()
        val restoredItem = db.rankedItemDao().getItemsForListOnce(restoredList.id).single()
        assertEquals("night", restoredItem.badgeId)
    }
}
