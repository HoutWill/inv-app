package com.example.inventoryapp.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.ProductImageHelper
import java.text.NumberFormat
import java.util.Locale

/**
 * Sequential Checkout Steps:
 * 1. ITEM_SUMMARY: Review items in cart, adjust quantities, verify subtotal.
 * 2. PAYMENT_SELECTION: Choose payment method (ABA KHQR, Cash, COD) matching Figma guidelines.
 * 3. PAYMENT_PROCESSING: Process payment (PayWay 2.11 QR, Cash Tender, COD Debt).
 */
enum class PosCheckoutStep {
    ITEM_SUMMARY,
    PAYMENT_SELECTION,
    PAYMENT_PROCESSING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCheckoutBottomSheet(
    totalAmountUsd: Double,
    itemCount: Int,
    cartItems: List<Pair<ProductEntity, Int>> = emptyList(),
    onUpdateQuantity: (ProductEntity, Int) -> Unit = { _, _ -> },
    itemsSummary: String = "",
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
    var isProcessing by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(PosCheckoutStep.ITEM_SUMMARY) }
    var selectedMethod by remember { mutableStateOf<PosPaymentMethod?>(null) }

    val totalAmountKhr = (totalAmountUsd * 4050.0).toLong()
    val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(totalAmountKhr)

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var dueDaysOption by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }
    var cashTenderedText by remember { mutableStateOf(String.format(Locale.US, "%.2f", totalAmountUsd)) }

    // Intercept back button to navigate backwards through the multi-step checkout
    BackHandler(enabled = true) {
        when (currentStep) {
            PosCheckoutStep.PAYMENT_PROCESSING -> {
                currentStep = PosCheckoutStep.PAYMENT_SELECTION
            }
            PosCheckoutStep.PAYMENT_SELECTION -> {
                currentStep = PosCheckoutStep.ITEM_SUMMARY
            }
            PosCheckoutStep.ITEM_SUMMARY -> {
                onDismiss()
            }
        }
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
        // Locked height prevents M3 AnchoredDraggableState recalculation glitches
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Bar with dynamic back navigation and title
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
                    when (currentStep) {
                        PosCheckoutStep.ITEM_SUMMARY -> {
                            // Close/Dismiss button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Checkout",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Cart Summary",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Step 1 of 2: Review items in cart",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        PosCheckoutStep.PAYMENT_SELECTION -> {
                            // Back to Item Summary
                            IconButton(
                                onClick = { currentStep = PosCheckoutStep.ITEM_SUMMARY },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Item Summary",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Payment Method",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Step 2 of 2: Choose payment option",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        PosCheckoutStep.PAYMENT_PROCESSING -> {
                            // Back to Payment Selection
                            IconButton(
                                onClick = { currentStep = PosCheckoutStep.PAYMENT_SELECTION },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Payment Selection",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = when (selectedMethod) {
                                        PosPaymentMethod.ABA_KHQR -> "ABA KHQR"
                                        PosPaymentMethod.CASH -> "Cash Payment"
                                        PosPaymentMethod.COD -> "Cash on Delivery"
                                        else -> "Payment"
                                    },
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap arrow to choose another method",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Total Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NawanoPrimary200.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                        Text(
                            text = "≈ $khrFormatted ៛",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Animated Screen Transition between Steps
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(tween(220)) { width -> width } + fadeIn(tween(180))).togetherWith(
                            slideOutHorizontally(tween(200)) { width -> -width } + fadeOut(tween(160))
                        )
                    } else {
                        (slideInHorizontally(tween(220)) { width -> -width } + fadeIn(tween(180))).togetherWith(
                            slideOutHorizontally(tween(200)) { width -> width } + fadeOut(tween(160))
                        )
                    }
                },
                label = "CheckoutStepTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { step ->
                when (step) {
                    // ==========================================
                    // STEP 1: ITEM SUMMARY (REVIEW CART)
                    // ==========================================
                    PosCheckoutStep.ITEM_SUMMARY -> {
                        ItemSummaryView(
                            cartItems = cartItems,
                            totalAmountUsd = totalAmountUsd,
                            khrFormatted = khrFormatted,
                            onUpdateQuantity = onUpdateQuantity,
                            onProceedToPayment = {
                                currentStep = PosCheckoutStep.PAYMENT_SELECTION
                            }
                        )
                    }

                    // ==========================================
                    // STEP 2: PAYMENT METHOD SELECTION (FIGMA)
                    // ==========================================
                    PosCheckoutStep.PAYMENT_SELECTION -> {
                        PaymentSelectionView(
                            totalAmountUsd = totalAmountUsd,
                            khrFormatted = khrFormatted,
                            itemCount = itemCount,
                            onSelectMethod = { method ->
                                selectedMethod = method
                                currentStep = PosCheckoutStep.PAYMENT_PROCESSING
                            }
                        )
                    }

                    // ==========================================
                    // STEP 3: PAYMENT PROCESSING (ABA / CASH / COD)
                    // ==========================================
                    PosCheckoutStep.PAYMENT_PROCESSING -> {
                        when (selectedMethod) {
                            PosPaymentMethod.ABA_KHQR -> {
                                AbaKhqrPaymentView(
                                    totalAmountUsd = totalAmountUsd,
                                    khrFormatted = khrFormatted,
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.ABA_KHQR,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.CASH -> {
                                CashPaymentView(
                                    totalAmountUsd = totalAmountUsd,
                                    cashTenderedText = cashTenderedText,
                                    onTenderChange = { cashTenderedText = it },
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.CASH,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.COD -> {
                                CodPaymentView(
                                    totalAmountUsd = totalAmountUsd,
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    dueDaysOption = dueDaysOption,
                                    note = note,
                                    onNameChange = { customerName = it },
                                    onPhoneChange = { customerPhone = it },
                                    onDaysChange = { dueDaysOption = it },
                                    onNoteChange = { note = it },
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
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
                                    }
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No payment method selected")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 1: Item Summary View
 * Displays all cart items with images, unit prices, quantity steppers, and line totals.
 * Includes prominent CTA to confirm summary and advance to payment selection.
 */
@Composable
private fun ItemSummaryView(
    cartItems: List<Pair<ProductEntity, Int>>,
    totalAmountUsd: Double,
    khrFormatted: String,
    onUpdateQuantity: (ProductEntity, Int) -> Unit,
    onProceedToPayment: () -> Unit
) {
    val totalPieces = cartItems.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Your cart is empty",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Scrollable Item List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ORDER ITEMS (${cartItems.size} products • $totalPieces pcs)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                cartItems.forEach { (product, qty) ->
                    val lineTotalUsd = product.sellingPrice * qty
                    val lineTotalKhr = (lineTotalUsd * 4050.0).toLong()

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Image
                            val imageModel = remember(product.sku, product.name, product.category, product.imageUri) {
                                ProductImageHelper.getProductImageModel(
                                    sku = product.sku,
                                    name = product.name,
                                    category = product.category,
                                    imageUri = product.imageUri
                                )
                            }
                            AsyncImage(
                                model = imageModel,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Spacer(Modifier.width(12.dp))

                            // Name & Unit Price
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)} / ${product.unit.ifBlank { "unit" }}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Stepper Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { onUpdateQuantity(product, qty - 1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (qty == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                            contentDescription = "Reduce",
                                            tint = if (qty == 1) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "$qty",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = NawanoPrimary200.copy(alpha = if (qty < product.quantity) 1f else 0.4f),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable(enabled = qty < product.quantity) {
                                            onUpdateQuantity(product, qty + 1)
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // Line Total
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", lineTotalUsd)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200
                                )
                                Text(
                                    text = "≈ ${NumberFormat.getNumberInstance(Locale.US).format(lineTotalKhr)} ៛",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Breakdown Calculation Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$${String.format(Locale.US, "%.2f", totalAmountUsd)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Official Exchange Rate", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1 USD = 4,050 KHR", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Payable", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NawanoPrimary200
                                )
                                Text(
                                    text = "≈ $khrFormatted ៛",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Bottom CTA Button: Proceed to Payment
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = onProceedToPayment,
                    enabled = cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confirm & Proceed to Payment",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Step 2: Payment Method Selection View
 * Strictly implements the Figma Merchant Integration Guideline:
 * - Circular logo badge (46dp)
 * - Method Title (15.5sp Bold)
 * - Subtitle description (#697386)
 * - Trailing rounded chevron pill (>)
 */
@Composable
private fun PaymentSelectionView(
    totalAmountUsd: Double,
    khrFormatted: String,
    itemCount: Int,
    onSelectMethod: (PosPaymentMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Select Payment Method",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. ABA KHQR (Official PayWay 2.11 Mockup)
        PaymentMethodCard(
            title = "ABA KHQR",
            subtitle = "Scan to pay with any banking app",
            logoContent = { AbaKhqrLogoBadge() },
            onClick = { onSelectMethod(PosPaymentMethod.ABA_KHQR) }
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
            onClick = { onSelectMethod(PosPaymentMethod.CASH) }
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
            onClick = { onSelectMethod(PosPaymentMethod.COD) }
        )

        Spacer(Modifier.height(6.dp))

        // Order Summary Reminder Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$itemCount items in cart",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "$${String.format(Locale.US, "%.2f", totalAmountUsd)} / $khrFormatted ៛",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )
            }
        }
    }
}

/**
 * Step 3A: Official ABA PayWay 2.11 Dynamic KHQR Payment View
 */
@Composable
private fun AbaKhqrPaymentView(
    totalAmountUsd: Double,
    khrFormatted: String,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
            onClick = onConfirmPaid,
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

/**
 * Step 3B: Cash Tender & Live Change Calculation View
 */
@Composable
private fun CashPaymentView(
    totalAmountUsd: Double,
    cashTenderedText: String,
    onTenderChange: (String) -> Unit,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    val tendered = cashTenderedText.toDoubleOrNull() ?: totalAmountUsd
    val changeUsd = (tendered - totalAmountUsd).coerceAtLeast(0.0)
    val changeKhr = (changeUsd * 4050.0).toLong()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                    onClick = {
                        onTenderChange(String.format(Locale.US, "%.2f", amt))
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChosen) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
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
            onValueChange = { onTenderChange(it.filter { c -> c.isDigit() || c == '.' }) },
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
            onClick = onConfirmPaid,
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

/**
 * Step 3C: COD (Cash on Delivery) / Customer Debt Form
 */
@Composable
private fun CodPaymentView(
    totalAmountUsd: Double,
    customerName: String,
    customerPhone: String,
    dueDaysOption: Int,
    note: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onDaysChange: (Int) -> Unit,
    onNoteChange: (String) -> Unit,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
            onValueChange = onNameChange,
            label = { Text("Customer Name *") },
            placeholder = { Text("e.g. John Doe, Coffee Shop A") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = customerPhone,
            onValueChange = onPhoneChange,
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
                    onClick = { onDaysChange(days) },
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
            onValueChange = onNoteChange,
            label = { Text("Delivery Address / Reference (Optional)") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onConfirmPaid,
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
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Confirm COD Tab ($${String.format(Locale.US, "%.2f", totalAmountUsd)})",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Reusable Payment Method Card strictly following Figma Mockup:
 * - Circular logo badge (46.dp)
 * - Bold title (15.5.sp)
 * - Subtitle (#697386)
 * - Trailing rounded chevron pill (>)
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
        shape = RoundedCornerShape(18.dp),
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
