package com.example.inventoryapp.ui.supplier

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

enum class SupplierAnalyticsDateFilter(val label: String) {
    THIS_MONTH("This Month"),
    THIS_WEEK("This Week"),
    TODAY("Today"),
    ALL_TIME("All Time")
}

/**
 * Supplier Analytics & KPI Performance Screen, matching the visual depth,
 * interactive bar charts, target goal progress, and metrics of Middleman StatisticsScreen.
 */
@Composable
fun SupplierAnalyticsScreen(
    orders: List<PurchaseOrderEntity>,
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var selectedFilter by remember { mutableStateOf(SupplierAnalyticsDateFilter.THIS_MONTH) }
    var filterDropdownExpanded by remember { mutableStateOf(false) }

    val totalValue = orders.sumOf { it.totalAmount }
    val totalQty = orders.sumOf { it.totalQuantity }
    val pendingCount = orders.count { it.status == PurchaseOrderStatus.PENDING }
    val inTransitCount = orders.count { it.status == PurchaseOrderStatus.DELIVERED }
    val completedCount = orders.count { it.status == PurchaseOrderStatus.RECEIVED }
    val rejectedCount = orders.count { it.status == PurchaseOrderStatus.REJECTED }

    val fulfillmentRate = if (orders.isEmpty()) 100.0 else {
        ((completedCount + inTransitCount).toDouble() / orders.size.toDouble()) * 100.0
    }

    val supplyTarget = 25000.0
    val targetProgress = (totalValue / supplyTarget).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
    ) {
        // 1. Date Filter & Dropdown Selector Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vendor Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Box {
                    Surface(
                        onClick = { filterDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        shadowElevation = if (isDarkTheme) 0.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedFilter.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = filterDropdownExpanded,
                        onDismissRequest = { filterDropdownExpanded = false }
                    ) {
                        SupplierAnalyticsDateFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.label) },
                                onClick = {
                                    selectedFilter = filter
                                    filterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Performance Card (Nawano Gradient with Target Progress)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NawanoGradientStart, Color(0xFF8F7FF8), NawanoGradientEnd),
                                start = Offset(0f, 0f),
                                end = Offset(1000f, 1000f)
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Wholesale Target",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "● Tier 1 Supplier",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
                        Spacer(Modifier.height(14.dp))

                        // Target Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Supply Target ($${String.format(Locale.US, "%,.0f", supplyTarget)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.0f%%", targetProgress * 100)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { targetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )

                        Spacer(Modifier.height(16.dp))

                        // 2x2 Metric Grid inside Hero Card
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fulfilled Rate", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", fulfillmentRate.coerceAtMost(100.0)),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("On-Time Dispatch", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                                Text("98.4%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. Weekly Fulfillment Canvas Bar Chart (Matching Middleman StatisticsScreen)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Supply Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Text(
                            text = "7-Day Outbound",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val mockVolumes = listOf(0.45f, 0.70f, 0.35f, 0.90f, 0.60f, 0.80f, 0.40f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { idx, day ->
                            val vol = mockVolumes[idx]
                            val isHighlighted = idx == 3 // Thursday highlight

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(vol)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(
                                            if (isHighlighted) NawanoPrimary200
                                            else if (isDarkTheme) Color(0xFF3B3363)
                                            else Color(0xFFE2E8F0)
                                        )
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isHighlighted) NawanoPrimary200 else textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Operational KPI Grid: 4 Cards (Matching Middleman NawanoOperationalCardsGrid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SupplierAnalyticsKpiCard(
                        title = "Gross Supply Value",
                        stat = "$${String.format(Locale.US, "%,.0f", totalValue)}",
                        description = "Cumulative wholesale revenue",
                        icon = Icons.Outlined.Paid,
                        iconTint = Color(0xFF10B981),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )

                    SupplierAnalyticsKpiCard(
                        title = "Units Supplied",
                        stat = "$totalQty Units",
                        description = "Total volume dispatched to HQ",
                        icon = Icons.Outlined.Inventory2,
                        iconTint = NawanoPrimary200,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SupplierAnalyticsKpiCard(
                        title = "Freight In Transit",
                        stat = "$inTransitCount Active",
                        description = "Courier shipments on the road",
                        icon = Icons.Outlined.LocalShipping,
                        iconTint = Color(0xFF6366F1),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )

                    SupplierAnalyticsKpiCard(
                        title = "Pending Approval",
                        stat = "$pendingCount Orders",
                        description = "Orders awaiting confirmation",
                        icon = Icons.Outlined.PendingActions,
                        iconTint = Color(0xFFF59E0B),
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Order Distribution Pipeline
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Order Distribution Pipeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(14.dp))

                    SupplierPipelineRow(
                        label = "In Delivery (Transit)",
                        count = inTransitCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFF6366F1),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    SupplierPipelineRow(
                        label = "Pending Confirmation",
                        count = pendingCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFFF59E0B),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    SupplierPipelineRow(
                        label = "Completed & Received",
                        count = completedCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFF10B981),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    SupplierPipelineRow(
                        label = "Rejected / Cancelled",
                        count = rejectedCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFFEF4444),
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        // 6. SLA & Settlement Terms Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Partner Settlement Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Orders issued by Middleman HQ settle on Net 30 terms upon delivery receipt. Cold-chain freight is insured under standard Nawano partner SLA.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplierAnalyticsKpiCard(
    title: String,
    stat: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stat,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
        }
    }
}

@Composable
private fun SupplierPipelineRow(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    isDarkTheme: Boolean
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val ratio = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = textPrimary)
            Text("$count orders", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textSecondary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
