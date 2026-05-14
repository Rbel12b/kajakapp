package com.rbel12b.kajakapp.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val token by vm.token.collectAsState()
    val saved by vm.saved.collectAsState()
    var showToken by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("API Token", style = MaterialTheme.typography.titleMedium)
            Text(
                "Token used to authenticate with kajakapp.com API. Leave blank to use the built-in default.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = token,
                onValueChange = vm::setToken,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Token") },
                singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showToken = !showToken }) {
                        Text(if (showToken) "Hide" else "Show")
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::save,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = vm::resetToDefault,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset default")
                }
            }

            if (saved) {
                Text(
                    "✓ Token saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
