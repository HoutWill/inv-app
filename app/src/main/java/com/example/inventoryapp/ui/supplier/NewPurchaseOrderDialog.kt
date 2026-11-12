package com.example.inventoryapp.ui.supplier

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

data class DraftOrderItem(
    val sku: String,
    val name: String,
    val quantity: Int,
    val unitCost: Double,
    val productId: String? = null
)

/**
 * Minimal Restock Purchase Order Dialog.
 * Features Dropdown Partner Selector and Scoped Partner-Only Items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseOrderDialog(
    initialSupplier: SupplierEntity?,
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
        estArrival: String
    ) -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val surfaceColor = if (isDarkTheme) NawanoDark200 else Color.White
    val cardColor = if (isDarkTheme) NawanoDarkCard else Color(0xFFF8FAFC)
    val outlineColor = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)

    var selectedSupplier by remember { mutableStateOf(initialSupplier ?: suppliers.firstOrNull()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var orderTitle by remember { mutableStateOf("") }
    var picSender by remember { mutableStateOf(selectedSupplier?.contactName ?: "Brandon Vaccaro") }
    var estArrival by remember { mutableStateOf("09:30 - 11:30 AM") }
    var ownerNote by remember { mutableStateOf("") }

    // Scoped Items: ONLY items belonging to the selected partner
    val partnerProducts = remember(selectedSupplier, products) {
        if (selectedSupplier == null) emptyList()
        else products.filter { it.supplierId == selectedSupplier?.id }
    }

    // Draft items for the PO
    val draftItems = remember { mutableStateListOf<DraftOrderItem>() }

    fun populateItemsForSupplier(sup: SupplierEntity?) {
        draftItems.clear()
        if (sup == null) return
        val supItems = products.filter { it.supplierId == sup.id }
        if (supItems.isNotEmpty()) {
            val first = supItems.first()
            draftItems.add(
                DraftOrderItem(
                    sku = first.sku,
                    name = first.name,
                    quantity = if (first.reorderQty > 0) first.reorderQty else 10,
                    unitCost = first.costPrice,
                    productId = first.id
                )
            )
        }
    }

    // Populate on initial launch
    LaunchedEffect(selectedSupplier?.id) {
        if (draftItems.isEmpty()) {
            populateItemsForSupplier(selectedSupplier)
        }
    }

    // Manual item inputs
    var newSku by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newQty by remember { mutableStateOf("10") }
    var newCost by remember { mutableStateOf("25.00") }

    AlertDialog(
        containerColor = surfaceColor,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restock",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Partner Dropdown Selector (drop select)
                Text(text = "Supplier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textSecondary)

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { isDropdownExpanded = !isDropdownExpanded },
                        shape = RoundedCornerShape(10.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, if (isDropdownExpanded) NawanoPrimary200 else outlineColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sup = selectedSupplier
                            if (sup != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val initials = sup.name.split(" ")
                                        .mapNotNull { it.firstOrNull()?.toString() }
                                        .take(2)
                                        .joinToString("")
                                        .ifBlank { "SU" }

                                    Surface(
                                        shape = CircleShape,
                                        color = NawanoPrimary200.copy(alpha = 0.12f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = initials,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NawanoPrimary200
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = sup.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (sup.contactName.isNotBlank()) {
                                            Text(
                                                text = sup.contactName,
                                                fontSize = 10.sp,
                                                color = textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Select Partner",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }

                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(18.dp)
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val inits = s.name.split(" ")
                                            .mapNotNull { it.firstOrNull()?.toString() }
                                            .take(2)
                                            .joinToString("")
                                            .ifBlank { "SU" }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) NawanoPrimary200 else NawanoPrimary200.copy(alpha = 0.12f),
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = inits,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else NawanoPrimary200
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = s.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) NawanoPrimary200 else textPrimary
                                            )
                                            if (s.contactName.isNotBlank()) {
                                                Text(
                                                    text = s.contactName,
                                                    fontSize = 10.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedSupplier = s
                                    if (s.contactName.isNotBlank()) picSender = s.contactName
                                    isDropdownExpanded = false
                                    populateItemsForSupplier(s)
                                }
                            )
                        }
                    }
                }

                // 2. Available Partner Items (Only items belonging to this partner)
                if (partnerProducts.isNotEmpty()) {
                    Text(
                        text = "Catalog (${partnerProducts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAlreadyInDraft) NawanoPrimary200.copy(alpha = 0.15f) else cardColor,
                                border = BorderStroke(
                                    1.dp,
                                    if (isAlreadyInDraft) NawanoPrimary200 else outlineColor
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAlreadyInDraft) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "${prod.name} ($${String.format(Locale.US, "%.0f", prod.costPrice)})",
                                        fontSize = 10.sp,
                                        fontWeight = if (isAlreadyInDraft) FontWeight.Bold else FontWeight.Normal,
                                        color = textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Order Title
                OutlinedTextField(
                    value = orderTitle,
                    onValueChange = { orderTitle = it },
                    label = { Text("Title") },
                    placeholder = { Text("Inbound ${selectedSupplier?.name ?: "Stock"}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Sender & Arrival Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = picSender,
                        onValueChange = { picSender = it },
                        label = { Text("Sender") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = estArrival,
                        onValueChange = { estArrival = it },
                        label = { Text("Arrival") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = outlineColor)

                // 5. Current Items on PO (Scoped to Selected Partner)
                Text(
                    text = "Items (${draftItems.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                draftItems.forEachIndexed { idx, item ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cardColor,
                        border = BorderStroke(1.dp, outlineColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.sku} • ${item.quantity} units @ $${String.format(Locale.US, "%.2f", item.unitCost)}",
                                    fontSize = 10.sp,
                                    color = textSecondary
                                )
                            }
                            IconButton(
                                onClick = { draftItems.removeAt(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // 6. Quick Custom Item Form
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, outlineColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "Item", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedTextField(
                                value = newSku,
                                onValueChange = { newSku = it },
                                placeholder = { Text("SKU") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                placeholder = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            OutlinedTextField(
                                value = newQty,
                                onValueChange = { newQty = it },
                                placeholder = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newCost,
                                onValueChange = { newCost = it },
                                placeholder = { Text("Cost") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val q = newQty.toIntOrNull() ?: 1
                                    val c = newCost.toDoubleOrNull() ?: 0.0
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
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                modifier = Modifier
                                    .height(32.dp)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // 7. Internal Note
                OutlinedTextField(
                    value = ownerNote,
                    onValueChange = { ownerNote = it },
                    label = { Text("Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sup = selectedSupplier ?: return@Button
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
                    val title = orderTitle.ifBlank { "Inbound Delivery from ${sup.name}" }
                    onSubmit(sup, title, picSender, finalItems, ownerNote, estArrival)
                },
                enabled = selectedSupplier != null && draftItems.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Cancel", color = textSecondary)
            }
        }
    )
}
