package com.alienmantech.onyx_hypernova.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alienmantech.onyx_hypernova.data.db.TagSummary
import com.alienmantech.onyx_hypernova.data.repository.RankItRepository
import com.alienmantech.onyx_hypernova.data.repository.TagRenameResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageTagsUiState(
    val tags: List<TagSummary> = emptyList()
)

@HiltViewModel
class ManageTagsViewModel @Inject constructor(
    private val repo: RankItRepository
) : ViewModel() {

    val uiState: StateFlow<ManageTagsUiState> = repo.getTagSummaries()
        .map { ManageTagsUiState(tags = it) }
        .catch { emit(ManageTagsUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageTagsUiState())

    fun hasTagNamed(name: String, excludeTagId: Long? = null): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false

        return uiState.value.tags.any { tag ->
            tag.id != excludeTagId && tag.name.equals(trimmed, ignoreCase = true)
        }
    }

    fun createTag(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            onComplete(repo.createTag(name) != null)
        }
    }

    fun renameTag(tagId: Long, newName: String, onComplete: (TagRenameResult) -> Unit) {
        viewModelScope.launch {
            onComplete(repo.renameTag(tagId, newName))
        }
    }

    fun mergeTags(sourceTagId: Long, targetTagId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repo.mergeTags(sourceTagId, targetTagId)
            onComplete()
        }
    }

    fun deleteTag(tagId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repo.deleteTag(tagId)
            onComplete()
        }
    }
}
