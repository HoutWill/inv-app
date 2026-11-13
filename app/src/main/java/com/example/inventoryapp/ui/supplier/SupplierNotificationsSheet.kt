package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*

/**
 * Supplier Notifications Modal Bottom Sheet, mirroring Middleman StockNotificationsDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierNotificationsSheet(
    orders: List<PurchaseOrderEntity>,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val pendingOrders = orders.filter { it.status == PurchaseOrderStatus.PENDING }
    val deliveredOrders = orders.filter { it.status == PurchaseOrderStatus.DELIVERED }
    val receivedOrders = orders.filter { it.status == PurchaseOrderStatus.RECEIVED }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Supplier Alerts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NawanoPrimary200.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${pendingOrders.size} Actionable",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Real-time purchase order updates, receipt acknowledgements, and HQ requests.",
                fontSize = 12.sp,
                color = textSecondary
            )

            Spacer(Modifier.height(4.dp))

            if (pendingOrders.isEmpty() && deliveredOrders.isEmpty() && receivedOrders.isEmpty()) {
                EmptyZeroState(
                    title = "No New Alerts",
                    subtitle = "All orders are up to date.",
                    icon = Icons.Default.NotificationsNone,
                    isDarkTheme = isDarkTheme
                )
            }

            // Pending Orders Alerts
            pendingOrders.forEach { order ->
                NotificationItemRow(
                    title = "New PO: ${order.orderNumber}",
                    subtitle = "${order.title} (${order.totalQuantity} pcs • $${String.format("%.2f", order.totalAmount)})",
                    time = "Action Required",
                    icon = Icons.Default.AddShoppingCart,
                    isDarkTheme = isDarkTheme
                )
            }

            // Delivered Orders
            deliveredOrders.forEach { order ->
                NotificationItemRow(
                    title = "Freight In Transit: ${order.orderNumber}",
                    subtitle = "Courier ${order.picSender} tracking ${order.trackingNumber}",
                    time = "En Route",
                    icon = Icons.Default.LocalShipping,
                    isDarkTheme = isDarkTheme
                )
            }

            // Received Orders
            receivedOrders.take(3).forEach { order ->
                NotificationItemRow(
                    title = "Stock Confirmed: ${order.orderNumber}",
                    subtitle = "Middleman HQ received & inspected stock into warehouse.",
                    time = "Completed",
                    icon = Icons.Default.CheckCircle,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
fun NotificationItemRow(
    title: String,
    subtitle: String,
    time: String,
    icon: ImageVector,
    isDarkTheme: Boolean
) {
    val cardBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = NawanoPrimary200.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
        }
    }
}
