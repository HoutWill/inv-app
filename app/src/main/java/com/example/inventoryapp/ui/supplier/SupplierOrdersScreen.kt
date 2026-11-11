package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.ui.theme.NawanoLightSurface
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.NawanoTextPrimaryLight
import com.example.inventoryapp.ui.theme.NawanoTextSecondaryLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrdersScreen(
    user: UserEntity,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = NawanoLightSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Supply Orders",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoTextPrimaryLight
                        )
                        Text(
                            text = user.fullName,
                            fontSize = 12.sp,
                            color = NawanoTextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = NawanoPrimary200
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NawanoLightSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No Orders Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoTextPrimaryLight
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Welcome to Nawano Supplier Portal! When the business issues purchase orders for you, they will appear here for confirmation and delivery tracking.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = NawanoTextSecondaryLight,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200
                    )
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}
