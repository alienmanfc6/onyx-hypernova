package com.alienmantech.onyx_hypernova.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alienmantech.onyx_hypernova.data.backup.BackupRepository
import com.alienmantech.onyx_hypernova.data.db.RankedListEntity
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository
import com.alienmantech.onyx_hypernova.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListWithCount(val list: RankedListEntity, val itemCount: Int)

sealed class BackupStatus {
    object Idle : BackupStatus()
    object InProgress : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

data class HomeUiState(
    val lists: List<ListWithCount> = emptyList(),
    val isDarkMode: Boolean = false,
    val backupStatus: BackupStatus = BackupStatus.Idle
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: RankItRepository,
    private val themeRepo: ThemeRepository,
    private val backupRepo: BackupRepository
) : ViewModel() {

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(repo.getAllLists(), themeRepo.isDarkMode) { lists, dark -> lists to dark }
            .flatMapLatest { (lists, dark) ->
                if (lists.isEmpty()) {
                    flowOf(HomeUiState(isDarkMode = dark))
                } else {
                    val countFlows = lists.map { list ->
                        repo.getItemCount(list.id).map { count -> ListWithCount(list, count) }
                    }
                    combine(countFlows) { array -> HomeUiState(array.toList(), dark) }
                }
            },
        _backupStatus
    ) { homeState, status ->
        homeState.copy(backupStatus = status)
    }
        .catch { /* TODO: surface DB errors to UI */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.createList(name) }
    }

    fun renameList(list: RankedListEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch { repo.renameList(list, newName) }
    }

    fun deleteList(list: RankedListEntity) {
        viewModelScope.launch { repo.deleteList(list) }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            themeRepo.setDarkMode(!uiState.value.isDarkMode)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress
            runCatching { backupRepo.exportToUri(uri) }
                .onSuccess { _backupStatus.value = BackupStatus.Success("Backup exported successfully") }
                .onFailure { _backupStatus.value = BackupStatus.Error("Export failed: ${it.message}") }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.InProgress
            runCatching { backupRepo.importFromUri(uri) }
                .onSuccess { _backupStatus.value = BackupStatus.Success("Backup imported successfully") }
                .onFailure { _backupStatus.value = BackupStatus.Error("Import failed: ${it.message}") }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = BackupStatus.Idle
    }
}
