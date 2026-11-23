package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.ProductImageHelper
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRegisterScreen(
    products: List<ProductEntity>,
    unpaidTabsCount: Int,
    cart: Map<String, Int>, // productId -> quantity
    onUpdateCartQuantity: (product: ProductEntity, newQty: Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenUnpaidTabs: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenCheckout: () -> Unit,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSubcategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Beverages", "Meat", "Seafood", "Produce", "Pantry", "Snacks", "Other")

    // Dynamic subcategories based on taxonomy
    val subcategories = remember(selectedCategory) {
        if (selectedCategory == "All") {
            emptyList()
        } else {
            val list = CategoryHierarchy.getSubcategories(selectedCategory)
            if (list.isNotEmpty()) listOf("All $selectedCategory") + list else emptyList()
        }
    }

    // Filter products
    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedSubcategory) {
        products.filter { product ->
            val matchesQuery = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = when (selectedCategory) {
                "All" -> true
                else -> product.category.equals(selectedCategory, ignoreCase = true)
            }

            val matchesSubcategory = when {
                selectedSubcategory == "All" || selectedSubcategory.startsWith("All ") -> true
                else -> product.subcategory.equals(selectedSubcategory, ignoreCase = true)
            }

            matchesQuery && matchesCategory && matchesSubcategory
        }
    }

    // Calculate Cart Totals
    val totalCartItems = cart.values.sum()
    val totalAmountUsd = cart.entries.sumOf { (prodId, qty) ->
        val prod = products.find { it.id == prodId }
        (prod?.sellingPrice ?: 0.0) * qty
    }
    val totalAmountKhr = (totalAmountUsd * 4050.0).toLong()
    val khrFormatted = NumberFormat.getNumberInstance(Locale.US).format(totalAmountKhr)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Bar (POS Register • My Stock • [ 🧾³ ] • [ 📷 ])
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "POS Register",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NawanoPrimary200.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, NawanoPrimary200.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "My Stock",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NawanoPrimary200,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Cart Button in Top Bar
                    IconButton(onClick = onOpenCheckout) {
                        BadgedBox(
                            badge = {
                                if (totalCartItems > 0) {
                                    Badge(
                                        containerColor = NawanoPrimary200,
                                        contentColor = Color.White
                                    ) {
                                        Text("$totalCartItems", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (totalCartItems > 0) Icons.Default.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = "Active Cart",
                                tint = if (totalCartItems > 0) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Unpaid Tabs / Pay Later Badge Button
                    IconButton(onClick = onOpenUnpaidTabs) {
                        BadgedBox(
                            badge = {
                                if (unpaidTabsCount > 0) {
                                    Badge(
                                        containerColor = Color(0xFFF59E0B),
                                        contentColor = Color.White
                                    ) {
                                        Text("$unpaidTabsCount", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = "Unpaid Tabs",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Barcode Scanner Shortcut
                    IconButton(onClick = onOpenScanner) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Search Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search My Stock or scan SKU...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Category Horizontal Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        onClick = {
                            selectedCategory = cat
                            selectedSubcategory = "All"
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 4. Subcategory Horizontal Pills (if available)
            if (subcategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    subcategories.forEach { sub ->
                        val isSelected = selectedSubcategory == sub
                        Surface(
                            onClick = { selectedSubcategory = sub },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NawanoPrimary200.copy(alpha = 0.18f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                text = sub,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) NawanoPrimary200 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4.5. Prominent Sticky Top Cart Summary Banner (Separated completely from bottom navigation bar)
            AnimatedVisibility(
                visible = totalCartItems > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    onClick = onOpenCheckout,
                    shape = RoundedCornerShape(18.dp),
                    color = NawanoPrimary200,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "$totalCartItems Items • $${String.format(Locale.US, "%.2f", totalAmountUsd)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "≈ $khrFormatted ៛",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Checkout",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NawanoPrimary200
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 5. Product 2-Column Grid
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No products found in this category",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp) // Clean padding above bottom navigation bar
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val currentQtyInCart = cart[product.id] ?: 0
                        PosProductCard(
                            product = product,
                            quantityInCart = currentQtyInCart,
                            isDarkTheme = isDarkTheme,
                            onAdd = {
                                onUpdateCartQuantity(product, currentQtyInCart + 1)
                            },
                            onIncrement = {
                                if (currentQtyInCart < product.quantity) {
                                    onUpdateCartQuantity(product, currentQtyInCart + 1)
                                }
                            },
                            onDecrement = {
                                onUpdateCartQuantity(product, (currentQtyInCart - 1).coerceAtLeast(0))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosProductCard(
    product: ProductEntity,
    quantityInCart: Int,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isInCart = quantityInCart > 0
    val isLowStock = product.quantity in 1..10
    val isOutOfStock = product.quantity <= 0

    val cardBg = if (isDarkTheme) {
        if (isInCart) Color(0xFF1E2248) else Color(0xFF191D2D)
    } else {
        if (isInCart) Color(0xFFF5F3FF) else Color.White
    }
    val cardBorder = if (isInCart) {
        NawanoPrimary200
    } else {
        if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
    }
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B)

    val imageModel = remember(product.sku, product.name, product.category, product.imageUri) {
        ProductImageHelper.getProductImageModel(
            sku = product.sku,
            name = product.name,
            category = product.category,
            imageUri = product.imageUri
        )
    }

    Surface(
        onClick = {
            if (!isOutOfStock) {
                if (!isInCart) onAdd() else onIncrement()
            }
        },
        enabled = !isOutOfStock,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(if (isInCart) 1.8.dp else 1.dp, cardBorder),
        shadowElevation = if (isInCart) 4.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Product Image & Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(116.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isDarkTheme) Color(0xFF0F1424) else Color(0xFFF1F5F9))
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle dark gradient at the bottom of the image for visual depth
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Top-Left Badge: In Cart Indicator
                if (isInCart) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NawanoPrimary200,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${quantityInCart}x",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-Right Badge: Stock Status
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isOutOfStock -> Color(0xFFEF4444)
                        isLowStock -> Color(0xFFF59E0B)
                        else -> Color(0xCC0F172A)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isOutOfStock -> Color.White
                                        isLowStock -> Color.White
                                        else -> Color(0xFF10B981)
                                    }
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when {
                                isOutOfStock -> "0 left"
                                else -> "${product.quantity} left"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Category & SKU Tag
            Text(
                text = "${product.category} • #${product.sku}".uppercase(Locale.ROOT),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(3.dp))

            // Product Name (fixed 2 lines for uniform grid height)
            Text(
                text = product.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 2,
                minLines = 2,
                lineHeight = 17.sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Price & Unit
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Text(
                    text = " / ${product.unit}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Action: Professional Add Button OR Stepper
            if (!isInCart) {
                Surface(
                    onClick = {
                        if (!isOutOfStock) onAdd()
                    },
                    enabled = !isOutOfStock,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOutOfStock) {
                        if (isDarkTheme) Color(0xFF2E334D).copy(alpha = 0.4f) else Color(0xFFF1F5F9)
                    } else {
                        NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isOutOfStock) {
                            if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
                        } else {
                            NawanoPrimary200.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isOutOfStock) {
                            Text(
                                text = "Out of Stock",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = NawanoPrimary200,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Add to Cart",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200
                            )
                        }
                    }
                }
            } else {
                // Stepper: [ - ]  quantity in cart  [ + ]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NawanoPrimary200,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (quantityInCart == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                                contentDescription = if (quantityInCart == 1) "Remove from cart" else "Decrement",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "$quantityInCart",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = onIncrement,
                            enabled = quantityInCart < product.quantity,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increment",
                                tint = if (quantityInCart < product.quantity) Color.White else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
