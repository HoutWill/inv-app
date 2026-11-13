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
 * - Category filter capsules (All, Seafood, Meat, Produce, Dairy, Beverages, etc.)
 * - Subheader with "Products", "● In Stock" toggle, and "⇅ Recent ▾" sort dropdown
 * - Product cards with thumbnail, tags, edit/delete actions, price, stock pill, + Restock, and stepper.
 */
@Composable
fun SupplierCatalogTab(
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    isSortAscending: Boolean,
    onToggleSort: () -> Unit,
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
    var isInStockOnly by remember { mutableStateOf(false) }

    val categories = listOf("All", "Seafood", "Meat", "Produce", "Dairy", "Beverages", "Pantry", "Snacks")

    val filteredProducts = remember(products, selectedCategory, isInStockOnly, searchQuery, isSortAscending) {
        var list = when (selectedCategory) {
            "All" -> products
            else -> products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        if (isInStockOnly) {
            list = list.filter { it.quantity > 0 }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.name.lowercase(Locale.ROOT).contains(q) ||
                it.sku.lowercase(Locale.ROOT).contains(q) ||
                it.category.lowercase(Locale.ROOT).contains(q)
            }
        }
        if (isSortAscending) list.sortedBy { it.costPrice } else list.sortedByDescending { it.updatedAt }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 140.dp)
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

        // 2. Category Filter Capsules (All, Seafood, Meat, Produce, ● In Stock, etc.)
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
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }

                // "● In Stock" toggle filter capsule in category scroll (Matches reference design)
                Surface(
                    onClick = { isInStockOnly = !isInStockOnly },
                    shape = CircleShape,
                    color = if (isInStockOnly) Color(0xFF10B981).copy(alpha = 0.2f) else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isInStockOnly) Color(0xFF10B981).copy(alpha = 0.6f) else if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isInStockOnly) Color(0xFF10B981) else textSecondary.copy(alpha = 0.6f))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "In Stock",
                            fontSize = 13.sp,
                            fontWeight = if (isInStockOnly) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInStockOnly) Color(0xFF10B981) else textSecondary
                        )
                    }
                }
            }
        }

        // 3. Sub-header Row: "Products (X)" on left, "⇅ Recent ▾" on right (Matches reference design)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Products (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                // "⇅ Recent ▾" Sort button
                Surface(
                    onClick = onToggleSort,
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
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isSortAscending) "Price" else "Recent",
                            fontSize = 12.sp,
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
            }
        }

        // 4. Product Cards List
        if (filteredProducts.isEmpty()) {
            item {
                EmptyZeroState(
                    title = if (searchQuery.isNotBlank()) "No match for '$searchQuery'" else "No Products in $selectedCategory",
                    subtitle = "Tap '+ Add Product' to register wholesale inventory items.",
                    icon = Icons.Default.Category,
                    isDarkTheme = isDarkTheme,
                    actionLabel = "+ Add Product",
                    onAction = onAddProduct
                )
            }
        } else {
            items(filteredProducts, key = { it.id }) { product ->
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

            // ROW 3: Tactile Stepper [ - ] Qty [ + ] on Right (Matches reference design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
