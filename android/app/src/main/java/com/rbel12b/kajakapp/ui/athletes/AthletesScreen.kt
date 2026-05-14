package com.rbel12b.kajakapp.ui.athletes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                is AthletesUiState.Idle -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Enter a name, nation (e.g. HUN) or club",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is AthletesUiState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is AthletesUiState.TooMany -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Too many results (${s.count}). Narrow your search.")
                }

                is AthletesUiState.Error -> ErrorState(s.message) { vm.setQuery(query) }

                is AthletesUiState.Results -> {
                    if (s.items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No athletes found.")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(s.items, key = { it.id }) { athlete ->
                                AthleteCard(athlete) { onAthleteClick(athlete.id, athlete.name) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AthleteCard(athlete: Athlete, onClick: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
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
        }
    }
}
