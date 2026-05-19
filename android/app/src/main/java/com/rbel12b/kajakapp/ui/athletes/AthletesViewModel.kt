package com.rbel12b.kajakapp.ui.athletes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.data.repository.KajakRepository
import com.rbel12b.kajakapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AthletesUiState {
    data object Idle : AthletesUiState
    data object Loading : AthletesUiState
    data class Results(val items: List<Athlete>, val query: String) : AthletesUiState
    data class TooMany(val count: Int) : AthletesUiState
    data class Error(val message: String) : AthletesUiState
}

class AthletesViewModel(
    private val repo: KajakRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AthletesUiState>(AthletesUiState.Idle)
    val uiState: StateFlow<AthletesUiState> = _uiState

    private var allAthletes: List<Athlete>? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val favoriteIds: StateFlow<Set<String>> = settingsRepo.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _favoriteAthletes = MutableStateFlow<List<Athlete>>(emptyList())
    val favoriteAthletes: StateFlow<List<Athlete>> = _favoriteAthletes

    init {
        viewModelScope.launch {
            var preloadStarted = false
            favoriteIds.collect { ids ->
                val cached = allAthletes
                if (cached != null) {
                    _favoriteAthletes.value = cached.filter { it.id in ids }
                } else if (ids.isNotEmpty() && !preloadStarted) {
                    preloadStarted = true
                    launch {
                        repo.getAthletes().onSuccess { list ->
                            allAthletes = list
                            _favoriteAthletes.value = list.filter { it.id in favoriteIds.value }
                        }
                    }
                }
            }
        }
    }

    fun toggleFavorite(athleteId: String) {
        viewModelScope.launch { settingsRepo.toggleFavorite(athleteId) }
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
        if (q.isBlank()) {
            _uiState.value = AthletesUiState.Idle
            return
        }
        val cached = allAthletes
        if (cached == null) {
            loadAndSearch(q)
        } else {
            applySearch(cached, q)
        }
    }

    private fun loadAndSearch(q: String) {
        viewModelScope.launch {
            _uiState.value = AthletesUiState.Loading
            repo.getAthletes().fold(
                onSuccess = { list ->
                    allAthletes = list
                    _favoriteAthletes.value = list.filter { it.id in favoriteIds.value }
                    applySearch(list, q)
                },
                onFailure = { _uiState.value = AthletesUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    private fun applySearch(athletes: List<Athlete>, q: String) {
        val lower = q.lowercase().trim()
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
        fun factory(repo: KajakRepository, settingsRepo: SettingsRepository) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AthletesViewModel(repo, settingsRepo) as T
            }
    }
}
