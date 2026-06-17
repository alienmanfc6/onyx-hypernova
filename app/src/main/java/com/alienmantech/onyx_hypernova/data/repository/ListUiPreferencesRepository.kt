package com.alienmantech.onyx_hypernova.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListUiPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isGroupByTag(listId: Long): Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[groupByTagKey(listId)] ?: false }

    suspend fun setGroupByTag(listId: Long, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[groupByTagKey(listId)] = enabled
        }
    }

    private fun groupByTagKey(listId: Long) = booleanPreferencesKey("list_${listId}_group_by_tag")
}
