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
 * Modern Floating Bottom Navigation Dock for Supplier Portal:
 * - Home (Orders)
 * - Items (Wholesale Catalog)
 * - Center elevated Floating Scanner FAB (Purple gradient with Viewfinder/QR icon)
 * - Report (Issue Reports received from Middlemen, with live open-issue badge)
 * - Middlemen (Middleman List / Directory)
 */
@Composable
fun SupplierBottomBar(
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    onScanClicked: () -> Unit,
    isDarkTheme: Boolean,
    openIssuesCount: Int = 0,
    middlemenCount: Int = 0,
    onOpenMenu: () -> Unit = {},
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
                // Tab 0: Home
                FloatingNavItem(
                    title = "Home",
                    icon = if (activeViewTab == 0) Icons.Default.Home else Icons.Outlined.Home,
                    isSelected = activeViewTab == 0,
                    onClick = { onTabSelected(0) }
                )

                // Tab 1: Items
                FloatingNavItem(
                    title = "Items",
                    icon = if (activeViewTab == 1) Icons.Default.Inventory2 else Icons.Outlined.Inventory2,
                    isSelected = activeViewTab == 1,
                    onClick = { onTabSelected(1) }
                )

                // Center placeholder space for the elevated pop-up scan button
                Spacer(modifier = Modifier.width(52.dp))

                // Tab 5: Report (Issue Reports from Middlemen)
                FloatingNavItem(
                    title = "Report",
                    icon = if (activeViewTab == 5) Icons.Default.ReportProblem else Icons.Outlined.ReportProblem,
                    isSelected = activeViewTab == 5,
                    badgeCount = openIssuesCount,
                    onClick = { onTabSelected(5) }
                )

                // Tab 4: Middlemen (Middleman Directory List)
                FloatingNavItem(
                    title = "Middlemen",
                    icon = if (activeViewTab == 4) Icons.Default.Groups else Icons.Outlined.Groups,
                    isSelected = activeViewTab == 4,
                    onClick = { onTabSelected(4) }
                )
            }
        }

        // Center Elevated Floating Scanner Button (Matching reference Behance mockup)
        Surface(
            onClick = onScanClicked,
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
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Quick Scan Barcode / Waybill",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
