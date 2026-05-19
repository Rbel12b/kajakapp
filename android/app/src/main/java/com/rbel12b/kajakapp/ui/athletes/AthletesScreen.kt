package com.rbel12b.kajakapp.ui.athletes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.rbel12b.kajakapp.data.api.model.Athlete
import com.rbel12b.kajakapp.ui.competitions.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthletesScreen(
    vm: AthletesViewModel,
    onAthleteClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val uiState by vm.uiState.collectAsState()
    val query by vm.searchQuery.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🏅 Athletes") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, nation or club…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
            )

            when (val s = uiState) {
                is AthletesUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is AthletesUiState.Error -> ErrorState(s.message) { vm.refresh() }

                is AthletesUiState.Ready -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = vm::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (s.favoriteAthletes.isNotEmpty()) {
                                item(key = "header_favs") {
                                    SectionHeader("Favourites")
                                }
                                items(s.favoriteAthletes, key = { "fav_${it.id}" }) { athlete ->
                                    AthleteCard(
                                        athlete = athlete,
                                        isFavorite = true,
                                        onFavoriteToggle = { vm.toggleFavorite(athlete.id) },
                                        onClick = { onAthleteClick(athlete.id, athlete.name) },
                                    )
                                }
                            }

                            if (s.tooMany) {
                                item(key = "too_many") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "Too many results. Narrow your search.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            } else if (s.athletes.isNotEmpty()) {
                                item(key = "header_athletes") {
                                    if (s.query.isEmpty()) {
                                        SectionHeader("All Athletes (${s.athletes.size + s.favoriteAthletes.size})")
                                    } else if (s.favoriteAthletes.isNotEmpty()) {
                                        SectionHeader("Results")
                                    }
                                }
                                items(s.athletes, key = { it.id }) { athlete ->
                                    AthleteCard(
                                        athlete = athlete,
                                        isFavorite = athlete.id in favoriteIds,
                                        onFavoriteToggle = { vm.toggleFavorite(athlete.id) },
                                        onClick = { onAthleteClick(athlete.id, athlete.name) },
                                    )
                                }
                            } else if (s.query.isNotEmpty() && s.favoriteAthletes.isEmpty()) {
                                item(key = "empty") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("No athletes found.") }
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
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun AthleteCard(
    athlete: Athlete,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (athlete.emoji.isNotBlank()) Text(athlete.emoji)
                    Text(athlete.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(athlete.nation, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    if (athlete.birthYear.isNotBlank()) {
                        Text("born ${athlete.birthYear}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (athlete.club.isNotBlank()) {
                    Text(athlete.club, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (athlete.icfWorldRank > 0) {
                Text("🌍 #${athlete.icfWorldRank}", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                    tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
