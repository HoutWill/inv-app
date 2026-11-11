package com.example.inventoryapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.ui.auth.AuthViewModel
import com.example.inventoryapp.ui.auth.LoginScreen
import com.example.inventoryapp.ui.auth.OnboardingScreen
import com.example.inventoryapp.ui.auth.ResetPasswordScreen
import com.example.inventoryapp.ui.auth.SignUpScreen
import com.example.inventoryapp.ui.dashboard.InventoryDashboardScreen
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import com.example.inventoryapp.ui.supplier.SupplierOrdersScreen
import kotlinx.coroutines.launch

object Routes {
    const val INTRO = "intro"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val RESET_PASSWORD = "reset_password"
    const val HOME = "home"
    const val SUPPLIER_ORDERS = "supplier_orders"
}

@Composable
fun AppNavHost(
    startDestination: String,
    inventoryViewModel: InventoryViewModel,
    authViewModel: AuthViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.INTRO) {
            OnboardingScreen(
                onFinished = {
                    scope.launch { sessionStore.setHasSeenIntro() }
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { user ->
                    val target = if (user.role == UserRole.SUPPLIER) Routes.SUPPLIER_ORDERS else Routes.HOME
                    navController.navigate(target) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Routes.SIGN_UP)
                },
                onNavigateToResetPassword = {
                    navController.navigate(Routes.RESET_PASSWORD)
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                viewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.RESET_PASSWORD) {
            ResetPasswordScreen(
                viewModel = authViewModel,
                onResetSuccess = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.HOME) {
            val user = currentUser
            val userName = user?.fullName?.ifBlank { "Anthony" } ?: "Anthony"
            val userRole = if (user?.role == UserRole.SUPPLIER) {
                "Supplier • Verified Partner"
            } else {
                "Inventory Manager • Nawano HQ"
            }

            InventoryDashboardScreen(
                viewModel = inventoryViewModel,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                userName = userName,
                userRole = userRole,
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.SUPPLIER_ORDERS) {
            val user = currentUser ?: UserEntity(
                fullName = "Too Frozen Food (Supplier)",
                email = "supplier@nawano.com",
                passwordHash = "",
                role = UserRole.SUPPLIER
            )
            val dashState by inventoryViewModel.uiState.collectAsState()

            SupplierOrdersScreen(
                user = user,
                orders = dashState.orders,
                onConfirmOrder = { orderId, note ->
                    inventoryViewModel.confirmOrder(orderId, note)
                },
                onRejectOrder = { orderId, reason ->
                    inventoryViewModel.rejectOrder(orderId, reason)
                },
                onMarkDelivered = { orderId ->
                    inventoryViewModel.markOrderDelivered(orderId)
                },
                onLogout = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
