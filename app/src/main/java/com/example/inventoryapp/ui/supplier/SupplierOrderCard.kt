package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.ProductThumbnail

/**
 * Minimal 3-Column Item Card Layout for Supplier Portal:
 * - Left Column: Square rounded product/brand thumbnail (48x48px, border-radius: 12px)
 * - Middle Column (Flex-grow): Line 1 (Clean Title), Line 2 (#PO-XXXX • Customer/Supplier • ETA)
 * - Right Column (Right-aligned stack):
 *     - Top: Status badge pill (Incoming / Confirmed / In Delivery / Completed)
 *     - Middle: Bold quantity delta in emerald green (+50, +90, +120)
 *     - Bottom: State-specific compact pill button (Accept / Dispatch / In Transit / Done)
 */
@Composable
fun SupplierDashboardCard(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onAccept: () -> Unit,
    onReject: () -> Unit = {},
    onSend: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDarkCard else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val cleanTitle = remember(order.title, order.supplierName) {
        cleanItemTitle(order.title.ifBlank { "Items from ${order.supplierName}" })
    }

    val shortPo = remember(order.orderNumber) { formatShortPoCode(order.orderNumber) }
    val partnerName = remember(order.supplierName) {
        cleanSupplierName(order.supplierName.ifBlank { "Nawano HQ" })
    }
    val eta = remember(order.estArrival) {
        order.estArrival.ifBlank { "In 2 days" }
    }
    val metadataLine = "$shortPo • $partnerName • $eta"

    // 1. Status badge pill styling (consistent across all cards)
    val (statusLabel, statusBg, statusFg) = when (order.status) {
        PurchaseOrderStatus.PENDING -> Triple(
            "Incoming",
            if (isDarkTheme) Color(0xFF78350F).copy(alpha = 0.45f) else Color(0xFFFEF3C7),
            if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
        )
        PurchaseOrderStatus.CONFIRMED -> Triple(
            "Confirmed",
            if (isDarkTheme) Color(0xFF312E81).copy(alpha = 0.45f) else Color(0xFFE0E7FF),
            if (isDarkTheme) Color(0xFF818CF8) else Color(0xFF4338CA)
        )
        PurchaseOrderStatus.DELIVERED -> Triple(
            "In Delivery",
            if (isDarkTheme) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFFE0F2FE),
            if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
        )
        PurchaseOrderStatus.RECEIVED -> Triple(
            "Completed",
            if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFD1FAE5),
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
        )
        PurchaseOrderStatus.REJECTED -> Triple(
            "Rejected",
            if (isDarkTheme) Color(0xFF7F1D1D).copy(alpha = 0.45f) else Color(0xFFFEE2E2),
            if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
        )
        PurchaseOrderStatus.CANCELLED -> Triple(
            "Cancelled",
            if (isDarkTheme) Color(0xFF374151).copy(alpha = 0.45f) else Color(0xFFF3F4F6),
            if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF4B5563)
        )
    }

    val deltaQty = if (order.totalQuantity > 0) order.totalQuantity else 10
    val quantityDeltaText = "+$deltaQty"

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Square rounded product/brand thumbnail (48x48px, border-radius: 12px)
            ProductThumbnail(
                sku = order.orderNumber,
                name = cleanTitle,
                category = null,
                imageUri = null,
                supplier = order.supplierName,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Middle Column (Flex-grow): Clean title + single muted metadata subtext line
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = metadataLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.5.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Column (Right-aligned stack: Status Pill -> Quantity Delta -> Action Button)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Top: Status badge pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                // Middle: Bold quantity delta in emerald green
                Text(
                    text = quantityDeltaText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = Color(0xFF10B981)
                )

                // Bottom: State-specific compact pill button
                when (order.status) {
                    PurchaseOrderStatus.PENDING -> {
                        Surface(
                            onClick = onAccept,
                            shape = RoundedCornerShape(12.dp),
                            color = NawanoPrimary200,
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Accept",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    PurchaseOrderStatus.CONFIRMED -> {
                        Surface(
                            onClick = onSend,
                            shape = RoundedCornerShape(12.dp),
                            color = NawanoPrimary200,
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Dispatch",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    PurchaseOrderStatus.DELIVERED -> {
                        Surface(
                            onClick = onView,
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFBAE6FD)),
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "In Transit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                    PurchaseOrderStatus.RECEIVED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                    else -> {
                        Surface(
                            onClick = onView,
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier.defaultMinSize(minWidth = 64.dp, minHeight = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
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

private fun cleanItemTitle(rawTitle: String): String {
    var title = rawTitle.trim()
    if (title.contains("•")) {
        title = title.substringAfterLast("•").trim()
    }
    title = title.replace(Regex("^PO-\\d{4}-\\d+\\s*-\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^PO-\\d+\\s*-\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^(Inbound Request|Cold-Chain Restock|Gourmet Pantry Inbound)\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^Stock (In|Out):\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("\\s+Restock$", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("\\s+Inbound$", RegexOption.IGNORE_CASE), "")
    return title.trim().ifBlank { rawTitle.trim() }
}

private fun formatShortPoCode(orderNumber: String): String {
    val clean = orderNumber.trim()
    val match = Regex("PO-\\d{4}-(\\d+)").find(clean)
    return if (match != null) {
        "#PO-${match.groupValues[1]}"
    } else if (clean.startsWith("#")) {
        clean
    } else if (clean.isNotBlank()) {
        "#$clean"
    } else {
        "#PO-REF"
    }
}

private fun cleanSupplierName(rawSupplier: String): String {
    return rawSupplier
        .replace(Regex("\\s+Supply$", RegexOption.IGNORE_CASE), "")
        .trim()
        .ifBlank { rawSupplier.trim() }
}
