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
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.ui.auth.AuthViewModel
import com.example.inventoryapp.ui.auth.AuthViewModelFactory
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import com.example.inventoryapp.ui.dashboard.InventoryViewModelFactory
import com.example.inventoryapp.ui.navigation.AppNavHost
import com.example.inventoryapp.ui.navigation.Routes
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** null while reading session state from DataStore/Room, keeping splash visible. */
    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        lifecycleScope.launch {
            val sessionStore = SessionStore(applicationContext)
            val hasSeenIntro = sessionStore.hasSeenIntro.first()

            if (!hasSeenIntro) {
                startDestination = Routes.INTRO
                return@launch
            }

            val loggedInUserId = sessionStore.loggedInUserId.first()
            if (loggedInUserId.isNullOrBlank()) {
                startDestination = Routes.LOGIN
                return@launch
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val user = db.userDao().findById(loggedInUserId)
            if (user != null) {
                startDestination = if (user.role == UserRole.SUPPLIER) Routes.SUPPLIER_ORDERS else Routes.HOME
            } else {
                sessionStore.clearSession()
                startDestination = Routes.LOGIN
            }
        }

        enableEdgeToEdge()
        setContent {
            val inventoryViewModel: InventoryViewModel = viewModel(
                factory = remember { InventoryViewModelFactory(application) }
            )
            val authViewModel: AuthViewModel = viewModel(
                factory = remember { AuthViewModelFactory(application) }
            )
            val isDarkMode by inventoryViewModel.isDarkMode.collectAsState()
            val start = startDestination

            InventoryAppTheme(darkTheme = isDarkMode) {
                if (start != null) {
                    AppNavHost(
                        startDestination = start,
                        inventoryViewModel = inventoryViewModel,
                        authViewModel = authViewModel,
                        isDarkTheme = isDarkMode,
                        onToggleTheme = { inventoryViewModel.toggleDarkMode() }
                    )
                }
            }
        }
    }
}
