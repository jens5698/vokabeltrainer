package de.vokabeltrainer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.vokabeltrainer.app.data.Language
import de.vokabeltrainer.app.data.Progress
import de.vokabeltrainer.app.data.Vocab

private fun stackCounts(vocab: List<Vocab>, state: UiState): IntArray {
    val counts = IntArray(Progress.STACKS)
    vocab.forEach { counts[state.progressOf(it).stack - 1]++ }
    return counts
}

private val stackHints = listOf("0×", "1×", "2×", "3×", "4×+")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: UiState, vm: TrainerViewModel) {
    val language = state.language
    val data = state.data.getValue(language)
    val topics = state.topicsOf(language)
    val byTopic = data.vocab.groupBy { it.topic }
    val inTopics = state.vocabInSelectedTopics()
    val selectedCounts = stackCounts(inTopics, state)
    val candidates = state.quizCandidates().size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vokabeltrainer") },
                actions = {
                    IconButton(onClick = vm::reloadAll) { Icon(Icons.Default.Refresh, "Neu laden") }
                    IconButton(onClick = { vm.open(Screen.SETTINGS) }) { Icon(Icons.Default.Settings, "Einstellungen") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.startQuiz() },
                    enabled = candidates > 0,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                ) { Text("Abfrage starten ($candidates Vokabeln)") }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                TabRow(selectedTabIndex = language.ordinal) {
                    Language.entries.forEach { lang ->
                        Tab(
                            selected = lang == language,
                            onClick = { vm.selectLanguage(lang) },
                            text = { Text("${lang.label} (${state.data.getValue(lang).vocab.size})") },
                        )
                    }
                }
                if (data.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (data.status.isNotEmpty()) {
                    Text(
                        data.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (data.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item { SectionTitle("Abfragerichtung") }
            item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Direction.entries.forEach { dir ->
                        FilterChip(
                            selected = state.direction == dir,
                            onClick = { vm.setDirection(dir) },
                            label = { Text(dir.label(language)) },
                        )
                    }
                }
            }

            item { SectionTitle("Stapel (wie oft gewusst)") }
            item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (stack in 1..Progress.STACKS) {
                        FilterChip(
                            selected = stack in state.selectedStacks,
                            onClick = { vm.toggleStack(stack) },
                            leadingIcon = { StackDot(stack) },
                            label = { Text("$stack: ${stackHints[stack - 1]} (${selectedCounts[stack - 1]})") },
                        )
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Themen",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.selectAllTopics(true) }) { Text("Alle") }
                    TextButton(onClick = { vm.selectAllTopics(false) }) { Text("Keine") }
                }
            }
            if (topics.isEmpty() && !data.loading) {
                item {
                    Text(
                        "Noch keine Vokabeln geladen.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(topics, key = { it }) { topic ->
                val vocab = byTopic[topic].orEmpty()
                TopicRow(
                    topic = topic,
                    total = vocab.size,
                    counts = stackCounts(vocab, state),
                    selected = state.isTopicSelected(language, topic),
                    onToggle = { vm.toggleTopic(topic) },
                    onShowList = { vm.openList(topic) },
                )
                HorizontalDivider(Modifier.padding(start = 64.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun StackDot(stack: Int, size: Int = 10) {
    Box(Modifier.size(size.dp).background(StackColors[stack - 1], CircleShape))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopicRow(topic: String, total: Int, counts: IntArray, selected: Boolean, onToggle: () -> Unit, onShowList: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("$topic ($total)", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                counts.forEachIndexed { i, count ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StackDot(i + 1, 8)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${i + 1} ($count)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        IconButton(onClick = onShowList) {
            Icon(Icons.AutoMirrored.Filled.List, "Vokabeln anzeigen")
        }
    }
}
