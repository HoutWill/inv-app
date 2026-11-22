package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.ProductImageHelper
import com.example.inventoryapp.util.ProductThumbnail
import java.util.Locale

/**
 * Wholesale Catalog View for the Supplier Portal, matching the reference design:
 * - Search bar with barcode scanner shortcut
 * - Dedicated stock status filter capsules: All, In Stock ("Our Own Stock"), Low Stock, Out of Stock
 * - Category filter capsules (All, Seafood, Meat, Produce, Dairy, Beverages, etc.)
 * - Subheader with "Products (X)" and multi-option sort dropdown (Name, Stock High-Low, Stock Low-High, Price, Recent)
 * - Product cards with thumbnail, tags, edit/delete actions, price, stock pill, + Restock, and stepper.
 */
enum class SupplierStockStatusFilter {
    ALL,
    IN_STOCK,
    LOW_STOCK,
    OUT_OF_STOCK
}

enum class SupplierCatalogSortOption(val displayName: String) {
    NAME_ASC("Name (A to Z)"),
    STOCK_HIGH_TO_LOW("Stock (Highest First)"),
    STOCK_LOW_TO_HIGH("Stock (Lowest First)"),
    PRICE_HIGH_TO_LOW("Price (Highest First)"),
    RECENT("Recently Added")
}

