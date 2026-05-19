package com.rbel12b.kajakapp.ui.athletes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.data.api.model.AthleteRaceEntry
import com.rbel12b.kajakapp.ui.competitions.ErrorState
import com.rbel12b.kajakapp.ui.formatDate
import com.rbel12b.kajakapp.ui.positionMedal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteDetailScreen(
    vm: AthleteDetailViewModel,
    athleteName: String,
    onBack: () -> Unit,
    onRaceClick: (String, String, String) -> Unit,
    onSetAsMe: ((String, String) -> Unit)? = null,
    favoriteIds: Set<String> = emptySet(),
    onFavoriteToggle: (String) -> Unit = {},
) {
    val uiState by vm.uiState.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val currentAthleteId = (uiState as? AthleteDetailUiState.Success)?.detail?.athlete?.id

    LaunchedEffect(selectedTab) { listState.scrollToItem(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(athleteName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentAthleteId != null) {
                        val isFav = currentAthleteId in favoriteIds
                        IconButton(onClick = { onFavoriteToggle(currentAthleteId) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (isFav) "Remove from favourites" else "Add to favourites",
                                tint = if (isFav) Color(0xFFFFB300) else LocalContentColor.current,
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val s = uiState) {
            is AthleteDetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is AthleteDetailUiState.Error -> Box(Modifier.padding(padding)) {
                ErrorState(s.message) { vm.load() }
            }

            is AthleteDetailUiState.Success -> {
                val athlete = s.detail.athlete
                val twoDaysAgo = LocalDate.now().minusDays(2).toString()
                val results = s.sortedRaces
                    .filter { it.isFinished || it.race.startDate <= twoDaysAgo }
                val upcoming = s.sortedRaces
                    .filter { !it.isFinished && it.race.startDate > twoDaysAgo }
                val displayList = if (selectedTab == 0) results else upcoming

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "${athlete.emoji.takeIf { it.isNotBlank() }?.let { "$it " } ?: ""}${athlete.name}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(athlete.nation, color = MaterialTheme.colorScheme.secondary)
                                        if (athlete.birthYear.isNotBlank()) Text("born ${athlete.birthYear}")
                                    }
                                    if (athlete.club.isNotBlank()) Text("🏫 ${athlete.club}")
                                    if (athlete.icfWorldRank > 0) {
                                        Text("🌍 ICF world rank #${athlete.icfWorldRank}", color = MaterialTheme.colorScheme.tertiary)
                                    }
                                    if (onSetAsMe != null) {
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedButton(onClick = { onSetAsMe(athlete.id, athlete.name) }) {
                                            Text("Set as Me")
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            PrimaryTabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Results (${results.size})") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("Upcoming (${upcoming.size})") }
                                )
                            }
                        }

                        if (displayList.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(if (selectedTab == 0) "No results yet." else "No upcoming races.")
                                }
                            }
                        } else {
                            items(displayList, key = { it.race.id }) { entry ->
                                AthleteRaceCard(
                                    entry = entry,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    val compId = entry.race.competition?.id ?: return@AthleteRaceCard
                                    onRaceClick(compId, entry.race.id, entry.race.name)
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
private fun AthleteRaceCard(entry: AthleteRaceEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val comp = entry.race.competition
    OutlinedCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (comp != null) {
                    Text(
                        "${comp.icon} ${comp.displayName}".trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(entry.race.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                if (entry.race.round.isNotBlank()) {
                    Text(entry.race.round, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatDate(entry.race.startDate), style = MaterialTheme.typography.bodySmall)
            }
            if (entry.isFinished && entry.finishPosition.isNotBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val medal = positionMedal(entry.finishPosition)
                    if (medal.isNotBlank()) Text(medal, style = MaterialTheme.typography.titleMedium)
                    Text(entry.finishPosition, style = MaterialTheme.typography.labelSmall)
                    if (entry.finishTime.isNotBlank()) {
                        Text(entry.finishTime, style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else if (!entry.isFinished) {
                Text("⏳", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
