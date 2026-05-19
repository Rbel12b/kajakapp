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
    data object Loading : AthletesUiState
    data class Ready(
        val favoriteAthletes: List<Athlete>,
        val athletes: List<Athlete>,
        val query: String,
        val tooMany: Boolean = false,
    ) : AthletesUiState
    data class Error(val message: String) : AthletesUiState
}

class AthletesViewModel(
    private val repo: KajakRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AthletesUiState>(AthletesUiState.Loading)
    val uiState: StateFlow<AthletesUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var allAthletes: List<Athlete> = emptyList()
    private var isLoaded = false

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val favoriteIds: StateFlow<Set<String>> = settingsRepo.favoriteIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            repo.getAthletes().fold(
                onSuccess = { list ->
                    allAthletes = list
                    isLoaded = true
                    updateState()
                },
                onFailure = { _uiState.value = AthletesUiState.Error(it.message ?: "Unknown error") }
            )
        }
        viewModelScope.launch {
            favoriteIds.collect { if (isLoaded) updateState() }
        }
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
        if (isLoaded) updateState()
    }

    private fun updateState() {
        val q = _searchQuery.value.lowercase().trim()
        val ids = favoriteIds.value
        val favs = allAthletes.filter { it.id in ids }

        if (q.isEmpty()) {
            _uiState.value = AthletesUiState.Ready(favs, allAthletes.filter { it.id !in ids }, "")
        } else {
            val filteredFavs = favs.filter { matches(it, q) }
            val filteredOthers = allAthletes.filter { it.id !in ids && matches(it, q) }
            _uiState.value = when {
                filteredOthers.size > 40 -> AthletesUiState.Ready(filteredFavs, emptyList(), q, tooMany = true)
                else -> AthletesUiState.Ready(filteredFavs, filteredOthers, q)
            }
        }
    }

    private fun matches(a: Athlete, q: String) =
        a.name.lowercase().contains(q) ||
            a.nation.lowercase().contains(q) ||
            a.club.lowercase().contains(q)

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.getAthletes(forceRefresh = true).fold(
                onSuccess = { list ->
                    allAthletes = list
                    isLoaded = true
                    updateState()
                },
                onFailure = { _uiState.value = AthletesUiState.Error(it.message ?: "Unknown error") }
            )
            _isRefreshing.value = false
        }
    }

    fun toggleFavorite(athleteId: String) {
        viewModelScope.launch { settingsRepo.toggleFavorite(athleteId) }
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
