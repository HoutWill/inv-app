package com.example.inventoryapp.ui.supplier

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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrdersScreen(
    user: UserEntity,
    orders: List<PurchaseOrderEntity> = emptyList(),
    onConfirmOrder: (String, String?) -> Unit = { _, _ -> },
    onRejectOrder: (String, String) -> Unit = { _, _ -> },
    onMarkDelivered: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All Orders", "Pending", "Confirmed", "In Delivery", "Received")

    var showRejectDialogForId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    val filteredOrders = remember(orders, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> orders.filter { it.status == PurchaseOrderStatus.PENDING }
            2 -> orders.filter { it.status == PurchaseOrderStatus.CONFIRMED }
            3 -> orders.filter { it.status == PurchaseOrderStatus.DELIVERED }
            4 -> orders.filter { it.status == PurchaseOrderStatus.RECEIVED }
            else -> orders
        }
    }

    Scaffold(
        containerColor = NawanoLightSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Supplier Portal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoTextPrimaryLight
                        )
                        Text(
                            text = user.fullName,
                            fontSize = 12.sp,
                            color = NawanoTextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = NawanoPrimary200
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NawanoLightSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEachIndexed { index, tabName ->
                    val isSelected = selectedFilterIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilterIndex = index },
                        label = {
                            Text(
                                text = tabName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NawanoPrimary200,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = NawanoPrimary200.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No Orders in this category",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoTextPrimaryLight
                        )
                        Text(
                            text = "Incoming orders from Nawano store will appear here.",
                            fontSize = 13.sp,
                            color = NawanoTextSecondaryLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        SupplierOrderCard(
                            order = order,
                            onConfirm = {
                                onConfirmOrder(order.id, "Order confirmed by supplier")
                                Toast.makeText(context, "Order ${order.orderNumber} confirmed!", Toast.LENGTH_SHORT).show()
                            },
                            onReject = {
                                showRejectDialogForId = order.id
                            },
                            onDispatch = {
                                onMarkDelivered(order.id)
                                Toast.makeText(context, "Order ${order.orderNumber} marked In Delivery!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRejectDialogForId != null) {
        val orderId = showRejectDialogForId!!
        AlertDialog(
            onDismissRequest = { showRejectDialogForId = null },
            title = { Text(text = "Reject Order", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Provide reason to inform the store owner:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("Out of stock / Production delay") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectOrder(orderId, rejectReason.ifBlank { "Supplier unable to fulfill" })
                        showRejectDialogForId = null
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reject", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialogForId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SupplierOrderCard(
    order: PurchaseOrderEntity,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onDispatch: () -> Unit
) {
    val (statusLabel, statusBg, statusFg) = when (order.status) {
        PurchaseOrderStatus.PENDING -> Triple("Pending Review", Color(0xFFFEF3C7), Color(0xFFB45309))
        PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFFEDE9FE), NawanoPrimary200)
        PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFFE0E7FF), Color(0xFF4338CA))
        PurchaseOrderStatus.RECEIVED -> Triple("Received & Completed", Color(0xFFD1FAE5), Color(0xFF047857))
        PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFFEE2E2), Color(0xFFDC2626))
        PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    val dateFormatted = remember(order.createdAt) {
        SimpleDateFormat("EEE, d MMM • hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: PO Number + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.orderNumber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoTextPrimaryLight
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = order.title.ifBlank { "Inbound Goods Request" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NawanoTextPrimaryLight
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "$dateFormatted • Est: ${order.estArrival}",
                fontSize = 12.sp,
                color = NawanoTextSecondaryLight
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF8FAFC))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.totalQuantity} items • Total: $${String.format(Locale.US, "%.2f", order.totalAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )

                // Actions based on state
                when (order.status) {
                    PurchaseOrderStatus.PENDING -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onReject,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Reject", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    PurchaseOrderStatus.CONFIRMED -> {
                        Button(
                            onClick = onDispatch,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Dispatch (In Delivery)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    PurchaseOrderStatus.DELIVERED -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE0E7FF)
                        ) {
                            Text(
                                text = "🚚 Dispatched • Awaiting Store Receipt",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4338CA),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    PurchaseOrderStatus.RECEIVED -> {
                        Text(
                            text = "✓ Stocked by Store Owner",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
