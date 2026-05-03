package com.alienmantech.onyx_hypernova.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alienmantech.onyx_hypernova.data.db.RankedListEntity
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository
import com.alienmantech.onyx_hypernova.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ListWithCount(val list: RankedListEntity, val itemCount: Int)

data class HomeUiState(
    val lists: List<ListWithCount> = emptyList(),
    val isDarkMode: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: RankItRepository,
    private val themeRepo: ThemeRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getAllLists(),
        themeRepo.isDarkMode
    ) { lists, dark ->
        Pair(lists, dark)
    }.flatMapLatest { (lists, dark) ->
        if (lists.isEmpty()) {
            flowOf(HomeUiState(isDarkMode = dark))
        } else {
            val countFlows = lists.map { list ->
                repo.getItemCount(list.id).map { count -> ListWithCount(list, count) }
            }
            combine(countFlows) { array -> HomeUiState(array.toList(), dark) }
        }
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
}
