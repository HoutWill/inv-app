package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import java.util.Locale

/**
 * Purchase Order Card for the Supplier Portal, matching the Behance "Supply Chain Mastery" design.
 * Features:
 * - Top Row: Status Tag ("Goods Inbound") on left, primary "Accept" pill + "Reject" text on right
 * - Middle Row: Bold title ("0603 - Items lobster in from...") & "Est. arrival time: 09:30 - 11:30 AM"
 * - Divider line
 * - Bottom Row: Circular avatar & Company Name + PIC Sender on left, Total Weight/Value on right
 */
@Composable
fun SupplierDashboardCard(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onAccept: () -> Unit,
    onReject: () -> Unit = {},
    onSend: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val dividerColor = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    val (statusLabel, statusBg, statusFg) = if (isDarkTheme) {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Goods Inbound", NawanoPrimary200.copy(alpha = 0.18f), NawanoPrimary200)
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFF6366F1).copy(alpha = 0.18f), Color(0xFFC084FC))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFF38BDF8).copy(alpha = 0.18f), Color(0xFF38BDF8))
            PurchaseOrderStatus.RECEIVED -> Triple("Completed", Color(0xFF10B981).copy(alpha = 0.18f), Color(0xFF34D399))
            PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFEF4444).copy(alpha = 0.18f), Color(0xFFF87171))
            PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFF6B7280).copy(alpha = 0.18f), Color(0xFF9CA3AF))
        }
    } else {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Goods Inbound", Color(0xFFF3E8FF), Color(0xFF7E22CE))
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFFEDE9FE), Color(0xFF6D28D9))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFFE0F2FE), Color(0xFF0284C7))
            PurchaseOrderStatus.RECEIVED -> Triple("Completed", Color(0xFFD1FAE5), Color(0xFF047857))
            PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFFEE2E2), Color(0xFFDC2626))
            PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFFF3F4F6), Color(0xFF4B5563))
        }
    }

    Surface(
        onClick = onView,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Top Row: Status Pill ("Goods Inbound") on Left, Actions ("Accept" & "Reject") on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Status Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                // Action Controls on Right
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    when (order.status) {
                        PurchaseOrderStatus.PENDING -> {
                            // "Accept" Primary Pill Button
                            Surface(
                                onClick = onAccept,
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.5.dp, NawanoPrimary200)
                            ) {
                                Text(
                                    text = "Accept",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }

                            // "Reject" Text Button right below Accept
                            Text(
                                text = "Reject",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
                                modifier = Modifier
                                    .clickable { onReject() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        PurchaseOrderStatus.CONFIRMED -> {
                            // "Dispatch" Action Button
                            Surface(
                                onClick = onSend,
                                shape = RoundedCornerShape(12.dp),
                                color = NawanoPrimary200
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Dispatch",
                                        fontSize = 12.sp,
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
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFBAE6FD))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "In Transit",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                onClick = onView,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = "View Details",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Middle Row: Product Thumbnail + Bold Title & Estimated Arrival Subtitle
            val displayTitle = if (order.title.isNotBlank()) {
                if (order.title.startsWith(order.orderNumber, ignoreCase = true)) {
                    order.title
                } else {
                    "${order.orderNumber} - ${order.title}"
                }
            } else {
                "${order.orderNumber} - Items inbound from supplier"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProductThumbnail(
                    sku = order.orderNumber,
                    name = order.title,
                    category = null,
                    imageUri = null,
                    supplier = order.supplierName,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Est. arrival time: ${order.estArrival.ifBlank { "09:30 - 11:30 AM" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(10.dp))

            // 3. Bottom Row: Supplier Avatar & PIC Sender on left, Total Weight/Value on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular Logo Avatar + Supplier Name + PIC Sender
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val companyName = order.supplierName.ifBlank { "Foodie Supplier" }
                    val initials = companyName.split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .ifBlank { "FS" }

                    Surface(
                        shape = CircleShape,
                        color = NawanoPrimary200.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NawanoPrimary200
                            )
                        }
                    }

                    Column {
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "PIC Sender: ${order.picSender.ifBlank { "Brandon Vaccaro" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Total Weight / Items & Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Weight",
                        style = MaterialTheme.typography.labelSmall,
                        color = textSecondary
                    )
                    val displayWeight = when {
                        order.totalWeight.isNotBlank() -> order.totalWeight
                        order.totalQuantity > 0 -> "${order.totalQuantity} Box @ 5KG"
                        else -> "3 Box @ 5KG"
                    }
                    Text(
                        text = displayWeight,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                }
            }
        }
    }
}
