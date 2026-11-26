package com.example.inventoryapp.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.PosPaymentStatus
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import com.example.inventoryapp.data.local.entity.SalesOrderLineItem
import com.example.inventoryapp.data.local.entity.parseReturnedQuantitiesMap
import com.example.inventoryapp.data.local.entity.parseSalesOrderLineItems
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import com.example.inventoryapp.ui.theme.consumeBottomSheetNestedDismiss
import com.example.inventoryapp.ui.theme.rememberSafeModalBottomSheetState
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Swipe-up ModalBottomSheet for "Report an Issue & Refund / Return".
 * Guides the cashier through:
 * 1. Selecting returned items & quantities (partial or full order)
 * 2. Answering structured "Report an Issue" questions (issue category & item condition)
 * 3. Writing a clear, auditable refund reason
 * 4. Attaching photo proof of the issue (Camera or Gallery saved locally)
 * 5. Choosing refund method & submitting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRefundIssueBottomSheet(
    order: SalesOrderEntity,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onDismiss: () -> Unit,
    onSubmitRefund: (
        returnedItems: List<SalesOrderLineItem>,
        issueType: String,
        conditionAnswer: String,
        reason: String,
        restockItems: Boolean,
        refundMethod: String,
        proofImageUri: String
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)

    val lineItems = remember(order) { parseSalesOrderLineItems(order) }
    val alreadyReturnedMap = remember(order) { parseReturnedQuantitiesMap(order.returnedItemsJson) }

    // Available returnable quantity per item key
    val returnableItems = remember(lineItems, alreadyReturnedMap) {
        lineItems.mapNotNull { item ->
            val key = item.productId.ifBlank { item.name }
            val alreadyReturned = alreadyReturnedMap[key] ?: 0
            val remaining = (item.quantity - alreadyReturned).coerceAtLeast(0)
            if (remaining > 0) item.copy(quantity = remaining) else null
        }
    }

    // Track selected return quantities per item index
    val selectedQuantities = remember(returnableItems) {
        mutableStateMapOf<Int, Int>().apply {
            returnableItems.forEachIndexed { index, item ->
                put(index, item.quantity) // Default to full remaining quantity; user can reduce for partial return
            }
        }
    }

    val issueOptions = listOf(
        "Damaged / Defective Item",
        "Expired / Spoiled Goods",
        "Wrong Item / Quantity",
        "Missing Item in Order",
        "Quality Not As Expected",
        "Customer Changed Mind"
    )
    var selectedIssue by remember { mutableStateOf(issueOptions.first()) }

    val conditionOptions = listOf(
        "Opened / Damaged / Defective",
        "Unopened & Sellable Condition",
        "Never Received / Missing"
    )
    var selectedCondition by remember { mutableStateOf(conditionOptions.first()) }
    var restockItems by remember { mutableStateOf(false) }

    var refundReason by remember { mutableStateOf("") }
    var proofImageUri by remember { mutableStateOf("") }
    var mediaError by remember { mutableStateOf<String?>(null) }

    val isUnpaidOrder = order.paymentStatus == PosPaymentStatus.UNPAID || order.paymentStatus == PosPaymentStatus.PARTIAL
    val refundMethodOptions = if (isUnpaidOrder) {
        listOf("Adjust Unpaid Tab Balance")
    } else {
        listOf("Cash", "ABA KHQR", "Store Credit")
    }
    var selectedRefundMethod by remember(isUnpaidOrder) { mutableStateOf(refundMethodOptions.first()) }

    // Camera capture launcher (saves local JPEG file)
    val takeProofPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "refund_proof_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                proofImageUri = file.toURI().toString()
                mediaError = null
            } catch (e: Exception) {
                mediaError = "Could not save proof photo: ${e.message}"
            }
        }
    }

    // Gallery picker launcher (copies into internal filesDir so proof persists reliably)
    val pickProofImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val file = File(context.filesDir, "refund_proof_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                proofImageUri = file.toURI().toString()
                mediaError = null
            } catch (e: Exception) {
                // Fallback to URI string if stream copy fails
                proofImageUri = uri.toString()
                mediaError = null
            }
        }
    }

    val selectedReturnList = returnableItems.mapIndexedNotNull { index, item ->
        val qty = selectedQuantities[index] ?: 0
        if (qty > 0) item.copy(quantity = qty) else null
    }
    val totalSelectedQty = selectedReturnList.sumOf { it.quantity }
    val totalReturnableQty = returnableItems.sumOf { it.quantity }

    val effectiveSubtotal = order.subtotalUsd.takeIf { it > 0.0 } ?: order.totalAmountUsd.coerceAtLeast(0.01)
    val ratioToGrandTotal = (order.totalAmountUsd / effectiveSubtotal).coerceAtLeast(0.0)
    val rawSelectedSubtotal = selectedReturnList.sumOf { it.lineTotalUsd }
    val estimatedRefundUsd = if (isUnpaidOrder) {
        (rawSelectedSubtotal * ratioToGrandTotal).coerceIn(0.0, order.remainingAmountUsd)
    } else {
        val maxPaidRemaining = (order.paidAmountUsd - order.refundedAmountUsd).coerceAtLeast(0.0)
        if (totalSelectedQty >= totalReturnableQty) {
            maxPaidRemaining
        } else {
            (rawSelectedSubtotal * ratioToGrandTotal).coerceIn(0.0, maxPaidRemaining)
        }
    }

    val isReasonValid = refundReason.trim().length >= 5
    val isProofAttached = proofImageUri.isNotBlank()
    val canSubmit = totalSelectedQty > 0 && isReasonValid && isProofAttached

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
            .padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.ReportProblem,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Report Issue & Refund",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Invoice #${order.invoiceNumber} • ${order.customerName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .consumeBottomSheetNestedDismiss()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ---------------------------------------------------------
                // 1. SELECT ITEMS TO RETURN
                // ---------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. SELECT ITEMS TO RETURN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = if (totalSelectedQty == totalReturnableQty) "Clear All" else "Select All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val selectAll = totalSelectedQty < totalReturnableQty
                                    returnableItems.forEachIndexed { idx, item ->
                                        selectedQuantities[idx] = if (selectAll) item.quantity else 0
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (returnableItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "All items on this order have already been returned.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        returnableItems.forEachIndexed { index, item ->
                            val qty = selectedQuantities[index] ?: 0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (qty > 0) NawanoPrimary200.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(
                                    1.dp,
                                    if (qty > 0) NawanoPrimary200.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Bought: ${item.quantity} × ${CurrencyFormatter.format(item.unitPriceUsd, currency, exchangeRate)}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable(enabled = qty > 0) {
                                                    selectedQuantities[index] = (qty - 1).coerceAtLeast(0)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(15.dp))
                                            }
                                        }
                                        Text(
                                            text = "$qty",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (qty > 0) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = NawanoPrimary200,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable(enabled = qty < item.quantity) {
                                                    selectedQuantities[index] = (qty + 1).coerceAtMost(item.quantity)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // 2. REPORT AN ISSUE QUESTIONS
                // ---------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "2. REPORT AN ISSUE QUESTIONNAIRE",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    Text(
                        text = "Q1. What issue happened with the item(s)?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    issueOptions.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { option ->
                                val selected = selectedIssue == option
                                Surface(
                                    onClick = {
                                        selectedIssue = option
                                        when (option) {
                                            "Customer Changed Mind" -> {
                                                selectedCondition = "Unopened & Sellable Condition"
                                                restockItems = true
                                            }
                                            "Missing Item in Order" -> {
                                                selectedCondition = "Never Received / Missing"
                                                restockItems = false
                                            }
                                            else -> {
                                                selectedCondition = "Opened / Damaged / Defective"
                                                restockItems = false
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) NawanoPrimary200.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Q2. Item condition & inventory handling?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    conditionOptions.forEach { cond ->
                        val selected = selectedCondition == cond
                        Surface(
                            onClick = {
                                selectedCondition = cond
                                restockItems = (cond == "Unopened & Sellable Condition")
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) NawanoPrimary200.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, if (selected) NawanoPrimary200 else Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        selectedCondition = cond
                                        restockItems = (cond == "Unopened & Sellable Condition")
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = NawanoPrimary200)
                                )
                                Text(text = cond, fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }

                    // Explicit Inventory Disposition toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (restockItems) Color(0xFF10B981).copy(alpha = 0.10f) else Color(0xFFEF4444).copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (restockItems) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (restockItems) "Restock back into inventory" else "Write-off as damaged / non-sellable",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (restockItems) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                                Text(
                                    text = if (restockItems) "Sellable stock count will increase by $totalSelectedQty" else "Logs audit loss without adding back to sellable stock",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = restockItems,
                                onCheckedChange = { restockItems = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF10B981))
                            )
                        }
                    }
                }

                // ---------------------------------------------------------
                // 3. GOOD REASON FOR REFUND
                // ---------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "3. GOOD REASON FOR REFUND (REQUIRED)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    // Quick reason templates matched to selected issue
                    val quickSuggestions = when (selectedIssue) {
                        "Damaged / Defective Item" -> listOf("Packaging crushed / leaking on handoff", "Broken seal noticed before use")
                        "Expired / Spoiled Goods" -> listOf("Passed best-before date on label", "Product spoiled / off odor upon opening")
                        "Wrong Item / Quantity" -> listOf("Wrong SKU packed during checkout", "Customer charged for extra quantity")
                        else -> listOf("Customer requested return within policy", "Verified with manager & customer receipt")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSuggestions.forEach { snippet ->
                            SuggestionChip(
                                onClick = { refundReason = snippet },
                                label = { Text(snippet, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it },
                        label = { Text("Explain the refund reason clearly") },
                        placeholder = { Text("e.g., Seal was torn and carton dented when customer inspected at counter") },
                        minLines = 2,
                        maxLines = 4,
                        isError = refundReason.isNotEmpty() && !isReasonValid,
                        supportingText = {
                            Text(
                                text = if (!isReasonValid) "Please enter at least 5 characters explaining the issue" else "Valid refund reason recorded ✓",
                                fontSize = 11.sp,
                                color = if (isReasonValid) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------------------------------------------------------
                // 4. IMAGE OF PROOF (CAMERA OR GALLERY)
                // ---------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. IMAGE OF PROOF (REQUIRED)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )
                        if (isProofAttached) {
                            Text(
                                text = "Proof Attached ✓",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(
                            1.dp,
                            if (isProofAttached) Color(0xFF10B981).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isProofAttached) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.08f))
                                ) {
                                    AsyncImage(
                                        model = proofImageUri,
                                        contentDescription = "Refund proof photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        onClick = { proofImageUri = "" },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove proof", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = NawanoPrimary200.copy(alpha = 0.14f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("Attach photo evidence", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                        Text("Take a clear photo of the damaged/wrong product or receipt", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { takeProofPhotoLauncher.launch(null) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Camera", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { pickProofImageLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                ) {
                                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Gallery", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (mediaError != null) {
                                Text(mediaError!!, fontSize = 11.5.sp, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // 5. REFUND PAYOUT METHOD
                // ---------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "5. REFUND PAYOUT METHOD",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        refundMethodOptions.forEach { method ->
                            val selected = selectedRefundMethod == method
                            Surface(
                                onClick = { selectedRefundMethod = method },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) NawanoPrimary200 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (selected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = method,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sticky Bottom Refund Summary & Submit Bar
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isUnpaidOrder) "Tab Balance Reduction ($totalSelectedQty pcs)" else "Refund Total ($totalSelectedQty pcs)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatWithEquivalent(estimatedRefundUsd, currency, exchangeRate),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626)
                            )
                        }
                        if (!canSubmit) {
                            Text(
                                text = when {
                                    totalSelectedQty == 0 -> "Select item(s)"
                                    !isReasonValid -> "Add refund reason"
                                    !isProofAttached -> "Attach proof photo"
                                    else -> ""
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSubmitRefund(
                                selectedReturnList,
                                selectedIssue,
                                selectedCondition,
                                refundReason.trim(),
                                restockItems,
                                selectedRefundMethod,
                                proofImageUri
                            )
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Outlined.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isUnpaidOrder) "Confirm Return & Adjust Tab" else "Submit Issue & Issue Refund",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Swipe-up ModalBottomSheet for Digital Invoice & Customer Receipt.
 * Displays itemized products, taxes, discounts, payment status, refund deductions,
 * and supports native Android Print / Save as PDF plus Share Receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosInvoiceReceiptBottomSheet(
    order: SalesOrderEntity,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onViewHistory: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val lineItems = remember(order) { parseSalesOrderLineItems(order) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = NawanoPrimary200.copy(alpha = 0.16f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = NawanoPrimary200, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Invoice & Receipt", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("#${order.invoiceNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onViewHistory != null) {
                        Surface(
                            onClick = onViewHistory,
                            shape = RoundedCornerShape(12.dp),
                            color = NawanoPrimary200.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Payment History",
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "History",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NawanoPrimary200
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Scrollable Receipt Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .consumeBottomSheetNestedDismiss()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Store Title & Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NAWANO POS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = NawanoPrimary200
                                )
                                Text(
                                    text = "Official Sales Invoice",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OrderStatusPill(status = order.paymentStatus)
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

                        // Metadata
                        ReceiptMetaRow("Invoice No", "#${order.invoiceNumber}")
                        ReceiptMetaRow("Date", dateFormat.format(Date(order.createdAt)))
                        ReceiptMetaRow("Customer", order.customerName)
                        if (order.customerPhone.isNotBlank()) {
                            ReceiptMetaRow("Phone", order.customerPhone)
                        }
                        ReceiptMetaRow("Payment", order.paymentMethod.name.replace('_', ' '))
                        if (order.splitDetails.isNotBlank()) {
                            ReceiptMetaRow("Split Info", order.splitDetails)
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

                        // Line Items Header
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("ITEM", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text("QTY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(38.dp), textAlign = TextAlign.Center)
                            Text("AMOUNT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp), textAlign = TextAlign.End)
                        }

                        lineItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${CurrencyFormatter.format(item.unitPriceUsd, currency, exchangeRate)} each",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${item.quantity}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(38.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = CurrencyFormatter.format(item.lineTotalUsd, currency, exchangeRate),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(96.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

                        // Financial Summary
                        ReceiptMetaRow("Subtotal", CurrencyFormatter.format(order.subtotalUsd, currency, exchangeRate))
                        if (order.discountAmountUsd > 0.0) {
                            ReceiptMetaRow(
                                "Discount (${order.discountType})",
                                "-${CurrencyFormatter.format(order.discountAmountUsd, currency, exchangeRate)}",
                                valueColor = Color(0xFF10B981)
                            )
                        }
                        if (order.taxAmountUsd > 0.0) {
                            ReceiptMetaRow(
                                "VAT (${(order.taxRate * 100).toInt()}%)",
                                "+${CurrencyFormatter.format(order.taxAmountUsd, currency, exchangeRate)}"
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grand Total", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.format(order.totalAmountUsd, currency, exchangeRate),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NawanoPrimary200
                                )
                                Text(
                                    text = "≈ ${CurrencyFormatter.formatEquivalentOnly(order.totalAmountUsd, currency, exchangeRate)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (order.refundedAmountUsd > 0.0) {
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                            ReceiptMetaRow(
                                "Refunded (${order.refundMethod.ifBlank { "Cash" }})",
                                "-${CurrencyFormatter.format(order.refundedAmountUsd, currency, exchangeRate)}",
                                valueColor = Color(0xFFDC2626)
                            )
                            val netUsd = (order.totalAmountUsd - order.refundedAmountUsd).coerceAtLeast(0.0)
                            ReceiptMetaRow(
                                "Net Kept",
                                CurrencyFormatter.format(netUsd, currency, exchangeRate)
                            )
                        } else if (order.remainingAmountUsd > 0.0) {
                            ReceiptMetaRow(
                                "Remaining Due",
                                CurrencyFormatter.format(order.remainingAmountUsd, currency, exchangeRate),
                                valueColor = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // Bottom Action Buttons: Share / Print PDF / Done
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { shareTextReceipt(context, order, lineItems, currency, exchangeRate) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Share", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { printInvoiceHtml(context, order, lineItems, currency, exchangeRate) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Print / PDF", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                ) {
                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun OrderStatusPill(status: PosPaymentStatus) {
    val (bg, fg, label) = when (status) {
        PosPaymentStatus.PAID -> Triple(Color(0xFF10B981).copy(alpha = 0.16f), Color(0xFF059669), "PAID")
        PosPaymentStatus.UNPAID -> Triple(Color(0xFFF59E0B).copy(alpha = 0.16f), Color(0xFFD97706), "UNPAID")
        PosPaymentStatus.PARTIAL -> Triple(Color(0xFFF59E0B).copy(alpha = 0.16f), Color(0xFFD97706), "PARTIAL")
        PosPaymentStatus.PARTIALLY_REFUNDED -> Triple(Color(0xFFF97316).copy(alpha = 0.16f), Color(0xFFEA580C), "PARTIAL REFUND")
        PosPaymentStatus.REFUNDED -> Triple(Color(0xFFEF4444).copy(alpha = 0.16f), Color(0xFFDC2626), "REFUNDED")
        PosPaymentStatus.CANCELLED -> Triple(Color(0xFFEF4444).copy(alpha = 0.16f), Color(0xFFDC2626), "CANCELLED")
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ReceiptMetaRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

private fun shareTextReceipt(
    context: Context,
    order: SalesOrderEntity,
    items: List<SalesOrderLineItem>,
    currency: AppCurrency,
    exchangeRate: Double
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    val sb = StringBuilder().apply {
        appendLine("========= NAWANO POS =========")
        appendLine("Invoice: #${order.invoiceNumber}")
        appendLine("Date: $dateStr")
        appendLine("Customer: ${order.customerName}")
        appendLine("Status: ${order.paymentStatus.name}")
        appendLine("Payment: ${order.paymentMethod.name.replace('_', ' ')}")
        appendLine("------------------------------")
        items.forEach { item ->
            appendLine("${item.quantity}x ${item.name}  ->  ${CurrencyFormatter.format(item.lineTotalUsd, currency, exchangeRate)}")
        }
        appendLine("------------------------------")
        appendLine("Subtotal: ${CurrencyFormatter.format(order.subtotalUsd, currency, exchangeRate)}")
        if (order.discountAmountUsd > 0.0) {
            appendLine("Discount: -${CurrencyFormatter.format(order.discountAmountUsd, currency, exchangeRate)}")
        }
        if (order.taxAmountUsd > 0.0) {
            appendLine("VAT (10%): +${CurrencyFormatter.format(order.taxAmountUsd, currency, exchangeRate)}")
        }
        appendLine("GRAND TOTAL: ${CurrencyFormatter.formatWithEquivalent(order.totalAmountUsd, currency, exchangeRate)}")
        if (order.refundedAmountUsd > 0.0) {
            appendLine("REFUNDED: -${CurrencyFormatter.format(order.refundedAmountUsd, currency, exchangeRate)} (${order.refundIssueType})")
        }
        appendLine("==============================")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invoice #${order.invoiceNumber} - Nawano POS")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Receipt"))
}

private fun printInvoiceHtml(
    context: Context,
    order: SalesOrderEntity,
    items: List<SalesOrderLineItem>,
    currency: AppCurrency,
    exchangeRate: Double
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    val rowsHtml = items.joinToString("") { item ->
        """
        <tr>
            <td>${item.name}</td>
            <td style="text-align:center;">${item.quantity}</td>
            <td style="text-align:right;">${CurrencyFormatter.format(item.lineTotalUsd, currency, exchangeRate)}</td>
        </tr>
        """.trimIndent()
    }
    val html = """
        <html>
        <body style="font-family: sans-serif; padding: 20px; color: #0f172a;">
            <h2 style="margin-bottom:4px;">NAWANO POS — INVOICE</h2>
            <p style="margin-top:0; color:#475569;">Invoice #${order.invoiceNumber} &bull; $dateStr</p>
            <p><strong>Customer:</strong> ${order.customerName} &nbsp;|&nbsp; <strong>Status:</strong> ${order.paymentStatus.name}</p>
            <table style="width:100%; border-collapse:collapse; margin-top:12px;">
                <thead>
                    <tr style="border-bottom:1px solid #cbd5e1;">
                        <th style="text-align:left; padding:6px 0;">Item</th>
                        <th style="text-align:center; padding:6px 0;">Qty</th>
                        <th style="text-align:right; padding:6px 0;">Total</th>
                    </tr>
                </thead>
                <tbody>$rowsHtml</tbody>
            </table>
            <hr style="margin:14px 0; border:none; border-top:1px dashed #94a3b8;"/>
            <p style="text-align:right;">Subtotal: <strong>${CurrencyFormatter.format(order.subtotalUsd, currency, exchangeRate)}</strong></p>
            ${if (order.discountAmountUsd > 0) "<p style=\"text-align:right;\">Discount: -${CurrencyFormatter.format(order.discountAmountUsd, currency, exchangeRate)}</p>" else ""}
            ${if (order.taxAmountUsd > 0) "<p style=\"text-align:right;\">VAT: +${CurrencyFormatter.format(order.taxAmountUsd, currency, exchangeRate)}</p>" else ""}
            <h3 style="text-align:right;">Grand Total: ${CurrencyFormatter.formatWithEquivalent(order.totalAmountUsd, currency, exchangeRate)}</h3>
            ${if (order.refundedAmountUsd > 0) "<p style=\"text-align:right; color:#dc2626;\">Refunded: -${CurrencyFormatter.format(order.refundedAmountUsd, currency, exchangeRate)} (${order.refundIssueType})</p>" else ""}
        </body>
        </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val jobName = "Nawano_Invoice_${order.invoiceNumber}"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}
