package com.tapboard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tapboard.app.ui.TapBoardNavHost
import com.tapboard.app.ui.TapBoardViewModel
import com.tapboard.app.ui.TapBoardViewModelFactory
import com.tapboard.app.ui.theme.TapBoardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TapBoardApp
        setContent {
            val vm: TapBoardViewModel = viewModel(
                factory = TapBoardViewModelFactory(app.connectionRepository, app.settingsRepository)
            )
            val darkTheme by vm.darkTheme.collectAsState()
            TapBoardTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TapBoardNavHost(viewModel = vm)
                }
            }
        }
    }
}
