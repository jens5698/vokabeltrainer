package de.vokabeltrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import de.vokabeltrainer.app.ui.TrainerApp
import de.vokabeltrainer.app.ui.TrainerViewModel
import de.vokabeltrainer.app.ui.VokabelTheme

class MainActivity : ComponentActivity() {
    private val viewModel: TrainerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VokabelTheme {
                TrainerApp(viewModel)
            }
        }
    }
}
