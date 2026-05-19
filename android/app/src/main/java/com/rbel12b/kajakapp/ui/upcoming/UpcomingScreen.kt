package com.rbel12b.kajakapp.ui.upcoming

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.ui.competitions.ErrorState
import com.rbel12b.kajakapp.ui.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    vm: UpcomingViewModel,
    onRaceClick: (String, String, String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by vm.uiState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📅 Upcoming Races") })
        }
    ) { padding ->
        when (val s = uiState) {
            is UpcomingUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is UpcomingUiState.Error -> Box(Modifier.padding(padding)) {
                ErrorState(s.message) { vm.load() }
            }

            is UpcomingUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    if (s.items.isEmpty() && !s.canLoadMore) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No upcoming races found.")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(s.items, key = { "${it.competitionId}_${it.raceId}" }) { item ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onRaceClick(item.competitionId, item.raceId, item.raceName) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "${item.competitionIcon} ${item.competitionName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (item.isBestFinal) Text("⭐", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(item.raceName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (item.raceRound.isNotBlank()) {
                                                Text(item.raceRound, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Text(
                                                "📅 ${formatDateTime(item.startDate)}",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                }
                            }
                            if (s.canLoadMore) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(onClick = vm::loadMore) { Text("Load more") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
