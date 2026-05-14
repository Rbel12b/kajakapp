package com.rbel12b.kajakapp.ui.athletes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.AthleteDetail
import com.rbel12b.kajakapp.data.api.model.AthleteRaceEntry
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AthleteDetailUiState {
    data object Loading : AthleteDetailUiState
    data class Success(
        val detail: AthleteDetail,
        val sortedRaces: List<AthleteRaceEntry>,
    ) : AthleteDetailUiState
    data class Error(val message: String) : AthleteDetailUiState
}

class AthleteDetailViewModel(
    private val repo: KajakRepository,
    private val athleteId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AthleteDetailUiState>(AthleteDetailUiState.Loading)
    val uiState: StateFlow<AthleteDetailUiState> = _uiState

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = AthleteDetailUiState.Loading
            repo.getAthlete(athleteId).fold(
                onSuccess = { detail ->
                    val sorted = detail.races.values
                        .sortedByDescending { it.race.startDate }
                    _uiState.value = AthleteDetailUiState.Success(detail, sorted)
                },
                onFailure = { _uiState.value = AthleteDetailUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    companion object {
        fun factory(repo: KajakRepository, athleteId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AthleteDetailViewModel(repo, athleteId) as T
        }
    }
}
