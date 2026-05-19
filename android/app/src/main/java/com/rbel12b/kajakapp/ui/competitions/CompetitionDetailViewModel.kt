package com.rbel12b.kajakapp.ui.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.CompetitionDetail
import com.rbel12b.kajakapp.data.api.model.RaceEntry
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface CompetitionDetailUiState {
    data object Loading : CompetitionDetailUiState
    data class Success(
        val detail: CompetitionDetail,
        val sortedRaces: List<Pair<String, RaceEntry>>,
    ) : CompetitionDetailUiState
    data class Error(val message: String) : CompetitionDetailUiState
}

class CompetitionDetailViewModel(
    private val repo: KajakRepository,
    private val competitionId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CompetitionDetailUiState>(CompetitionDetailUiState.Loading)
    val uiState: StateFlow<CompetitionDetailUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CompetitionDetailUiState.Loading
            fetchAndUpdate(forceRefresh = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchAndUpdate(forceRefresh = true)
            _isRefreshing.value = false
        }
    }

    private suspend fun fetchAndUpdate(forceRefresh: Boolean) {
        repo.getCompetition(competitionId, forceRefresh).fold(
            onSuccess = { detail ->
                val sorted = detail.races.entries
                    .sortedBy { it.key.toIntOrNull() ?: 0 }
                    .map { it.key to it.value }
                _uiState.value = CompetitionDetailUiState.Success(detail, sorted)
            },
            onFailure = { _uiState.value = CompetitionDetailUiState.Error(it.message ?: "Unknown error") }
        )
    }

    companion object {
        fun factory(repo: KajakRepository, competitionId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CompetitionDetailViewModel(repo, competitionId) as T
        }
    }
}
