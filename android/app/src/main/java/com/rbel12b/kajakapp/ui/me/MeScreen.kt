package com.rbel12b.kajakapp.ui.me

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rbel12b.kajakapp.data.repository.SettingsRepository
import com.rbel12b.kajakapp.ui.athletes.AthleteDetailScreen
import com.rbel12b.kajakapp.ui.athletes.AthleteDetailViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    settingsRepo: SettingsRepository,
    makeAthleteDetailVm: (String) -> AthleteDetailViewModel,
    onRaceClick: (String, String, String) -> Unit,
    onClearMe: () -> Unit,
) {
    val selectedId by settingsRepo.selectedAthleteIdFlow.collectAsState(initial = null)
    val selectedName by settingsRepo.selectedAthleteNameFlow.collectAsState(initial = null)

    if (selectedId == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("👤 Me") }) }
        ) { padding ->
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "No athlete selected.\n\nSearch an athlete and tap \"Set as Me\" to see your race history here.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }
    } else {
        val vm = remember(selectedId) { makeAthleteDetailVm(selectedId!!) }
        AthleteDetailScreen(
            vm = vm,
            athleteName = selectedName ?: selectedId!!,
            onBack = onClearMe,
            onRaceClick = onRaceClick,
            onSetAsMe = null,
        )
    }
}
