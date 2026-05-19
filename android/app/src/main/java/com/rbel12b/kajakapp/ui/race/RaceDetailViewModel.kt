package com.rbel12b.kajakapp.ui.race

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.RaceDetail
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RaceDetailUiState {
    data object Loading : RaceDetailUiState
    data class Success(val detail: RaceDetail) : RaceDetailUiState
    data class Error(val message: String) : RaceDetailUiState
}

class RaceDetailViewModel(
    private val repo: KajakRepository,
    private val competitionId: String,
    private val raceId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RaceDetailUiState>(RaceDetailUiState.Loading)
    val uiState: StateFlow<RaceDetailUiState> = _uiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RaceDetailUiState.Loading
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
        repo.getRace(competitionId, raceId, forceRefresh).fold(
            onSuccess = { _uiState.value = RaceDetailUiState.Success(it) },
            onFailure = { _uiState.value = RaceDetailUiState.Error(it.message ?: "Unknown error") }
        )
    }

    companion object {
        fun factory(repo: KajakRepository, competitionId: String, raceId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RaceDetailViewModel(repo, competitionId, raceId) as T
            }
    }
}
