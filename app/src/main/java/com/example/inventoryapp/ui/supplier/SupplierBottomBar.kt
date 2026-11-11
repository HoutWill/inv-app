package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.ui.dashboard.FloatingNavItem
import com.example.inventoryapp.ui.theme.*

/**
 * Modern Floating Bottom Navigation Bar for Supplier Portal.
 * Features central elevated quick-scan FloatingScanButton matching Middleman.
 *
 * Tabs:
 * 0: Orders
 * 1: Wholesale Catalog
 * 2: Stats (Analytics & Performance)
 * 3: Settings (Preferences & Profile)
 */
@Composable
fun SupplierBottomBar(
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClicked: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(top = 18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = if (isDarkTheme) NawanoDark200 else Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) NawanoDarkOutline else NawanoPrimary200.copy(alpha = 0.20f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Orders
                FloatingNavItem(
                    title = "Orders",
                    icon = if (activeViewTab == 0) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                    isSelected = activeViewTab == 0,
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: Catalog
                FloatingNavItem(
                    title = "Catalog",
                    icon = if (activeViewTab == 1) Icons.Default.Category else Icons.Outlined.Category,
                    isSelected = activeViewTab == 1,
                    onClick = { onTabSelected(1) }
                )

                // Center placeholder space for the elevated pop-up scan button
                Spacer(modifier = Modifier.width(52.dp))

                // Tab 2: Stats (Analytics & Performance)
                FloatingNavItem(
                    title = "Stats",
                    icon = if (activeViewTab == 2) Icons.Default.BarChart else Icons.Outlined.BarChart,
                    isSelected = activeViewTab == 2,
                    onClick = { onTabSelected(2) }
                )

                // Tab 3: Settings
                FloatingNavItem(
                    title = "Settings",
                    icon = if (activeViewTab == 3) Icons.Default.Settings else Icons.Outlined.Settings,
                    isSelected = activeViewTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }

        // Center Elevated Floating Add Button (Matching Middleman floating button with Nawano Gradient)
        Surface(
            onClick = onAddClicked,
            shape = CircleShape,
            color = Color.Transparent,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                4.dp,
                if (isDarkTheme) NawanoDark400 else MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-18).dp)
                .size(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(NawanoGradientStart, NawanoGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Order / Add",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
