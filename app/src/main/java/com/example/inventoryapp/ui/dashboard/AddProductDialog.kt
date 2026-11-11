package com.example.inventoryapp.ui.dashboard

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.remote.BarcodeLookupService
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class AddProductDraft(
    val sku: String = "",
    val name: String = "",
    val category: String = "Electronics",
    val subcategory: String = "Audio",
    val brand: String = "",
    val unit: String = "Pcs",
    val sellingPrice: String = "29.99",
    val costPrice: String = "15.00",
    val quantity: String = "10",
    val reorderLevel: String = "5",
    val location: String = "Aisle 1 - Shelf A",
    val imageUri: String? = null,
    val supplierId: String? = null,
    val reorderQty: String = "10"
)

/**
 * Modern Swipe-Up BottomSheet for adding products with camera photo capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    initialDraft: AddProductDraft = AddProductDraft(),
    suppliers: List<SupplierEntity> = emptyList(),
    onDismiss: () -> Unit,
    onRequestScanSku: (AddProductDraft) -> Unit,
    onProductAdded: (ProductEntity) -> Unit
) {
    var sku by remember { mutableStateOf(initialDraft.sku) }
    var name by remember { mutableStateOf(initialDraft.name) }
    var category by remember { mutableStateOf(initialDraft.category) }
    var subcategory by remember { mutableStateOf(initialDraft.subcategory.ifBlank { CategoryHierarchy.getSubcategories(initialDraft.category).firstOrNull() ?: "General" }) }
    var brand by remember { mutableStateOf(initialDraft.brand) }
    var unit by remember { mutableStateOf(initialDraft.unit) }
    var sellingPriceText by remember { mutableStateOf(initialDraft.sellingPrice) }
    var costPriceText by remember { mutableStateOf(initialDraft.costPrice) }
    var quantityText by remember { mutableStateOf(initialDraft.quantity) }
    var reorderLevelText by remember { mutableStateOf(initialDraft.reorderLevel) }
    var location by remember { mutableStateOf(initialDraft.location) }
    var imageUri by remember { mutableStateOf(initialDraft.imageUri) }
    var selectedSupplierId by remember { mutableStateOf(initialDraft.supplierId) }
    var reorderQtyText by remember { mutableStateOf(initialDraft.reorderQty) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var isFetchingOnline by remember { mutableStateOf(false) }
    var onlineFetchSuccessNote by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Camera launcher for taking product photos
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val file = File(context.filesDir, "prod_${UUID.randomUUID()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                imageUri = file.toURI().toString()
            } catch (e: Exception) {
                errorMessage = "Failed to save photo: ${e.message}"
            }
        }
    }

    // Gallery picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    val categoryOptions = listOf("Electronics", "Accessories", "Furniture", "Hardware", "Grocery", "Other")
    val unitOptions = listOf("Pcs", "Box", "Carton", "Kg", "Liter", "Set")

    var categoryExpanded by remember { mutableStateOf(false) }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    fun fetchOnlineDetails(targetSku: String) {
        val clean = targetSku.trim()
        if (clean.length < 4) return
        isFetchingOnline = true
        onlineFetchSuccessNote = null
        errorMessage = null
        coroutineScope.launch {
            try {
                val data = BarcodeLookupService.lookupBarcode(clean)
                if (data != null) {
                    name = data.name
                    brand = data.brand
                    if (data.category in categoryOptions) {
                        category = data.category
                    }
                    if (data.unit in unitOptions) {
                        unit = data.unit
                    }
                    sellingPriceText = String.format(Locale.US, "%.2f", data.suggestedPrice)
                    costPriceText = String.format(Locale.US, "%.2f", data.suggestedPrice * 0.6)
                    onlineFetchSuccessNote = "Product Data Fetched: ${data.name}"
                } else {
                    errorMessage = "Barcode captured ($clean). Product not in online registry; please enter details manually."
                }
            } catch (e: Exception) {
                errorMessage = "Network lookup failed: ${e.message}"
            } finally {
                isFetchingOnline = false
            }
        }
    }

    // Automatically trigger online data lookup if an initial barcode SKU was passed and name is still empty
    LaunchedEffect(initialDraft.sku) {
        if (initialDraft.sku.isNotBlank() && initialDraft.name.isBlank()) {
            fetchOnlineDetails(initialDraft.sku)
        }
    }

    fun buildCurrentDraft() = AddProductDraft(
        sku = sku,
        name = name,
        category = category,
        subcategory = subcategory,
        brand = brand,
        unit = unit,
        sellingPrice = sellingPriceText,
        costPrice = costPriceText,
        quantity = quantityText,
        reorderLevel = reorderLevelText,
        location = location,
        imageUri = imageUri,
        supplierId = selectedSupplierId,
        reorderQty = reorderQtyText
    )

    fun animateDismiss() {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismiss()
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row (Nawano Behance UI Kit: Slide 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add New Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Provide detailed information to create a new item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Required Fields Indicator Note
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "*",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Required fields are marked with red asterisk (*)",
                        color = Color(0xFFDC2626),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Online Fetching Spinner Banner
            if (isFetchingOnline) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fetching product data online...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Online Fetch Success Note
            onlineFetchSuccessNote?.let { note ->
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = note,
                            color = Color(0xFF047857),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Product Photo Capture Section (Nawano UI Kit: Slide 5 Dashed Container with (+) Add Photos)
            Text(
                text = "Photo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!imageUri.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Product Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, NawanoPrimary200.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Photo Attached",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { takePicturePreviewLauncher.launch(null) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retake", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { imageUri = null },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Remove", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NawanoPrimary200,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Photos",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add Photos",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { takePicturePreviewLauncher.launch(null) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, NawanoPrimary100.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp), tint = NawanoPrimary200)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera", style = MaterialTheme.typography.labelSmall, color = NawanoPrimary200, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                onClick = { pickImageLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, NawanoPrimary100.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(13.dp), tint = NawanoPrimary200)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Gallery", style = MaterialTheme.typography.labelSmall, color = NawanoPrimary200, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = "Max file size is 5mb. Only jpg files are supported",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))

            val isSkuError = hasAttemptedSubmit && sku.isBlank()

            // SKU Field with Camera Scan and Online Fetch buttons
            OutlinedTextField(
                value = sku,
                onValueChange = {
                    sku = it
                    errorMessage = null
                    onlineFetchSuccessNote = null
                },
                label = {
                    Text(
                        buildAnnotatedString {
                            append("SKU Barcode Code ")
                            withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                                append("*")
                            }
                        }
                    )
                },
                isError = isSkuError,
                supportingText = if (isSkuError) {
                    { Text("SKU Barcode is required *", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall) }
                } else null,
                placeholder = { Text("e.g. 049000000443 or SKU-1001") },
                leadingIcon = {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sku.length >= 4) {
                            IconButton(
                                onClick = { fetchOnlineDetails(sku) },
                                enabled = !isFetchingOnline
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = "Fetch data online",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRequestScanSku(buildCurrentDraft()) }
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode with Camera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            val isNameError = hasAttemptedSubmit && name.isBlank()

            // Product Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
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
                isError = isNameError,
                supportingText = if (isNameError) {
                    { Text("Product Name is required *", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall) }
                } else null,
                placeholder = { Text("e.g. Coca-Cola Classic, Wireless Mouse") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category & Subcategory Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                buildAnnotatedString {
                                    append("Category ")
                                    withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                                        append("*")
                                    }
                                }
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    val subList = CategoryHierarchy.getSubcategories(option)
                                    subcategory = subList.firstOrNull() ?: "General"
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subcategory Dropdown (hierarchically linked to Category)
                val subcategoryOptions = remember(category) {
                    CategoryHierarchy.getSubcategories(category).ifEmpty { listOf("General") }
                }
                ExposedDropdownMenuBox(
                    expanded = subcategoryExpanded,
                    onExpandedChange = { subcategoryExpanded = !subcategoryExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = subcategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subcategory") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcategoryExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subcategoryExpanded,
                        onDismissRequest = { subcategoryExpanded = false }
                    ) {
                        subcategoryOptions.forEach { subOption ->
                            DropdownMenuItem(
                                text = { Text(subOption) },
                                onClick = {
                                    subcategory = subOption
                                    subcategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Brand & Unit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand / Supplier") },
                    placeholder = { Text("e.g. Logitech, Sony") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.3f),
                    singleLine = true
                )

                // Unit Dropdown
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = !unitExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    unit = option
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isSellingPriceError = hasAttemptedSubmit && sellingPriceText.isNotBlank() && sellingPriceText.toDoubleOrNull() == null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    label = {
                        Text(
                            buildAnnotatedString {
                                append("Selling Price ($) ")
                                withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                                    append("*")
                                }
                            }
                        )
                    },
                    isError = isSellingPriceError,
                    supportingText = if (isSellingPriceError) {
                        { Text("Invalid price *", color = Color(0xFFEF4444), style = MaterialTheme.typography.labelSmall) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { costPriceText = it },
                    label = { Text("Cost Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = {
                        Text(
                            buildAnnotatedString {
                                append("Initial Stock ")
                                withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)) {
                                    append("*")
                                }
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = reorderLevelText,
                    onValueChange = { reorderLevelText = it },
                    label = { Text("Reorder Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Shelf / Storage Location") },
                placeholder = { Text("e.g. Aisle 4 - Shelf C") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reorder Qty & Supplier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = reorderQtyText,
                    onValueChange = { reorderQtyText = it },
                    label = { Text("Reorder Batch Qty") },
                    placeholder = { Text("10") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Supplier Dropdown
                val selectedSupplierName = suppliers.find { it.id == selectedSupplierId }?.name ?: "No Supplier"
                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                    modifier = Modifier.weight(1.5f)
                ) {
                    OutlinedTextField(
                        value = selectedSupplierName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Supplier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Direct Stock)") },
                            onClick = {
                                selectedSupplierId = null
                                supplierDropdownExpanded = false
                            }
                        )
                        suppliers.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSupplierId = s.id
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (sku.isBlank()) {
                            errorMessage = "Please enter or scan a valid SKU barcode."
                            return@Button
                        }
                        if (name.isBlank()) {
                            errorMessage = "Please enter or scan a product name."
                            return@Button
                        }
                        val sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0
                        val costPrice = costPriceText.toDoubleOrNull() ?: 0.0
                        val qty = quantityText.toIntOrNull() ?: 1
                        val reorderLevel = reorderLevelText.toIntOrNull() ?: 5

                        val reorderQty = reorderQtyText.toIntOrNull() ?: 10

                        val newItem = ProductEntity(
                            id = UUID.randomUUID().toString(),
                            sku = sku.trim(),
                            name = name.trim(),
                            category = category,
                            subcategory = subcategory,
                            brand = brand.trim(),
                            costPrice = costPrice,
                            sellingPrice = sellingPrice,
                            quantity = qty,
                            unit = unit,
                            reorderLevel = reorderLevel,
                            reorderQty = reorderQty,
                            supplierId = selectedSupplierId,
                            location = location.ifBlank { "Warehouse A" },
                            imageUri = imageUri
                        )

                        onProductAdded(newItem)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Product", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
