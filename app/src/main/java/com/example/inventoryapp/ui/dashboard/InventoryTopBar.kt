package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.theme.NawanoGradientEnd
import com.example.inventoryapp.ui.theme.NawanoGradientStart
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.util.AppCurrency

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
 * Nawano Currency Selector Pill for choosing between USD and Cambodian Riel (KHR).
 */
@Composable
fun CurrencySelectorPill(
    currentCurrency: AppCurrency,
    onCurrencySelected: (AppCurrency) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val pillBorder = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
    val isUsdSelected = currentCurrency == AppCurrency.USD

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder),
        shadowElevation = 0.dp,
        modifier = modifier.height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(2.dp)
        ) {
            // USD Option
            Surface(
                onClick = { onCurrencySelected(AppCurrency.USD) },
                shape = RoundedCornerShape(16.dp),
                color = if (isUsdSelected) NawanoPrimary200 else Color.Transparent
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$ USD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isUsdSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = if (isUsdSelected) Color.White else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // Divider |
            Text(
                text = "|",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            // Riel Option
            val isRielSelected = !isUsdSelected
            Surface(
                onClick = { onCurrencySelected(AppCurrency.KHR) },
                shape = RoundedCornerShape(16.dp),
                color = if (isRielSelected) NawanoPrimary200 else Color.Transparent
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "៛ Riel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isRielSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = if (isRielSelected) Color.White else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * Top App Bar for Inventory Dashboard.
 * Includes Drawer menu button, Nawano brand title with SKU counter, Currency switcher (USD/Riel),
 * Dark/Light mode switcher, and Notifications badge with real-time low-stock indicator.
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
    onAddProduct: (() -> Unit)? = null,
    showTitleAndLogo: Boolean = false,
    showCurrencyPill: Boolean = true,
    customTitle: String? = null,
    pendingPartnerRequestsCount: Int = 0,
    onOpenPartnerNetwork: (() -> Unit)? = null,
    currentCurrency: AppCurrency = AppCurrency.USD,
    onCurrencySelected: ((AppCurrency) -> Unit)? = null,
    onToggleCurrency: (() -> Unit)? = null,
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
            if (customTitle != null) {
                Text(
                    text = customTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (showTitleAndLogo) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Nawano",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
        },
        actions = {
            // Currency Switcher (USD / Riel)
            if (showCurrencyPill) {
                CurrencySelectorPill(
                    currentCurrency = currentCurrency,
                    onCurrencySelected = { currency ->
                        onCurrencySelected?.invoke(currency) ?: onToggleCurrency?.invoke()
                    },
                    isDarkTheme = isDarkTheme
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

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

            // Notification Bell with Stock Alert Badge (orange badge with '3' like screenshot)
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.size(36.dp)
            ) {
                val badgeCount = if (pendingPartnerRequestsCount > 0) pendingPartnerRequestsCount else if (lowStockCount > 0) lowStockCount else 3
                BadgedBox(
                    badge = {
                        if (badgeCount > 0) {
                            Badge(
                                containerColor = Color(0xFFF97316),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else "$badgeCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications & Alerts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Partner Connections Button
            if (onOpenPartnerNetwork != null) {
                IconButton(
                    onClick = onOpenPartnerNetwork,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = "Partner Connections",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Quick "+ Add" Product Action
            if (onAddProduct != null) {
                TextButton(
                    onClick = onAddProduct,
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
    onAddProduct: (() -> Unit)? = null,
    showTitleAndLogo: Boolean = false,
    pendingPartnerRequestsCount: Int = 0,
    onOpenPartnerNetwork: (() -> Unit)? = null,
    currentCurrency: AppCurrency = AppCurrency.USD,
    onCurrencySelected: ((AppCurrency) -> Unit)? = null,
    onToggleCurrency: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) = InventoryTopBar(
    totalSkus = totalSkus,
    lowStockCount = lowStockCount,
    isDarkTheme = isDarkTheme,
    onOpenDrawer = onOpenDrawer,
    onToggleTheme = onToggleTheme,
    onOpenNotifications = onOpenNotifications,
    onAddProduct = onAddProduct,
    showTitleAndLogo = showTitleAndLogo,
    pendingPartnerRequestsCount = pendingPartnerRequestsCount,
    onOpenPartnerNetwork = onOpenPartnerNetwork,
    currentCurrency = currentCurrency,
    onCurrencySelected = onCurrencySelected,
    onToggleCurrency = onToggleCurrency,
    modifier = modifier
)
