package com.rbel12b.kajakapp.ui.athletes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AthletesUiState {
    data object Idle : AthletesUiState
    data object Loading : AthletesUiState
    data class Results(val items: List<Athlete>, val query: String) : AthletesUiState
    data class TooMany(val count: Int) : AthletesUiState
    data class Error(val message: String) : AthletesUiState
}

class AthletesViewModel(private val repo: KajakRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AthletesUiState>(AthletesUiState.Idle)
    val uiState: StateFlow<AthletesUiState> = _uiState

    private var allAthletes: List<Athlete>? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun setQuery(q: String) {
        val trimmedQuery = q.trim()
        _searchQuery.value = trimmedQuery
        if (trimmedQuery.isBlank()) {
            _uiState.value = AthletesUiState.Idle
            return
        }
        val cached = allAthletes
        if (cached == null) {
            loadAndSearch(trimmedQuery)
        } else {
            applySearch(cached, trimmedQuery)
        }
    }

    private fun loadAndSearch(q: String) {
        viewModelScope.launch {
            _uiState.value = AthletesUiState.Loading
            repo.getAthletes().fold(
                onSuccess = { list ->
                    allAthletes = list
                    applySearch(list, q)
                },
                onFailure = { _uiState.value = AthletesUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    private fun applySearch(athletes: List<Athlete>, q: String) {
        val lower = q.lowercase()
        val filtered = athletes.filter { a ->
            a.name.lowercase().contains(lower) ||
                a.nation.lowercase().contains(lower) ||
                a.club.lowercase().contains(lower)
        }
        _uiState.value = when {
            filtered.size > 40 -> AthletesUiState.TooMany(filtered.size)
            else -> AthletesUiState.Results(filtered, q)
        }
    }

    companion object {
        fun factory(repo: KajakRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AthletesViewModel(repo) as T
        }
    }
}
