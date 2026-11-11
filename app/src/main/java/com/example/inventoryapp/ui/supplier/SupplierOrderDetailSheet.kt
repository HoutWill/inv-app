package com.example.inventoryapp.ui.supplier

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Detailed Order Breakdown, Manifest & Packing Slip Bottom Sheet.
 * Displays PO line items with interactive verification checklist, barcode tracking, and dispatch controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrderDetailSheet(
    order: PurchaseOrderEntity,
    orderWithItems: OrderWithItems?,
    isDarkTheme: Boolean = false,
    packedItemSku: String? = null,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onAccept: () -> Unit,
    onRequestScanToPack: () -> Unit = {}
) {
    val context = LocalContext.current
    val items = orderWithItems?.items ?: emptyList()

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
    val itemCardBg = if (isDarkTheme) NawanoDark400 else Color(0xFFF8FAFC)
    val itemCardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val itemCheckedBg = if (isDarkTheme) Color(0xFF0F2E22) else Color(0xFFD1FAE5)

    // Interactive warehouse packing checklist
    val packedItemsMap = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(packedItemSku) {
        if (!packedItemSku.isNullOrBlank()) {
            val matched = items.find { it.sku.equals(packedItemSku, ignoreCase = true) }
            if (matched != null) {
                packedItemsMap[matched.id] = true
                Toast.makeText(context, "✓ Packed item: ${matched.productName}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Scanned barcode '$packedItemSku' is not in this PO", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: PO Number + Copy + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = order.orderNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy PO",
                        tint = NawanoPrimary200,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("PO Number", order.orderNumber))
                                Toast.makeText(context, "Copied ${order.orderNumber}", Toast.LENGTH_SHORT).show()
                            }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.2f) else Color(0xFFEDE9FE)
                ) {
                    Text(
                        text = order.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) NawanoPrimary200 else Color(0xFF6D28D9),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = order.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(Modifier.height(14.dp))

            // Logistics Route Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Carrier & Route", fontSize = 11.sp, color = textSecondary)
                    Text(
                        text = "${order.picSender.ifBlank { "Apex Logistics" }} -> Nawano HQ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Tracking Code", fontSize = 11.sp, color = textSecondary)
                    Text(
                        text = order.trackingNumber.ifBlank { "Pending Dispatch" },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF6366F1)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Packing Slip Line Items Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Packing Checklist (${if (items.isEmpty()) 1 else items.size} items)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Surface(
                    onClick = onRequestScanToPack,
                    shape = RoundedCornerShape(10.dp),
                    color = NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.25f else 0.12f),
                    border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan to Pack",
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Scan to Pack",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Line items list
            if (items.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = itemCardBg,
                    border = BorderStroke(1.dp, itemCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(order.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("${order.totalQuantity} Units", fontSize = 11.sp, color = textSecondary)
                        }
                        Text(
                            "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        val isPacked = packedItemsMap[item.id] ?: false
                        Surface(
                            onClick = { packedItemsMap[item.id] = !isPacked },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPacked) itemCheckedBg else itemCardBg,
                            border = BorderStroke(1.dp, if (isPacked) Color(0xFF10B981) else itemCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPacked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isPacked) Color(0xFF10B981) else textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "SKU: ${item.sku} • Qty: ${item.quantity} units",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.unitCost * item.quantity)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPacked) Color(0xFF047857) else textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Logistics Notes Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "FREIGHT & DISPATCH NOTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (order.supplierNote.isNotBlank()) order.supplierNote else "Direct cold-chain courier transport with temperature telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            when (order.status) {
                PurchaseOrderStatus.PENDING -> {
                    Button(
                        onClick = {
                            onAccept()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Accept & Confirm Order", fontWeight = FontWeight.Bold)
                    }
                }
                PurchaseOrderStatus.CONFIRMED -> {
                    Button(
                        onClick = {
                            onDismiss()
                            onSend()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Dispatch Outbound Freight", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close Manifest")
                    }
                }
            }
        }
    }
}
