package de.vokabeltrainer.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.vokabeltrainer.app.data.Progress
import de.vokabeltrainer.app.data.Vocab

/** All vocabulary of one topic, optionally filtered by stack. Tap an entry for the example sentences. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListScreen(state: UiState, vm: TrainerViewModel) {
    val topic = state.listTopic ?: return
    val language = state.language
    val all = state.data.getValue(language).vocab.filter { it.topic == topic }
    var stackFilter by rememberSaveable { mutableStateOf<Int?>(null) }
    val shown = all.filter { stackFilter == null || state.progressOf(it).stack == stackFilter }
    var expanded by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$topic (${all.size})")
                        Text(language.label, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.open(Screen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.startQuiz(shown) },
                    enabled = shown.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) { Text("Diese abfragen (${shown.size})") }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = stackFilter == null,
                        onClick = { stackFilter = null },
                        label = { Text("Alle (${all.size})") },
                    )
                    for (stack in 1..Progress.STACKS) {
                        val count = all.count { state.progressOf(it).stack == stack }
                        FilterChip(
                            selected = stackFilter == stack,
                            onClick = { stackFilter = if (stackFilter == stack) null else stack },
                            leadingIcon = { StackDot(stack) },
                            label = { Text("$stack ($count)") },
                        )
                    }
                }
            }
            itemsIndexed(shown, key = { i, v -> "$i|${v.id}" }) { _, vocab ->
                VocabRow(
                    vocab = vocab,
                    progress = state.progressOf(vocab),
                    expanded = expanded == vocab.id,
                    onClick = { expanded = if (expanded == vocab.id) null else vocab.id },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VocabRow(vocab: Vocab, progress: Progress, expanded: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StackDot(progress.stack)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(vocab.foreign, fontWeight = FontWeight.SemiBold)
                Text(vocab.german, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${progress.known}✓ ${progress.unknown}✗",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded && (vocab.example.isNotEmpty() || vocab.exampleGerman.isNotEmpty())) {
            Column(Modifier.padding(start = 20.dp, top = 6.dp)) {
                if (vocab.example.isNotEmpty()) {
                    Text(vocab.example, fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodySmall)
                }
                if (vocab.exampleGerman.isNotEmpty()) {
                    Text(
                        vocab.exampleGerman,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
