package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
 * Slide-out navigation drawer sheet for the Supplier Portal, matching the authentic design of InventoryDrawerSheet.
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
    onSwitchToMiddleman: (() -> Unit)? = null,
    onOpenShipments: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 290.dp)
            .fillMaxWidth(0.82f)
            .fillMaxHeight(),
        drawerContainerColor = if (isDarkTheme) NawanoDark200 else Color.White,
        drawerTonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Profile & Branding Header (Matching Middleman Drawer Header)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) NawanoDark400 else MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials.filter { it.isLetter() }.take(2).ifBlank { "FS" },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Supplier",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Views Section (One-Word Section & Item Names)
            Text(
                text = "OVERVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Item 0: Orders
            SupplierDrawerItem(
                label = "Orders",
                icon = Icons.Default.Dashboard,
                badge = "$ordersCount",
                isSelected = activeViewTab == 0,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(0) }
            )

            // Item 1: Catalog
            SupplierDrawerItem(
                label = "Catalog",
                icon = Icons.Default.Inventory2,
                badge = "$catalogCount",
                isSelected = activeViewTab == 1,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(1) }
            )

            // Item 2: Analytics
            SupplierDrawerItem(
                label = "Analytics",
                icon = Icons.Default.BarChart,
                badge = null,
                isSelected = activeViewTab == 2,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(2) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Logistics Section
            Text(
                text = "LOGISTICS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            SupplierDrawerItem(
                label = "Shipments",
                icon = Icons.Default.LocalShipping,
                badge = if (shipmentsCount > 0) "$shipmentsCount" else null,
                badgeColor = Color(0xFF6366F1),
                isSelected = false,
                isDarkTheme = isDarkTheme,
                onClick = onOpenShipments
            )

            SupplierDrawerItem(
                label = "Alerts",
                icon = Icons.Default.Notifications,
                badge = null,
                badgeColor = Color(0xFFF59E0B),
                isSelected = false,
                isDarkTheme = isDarkTheme,
                onClick = onOpenNotifications
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // System Section
            Text(
                text = "SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textSecondary.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Item 3: Settings
            SupplierDrawerItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                isSelected = activeViewTab == 3,
                isDarkTheme = isDarkTheme,
                onClick = { onTabSelected(3) }
            )

            // Theme Switcher (One-Word Label: Theme)
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
                        text = "Theme",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFFFBBF24).copy(alpha = 0.15f) else NawanoPrimary200.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isDarkTheme) "Dark" else "Light",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFFBBF24) else NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(20.dp))

            if (onSwitchToMiddleman != null) {
                SupplierDrawerItem(
                    label = "Middleman",
                    icon = Icons.Default.SwapHoriz,
                    badge = "HQ",
                    badgeColor = NawanoPrimary200,
                    isSelected = false,
                    isDarkTheme = isDarkTheme,
                    onClick = onSwitchToMiddleman
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Log Out Button (One-Word Label: Logout)
            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0xFF2D1515) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
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
                        text = "Logout",
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
    badgeColor: Color = NawanoPrimary200,
    isSelected: Boolean = false,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit
) {
    val activeBg = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer
    val activeBorder = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeBg else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, activeBorder) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) NawanoPrimary200 else textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) (if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onPrimaryContainer) else textPrimary,
                modifier = Modifier.weight(1f)
            )
            badge?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = it,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
