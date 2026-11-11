package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.theme.NawanoPrimary200

/**
 * Destinations accessible from the Navigation Drawer.
 */
enum class DrawerDestination {
    DASHBOARD,
    ITEMS,
    STATISTICS,
    INVENTORY_SUMMARY,
    MOVEMENT_LIST,
    SUPPLIERS,
    AUDIT_LOGS,
    SETTINGS
}

/**
 * Complete Navigation Drawer wrapper that wraps the content in a [ModalNavigationDrawer].
 */
@Composable
fun InventoryNavigationDrawer(
    drawerState: DrawerState,
    currentDestination: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    totalSkus: Int,
    userName: String = "Anthony",
    userRole: String = "Inventory Manager • Nawano HQ",
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            InventoryDrawerSheet(
                currentDestination = currentDestination,
                onDestinationSelected = onDestinationSelected,
                onCloseDrawer = onCloseDrawer,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                totalSkus = totalSkus,
                userName = userName,
                userRole = userRole,
                onLogout = onLogout
            )
        },
        content = content
    )
}

/**
 * Authentic Nawano Navigation Drawer Sheet (Behance UI Kit).
 * Organizes core screens, auxiliary static screens (Inventory Summary, Movement List),
 * and dedicated Stock Audit Logs into a unified navigation hub.
 */
@Composable
fun InventoryDrawerSheet(
    currentDestination: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    totalSkus: Int,
    userName: String = "Anthony",
    userRole: String = "Inventory Manager • Nawano HQ",
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
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
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        val initials = userName.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .map { it.first() }
                            .take(2)
                            .joinToString("")
                            .uppercase()
                            .ifEmpty { "NW" }

                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Close Button
                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userRole,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Views Section
            Text(
                text = "MAIN OVERVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            DrawerNavRow(
                icon = Icons.Default.Dashboard,
                label = "Dashboard",
                isSelected = currentDestination == DrawerDestination.DASHBOARD,
                badge = null,
                onClick = { onDestinationSelected(DrawerDestination.DASHBOARD) }
            )

            DrawerNavRow(
                painter = painterResource(R.drawable.ic_inventory_box),
                label = "Items Catalog",
                isSelected = currentDestination == DrawerDestination.ITEMS,
                badge = "$totalSkus",
                onClick = { onDestinationSelected(DrawerDestination.ITEMS) }
            )

            DrawerNavRow(
                icon = Icons.Default.BarChart,
                label = "Statistics (Sales & Target)",
                isSelected = currentDestination == DrawerDestination.STATISTICS,
                badge = null,
                onClick = { onDestinationSelected(DrawerDestination.STATISTICS) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Logistics & Inventory Section
            Text(
                text = "LOGISTICS & AUDIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            DrawerNavRow(
                painter = painterResource(R.drawable.ic_inventory_packages),
                label = "Inventory Summary",
                isSelected = currentDestination == DrawerDestination.INVENTORY_SUMMARY,
                badge = "New",
                badgeColor = NawanoPrimary200,
                onClick = { onDestinationSelected(DrawerDestination.INVENTORY_SUMMARY) }
            )

            DrawerNavRow(
                painter = painterResource(R.drawable.ic_bus_intro),
                label = "Movement List",
                isSelected = currentDestination == DrawerDestination.MOVEMENT_LIST,
                badge = "Logistics",
                badgeColor = Color(0xFF06B6D4),
                onClick = { onDestinationSelected(DrawerDestination.MOVEMENT_LIST) }
            )

            DrawerNavRow(
                icon = Icons.Default.LocalShipping,
                label = "Suppliers Directory",
                isSelected = currentDestination == DrawerDestination.SUPPLIERS,
                badge = "Vendor",
                badgeColor = NawanoPrimary200,
                onClick = { onDestinationSelected(DrawerDestination.SUPPLIERS) }
            )

            DrawerNavRow(
                icon = Icons.Default.History,
                label = "Stock Audit Logs",
                isSelected = currentDestination == DrawerDestination.AUDIT_LOGS,
                badge = null,
                onClick = { onDestinationSelected(DrawerDestination.AUDIT_LOGS) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            // Settings & Quick Actions
            Text(
                text = "SYSTEM & TOOLS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            DrawerNavRow(
                icon = Icons.Default.Settings,
                label = "Settings & Database",
                isSelected = currentDestination == DrawerDestination.SETTINGS,
                badge = null,
                onClick = { onDestinationSelected(DrawerDestination.SETTINGS) }
            )

            DrawerNavRow(
                icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                label = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                isSelected = false,
                badge = if (isDarkTheme) "Dark" else "Light",
                onClick = onToggleTheme
            )

            if (onLogout != null) {
                DrawerNavRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Log Out",
                    isSelected = false,
                    badge = null,
                    onClick = onLogout
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Nawano Inventory Kit v2.4",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Clean Architecture • Offline-First Native",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Backward-compatibility alias for [InventoryDrawerSheet].
 */
@Composable
fun NawanoDrawerSheet(
    currentDestination: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onCloseDrawer: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    totalSkus: Int,
    userName: String = "Anthony",
    userRole: String = "Inventory Manager • Nawano HQ",
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) = InventoryDrawerSheet(
    currentDestination = currentDestination,
    onDestinationSelected = onDestinationSelected,
    onCloseDrawer = onCloseDrawer,
    isDarkTheme = isDarkTheme,
    onToggleTheme = onToggleTheme,
    totalSkus = totalSkus,
    userName = userName,
    userRole = userRole,
    onLogout = onLogout,
    modifier = modifier
)

@Composable
fun DrawerNavRow(
    label: String,
    isSelected: Boolean,
    badge: String?,
    badgeColor: Color = NawanoPrimary200,
    icon: ImageVector? = null,
    painter: Painter? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (painter != null) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        tint = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
