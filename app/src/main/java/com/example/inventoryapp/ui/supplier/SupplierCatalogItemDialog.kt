package com.example.inventoryapp.ui.supplier

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*

/**
 * Reusable outlined box with always-floating label cut into the top border outline,
 * guaranteeing 100% fidelity to the visual mockup.
 */
@Composable
fun CatalogOutlinedBox(
    label: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
                .border(
                    width = 1.2.dp,
                    color = fieldBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }

        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .background(cardBg)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * Swipe-Up BottomSheet to register a new product into the Supplier's wholesale catalog,
 * matching the exact minimal design mockup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCatalogProductDialog(
    initialSku: String = "",
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRequestScanSku: () -> Unit = {},
    onSave: (ProductEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf(initialSku.ifBlank { "SKU-${(1000..9999).random()}" }) }
    var category by remember { mutableStateOf("Seafood") }
    var costPrice by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("0") }
    var unit by remember { mutableStateOf("Kg") }
    var imageUri by remember { mutableStateOf<String?>(null) }

    var categoryDropdownOpen by remember { mutableStateOf(false) }
    var unitDropdownOpen by remember { mutableStateOf(false) }

    val categories = listOf("Seafood", "Beverages", "Pantry", "Meats", "Dairy-Free", "Produce", "Hardware", "General")
    val units = listOf("Kg", "Pcs", "Box", "Carton", "Bottle", "Pack", "Can", "Jar", "Bag")

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    LaunchedEffect(initialSku) {
        if (initialSku.isNotBlank()) {
            sku = initialSku
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1),
                width = 36.dp,
                height = 4.dp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Catalog",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Row 1: Upload Product Image (left) + Product Name (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Upload Product Image Box
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "UPLOAD PRODUCT IMAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9))
                            .drawBehind {
                                val stroke = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                                drawRoundRect(
                                    color = if (isDarkTheme) NawanoDarkOutline else Color(0xFF94A3B8),
                                    cornerRadius = CornerRadius(14.dp.toPx()),
                                    style = stroke
                                )
                            }
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Product Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Add Photo",
                                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "+ Add Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                // Product Name Box with always-floating label and multiline hint
                CatalogOutlinedBox(
                    label = "Product Name",
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 19.sp
                        ),
                        cursorBrush = SolidColor(NawanoPrimary200),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 6.dp),
                        decorationBox = { innerTextField ->
                            if (name.isEmpty()) {
                                Text(
                                    text = "e.g., Coca-Cola Original\n(330ml Can)",
                                    color = textSecondary.copy(alpha = 0.5f),
                                    fontSize = 13.5.sp,
                                    lineHeight = 18.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // Row 2: SKU Code / Barcode (left) + Category Group (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CatalogOutlinedBox(
                    label = "SKU Code / Barcode",
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(NawanoPrimary200),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onRequestScanSku,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan SKU Barcode",
                                tint = textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                CatalogOutlinedBox(
                    label = "Category Group",
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { categoryDropdownOpen = true },
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = categoryDropdownOpen,
                        onDismissRequest = { categoryDropdownOpen = false },
                        modifier = Modifier.background(cardBg)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        color = textPrimary,
                                        fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    category = cat
                                    categoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Row 3: Supply Price ($) + Initial Stock + Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CatalogOutlinedBox(
                    label = "Supply Price ($)",
                    modifier = Modifier
                        .weight(1.15f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$",
                            color = textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicTextField(
                            value = costPrice,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    costPrice = input
                                }
                            },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(NawanoPrimary200),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterEnd,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (costPrice.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            color = textSecondary.copy(alpha = 0.5f),
                                            fontSize = 15.sp,
                                            textAlign = TextAlign.End,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                CatalogOutlinedBox(
                    label = "Initial Stock",
                    modifier = Modifier
                        .weight(1.05f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = quantity,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                quantity = digits.ifEmpty { "0" }
                            },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(NawanoPrimary200),
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val q = quantity.toIntOrNull() ?: 0
                                    quantity = (q - 1).coerceAtLeast(0).toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val q = quantity.toIntOrNull() ?: 0
                                    quantity = (q + 1).toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                CatalogOutlinedBox(
                    label = "Unit",
                    modifier = Modifier
                        .weight(0.8f)
                        .height(58.dp)
                        .clickable { unitDropdownOpen = true },
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = unit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Unit",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = unitDropdownOpen,
                        onDismissRequest = { unitDropdownOpen = false },
                        modifier = Modifier.background(cardBg)
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = u,
                                        color = textPrimary,
                                        fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    unit = u
                                    unitDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Action Buttons (Cancel + Add Product Pill Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.5.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )
                }

                Button(
                    onClick = {
                        val cost = costPrice.toDoubleOrNull() ?: 0.0
                        val qty = quantity.toIntOrNull() ?: 0
                        val prod = ProductEntity(
                            sku = sku.trim().ifBlank { "SKU-${(1000..9999).random()}" },
                            name = name.trim().ifBlank { "Item ${sku.trim()}" },
                            category = category.trim().ifBlank { "General" },
                            costPrice = cost,
                            sellingPrice = cost * 1.5,
                            quantity = qty,
                            reorderLevel = 10,
                            unit = unit.trim().ifBlank { "Kg" },
                            imageUri = imageUri,
                            supplierId = "middleman-supplier-demo",
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(prod)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Add Product",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Swipe-Up BottomSheet to edit an existing wholesale catalog item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCatalogProductDialog(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var name by remember { mutableStateOf(product.name) }
    var sku by remember { mutableStateOf(product.sku) }
    var category by remember { mutableStateOf(product.category) }
    var costPrice by remember { mutableStateOf(if (product.costPrice > 0.0) String.format(java.util.Locale.US, "%.2f", product.costPrice) else "") }
    var quantity by remember { mutableStateOf(product.quantity.toString()) }
    var unit by remember { mutableStateOf(product.unit.ifBlank { "Kg" }) }
    var imageUri by remember { mutableStateOf(product.imageUri) }

    var categoryDropdownOpen by remember { mutableStateOf(false) }
    var unitDropdownOpen by remember { mutableStateOf(false) }

    val categories = listOf("Seafood", "Beverages", "Pantry", "Meats", "Dairy-Free", "Produce", "Hardware", "General")
    val units = listOf("Kg", "Pcs", "Box", "Carton", "Bottle", "Pack", "Can", "Jar", "Bag")

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1),
                width = 36.dp,
                height = 4.dp
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Product",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Row 1: Upload / Change Product Image (left) + Product Name (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PRODUCT IMAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = textSecondary
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9))
                            .drawBehind {
                                val stroke = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                )
                                drawRoundRect(
                                    color = if (isDarkTheme) NawanoDarkOutline else Color(0xFF94A3B8),
                                    cornerRadius = CornerRadius(14.dp.toPx()),
                                    style = stroke
                                )
                            }
                            .clickable { pickImageLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Product Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = "Add Photo",
                                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Change Photo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                CatalogOutlinedBox(
                    label = "Product Name",
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 19.sp
                        ),
                        cursorBrush = SolidColor(NawanoPrimary200),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 6.dp)
                    )
                }
            }

            // Row 2: SKU Code / Barcode (left) + Category Group (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CatalogOutlinedBox(
                    label = "SKU Code / Barcode",
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    BasicTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(NawanoPrimary200),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                CatalogOutlinedBox(
                    label = "Category Group",
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { categoryDropdownOpen = true },
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = categoryDropdownOpen,
                        onDismissRequest = { categoryDropdownOpen = false },
                        modifier = Modifier.background(cardBg)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        color = textPrimary,
                                        fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    category = cat
                                    categoryDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            // Row 3: Supply Price ($) + Stock Qty + Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CatalogOutlinedBox(
                    label = "Supply Price ($)",
                    modifier = Modifier
                        .weight(1.15f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$",
                            color = textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        BasicTextField(
                            value = costPrice,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                    costPrice = input
                                }
                            },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(NawanoPrimary200),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                CatalogOutlinedBox(
                    label = "Stock Qty",
                    modifier = Modifier
                        .weight(1.05f)
                        .height(58.dp),
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BasicTextField(
                            value = quantity,
                            onValueChange = { input ->
                                val digits = input.filter { it.isDigit() }
                                quantity = digits.ifEmpty { "0" }
                            },
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(NawanoPrimary200),
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val q = quantity.toIntOrNull() ?: 0
                                    quantity = (q - 1).coerceAtLeast(0).toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "−",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val q = quantity.toIntOrNull() ?: 0
                                    quantity = (q + 1).toString()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(
                                    text = "+",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                CatalogOutlinedBox(
                    label = "Unit",
                    modifier = Modifier
                        .weight(0.8f)
                        .height(58.dp)
                        .clickable { unitDropdownOpen = true },
                    isDarkTheme = isDarkTheme
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = unit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Unit",
                            tint = textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = unitDropdownOpen,
                        onDismissRequest = { unitDropdownOpen = false },
                        modifier = Modifier.background(cardBg)
                    ) {
                        units.forEach { u ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = u,
                                        color = textPrimary,
                                        fontWeight = if (u == unit) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    unit = u
                                    unitDropdownOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Action Buttons (Cancel + Save Changes Pill Buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.5.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary
                    )
                }

                Button(
                    onClick = {
                        val cost = costPrice.toDoubleOrNull() ?: product.costPrice
                        val qty = quantity.toIntOrNull() ?: product.quantity
                        onSave(
                            product.copy(
                                name = name.trim().ifBlank { product.name },
                                sku = sku.trim().ifBlank { product.sku },
                                category = category.trim().ifBlank { product.category },
                                costPrice = cost,
                                quantity = qty,
                                unit = unit.trim().ifBlank { product.unit },
                                imageUri = imageUri,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Swipe-Up BottomSheet to restock wholesale quantity for a product in the Supplier portal.
 * Features quick addition chips (+5, +10, +25, +50, +100) and custom quantity input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockProductDialog(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onRestockConfirmed: (quantityToAdd: Int) -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val fieldBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)

    var addQtyText by remember { mutableStateOf("10") }
    val addQtyInt = addQtyText.toIntOrNull() ?: 0
    val newTotal = (product.quantity + addQtyInt).coerceAtLeast(0)

    val quickAmounts = listOf(5, 10, 25, 50, 100)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = cardBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDarkTheme) NawanoDarkOutline else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 40.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NawanoPrimary200.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Restock Product",
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontSize = 17.sp
                        )
                        Text(
                            text = product.name,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            // Current Stock Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, fieldBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Current Stock", fontSize = 11.sp, color = textSecondary)
                        Text(
                            "${product.quantity} ${product.unit}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("After Restock", fontSize = 11.sp, color = textSecondary)
                        Text(
                            "$newTotal ${product.unit}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            // Quick Increment Chips (+5, +10, +25, +50, +100)
            Text(
                "Quick Add Quantity",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickAmounts.forEach { amt ->
                    val isSelected = addQtyText == amt.toString()
                    Surface(
                        onClick = { addQtyText = amt.toString() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NawanoPrimary200 else (if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, if (isSelected) NawanoPrimary200 else fieldBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "+$amt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else textPrimary,
                            modifier = Modifier.padding(vertical = 7.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Custom Quantity Field
            OutlinedTextField(
                value = addQtyText,
                onValueChange = { addQtyText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Units to Add", color = textSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = NawanoPrimary200,
                    unfocusedBorderColor = fieldBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel", color = textSecondary)
                }

                Button(
                    onClick = {
                        if (addQtyInt > 0) {
                            onRestockConfirmed(addQtyInt)
                        }
                    },
                    enabled = addQtyInt > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Restock +$addQtyInt",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
