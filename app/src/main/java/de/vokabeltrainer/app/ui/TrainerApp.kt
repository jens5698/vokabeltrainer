package de.vokabeltrainer.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TrainerApp(viewModel: TrainerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.screen != Screen.HOME) { viewModel.open(Screen.HOME) }
    when (state.screen) {
        Screen.HOME -> HomeScreen(state, viewModel)
        Screen.QUIZ -> QuizScreen(state, viewModel)
        Screen.SETTINGS -> SettingsScreen(state, viewModel)
        Screen.LIST -> ListScreen(state, viewModel)
    }
}
