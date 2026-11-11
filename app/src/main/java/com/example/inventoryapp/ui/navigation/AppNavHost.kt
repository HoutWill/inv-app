package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.ui.auth.OnboardingScreen
import com.example.inventoryapp.ui.dashboard.InventoryDashboardScreen
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import kotlinx.coroutines.launch

object Routes {
    const val INTRO = "intro"
    const val HOME = "home"
    // Step 2 adds: LOGIN, SIGN_UP, RESET_PASSWORD, SUPPLIER_ORDERS
}

@Composable
fun AppNavHost(
    startDestination: String,
    inventoryViewModel: InventoryViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.INTRO) {
            OnboardingScreen(
                onFinished = {
                    scope.launch { sessionStore.setHasSeenIntro() }
                    // Step 2: navigate to LOGIN instead of HOME.
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            InventoryDashboardScreen(
                viewModel = inventoryViewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}
