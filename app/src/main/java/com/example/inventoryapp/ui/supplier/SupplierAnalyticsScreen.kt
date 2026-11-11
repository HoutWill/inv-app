package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Supplier Analytics & KPI Performance Screen, matching the visual depth of Middleman StatisticsScreen.
 */
@Composable
fun SupplierAnalyticsScreen(
    orders: List<PurchaseOrderEntity>,
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val totalValue = orders.sumOf { it.totalAmount }
    val totalQty = orders.sumOf { it.totalQuantity }
    val pendingCount = orders.count { it.status == PurchaseOrderStatus.PENDING }
    val inTransitCount = orders.count { it.status == PurchaseOrderStatus.DELIVERED }
    val completedCount = orders.count { it.status == PurchaseOrderStatus.RECEIVED }
    val rejectedCount = orders.count { it.status == PurchaseOrderStatus.REJECTED }

    val fulfillmentRate = if (orders.isEmpty()) 100.0 else {
        ((completedCount + inTransitCount).toDouble() / orders.size.toDouble()) * 100.0
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Hero Performance Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6366F1), NawanoPrimary200, Color(0xFF9061F9))
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
                            Text(
                                text = "Supplier Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Tier: Gold Partner",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fulfillment Rate", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", fulfillmentRate.coerceAtMost(100.0)),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("On-Time Dispatch", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                                Text("97.2%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid (Revenue & Total Qty)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Supply Value", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%,.2f", totalValue)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("${orders.size} Total Orders", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Units Supplied", style = MaterialTheme.typography.labelSmall, color = textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$totalQty Units",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = NawanoPrimary200
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("${products.size} Catalog SKUs", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                    }
                }
            }
        }

        // Order Status Distribution Breakdown
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
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

                    StatusProgressRow(
                        label = "In Delivery (Transit)",
                        count = inTransitCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFF6366F1),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    StatusProgressRow(
                        label = "Pending Confirmation",
                        count = pendingCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFFF59E0B),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    StatusProgressRow(
                        label = "Completed & Received",
                        count = completedCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFF10B981),
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(Modifier.height(10.dp))

                    StatusProgressRow(
                        label = "Rejected / Cancelled",
                        count = rejectedCount,
                        total = orders.size.coerceAtLeast(1),
                        color = Color(0xFFEF4444),
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        // Wholesale Settlement Terms
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Settlement & Payment Terms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All purchase orders issued by Middleman HQ settle on Net 30 terms upon receipt confirmation. Cold-chain deliveries are insured under standard freight SLA.",
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
private fun StatusProgressRow(
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
