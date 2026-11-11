package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Bottom Sheet allowing Supplier to create and issue a new wholesale shipment / Purchase Order to Middleman HQ.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderBottomSheet(
    products: List<ProductEntity>,
    scannedSku: String? = null,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRequestScanProduct: () -> Unit = {},
    onSave: (title: String, quantity: Int, price: Double, estArrival: String) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val innerCardBg = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf("25.00") }
    var selectedArrivalOption by remember { mutableStateOf("Today") }
    var customArrivalText by remember { mutableStateOf("") }

    LaunchedEffect(scannedSku) {
        if (!scannedSku.isNullOrBlank()) {
            val matched = products.find { it.sku.equals(scannedSku, ignoreCase = true) }
            if (matched != null) {
                selectedProduct = matched
                title = matched.name
                priceText = String.format(Locale.US, "%.2f", matched.costPrice)
            } else {
                title = "Wholesale Item ($scannedSku)"
            }
        }
    }

    val qtyInt = remember(qtyText) { qtyText.toIntOrNull() ?: 1 }
    val priceDouble = remember(priceText) { priceText.toDoubleOrNull() ?: 0.0 }
    val totalAmount = remember(qtyInt, priceDouble) { qtyInt * priceDouble }

    val effectiveArrival = remember(selectedArrivalOption, customArrivalText) {
        if (selectedArrivalOption == "Custom") {
            customArrivalText.ifBlank { "Next Scheduled Inbound" }
        } else {
            selectedArrivalOption
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(NawanoGradientStart, NawanoGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "New Purchase Order",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                        Text(
                            text = "Wholesale shipment to Middleman HQ",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 1: Quick Pick from Wholesale Catalog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select from Catalog",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )

                Surface(
                    onClick = onRequestScanProduct,
                    shape = RoundedCornerShape(8.dp),
                    color = NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.25f else 0.12f),
                    border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Item Barcode",
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Scan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (products.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    products.forEach { prod ->
                        val isSelected = selectedProduct?.id == prod.id
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    selectedProduct = null
                                } else {
                                    selectedProduct = prod
                                    title = prod.name
                                    priceText = String.format(Locale.US, "%.2f", prod.costPrice)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.25f else 0.12f) else innerCardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NawanoPrimary200 else fieldBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Column {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NawanoPrimary200 else textPrimary
                                    )
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", prod.costPrice)} / ${prod.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Section 2: Order Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Item Name / Description", color = textSecondary) },
                placeholder = { Text("e.g. Pacific Salmon Bulk", color = textSecondary.copy(alpha = 0.5f)) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = fieldBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section 3: Quantity Stepper & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Stepper with [-] and [+] controls
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = innerCardBg,
                        border = BorderStroke(1.dp, fieldBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val current = qtyText.toIntOrNull() ?: 1
                                    if (current > 1) qtyText = (current - 1).toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }

                            Text(
                                text = qtyText.ifBlank { "1" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary
                            )

                            IconButton(
                                onClick = {
                                    val current = qtyText.toIntOrNull() ?: 0
                                    qtyText = (current + 1).toString()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                            }
                        }
                    }
                }

                // Unit Supply Price Input
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unit Price ($)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = NawanoPrimary200,
                            unfocusedBorderColor = fieldBorder,
                            focusedContainerColor = innerCardBg,
                            unfocusedContainerColor = innerCardBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 4: Estimated Arrival Window
            Text(
                text = "Estimated Arrival",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today", "Tomorrow", "Next 48h", "Custom").forEach { opt ->
                    val isSelected = selectedArrivalOption == opt
                    Surface(
                        onClick = { selectedArrivalOption = opt },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) NawanoPrimary200 else innerCardBg,
                        border = BorderStroke(1.dp, if (isSelected) NawanoPrimary200 else fieldBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = opt,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else textSecondary
                            )
                        }
                    }
                }
            }

            if (selectedArrivalOption == "Custom") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customArrivalText,
                    onValueChange = { customArrivalText = it },
                    placeholder = { Text("e.g. Wednesday 09:30 - 11:30 AM", color = textSecondary.copy(alpha = 0.5f)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = NawanoPrimary200,
                        unfocusedBorderColor = fieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Order Total Calculation Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = innerCardBg,
                border = BorderStroke(1.dp, fieldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Order Total", style = MaterialTheme.typography.bodySmall, color = textSecondary)
                        Text(
                            text = "$qtyInt units • Net 30 Terms",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color(0xFF10B981) else Color(0xFF059669)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button with Gradient and Live Total
            val finalTitle = title.ifBlank { selectedProduct?.name ?: "" }
            val isFormValid = finalTitle.isNotBlank() && qtyInt > 0 && priceDouble >= 0.0

            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(finalTitle.trim(), qtyInt, totalAmount, effectiveArrival)
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
                ),
                contentPadding = PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isFormValid) {
                                Brush.linearGradient(
                                    listOf(NawanoGradientStart, NawanoGradientEnd)
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.Transparent)
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isFormValid) Color.White else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Order • $${String.format(Locale.US, "%,.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isFormValid) Color.White else textSecondary
                        )
                    }
                }
            }
        }
    }
}
