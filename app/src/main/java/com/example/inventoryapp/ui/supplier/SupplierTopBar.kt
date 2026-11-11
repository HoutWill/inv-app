package com.example.inventoryapp.ui.supplier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.inventoryapp.ui.theme.*

/**
 * Top App Bar for the Supplier Portal, matching the polished design of Middleman InventoryTopBar.
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
            // Left Group: Hamburger + Brand Avatar + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = textPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(NawanoGradientStart, NawanoGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Column {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (activeViewTab) {
                            0 -> "Orders • $ordersCount Active"
                            1 -> "Catalog • $catalogCount SKUs"
                            2 -> "Analytics & Performance"
                            else -> "Supplier Settings"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Group: Theme Toggle & Notification Bell (Add and Scan buttons removed per specification)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Dark / Light Mode Toggle
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Light Mode" else "Dark Mode",
                        tint = if (isDarkTheme) Color(0xFFFBBF24) else textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Notification Bell with Badge
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.size(38.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (pendingNotificationCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = "$pendingNotificationCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Collapsible Search Bar Input
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = if (activeViewTab == 1) "Search catalog by name or SKU..." else "Search PO number, items, carrier...",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1),
                    focusedContainerColor = if (isDarkTheme) NawanoDark200 else Color.White,
                    unfocusedContainerColor = if (isDarkTheme) NawanoDark200 else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}
