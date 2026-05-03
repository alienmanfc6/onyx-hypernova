package com.alienmantech.onyx_hypernova.data.backup

data class BackupFile(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val lists: List<BackupList> = emptyList()
)

data class BackupList(
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<BackupItem> = emptyList()
)

data class BackupItem(
    val name: String,
    val position: Int,
    val color: String? = null,
    val tags: List<String> = emptyList()
)
