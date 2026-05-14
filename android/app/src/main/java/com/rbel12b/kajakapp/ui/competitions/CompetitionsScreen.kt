package com.rbel12b.kajakapp.ui.competitions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.data.api.model.Competition
import com.rbel12b.kajakapp.ui.categoryColor
import com.rbel12b.kajakapp.ui.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionsScreen(
    vm: CompetitionsViewModel,
    onCompetitionClick: (String, String, String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by vm.uiState.collectAsState()
    val filter by vm.filter.collectAsState()
    val search by vm.search.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏆 Competitions") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = if (filter == CompetitionsFilter.UPCOMING) 0 else 1) {
                Tab(
                    selected = filter == CompetitionsFilter.UPCOMING,
                    onClick = { vm.setFilter(CompetitionsFilter.UPCOMING) },
                    text = { Text("Upcoming") }
                )
                Tab(
                    selected = filter == CompetitionsFilter.ALL,
                    onClick = { vm.setFilter(CompetitionsFilter.ALL) },
                    text = { Text("All") }
                )
            }

            OutlinedTextField(
                value = search,
                onValueChange = vm::setSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search competitions…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearch("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
            )

            when (val s = uiState) {
                is CompetitionsUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is CompetitionsUiState.Error -> ErrorState(s.message) { vm.load() }

                is CompetitionsUiState.Success -> {
                    if (s.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No competitions found.", style = MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.items, key = { it.id }) { comp ->
                                CompetitionCard(comp) {
                                    onCompetitionClick(comp.id, comp.displayName, comp.icon)
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
private fun CompetitionCard(comp: Competition, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${comp.icon} ${comp.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("📍 ${comp.displayLocation}", style = MaterialTheme.typography.bodySmall)
            Text(
                "📅 ${formatDate(comp.startDate)} → ${formatDate(comp.endDate)}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                comp.category,
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor(comp.category),
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Error: $message", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Retry")
            }
        }
    }
}
