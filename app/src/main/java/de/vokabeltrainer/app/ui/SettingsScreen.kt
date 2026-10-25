package de.vokabeltrainer.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.vokabeltrainer.app.data.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: UiState, vm: TrainerViewModel) {
    val links = remember { Language.entries.associateWith { mutableStateOf(vm.settings.link(it)) } }
    var resetOnWrong by remember { mutableStateOf(vm.settings.resetOnWrong) }
    var confirmReset by remember { mutableStateOf<Language?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = { vm.open(Screen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text("Vokabeldateien in Google Drive", style = MaterialTheme.typography.titleMedium)
            Text(
                "In Google Drive bei der Datei „Teilen“ → „Jeder mit dem Link“ (Betrachter) wählen, " +
                    "„Link kopieren“ und hier einfügen. Die Dateien werden bei jedem Start neu geladen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Language.entries.forEach { lang ->
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = links.getValue(lang).value,
                    onValueChange = { links.getValue(lang).value = it },
                    label = { Text("Link Vokabeln_${lang.fileSuffix}.xlsx (${lang.label})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bei „Nicht gewusst“ zurück auf Stapel 1", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Aus: Es wird nur mitgezählt, der Stapel bleibt. An: klassischer Karteikasten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = resetOnWrong, onCheckedChange = { resetOnWrong = it })
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.saveSettings(links.mapValues { it.value.value }, resetOnWrong) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Speichern") }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))
            Text("Lernfortschritt", style = MaterialTheme.typography.titleMedium)
            Language.entries.forEach { lang ->
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { confirmReset = lang }, modifier = Modifier.fillMaxWidth()) {
                    Text("${lang.label}: Fortschritt zurücksetzen")
                }
            }
        }
    }

    confirmReset?.let { lang ->
        AlertDialog(
            onDismissRequest = { confirmReset = null },
            title = { Text("Fortschritt zurücksetzen?") },
            text = { Text("Alle ${lang.label}-Vokabeln landen wieder in Stapel 1. Das lässt sich nicht rückgängig machen.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetProgress(lang)
                    confirmReset = null
                }) { Text("Zurücksetzen") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = null }) { Text("Abbrechen") } },
        )
    }
}
