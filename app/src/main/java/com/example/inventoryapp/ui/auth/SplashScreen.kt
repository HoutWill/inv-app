package com.example.inventoryapp.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.theme.NawanoPrimary200

/**
 * Full-screen purple splash screen matching design image 1.
 * Renders instantly while initial session state is determined.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NawanoPrimary200),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_splash_logo),
            contentDescription = "Nawano Inventory",
            modifier = Modifier.size(96.dp)
        )
    }
}
