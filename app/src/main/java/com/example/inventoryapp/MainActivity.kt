package com.example.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import com.example.inventoryapp.ui.dashboard.InventoryViewModelFactory
import com.example.inventoryapp.ui.navigation.AppNavHost
import com.example.inventoryapp.ui.navigation.Routes
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** null while we are still reading the session, so the splash stays visible. */
    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Purple splash with logo (Theme.InventoryApp.Starting) until we know where to go.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        lifecycleScope.launch {
            val hasSeenIntro = SessionStore(applicationContext).hasSeenIntro.first()
            startDestination = if (hasSeenIntro) Routes.HOME else Routes.INTRO
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: InventoryViewModel = viewModel(
                factory = remember { InventoryViewModelFactory(application) }
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val start = startDestination

            InventoryAppTheme(darkTheme = isDarkMode) {
                if (start != null) {
                    AppNavHost(
                        startDestination = start,
                        inventoryViewModel = viewModel,
                        isDarkTheme = isDarkMode,
                        onToggleTheme = { viewModel.toggleDarkMode() }
                    )
                }
            }
        }
    }
}