@Composable
fun SupplierCatalogTab(
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    isSortAscending: Boolean = false,
    onToggleSort: (() -> Unit)? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onAddProduct: () -> Unit = {},
    onEditProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onRestockProduct: (ProductEntity) -> Unit = {},
    onQuantityChange: ((ProductEntity, Int) -> Unit)? = null,
    onScanClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    var selectedCategory by remember { mutableStateOf("All") }
    var stockStatusFilter by remember { mutableStateOf(SupplierStockStatusFilter.ALL) }
    var sortOption by remember { mutableStateOf(SupplierCatalogSortOption.NAME_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val categories = remember(products) {
        val standard = listOf("All", "Seafood", "Meat", "Produce", "Dairy", "Beverages", "Pantry", "Snacks")
        val custom = products.map { it.category }.filter { it.isNotBlank() }.distinct()
        standard + custom.filter { c -> standard.none { it.equals(c, ignoreCase = true) } }
    }

    val baseProducts = remember(products, selectedCategory, searchQuery) {
        var list = when (selectedCategory) {
            "All" -> products
            else -> products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.name.lowercase(Locale.ROOT).contains(q) ||
                it.sku.lowercase(Locale.ROOT).contains(q) ||
                it.category.lowercase(Locale.ROOT).contains(q)
            }
        }
        list
    }

    val inStockCount = remember(baseProducts) { baseProducts.count { it.quantity > 0 } }
    val lowStockCount = remember(baseProducts) { baseProducts.count { it.quantity in 1..it.reorderLevel } }
    val outOfStockCount = remember(baseProducts) { baseProducts.count { it.quantity <= 0 } }
    val allCount = baseProducts.size

    val displayedProducts = remember(baseProducts, stockStatusFilter, sortOption) {
        val filtered = when (stockStatusFilter) {
            SupplierStockStatusFilter.ALL -> baseProducts
            SupplierStockStatusFilter.IN_STOCK -> baseProducts.filter { it.quantity > 0 }
            SupplierStockStatusFilter.LOW_STOCK -> baseProducts.filter { it.quantity in 1..it.reorderLevel }
            SupplierStockStatusFilter.OUT_OF_STOCK -> baseProducts.filter { it.quantity <= 0 }
        }
        when (sortOption) {
            SupplierCatalogSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
            SupplierCatalogSortOption.STOCK_HIGH_TO_LOW -> filtered.sortedByDescending { it.quantity }
            SupplierCatalogSortOption.STOCK_LOW_TO_HIGH -> filtered.sortedBy { it.quantity }
            SupplierCatalogSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.costPrice }
            SupplierCatalogSortOption.RECENT -> filtered.sortedByDescending { it.updatedAt }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 170.dp)
    ) {
        // 1. Search Bar with Scanner Shortcut (Matches screenshot: Search 13 products, SKU... [|||||])
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        cursorBrush = SolidColor(NawanoPrimary200),
                        textStyle = TextStyle(
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search ${products.size} products, SKU...",
                                    color = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onScanClicked?.invoke() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pos_barcode),
                            contentDescription = "Scan SKU to Search",
                            tint = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Stock Level Filter Row (Separates "Our Own Stock" from Out of Stock & Low Stock)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" Pill
                val isAllSelected = stockStatusFilter == SupplierStockStatusFilter.ALL
                Surface(
                    onClick = { stockStatusFilter = SupplierStockStatusFilter.ALL },
                    shape = CircleShape,
                    color = if (isAllSelected) NawanoPrimary200 else cardBg,
                    border = if (isAllSelected) null else BorderStroke(1.dp, cardBorder)
                ) {
                    Text(
                        text = "All ($allCount)",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllSelected) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                // "In Stock (Our Own Stock)" Pill with Emerald Green Dot
                val isInStockSelected = stockStatusFilter == SupplierStockStatusFilter.IN_STOCK
                Surface(
                    onClick = { stockStatusFilter = SupplierStockStatusFilter.IN_STOCK },
                    shape = CircleShape,
                    color = if (isInStockSelected) Color(0xFF10B981) else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isInStockSelected) Color(0xFF10B981) else cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isInStockSelected) Color.White else Color(0xFF10B981))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "In Stock ($inStockCount)",
                            fontSize = 12.sp,
                            fontWeight = if (isInStockSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInStockSelected) Color.White else if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                        )
                    }
                }

                // "Low Stock" Pill with Amber Dot
                val isLowStockSelected = stockStatusFilter == SupplierStockStatusFilter.LOW_STOCK
                Surface(
                    onClick = { stockStatusFilter = SupplierStockStatusFilter.LOW_STOCK },
                    shape = CircleShape,
                    color = if (isLowStockSelected) Color(0xFFF59E0B) else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isLowStockSelected) Color(0xFFF59E0B) else cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isLowStockSelected) Color.White else Color(0xFFF59E0B))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Low Stock ($lowStockCount)",
                            fontSize = 12.sp,
                            fontWeight = if (isLowStockSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLowStockSelected) Color.White else if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
                        )
                    }
                }

                // "Out of Stock" Pill with Rose Dot
                val isOutSelected = stockStatusFilter == SupplierStockStatusFilter.OUT_OF_STOCK
                Surface(
                    onClick = { stockStatusFilter = SupplierStockStatusFilter.OUT_OF_STOCK },
                    shape = CircleShape,
                    color = if (isOutSelected) Color(0xFFEF4444) else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isOutSelected) Color(0xFFEF4444) else cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOutSelected) Color.White else Color(0xFFEF4444))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Out of Stock ($outOfStockCount)",
                            fontSize = 12.sp,
                            fontWeight = if (isOutSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isOutSelected) Color.White else if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // 3. Category Filter Capsules (All, Seafood, Meat, Produce, Dairy, etc.)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6C63FF) else cardBg,
                        border = if (isSelected) null else BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)),
                        shadowElevation = if (isSelected) 3.dp else 0.dp
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 4. Sub-header Row: "Products (X)" on left, "⇅ Sort: ... ▾" on right
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (stockStatusFilter == SupplierStockStatusFilter.IN_STOCK) "In-Stock Products" else "Products",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (displayedProducts.isNotEmpty()) {
                        Text(
                            text = "(${displayedProducts.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }

                // "⇅ Sort ▾" Sort button
                Box {
                    Surface(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = when (sortOption) {
                                    SupplierCatalogSortOption.NAME_ASC -> "Sort: Name"
                                    SupplierCatalogSortOption.STOCK_HIGH_TO_LOW -> "Sort: Stock ↓"
                                    SupplierCatalogSortOption.STOCK_LOW_TO_HIGH -> "Sort: Stock ↑"
                                    SupplierCatalogSortOption.PRICE_HIGH_TO_LOW -> "Sort: Price ↓"
                                    SupplierCatalogSortOption.RECENT -> "Sort: Recent"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SupplierCatalogSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortOption == option) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    sortOption = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. Product Cards List
        if (displayedProducts.isEmpty()) {
            item {
                EmptyZeroState(
                    title = if (searchQuery.isNotBlank()) "No match for '$searchQuery'"
                            else if (stockStatusFilter != SupplierStockStatusFilter.ALL) "No ${stockStatusFilter.name.lowercase().replace('_', ' ')} products"
                            else "No Products in $selectedCategory",
                    subtitle = "Tap '+ Add Product' to register wholesale inventory items.",
                    icon = Icons.Default.Category,
                    isDarkTheme = isDarkTheme,
                    actionLabel = "+ Add Product",
                    onAction = onAddProduct
                )
            }
        } else {
            items(displayedProducts, key = { it.id }) { product ->
                SupplierCatalogProductCard(
                    product = product,
                    isDarkTheme = isDarkTheme,
                    onEdit = { onEditProduct(product) },
                    onDelete = { onDeleteProduct(product) },
                    onRestock = { onRestockProduct(product) },
                    onQuantityChange = onQuantityChange
                )
            }
        }
    }
}

/**
 * Product Card for Supplier Catalog, matching the reference design:
 * - Square Product Image (54dp rounded 14dp)
 * - Title + Category Badge + #SKU
 * - Edit Pencil (grey) and Delete Trash (red) top-right
 * - Price ($X.XX / Unit in bright emerald green) + Stock Status Pill on right
 * - Bottom Row: [+ Restock] pill on left, Tactile Stepper on right.
 */
@Composable
fun SupplierCatalogProductCard(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestock: () -> Unit = {},
    onQuantityChange: ((ProductEntity, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) Color(0xFF191D2D) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B)

    val isLowStock = product.quantity <= product.reorderLevel
    val stockPillBg = when {
        product.quantity <= 0 -> if (isDarkTheme) Color(0xFF3B1818) else Color(0xFFFEE2E2)
        isLowStock -> if (isDarkTheme) Color(0xFF382710) else Color(0xFFFEF3C7)
        else -> if (isDarkTheme) Color(0xFF102E29) else Color(0xFFD1FAE5)
    }
    val stockPillTextColor = when {
        product.quantity <= 0 -> Color(0xFFEF4444)
        isLowStock -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
    val stockPillLabel = when {
        product.quantity <= 0 -> "Out of stock"
        isLowStock -> "Low Stock (${product.quantity} ${product.unit})"
        else -> "${product.quantity} in stock"
    }

    val skuLabel = if (product.sku.startsWith("#")) product.sku else "#${product.sku}"

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ROW 1: Thumbnail + Title/Tags + Edit & Delete Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 54dp Rounded Product Thumbnail
                ProductThumbnail(
                    product = product,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Category/SKU badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF282B44) else Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = product.category.ifBlank { "General" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color(0xFFA5B4FC) else NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "• $skuLabel",
                            fontSize = 11.sp,
                            letterSpacing = (-0.2).sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Edit pencil & Delete trash
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ROW 2: Wholesale Price ($X.XX / Unit) on Left, Stock Status Pill on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bright emerald green price
                Text(
                    text = "$${String.format(Locale.US, "%.2f", product.costPrice)} / ${product.unit}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color(0xFF10B981)
                )

                // Stock status pill: ● X in stock
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = stockPillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stockPillTextColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stockPillLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = stockPillTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ROW 3: [+ Restock] Button on Left, Tactile Stepper [ - ] Qty [ + ] on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [+ Restock] pill button
                Surface(
                    onClick = onRestock,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF22263C) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF333856) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Restock",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFA5B4FC) else NawanoPrimary200
                        )
                    }
                }

                // Tactile Stepper: [ - ]  Quantity  [ + ]
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDarkTheme) Color(0xFF22263C) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF333856) else Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Minus Button
                        Surface(
                            onClick = { onQuantityChange?.invoke(product, -1) },
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF2D324E) else Color.White,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = textPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "${product.quantity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Plus Button (Solid Purple)
                        Surface(
                            onClick = { onQuantityChange?.invoke(product, 1) },
                            shape = CircleShape,
                            color = Color(0xFF6C63FF),
                            modifier = Modifier.size(28.dp)
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
                }
            }
        }
    }
}
