package com.rbel12b.kajakapp.ui.race

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.data.api.model.Boat
import com.rbel12b.kajakapp.data.api.model.RaceDetail
import com.rbel12b.kajakapp.ui.categoryColor
import com.rbel12b.kajakapp.ui.competitions.ErrorState
import com.rbel12b.kajakapp.ui.formatDateTime
import com.rbel12b.kajakapp.ui.positionMedal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceDetailScreen(
    vm: RaceDetailViewModel,
    raceName: String,
    onBack: () -> Unit,
    onAthleteClick: (id: String, name: String) -> Unit = { _, _ -> },
) {
    val uiState by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛶 $raceName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = uiState) {
            is RaceDetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is RaceDetailUiState.Error -> Box(Modifier.padding(padding)) {
                ErrorState(s.message) { vm.load() }
            }

            is RaceDetailUiState.Success -> RaceContent(
                detail = s.detail,
                modifier = Modifier.padding(padding),
                onAthleteClick = onAthleteClick,
            )
        }
    }
}

@Composable
private fun RaceContent(
    detail: RaceDetail,
    modifier: Modifier = Modifier,
    onAthleteClick: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(detail.race.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(detail.race.round, style = MaterialTheme.typography.bodyMedium)
                    Text("📅 ${formatDateTime(detail.race.startDate)}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (detail.isFinished) "✓ Finished" else "⏳ Upcoming",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (detail.isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        )
                        if (detail.race.isBestFinal) Text("⭐ Best Final", style = MaterialTheme.typography.labelMedium)
                    }
                    if (detail.race.raceCategory.isNotBlank()) {
                        Text(
                            detail.race.raceCategory,
                            style = MaterialTheme.typography.labelMedium,
                            color = categoryColor(detail.race.raceCategory),
                        )
                    }
                }
            }
        }

        if (detail.boats.isEmpty()) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No startlist / results yet.")
                    }
                }
            }
        } else {
            items(detail.boats) { boat ->
                BoatCard(boat, onAthleteClick)
            }
        }
    }
}

@Composable
private fun BoatCard(boat: Boat, onAthleteClick: (String, String) -> Unit) {
    val medal = positionMedal(boat.finishPosition)
    val isTop3 = boat.finishPosition in listOf("1.", "2.", "3.")

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Line 1: medal + position + time + delta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (medal.isNotBlank()) Text(medal, style = MaterialTheme.typography.titleMedium)
                Text(
                    boat.finishPosition.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal,
                )
                if (boat.finishTime.isNotBlank()) {
                    Text(
                        boat.finishTime,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = if (isTop3) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (boat.finishTimeDelta.isNotBlank()) {
                    Text(
                        "+${boat.finishTimeDelta}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Lane ${boat.startNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Line 2: athletes + nations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                boat.athletes.forEachIndexed { i, a ->
                    if (i > 0) Text(" / ", style = MaterialTheme.typography.bodySmall)
                    val label = if (a.emoji.isNotBlank()) "${a.name} ${a.emoji}" else a.name
                    if (a.id.isNotBlank()) {
                        TextButton(
                            onClick = { onAthleteClick(a.id, a.name) },
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val nations = boat.athletes.map { it.nation }.distinct().joinToString("/")
                if (nations.isNotBlank()) {
                    Spacer(Modifier.weight(1f))
                    Text(nations, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
