package com.example.inventoryapp.ui.supplier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import java.util.Locale

/**
 * Wholesale Catalog View for the Supplier Portal, matching the visual polish of Middleman's Items Catalog Tab.
 */
@Composable
fun SupplierCatalogTab(
    products: List<ProductEntity>,
    isDarkTheme: Boolean = false,
    isSortAscending: Boolean,
    onToggleSort: () -> Unit,
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
    val categories = listOf("All", "Seafood", "Meat", "Produce", "Dairy", "Hardware", "Low Stock")

    val filteredProducts = remember(products, selectedCategory) {
        when (selectedCategory) {
            "All" -> products
            "Low Stock" -> products.filter { it.quantity <= it.reorderLevel }
            else -> products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
    ) {
        // 1. Category Filter Chips (Matching Middleman NawanoActivityFilterChips)
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
                        shadowElevation = if (isSelected) 3.dp else (if (isDarkTheme) 0.dp else 1.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Summary & Action Bar: Count, Sort Button, and Add Product Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProducts.size} Products",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Minimal Sort Button
                    Surface(
                        onClick = onToggleSort,
                        shape = RoundedCornerShape(10.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = if (isSortAscending) "Price" else "Recent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }

                    // Prominent Add Product Button
                    Button(
                        onClick = onAddProduct,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Add Product",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Product Cards List
        if (filteredProducts.isEmpty()) {
            item {
                EmptyZeroState(
                    title = "No Products in $selectedCategory",
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
 * Product Card for Supplier Catalog, matching Middleman InventoryItemCard layout and polish.
 */
@Composable
fun SupplierCatalogProductCard(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestock: () -> Unit = {},
    onQuantityChange: ((ProductEntity, Int) -> Unit)? = null
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    val isLowStock = product.quantity <= product.reorderLevel
    val (stockText, stockColor, stockContainerColor) = when {
        product.quantity <= 0 -> Triple("Out of Stock", Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.12f))
        isLowStock -> Triple("Low Stock (${product.quantity} ${product.unit})", Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.12f))
        else -> Triple("${product.quantity} ${product.unit} in stock", Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.12f))
    }

    val categoryIcon: ImageVector = when (product.category.lowercase(Locale.ROOT)) {
        "seafood", "fish" -> Icons.Default.Water
        "meat", "poultry" -> Icons.Default.Restaurant
        "produce", "vegetables", "fruits" -> Icons.Default.Eco
        "dairy", "beverages" -> Icons.Default.LocalDrink
        else -> Icons.Default.Inventory2
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Thumbnail Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NawanoPrimary200.copy(alpha = 0.12f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = product.category,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Product Title, Category Chip, and SKU
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) NawanoPrimary200.copy(alpha = 0.2f) else Color(0xFFF3E8FF)
                        ) {
                            Text(
                                text = product.category.ifBlank { "Wholesale" },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = product.sku,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }
                }

                // Edit & Delete Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Product",
                            tint = if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDarkTheme) NawanoDarkOutline.copy(alpha = 0.5f) else Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Financial & Stock Stepper Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wholesale Price & Stock Status
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", product.costPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = " / ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = stockContainerColor,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = stockText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = stockColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Restock button & Tactile Stepper buttons (matching Middleman stock adjuster)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Restock Dialog Button
                    Surface(
                        onClick = onRestock,
                        shape = RoundedCornerShape(8.dp),
                        color = NawanoPrimary200.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Restock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                        }
                    }

                    if (onQuantityChange != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { onQuantityChange(product, -1) },
                                shape = CircleShape,
                                color = if (isDarkTheme) NawanoDark400 else Color(0xFFF1F5F9),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "−",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = textPrimary
                                    )
                                }
                            }

                            Text(
                                text = "${product.quantity}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = textPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Surface(
                                onClick = { onQuantityChange(product, 1) },
                                shape = CircleShape,
                                color = NawanoPrimary200,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
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
}
