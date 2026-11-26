package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosUnpaidTabsBottomSheet(
    unpaidOrders: List<SalesOrderEntity>,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    currency: AppCurrency = AppCurrency.USD,
    onDismiss: () -> Unit,
    onSettleOrder: (orderId: String, method: PosPaymentMethod) -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val totalDebtUsd = unpaidOrders.sumOf { it.remainingAmountUsd }
    val totalDebtKhr = (totalDebtUsd * exchangeRate).toLong()
    val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(totalDebtKhr)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Unpaid Customer Tabs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${unpaidOrders.size} pending collections",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalDebtUsd)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                        Text(
                            text = "≈ $khrFormatted ៛",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (unpaidOrders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("All Tabs Cleared!", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("No outstanding customer debts found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(unpaidOrders, key = { it.id }) { tab ->
                        UnpaidTabCard(
                            order = tab,
                            exchangeRate = exchangeRate,
                            onSettle = { method ->
                                onSettleOrder(tab.id, method)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnpaidTabCard(
    order: SalesOrderEntity,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onSettle: (PosPaymentMethod) -> Unit
) {
    val now = System.currentTimeMillis()
    val isOverdue = order.dueDate != null && order.dueDate < now
    val daysRemaining = if (order.dueDate != null) {
        val diff = order.dueDate - now
        (diff / (86_400_000L)).toInt()
    } else null

    val khrAmount = (order.remainingAmountUsd * exchangeRate).toLong()
    val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(khrAmount)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.4f) else NawanoPrimary200.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Customer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (order.customerPhone.isNotBlank()) {
                        Text(
                            text = order.customerPhone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Balance due
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", order.remainingAmountUsd)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isOverdue) Color(0xFFEF4444) else NawanoPrimary200
                    )
                    Text(
                        text = "≈ $khrFormatted ៛",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Items summary
            if (order.itemsSummary.isNotBlank()) {
                Text(
                    text = order.itemsSummary,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(10.dp))

            // Due Status & Settle Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due date badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = when {
                            isOverdue -> "Overdue"
                            daysRemaining != null && daysRemaining <= 0 -> "Due Today"
                            daysRemaining != null -> "Due in $daysRemaining days"
                            else -> "No due date"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) Color(0xFFEF4444) else Color(0xFFD97706),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Quick Settle Actions
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onSettle(PosPaymentMethod.ABA_KHQR) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("KHQR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSettle(PosPaymentMethod.CASH) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Settle Cash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
