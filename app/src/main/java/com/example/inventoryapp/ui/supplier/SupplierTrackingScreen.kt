package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.dashboard.OrderTrackingCard
import com.example.inventoryapp.ui.dashboard.TrackingSearchAndFilters
import com.example.inventoryapp.ui.dashboard.TrackingStatMetric
import com.example.inventoryapp.ui.dashboard.TrackingSummaryBanner
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.util.Locale

/**
 * Supplier Outbound Freight & Order Tracking Screen.
 * Tracks every middleman order across Ordered -> Confirmed -> Dispatched -> Received milestones.
 */
@Composable
fun SupplierTrackingScreen(
    orders: List<PurchaseOrderEntity>,
    isDarkTheme: Boolean = false,
    onViewOrder: (PurchaseOrderEntity) -> Unit = {},
    onConfirmOrder: (PurchaseOrderEntity) -> Unit = {},
    onDispatchOrder: (PurchaseOrderEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "In Transit", "Ready to Ship", "Pending", "Received")

    val inTransitOrders = remember(orders) { orders.filter { it.status == PurchaseOrderStatus.DELIVERED } }
    val readyToShipOrders = remember(orders) { orders.filter { it.status == PurchaseOrderStatus.CONFIRMED } }
    val totalEnRouteValue = remember(inTransitOrders) { inTransitOrders.sumOf { it.totalAmount } }

    val filteredOrders = remember(orders, searchQuery, selectedFilterIndex) {
        orders.filter { order ->
            val matchesStatus = when (selectedFilterIndex) {
                1 -> order.status == PurchaseOrderStatus.DELIVERED
                2 -> order.status == PurchaseOrderStatus.CONFIRMED
                3 -> order.status == PurchaseOrderStatus.PENDING
                4 -> order.status == PurchaseOrderStatus.RECEIVED
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    order.trackingNumber.contains(searchQuery, ignoreCase = true) ||
                    order.title.contains(searchQuery, ignoreCase = true) ||
                    order.picSender.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesQuery
        }.sortedByDescending { it.updatedAt }
    }

    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        // Hero Logistics Status Banner
        item {
            TrackingSummaryBanner(
                title = "Supplier Dispatch Tracker",
                badgeText = "${inTransitOrders.size} Live En Route",
                icon = Icons.Default.LocalShipping,
                gradientColors = listOf(
                    Color(0xFF3B82F6),
                    NawanoPrimary200,
                    Color(0xFF6366F1)
                ),
                metrics = listOf(
                    TrackingStatMetric("In Transit", "${inTransitOrders.size}"),
                    TrackingStatMetric("Ready", "${readyToShipOrders.size}"),
                    TrackingStatMetric(
                        "En-Route Value",
                        "$${String.format(Locale.US, "%,.0f", totalEnRouteValue)}",
                        weight = 1.3f
                    )
                ),
                isDarkTheme = isDarkTheme
            )
        }

        // Search & Filter Pills
        item {
            TrackingSearchAndFilters(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                searchPlaceholder = "Search PO #, tracking #, or carrier...",
                filterTabs = filterTabs,
                selectedFilterIndex = selectedFilterIndex,
                onFilterSelected = { selectedFilterIndex = it },
                isDarkTheme = isDarkTheme
            )
        }

        // Section Title
        item {
            Text(
                text = "Outbound Shipments (${filteredOrders.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        if (filteredOrders.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isDarkTheme) NawanoDark200 else Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = NawanoPrimary200.copy(alpha = 0.55f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "No matching shipments",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Confirm purchase orders to schedule carrier dispatch and tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredOrders, key = { it.id }) { order ->
                OrderTrackingCard(
                    order = order,
                    partnerLabel = "Middleman",
                    partnerName = "Nawano HQ",
                    stepLabels = listOf("Ordered", "Confirmed", "Dispatched", "Received"),
                    isDarkTheme = isDarkTheme,
                    currency = AppCurrency.USD,
                    exchangeRate = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
                    primaryActionLabel = when (order.status) {
                        PurchaseOrderStatus.PENDING -> "Confirm Order"
                        PurchaseOrderStatus.CONFIRMED -> "Dispatch & Track"
                        else -> "View Details"
                    },
                    primaryActionIcon = when (order.status) {
                        PurchaseOrderStatus.PENDING -> Icons.Default.CheckCircle
                        PurchaseOrderStatus.CONFIRMED -> Icons.Default.LocalShipping
                        else -> null
                    },
                    primaryActionColor = when (order.status) {
                        PurchaseOrderStatus.CONFIRMED -> Color(0xFF3B82F6)
                        else -> NawanoPrimary200
                    },
                    onPrimaryAction = {
                        when (order.status) {
                            PurchaseOrderStatus.PENDING -> onConfirmOrder(order)
                            PurchaseOrderStatus.CONFIRMED -> onDispatchOrder(order)
                            else -> onViewOrder(order)
                        }
                    },
                    secondaryActionLabel = if (order.status == PurchaseOrderStatus.PENDING || order.status == PurchaseOrderStatus.CONFIRMED) {
                        "Details"
                    } else null,
                    onSecondaryAction = if (order.status == PurchaseOrderStatus.PENDING || order.status == PurchaseOrderStatus.CONFIRMED) {
                        { onViewOrder(order) }
                    } else null,
                    onCardClick = { onViewOrder(order) }
                )
            }
        }
    }
}
