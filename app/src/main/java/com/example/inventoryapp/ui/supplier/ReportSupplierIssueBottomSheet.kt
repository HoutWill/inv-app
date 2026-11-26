package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.IssueSeverity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Swipe-up Modal Bottom Sheet allowing a Middleman to report an issue
 * (damaged delivery, missing units, wrong item, quality/expiry, late freight)
 * directly to a Supplier or on a specific Purchase Order.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSupplierIssueBottomSheet(
    supplierName: String,
    orderNumber: String = "",
    orderItems: List<PurchaseOrderItemEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSubmitIssue: (
        issueType: String,
        severity: IssueSeverity,
        affectedItemName: String,
        affectedQuantity: Int,
        description: String
    ) -> Unit
) {
    val sheetState = rememberSafeModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val borderCol = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val issueCategories = remember {
        listOf(
            "Damaged Goods",
            "Missing / Short Quantity",
            "Wrong Item Delivered",
            "Expired / Quality Issue",
            "Late Shipment",
            "Invoice Dispute"
        )
    }

    var selectedIssueType by remember { mutableStateOf(issueCategories.first()) }
    var selectedSeverity by remember { mutableStateOf(IssueSeverity.HIGH) }
    var affectedItemName by remember {
        mutableStateOf(orderItems.firstOrNull()?.productName ?: "")
    }
    var affectedQuantityText by remember {
        mutableStateOf(orderItems.firstOrNull()?.quantity?.toString() ?: "1")
    }
    var description by remember { mutableStateOf("") }

    val quickSuggestions = remember(selectedIssueType) {
        when (selectedIssueType) {
            "Damaged Goods" -> listOf(
                "Outer cartons crushed on arrival",
                "Broken seals / leaking packaging",
                "Cold-chain seal broken during transit"
            )
            "Missing / Short Quantity" -> listOf(
                "Received fewer units than packing manifest",
                "1 master case missing from pallet"
            )
            "Wrong Item Delivered" -> listOf(
                "Wrong SKU or pack size shipped",
                "Received unrequested substitute product"
            )
            "Expired / Quality Issue" -> listOf(
                "Near-expiry batch (< 14 days left)",
                "Product thawed or off-temperature on dock"
            )
            "Late Shipment" -> listOf(
                "Delivery missed scheduled ETA window",
                "Carrier tracking stalled without update"
            )
            else -> listOf(
                "Unit cost differs from agreed PO rate",
                "Duplicate charge on wholesale invoice"
            )
        }
    }

    fun animateDismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = cardBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = textSecondary.copy(alpha = 0.4f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444).copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.ReportProblem,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Report Issue to Supplier",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (orderNumber.isNotBlank()) "$supplierName • $orderNumber" else supplierName,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { animateDismiss() },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary
                    )
                }
            }

            // Issue Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ISSUE CATEGORY",
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
                    issueCategories.forEach { category ->
                        val isSelected = selectedIssueType == category
                        Surface(
                            onClick = { selectedIssueType = category },
                            shape = CircleShape,
                            color = if (isSelected) NawanoPrimary200 else innerBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NawanoPrimary200 else borderCol
                            )
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else textPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Severity Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SEVERITY PRIORITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 0.8.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IssueSeverity.entries.forEach { sev ->
                        val isSelected = selectedSeverity == sev
                        val sevColor = when (sev) {
                            IssueSeverity.CRITICAL -> Color(0xFFDC2626)
                            IssueSeverity.HIGH -> Color(0xFFF97316)
                            IssueSeverity.MEDIUM -> Color(0xFFF59E0B)
                            IssueSeverity.LOW -> Color(0xFF3B82F6)
                        }
                        Surface(
                            onClick = { selectedSeverity = sev },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) sevColor.copy(alpha = 0.16f) else innerBg,
                            border = BorderStroke(1.dp, if (isSelected) sevColor else borderCol),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = sev.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) sevColor else textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Item Chips when opened from a Purchase Order
            if (orderItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SELECT AFFECTED ITEM FROM ORDER",
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
                        orderItems.forEach { item ->
                            val isSelected = affectedItemName == item.productName
                            Surface(
                                onClick = {
                                    affectedItemName = item.productName
                                    affectedQuantityText = item.quantity.toString()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NawanoPrimary200.copy(alpha = 0.14f) else innerBg,
                                border = BorderStroke(1.dp, if (isSelected) NawanoPrimary200 else borderCol)
                            ) {
                                Text(
                                    text = "${item.productName} (${item.quantity})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) NawanoPrimary200 else textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Affected Product & Affected Quantity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = affectedItemName,
                    onValueChange = { affectedItemName = it },
                    label = { Text("Affected Product / SKU") },
                    placeholder = { Text("e.g. Nutella 400g") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.8f)
                )
                OutlinedTextField(
                    value = affectedQuantityText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) affectedQuantityText = input
                    },
                    label = { Text("Qty") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.75f)
                )
            }

            // Quick Suggestion Templates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickSuggestions.forEach { suggestion ->
                    Surface(
                        onClick = { description = suggestion },
                        shape = RoundedCornerShape(10.dp),
                        color = NawanoPrimary200.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "+ $suggestion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Detailed Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Issue Details for Supplier *") },
                placeholder = { Text("Describe damage, missing count, or required replacement...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Submit Button
            val canSubmit = description.isNotBlank()
            Button(
                onClick = {
                    val qty = affectedQuantityText.toIntOrNull() ?: 0
                    onSubmitIssue(
                        selectedIssueType,
                        selectedSeverity,
                        affectedItemName,
                        qty,
                        description
                    )
                    animateDismiss()
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReportProblem,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Submit Issue Report",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
