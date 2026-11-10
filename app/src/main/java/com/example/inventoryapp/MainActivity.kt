package com.example.inventoryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.inventoryapp.ui.dashboard.InventoryDashboardScreen
import com.example.inventoryapp.ui.dashboard.InventoryViewModel
import com.example.inventoryapp.ui.dashboard.InventoryViewModelFactory
import com.example.inventoryapp.ui.theme.InventoryAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: InventoryViewModel = viewModel(
                factory = remember { InventoryViewModelFactory(application) }
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            InventoryAppTheme(darkTheme = isDarkMode) {
                InventoryDashboardScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkMode,
                    onToggleTheme = { viewModel.toggleDarkMode() }
                )
            }
        }
    }
}
