package com.example.inventoryapp.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.*
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import com.example.inventoryapp.util.ProductImageHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
private fun PosTopBarActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    count: Int,
    accentColor: Color,
    isActive: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isActive && count > 0 -> accentColor.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
        isActive -> NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.16f else 0.08f)
        isDarkTheme -> NawanoDarkCard
        else -> NawanoLightSurface
    }
    val borderColor = when {
        isActive && count > 0 -> accentColor.copy(alpha = 0.45f)
        isActive -> NawanoPrimary200.copy(alpha = 0.30f)
        isDarkTheme -> NawanoDarkOutline
        else -> NawanoLightOutline
    }
    val iconTint = when {
        isActive && count > 0 -> accentColor
        isActive -> NawanoPrimary200
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (count > 0) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accentColor)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 99) "99+" else "$count",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosRegisterScreen(
    products: List<ProductEntity>,
    unpaidTabsCount: Int,
    parkedCartsCount: Int = 0,
    salesHistoryCount: Int = 0,
    cart: Map<String, Int>, // productId -> quantity
    onUpdateCartQuantity: (product: ProductEntity, newQty: Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenUnpaidTabs: () -> Unit,
    onOpenSalesHistory: (() -> Unit)? = null,
    onOpenScanner: () -> Unit,
    onOpenCheckout: () -> Unit,
    onHoldCart: () -> Unit = {},
    onOpenPartnerNetwork: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isCartMinimized: Boolean = false,
    isBottomBarVisible: Boolean = true,
    onToggleCartMinimized: (() -> Unit)? = null,
    showSubHeader: Boolean = false,
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSubcategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Beverages", "Meat", "Seafood", "Produce", "Packaging", "Other")

    // Dynamic subcategories based on taxonomy
    val subcategories = remember(selectedCategory) {
        if (selectedCategory == "All") {
            emptyList()
        } else {
            val list = CategoryHierarchy.getSubcategories(selectedCategory)
            if (list.isNotEmpty()) listOf("All $selectedCategory") + list else emptyList()
        }
    }

    // Filter products (Prioritize mockup featured items in exact screenshot order when on All)
    val preferredSkus = listOf("BOX-1001", "TOFU-0501", "WGY-0801", "CRB-0603")
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
        }.sortedWith { a, b ->
            val idxA = preferredSkus.indexOf(a.sku)
            val idxB = preferredSkus.indexOf(b.sku)
            when {
                idxA != -1 && idxB != -1 -> idxA.compareTo(idxB)
                idxA != -1 -> -1
                idxB != -1 -> 1
                else -> 0
            }
        }
    }

    // Calculate Cart Totals
    val totalCartItems = cart.values.sum()
    val cartBottomOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isBottomBarVisible) 104.dp else 14.dp,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "CartBottomOffset"
    )
    val totalAmountUsd = cart.entries.sumOf { (prodId, qty) ->
        val prod = products.find { it.id == prodId }
        (prod?.sellingPrice ?: 0.0) * qty
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 1. Unified Search + Scan Pill and 3 Action Icons (Unpaid Tabs, History, Cart)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) NawanoDarkCard else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else NawanoLightOutline),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search SKU...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onOpenScanner,
                            shape = RoundedCornerShape(10.dp),
                            color = NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.22f else 0.10f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                val totalUnpaid = unpaidTabsCount + parkedCartsCount
                PosTopBarActionPill(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = "Orders & Payment History",
                    count = totalUnpaid,
                    accentColor = if (totalUnpaid > 0) Color(0xFFF59E0B) else NawanoPrimary200,
                    isActive = totalUnpaid > 0 || salesHistoryCount > 0,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        if (totalUnpaid > 0) {
                            onOpenUnpaidTabs()
                        } else {
                            (onOpenSalesHistory ?: onOpenUnpaidTabs).invoke()
                        }
                    }
                )

                PosTopBarActionPill(
                    icon = if (totalCartItems > 0) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                    contentDescription = "Shopping Cart",
                    count = totalCartItems,
                    accentColor = if (totalCartItems > 0) Color(0xFFEF4444) else NawanoPrimary200,
                    isActive = totalCartItems > 0,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        if (totalCartItems > 0) {
                            if (isCartMinimized && onToggleCartMinimized != null) {
                                onToggleCartMinimized()
                            } else {
                                onOpenCheckout()
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(4.dp))

            // 2. Category Horizontal Filter Pills
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
                    contentPadding = PaddingValues(
                        bottom = if (totalCartItems > 0 && !isCartMinimized) {
                            if (isBottomBarVisible) 176.dp else 92.dp
                        } else {
                            if (isBottomBarVisible) 112.dp else 24.dp
                        }
                    )
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val currentQtyInCart = cart[product.id] ?: 0
                        PosProductCard(
                            product = product,
                            quantityInCart = currentQtyInCart,
                            isDarkTheme = isDarkTheme,
                            currency = currency,
                            exchangeRate = exchangeRate,
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

        // Docked Floating Cart Summary Bar (Positioned cleanly ABOVE InventoryBottomBar so nav stays usable)
        AnimatedVisibility(
            visible = totalCartItems > 0 && !isCartMinimized,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = cartBottomOffset)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NawanoGradientStart, NawanoPrimary200)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Cart Icon + Count + Total + Helper Hint (tap opens checkout/review)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenCheckout)
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            val cartDisplayTotal = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)
                            val itemWord = if (totalCartItems == 1) "item" else "items"
                            Text(
                                text = "$cartDisplayTotal • $totalCartItems $itemWord",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to review & pay",
                                fontSize = 10.5.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Hold / Park Cart Pill
                        Surface(
                            onClick = onHoldCart,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Hold Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Hold",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Primary Checkout Pill
                        Surface(
                            onClick = onOpenCheckout,
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Checkout",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NawanoPrimary200
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = NawanoPrimary200,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (onToggleCartMinimized != null) {
                            Surface(
                                onClick = onToggleCartMinimized,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize Cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Floating Reopen Pill (Hovering cleanly above InventoryBottomBar when collapsed)
        AnimatedVisibility(
            visible = totalCartItems > 0 && isCartMinimized,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = cartBottomOffset)
        ) {
            Surface(
                onClick = { onToggleCartMinimized?.invoke() },
                shape = RoundedCornerShape(20.dp),
                color = NawanoPrimary200,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    val cartDisplayTotal = CurrencyFormatter.format(totalAmountUsd, currency, exchangeRate)
                    Text(
                        text = "$totalCartItems in Cart • $cartDisplayTotal",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Reopen",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
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
    currency: AppCurrency = AppCurrency.USD,
    exchangeRate: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val availableStock = (product.quantity - quantityInCart).coerceAtLeast(0)
    val isInCart = quantityInCart > 0
    val isLowStock = availableStock in 1..10
    val isOutOfStock = availableStock <= 0

    val cardBg = if (isDarkTheme) Color(0xFF191D2D) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
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
            if (!isOutOfStock) onAdd()
        },
        enabled = !isOutOfStock,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.dp,
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

                // Top-Right Badge: Stock Status
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isOutOfStock -> Color(0xFFEF4444)
                                        isLowStock -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    }
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = when {
                                isOutOfStock -> "0 left"
                                else -> "$availableStock left"
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
                    text = CurrencyFormatter.format(product.sellingPrice, currency, exchangeRate),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
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

            // Action: Professional Add Button (Matching exact uploaded design)
            Surface(
                onClick = {
                    if (!isOutOfStock) onAdd()
                },
                enabled = !isOutOfStock,
                shape = RoundedCornerShape(10.dp),
                color = NawanoPrimary200.copy(alpha = if (isDarkTheme) 0.12f else 0.08f),
                border = BorderStroke(
                    1.dp,
                    if (isOutOfStock) {
                        if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
                    } else {
                        NawanoPrimary200.copy(alpha = 0.5f)
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
                            text = if (quantityInCart > 0) "Add to Cart ($quantityInCart)" else "Add to Cart",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }
        }
    }
}
