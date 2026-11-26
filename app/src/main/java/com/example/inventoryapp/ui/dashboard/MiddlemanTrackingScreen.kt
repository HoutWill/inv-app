package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter

/**
 * Middleman Inbound Logistics & Purchase Order Tracking Screen.
 * Tracks every supplier order across Placed -> Confirmed -> In Transit -> Received milestones.
 */
@Composable
fun MiddlemanTrackingScreen(
    orders: List<PurchaseOrderEntity>,
    onBack: () -> Unit,
    onScanClicked: () -> Unit,
    onOrderClick: (String) -> Unit,
    onReceiveOrder: (String) -> Unit,
    onReportIssue: (PurchaseOrderEntity) -> Unit,
    isDarkTheme: Boolean = false,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "In Transit", "Pending", "Confirmed", "Received")

    val inTransitCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.DELIVERED } }
    val processingCount = remember(orders) {
        orders.count { it.status == PurchaseOrderStatus.PENDING || it.status == PurchaseOrderStatus.CONFIRMED }
    }
    val receivedCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.RECEIVED } }

    val filteredOrders = remember(orders, searchQuery, selectedFilterIndex) {
        orders.filter { order ->
            val matchesStatus = when (selectedFilterIndex) {
                1 -> order.status == PurchaseOrderStatus.DELIVERED
                2 -> order.status == PurchaseOrderStatus.PENDING
                3 -> order.status == PurchaseOrderStatus.CONFIRMED
                4 -> order.status == PurchaseOrderStatus.RECEIVED
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    order.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    order.trackingNumber.contains(searchQuery, ignoreCase = true) ||
                    order.supplierName.contains(searchQuery, ignoreCase = true) ||
                    order.title.contains(searchQuery, ignoreCase = true) ||
                    order.picSender.contains(searchQuery, ignoreCase = true)

            matchesStatus && matchesQuery
        }.sortedByDescending { it.updatedAt }
    }

    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Order Tracking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Middleman Inbound Logistics",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onScanClicked,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan Tracking Barcode",
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp)
        ) {
            // Hero Logistics Banner
            item {
                TrackingSummaryBanner(
                    title = "Inbound Shipment Tracker",
                    badgeText = "$inTransitCount En Route",
                    icon = Icons.Default.LocalShipping,
                    gradientColors = listOf(
                        NawanoGradientStart,
                        NawanoPrimary200,
                        NawanoGradientEnd
                    ),
                    metrics = listOf(
                        TrackingStatMetric("In Transit", "$inTransitCount"),
                        TrackingStatMetric("Processing", "$processingCount"),
                        TrackingStatMetric("Received", "$receivedCount")
                    ),
                    isDarkTheme = isDarkTheme
                )
            }

            // Search & Filter Pills
            item {
                TrackingSearchAndFilters(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchPlaceholder = "Search PO #, tracking #, or supplier...",
                    filterTabs = filterTabs,
                    selectedFilterIndex = selectedFilterIndex,
                    onFilterSelected = { selectedFilterIndex = it },
                    isDarkTheme = isDarkTheme
                )
            }

            // Section Title
            item {
                Text(
                    text = "Tracked Orders (${filteredOrders.size})",
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
                                text = "No matching tracked orders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search or status filter.",
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
                        partnerLabel = "Supplier",
                        partnerName = order.supplierName.ifBlank { "Wholesale Supplier" },
                        stepLabels = listOf("Placed", "Confirmed", "In Transit", "Received"),
                        isDarkTheme = isDarkTheme,
                        currency = currency,
                        exchangeRate = exchangeRate,
                        primaryActionLabel = when (order.status) {
                            PurchaseOrderStatus.DELIVERED -> "Confirm Receipt"
                            else -> "View Details"
                        },
                        primaryActionIcon = when (order.status) {
                            PurchaseOrderStatus.DELIVERED -> Icons.Default.Inventory
                            else -> null
                        },
                        primaryActionColor = when (order.status) {
                            PurchaseOrderStatus.DELIVERED -> Color(0xFF10B981)
                            else -> NawanoPrimary200
                        },
                        onPrimaryAction = {
                            if (order.status == PurchaseOrderStatus.DELIVERED) {
                                onReceiveOrder(order.id)
                            } else {
                                onOrderClick(order.id)
                            }
                        },
                        secondaryActionLabel = if (order.status == PurchaseOrderStatus.DELIVERED || order.status == PurchaseOrderStatus.RECEIVED) {
                            "Report Issue"
                        } else null,
                        onSecondaryAction = if (order.status == PurchaseOrderStatus.DELIVERED || order.status == PurchaseOrderStatus.RECEIVED) {
                            { onReportIssue(order) }
                        } else null,
                        onCardClick = { onOrderClick(order.id) }
                    )
                }
            }
        }
    }
}
