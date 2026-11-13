package com.example.inventoryapp.ui.supplier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.dashboard.NawanoLogoBadge
import com.example.inventoryapp.ui.theme.*

/**
 * Top App Bar for the Supplier Portal, matching the polished design and branding of Middleman InventoryTopBar.
 */
@Composable
fun SupplierTopBar(
    userName: String,
    initials: String,
    activeViewTab: Int,
    ordersCount: Int,
    catalogCount: Int,
    shipmentsCount: Int = 0,
    pendingNotificationCount: Int,
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenScanner: () -> Unit = {},
    onQuickAdd: () -> Unit = {},
    showTitleAndLogo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val focusManager = LocalFocusManager.current

    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Main Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Hamburger + Title ("Nawano") & Subtitle ("Wholesale • X SKUs")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = textPrimary
                    )
                }

                Column {
                    Text(
                        text = "Nawano",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Wholesale • $catalogCount SKUs",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Group: Dark/Light Theme Toggle, Notification Bell & "+ Add" Quick Action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dark / Light Mode Toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Light Mode" else "Dark Mode",
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Notification Bell with Badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(36.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (pendingNotificationCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = if (pendingNotificationCount > 99) "99+" else "$pendingNotificationCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (pendingNotificationCount > 0) Icons.Default.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = if (pendingNotificationCount > 0) NawanoPrimary200 else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // "+ Add" Button
                TextButton(
                    onClick = onQuickAdd,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ Add",
                        color = NawanoPrimary200,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
