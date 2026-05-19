package com.rbel12b.kajakapp.ui.competitions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.data.api.model.RaceEntry
import com.rbel12b.kajakapp.ui.categoryColor
import com.rbel12b.kajakapp.ui.formatDateTime
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionDetailScreen(
    vm: CompetitionDetailViewModel,
    competitionName: String,
    competitionIcon: String,
    onBack: () -> Unit,
    onRaceClick: (String, String) -> Unit,
) {
    val uiState by vm.uiState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$competitionIcon $competitionName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = uiState) {
                is CompetitionDetailUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is CompetitionDetailUiState.Error -> ErrorState(s.message) { vm.load() }

                is CompetitionDetailUiState.Success -> {
                    val twoDaysAgo = LocalDate.now().minusDays(2).toString()
                    val upcoming = s.sortedRaces.filter { (_, entry) ->
                        !entry.isFinished && entry.race.startDate > twoDaysAgo
                    }
                    val finished = s.sortedRaces
                        .filter { (_, entry) ->
                            entry.isFinished || entry.race.startDate <= twoDaysAgo
                        }
                        .sortedByDescending { (_, entry) -> entry.race.startDate }

                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Upcoming (${upcoming.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Results (${finished.size})") }
                        )
                    }

                    val displayList = if (selectedTab == 0) upcoming else finished

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = vm::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (displayList.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(if (selectedTab == 0) "No upcoming races." else "No results yet.")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(displayList, key = { it.first }) { (raceId, entry) ->
                                    RaceEntryCard(entry) { onRaceClick(raceId, entry.race.name) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RaceEntryCard(entry: RaceEntry, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(entry.race.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                if (entry.race.isBestFinal) Text("⭐", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (entry.isFinished) "✓" else "⏳",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                )
            }
            if (entry.race.round.isNotBlank()) {
                Text(entry.race.round, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatDateTime(entry.race.startDate), style = MaterialTheme.typography.bodySmall)
            if (entry.race.raceCategory.isNotBlank()) {
                Text(
                    entry.race.raceCategory,
                    style = MaterialTheme.typography.labelSmall,
                    color = categoryColor(entry.race.raceCategory),
                )
            }
        }
    }
}
