package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ParkedCartEntity
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.data.local.entity.PosPaymentStatus
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import com.example.inventoryapp.data.local.entity.SalesOrderLineItem
import com.example.inventoryapp.data.local.entity.parseSalesOrderLineItems
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Nawano-styled Order Management & Payment History Modal Bottom Sheet:
 * Tab 0: Unpaid Tabs (COD & Pay Later debts with quick settlement)
 * Tab 1: Payment History (Searchable, filterable revenue summary & receipts)
 * Tab 2: Parked Carts (Held orders with resume to cart & delete)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosOrdersManagementBottomSheet(
    unpaidOrders: List<SalesOrderEntity>,
    allOrders: List<SalesOrderEntity>,
    parkedCarts: List<ParkedCartEntity>,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    currency: AppCurrency = AppCurrency.USD,
    initialTab: Int = 0,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSettleOrder: (orderId: String, method: PosPaymentMethod) -> Unit,
    onVoidOrder: (orderId: String, reason: String) -> Unit,
    onProcessRefund: (
        orderId: String,
        returnedItems: List<SalesOrderLineItem>,
        issueType: String,
        conditionAnswer: String,
        reason: String,
        restockItems: Boolean,
        refundMethod: String,
        proofImageUri: String,
        onUpdated: (SalesOrderEntity) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onResumeParkedCart: (cart: ParkedCartEntity) -> Unit,
    onDeleteParkedCart: (cartId: String) -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var viewingOrderDetails by remember { mutableStateOf<SalesOrderEntity?>(null) }
    var orderToVoid by remember { mutableStateOf<SalesOrderEntity?>(null) }
    var orderToRefund by remember { mutableStateOf<SalesOrderEntity?>(null) }
    var orderReceiptToView by remember { mutableStateOf<SalesOrderEntity?>(null) }
    var voidReason by remember { mutableStateOf("Customer returned / cancelled item") }

    val sheetBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerBg = if (isDarkTheme) NawanoDark400 else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = textSecondary.copy(alpha = 0.4f)
            )
        },
        containerColor = sheetBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            // Modern Nawano Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (viewingOrderDetails != null) {
                        IconButton(
                            onClick = { viewingOrderDetails = null },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                        }
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Surface(
                            color = NawanoPrimary200.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Outlined.History else Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = if (viewingOrderDetails != null) {
                                "Order #${viewingOrderDetails?.invoiceNumber}"
                            } else if (selectedTab == 1) {
                                "Payment History"
                            } else {
                                "Order Management"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = if (viewingOrderDetails != null) {
                                "Receipt, refund & audit details"
                            } else {
                                "Unpaid tabs, payment history & parked carts"
                            },
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            AnimatedContent(
                targetState = viewingOrderDetails,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut())
                    }
                },
                label = "OrderMgmtDetailsTransition",
                modifier = Modifier
                    .weight(1f)
                    .consumeBottomSheetNestedDismiss()
            ) { detailOrder ->
                if (detailOrder == null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(6.dp))

                        // Modern Pill Segmented Control (matching PartnerRequestsBottomSheet & Supplier UI)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = innerBg,
                            border = BorderStroke(1.dp, borderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                val tabs = listOf(
                                    "Unpaid (${unpaidOrders.size})",
                                    "History (${allOrders.size})",
                                    "Parked (${parkedCarts.size})"
                                )
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = selectedTab == index
                                    Surface(
                                        onClick = { selectedTab = index },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) NawanoPrimary200 else Color.Transparent,
                                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Tab Content
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                0 -> UnpaidTabsList(
                                    orders = unpaidOrders,
                                    exchangeRate = exchangeRate,
                                    currency = currency,
                                    isDarkTheme = isDarkTheme,
                                    onSettle = onSettleOrder,
                                    onSelectOrder = { viewingOrderDetails = it }
                                )
                                1 -> SalesHistoryList(
                                    orders = allOrders,
                                    exchangeRate = exchangeRate,
                                    currency = currency,
                                    isDarkTheme = isDarkTheme,
                                    onSelectOrder = { viewingOrderDetails = it }
                                )
                                2 -> ParkedCartsList(
                                    parkedCarts = parkedCarts,
                                    exchangeRate = exchangeRate,
                                    currency = currency,
                                    isDarkTheme = isDarkTheme,
                                    onResume = {
                                        onResumeParkedCart(it)
                                        onDismiss()
                                    },
                                    onDelete = onDeleteParkedCart
                                )
                            }
                        }
                    }
                } else {
                    OrderDetailsView(
                        order = detailOrder,
                        exchangeRate = exchangeRate,
                        currency = currency,
                        onOpenReceipt = { orderReceiptToView = detailOrder },
                        onRefundPrompt = { orderToRefund = detailOrder },
                        onVoidPrompt = { orderToVoid = detailOrder },
                        onSettle = { method ->
                            onSettleOrder(detailOrder.id, method)
                            viewingOrderDetails = null
                        }
                    )
                }
            }
        }
    }

    // Guided Report an Issue & Refund Sheet
    if (orderToRefund != null) {
        val targetOrder = orderToRefund!!
        PosRefundIssueBottomSheet(
            order = targetOrder,
            currency = currency,
            exchangeRate = exchangeRate,
            onDismiss = { orderToRefund = null },
            onSubmitRefund = { returnedItems, issueType, conditionAnswer, reason, restockItems, refundMethod, proofImageUri ->
                onProcessRefund(
                    targetOrder.id,
                    returnedItems,
                    issueType,
                    conditionAnswer,
                    reason,
                    restockItems,
                    refundMethod,
                    proofImageUri
                ) { updatedOrder ->
                    viewingOrderDetails = updatedOrder
                }
                orderToRefund = null
            }
        )
    }

    // Full Itemized Invoice & Receipt Sheet (Print / Save PDF / Share)
    if (orderReceiptToView != null) {
        PosInvoiceReceiptBottomSheet(
            order = orderReceiptToView!!,
            currency = currency,
            exchangeRate = exchangeRate,
            onDismiss = { orderReceiptToView = null }
        )
    }

    // Void Order Confirmation Sheet / Dialog (using Material 3 inline sub-sheet)
    if (orderToVoid != null) {
        val targetOrder = orderToVoid!!
        ModalBottomSheet(
            onDismissRequest = { orderToVoid = null },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Void / Cancel Order", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        Text("Invoice #${targetOrder.invoiceNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Voiding this order will mark it as CANCELLED and automatically RESTOCK ${targetOrder.itemCount} items back into inventory with an audit log.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Text("Reason for cancellation:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                val reasons = listOf(
                    "Customer returned / cancelled item",
                    "Cashier wrong item entered",
                    "Defective / damaged merchandise",
                    "Payment dispute / failed"
                )

                reasons.forEach { r ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (voidReason == r) NawanoPrimary200.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (voidReason == r) NawanoPrimary200 else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { voidReason = r }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = voidReason == r,
                                onClick = { voidReason = r },
                                colors = RadioButtonDefaults.colors(selectedColor = NawanoPrimary200)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = r, fontSize = 13.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { orderToVoid = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Keep Sale")
                    }

                    Button(
                        onClick = {
                            onVoidOrder(targetOrder.id, voidReason)
                            orderToVoid = null
                            viewingOrderDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm Void & Restock", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SUB-LISTS: UNPAID, HISTORY, PARKED
// ---------------------------------------------------------------------------

@Composable
private fun UnpaidTabsList(
    orders: List<SalesOrderEntity>,
    exchangeRate: Double,
    currency: AppCurrency,
    isDarkTheme: Boolean = false,
    onSettle: (String, PosPaymentMethod) -> Unit,
    onSelectOrder: (SalesOrderEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(10.dp))
                Text("All customer tabs are fully settled!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(orders, key = { it.id }) { tab ->
                val now = System.currentTimeMillis()
                val isOverdue = tab.dueDate != null && tab.dueDate < now

                Surface(
                    onClick = { onSelectOrder(tab) },
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.45f) else cardBorder),
                    shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tab.customerName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Invoice #${tab.invoiceNumber}", fontSize = 11.5.sp, color = textSecondary)
                            }
                            Text(
                                text = CurrencyFormatter.format(tab.remainingAmountUsd, currency, exchangeRate),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isOverdue) Color(0xFFDC2626) else NawanoPrimary200
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(tab.itemsSummary, fontSize = 12.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onSettle(tab.id, PosPaymentMethod.ABA_KHQR) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("KHQR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { onSettle(tab.id, PosPaymentMethod.CASH) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Settle Cash", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesHistoryList(
    orders: List<SalesOrderEntity>,
    exchangeRate: Double,
    currency: AppCurrency,
    isDarkTheme: Boolean = false,
    onSelectOrder: (SalesOrderEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val searchBg = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val filterOptions = listOf("All", "Paid", "ABA KHQR", "Cash", "Card", "Unpaid", "Refund/Void")

    val paidRevenueUsd = remember(orders) {
        orders.filter {
            it.paymentStatus != PosPaymentStatus.CANCELLED &&
                it.paymentStatus != PosPaymentStatus.REFUNDED
        }.sumOf { (it.paidAmountUsd - it.refundedAmountUsd).coerceAtLeast(0.0) }
    }
    val completedCount = remember(orders) {
        orders.count { it.paymentStatus == PosPaymentStatus.PAID }
    }

    val filteredOrders = remember(orders, searchQuery, selectedFilter) {
        orders.filter { order ->
            val matchesFilter = when (selectedFilter) {
                "Paid" -> order.paymentStatus == PosPaymentStatus.PAID
                "ABA KHQR" -> order.paymentMethod == PosPaymentMethod.ABA_KHQR
                "Cash" -> order.paymentMethod == PosPaymentMethod.CASH
                "Card" -> order.paymentMethod == PosPaymentMethod.CARD
                "Unpaid" -> order.paymentMethod == PosPaymentMethod.PAY_LATER || order.paymentMethod == PosPaymentMethod.COD
                "Refund/Void" -> order.paymentStatus == PosPaymentStatus.REFUNDED ||
                    order.paymentStatus == PosPaymentStatus.PARTIALLY_REFUNDED ||
                    order.paymentStatus == PosPaymentStatus.CANCELLED
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                order.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                order.customerName.contains(searchQuery, ignoreCase = true) ||
                order.itemsSummary.contains(searchQuery, ignoreCase = true) ||
                order.paymentMethod.name.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Modern Gradient Revenue Summary Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            shadowElevation = if (isDarkTheme) 0.dp else 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NawanoGradientStart,
                                NawanoPrimary200,
                                NawanoGradientEnd
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PAID PAYMENTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.78f), letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("$completedCount of ${orders.size} orders", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("COLLECTED REVENUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.78f), letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = CurrencyFormatter.format(paidRevenueUsd, currency, exchangeRate),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sleek Modern Nawano Search Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = searchBg,
            border = BorderStroke(1.dp, cardBorder),
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
                    contentDescription = "Search",
                    tint = textSecondary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    cursorBrush = SolidColor(NawanoPrimary200),
                    textStyle = TextStyle(
                        color = textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search invoice #, customer, item...",
                                color = textSecondary.copy(alpha = 0.75f),
                                fontSize = 13.5.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Modern Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { option ->
                val isSelected = selectedFilter == option
                Surface(
                    onClick = { selectedFilter = option },
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF6C63FF) else cardBg,
                    border = if (isSelected) null else BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (orders.isEmpty()) "No payment history recorded yet" else "No payments matching \"$selectedFilter\"",
                    color = textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    val isCancelled = order.paymentStatus == PosPaymentStatus.CANCELLED
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                    val (methodIcon, methodColor) = when (order.paymentMethod) {
                        PosPaymentMethod.ABA_KHQR -> Icons.Default.QrCode2 to Color(0xFF004F71)
                        PosPaymentMethod.CASH -> Icons.Outlined.Payments to Color(0xFF10B981)
                        PosPaymentMethod.CARD -> Icons.Outlined.CreditCard to Color(0xFF6366F1)
                        PosPaymentMethod.SPLIT -> Icons.Outlined.CallSplit to NawanoPrimary200
                        else -> Icons.Outlined.LocalShipping to Color(0xFFF59E0B)
                    }

                    Surface(
                        onClick = { onSelectOrder(order) },
                        shape = RoundedCornerShape(18.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, if (isCancelled) Color(0xFFEF4444).copy(alpha = 0.3f) else cardBorder),
                        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Payment Method Icon Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = methodColor.copy(alpha = 0.13f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = methodIcon,
                                        contentDescription = null,
                                        tint = methodColor,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("#${order.invoiceNumber}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Spacer(Modifier.width(6.dp))
                                    OrderStatusPill(status = order.paymentStatus)
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(order.customerName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                if (order.itemsSummary.isNotBlank()) {
                                    Text(
                                        text = order.itemsSummary,
                                        fontSize = 11.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(dateFormat.format(Date(order.createdAt)), fontSize = 10.5.sp, color = textSecondary.copy(alpha = 0.75f))
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(order.totalAmountUsd, currency, exchangeRate),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCancelled) textSecondary else NawanoPrimary200
                                )
                                Text(
                                    text = order.paymentMethod.name.replace('_', ' '),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = methodColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkedCartsList(
    parkedCarts: List<ParkedCartEntity>,
    exchangeRate: Double,
    currency: AppCurrency,
    isDarkTheme: Boolean = false,
    onResume: (ParkedCartEntity) -> Unit,
    onDelete: (String) -> Unit
) {
    val cardBg = if (isDarkTheme) Color(0xFF161A2B) else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    if (parkedCarts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PauseCircleOutline, contentDescription = null, tint = textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(10.dp))
                Text("No parked carts", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text("Tap 'Hold Cart' on register to park an active order", fontSize = 12.sp, color = textSecondary.copy(alpha = 0.7f))
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(parkedCarts, key = { it.id }) { cart ->
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(cart.createdAt))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(cart.label, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                    Text("Parked at $timeStr • ${cart.itemCount} items", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(cart.totalAmountUsd, currency, exchangeRate),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Black,
                                color = NawanoPrimary200
                            )
                        }

                        if (cart.note.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text("Note: ${cart.note}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onDelete(cart.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Button(
                                onClick = { onResume(cart) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume Cart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ORDER RECEIPT & VOID VIEW
// ---------------------------------------------------------------------------

@Composable
private fun OrderDetailsView(
    order: SalesOrderEntity,
    exchangeRate: Double,
    currency: AppCurrency,
    onOpenReceipt: () -> Unit,
    onRefundPrompt: () -> Unit,
    onVoidPrompt: () -> Unit,
    onSettle: (PosPaymentMethod) -> Unit
) {
    val isCancelled = order.paymentStatus == PosPaymentStatus.CANCELLED
    val isFullyRefunded = order.paymentStatus == PosPaymentStatus.REFUNDED
    val canRefundOrVoid = !isCancelled && !isFullyRefunded
    val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy • hh:mm:ss a", Locale.getDefault())
    val lineItems = remember(order) { parseSalesOrderLineItems(order) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status & Metadata Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Customer: ${order.customerName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        OrderStatusPill(status = order.paymentStatus)
                    }
                    Text("Created: ${dateFormat.format(Date(order.createdAt))}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (order.splitDetails.isNotBlank()) {
                        Text("Split Breakdown: ${order.splitDetails}", fontSize = 12.sp, color = NawanoPrimary200, fontWeight = FontWeight.Medium)
                    }
                    if (isCancelled && order.cancelReason.isNotBlank()) {
                        Text("Void Reason: ${order.cancelReason}", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Itemized Order Lines
            if (lineItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ITEMS (${lineItems.sumOf { it.quantity }} pcs)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                        lineItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${item.quantity}x ${item.name}", fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(8.dp))
                                Text(CurrencyFormatter.format(item.lineTotalUsd, currency, exchangeRate), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Financial Breakdown Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("FINANCIAL RECEIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(2.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CurrencyFormatter.format(order.subtotalUsd, currency, exchangeRate), fontSize = 12.5.sp)
                    }

                    if (order.discountAmountUsd > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount (${order.discountType})", fontSize = 12.5.sp, color = Color(0xFF059669))
                            Text("-${CurrencyFormatter.format(order.discountAmountUsd, currency, exchangeRate)}", fontSize = 12.5.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (order.taxAmountUsd > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("VAT (10%)", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${CurrencyFormatter.format(order.taxAmountUsd, currency, exchangeRate)}", fontSize = 12.5.sp, color = NawanoPrimary200, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Grand Total", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(CurrencyFormatter.format(order.totalAmountUsd, currency, exchangeRate), fontSize = 17.sp, fontWeight = FontWeight.Black, color = NawanoPrimary200)
                    }

                    if (order.refundedAmountUsd > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Refunded (${order.refundMethod.ifBlank { "Cash" }})", fontSize = 12.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                            Text("-${CurrencyFormatter.format(order.refundedAmountUsd, currency, exchangeRate)}", fontSize = 13.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Refund / Issue Evidence Card (Shown when an issue/refund was reported)
            if (order.refundIssueType.isNotBlank() || order.refundProofImageUri.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF97316).copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("REFUND & ISSUE REPORT", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C), letterSpacing = 0.6.sp)
                        }
                        if (order.refundIssueType.isNotBlank()) {
                            Text("Issue: ${order.refundIssueType}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        if (order.refundConditionAnswer.isNotBlank()) {
                            Text("Condition: ${order.refundConditionAnswer} (${order.refundRestockAction})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (order.refundReason.isNotBlank()) {
                            Text("Reason: \"${order.refundReason}\"", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (order.refundProofImageUri.isNotBlank()) {
                            Text("Attached Proof Photo:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            AsyncImage(
                                model = order.refundProofImageUri,
                                contentDescription = "Issue proof photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row 1: View / Print Invoice Receipt
        OutlinedButton(
            onClick = onOpenReceipt,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View / Print Invoice Receipt", fontWeight = FontWeight.Bold)
        }

        // Action Buttons Row 2: Settle / Report Issue & Refund / Void
        if (canRefundOrVoid) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (order.paymentStatus == PosPaymentStatus.UNPAID || order.paymentStatus == PosPaymentStatus.PARTIAL) {
                    Button(
                        onClick = { onSettle(PosPaymentMethod.CASH) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Settle Tab", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onRefundPrompt,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Outlined.AssignmentReturn, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Refund / Issue", fontSize = 12.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onVoidPrompt,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .height(46.dp)
                ) {
                    Text("Void", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
