package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.theme.*

/**
 * Slide-out navigation drawer sheet for the Supplier Portal, matching the design of InventoryDrawerSheet.
 */
@Composable
fun SupplierDrawerSheet(
    userName: String,
    initials: String,
    activeViewTab: Int,
    onTabSelected: (Int) -> Unit,
    ordersCount: Int,
    catalogCount: Int,
    shipmentsCount: Int,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = if (isDarkTheme) NawanoDark200 else Color.White,
        drawerTonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Profile & Branding Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) NawanoDark200 else Color(0xFFF3E8FF),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(NawanoGradientStart, NawanoGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Drawer",
                                tint = textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Verified Supplier • Nawano Partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PORTAL VIEWS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Item 0: Orders
            SupplierDrawerItem(
                label = "Purchase Orders",
                icon = Icons.Default.Inventory2,
                badge = "$ordersCount",
                isSelected = activeViewTab == 0,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(0) }
            )

            // Item 1: Catalog
            SupplierDrawerItem(
                label = "Wholesale Catalog",
                icon = Icons.Default.Category,
                badge = "$catalogCount",
                isSelected = activeViewTab == 1,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(1) }
            )

            // Item 2: Stats
            SupplierDrawerItem(
                label = "Analytics & Performance",
                icon = Icons.Default.BarChart,
                badge = "KPIs",
                isSelected = activeViewTab == 2,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(2) }
            )

            // Item 3: Settings
            SupplierDrawerItem(
                label = "Supplier Settings",
                icon = Icons.Default.Settings,
                isSelected = activeViewTab == 3,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(3) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PREFERENCES & SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dark / Light Mode Switcher
            Surface(
                onClick = onToggleTheme,
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else NawanoPrimary200,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(24.dp))

            // Log Out Button
            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0xFF2D1515) else Color(0xFFFEF2F2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Log Out of Supplier",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierDrawerItem(
    label: String,
    icon: ImageVector,
    badge: String? = null,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val selectedBg = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.25f) else Color(0xFFEEF2FF)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) NawanoPrimary200 else (if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) NawanoPrimary200 else textPrimary,
                modifier = Modifier.weight(1f)
            )
            badge?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NawanoPrimary200 else (if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else (if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
