package com.example.inventoryapp.ui.supplier

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.SupplierPaymentMethod
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

data class DraftOrderItem(
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitCost: Double,
    val productId: String? = null
)

private enum class PoCheckoutStep {
    ORDER_DETAILS,
    PAYMENT_SELECTION,
    PAYMENT_CONFIRMATION
}

/**
 * Clean 2-Step Checkout-Style Restock Purchase Order Bottom Sheet:
 * Step 1: Supplier, Catalog Quick-Add, Cart-Style Item Steppers, Optional Custom Item & Delivery/Note
 * Step 2: Checkout-Style Payment Method Selection (ABA KHQR, Cash Upfront, COD, Net Terms)
 * Step 3: Payment Confirmation / KHQR Transfer Card & Send PO
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseOrderDialog(
    initialSupplier: SupplierEntity?,
    initialProduct: ProductEntity? = null,
    suppliers: List<SupplierEntity>,
    products: List<ProductEntity>,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onDismiss: () -> Unit,
    onSubmit: (
        supplier: SupplierEntity,
        title: String,
        picSender: String,
        items: List<PurchaseOrderItemEntity>,
        ownerNote: String,
        estArrival: String,
        paymentMethod: SupplierPaymentMethod
    ) -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val surfaceColor = if (isDarkTheme) NawanoDark200 else Color.White
    val cardColor = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val outlineColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    var currentStep by remember { mutableStateOf(PoCheckoutStep.ORDER_DETAILS) }
    var selectedSupplier by remember {
        mutableStateOf(
            initialSupplier
                ?: initialProduct?.let { p -> suppliers.find { it.id == p.supplierId } }
                ?: suppliers.firstOrNull()
        )
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val arrivalOptions = listOf("Today", "Next Business Day", "2-3 Days")
    var estArrival by remember { mutableStateOf("Next Business Day") }
    var ownerNote by remember { mutableStateOf("") }
    var showCustomItemForm by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(SupplierPaymentMethod.COD) }

    val partnerProducts = remember(selectedSupplier, products) {
        if (selectedSupplier == null) emptyList()
        else products.filter { it.supplierId == selectedSupplier?.id }
    }

    val draftItems = remember { mutableStateListOf<DraftOrderItem>() }

    fun populateItemsForSupplier(sup: SupplierEntity?, presetProduct: ProductEntity? = null) {
        draftItems.clear()
        if (sup == null) return
        val supItems = products.filter { it.supplierId == sup.id }
        val target = presetProduct?.takeIf { it.supplierId == sup.id } ?: supItems.firstOrNull()
        if (target != null) {
            draftItems.add(
                DraftOrderItem(
                    sku = target.sku,
                    name = target.name,
                    quantity = if (target.reorderQty > 0) target.reorderQty else 10,
                    unitCost = target.costPrice,
                    productId = target.id
                )
            )
        }
    }

    LaunchedEffect(selectedSupplier?.id) {
        if (draftItems.isEmpty()) {
            populateItemsForSupplier(selectedSupplier, initialProduct)
        }
    }

    // Manual custom item inputs
    var newSku by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("10") }
    var newCost by remember { mutableStateOf("25.00") }

    val totalAmountUsd = draftItems.sumOf { it.quantity * it.unitCost }
    val totalPieces = draftItems.sumOf { it.quantity }

    fun submitOrder() {
        val sup = selectedSupplier ?: return
        val finalItems = draftItems.map { d ->
            PurchaseOrderItemEntity(
                orderId = "",
                productId = d.productId,
                sku = d.sku,
                productName = d.name,
                quantity = d.quantity,
                unitCost = d.unitCost
            )
        }
        val autoTitle = when {
            finalItems.size == 1 -> "Restock: ${finalItems.first().productName}"
            finalItems.size > 1 -> "Restock: ${finalItems.first().productName} +${finalItems.size - 1}"
            else -> "Inbound from ${sup.name}"
        }
        onSubmit(
            sup,
            autoTitle,
            sup.contactName.ifBlank { "Supplier Courier" },
            finalItems,
            ownerNote.trim(),
            estArrival,
            selectedPaymentMethod
        )
    }

    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Unified Checkout-Style Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (currentStep) {
                        PoCheckoutStep.ORDER_DETAILS -> {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Restock Request", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                                Text("Step 1 of 2: Select supplier & items", fontSize = 11.5.sp, color = textSecondary)
                            }
                        }
                        PoCheckoutStep.PAYMENT_SELECTION -> {
                            IconButton(onClick = { currentStep = PoCheckoutStep.ORDER_DETAILS }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Pay Supplier", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                                Text("Step 2 of 2: Choose payment option", fontSize = 11.5.sp, color = textSecondary)
                            }
                        }
                        PoCheckoutStep.PAYMENT_CONFIRMATION -> {
                            IconButton(onClick = { currentStep = PoCheckoutStep.PAYMENT_SELECTION }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(selectedPaymentMethod.label, fontWeight = FontWeight.Bold, fontSize = 17.5.sp, color = textPrimary)
                                Text("Tap arrow to change payment method", fontSize = 11.5.sp, color = textSecondary)
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
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalAmountUsd)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                thickness = 1.dp,
                color = outlineColor.copy(alpha = 0.6f)
            )

            Crossfade(
                targetState = currentStep,
                animationSpec = tween(150),
                label = "PoCheckoutTransition",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { step ->
                when (step) {
                    // ==================================================
                    // STEP 1: CLEAN RESTOCK REQUEST SUMMARY
                    // ==================================================
                    PoCheckoutStep.ORDER_DETAILS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // 1. Supplier Selector Card
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "SUPPLIER",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        Surface(
                                            onClick = { isDropdownExpanded = !isDropdownExpanded },
                                            shape = RoundedCornerShape(14.dp),
                                            color = cardColor,
                                            border = BorderStroke(1.dp, if (isDropdownExpanded) NawanoPrimary200 else outlineColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val sup = selectedSupplier
                                                if (sup != null) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        val initials = sup.name.split(" ")
                                                            .mapNotNull { it.firstOrNull()?.toString() }
                                                            .take(2)
                                                            .joinToString("")
                                                            .ifBlank { "SU" }

                                                        Surface(
                                                            shape = CircleShape,
                                                            color = NawanoPrimary200.copy(alpha = 0.14f),
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text(initials, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = NawanoPrimary200)
                                                            }
                                                        }
                                                        Column {
                                                            Text(sup.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            if (sup.contactName.isNotBlank()) {
                                                                Text("Contact: ${sup.contactName}", fontSize = 11.5.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Text("Select Supplier", fontSize = 13.5.sp, color = textSecondary)
                                                }
                                                Icon(
                                                    imageVector = if (isDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Select Supplier",
                                                    tint = NawanoPrimary200,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = isDropdownExpanded,
                                            onDismissRequest = { isDropdownExpanded = false },
                                            modifier = Modifier
                                                .background(surfaceColor)
                                                .fillMaxWidth(0.85f)
                                        ) {
                                            suppliers.forEach { s ->
                                                val isSelected = selectedSupplier?.id == s.id
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(s.name, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) NawanoPrimary200 else textPrimary)
                                                            if (s.contactName.isNotBlank()) {
                                                                Text(s.contactName, fontSize = 10.5.sp, color = textSecondary)
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        selectedSupplier = s
                                                        isDropdownExpanded = false
                                                        populateItemsForSupplier(s)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Supplier Catalog Quick-Add Chips
                                if (partnerProducts.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "SUPPLIER CATALOG (${partnerProducts.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textSecondary,
                                            letterSpacing = 0.8.sp
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            partnerProducts.forEach { prod ->
                                                val isAlreadyInDraft = draftItems.any { it.sku == prod.sku }
                                                Surface(
                                                    onClick = {
                                                        val existingIndex = draftItems.indexOfFirst { it.sku == prod.sku }
                                                        if (existingIndex >= 0) {
                                                            val current = draftItems[existingIndex]
                                                            draftItems[existingIndex] = current.copy(quantity = current.quantity + 5)
                                                        } else {
                                                            draftItems.add(
                                                                DraftOrderItem(
                                                                    sku = prod.sku,
                                                                    name = prod.name,
                                                                    quantity = if (prod.reorderQty > 0) prod.reorderQty else 10,
                                                                    unitCost = prod.costPrice,
                                                                    productId = prod.id
                                                                )
                                                            )
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isAlreadyInDraft) NawanoPrimary200.copy(alpha = 0.14f) else cardColor,
                                                    border = BorderStroke(1.dp, if (isAlreadyInDraft) NawanoPrimary200 else outlineColor)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isAlreadyInDraft) Icons.Default.Check else Icons.Default.Add,
                                                            contentDescription = "Add",
                                                            tint = NawanoPrimary200,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(Modifier.width(5.dp))
                                                        Text(
                                                            text = "${prod.name} ($${String.format(Locale.US, "%.2f", prod.costPrice)})",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = if (isAlreadyInDraft) FontWeight.Bold else FontWeight.Medium,
                                                            color = textPrimary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Cart-Summary Style Order Items with - Qty + Steppers
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RESTOCK ITEMS (${draftItems.size} products • $totalPieces units)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textSecondary,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = if (showCustomItemForm) "Hide Custom" else "+ Custom Item",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NawanoPrimary200,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { showCustomItemForm = !showCustomItemForm }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (draftItems.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = cardColor,
                                            border = BorderStroke(1.dp, outlineColor),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Tap a catalog product above or '+ Custom Item' to add restock items.",
                                                fontSize = 12.5.sp,
                                                color = textSecondary,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    } else {
                                        draftItems.forEachIndexed { idx, item ->
                                            val lineTotal = item.quantity * item.unitCost
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = cardColor,
                                                border = BorderStroke(1.dp, outlineColor),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.name,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                            text = "${item.sku} • $${String.format(Locale.US, "%.2f", item.unitCost)} / unit",
                                                            fontSize = 11.sp,
                                                            color = textSecondary
                                                        )
                                                    }

                                                    // Quantity Stepper
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = outlineColor.copy(alpha = 0.55f),
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clickable {
                                                                    if (item.quantity <= 1) {
                                                                        draftItems.removeAt(idx)
                                                                    } else {
                                                                        draftItems[idx] = item.copy(quantity = item.quantity - 1)
                                                                    }
                                                                }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = if (item.quantity <= 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                                                    contentDescription = "Decrease",
                                                                    tint = if (item.quantity <= 1) Color(0xFFEF4444) else textPrimary,
                                                                    modifier = Modifier.size(15.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = "${item.quantity}",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = textPrimary
                                                        )
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = NawanoPrimary200,
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clickable {
                                                                    draftItems[idx] = item.copy(quantity = item.quantity + 1)
                                                                }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(15.dp))
                                                            }
                                                        }
                                                    }

                                                    Spacer(Modifier.width(10.dp))

                                                    Text(
                                                        text = "$${String.format(Locale.US, "%.2f", lineTotal)}",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = NawanoPrimary200
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Collapsible Custom Item Form (Only shown when toggled)
                                    AnimatedVisibility(visible = showCustomItemForm) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = cardColor,
                                            border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Add Unlisted / Custom Item", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = newSku,
                                                        onValueChange = { newSku = it },
                                                        label = { Text("SKU") },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = newName,
                                                        onValueChange = { newName = it },
                                                        label = { Text("Item Name *") },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1.8f)
                                                    )
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = newQty,
                                                        onValueChange = { newQty = it.filter { ch -> ch.isDigit() } },
                                                        label = { Text("Qty") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = newCost,
                                                        onValueChange = { newCost = it },
                                                        label = { Text("Unit Cost ($)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1.2f)
                                                    )
                                                    Button(
                                                        onClick = {
                                                            val q = (newQty.toIntOrNull() ?: 1).coerceAtLeast(1)
                                                            val c = (newCost.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                                                            if (newName.isNotBlank()) {
                                                                draftItems.add(
                                                                    DraftOrderItem(
                                                                        sku = newSku.ifBlank { "SKU-${System.currentTimeMillis() % 1000}" },
                                                                        name = newName.trim(),
                                                                        quantity = q,
                                                                        unitCost = c
                                                                    )
                                                                )
                                                                newSku = ""
                                                                newName = ""
                                                                newQty = "10"
                                                                newCost = "25.00"
                                                                showCustomItemForm = false
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                                        modifier = Modifier
                                                            .height(50.dp)
                                                            .align(Alignment.CenterVertically)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Expected Delivery & Optional Note (Clean Chips)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "EXPECTED DELIVERY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        arrivalOptions.forEach { option ->
                                            val selected = estArrival == option
                                            Surface(
                                                onClick = { estArrival = option },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (selected) NawanoPrimary200 else cardColor,
                                                border = BorderStroke(1.dp, if (selected) NawanoPrimary200 else outlineColor),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = option,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) Color.White else textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = ownerNote,
                                        onValueChange = { ownerNote = it },
                                        label = { Text("Note to Supplier (Optional)") },
                                        placeholder = { Text("e.g., Deliver to rear loading bay") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Sticky Checkout-Style Footer CTA
                            Surface(
                                tonalElevation = 4.dp,
                                shadowElevation = 8.dp,
                                color = surfaceColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { currentStep = PoCheckoutStep.PAYMENT_SELECTION },
                                    enabled = selectedSupplier != null && draftItems.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$${String.format(Locale.US, "%,.2f", totalAmountUsd)}",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================================================
                    // STEP 2: CHECKOUT-STYLE PAYMENT SELECTION PAGE
                    // ==================================================
                    PoCheckoutStep.PAYMENT_SELECTION -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Select Supplier Payment Method",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )

                            // 1. ABA KHQR / Bank
                            PoPaymentMethodCard(
                                title = SupplierPaymentMethod.ABA_KHQR.label,
                                subtitle = SupplierPaymentMethod.ABA_KHQR.subtitle,
                                cardColor = cardColor,
                                outlineColor = outlineColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF005B7F)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("ABA", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFE11D48))
                                                    .padding(vertical = 1.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("KHQR", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPaymentMethod = SupplierPaymentMethod.ABA_KHQR
                                    currentStep = PoCheckoutStep.PAYMENT_CONFIRMATION
                                }
                            )

                            // 2. Cash Upfront
                            PoPaymentMethodCard(
                                title = SupplierPaymentMethod.CASH.label,
                                subtitle = SupplierPaymentMethod.CASH.subtitle,
                                cardColor = cardColor,
                                outlineColor = outlineColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                },
                                onClick = {
                                    selectedPaymentMethod = SupplierPaymentMethod.CASH
                                    currentStep = PoCheckoutStep.PAYMENT_CONFIRMATION
                                }
                            )

                            // 3. Cash on Delivery (COD)
                            PoPaymentMethodCard(
                                title = SupplierPaymentMethod.COD.label,
                                subtitle = SupplierPaymentMethod.COD.subtitle,
                                cardColor = cardColor,
                                outlineColor = outlineColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                },
                                onClick = {
                                    selectedPaymentMethod = SupplierPaymentMethod.COD
                                    currentStep = PoCheckoutStep.PAYMENT_CONFIRMATION
                                }
                            )

                            // 4. Net Terms
                            PoPaymentMethodCard(
                                title = SupplierPaymentMethod.NET_TERMS.label,
                                subtitle = SupplierPaymentMethod.NET_TERMS.subtitle,
                                cardColor = cardColor,
                                outlineColor = outlineColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                logoContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(NawanoPrimary200),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.EventNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                },
                                onClick = {
                                    selectedPaymentMethod = SupplierPaymentMethod.NET_TERMS
                                    currentStep = PoCheckoutStep.PAYMENT_CONFIRMATION
                                }
                            )

                            Spacer(Modifier.height(6.dp))

                            // Order Summary Reminder Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardColor,
                                border = BorderStroke(1.dp, outlineColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("ORDER SUMMARY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${selectedSupplier?.name ?: "Supplier"} • ${draftItems.size} items ($totalPieces units)",
                                            fontSize = 13.sp,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%,.2f", totalAmountUsd)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = NawanoPrimary200
                                    )
                                }
                            }
                        }
                    }

                    // ==================================================
                    // STEP 3: PAYMENT CONFIRMATION & SUBMIT
                    // ==================================================
                    PoCheckoutStep.PAYMENT_CONFIRMATION -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (selectedPaymentMethod == SupplierPaymentMethod.ABA_KHQR) {
                                // ABA KHQR Transfer Card
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, outlineColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFFE11D48))
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("KHQR • SUPPLIER PAY", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
                                        }
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = selectedSupplier?.name ?: "Supplier",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "$${String.format(Locale.US, "%,.2f", totalAmountUsd)}",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF005B7F)
                                            )
                                            Text(
                                                text = "≈ ${String.format(Locale.US, "%,d", (totalAmountUsd * 4000).toLong())} ៛",
                                                fontSize = 12.5.sp,
                                                color = Color(0xFF64748B)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.size(120.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.QrCode2, contentDescription = "ABA KHQR", tint = Color.White, modifier = Modifier.size(92.dp))
                                                }
                                            }
                                            Text(
                                                text = "Scan or transfer to supplier ABA account, then confirm below",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = cardColor,
                                border = BorderStroke(1.dp, outlineColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("PURCHASE ORDER REVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 0.8.sp)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Supplier", fontSize = 13.sp, color = textSecondary)
                                        Text(selectedSupplier?.name ?: "-", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Items", fontSize = 13.sp, color = textSecondary)
                                        Text("${draftItems.size} SKUs ($totalPieces units)", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Expected Delivery", fontSize = 13.sp, color = textSecondary)
                                        Text(estArrival, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Payment Option", fontSize = 13.sp, color = textSecondary)
                                        Text(selectedPaymentMethod.label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                                    }
                                    HorizontalDivider(color = outlineColor)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Total Payable", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        Text("$${String.format(Locale.US, "%,.2f", totalAmountUsd)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NawanoPrimary200)
                                    }
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = { submitOrder() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedPaymentMethod == SupplierPaymentMethod.COD) Color(0xFFF59E0B) else NawanoPrimary200
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (selectedPaymentMethod == SupplierPaymentMethod.ABA_KHQR || selectedPaymentMethod == SupplierPaymentMethod.CASH)
                                        "Confirm Paid & Send PO"
                                    else
                                        "Confirm & Send Purchase Order",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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
private fun PoPaymentMethodCard(
    title: String,
    subtitle: String,
    cardColor: Color,
    outlineColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    logoContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        border = BorderStroke(1.dp, outlineColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            logoContent()
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = textSecondary)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = outlineColor.copy(alpha = 0.45f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
