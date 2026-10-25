package de.vokabeltrainer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(state: UiState, vm: TrainerViewModel) {
    val quiz = state.quiz ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val pos = minOf(quiz.index + 1, quiz.cards.size)
                    Text("${quiz.language.label} · $pos / ${quiz.cards.size}")
                },
                navigationIcon = {
                    IconButton(onClick = { vm.open(Screen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { quiz.index.toFloat() / quiz.cards.size },
                modifier = Modifier.fillMaxWidth(),
            )
            if (quiz.finished) Summary(quiz, vm) else CardContent(state, quiz, vm)
        }
    }
}

@Composable
private fun CardContent(state: UiState, quiz: QuizState, vm: TrainerViewModel) {
    val card = quiz.current ?: return
    val vocab = card.vocab
    val progress = state.progressOf(vocab)
    val prompt = if (card.showForeign) vocab.foreign else vocab.german
    val solution = if (card.showForeign) vocab.german else vocab.foreign
    val promptLang = if (card.showForeign) quiz.language.label else "Deutsch"
    val solutionLang = if (card.showForeign) "Deutsch" else quiz.language.label

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Card(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StackDot(progress.stack)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Stapel ${progress.stack} · ${progress.known}× gewusst · ${vocab.topic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(promptLang, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(prompt, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                if (quiz.revealed) {
                    HorizontalDivider(Modifier.padding(vertical = 24.dp))
                    Text(solutionLang, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(solution, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    if (vocab.example.isNotEmpty() || vocab.exampleGerman.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text("Beispiel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        if (vocab.example.isNotEmpty()) {
                            Text(vocab.example, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center)
                        }
                        if (vocab.exampleGerman.isNotEmpty()) {
                            Text(
                                vocab.exampleGerman,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (!quiz.revealed) {
            OutlinedButton(onClick = vm::reveal, modifier = Modifier.fillMaxWidth()) { Text("Lösung zeigen") }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { vm.answer(false) },
                colors = ButtonDefaults.buttonColors(containerColor = StackColors[0]),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(6.dp))
                Text("Nicht gewusst")
            }
            Button(
                onClick = { vm.answer(true) },
                colors = ButtonDefaults.buttonColors(containerColor = StackColors[4]),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(6.dp))
                Text("Gewusst")
            }
        }
    }
}

@Composable
private fun Summary(quiz: QuizState, vm: TrainerViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Geschafft!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text("Gewusst: ${quiz.known}", style = MaterialTheme.typography.titleMedium, color = StackColors[4])
        Text("Nicht gewusst: ${quiz.unknown}", style = MaterialTheme.typography.titleMedium, color = StackColors[0])
        Spacer(Modifier.height(32.dp))
        if (quiz.missed.isNotEmpty()) {
            Button(onClick = { vm.startQuiz(quiz.missed) }, modifier = Modifier.fillMaxWidth()) {
                Text("Nicht gewusste wiederholen (${quiz.missed.size})")
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = { vm.open(Screen.HOME) }, modifier = Modifier.fillMaxWidth()) {
            Text("Zurück zur Auswahl")
        }
    }
}

