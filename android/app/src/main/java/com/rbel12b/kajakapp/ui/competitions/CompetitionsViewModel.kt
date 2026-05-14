package com.rbel12b.kajakapp.ui.competitions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface CompetitionsUiState {
    data object Loading : CompetitionsUiState
    data class Success(val items: List<Competition>) : CompetitionsUiState
    data class Error(val message: String) : CompetitionsUiState
}

enum class CompetitionsFilter { UPCOMING, ALL }

class CompetitionsViewModel(private val repo: KajakRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CompetitionsUiState>(CompetitionsUiState.Loading)
    val uiState: StateFlow<CompetitionsUiState> = _uiState

    private var allCompetitions: List<Competition> = emptyList()

    private val _filter = MutableStateFlow(CompetitionsFilter.UPCOMING)
    val filter: StateFlow<CompetitionsFilter> = _filter

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CompetitionsUiState.Loading
            repo.getCompetitions().fold(
                onSuccess = { list ->
                    allCompetitions = list
                    applyFilter()
                },
                onFailure = { _uiState.value = CompetitionsUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun setFilter(f: CompetitionsFilter) {
        _filter.value = f
        applyFilter()
    }

    fun setSearch(q: String) {
        _search.value = q
        applyFilter()
    }

    private fun applyFilter() {
        val today = LocalDate.now().toString()
        val q = _search.value.lowercase()
        val filtered = allCompetitions
            .filter { c ->
                when (_filter.value) {
                    CompetitionsFilter.UPCOMING -> c.endDate >= today
                    CompetitionsFilter.ALL -> true
                }
            }
            .filter { c ->
                q.isEmpty() ||
                    c.displayName.lowercase().contains(q) ||
                    c.displayLocation.lowercase().contains(q) ||
                    c.category.lowercase().contains(q)
            }
            .let { list ->
                when (_filter.value) {
                    CompetitionsFilter.UPCOMING -> list.sortedBy { it.startDate }
                    CompetitionsFilter.ALL -> list.sortedByDescending { it.startDate }
                }
            }
        _uiState.value = CompetitionsUiState.Success(filtered)
    }

    companion object {
        fun factory(repo: KajakRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CompetitionsViewModel(repo) as T
        }
    }
}
