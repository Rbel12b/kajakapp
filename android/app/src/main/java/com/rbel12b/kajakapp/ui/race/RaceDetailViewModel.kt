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

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = RaceDetailUiState.Loading
            repo.getRace(competitionId, raceId).fold(
                onSuccess = { _uiState.value = RaceDetailUiState.Success(it) },
                onFailure = { _uiState.value = RaceDetailUiState.Error(it.message ?: "Unknown error") }
            )
        }
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
