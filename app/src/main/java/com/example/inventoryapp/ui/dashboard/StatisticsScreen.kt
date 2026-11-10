package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class DateFilterType(val label: String) {
    ALL_TIME("All Dates"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM_DATE("Custom")
}

data class ExpenseReportItem(
    val id: String,
    val productName: String,
    val category: String,
    val quantity: Int,
    val unitCost: Double,
    val totalExpense: Double,
    val timestamp: Long,
    val note: String? = null
)

data class DayExpenseSalesPoint(
    val dayLabel: String,
    val dateLabel: String,
    val spendAmount: Double,
    val salesAmount: Double,
    val spendFraction: Float,
    val salesFraction: Float,
    val salesCount: Int = 0,
    val isHighlighted: Boolean = false
)

/**
 * Clean & Minimal Nawano Statistics Screen (Behance: 174422339).
 * Features:
 * - Minimal Dropdown Selector (replaces cluttered segmented bars and custom rows).
 * - Exact daily sales count under chart days ("Mon 09 • 2 sales").
 * - Tightened margins and reduced padding to eliminate design overload.
 * - Concise, essential wording throughout all cards.
 * - Date-filtered & sorted expense feed and weekly comparison.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    totalSkus: Int,
    totalItems: Int,
    totalValue: Double,
    lowStockCount: Int,
    outOfStockCount: Int,
    products: List<ProductEntity> = emptyList(),
    transactions: List<StockTransactionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    // 1. Filter State
    var dateFilterType by remember { mutableStateOf(DateFilterType.THIS_MONTH) }
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePickerSheet by remember { mutableStateOf(false) }
    var filterDropdownExpanded by remember { mutableStateOf(false) }

    // 2. Target State
    var salesTarget by remember { mutableDoubleStateOf(7500.0) }
    var showSetTargetDialog by remember { mutableStateOf(false) }

    // 3. ABC Guide State
    var showAbcGuideDialog by remember { mutableStateOf(false) }

    // 4. Expense Sorting State
    var expenseSortDescending by remember { mutableStateOf(true) }

    // Formatters
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.US) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }

    // Lookup Maps
    val productMap = remember(products) { products.associateBy { it.id } }
    val productSkuMap = remember(products) { products.associateBy { it.sku } }

    // Filter transactions by active date
    val periodTransactions = remember(transactions, dateFilterType, selectedCalendar.timeInMillis) {
        val selYear = selectedCalendar.get(Calendar.YEAR)
        val selDayOfYear = selectedCalendar.get(Calendar.DAY_OF_YEAR)
        val selMonth = selectedCalendar.get(Calendar.MONTH)
        val selWeek = selectedCalendar.get(Calendar.WEEK_OF_YEAR)

        transactions.filter { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val txYear = txCal.get(Calendar.YEAR)
            when (dateFilterType) {
                DateFilterType.ALL_TIME -> true
                DateFilterType.TODAY -> {
                    val nowCal = Calendar.getInstance()
                    txYear == nowCal.get(Calendar.YEAR) && txCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                }
                DateFilterType.CUSTOM_DATE -> {
                    txYear == selYear && txCal.get(Calendar.DAY_OF_YEAR) == selDayOfYear
                }
                DateFilterType.THIS_WEEK -> {
                    txYear == selYear && txCal.get(Calendar.WEEK_OF_YEAR) == selWeek
                }
                DateFilterType.THIS_MONTH -> {
                    txYear == selYear && txCal.get(Calendar.MONTH) == selMonth
                }
            }
        }
    }

    // Dynamic Sales, Spend, and Units from real transactions
    val (dynamicSales, dynamicSpend, itemsSold) = remember(periodTransactions, productMap, productSkuMap) {
        val salesFromTx = periodTransactions
            .filter { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }
            .sumOf { tx ->
                val prod = productMap[tx.productId] ?: productSkuMap[tx.sku]
                val unitPrice = prod?.sellingPrice ?: 0.0
                abs(tx.quantityDelta) * unitPrice
            }
        val spendFromTx = periodTransactions
            .filter { it.quantityDelta > 0 || it.transactionType.contains("IN", ignoreCase = true) }
            .sumOf { tx ->
                val prod = productMap[tx.productId] ?: productSkuMap[tx.sku]
                val unitCost = prod?.costPrice ?: 0.0
                abs(tx.quantityDelta) * unitCost
            }
        val unitsSoldFromTx = periodTransactions
            .filter { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }
            .sumOf { abs(it.quantityDelta) }

        Triple(salesFromTx, spendFromTx, unitsSoldFromTx)
    }

    // Filtered & Sorted Expense Items from real transactions
    val periodExpenses = remember(periodTransactions, productMap, productSkuMap) {
        periodTransactions
            .filter { it.quantityDelta > 0 || it.transactionType.contains("IN", ignoreCase = true) }
            .map { tx ->
                val prod = productMap[tx.productId] ?: productSkuMap[tx.sku]
                val unitCost = prod?.costPrice ?: 0.0
                val totalExpense = abs(tx.quantityDelta) * unitCost
                ExpenseReportItem(
                    id = tx.id,
                    productName = tx.productName,
                    category = prod?.category?.ifBlank { "Restock" } ?: "Restock",
                    quantity = abs(tx.quantityDelta),
                    unitCost = unitCost,
                    totalExpense = totalExpense,
                    timestamp = tx.timestamp,
                    note = tx.note
                )
            }
    }

    val sortedExpenses = remember(periodExpenses, expenseSortDescending) {
        if (expenseSortDescending) {
            periodExpenses.sortedByDescending { it.timestamp }
        } else {
            periodExpenses.sortedBy { it.timestamp }
        }
    }

    // Dynamic Chart Points (Adapts to All Date Filters: Weekly, Monthly, All Dates, Today)
    val chartPoints = remember(transactions, selectedCalendar.timeInMillis, dateFilterType, productMap) {
        val cal = (selectedCalendar.clone() as Calendar)
        val points = mutableListOf<DayExpenseSalesPoint>()
        var maxSpend = 1.0
        var maxSales = 1.0

        when (dateFilterType) {
            DateFilterType.THIS_WEEK -> {
                // 7 days: Mon to Sun
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val mondayCal = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                for (i in 0 until 7) {
                    val dayCal = (mondayCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, i)
                    }
                    val dLabel = days[i]
                    val dateNumStr = String.format(Locale.US, "%02d", dayCal.get(Calendar.DAY_OF_MONTH))
                    val isCurrentOrSelected = (dayCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                            dayCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR))

                    val dayTx = transactions.filter { tx ->
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                txCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                    }

                    val daySpend = dayTx.filter { it.quantityDelta > 0 }.sumOf {
                        val cost = productMap[it.productId]?.costPrice ?: 0.0
                        it.quantityDelta * cost
                    }
                    val daySales = dayTx.filter { it.quantityDelta < 0 }.sumOf {
                        val price = productMap[it.productId]?.sellingPrice ?: 0.0
                        abs(it.quantityDelta) * price
                    }
                    val realSalesCount = dayTx.count { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }

                    if (daySpend > maxSpend) maxSpend = daySpend
                    if (daySales > maxSales) maxSales = daySales

                    points.add(
                        DayExpenseSalesPoint(
                            dayLabel = dLabel,
                            dateLabel = dateNumStr,
                            spendAmount = daySpend,
                            salesAmount = daySales,
                            spendFraction = 0f,
                            salesFraction = 0f,
                            salesCount = realSalesCount,
                            isHighlighted = isCurrentOrSelected
                        )
                    )
                }
            }

            DateFilterType.THIS_MONTH -> {
                // Breakdown into 5 weeks of the month: W1 (01-07), W2 (08-14), W3 (15-21), W4 (22-28), W5 (29-end)
                val targetMonth = cal.get(Calendar.MONTH)
                val targetYear = cal.get(Calendar.YEAR)
                val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                val isCurrentMonth = (Calendar.getInstance().get(Calendar.MONTH) == targetMonth &&
                        Calendar.getInstance().get(Calendar.YEAR) == targetYear)

                val weekBuckets = mutableListOf<Triple<String, String, IntRange>>()
                weekBuckets.add(Triple("W1", "01-07", 1..7))
                weekBuckets.add(Triple("W2", "08-14", 8..14))
                weekBuckets.add(Triple("W3", "15-21", 15..21))
                weekBuckets.add(Triple("W4", "22-28", 22..28))
                if (maxDaysInMonth > 28) {
                    weekBuckets.add(Triple("W5", "29-$maxDaysInMonth", 29..maxDaysInMonth))
                }

                weekBuckets.forEach { (wLabel, dateRangeStr, range) ->
                    val isHighlighted = isCurrentMonth && currentDayOfMonth in range

                    val weekTx = transactions.filter { tx ->
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == targetYear &&
                                txCal.get(Calendar.MONTH) == targetMonth &&
                                txCal.get(Calendar.DAY_OF_MONTH) in range
                    }

                    val weekSpend = weekTx.filter { it.quantityDelta > 0 }.sumOf {
                        val cost = productMap[it.productId]?.costPrice ?: 0.0
                        it.quantityDelta * cost
                    }
                    val weekSales = weekTx.filter { it.quantityDelta < 0 }.sumOf {
                        val price = productMap[it.productId]?.sellingPrice ?: 0.0
                        abs(it.quantityDelta) * price
                    }
                    val realSalesCount = weekTx.count { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }

                    if (weekSpend > maxSpend) maxSpend = weekSpend
                    if (weekSales > maxSales) maxSales = weekSales

                    points.add(
                        DayExpenseSalesPoint(
                            dayLabel = wLabel,
                            dateLabel = dateRangeStr,
                            spendAmount = weekSpend,
                            salesAmount = weekSales,
                            spendFraction = 0f,
                            salesFraction = 0f,
                            salesCount = realSalesCount,
                            isHighlighted = isHighlighted
                        )
                    )
                }
            }

            DateFilterType.ALL_TIME -> {
                // Past 6 months: e.g. Jun, Jul, Aug, Sep, Oct, Nov
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val nowCal = Calendar.getInstance()
                for (offset in 5 downTo 0) {
                    val mCal = (nowCal.clone() as Calendar).apply {
                        add(Calendar.MONTH, -offset)
                    }
                    val mYear = mCal.get(Calendar.YEAR)
                    val mMonth = mCal.get(Calendar.MONTH)
                    val mLabel = monthNames[mMonth]
                    val yrLabel = "'${(mYear % 100).toString().padStart(2, '0')}"
                    val isCurrent = offset == 0

                    val mTx = transactions.filter { tx ->
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == mYear && txCal.get(Calendar.MONTH) == mMonth
                    }

                    val mSpend = mTx.filter { it.quantityDelta > 0 }.sumOf {
                        val cost = productMap[it.productId]?.costPrice ?: 0.0
                        it.quantityDelta * cost
                    }
                    val mSales = mTx.filter { it.quantityDelta < 0 }.sumOf {
                        val price = productMap[it.productId]?.sellingPrice ?: 0.0
                        abs(it.quantityDelta) * price
                    }
                    val realSalesCount = mTx.count { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }

                    if (mSpend > maxSpend) maxSpend = mSpend
                    if (mSales > maxSales) maxSales = mSales

                    points.add(
                        DayExpenseSalesPoint(
                            dayLabel = mLabel,
                            dateLabel = yrLabel,
                            spendAmount = mSpend,
                            salesAmount = mSales,
                            spendFraction = 0f,
                            salesFraction = 0f,
                            salesCount = realSalesCount,
                            isHighlighted = isCurrent
                        )
                    )
                }
            }

            DateFilterType.TODAY, DateFilterType.CUSTOM_DATE -> {
                val targetYear = cal.get(Calendar.YEAR)
                val targetDay = cal.get(Calendar.DAY_OF_YEAR)
                val timeWindows = listOf(
                    Triple("08:00", "Morn", 6..9),
                    Triple("11:00", "Noon", 10..12),
                    Triple("14:00", "Aft", 13..15),
                    Triple("17:00", "Late", 16..18),
                    Triple("20:00", "Eve", 19..23)
                )

                timeWindows.forEachIndexed { idx, (timeLabel, descLabel, hourRange) ->
                    val slotTx = transactions.filter { tx ->
                        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                        txCal.get(Calendar.YEAR) == targetYear &&
                                txCal.get(Calendar.DAY_OF_YEAR) == targetDay &&
                                txCal.get(Calendar.HOUR_OF_DAY) in hourRange
                    }

                    val slotSpend = slotTx.filter { it.quantityDelta > 0 }.sumOf {
                        val cost = productMap[it.productId]?.costPrice ?: 0.0
                        it.quantityDelta * cost
                    }
                    val slotSales = slotTx.filter { it.quantityDelta < 0 }.sumOf {
                        val price = productMap[it.productId]?.sellingPrice ?: 0.0
                        abs(it.quantityDelta) * price
                    }
                    val realSalesCount = slotTx.count { it.quantityDelta < 0 || it.transactionType.contains("OUT", ignoreCase = true) }

                    if (slotSpend > maxSpend) maxSpend = slotSpend
                    if (slotSales > maxSales) maxSales = slotSales

                    points.add(
                        DayExpenseSalesPoint(
                            dayLabel = timeLabel,
                            dateLabel = descLabel,
                            spendAmount = slotSpend,
                            salesAmount = slotSales,
                            spendFraction = 0f,
                            salesFraction = 0f,
                            salesCount = realSalesCount,
                            isHighlighted = idx == 2
                        )
                    )
                }
            }
        }

        val maxVal = maxOf(maxSpend, maxSales, 1.0)
        points.map { p ->
            p.copy(
                spendFraction = if (maxVal > 0 && p.spendAmount > 0) (p.spendAmount / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f,
                salesFraction = if (maxVal > 0 && p.salesAmount > 0) (p.salesAmount / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f
            )
        }
    }

    val targetProgress = if (salesTarget > 0 && dynamicSales > 0) (dynamicSales / salesTarget).toFloat().coerceIn(0f, 1f) else 0f
    val targetPercentage = if (salesTarget > 0) ((dynamicSales / salesTarget) * 100).toInt() else 0
    val targetStatus = when {
        targetPercentage >= 100 -> "Achieved 🎉"
        targetPercentage >= 75 -> "On Track"
        else -> "In Progress"
    }
    val targetStatusColor = when {
        targetPercentage >= 100 -> NawanoEmerald
        targetPercentage >= 75 -> NawanoPrimary200
        else -> NawanoRose
    }

    // ABC Categorization from real inventory products
    val abcItems = remember(products) {
        if (products.isEmpty()) {
            emptyList<AbcItemData>()
        } else {
            val sorted = products.sortedByDescending { it.quantity * it.sellingPrice }
            val totalCatValue = sorted.sumOf { it.quantity * it.sellingPrice }.coerceAtLeast(1.0)
            var runningVal = 0.0

            sorted.map { p ->
                val itemVal = p.quantity * p.sellingPrice
                runningVal += itemVal
                val cumPct = runningVal / totalCatValue
                val classification = when {
                    cumPct <= 0.75 -> "Class A"
                    cumPct <= 0.92 -> "Class B"
                    else -> "Class C"
                }
                val isAlert = p.quantity <= p.reorderLevel
                AbcItemData(
                    classification = classification,
                    title = p.name,
                    supplier = p.brand.ifBlank { p.category },
                    stock = "${p.quantity} ${p.unit}",
                    valueFormatted = "$${String.format(Locale.US, "%.2f", itemVal)}",
                    isAlert = isAlert
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // =========================================================================
        // 1. MINIMAL HEADER WITH COMPACT DROPDOWN SELECTOR (Zero Clutter)
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Minimal Dropdown Selector Pill
            Box {
                Surface(
                    onClick = { filterDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val filterLabel = when (dateFilterType) {
                            DateFilterType.ALL_TIME -> "All Dates"
                            DateFilterType.TODAY -> "Today"
                            DateFilterType.THIS_WEEK -> "This Week"
                            DateFilterType.THIS_MONTH -> "This Month"
                            DateFilterType.CUSTOM_DATE -> dateFormat.format(selectedCalendar.time)
                        }
                        Text(
                            text = filterLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = filterDropdownExpanded,
                    onDismissRequest = { filterDropdownExpanded = false },
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    DropdownMenuItem(
                        text = { Text("All Dates", fontWeight = if (dateFilterType == DateFilterType.ALL_TIME) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            dateFilterType = DateFilterType.ALL_TIME
                            filterDropdownExpanded = false
                        },
                        leadingIcon = {
                            if (dateFilterType == DateFilterType.ALL_TIME) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Today", fontWeight = if (dateFilterType == DateFilterType.TODAY) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            dateFilterType = DateFilterType.TODAY
                            filterDropdownExpanded = false
                        },
                        leadingIcon = {
                            if (dateFilterType == DateFilterType.TODAY) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("This Week", fontWeight = if (dateFilterType == DateFilterType.THIS_WEEK) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            dateFilterType = DateFilterType.THIS_WEEK
                            filterDropdownExpanded = false
                        },
                        leadingIcon = {
                            if (dateFilterType == DateFilterType.THIS_WEEK) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("This Month", fontWeight = if (dateFilterType == DateFilterType.THIS_MONTH) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            dateFilterType = DateFilterType.THIS_MONTH
                            filterDropdownExpanded = false
                        },
                        leadingIcon = {
                            if (dateFilterType == DateFilterType.THIS_MONTH) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    DropdownMenuItem(
                        text = { Text("Pick Date...", fontWeight = if (dateFilterType == DateFilterType.CUSTOM_DATE) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            filterDropdownExpanded = false
                            showDatePickerSheet = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // =========================================================================
        // 2. OVERVIEW & TARGET CARD (Minimal Word & Clean Layout)
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header: Title + Target Setting Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sales Overview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Target $${String.format(Locale.US, "%,.0f", salesTarget)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = { showSetTargetDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Target",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Target",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Sales",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$${String.format(Locale.US, "%,.2f", dynamicSales)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = targetStatusColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = targetStatus,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = targetStatusColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "Spend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", dynamicSpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Compact Gauge
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 8.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            val arcSize = Size(diameter, diameter)

                            drawArc(
                                color = Color(0xFFE2E4F0).copy(alpha = 0.4f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(NawanoPrimary200, NawanoGradientEnd, NawanoPrimary200)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * targetProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${targetPercentage}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "of goal",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(8.dp))

                // 3 Concise Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricMiniPill(label = "Units Sold", value = "$itemsSold", color = NawanoPrimary200)
                    MetricMiniPill(label = "Low Stock", value = "$lowStockCount", color = NawanoAmber)
                    MetricMiniPill(label = "Out of Stock", value = "$outOfStockCount", color = NawanoRose)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =========================================================================
        // 3. ACTIVITY & EXPENSES CARD (Chart with daily sales + minimal list)
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header with Minimal Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activityTitle = when (dateFilterType) {
                        DateFilterType.ALL_TIME -> "All-Time Activity"
                        DateFilterType.THIS_WEEK -> "Weekly Activity"
                        DateFilterType.THIS_MONTH -> "Monthly Activity"
                        DateFilterType.TODAY -> "Today's Activity"
                        DateFilterType.CUSTOM_DATE -> "${dateFormat.format(selectedCalendar.time)} Activity"
                    }
                    Text(
                        text = activityTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NawanoPrimary200))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Spend", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(NawanoPrimary100))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Sales", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Weekly Bar Chart with Sales Count under each day
                WeeklyBarChartCanvas(points = chartPoints)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(8.dp))

                // Minimal Expense Activity Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expenses (${sortedExpenses.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        onClick = { expenseSortDescending = !expenseSortDescending },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (expenseSortDescending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = "Sort",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (expenseSortDescending) "Newest" else "Oldest",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Compact List of Expense Items (No Visual Overload)
                if (sortedExpenses.isEmpty()) {
                    Text(
                        text = "No expense activity recorded for this period.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sortedExpenses.take(3).forEach { expense ->
                            MinimalExpenseRowItem(expense = expense)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =========================================================================
        // 4. ABC ANALYSIS (Clean & Compact)
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ABC Analysis",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(
                onClick = { showAbcGuideDialog = true },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Guide ?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (abcItems.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No products registered for ABC analysis yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                abcItems.take(6).forEach { item ->
                    AbcAnalysisCard(item = item)
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // =========================================================================
    // MODALS & SHEETS
    // =========================================================================

    if (showDatePickerSheet) {
        NawanoDatePickerBottomSheet(
            selectedDate = selectedCalendar,
            transactions = transactions,
            onDateSelected = { clickedCal ->
                selectedCalendar = clickedCal
                dateFilterType = DateFilterType.CUSTOM_DATE
            },
            onDismiss = { showDatePickerSheet = false }
        )
    }

    if (showSetTargetDialog) {
        SetTargetDialog(
            currentTarget = salesTarget,
            onDismiss = { showSetTargetDialog = false },
            onTargetSaved = { newTarget ->
                salesTarget = newTarget
                showSetTargetDialog = false
            }
        )
    }

    if (showAbcGuideDialog) {
        AbcAnalysisGuideDialog(onDismiss = { showAbcGuideDialog = false })
    }
}

// =========================================================================
// REFINED DATE PICKER BOTTOM SHEET
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NawanoDatePickerBottomSheet(
    selectedDate: Calendar,
    transactions: List<StockTransactionEntity>,
    onDateSelected: (Calendar) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentViewMonth by remember {
        mutableStateOf((selectedDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) })
    }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 36.dp, height = 3.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    onClick = {
                        val today = Calendar.getInstance()
                        onDateSelected(today)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Month Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        currentViewMonth = (currentViewMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = monthFormat.format(currentViewMonth.time),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        currentViewMonth = (currentViewMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NawanoInteractiveCalendarGrid(
                viewMonth = currentViewMonth,
                selectedDate = selectedDate,
                transactions = transactions,
                onDateSelected = { clickedCal ->
                    onDateSelected(clickedCal)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// CALENDAR GRID COMPONENT
// =========================================================================

@Composable
private fun NawanoInteractiveCalendarGrid(
    viewMonth: Calendar,
    selectedDate: Calendar,
    transactions: List<StockTransactionEntity>,
    onDateSelected: (Calendar) -> Unit
) {
    val dayNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    val cal = (viewMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val currentYear = viewMonth.get(Calendar.YEAR)
    val currentMonth = viewMonth.get(Calendar.MONTH)

    val datesWithActivity = remember(transactions, currentYear, currentMonth) {
        val daysSet = mutableSetOf<Int>()
        transactions.forEach { tx ->
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            if (txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth) {
                daysSet.add(txCal.get(Calendar.DAY_OF_MONTH))
            }
        }
        daysSet
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayNames.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val totalSlots = firstDayOfWeek + daysInMonth
        val rows = (totalSlots + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val slotIndex = row * 7 + col
                    val dayNum = slotIndex - firstDayOfWeek + 1

                    if (dayNum in 1..daysInMonth) {
                        val isSelected = selectedDate.get(Calendar.YEAR) == currentYear &&
                                selectedDate.get(Calendar.MONTH) == currentMonth &&
                                selectedDate.get(Calendar.DAY_OF_MONTH) == dayNum

                        val isToday = Calendar.getInstance().let {
                            it.get(Calendar.YEAR) == currentYear &&
                                    it.get(Calendar.MONTH) == currentMonth &&
                                    it.get(Calendar.DAY_OF_MONTH) == dayNum
                        }

                        val hasActivity = datesWithActivity.contains(dayNum)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val newDate = (viewMonth.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                    }
                                    onDateSelected(newDate)
                                }
                                .padding(vertical = 3.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) NawanoPrimary200 else Color.Transparent,
                                border = if (isToday && !isSelected) BorderStroke(1.dp, NawanoPrimary200) else null,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$dayNum",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(1.dp))
                            if (hasActivity) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NawanoPrimary200 else NawanoEmerald)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(32.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// MINIMAL EXPENSE ROW ITEM (Clean & Tight)
// =========================================================================

@Composable
private fun MinimalExpenseRowItem(expense: ExpenseReportItem) {
    val timeFormat = remember { SimpleDateFormat("MMM dd", Locale.US) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NawanoRose)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = expense.productName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${expense.category} • ${timeFormat.format(Date(expense.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$${String.format(Locale.US, "%,.2f", expense.totalExpense)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NawanoRose
                )
                Text(
                    text = "+${expense.quantity} pcs",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================================
// TARGET SETTING DIALOG
// =========================================================================

@Composable
private fun SetTargetDialog(
    currentTarget: Double,
    onDismiss: () -> Unit,
    onTargetSaved: (Double) -> Unit
) {
    var targetInput by remember { mutableStateOf(currentTarget.toInt().toString()) }
    val quickPresets = listOf(5000, 7500, 10000, 15000, 20000)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Sales Target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Target Amount ($)") },
                    prefix = { Text("$ ", fontWeight = FontWeight.Bold, color = NawanoPrimary200) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        val isSelected = targetInput == preset.toString()
                        Surface(
                            onClick = { targetInput = preset.toString() },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%,d", preset)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val parsed = targetInput.toDoubleOrNull() ?: currentTarget
                            onTargetSaved(parsed)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// ABC ANALYSIS GUIDE DIALOG
// =========================================================================

@Composable
private fun AbcAnalysisGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ABC Inventory Guide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "80/20 Pareto Rule categorization",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                AbcGuideClassCard(
                    tier = "Class A",
                    badgeColor = NawanoPrimary200,
                    percentage = "~80% Value • 20% Items",
                    description = "High value, essential revenue drivers. Requires strict tracking and priority restocking."
                )

                Spacer(modifier = Modifier.height(8.dp))

                AbcGuideClassCard(
                    tier = "Class B",
                    badgeColor = NawanoAmber,
                    percentage = "~15% Value • 30% Items",
                    description = "Moderate value items with predictable demand. Managed with automated reorders."
                )

                Spacer(modifier = Modifier.height(8.dp))

                AbcGuideClassCard(
                    tier = "Class C",
                    badgeColor = Color(0xFF06B6D4),
                    percentage = "~5% Value • 50% Items",
                    description = "Low value consumables. Managed with bulk ordering and safe buffer stock."
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AbcGuideClassCard(
    tier: String,
    badgeColor: Color,
    percentage: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tier,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
                Text(
                    text = percentage,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

// =========================================================================
// DATA MODELS & CHART SUBCOMPONENTS
// =========================================================================

data class AbcItemData(
    val classification: String,
    val title: String,
    val supplier: String,
    val stock: String,
    val valueFormatted: String,
    val isAlert: Boolean
)

@Composable
private fun MetricMiniPill(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun WeeklyBarChartCanvas(points: List<DayExpenseSalesPoint>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val totalWidth = size.width
            val chartHeight = size.height
            val count = points.size.coerceAtLeast(1)
            val barGroupWidth = totalWidth / count
            val barWidth = when {
                count <= 5 -> 12.dp.toPx()
                count <= 6 -> 10.dp.toPx()
                count <= 7 -> 9.dp.toPx()
                else -> 7.dp.toPx()
            }

            val steps = 3
            for (i in 0..steps) {
                val y = chartHeight * (i.toFloat() / steps)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.18f),
                    start = Offset(0f, y),
                    end = Offset(totalWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.forEachIndexed { index, point ->
                val centerX = barGroupWidth * index + barGroupWidth / 2

                // Spend bar (Left)
                val spendHeight = chartHeight * point.spendFraction
                val spendLeft = centerX - barWidth - 1.5.dp.toPx()
                drawRoundRect(
                    color = NawanoPrimary200,
                    topLeft = Offset(spendLeft, chartHeight - spendHeight),
                    size = Size(barWidth, spendHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Sales bar (Right)
                val salesHeight = chartHeight * point.salesFraction
                val salesLeft = centerX + 1.5.dp.toPx()
                drawRoundRect(
                    color = NawanoPrimary100,
                    topLeft = Offset(salesLeft, chartHeight - salesHeight),
                    size = Size(barWidth, salesHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Days row: Day/Period Name + Date Range/Number + Sales Count (e.g. "Mon / 09 / 2 sales" or "W1 / 01-07 / 14 sales")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = point.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (point.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (point.isHighlighted) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = point.dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (point.isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (point.isHighlighted) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${point.salesCount} ${if (point.salesCount == 1) "sale" else "sales"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (point.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (point.isHighlighted) NawanoPrimary200 else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AbcAnalysisCard(item: AbcItemData) {
    val badgeBg = when (item.classification) {
        "Class A" -> NawanoPrimary200
        "Class B" -> NawanoAmber
        else -> Color(0xFF06B6D4)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (item.isAlert) NawanoRose.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        shadowElevation = 1.dp,
        modifier = Modifier.width(138.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = item.classification,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeBg,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                Text(
                    text = item.valueFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isAlert) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NawanoRose,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = item.stock,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (item.isAlert) NawanoRose else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
