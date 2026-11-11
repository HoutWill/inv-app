package com.example.inventoryapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Authentic Behance "Detail Movement" Screen (Image 1 Right Screen).
 * Displays order tracking, QR code, supplier details, goods items, and one-tap physical receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailScreen(
    order: PurchaseOrderEntity,
    items: List<PurchaseOrderItemEntity>,
    onBack: () -> Unit,
    onReceiveOrder: (String) -> Unit,
    onRejectOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRejectConfirmDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val formattedDate = remember(order.createdAt) {
        SimpleDateFormat("EEE, d MMM hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    }

    val (statusLabel, statusBg, statusFg) = when (order.status) {
        PurchaseOrderStatus.PENDING -> Triple("Pending Review", Color(0xFFFEF3C7), Color(0xFFB45309))
        PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFFEDE9FE), NawanoPrimary200)
        PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFFE0E7FF), Color(0xFF4338CA))
        PurchaseOrderStatus.RECEIVED -> Triple("Received & Stocked", Color(0xFFD1FAE5), Color(0xFF047857))
        PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFFEE2E2), Color(0xFFDC2626))
        PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Scaffold(
        containerColor = NawanoLightSurface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detail Movement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoTextPrimaryLight
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NawanoTextPrimaryLight
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NawanoLightSurface
                )
            )
        },
        bottomBar = {
            if (order.status == PurchaseOrderStatus.DELIVERED || order.status == PurchaseOrderStatus.CONFIRMED) {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRejectConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Text(text = "Reject", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                isProcessing = true
                                onReceiveOrder(order.id)
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NawanoPrimary200,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isProcessing) "Stocking..." else "Accept & Inbound",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Top Movement Card (Behance design)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Title & QR Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = order.title.ifBlank { "${order.orderNumber} - Inbound delivery" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoTextPrimaryLight,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(Modifier.width(16.dp))

                        // QR Code Graphic Mockup
                        MockQrCode(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(4.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(16.dp))

                    // Order ID with Copy
                    MovementInfoRow(
                        label = "Order ID",
                        valueContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Order ID", order.orderNumber))
                                    Toast.makeText(context, "Order ID copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = order.orderNumber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = NawanoTextPrimaryLight
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Supplier From with Red Logo Dot
                    MovementInfoRow(
                        label = "Supplier From",
                        valueContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = order.supplierName.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = order.supplierName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NawanoTextPrimaryLight
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Movement Type Badge
                    MovementInfoRow(
                        label = "Movement Type",
                        valueContent = {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3E8FF)
                            ) {
                                Text(
                                    text = order.movementType,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // PIC Sender
                    MovementInfoRow(
                        label = "PIC Sender",
                        valueContent = {
                            Text(
                                text = order.picSender.ifBlank { "Leslie Alexander" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = NawanoTextPrimaryLight
                            )
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Total Weight / Total Qty
                    MovementInfoRow(
                        label = "Total Weight",
                        valueContent = {
                            Text(
                                text = order.totalWeight.ifBlank { "${order.totalQuantity} Units" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NawanoTextPrimaryLight
                            )
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    // Created Date
                    MovementInfoRow(
                        label = "Created Date",
                        valueContent = {
                            Text(
                                text = formattedDate,
                                fontSize = 13.sp,
                                color = NawanoTextSecondaryLight
                            )
                        }
                    )
                }
            }

            // Inbound Items Manifest
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Shipment Manifest (${items.size} SKUs)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoTextPrimaryLight
                    )

                    Spacer(Modifier.height(12.dp))

                    if (items.isEmpty()) {
                        Text(
                            text = "No items detailed in manifest",
                            fontSize = 13.sp,
                            color = NawanoTextSecondaryLight
                        )
                    } else {
                        items.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NawanoTextPrimaryLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "SKU: ${item.sku} • $${String.format(Locale.US, "%.2f", item.unitCost)} / unit",
                                        fontSize = 12.sp,
                                        color = NawanoTextSecondaryLight
                                    )
                                }
                                Text(
                                    text = "+${item.quantity}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200
                                )
                            }
                            if (index < items.size - 1) {
                                HorizontalDivider(color = Color(0xFFF8FAFC))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Total Valuation", fontSize = 14.sp, color = NawanoTextSecondaryLight)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", order.totalAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }

            // Notes Section
            if (order.ownerNote.isNotBlank() || order.supplierNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (order.ownerNote.isNotBlank()) {
                            Text(text = "Owner Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NawanoTextSecondaryLight)
                            Text(text = order.ownerNote, fontSize = 13.sp, color = NawanoTextPrimaryLight)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (order.supplierNote.isNotBlank()) {
                            Text(text = "Supplier Dispatch Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NawanoTextSecondaryLight)
                            Text(text = order.supplierNote, fontSize = 13.sp, color = NawanoTextPrimaryLight)
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (showRejectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRejectConfirmDialog = false },
            title = { Text(text = "Reject Inbound Delivery", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = "Please enter reason for rejecting this inbound shipment:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        placeholder = { Text("Damaged goods / wrong manifest / expired") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectConfirmDialog = false
                        onRejectOrder(order.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reject Shipment", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MovementInfoRow(
    label: String,
    valueContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = NawanoTextSecondaryLight
        )
        valueContent()
    }
}

/**
 * QR Code Canvas mockup matching Behance card style.
 */
@Composable
private fun MockQrCode(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cellW = w / 7f
        val cellH = h / 7f

        // Draw Corner Finder Patterns
        // Top-left finder
        drawRect(Color.Black, Offset(0f, 0f), Size(cellW * 3, cellH * 3))
        drawRect(Color.White, Offset(cellW, cellH), Size(cellW, cellH))

        // Top-right finder
        drawRect(Color.Black, Offset(cellW * 4, 0f), Size(cellW * 3, cellH * 3))
        drawRect(Color.White, Offset(cellW * 5, cellH), Size(cellW, cellH))

        // Bottom-left finder
        drawRect(Color.Black, Offset(0f, cellH * 4), Size(cellW * 3, cellH * 3))
        drawRect(Color.White, Offset(cellW, cellH * 5), Size(cellW, cellH))

        // Data dots
        drawRect(Color.Black, Offset(cellW * 3, cellH * 3), Size(cellW, cellH))
        drawRect(Color.Black, Offset(cellW * 4, cellH * 4), Size(cellW, cellH))
        drawRect(Color.Black, Offset(cellW * 5, cellH * 3), Size(cellW, cellH))
        drawRect(Color.Black, Offset(cellW * 3, cellH * 5), Size(cellW, cellH))
        drawRect(Color.Black, Offset(cellW * 4, cellH * 6), Size(cellW, cellH))
    }
}
