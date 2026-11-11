package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Modern Purchase Order Card for the Supplier Portal, matching the Behance design system.
 */
@Composable
fun SupplierDashboardCard(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onAccept: () -> Unit,
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
    val deleteBtnText = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)

    val (statusLabel, statusBg, statusFg) = if (isDarkTheme) {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Pending", Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFFBBF24))
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFFC084FC))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFF6366F1).copy(alpha = 0.15f), Color(0xFFC084FC))
            PurchaseOrderStatus.RECEIVED -> Triple("Completed", Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF34D399))
            PurchaseOrderStatus.REJECTED -> Triple("Rejected", Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFF87171))
            PurchaseOrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF9CA3AF))
        }
    } else {
        when (order.status) {
            PurchaseOrderStatus.PENDING -> Triple("Pending", Color(0xFFFEF3C7), Color(0xFFD97706))
            PurchaseOrderStatus.CONFIRMED -> Triple("Confirmed", Color(0xFFEDE9FE), Color(0xFF6D28D9))
            PurchaseOrderStatus.DELIVERED -> Triple("In Delivery", Color(0xFFE0E7FF), Color(0xFF4338CA))
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
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: PO Number, Status Pill, Terms Pill & Emerald Total Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "Net 30",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF0284C7),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "$${String.format(Locale.US, "%,.2f", order.totalAmount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Title & Qty
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.title.ifBlank { "Goods Inbound" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = "${order.totalQuantity} Qty",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 3: Tracking & Driver / Courier Line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = buildString {
                        if (order.trackingNumber.isNotBlank()) append(order.trackingNumber)
                        else append("TRK-${order.orderNumber.takeLast(6)}")
                        append(" • ")
                        append(order.picSender.ifBlank { "Leslie Alexander" })
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(8.dp))

            // Row 4: Est Arrival on Left + Action Buttons on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Est: ${order.estArrival.ifBlank { "Today" }}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (order.status) {
                        PurchaseOrderStatus.PENDING -> {
                            Surface(
                                onClick = onAccept,
                                shape = RoundedCornerShape(12.dp),
                                color = NawanoPrimary200
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Accept",
                                        style = MaterialTheme.typography.labelSmall,
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
                                color = Color(0xFF4338CA)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Dispatch",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                onClick = onView,
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0xFF2A2E63) else Color(0xFFEEF2FF)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color.White else Color(0xFF4338CA),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "View",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else Color(0xFF4338CA)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = deleteBtnText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty placeholder state card when no orders or catalog items match.
 */
@Composable
fun EmptyZeroState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NawanoPrimary200.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }
}
