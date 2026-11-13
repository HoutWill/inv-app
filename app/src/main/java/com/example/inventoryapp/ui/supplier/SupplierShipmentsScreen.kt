package com.example.inventoryapp.ui.supplier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Active Outbound Freight & In-Transit Shipments Screen for the Supplier.
 * Mirrors the Middleman MovementListScreen with live logistics monitoring.
 */
@Composable
fun SupplierShipmentsScreen(
    orders: List<PurchaseOrderEntity>,
    isDarkTheme: Boolean = false,
    onViewOrder: (PurchaseOrderEntity) -> Unit = {},
    onDispatchOrder: (PurchaseOrderEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedShipmentFilter by remember { mutableIntStateOf(0) }
    val shipmentFilterTabs = listOf("All Shipments", "In Transit", "Ready to Dispatch", "Received")

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val inTransitOrders = remember(orders) {
        orders.filter { it.status == PurchaseOrderStatus.DELIVERED }
    }
    val readyToDispatchOrders = remember(orders) {
        orders.filter { it.status == PurchaseOrderStatus.CONFIRMED }
    }
    val receivedOrders = remember(orders) {
        orders.filter { it.status == PurchaseOrderStatus.RECEIVED }
    }

    val filteredList = remember(orders, selectedShipmentFilter) {
        when (selectedShipmentFilter) {
            1 -> inTransitOrders
            2 -> readyToDispatchOrders
            3 -> receivedOrders
            else -> orders.filter {
                it.status == PurchaseOrderStatus.DELIVERED ||
                it.status == PurchaseOrderStatus.CONFIRMED ||
                it.status == PurchaseOrderStatus.RECEIVED
            }
        }
    }

    val totalEnRouteValue = remember(inTransitOrders) { inTransitOrders.sumOf { it.totalAmount } }
    val totalEnRouteQty = remember(inTransitOrders) { inTransitOrders.sumOf { it.totalQuantity } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Logistics Status Banner
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF3B82F6),
                                    NawanoPrimary200,
                                    Color(0xFF6366F1)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Freight Dispatch Monitor",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "${inTransitOrders.size} Live En Route",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("In Transit", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("${inTransitOrders.size}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cargo Units", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("$totalEnRouteQty", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text("En-Route Value", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("$${String.format(Locale.US, "%,.0f", totalEnRouteValue)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Filter Pills
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shipmentFilterTabs.forEachIndexed { index, name ->
                    val isSelected = selectedShipmentFilter == index
                    Surface(
                        onClick = { selectedShipmentFilter = index },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) NawanoPrimary200 else cardBg,
                        border = if (isSelected) null else BorderStroke(1.dp, cardBorder)
                    ) {
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Shipments Count
        item {
            Text(
                text = "Shipments (${filteredList.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        if (filteredList.isEmpty()) {
            item {
                EmptyZeroState(
                    title = "No Active Shipments",
                    subtitle = "Confirm purchase orders to schedule and dispatch carrier freight.",
                    icon = Icons.Default.LocalShipping,
                    isDarkTheme = isDarkTheme
                )
            }
        } else {
            items(filteredList, key = { it.id }) { order ->
                ShipmentCard(
                    order = order,
                    isDarkTheme = isDarkTheme,
                    onView = { onViewOrder(order) },
                    onDispatch = { onDispatchOrder(order) }
                )
            }
        }
    }
}

@Composable
private fun ShipmentCard(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean,
    onView: () -> Unit,
    onDispatch: () -> Unit
) {
    val context = LocalContext.current
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val isDelivering = order.status == PurchaseOrderStatus.DELIVERED
    val isReady = order.status == PurchaseOrderStatus.CONFIRMED

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Carrier Badge & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDelivering) Icons.Default.DirectionsTransit else Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = if (isDelivering) Color(0xFF3B82F6) else NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = order.picSender.ifBlank { "Apex Cold Freight" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isDelivering -> if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                        isReady -> if (isDarkTheme) Color(0xFF312E81) else Color(0xFFEDE9FE)
                        else -> if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                    }
                ) {
                    Text(
                        text = when {
                            isDelivering -> "🚚 In Transit"
                            isReady -> "📦 Pack & Dispatch"
                            else -> "✓ Delivered"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isDelivering -> if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                            isReady -> if (isDarkTheme) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
                            else -> if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Order title & manifest summary
            Text(
                text = "${order.orderNumber} • ${order.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            // ETA and Route
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ETA: ${order.estArrival.ifBlank { "Today" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NawanoPrimary200
                )
                Text(
                    text = "Gross Wt: ${order.totalWeight.ifBlank { "12.0 KG" }}",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            Spacer(Modifier.height(8.dp))

            // Tracking row & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        if (order.trackingNumber.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Tracking Number", order.trackingNumber))
                            Toast.makeText(context, "Copied tracking: ${order.trackingNumber}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy tracking",
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (order.trackingNumber.isNotBlank()) order.trackingNumber else "Assign Waybill",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textSecondary
                    )
                }

                if (isReady) {
                    Surface(
                        onClick = onDispatch,
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4338CA)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", order.totalAmount)} (${order.totalQuantity} pcs)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                }
            }
        }
    }
}
