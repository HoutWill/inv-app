package com.example.inventoryapp.ui.dashboard

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.remote.payway.AbaPaywayService
import com.example.inventoryapp.data.remote.payway.PayWayQrResult
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import com.example.inventoryapp.util.ProductImageHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

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
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    paywayMerchantId: String = "ec000002",
    paywayApiKey: String = "",
    paywayIsSandbox: Boolean = true,
    onDismiss: () -> Unit,
    onConfirmSale: (
        paymentMethod: PosPaymentMethod,
        customerName: String,
        customerPhone: String,
        dueDate: Long?,
        note: String,
        subtotalUsd: Double,
        discountType: String,
        discountValue: Double,
        discountAmountUsd: Double,
        taxRate: Double,
        taxAmountUsd: Double,
        splitDetails: String
    ) -> Unit
) {
    var isProcessing by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(PosCheckoutStep.ITEM_SUMMARY) }
    var selectedMethod by remember { mutableStateOf<PosPaymentMethod?>(null) }

    val sheetState = rememberSafeModalBottomSheetState(
        skipPartiallyExpanded = true,
        extraConfirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                // Prevent accidental swipe-down dismiss on payment selection & payment processing screens.
                // Make it stay!
                !isProcessing && currentStep == PosCheckoutStep.ITEM_SUMMARY
            } else {
                true
            }
        }
    )

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var dueDaysOption by remember { mutableIntStateOf(14) }
    var note by remember { mutableStateOf("") }

    // Tax & Discount Engine Calculations
    val computedSubtotalUsd = if (cartItems.isNotEmpty()) {
        cartItems.sumOf { it.first.sellingPrice * it.second }
    } else {
        totalAmountUsd
    }

    var discountType by remember { mutableStateOf("NONE") } // "NONE", "PERCENT_5", "PERCENT_10", "PERCENT_15", "PERCENT_20", "PERCENT_CUSTOM", "FIXED"
    var customDiscountValueText by remember { mutableStateOf("") }
    var isVatEnabled by remember { mutableStateOf(false) }

    val discountAmountUsd = remember(computedSubtotalUsd, discountType, customDiscountValueText) {
        when (discountType) {
            "PERCENT_5" -> computedSubtotalUsd * 0.05
            "PERCENT_10" -> computedSubtotalUsd * 0.10
            "PERCENT_15" -> computedSubtotalUsd * 0.15
            "PERCENT_20" -> computedSubtotalUsd * 0.20
            "PERCENT_CUSTOM" -> {
                val pct = (customDiscountValueText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
                computedSubtotalUsd * (pct / 100.0)
            }
            "FIXED" -> {
                val fixed = customDiscountValueText.toDoubleOrNull() ?: 0.0
                fixed.coerceIn(0.0, computedSubtotalUsd)
            }
            else -> 0.0
        }
    }

    val discountValue = when (discountType) {
        "PERCENT_5" -> 5.0
        "PERCENT_10" -> 10.0
        "PERCENT_15" -> 15.0
        "PERCENT_20" -> 20.0
        "PERCENT_CUSTOM", "FIXED" -> customDiscountValueText.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    val taxableAmount = (computedSubtotalUsd - discountAmountUsd).coerceAtLeast(0.0)
    val taxRate = if (isVatEnabled) 0.10 else 0.0
    val taxAmountUsd = if (isVatEnabled) taxableAmount * taxRate else 0.0
    val finalTotalUsd = (taxableAmount + taxAmountUsd).coerceAtLeast(0.0)

    val defaultTender = if (currency == AppCurrency.USD) {
        String.format(Locale.US, "%.2f", finalTotalUsd)
    } else {
        "${CurrencyFormatter.toKhr(finalTotalUsd, exchangeRate)}"
    }
    var cashTenderedText by remember(finalTotalUsd, currency) { mutableStateOf(defaultTender) }

    ModalBottomSheet(
        onDismissRequest = {
            // Tapping outside scrim or backdrop will NOT dismiss during payment method selection or processing!
            // Make it stay! Only allow dismissal from Item Summary.
            if (!isProcessing && currentStep == PosCheckoutStep.ITEM_SUMMARY) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            if (currentStep == PosCheckoutStep.ITEM_SUMMARY) {
                BottomSheetDefaults.DragHandle(
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    width = 44.dp,
                    height = 5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            } else {
                // Fixed static indicator on payment method & processing to indicate locked stay mode
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        // Intercept back button INSIDE bottom sheet dialog to navigate backwards through multi-step checkout
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
                                        PosPaymentMethod.CARD -> "Credit/Debit Card"
                                        PosPaymentMethod.ALIPAY -> "Alipay"
                                        PosPaymentMethod.WECHAT -> "WeChat Pay"
                                        PosPaymentMethod.CASH -> "Cash Payment"
                                        PosPaymentMethod.COD -> "Cash on Delivery"
                                        PosPaymentMethod.SPLIT -> "Split Payment"
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
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = CurrencyFormatter.format(finalTotalUsd, currency, exchangeRate),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Stable screen transition between Steps without displacement gestures
            Crossfade(
                targetState = currentStep,
                animationSpec = tween(150),
                label = "CheckoutStepTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .consumeBottomSheetNestedDismiss()
            ) { step ->
                when (step) {
                    // ==========================================
                    // STEP 1: ITEM SUMMARY (REVIEW CART)
                    // ==========================================
                    PosCheckoutStep.ITEM_SUMMARY -> {
                        ItemSummaryView(
                            cartItems = cartItems,
                            subtotalUsd = computedSubtotalUsd,
                            discountType = discountType,
                            onDiscountTypeChange = { discountType = it },
                            customDiscountText = customDiscountValueText,
                            onCustomDiscountTextChange = { customDiscountValueText = it },
                            isVatEnabled = isVatEnabled,
                            onToggleVat = { isVatEnabled = it },
                            discountAmountUsd = discountAmountUsd,
                            taxAmountUsd = taxAmountUsd,
                            finalTotalUsd = finalTotalUsd,
                            currency = currency,
                            exchangeRate = exchangeRate,
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
                            totalAmountUsd = finalTotalUsd,
                            currency = currency,
                            exchangeRate = exchangeRate,
                            itemCount = itemCount,
                            onSelectMethod = { method ->
                                selectedMethod = method
                                currentStep = PosCheckoutStep.PAYMENT_PROCESSING
                            }
                        )
                    }

                    // ==========================================
                    // STEP 3: PAYMENT PROCESSING (ABA / CASH / COD / SPLIT)
                    // ==========================================
                    PosCheckoutStep.PAYMENT_PROCESSING -> {
                        when (selectedMethod) {
                            PosPaymentMethod.ABA_KHQR -> {
                                AbaKhqrPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    merchantId = paywayMerchantId,
                                    apiKey = paywayApiKey,
                                    isSandbox = paywayIsSandbox,
                                    itemsSummary = itemsSummary,
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.ABA_KHQR,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.CARD -> {
                                CreditDebitCardPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.CARD,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.ALIPAY -> {
                                AlipayPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.ALIPAY,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.WECHAT -> {
                                WeChatPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    isProcessing = isProcessing,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.WECHAT,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.CASH -> {
                                CashPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    cashTenderedText = cashTenderedText,
                                    onTenderChange = { cashTenderedText = it },
                                    isProcessing = isProcessing,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    onConfirmPaid = {
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.CASH,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.COD -> {
                                CodPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
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
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                ""
                                            )
                                        }
                                    }
                                )
                            }
                            PosPaymentMethod.SPLIT -> {
                                SplitPaymentView(
                                    totalAmountUsd = finalTotalUsd,
                                    currency = currency,
                                    exchangeRate = exchangeRate,
                                    isProcessing = isProcessing,
                                    onConfirmSplit = { splitDetails ->
                                        if (!isProcessing) {
                                            isProcessing = true
                                            onConfirmSale(
                                                PosPaymentMethod.SPLIT,
                                                "Walk-in Customer",
                                                "",
                                                null,
                                                note,
                                                computedSubtotalUsd,
                                                discountType,
                                                discountValue,
                                                discountAmountUsd,
                                                taxRate,
                                                taxAmountUsd,
                                                splitDetails
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
    subtotalUsd: Double,
    discountType: String,
    onDiscountTypeChange: (String) -> Unit,
    customDiscountText: String,
    onCustomDiscountTextChange: (String) -> Unit,
    isVatEnabled: Boolean,
    onToggleVat: (Boolean) -> Unit,
    discountAmountUsd: Double,
    taxAmountUsd: Double,
    finalTotalUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
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
            // Scrollable Item List & Financial Engine
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
                                    text = "${CurrencyFormatter.format(product.sellingPrice, currency, exchangeRate)} / ${product.unit.ifBlank { "unit" }}",
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
                                    text = CurrencyFormatter.format(lineTotalUsd, currency, exchangeRate),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // -----------------------------------------------------------
                // TAX & DISCOUNT ENGINE
                // -----------------------------------------------------------

                // 1. Discount Selector Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalOffer,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "DISCOUNT & PROMOTION",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            if (discountAmountUsd > 0.0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "-${CurrencyFormatter.format(discountAmountUsd, currency, exchangeRate)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Preset chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val discountPresets = listOf(
                                "NONE" to "None",
                                "PERCENT_5" to "5%",
                                "PERCENT_10" to "10%",
                                "PERCENT_15" to "15%",
                                "PERCENT_20" to "20%",
                                "PERCENT_CUSTOM" to "Custom %",
                                "FIXED" to "Flat $"
                            )

                            discountPresets.forEach { (typeKey, label) ->
                                val isSelected = discountType == typeKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.clickable { onDiscountTypeChange(typeKey) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Custom discount input when Custom % or Fixed $ is selected
                        if (discountType == "PERCENT_CUSTOM" || discountType == "FIXED") {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customDiscountText,
                                onValueChange = { onCustomDiscountTextChange(it.filter { c -> c.isDigit() || c == '.' }) },
                                label = { Text(if (discountType == "PERCENT_CUSTOM") "Enter Discount Percentage (%)" else "Enter Discount Amount ($)") },
                                leadingIcon = {
                                    Text(
                                        text = if (discountType == "PERCENT_CUSTOM") "%" else "$",
                                        fontWeight = FontWeight.Bold,
                                        color = NawanoPrimary200
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 2. Tax / VAT Toggle Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NawanoPrimary200.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Cambodia VAT (10%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isVatEnabled) "+${CurrencyFormatter.format(taxAmountUsd, currency, exchangeRate)} tax added"
                                           else "Standard Value Added Tax",
                                    fontSize = 11.sp,
                                    color = if (isVatEnabled) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isVatEnabled,
                            onCheckedChange = onToggleVat,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = NawanoPrimary200
                            )
                        )
                    }
                }

                // 3. Breakdown Calculation Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.format(subtotalUsd, currency, exchangeRate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Discount (if any)
                        if (discountAmountUsd > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Discount ($discountType)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF059669)
                                )
                                Text(
                                    text = "-${CurrencyFormatter.format(discountAmountUsd, currency, exchangeRate)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }

                        // VAT Tax (if enabled)
                        if (isVatEnabled && taxAmountUsd > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "VAT (10%)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${CurrencyFormatter.format(taxAmountUsd, currency, exchangeRate)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NawanoPrimary200
                                )
                            }
                        }

                        // Items Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Items Count", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${cartItems.size} products ($totalPieces pcs)",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Payable", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                val equiv = CurrencyFormatter.formatEquivalentOnly(finalTotalUsd, currency, exchangeRate)
                                if (equiv.isNotBlank()) {
                                    Text(
                                        text = "≈ $equiv",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = CurrencyFormatter.format(finalTotalUsd, currency, exchangeRate),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = NawanoPrimary200
                            )
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
                                text = CurrencyFormatter.format(finalTotalUsd, currency, exchangeRate),
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
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
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

        // 3. Split Payment
        PaymentMethodCard(
            title = "Split Payment",
            subtitle = "Split bill across multiple tenders (Cash, KHQR)",
            logoContent = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = "Split",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            onClick = { onSelectMethod(PosPaymentMethod.SPLIT) }
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
                    text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )
            }
        }
    }
}

/**
 * Step 3: Split Payment View
 * Allows dividing the total payable bill across multiple tenders (Cash, KHQR, Card, etc.)
 */
private data class SplitTenderEntry(
    val id: String = UUID.randomUUID().toString(),
    val method: PosPaymentMethod,
    val amountUsd: Double
)

@Composable
private fun SplitPaymentView(
    totalAmountUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    isProcessing: Boolean = false,
    onConfirmSplit: (splitDetails: String) -> Unit
) {
    val tenders = remember { mutableStateListOf<SplitTenderEntry>() }
    var selectedMethodForNewTender by remember { mutableStateOf(PosPaymentMethod.CASH) }

    val totalPaidUsd = tenders.sumOf { it.amountUsd }
    val remainingDueUsd = (totalAmountUsd - totalPaidUsd).coerceAtLeast(0.0)
    var tenderAmountInput by remember(remainingDueUsd) {
        mutableStateOf(if (remainingDueUsd > 0.0) String.format(Locale.US, "%.2f", remainingDueUsd) else "0.00")
    }

    val isFullyPaid = remainingDueUsd <= 0.005

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Balance Overview Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL BILL DUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isFullyPaid) Color(0xFF10B981).copy(alpha = 0.15f) else NawanoPrimary200.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isFullyPaid) "FULLY TENDERED" else "PARTIALLY PAID",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFullyPaid) Color(0xFF059669) else NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Progress Bar
                val progress = if (totalAmountUsd > 0) (totalPaidUsd / totalAmountUsd).toFloat().coerceIn(0f, 1f) else 1f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isFullyPaid) Color(0xFF10B981) else NawanoPrimary200,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Paid: ${CurrencyFormatter.format(totalPaidUsd, currency, exchangeRate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                    Text(
                        text = "Remaining: ${CurrencyFormatter.format(remainingDueUsd, currency, exchangeRate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingDueUsd > 0.005) Color(0xFFEF4444) else Color(0xFF059669)
                    )
                }
            }
        }

        // List of Added Split Tenders
        if (tenders.isNotEmpty()) {
            Text(
                text = "APPLIED TENDERS (${tenders.size})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )

            tenders.forEachIndexed { index, tender ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = when (tender.method) {
                                    PosPaymentMethod.CASH -> Color(0xFF10B981)
                                    PosPaymentMethod.ABA_KHQR -> Color(0xFF005E7B)
                                    PosPaymentMethod.CARD -> Color(0xFF0E5268)
                                    PosPaymentMethod.ALIPAY -> Color(0xFF1677FF)
                                    PosPaymentMethod.WECHAT -> Color(0xFF07C160)
                                    else -> NawanoPrimary200
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (tender.method) {
                                            PosPaymentMethod.CASH -> Icons.Outlined.Payments
                                            PosPaymentMethod.ABA_KHQR -> Icons.Default.QrCode2
                                            PosPaymentMethod.CARD -> Icons.Outlined.CreditCard
                                            else -> Icons.Outlined.AccountBalanceWallet
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tender #${index + 1}: ${tender.method.name.replace('_', ' ')}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = CurrencyFormatter.format(tender.amountUsd, currency, exchangeRate),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { tenders.removeAt(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove tender",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Tender Form (only if remaining balance > 0)
        if (!isFullyPaid) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ADD SPLIT TENDER",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        letterSpacing = 0.8.sp
                    )

                    // Method Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableSplitMethods = listOf(
                            PosPaymentMethod.CASH to "Cash",
                            PosPaymentMethod.ABA_KHQR to "KHQR",
                            PosPaymentMethod.CARD to "Card",
                            PosPaymentMethod.ALIPAY to "Alipay",
                            PosPaymentMethod.WECHAT to "WeChat"
                        )
                        availableSplitMethods.forEach { (m, label) ->
                            val isSelected = selectedMethodForNewTender == m
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.clickable { selectedMethodForNewTender = m }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Tender Amount Input
                    OutlinedTextField(
                        value = tenderAmountInput,
                        onValueChange = { tenderAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Tender Amount ($)") },
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = NawanoPrimary200) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Add Tender Button
                    val inputVal = tenderAmountInput.toDoubleOrNull() ?: 0.0
                    Button(
                        onClick = {
                            val validAmt = inputVal.coerceIn(0.01, remainingDueUsd)
                            tenders.add(SplitTenderEntry(method = selectedMethodForNewTender, amountUsd = validAmt))
                        },
                        enabled = inputVal > 0.0 && remainingDueUsd > 0.0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Add ${selectedMethodForNewTender.name.replace('_', ' ')} Tender ($${String.format(Locale.US, "%.2f", inputVal.coerceIn(0.0, remainingDueUsd))})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Finalize Split Payment CTA
        Button(
            onClick = {
                val details = tenders.joinToString(", ") { "${it.method.name}: \$${String.format(Locale.US, "%.2f", it.amountUsd)}" }
                onConfirmSplit(details)
            },
            enabled = isFullyPaid && !isProcessing,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isFullyPaid) "Complete Split Sale (${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)})"
                else "Pay Remaining ${CurrencyFormatter.format(remainingDueUsd, currency, exchangeRate)} to Complete",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun decodeBase64ToBitmap(base64Str: String?): ImageBitmap? {
    if (base64Str.isNullOrBlank()) return null
    return try {
        val clean = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bmp?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/**
 * Step 3A: Official ABA PayWay 2.11 Dynamic KHQR Payment View
 */
@Composable
private fun AbaKhqrPaymentView(
    totalAmountUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    merchantId: String = "ec000002",
    apiKey: String = "",
    isSandbox: Boolean = true,
    itemsSummary: String = "Nawano POS Order",
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var qrResult by remember { mutableStateOf<PayWayQrResult?>(null) }
    var isLoadingQr by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableIntStateOf(180) } // 3 minutes
    var isCheckingStatus by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isPaymentApproved by remember { mutableStateOf(false) }

    fun loadQr() {
        isLoadingQr = true
        statusMessage = null
        secondsRemaining = 180
        coroutineScope.launch {
            val amount = if (currency == AppCurrency.KHR) {
                CurrencyFormatter.toKhr(totalAmountUsd, exchangeRate).toDouble()
            } else {
                totalAmountUsd
            }
            val res = AbaPaywayService.generateKhqr(
                merchantId = merchantId,
                apiKey = apiKey,
                isSandbox = isSandbox,
                amount = amount,
                currency = currency.name,
                itemsSummary = itemsSummary.ifBlank { "POS Cart Order" }
            )
            qrResult = res
            isLoadingQr = false
        }
    }

    LaunchedEffect(totalAmountUsd, currency, merchantId, isSandbox) {
        loadQr()
    }

    // Countdown Timer
    LaunchedEffect(isLoadingQr, secondsRemaining) {
        if (!isLoadingQr && secondsRemaining > 0 && !isPaymentApproved) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

    // Automated Polling every 4 seconds if API key is present
    LaunchedEffect(qrResult?.tranId, apiKey, isPaymentApproved) {
        val tranId = qrResult?.tranId
        if (apiKey.isNotBlank() && tranId != null && !isPaymentApproved) {
            while (secondsRemaining > 0 && !isPaymentApproved) {
                delay(4000)
                val check = AbaPaywayService.checkTransaction(merchantId, apiKey, isSandbox, tranId)
                if (check.isApproved) {
                    isPaymentApproved = true
                    statusMessage = "Payment Approved! Completing sale..."
                    delay(1200)
                    onConfirmPaid()
                    break
                }
            }
        }
    }

    val qrBitmap = remember(qrResult?.qrImageBase64) {
        decodeBase64ToBitmap(qrResult?.qrImageBase64)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode indicator pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSandbox) NawanoPrimary200.copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isSandbox) "ABA PAYWAY SANDBOX" else "ABA PAYWAY LIVE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSandbox) NawanoPrimary200 else Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (qrResult?.isLiveApi == true) {
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "● CONNECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // KHQR Authentic Card
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
                    text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                val equiv = CurrencyFormatter.formatEquivalentOnly(totalAmountUsd, currency, exchangeRate)
                if (equiv.isNotBlank()) {
                    Text(
                        text = "≈ $equiv",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(8.dp))

                // QR Graphic Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.size(150.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLoadingQr) {
                            CircularProgressIndicator(color = NawanoPrimary200, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                        } else if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "ABA KHQR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Standard high-contrast vector KHQR representation
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
                }

                Spacer(Modifier.height(6.dp))
                qrResult?.tranId?.let { id ->
                    Text(
                        text = "Ref: #$id",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(Modifier.height(4.dp))
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                val timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                Text(
                    text = if (secondsRemaining > 0) "Expires in $timeStr" else "Expired",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (secondsRemaining > 30) Color(0xFFE11D48) else Color(0xFFDC2626),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Open in ABA Mobile Button (App-to-App Deeplink)
        qrResult?.deeplink?.let { dl ->
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dl))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ABA.Mobile"))
                            context.startActivity(storeIntent)
                        } catch (_: Exception) {}
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF005E7B).copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF005E7B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open in ABA Mobile App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
        }

        // Status Feedback (if checking)
        if (statusMessage != null) {
            Text(
                text = statusMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isPaymentApproved) Color(0xFF10B981) else NawanoPrimary200,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
        }

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check status on demand
            OutlinedButton(
                onClick = {
                    val tranId = qrResult?.tranId
                    if (tranId != null && apiKey.isNotBlank()) {
                        isCheckingStatus = true
                        statusMessage = "Checking with ABA PayWay..."
                        coroutineScope.launch {
                            val check = AbaPaywayService.checkTransaction(merchantId, apiKey, isSandbox, tranId)
                            isCheckingStatus = false
                            if (check.isApproved) {
                                isPaymentApproved = true
                                statusMessage = "Payment Approved! Completing sale..."
                                delay(1000)
                                onConfirmPaid()
                            } else {
                                statusMessage = "Status: ${check.statusMessage}"
                            }
                        }
                    } else if (secondsRemaining <= 0) {
                        loadQr()
                    } else {
                        statusMessage = "Simulated QR active (tap Confirm to finalize)"
                    }
                },
                enabled = !isCheckingStatus && !isProcessing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(0.38f)
                    .height(48.dp)
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (secondsRemaining <= 0) "Refresh" else "Check", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Confirm Paid
            Button(
                onClick = onConfirmPaid,
                enabled = !isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                modifier = Modifier
                    .weight(0.62f)
                    .height(48.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Confirm Paid",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onConfirmPaid: () -> Unit
) {
    val totalAmountInCurrency = if (currency == AppCurrency.USD) totalAmountUsd else CurrencyFormatter.toKhr(totalAmountUsd, exchangeRate).toDouble()
    val tendered = cashTenderedText.toDoubleOrNull() ?: totalAmountInCurrency
    val change = (tendered - totalAmountInCurrency).coerceAtLeast(0.0)
    val changeUsd = if (currency == AppCurrency.USD) change else (change / exchangeRate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Fast Cash Tender:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickAmounts = if (currency == AppCurrency.USD) {
                listOf(totalAmountUsd, 5.0, 10.0, 20.0, 50.0).distinct()
            } else {
                val totalKhr = CurrencyFormatter.toKhr(totalAmountUsd, exchangeRate).toDouble()
                listOf(totalKhr, 10000.0, 20000.0, 50000.0, 100000.0).distinct()
            }
            quickAmounts.forEach { amt ->
                val amtFormatted = if (currency == AppCurrency.USD) {
                    String.format(Locale.US, "%.2f", amt)
                } else {
                    "${amt.toLong()}"
                }
                val isChosen = cashTenderedText == amtFormatted
                val displayLabel = if (currency == AppCurrency.USD) {
                    "$${String.format(Locale.US, if (amt % 1.0 == 0.0) "%.0f" else "%.2f", amt)}"
                } else {
                    "${NumberFormat.getNumberInstance(Locale.US).format(amt.toLong())} ៛"
                }
                Surface(
                    onClick = {
                        onTenderChange(amtFormatted)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChosen) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = displayLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = cashTenderedText,
            onValueChange = { onTenderChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text("Cash Received (${currency.symbol})") },
            prefix = { Text("${currency.symbol} ") },
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
                Text("Change Due:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                Text(
                    text = CurrencyFormatter.format(changeUsd, currency, exchangeRate),
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
                        text = "Complete Cash Sale (${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)})",
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
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
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
                        text = "Confirm COD Tab (${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)})",
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
 * - Subtitle (#697386) or custom Subtitle Content (Card brand badges)
 * - Trailing rounded chevron pill (>)
 */
@Composable
private fun PaymentMethodCard(
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
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

            // Title + Subtitle or SubtitleContent
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                if (subtitleContent != null) {
                    subtitleContent()
                } else if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF697386), // Exact grey from Figma Merchant Guideline Node 1:1672
                        lineHeight = 16.sp
                    )
                }
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
 * Authentic Card Brand Badges strictly adhering to Figma Merchant Integration Guideline:
 * - Visa (Blue background, italicized bold white VISA)
 * - Mastercard (Dark background with interlocking red and orange circles)
 * - UnionPay (Red/Blue gradient block with white UnionPay text)
 * - JCB (White badge with blue, red, green stripes and JCB text)
 */
@Composable
private fun VisaBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFF1A1F71),
        modifier = modifier.height(18.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "VISA",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                letterSpacing = (-0.4).sp
            )
        }
    }
}

@Composable
private fun MastercardBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color(0xFF222222),
        modifier = modifier
            .width(28.dp)
            .height(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(20.dp, 12.dp)) {
                val circleRadius = 5.5.dp.toPx()
                val centerY = size.height / 2
                drawCircle(
                    color = Color(0xFFEB001B),
                    radius = circleRadius,
                    center = Offset(size.width * 0.38f, centerY)
                )
                drawCircle(
                    color = Color(0xFFF79E1B),
                    radius = circleRadius,
                    center = Offset(size.width * 0.62f, centerY)
                )
            }
        }
    }
}

@Composable
private fun UnionPayBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .width(32.dp)
            .height(18.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFE60012), Color(0xFF004B87), Color(0xFF007A3D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "UnionPay",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun JcbBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .width(26.dp)
            .height(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF003B91))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD81E06))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF007A3D))
                )
            }
            Text(
                text = "JCB",
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CardBrandsRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VisaBadge()
        MastercardBadge()
        UnionPayBadge()
        JcbBadge()
    }
}

@Composable
private fun CreditCardLogoBadge() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF0E5268)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = "Credit/Debit Card",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AlipayLogoBadge() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF1677FF)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "支",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

@Composable
private fun WeChatLogoBadge() {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xFF07C160)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(26.dp)) {
            // Main larger speech bubble (top-left)
            drawCircle(
                color = Color.White,
                radius = 7.5.dp.toPx(),
                center = Offset(10.dp.toPx(), 11.dp.toPx())
            )
            // Eyes on large bubble
            drawCircle(
                color = Color(0xFF07C160),
                radius = 1.2.dp.toPx(),
                center = Offset(8.dp.toPx(), 10.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF07C160),
                radius = 1.2.dp.toPx(),
                center = Offset(12.dp.toPx(), 10.dp.toPx())
            )

            // Smaller speech bubble (bottom-right)
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(17.dp.toPx(), 16.dp.toPx())
            )
            // Eyes on small bubble
            drawCircle(
                color = Color(0xFF07C160),
                radius = 1.0.dp.toPx(),
                center = Offset(15.5.dp.toPx(), 15.5.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF07C160),
                radius = 1.0.dp.toPx(),
                center = Offset(18.5.dp.toPx(), 15.5.dp.toPx())
            )
        }
    }
}

/**
 * Step 3C: Credit / Debit Card Payment Processing View
 */
@Composable
private fun CreditDebitCardPaymentView(
    totalAmountUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    var rawCardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardholderName by remember { mutableStateOf("") }
    var isNfcSimulating by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val detectedBrand = remember(rawCardNumber) {
        when {
            rawCardNumber.startsWith("4") -> "VISA"
            rawCardNumber.startsWith("5") -> "MASTERCARD"
            rawCardNumber.startsWith("62") -> "UNIONPAY"
            rawCardNumber.startsWith("35") -> "JCB"
            else -> null
        }
    }

    val formattedCardNumber = remember(rawCardNumber) {
        rawCardNumber.chunked(4).joinToString(" ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Luxury Visual Card Preview
        Surface(
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: EMV Chip + NFC Waves + Brand Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0xFFD4AF37),
                                border = BorderStroke(1.dp, Color(0xFFF3E5AB)),
                                modifier = Modifier.size(width = 34.dp, height = 26.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawLine(
                                            color = Color(0xFF996515),
                                            start = Offset(0f, size.height / 2),
                                            end = Offset(size.width, size.height / 2),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        drawLine(
                                            color = Color(0xFF996515),
                                            start = Offset(size.width / 2, 0f),
                                            end = Offset(size.width / 2, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Contactless",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        when (detectedBrand) {
                            "VISA" -> VisaBadge()
                            "MASTERCARD" -> MastercardBadge()
                            "UNIONPAY" -> UnionPayBadge()
                            "JCB" -> JcbBadge()
                            else -> CardBrandsRow()
                        }
                    }

                    // Middle: Card Number
                    Text(
                        text = if (formattedCardNumber.isNotBlank()) formattedCardNumber else "••••  ••••  ••••  ••••",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    // Bottom: Cardholder & Expiry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "CARDHOLDER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = cardholderName.ifBlank { "VALUED CUSTOMER" }.uppercase(Locale.US),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "EXPIRES",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (expiryDate.isNotBlank()) expiryDate else "MM/YY",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Fast Terminal Tap Simulator Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "POS Card Terminal / Contactless",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap or insert customer card",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        isNfcSimulating = true
                        rawCardNumber = "4532891234567890"
                        expiryDate = "12/28"
                        cvv = "888"
                        cardholderName = "Walk-in Customer"
                        coroutineScope.launch {
                            delay(600)
                            isNfcSimulating = false
                            onConfirmPaid()
                        }
                    },
                    enabled = !isProcessing && !isNfcSimulating,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (isNfcSimulating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Simulate Tap", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Manual Card Inputs
        OutlinedTextField(
            value = formattedCardNumber,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(16)
                rawCardNumber = digitsOnly
            },
            label = { Text("Card Number") },
            placeholder = { Text("4532 0000 0000 0000") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                when (detectedBrand) {
                    "VISA" -> VisaBadge(modifier = Modifier.padding(end = 6.dp))
                    "MASTERCARD" -> MastercardBadge(modifier = Modifier.padding(end = 6.dp))
                    "UNIONPAY" -> UnionPayBadge(modifier = Modifier.padding(end = 6.dp))
                    "JCB" -> JcbBadge(modifier = Modifier.padding(end = 6.dp))
                    else -> null
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(4)
                    expiryDate = if (digits.length >= 3) {
                        "${digits.substring(0, 2)}/${digits.substring(2)}"
                    } else {
                        digits
                    }
                },
                label = { Text("Expiry (MM/YY)") },
                placeholder = { Text("12/28") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = cvv,
                onValueChange = { input ->
                    cvv = input.filter { it.isDigit() }.take(4)
                },
                label = { Text("CVV / CVC") },
                placeholder = { Text("888") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = cardholderName,
            onValueChange = { cardholderName = it },
            label = { Text("Cardholder Name (Optional)") },
            placeholder = { Text("John Doe") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Authorize & Pay CTA
        Button(
            onClick = onConfirmPaid,
            enabled = !isProcessing && (rawCardNumber.length >= 12 || isNfcSimulating),
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
                        text = "Charge ${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)} to Card",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Step 3D: Alipay Cross-Border Payment Processing View
 */
@Composable
private fun AlipayPaymentView(
    totalAmountUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(180) }
    val cnyAmount = remember(totalAmountUsd) { totalAmountUsd * 7.24 }

    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color(0xFF1677FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "支 Alipay",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "NAWANO POS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "≈ ¥${String.format(Locale.US, "%.2f", cnyAmount)} CNY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1677FF)
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0A192F),
                    modifier = Modifier.size(150.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Alipay QR Code",
                            tint = Color.White,
                            modifier = Modifier.size(130.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1677FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("支", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Alipay+ Global QR",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (secondsRemaining < 30) Color(0xFFE11D48) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${secondsRemaining / 60}:${(secondsRemaining % 60).toString().padStart(2, '0')}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (secondsRemaining < 30) Color(0xFFE11D48) else Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlipayLogoBadge()
                Column {
                    Text(
                        text = "Scan with Alipay or Partner Wallets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Supports Alipay (CN), AlipayHK, KakaoPay, GCash & Touch 'n Go",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onConfirmPaid,
            enabled = !isProcessing,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1677FF)),
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
                        text = "Confirm Alipay Paid (${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)})",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Step 3E: WeChat Pay Payment Processing View
 */
@Composable
private fun WeChatPaymentView(
    totalAmountUsd: Double,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    isProcessing: Boolean,
    onConfirmPaid: () -> Unit
) {
    var secondsRemaining by remember { mutableIntStateOf(180) }
    val cnyAmount = remember(totalAmountUsd) { totalAmountUsd * 7.24 }

    LaunchedEffect(secondsRemaining) {
        if (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining -= 1
        }
    }

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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color(0xFF07C160),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "WeChat Pay (微信支付)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "NAWANO POS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "≈ ¥${String.format(Locale.US, "%.2f", cnyAmount)} CNY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF07C160)
                )

                Spacer(Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF062C18),
                    modifier = Modifier.size(150.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "WeChat Pay QR Code",
                            tint = Color.White,
                            modifier = Modifier.size(130.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF07C160),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(7.dp.toPx(), 8.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 4.dp.toPx(),
                                        center = Offset(12.dp.toPx(), 11.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = "WeChat Official QR Pay",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (secondsRemaining < 30) Color(0xFFE11D48) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${secondsRemaining / 60}:${(secondsRemaining % 60).toString().padStart(2, '0')}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (secondsRemaining < 30) Color(0xFFE11D48) else Color(0xFF1E293B)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeChatLogoBadge()
                Column {
                    Text(
                        text = "Scan with WeChat App",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Open WeChat > Scan > Pay directly from WeChat wallet",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onConfirmPaid,
            enabled = !isProcessing,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF07C160)),
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
                        text = "Confirm WeChat Paid (${CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)})",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
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
