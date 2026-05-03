package com.alienmantech.onyx_hypernova.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val rankItRepository: RankItRepository,
    private val contentResolver: ContentResolver
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToUri(uri: Uri) = withContext(Dispatchers.IO) {
        val json = gson.toJson(rankItRepository.exportBackup())
        contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("Could not open output stream for URI: $uri")
    }

    suspend fun importFromUri(uri: Uri) = withContext(Dispatchers.IO) {
        val json = contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("Could not open input stream for URI: $uri")
        val backup = gson.fromJson(json, BackupFile::class.java)
        require(backup.version == 1) { "Unsupported backup version: ${backup.version}" }
        rankItRepository.importBackup(backup)
    }
}
