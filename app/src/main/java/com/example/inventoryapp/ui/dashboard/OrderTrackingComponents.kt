package com.example.inventoryapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TrackingStatMetric(
    val label: String,
    val value: String,
    val weight: Float = 1f
)

/**
 * Shared gradient summary banner used by both Middleman and Supplier tracking pages.
 */
@Composable
fun TrackingSummaryBanner(
    title: String,
    badgeText: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    metrics: List<TrackingStatMetric>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.24f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    metrics.forEach { metric ->
                        Column(modifier = Modifier.weight(metric.weight)) {
                            Text(
                                text = metric.label,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = metric.value,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared search input and status filter pill row for tracking pages.
 */
@Composable
fun TrackingSearchAndFilters(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    filterTabs: List<String>,
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF191D2D) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 13.5.sp
                        ),
                        cursorBrush = SolidColor(NawanoPrimary200),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterTabs.forEachIndexed { index, label ->
                val isSelected = selectedFilterIndex == index
                Surface(
                    onClick = { onFilterSelected(index) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) NawanoPrimary200 else cardBg,
                    border = if (isSelected) null else BorderStroke(1.dp, cardBorder)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual 4-step milestone progress bar for Purchase Order logistics tracking.
 */
@Composable
fun OrderTrackingTimeline(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean,
    stepLabels: List<String> = listOf("Placed", "Confirmed", "In Transit", "Received"),
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    if (order.status == PurchaseOrderStatus.REJECTED || order.status == PurchaseOrderStatus.CANCELLED) {
        val isRejected = order.status == PurchaseOrderStatus.REJECTED
        val bg = if (isDarkTheme) Color(0xFF450A0A) else Color(0xFFFEE2E2)
        val fg = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bg,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isRejected) "Order Rejected by Supplier" else "Order Cancelled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
                    )
                    val reason = order.supplierNote.ifBlank { order.ownerNote }
                    if (reason.isNotBlank()) {
                        Text(
                            text = reason,
                            fontSize = 11.sp,
                            color = fg.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        return
    }

    val currentStep = when (order.status) {
        PurchaseOrderStatus.PENDING -> 0
        PurchaseOrderStatus.CONFIRMED -> 1
        PurchaseOrderStatus.DELIVERED -> 2
        PurchaseOrderStatus.RECEIVED -> 3
        else -> 0
    }

    val timestamps = listOf(
        order.createdAt,
        order.confirmedAt,
        order.deliveredAt,
        order.receivedAt
    )

    val activeColor = when (order.status) {
        PurchaseOrderStatus.RECEIVED -> Color(0xFF10B981)
        PurchaseOrderStatus.DELIVERED -> Color(0xFF3B82F6)
        else -> NawanoPrimary200
    }
    val inactiveTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stepLabels.forEachIndexed { index, _ ->
                val isCompleted = index <= currentStep
                val isCurrent = index == currentStep

                // Node Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isCurrent) 24.dp else 20.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) activeColor else inactiveTrackColor)
                ) {
                    if (index < currentStep || order.status == PurchaseOrderStatus.RECEIVED) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Connector Line
                if (index < stepLabels.lastIndex) {
                    val lineCompleted = index < currentStep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (lineCompleted) activeColor else inactiveTrackColor)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stepLabels.forEachIndexed { index, label ->
                val isCompleted = index <= currentStep
                val timeStr = timestamps.getOrNull(index)?.let { formatTrackingTime(it) }
                Column(
                    horizontalAlignment = when (index) {
                        0 -> Alignment.Start
                        stepLabels.lastIndex -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCompleted) textPrimary else textSecondary.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (timeStr != null) {
                        Text(
                            text = timeStr,
                            fontSize = 9.5.sp,
                            color = textSecondary.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared Tracking Card used on both Middleman and Supplier tracking pages.
 */
@Composable
fun OrderTrackingCard(
    order: PurchaseOrderEntity,
    partnerLabel: String,
    partnerName: String,
    stepLabels: List<String>,
    isDarkTheme: Boolean,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    primaryActionLabel: String? = null,
    primaryActionIcon: ImageVector? = null,
    primaryActionColor: Color = NawanoPrimary200,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val (statusLabel, statusBg, statusFg) = when (order.status) {
        PurchaseOrderStatus.PENDING -> Triple(
            "Pending",
            if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7),
            if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309)
        )
        PurchaseOrderStatus.CONFIRMED -> Triple(
            "Confirmed",
            if (isDarkTheme) Color(0xFF312E81) else Color(0xFFEDE9FE),
            if (isDarkTheme) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
        )
        PurchaseOrderStatus.DELIVERED -> Triple(
            "In Transit",
            if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
            if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        )
        PurchaseOrderStatus.RECEIVED -> Triple(
            "Received",
            if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
        )
        PurchaseOrderStatus.REJECTED -> Triple(
            "Rejected",
            if (isDarkTheme) Color(0xFF450A0A) else Color(0xFFFEE2E2),
            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
        )
        PurchaseOrderStatus.CANCELLED -> Triple(
            "Cancelled",
            if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
        )
    }

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Order Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NawanoPrimary200.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = order.orderNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "$partnerLabel: $partnerName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Order Title & Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.title.ifBlank { "Wholesale Purchase Order" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("${order.totalQuantity} units")
                            if (order.totalWeight.isNotBlank()) append(" • ${order.totalWeight}")
                            if (order.estArrival.isNotBlank()) append(" • ETA ${order.estArrival}")
                        },
                        fontSize = 11.5.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = CurrencyFormatter.format(order.totalAmount, currency, exchangeRate),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            Spacer(Modifier.height(14.dp))

            // Step-by-Step Progress Timeline
            OrderTrackingTimeline(
                order = order,
                isDarkTheme = isDarkTheme,
                stepLabels = stepLabels
            )

            // Carrier & Copyable Tracking Number Row (when available)
            if (order.trackingNumber.isNotBlank() || order.picSender.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF191D2D) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                if (order.picSender.isNotBlank()) {
                                    Text(
                                        text = order.picSender,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (order.trackingNumber.isNotBlank()) {
                                    Text(
                                        text = order.trackingNumber,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (order.trackingNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NawanoPrimary200.copy(alpha = 0.12f),
                                modifier = Modifier.clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tracking Number", order.trackingNumber))
                                    Toast.makeText(context, "Tracking # copied", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy Tracking Number",
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Copy",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NawanoPrimary200
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if ((primaryActionLabel != null && onPrimaryAction != null) ||
                (secondaryActionLabel != null && onSecondaryAction != null)
            ) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (secondaryActionLabel != null && onSecondaryAction != null) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, cardBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = secondaryActionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (primaryActionLabel != null && onPrimaryAction != null) {
                        Button(
                            onClick = onPrimaryAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryActionColor,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (primaryActionIcon != null) {
                                Icon(
                                    imageVector = primaryActionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = primaryActionLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTrackingTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.US)
    return sdf.format(Date(timestamp))
}
