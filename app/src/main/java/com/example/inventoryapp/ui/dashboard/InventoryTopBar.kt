package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.ui.theme.NawanoGradientEnd
import com.example.inventoryapp.ui.theme.NawanoGradientStart
import com.example.inventoryapp.ui.theme.NawanoPrimary200

/**
 * Nawano Application Logo Badge with dual ribbon gradient.
 */
@Composable
fun NawanoLogoBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(7.dp)) {
            val w = size.width
            val h = size.height
            // Draw left ribbon
            val pathLeft = Path().apply {
                moveTo(w * 0.42f, 0f)
                lineTo(w * 0.08f, h * 0.22f)
                lineTo(w * 0.08f, h * 0.78f)
                lineTo(w * 0.42f, h)
                lineTo(w * 0.42f, h * 0.65f)
                lineTo(w * 0.24f, h * 0.52f)
                lineTo(w * 0.24f, h * 0.32f)
                lineTo(w * 0.42f, h * 0.20f)
                close()
            }
            // Draw right ribbon
            val pathRight = Path().apply {
                moveTo(w * 0.58f, 0f)
                lineTo(w * 0.92f, h * 0.22f)
                lineTo(w * 0.92f, h * 0.78f)
                lineTo(w * 0.58f, h)
                lineTo(w * 0.58f, h * 0.65f)
                lineTo(w * 0.76f, h * 0.52f)
                lineTo(w * 0.76f, h * 0.32f)
                lineTo(w * 0.58f, h * 0.20f)
                close()
            }
            val brush = Brush.linearGradient(
                colors = listOf(NawanoGradientStart, Color(0xFFB57BF8), NawanoGradientEnd),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
            drawPath(pathLeft, brush)
            drawPath(pathRight, brush)
        }
    }
}

/**
 * Top App Bar for Inventory Dashboard.
 * Includes Drawer menu button, Nawano brand title with SKU counter, Dark/Light mode switcher,
 * and Notifications badge with real-time low-stock indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTopBar(
    totalSkus: Int,
    lowStockCount: Int,
    isDarkTheme: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    showTitleAndLogo: Boolean = false,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            if (showTitleAndLogo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    NawanoLogoBadge()
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nawano",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Smart Stock Manager • $totalSkus SKUs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            // Dark / Light Mode Toggle Button
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkTheme) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Notification Bell with Stock Alert Badge
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.size(36.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (lowStockCount > 0) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (lowStockCount > 99) "99+" else lowStockCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (lowStockCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                        contentDescription = "Notifications & Alerts",
                        tint = if (lowStockCount > 0) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Backward-compatibility alias for [InventoryTopBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NawanoTopBar(
    totalSkus: Int,
    lowStockCount: Int,
    isDarkTheme: Boolean,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    showTitleAndLogo: Boolean = false,
    modifier: Modifier = Modifier
) = InventoryTopBar(
    totalSkus = totalSkus,
    lowStockCount = lowStockCount,
    isDarkTheme = isDarkTheme,
    onOpenDrawer = onOpenDrawer,
    onToggleTheme = onToggleTheme,
    onOpenNotifications = onOpenNotifications,
    showTitleAndLogo = showTitleAndLogo,
    modifier = modifier
)
