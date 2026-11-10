package com.example.inventoryapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Authentic Nawano Statistics Screen (Behance: 174422339 Slide 6 & 7).
 * Displays net sales target gauge, sales vs spend overview, weekly expense bar chart,
 * and ABC inventory categorization analysis.
 */
@Composable
fun StatisticsScreen(
    totalSkus: Int,
    totalItems: Int,
    totalValue: Double,
    lowStockCount: Int,
    outOfStockCount: Int,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("Weekly, 22 - 28 May") }
    var periodMenuExpanded by remember { mutableStateOf(false) }

    val periods = listOf(
        "Weekly, 22 - 28 May",
        "Monthly, May 2026",
        "Quarterly, Q2 2026",
        "Year to Date 2026"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Period Dropdown Selector Pill
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { periodMenuExpanded = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 1.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedPeriod,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = periodMenuExpanded,
                onDismissRequest = { periodMenuExpanded = false }
            ) {
                periods.forEach { period ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = period,
                                fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                                color = if (period == selectedPeriod) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            selectedPeriod = period
                            periodMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Inventory Overview Card (with Circular Progress Ring)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Inventory Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Target $7,500 Sales (Net)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Total Sales & Total Spend
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Total Sales
                        Column {
                            Text(
                                text = "Total Sales",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$6,280.50",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NawanoRose.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "+1.8%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NawanoRose,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Not Achieved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Total Spend
                        Column {
                            Text(
                                text = "Total Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$1,530.00",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NawanoRose.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "-21.5%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NawanoRose,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Circular Progress Gauge (88% of Target)
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            val arcSize = Size(diameter, diameter)

                            // Background Track
                            drawArc(
                                color = Color(0xFFE2E4F0),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Active Progress Arc (88% = 316.8 degrees)
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(NawanoPrimary200, NawanoGradientEnd, NawanoPrimary200)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * 0.88f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "88%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(14.dp))

                // 3 Metric Pills Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniPill(label = "Items Sold", value = "276 Items", color = NawanoPrimary200)
                    MetricMiniPill(label = "Low Stock", value = "$lowStockCount Items", color = NawanoAmber)
                    MetricMiniPill(label = "Out of Stock", value = "$outOfStockCount Items", color = NawanoRose)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Expense Report Card (Weekly Bar Chart)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Legend: Spend (solid purple) • Sales (lavender)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NawanoPrimary200))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NawanoPrimary100))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Weekly Bar Chart Canvas (Tue, Wed, Thu, Fri, Sat, Sun, Mon)
                WeeklyBarChartCanvas()
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. ABC Analysis Section (Behance Slide 6/7)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ABC Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "View all >",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NawanoPrimary200
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrolling ABC analysis cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AbcAnalysisCard(
                classification = "Class A",
                title = "Boneless Chicken",
                supplier = "Central Outlet",
                stock = "41 Stock",
                isAlert = false
            )
            AbcAnalysisCard(
                classification = "Class B",
                title = "Garlic Parmesan",
                supplier = "Sauce World Co",
                stock = "2 Stock",
                isAlert = true
            )
            AbcAnalysisCard(
                classification = "Class C",
                title = "Cooking Oil",
                supplier = "Toa Frozen",
                stock = "180 Stock",
                isAlert = false
            )
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun MetricMiniPill(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun WeeklyBarChartCanvas() {
    val days = listOf("Tue", "Wed", "Thu", "Fri", "Sat", "Sun", "Mon")
    // Pairs of (SpendFraction, SalesFraction) normalized 0.0 to 1.0
    val data = listOf(
        Pair(0.40f, 0.65f),
        Pair(0.55f, 0.85f),
        Pair(0.30f, 0.50f),
        Pair(0.70f, 0.90f),
        Pair(0.45f, 0.75f),
        Pair(0.80f, 0.95f),
        Pair(0.35f, 0.60f)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val totalWidth = size.width
            val chartHeight = size.height
            val barGroupWidth = totalWidth / days.size
            val barWidth = 10.dp.toPx()

            // Draw horizontal guide lines
            val steps = 3
            for (i in 0..steps) {
                val y = chartHeight * (i.toFloat() / steps)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    start = Offset(0f, y),
                    end = Offset(totalWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw paired bars
            data.forEachIndexed { index, (spend, sales) ->
                val centerX = barGroupWidth * index + barGroupWidth / 2

                // Spend bar (Left)
                val spendHeight = chartHeight * spend
                val spendLeft = centerX - barWidth - 2.dp.toPx()
                drawRoundRect(
                    color = NawanoPrimary200,
                    topLeft = Offset(spendLeft, chartHeight - spendHeight),
                    size = Size(barWidth, spendHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Sales bar (Right)
                val salesHeight = chartHeight * sales
                val salesLeft = centerX + 2.dp.toPx()
                drawRoundRect(
                    color = NawanoPrimary100,
                    topLeft = Offset(salesLeft, chartHeight - salesHeight),
                    size = Size(barWidth, salesHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AbcAnalysisCard(
    classification: String,
    title: String,
    supplier: String,
    stock: String,
    isAlert: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isAlert) NawanoRose.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 1.dp,
        modifier = Modifier.width(145.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isAlert) NawanoRose.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = classification,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) NawanoRose else NawanoPrimary200,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = supplier,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAlert) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NawanoRose,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = stock,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) NawanoRose else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
