package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.SupplierPaymentStatus
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Nawano Notifications & Alerts Modal Bottom Sheet.
 * Tabs:
 *  0 -> Stock Alerts (Out of stock & low stock with 1-tap +10 restock)
 *  1 -> Supplier (Purchase order deliveries, confirmations, and COD due alerts)
 *  2 -> Activity (Recent stock movements & adjustments)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockNotificationsDialog(
    lowStockProducts: List<ProductEntity>,
    recentTransactions: List<StockTransactionEntity>,
    purchaseOrders: List<PurchaseOrderEntity> = emptyList(),
    suppliers: List<SupplierEntity> = emptyList(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onDismiss: () -> Unit,
    onRestock: (ProductEntity, Int) -> Unit,
    onOpenOrder: (PurchaseOrderEntity) -> Unit = {},
    onOpenSuppliers: () -> Unit = {},
    onViewAllCatalog: () -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Stock, 1=Supplier, 2=Activity

    // Per-tab filter & sort state
    var stockFilter by remember { mutableStateOf("All") }
    var stockSortByName by remember { mutableStateOf(false) }

    var supplierFilter by remember { mutableStateOf("All") }
    var supplierSortByAmount by remember { mutableStateOf(false) }

    var activityFilter by remember { mutableStateOf("All") }
    var activitySortOldest by remember { mutableStateOf(false) }

    val filteredLowStock = remember(lowStockProducts, stockFilter, stockSortByName) {
        val list = when (stockFilter) {
            "Out of Stock" -> lowStockProducts.filter { it.quantity <= 0 }
            "Low Stock" -> lowStockProducts.filter { it.quantity > 0 }
            else -> lowStockProducts
        }
        if (stockSortByName) list.sortedBy { it.name.lowercase(Locale.ROOT) }
        else list.sortedBy { it.quantity }
    }

    val supplierOrdersBase = remember(purchaseOrders) {
        purchaseOrders.filter { it.status != PurchaseOrderStatus.CANCELLED }
    }
    val filteredSupplierOrders = remember(supplierOrdersBase, supplierFilter, supplierSortByAmount) {
        val list = when (supplierFilter) {
            "Active" -> supplierOrdersBase.filter {
                it.status == PurchaseOrderStatus.PENDING ||
                    it.status == PurchaseOrderStatus.CONFIRMED ||
                    it.status == PurchaseOrderStatus.DELIVERED
            }
            "COD Due" -> supplierOrdersBase.filter {
                it.paymentStatus == SupplierPaymentStatus.UNPAID && it.status != PurchaseOrderStatus.REJECTED
            }
            "Received" -> supplierOrdersBase.filter { it.status == PurchaseOrderStatus.RECEIVED }
            else -> supplierOrdersBase
        }
        if (supplierSortByAmount) list.sortedByDescending { it.totalAmount }
        else list.sortedByDescending { it.updatedAt }
    }

    val filteredTransactions = remember(recentTransactions, activityFilter, activitySortOldest) {
        val base = recentTransactions.take(40).filter { tx ->
            when (activityFilter) {
                "Stock In" -> tx.transactionType == "STOCK_IN" || tx.transactionType == "PO_RECEIVED" || tx.quantityDelta > 0
                "Stock Out" -> tx.transactionType == "STOCK_OUT" || tx.transactionType == "POS_SALE" || tx.quantityDelta < 0
                else -> true
            }
        }
        val sorted = if (activitySortOldest) base.sortedBy { it.timestamp } else base.sortedByDescending { it.timestamp }
        sorted.take(20)
    }

    val supplierActionableCount = remember(supplierOrdersBase) {
        supplierOrdersBase.count {
            it.status == PurchaseOrderStatus.DELIVERED ||
                it.status == PurchaseOrderStatus.CONFIRMED ||
                it.status == PurchaseOrderStatus.PENDING ||
                (it.paymentStatus == SupplierPaymentStatus.UNPAID && it.status != PurchaseOrderStatus.REJECTED)
        }
    }

    val sheetBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerBg = if (isDarkTheme) NawanoDark400 else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = textSecondary.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val hasUrgent = lowStockProducts.isNotEmpty() || supplierActionableCount > 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasUrgent) NawanoPrimary200.copy(alpha = 0.14f) else NawanoEmerald.copy(alpha = 0.14f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = if (hasUrgent) NawanoPrimary200 else NawanoEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Notifications",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "${lowStockProducts.size} stock • $supplierActionableCount supplier updates",
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Modern Pill Segmented Control (Stock | Supplier | Activity)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val tabs = listOf(
                        "Stock (${lowStockProducts.size})",
                        "Supplier (${supplierOrdersBase.size})",
                        "Activity (${recentTransactions.take(20).size})"
                    )
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            onClick = { selectedTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NawanoPrimary200 else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Filter & Sort Row
            val activeFilters = when (selectedTab) {
                0 -> listOf("All", "Out of Stock", "Low Stock")
                1 -> listOf("All", "Active", "COD Due", "Received")
                else -> listOf("All", "Stock In", "Stock Out")
            }
            val currentFilter = when (selectedTab) {
                0 -> stockFilter
                1 -> supplierFilter
                else -> activityFilter
            }
            val currentSortLabel = when (selectedTab) {
                0 -> if (stockSortByName) "A-Z" else "Lowest"
                1 -> if (supplierSortByAmount) "Highest $" else "Newest"
                else -> if (activitySortOldest) "Oldest" else "Newest"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeFilters.forEach { option ->
                        val isSelected = currentFilter == option
                        Surface(
                            onClick = {
                                when (selectedTab) {
                                    0 -> stockFilter = option
                                    1 -> supplierFilter = option
                                    else -> activityFilter = option
                                }
                            },
                            shape = CircleShape,
                            color = if (isSelected) NawanoPrimary200.copy(alpha = 0.14f) else innerBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NawanoPrimary200.copy(alpha = 0.55f) else borderCol
                            )
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NawanoPrimary200 else textSecondary,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Sort Pill Button
                Surface(
                    onClick = {
                        when (selectedTab) {
                            0 -> stockSortByName = !stockSortByName
                            1 -> supplierSortByAmount = !supplierSortByAmount
                            else -> activitySortOldest = !activitySortOldest
                        }
                    },
                    shape = CircleShape,
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort",
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = currentSortLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeBottomSheetNestedDismiss()
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                        }
                    },
                    label = "NotificationsTabTransition"
                ) { tab ->
                    when (tab) {
                        0 -> StockAlertsTabContent(
                            lowStockProducts = filteredLowStock,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onRestock = onRestock
                        )
                        1 -> SupplierAlertsTabContent(
                            orders = filteredSupplierOrders,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onOpenOrder = { order ->
                                onDismiss()
                                onOpenOrder(order)
                            }
                        )
                        else -> ActivityAlertsTabContent(
                            transactions = filteredTransactions,
                            cardBg = cardBg,
                            borderCol = borderCol,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderCol)
                ) {
                    Text("Dismiss", fontWeight = FontWeight.SemiBold, color = textPrimary)
                }

                Button(
                    onClick = {
                        onDismiss()
                        if (selectedTab == 1) onOpenSuppliers() else onViewAllCatalog()
                    },
                    modifier = Modifier
                        .weight(1.35f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
                ) {
                    Text(
                        text = if (selectedTab == 1) "Suppliers Directory" else "View Catalog",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StockAlertsTabContent(
    lowStockProducts: List<ProductEntity>,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onRestock: (ProductEntity, Int) -> Unit
) {
    if (lowStockProducts.isEmpty()) {
        NotificationEmptyState(
            icon = Icons.Default.CheckCircle,
            iconTint = NawanoEmerald,
            title = "All Stock Levels Healthy!",
            subtitle = "No products are currently under reorder thresholds.",
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(lowStockProducts, key = { it.id }) { product ->
                val isOutOfStock = product.quantity <= 0
                val alertColor = if (isOutOfStock) NawanoRose else NawanoAmber

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, alertColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = alertColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isOutOfStock) Icons.Default.ErrorOutline else Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = alertColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = product.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = product.sku,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = NawanoPrimary200
                                    )
                                    Text(text = "•", color = textSecondary)
                                    Text(
                                        text = if (isOutOfStock) "Out of Stock (0)"
                                        else "${product.quantity} left (Min: ${product.reorderLevel})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = alertColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onRestock(product, 10) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NawanoPrimary200,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+10", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierAlertsTabContent(
    orders: List<PurchaseOrderEntity>,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    onOpenOrder: (PurchaseOrderEntity) -> Unit
) {
    if (orders.isEmpty()) {
        NotificationEmptyState(
            icon = Icons.Outlined.LocalShipping,
            iconTint = NawanoPrimary200,
            title = "No Supplier Updates Yet",
            subtitle = "Purchase orders, deliveries, and COD supplier payments will appear here.",
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(orders, key = { it.id }) { order ->
                val (statusLabel, accentColor, statusIcon) = when (order.status) {
                    PurchaseOrderStatus.DELIVERED -> Triple("In Delivery • Tap to Receive", Color(0xFF0284C7), Icons.Outlined.LocalShipping)
                    PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed by Supplier", NawanoPrimary200, Icons.Outlined.Verified)
                    PurchaseOrderStatus.PENDING -> Triple("Awaiting Supplier Confirmation", NawanoAmber, Icons.Outlined.Schedule)
                    PurchaseOrderStatus.RECEIVED -> Triple("Received & Stocked", NawanoEmerald, Icons.Outlined.Inventory2)
                    PurchaseOrderStatus.REJECTED -> Triple("Rejected by Supplier", NawanoRose, Icons.Outlined.Cancel)
                    PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", textSecondary, Icons.Outlined.Block)
                }
                val isUnpaid = order.paymentStatus == SupplierPaymentStatus.UNPAID && order.status != PurchaseOrderStatus.REJECTED

                Surface(
                    onClick = { onOpenOrder(order) },
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = order.supplierName.ifBlank { "Supplier" },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textPrimary
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${order.orderNumber} • $statusLabel",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isUnpaid) NawanoAmber.copy(alpha = 0.14f) else NawanoEmerald.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${order.paymentMethod.name} • ${if (isUnpaid) "Due" else "Paid"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnpaid) Color(0xFFD97706) else Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(order.updatedAt)),
                                    fontSize = 10.5.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityAlertsTabContent(
    transactions: List<StockTransactionEntity>,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    if (transactions.isEmpty()) {
        NotificationEmptyState(
            icon = Icons.Default.History,
            iconTint = textSecondary,
            title = "No Recent Activity Recorded",
            subtitle = "Stock movements, restocks, and POS deductions will show here.",
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(transactions, key = { it.id }) { tx ->
                val isStockIn = tx.transactionType == "STOCK_IN" || tx.transactionType == "PO_RECEIVED"
                val isStockOut = tx.transactionType == "STOCK_OUT" || tx.transactionType == "POS_SALE"
                val txColor = when {
                    isStockIn -> NawanoEmerald
                    isStockOut -> NawanoRose
                    else -> NawanoPrimary200
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = txColor.copy(alpha = 0.14f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isStockIn) Icons.Default.ArrowDownward
                                        else if (isStockOut) Icons.Default.ArrowUpward
                                        else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = txColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = tx.productName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.sku} • ${tx.note ?: tx.transactionType}",
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = if (tx.quantityDelta > 0) "+${tx.quantityDelta}" else "${tx.quantityDelta}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = txColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = iconTint.copy(alpha = 0.14f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 12.5.sp,
            color = textSecondary
        )
    }
}
