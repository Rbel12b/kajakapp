package com.rbel12b.kajakapp.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.data.api.model.RaceDetail
import com.rbel12b.kajakapp.data.repository.KajakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UpcomingRaceItem(
    val competitionId: String,
    val raceId: String,
    val competitionName: String,
    val competitionIcon: String,
    val raceName: String,
    val raceRound: String,
    val startDate: String,
    val isBestFinal: Boolean,
)

sealed interface UpcomingUiState {
    data object Loading : UpcomingUiState
    data class Success(val items: List<UpcomingRaceItem>, val canLoadMore: Boolean) : UpcomingUiState
    data class Error(val message: String) : UpcomingUiState
}

class UpcomingViewModel(private val repo: KajakRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UpcomingUiState>(UpcomingUiState.Loading)
    val uiState: StateFlow<UpcomingUiState> = _uiState

    private var allUpcomingComps: List<Competition> = emptyList()
    private var loadedRaces: MutableList<UpcomingRaceItem> = mutableListOf()
    private var compIndex = 0
    private var raceIndex = 1
    private val pageSize = 10

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UpcomingUiState.Loading
            repo.getCompetitions().fold(
                onSuccess = { list ->
                    val today = LocalDate.now().toString()
                    allUpcomingComps = list
                        .filter { it.endDate >= today }
                        .sortedBy { it.startDate }
                    compIndex = 0
                    raceIndex = 1
                    loadedRaces.clear()
                    loadMore()
                },
                onFailure = { _uiState.value = UpcomingUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun loadMore() {
        viewModelScope.launch {
            val target = loadedRaces.size + pageSize
            while (loadedRaces.size < target && compIndex < allUpcomingComps.size) {
                val comp = allUpcomingComps[compIndex]
                val result = repo.getRace(comp.id, raceIndex.toString())
                result.fold(
                    onSuccess = { race ->
                        if (race.race.name.isBlank()) {
                            compIndex++
                            raceIndex = 1
                        } else {
                            if (!race.isFinished) {
                                loadedRaces.add(
                                    UpcomingRaceItem(
                                        competitionId = comp.id,
                                        raceId = raceIndex.toString(),
                                        competitionName = comp.displayName,
                                        competitionIcon = comp.icon,
                                        raceName = race.race.name,
                                        raceRound = race.race.round,
                                        startDate = race.race.startDate,
                                        isBestFinal = race.race.isBestFinal,
                                    )
                                )
                            }
                            raceIndex++
                        }
                    },
                    onFailure = {
                        compIndex++
                        raceIndex = 1
                    }
                )
            }
            val canLoadMore = compIndex < allUpcomingComps.size
            _uiState.value = UpcomingUiState.Success(loadedRaces.toList(), canLoadMore)
        }
    }

    companion object {
        fun factory(repo: KajakRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UpcomingViewModel(repo) as T
        }
    }
}
