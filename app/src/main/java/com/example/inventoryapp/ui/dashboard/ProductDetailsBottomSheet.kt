package com.example.inventoryapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

/**
 * Modern Material 3 Swipe-Up Product Details View.
 * Provides high-res photo inspection, on-screen barcode generator,
 * profit margin financials, quick batch stock adjustments, and in-place editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsBottomSheet(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onQuantityAdjust: (delta: Int, note: String) -> Unit,
    onProductUpdated: (ProductEntity) -> Unit,
    onDeleteProduct: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Editable draft state
    var editName by remember(product) { mutableStateOf(product.name) }
    var editBrand by remember(product) { mutableStateOf(product.brand) }
    var editCategory by remember(product) { mutableStateOf(product.category) }
    var editSubcategory by remember(product) { mutableStateOf(product.subcategory.ifBlank { CategoryHierarchy.getSubcategories(product.category).firstOrNull() ?: "General" }) }
    var editUnit by remember(product) { mutableStateOf(product.unit) }
    var editSellingPriceText by remember(product) { mutableStateOf(String.format(Locale.US, "%.2f", product.sellingPrice)) }
    var editCostPriceText by remember(product) { mutableStateOf(String.format(Locale.US, "%.2f", product.costPrice)) }
    var editReorderLevelText by remember(product) { mutableStateOf(product.reorderLevel.toString()) }
    var editLocation by remember(product) { mutableStateOf(product.location) }
    var editImageUri by remember(product) { mutableStateOf(product.imageUri) }

    var categoryDropdownOpen by remember { mutableStateOf(false) }
    var subcategoryDropdownOpen by remember { mutableStateOf(false) }
    var customAdjustmentText by remember { mutableStateOf("") }
    var customAdjustmentNote by remember { mutableStateOf("") }
    var showCustomAdjustmentDialog by remember { mutableStateOf(false) }

    val categoryOptions = listOf("Electronics", "Accessories", "Furniture", "Hardware", "Grocery", "Other")

    // Camera launcher for updating product photo
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "prod_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                val newUri = file.toURI().toString()
                editImageUri = newUri
                // Auto-save photo update if not in edit mode
                if (!isEditMode) {
                    onProductUpdated(product.copy(imageUri = newUri))
                    Toast.makeText(context, "Product photo updated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not save photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gallery launcher
    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            editImageUri = uriStr
            if (!isEditMode) {
                onProductUpdated(product.copy(imageUri = uriStr))
                Toast.makeText(context, "Product photo updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 72.dp),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                width = 44.dp,
                height = 5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        },
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Navigation & Action Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        val categoryBreadcrumb = if (product.subcategory.isNotBlank()) {
                            "${product.category.uppercase(Locale.ROOT)} > ${product.subcategory.uppercase(Locale.ROOT)}"
                        } else {
                            product.category.uppercase(Locale.ROOT)
                        }
                        Text(
                            text = if (isEditMode) "EDIT MODE" else categoryBreadcrumb,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Editing Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isEditMode) {
                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Product",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Product",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { animateDismiss() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isEditMode) {
                // ==========================================
                // EDIT MODE FORM
                // ==========================================
                Text(
                    text = "Edit Product Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Update catalog metadata, pricing, and reorder levels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Photo preview & update in Edit Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!editImageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = editImageUri,
                            contentDescription = "Product Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, NawanoPrimary200.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { takePictureLauncher.launch(null) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { pickGalleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (!editImageUri.isNullOrBlank()) {
                            TextButton(
                                onClick = { editImageUri = null },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Remove photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // Fields
                val isEditNameError = editName.isBlank()
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = {
                        Text(
                            buildAnnotatedString {
                                append("Product Name ")
                                withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                                    append("*")
                                }
                            }
                        )
                    },
                    isError = isEditNameError,
                    supportingText = if (isEditNameError) {
                        { Text("Product Name is required *", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category & Subcategory Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Category Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { categoryDropdownOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = categoryDropdownOpen,
                            onDismissRequest = { categoryDropdownOpen = false }
                        ) {
                            categoryOptions.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        editCategory = cat
                                        editSubcategory = CategoryHierarchy.getSubcategories(cat).firstOrNull() ?: "General"
                                        categoryDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Subcategory Dropdown
                    val subOptions = remember(editCategory) {
                        CategoryHierarchy.getSubcategories(editCategory).ifEmpty { listOf("General") }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = editSubcategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subcategory") },
                            trailingIcon = {
                                IconButton(onClick = { subcategoryDropdownOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = subcategoryDropdownOpen,
                            onDismissRequest = { subcategoryDropdownOpen = false }
                        ) {
                            subOptions.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub) },
                                    onClick = {
                                        editSubcategory = sub
                                        subcategoryDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editBrand,
                    onValueChange = { editBrand = it },
                    label = { Text("Brand / Author") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editSellingPriceText,
                        onValueChange = { editSellingPriceText = it },
                        label = { Text("Selling Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCostPriceText,
                        onValueChange = { editCostPriceText = it },
                        label = { Text("Cost Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("Shelf Location") },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editReorderLevelText,
                        onValueChange = { editReorderLevelText = it },
                        label = { Text("Alert Below") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Cancel Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { isEditMode = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val parsedSelling = editSellingPriceText.toDoubleOrNull() ?: product.sellingPrice
                            val parsedCost = editCostPriceText.toDoubleOrNull() ?: product.costPrice
                            val parsedReorder = editReorderLevelText.toIntOrNull() ?: product.reorderLevel

                            val updated = product.copy(
                                name = editName.trim().ifBlank { product.name },
                                brand = editBrand.trim(),
                                category = editCategory,
                                subcategory = editSubcategory,
                                sellingPrice = parsedSelling,
                                costPrice = parsedCost,
                                location = editLocation.trim(),
                                reorderLevel = parsedReorder,
                                imageUri = editImageUri,
                                updatedAt = System.currentTimeMillis()
                            )
                            onProductUpdated(updated)
                            isEditMode = false
                            Toast.makeText(context, "Product changes saved!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NawanoPrimary200,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // ==========================================
                // VIEW MODE: RICH PRODUCT SHOWCASE (Nawano UI Kit)
                // ==========================================

                // Hero Media + Title & Stock Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // High-Res Thumbnail with Quick Camera Button
                    Box(modifier = Modifier.size(88.dp)) {
                        if (!product.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = product.imageUri,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.5.dp, NawanoPrimary200.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getCategoryIcon(product.category),
                                        contentDescription = null,
                                        tint = NawanoPrimary200,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        // Floating quick camera overlay button
                        Surface(
                            onClick = { takePictureLauncher.launch(null) },
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Update Photo", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (product.brand.isNotBlank()) {
                            Text(
                                text = "by ${product.brand}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Stock Status Pill
                        val isLowStock = product.quantity <= product.reorderLevel
                        val (stockLabel, stockBg, stockColor) = when {
                            product.quantity <= 0 -> Triple("Out of Stock", Color(0xFFEF4444).copy(alpha = 0.12f), Color(0xFFDC2626))
                            isLowStock -> Triple("${product.quantity} ${product.unit} left (Low Stock)", Color(0xFFF59E0B).copy(alpha = 0.12f), Color(0xFFD97706))
                            else -> Triple("${product.quantity} ${product.unit} in stock", Color(0xFF10B981).copy(alpha = 0.12f), Color(0xFF047857))
                        }

                        Surface(
                            color = stockBg,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, stockColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(stockColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = stockLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = stockColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ------------------------------------------
                // FEATURE 1: DIGITAL BARCODE GENERATOR DISPLAY
                // ------------------------------------------
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ON-SCREEN DIGITAL BARCODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Copy SKU button
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Product SKU", product.sku)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "SKU '${product.sku}' copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp), tint = NawanoPrimary200)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = NawanoPrimary200)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // High-contrast Canvas Barcode Representation (Scannable off screen!)
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BarcodeLinesCanvas(
                                    sku = product.sku,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = product.sku,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scannable directly by physical hardware & camera barcode readers",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ------------------------------------------
                // FEATURE 2: FINANCIALS & PROFIT MARGIN ANALYTICS
                // ------------------------------------------
                val profitPerUnit = (product.sellingPrice - product.costPrice).coerceAtLeast(0.0)
                val profitMarginPercent = if (product.sellingPrice > 0) (profitPerUnit / product.sellingPrice * 100) else 0.0
                val totalHoldingValue = product.quantity * product.sellingPrice

                Text(
                    text = "Financials & Valuation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Selling Price Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Selling Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = NawanoPrimary200
                            )
                        }
                    }

                    // Cost Price Card
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Unit Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", product.costPrice)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Profit Margin Card
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Profit Margin", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+$${String.format(Locale.US, "%.2f", profitPerUnit)} (${String.format(Locale.US, "%.0f", profitMarginPercent)}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Location & Total Inventory Holding Value
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Location:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = product.location.ifBlank { "Unassigned" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Asset Value:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", totalHoldingValue)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = NawanoPrimary200
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ------------------------------------------
                // FEATURE 3: QUICK BATCH STOCK ADJUSTMENTS
                // ------------------------------------------
                Text(
                    text = "Quick Stock Adjustments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Instantly record stock received or dispatched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Batch Adjuster Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Deduct -5
                    OutlinedButton(
                        onClick = { onQuantityAdjust(-5, "Batch deduction (-5)") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-5", fontWeight = FontWeight.Bold)
                    }

                    // Deduct -1
                    OutlinedButton(
                        onClick = { onQuantityAdjust(-1, "Quick deduction (-1)") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-1", fontWeight = FontWeight.Bold)
                    }

                    // Current Qty Display
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${product.quantity} ${product.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Add +1
                    Button(
                        onClick = { onQuantityAdjust(1, "Quick restock (+1)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1", fontWeight = FontWeight.Bold)
                    }

                    // Add +5
                    Button(
                        onClick = { onQuantityAdjust(5, "Batch restock (+5)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+5", fontWeight = FontWeight.Bold)
                    }

                    // Add +10
                    Button(
                        onClick = { onQuantityAdjust(10, "Batch restock (+10)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Text("+10", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Adjustment Button
                OutlinedButton(
                    onClick = { showCustomAdjustmentDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom Adjustment with Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Custom Adjustment & Reconciliation Modal Dialog
    if (showCustomAdjustmentDialog) {
        val reconciliationPresets = listOf(
            Triple("Shrinkage / Theft", -1, Color(0xFFEF4444)),
            Triple("Damaged Goods", -1, Color(0xFFF97316)),
            Triple("Expired Stock", -1, Color(0xFFEAB308)),
            Triple("Audit Reconciliation", 0, Color(0xFF6366F1)),
            Triple("Customer Return", 1, Color(0xFF10B981)),
            Triple("Supplier Restock", 10, NawanoPrimary200)
        )

        AlertDialog(
            onDismissRequest = { showCustomAdjustmentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stock Reconciliation & Audit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1-Tap Quick Reason Preset:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        reconciliationPresets.forEach { (reason, defaultDelta, color) ->
                            val isSelected = customAdjustmentNote == reason
                            Surface(
                                onClick = {
                                    customAdjustmentNote = reason
                                    if (customAdjustmentText.isBlank() || customAdjustmentText == "0") {
                                        customAdjustmentText = if (defaultDelta > 0) "+$defaultDelta" else "$defaultDelta"
                                    } else if (defaultDelta < 0 && !customAdjustmentText.startsWith("-")) {
                                        customAdjustmentText = "-$customAdjustmentText"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    OutlinedTextField(
                        value = customAdjustmentText,
                        onValueChange = { customAdjustmentText = it },
                        label = { Text("Quantity Delta (e.g. -3 or +25)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customAdjustmentNote,
                        onValueChange = { customAdjustmentNote = it },
                        label = { Text("Note / Audit Reason") },
                        placeholder = { Text("e.g. Broken in transit, weekly audit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = customAdjustmentText.trim().toIntOrNull()
                        if (delta != null && delta != 0) {
                            val finalType = if (delta < 0) "STOCK_OUT" else "STOCK_IN"
                            onQuantityAdjust(delta, customAdjustmentNote.ifBlank { "Reconciliation adjustment" })
                            showCustomAdjustmentDialog = false
                            customAdjustmentText = ""
                            customAdjustmentNote = ""
                            Toast.makeText(context, "Stock adjusted by $delta", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter a valid non-zero integer", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAdjustmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete '${product.name}'?") },
            text = { Text("This will permanently remove this SKU and its stock history from your inventory.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteProduct()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Procedural Barcode Canvas Generator.
 * Draws high-contrast alternating barcode lines derived from the product SKU.
 */
@Composable
private fun BarcodeLinesCanvas(
    sku: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val barHeight = size.height
        val hash = sku.hashCode()

        // Generate deterministic bar widths based on SKU characters
        val numBars = 55
        val barWidth = totalWidth / numBars

        // Outer guard bars
        drawRect(Color.Black, Offset(0f, 0f), Size(barWidth * 1.5f, barHeight))
        drawRect(Color.Black, Offset(barWidth * 2.5f, 0f), Size(barWidth, barHeight))

        for (i in 4 until numBars - 4) {
            val isBlack = ((hash xor (i * 31) xor sku.length) and (1 shl (i % 16))) != 0
            if (isBlack) {
                val thicknessMultiplier = if (i % 7 == 0) 1.8f else 1.0f
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(i * barWidth, 0f),
                    size = Size(barWidth * thicknessMultiplier, barHeight)
                )
            }
        }

        // End guard bars
        drawRect(Color.Black, Offset(totalWidth - barWidth * 3.5f, 0f), Size(barWidth, barHeight))
        drawRect(Color.Black, Offset(totalWidth - barWidth * 1.5f, 0f), Size(barWidth * 1.5f, barHeight))
    }
}
