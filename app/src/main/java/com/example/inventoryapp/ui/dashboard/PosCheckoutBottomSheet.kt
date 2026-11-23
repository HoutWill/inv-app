package com.example.inventoryapp.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutBottomSheet(
    totalAmountUsd: Double,
    itemCount: Int,
    itemsSummary: String,
    onDismiss: () -> Unit,
    onConfirmSale: (
        paymentMethod: PosPaymentMethod,
        customerName: String,
        customerPhone: String,
        dueDate: Long?,
        note: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    val totalAmountKhr = (totalAmountUsd * 4050.0).toLong()
    val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(totalAmountKhr)

    // Selection starts as null: displays the 3 payment method cards exactly like Figma Image 2
    var selectedMethod by remember { mutableStateOf<PosPaymentMethod?>(null) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var dueDaysOption by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }
    var cashTenderedText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalAmountUsd)) }

    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    // Intercept back button to return to payment methods list if a method is open
    BackHandler(enabled = selectedMethod != null) {
        selectedMethod = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        // Fixed container height prevents M3 AnchoredDraggableState recalculation glitches
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Bar
            if (selectedMethod == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "POS Checkout",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$itemCount items in cart",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NawanoPrimary200.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                            Text(
                                text = "≈ $khrFormatted ៛",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { selectedMethod = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to payment methods",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (selectedMethod) {
                                    PosPaymentMethod.ABA_KHQR -> "ABA KHQR"
                                    PosPaymentMethod.CASH -> "Cash Payment"
                                    PosPaymentMethod.COD -> "Cash on Delivery"
                                    else -> ""
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap arrow to change method",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NawanoPrimary200.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Dynamic Animated Content Area
            AnimatedContent(
                targetState = selectedMethod,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "PaymentScreenTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { method ->
                when (method) {
                    null -> {
                        // Payment Method Selection List strictly following Figma Image 2
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Select Payment Method",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 1. ABA KHQR (Figma Guideline Mockup)
                            PaymentMethodCard(
                                title = "ABA KHQR",
                                subtitle = "Scan to pay with any banking app",
                                logoContent = { AbaKhqrLogoBadge() },
                                onClick = { selectedMethod = PosPaymentMethod.ABA_KHQR }
                            )

                            // 2. Cash
                            PaymentMethodCard(
                                title = "Cash",
                                subtitle = "Pay directly at register with USD or KHR",
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Payments,
                                            contentDescription = "Cash",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                onClick = { selectedMethod = PosPaymentMethod.CASH }
                            )

                            // 3. Cash on Delivery (COD)
                            PaymentMethodCard(
                                title = "Cash on Delivery (COD)",
                                subtitle = "Pay upon delivery or record customer debt",
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalShipping,
                                            contentDescription = "COD",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                onClick = { selectedMethod = PosPaymentMethod.COD }
                            )

                            Spacer(Modifier.height(12.dp))

                            // Order Summary Preview
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "ORDER SUMMARY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = itemsSummary,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    PosPaymentMethod.ABA_KHQR -> {
                        // Official ABA PayWay 2.11 Dynamic KHQR View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier.width(260.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // KHQR Red Header
                                    Surface(
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "KHQR",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                letterSpacing = 2.sp
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "NAWANO POS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)} / $khrFormatted ៛",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    // Dynamic QR Graphic Box
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.size(150.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(130.dp)
                                            )
                                            // Center ABA Bank emblem
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF005B82),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("ABA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Scan with any Bank or Wallet",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Expires in 02:59",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Supports ABA, ACLEDA, Wing, Sathapana & all Bakong apps",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    if (!isProcessing) {
                                        isProcessing = true
                                        onConfirmSale(
                                            PosPaymentMethod.ABA_KHQR,
                                            customerName.ifBlank { "Walk-in Customer" },
                                            customerPhone,
                                            null,
                                            note
                                        )
                                    }
                                },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Confirm KHQR Paid ($${String.format(Locale.US, "%.2f", totalAmountUsd)})",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PosPaymentMethod.CASH -> {
                        // Cash Tender View with Live Change Calculation
                        val tendered = cashTenderedText.toDoubleOrNull() ?: totalAmountUsd
                        val changeUsd = (tendered - totalAmountUsd).coerceAtLeast(0.0)
                        val changeKhr = (changeUsd * 4050.0).toLong()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Fast Cash Tender:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(totalAmountUsd, 5.0, 10.0, 20.0, 50.0).distinct().forEach { amt ->
                                    val isChosen = cashTenderedText == String.format(Locale.US, "%.2f", amt)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isChosen) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                cashTenderedText = String.format(Locale.US, "%.2f", amt)
                                            }
                                    ) {
                                        Text(
                                            text = "$${String.format(Locale.US, if (amt % 1.0 == 0.0) "%.0f" else "%.2f", amt)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = cashTenderedText,
                                onValueChange = { cashTenderedText = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Cash Received ($)") },
                                prefix = { Text("$ ") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Change Due Banner
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Change Due:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                                        Text(
                                            text = "≈ ${NumberFormat.getNumberInstance(Locale.US).format(changeKhr)} ៛",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", changeUsd)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (!isProcessing) {
                                        isProcessing = true
                                        onConfirmSale(
                                            PosPaymentMethod.CASH,
                                            customerName.ifBlank { "Walk-in Customer" },
                                            customerPhone,
                                            null,
                                            note
                                        )
                                    }
                                },
                                enabled = !isProcessing,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Complete Cash Sale ($${String.format(Locale.US, "%.2f", totalAmountUsd)})",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PosPaymentMethod.COD -> {
                        // Customer Debt / Pay Later fields for COD
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Item stock will be deducted now. Customer debt will be recorded in Unpaid Tabs.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("Customer Name *") },
                                placeholder = { Text("e.g. John Doe, Coffee Shop A") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Phone / Telegram / WhatsApp") },
                                placeholder = { Text("+855 12 345 678") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Payment Due In:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(7, 14, 30).forEach { days ->
                                    val isSelected = dueDaysOption == days
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { dueDaysOption = days },
                                        label = { Text("$days Days", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFD97706),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Delivery Address / Reference (Optional)") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (!isProcessing) {
                                        isProcessing = true
                                        val dueTimestamp = System.currentTimeMillis() + (dueDaysOption * 86_400_000L)
                                        onConfirmSale(
                                            PosPaymentMethod.COD,
                                            customerName,
                                            customerPhone,
                                            dueTimestamp,
                                            note
                                        )
                                    }
                                },
                                enabled = !isProcessing && customerName.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Confirm COD Order ($${String.format(Locale.US, "%.2f", totalAmountUsd)})",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Reusable Payment Option Card strictly matching the Figma Merchant Integration Guideline (Image 2):
 * - Circular logo with official brand colors (Figma node 1:1917)
 * - Title and subtitle with official guideline color #697386 (Figma node 1:1672)
 * - Subtle rounded chevron pill on the right
 */
@Composable
private fun PaymentMethodCard(
    title: String,
    subtitle: String,
    logoContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Logo Badge (46.dp)
            logoContent()

            Spacer(Modifier.width(16.dp))

            // Title + Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = Color(0xFF697386), // Exact grey from Figma Merchant Guideline Node 1:1672
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            // Trailing rounded chevron pill (Figma next_arrow)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF697386),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authentic ABA KHQR circular brand logo badge (Figma Node 1:1972):
 * - Circular shape (CircleShape)
 * - Upper navy section (#005E7B) with white "ABA" and red accent dot
 * - Lower red section (#EB2227) with white bold "KHQR"
 */
@Composable
private fun AbaKhqrLogoBadge() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF005E7B))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Upper: ABA brand (60% height)
            Box(
                modifier = Modifier
                    .weight(0.60f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "ABA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "’",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEB2227)
                    )
                }
            }

            // Lower: KHQR red banner (40% height)
            Box(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxWidth()
                    .background(Color(0xFFEB2227)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KHQR",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
