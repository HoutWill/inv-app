package com.example.inventoryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.ui.auth.AuthViewModel
import com.example.inventoryapp.ui.auth.AuthViewModelFactory
import com.example.inventoryapp.ui.auth.SplashScreen
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import com.example.inventoryapp.ui.dashboard.InventoryViewModelFactory
import com.example.inventoryapp.ui.navigation.AppNavHost
import com.example.inventoryapp.ui.navigation.Routes
import com.example.inventoryapp.ui.theme.InventoryAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val destination = determineStartDestination()
            withContext(Dispatchers.Main) {
                startDestination = destination
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
            val destination = startDestination

            InventoryAppTheme(darkTheme = isDarkMode) {
                if (destination != null) {
                    AppNavHost(
                        startDestination = destination,
                        inventoryViewModel = inventoryViewModel,
                        authViewModel = authViewModel,
                        isDarkTheme = isDarkMode,
                        onToggleTheme = { inventoryViewModel.toggleDarkMode() }
                    )
                } else {
                    SplashScreen()
                }
            }
        }
    }

    private suspend fun determineStartDestination(): String {
        return try {
            val sessionStore = SessionStore(applicationContext)
            val hasSeenIntro = withTimeoutOrNull(2000) {
                sessionStore.hasSeenIntro.first()
            } ?: false

            if (!hasSeenIntro) {
                return Routes.INTRO
            }

            val loggedInUserId = withTimeoutOrNull(2000) {
                sessionStore.loggedInUserId.first()
            }

            if (loggedInUserId.isNullOrBlank()) {
                return Routes.LOGIN
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val user = db.userDao().findById(loggedInUserId)
            if (user != null) {
                if (user.role == UserRole.SUPPLIER) Routes.SUPPLIER_ORDERS else Routes.HOME
            } else {
                sessionStore.clearSession()
                Routes.LOGIN
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading session state, defaulting to LOGIN", e)
            Routes.LOGIN
        }
    }
}
