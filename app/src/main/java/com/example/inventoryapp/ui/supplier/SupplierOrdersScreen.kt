package com.example.inventoryapp.ui.supplier

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.scanner.BarcodeScannerScreen
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class SupplierScannerTarget {
    NONE,
    GENERAL_SCAN,
    CATALOG_SKU_SCAN,
    DISPATCH_TRACKING_SCAN,
    PACKING_CHECKLIST_SCAN,
    CREATE_ORDER_SKU_SCAN
}

/**
 * Supplier Portal Main Coordinator Screen.
 * Orchestrates Orders, Catalog, Freight Shipments, Vendor Analytics, and Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierOrdersScreen(
    user: UserEntity,
    orders: List<PurchaseOrderEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {},
    onConfirmOrder: (String, String?) -> Unit = { _, _ -> },
    onRejectOrder: (String, String) -> Unit = { _, _ -> },
    onMarkDelivered: (orderId: String, trackingNumber: String?, carrier: String?, estArrival: String?, totalWeight: String?, note: String?) -> Unit = { _, _, _, _, _, _ -> },
    onCreateOrder: (title: String, quantity: Int, price: Double, estArrival: String) -> Unit = { _, _, _, _ -> },
    onUpdateOrder: (orderId: String, title: String, quantity: Int, price: Double) -> Unit = { _, _, _, _ -> },
    onDeleteOrder: (String) -> Unit = {},
    onAddProduct: (ProductEntity) -> Unit = {},
    onUpdateProduct: (ProductEntity) -> Unit = {},
    onDeleteProduct: (ProductEntity) -> Unit = {},
    onLoadOrderDetails: suspend (String) -> OrderWithItems? = { null },
    onLogout: () -> Unit = {},
    onSwitchToMiddleman: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Active View Tab:
    // 0: Home (Orders), 1: Items (Wholesale Catalog), 2: Stats (Analytics), 3: Settings
    var activeViewTab by remember { mutableIntStateOf(0) }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    // Drawer, Modals & Dialogs State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showNotificationSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToDelete by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var orderToDispatch by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var orderToViewDetails by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var detailedOrderWithItems by remember { mutableStateOf<OrderWithItems?>(null) }

    // Live Scanner & Search State
    var scannerTarget by remember { mutableStateOf(SupplierScannerTarget.NONE) }
    var scannedSkuForCatalog by remember { mutableStateOf("") }
    var scannedTrackingForDispatch by remember { mutableStateOf("") }
    var scannedSkuForPacking by remember { mutableStateOf<String?>(null) }
    var scannedSkuForNewOrder by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSortAscending by remember { mutableStateOf(false) }

    // Catalog Modals
    var showAddCatalogItemDialog by remember { mutableStateOf(false) }
    var catalogItemToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var productToRestock by remember { mutableStateOf<ProductEntity?>(null) }
    var showQuickAddChoiceSheet by remember { mutableStateOf(false) }

    // Metrics calculations
    val totalOrders = orders.size
    val totalSkus = remember(orders) { orders.map { it.orderNumber }.distinct().size }
    val totalQty = remember(orders) { orders.sumOf { it.totalQuantity } }
    val totalValue = remember(orders) { orders.sumOf { it.totalAmount } }
    val inTransitCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.DELIVERED } }
    val pendingCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.PENDING } }
    val confirmedCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.CONFIRMED } }
    val completedCount = remember(orders) { orders.count { it.status == PurchaseOrderStatus.RECEIVED } }

    val filterTabsList = remember(orders.size, pendingCount, confirmedCount, inTransitCount, completedCount) {
        listOf(
            "All (${orders.size})",
            "Incoming ($pendingCount)",
            "Confirmed ($confirmedCount)",
            "Transit ($inTransitCount)",
            "Completed ($completedCount)"
        )
    }

    val filteredOrders = remember(orders, selectedFilterIndex, isSortAscending, searchQuery) {
        val list = when (selectedFilterIndex) {
            1 -> orders.filter { it.status == PurchaseOrderStatus.PENDING }
            2 -> orders.filter { it.status == PurchaseOrderStatus.CONFIRMED }
            3 -> orders.filter { it.status == PurchaseOrderStatus.DELIVERED }
            4 -> orders.filter { it.status == PurchaseOrderStatus.RECEIVED }
            else -> orders
        }.filter {
            if (searchQuery.isBlank()) true
            else it.orderNumber.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true)
        }
        if (isSortAscending) list.sortedBy { it.totalAmount } else list.sortedByDescending { it.createdAt }
    }

    val filteredCatalogProducts = remember(products, searchQuery, isSortAscending) {
        val list = if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
        }
        if (isSortAscending) list.sortedBy { it.costPrice } else list.sortedByDescending { it.updatedAt }
    }

    val initials = remember(user.fullName) {
        val parts = user.fullName.trim().split(" ")
        if (parts.size >= 2) {
            "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        } else {
            user.fullName.take(2).uppercase()
        }
    }

    // Camera Barcode Scanner View Overlay with Full Contextual Target Dispatch
    if (scannerTarget != SupplierScannerTarget.NONE) {
        BarcodeScannerScreen(
            onBarcodeScanned = { scanned ->
                val clean = scanned.trim()
                when (scannerTarget) {
                    SupplierScannerTarget.GENERAL_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        // Check if matches order number or tracking
                        val matchedOrder = orders.find {
                            it.orderNumber.contains(clean, ignoreCase = true) ||
                            (it.trackingNumber.isNotBlank() && it.trackingNumber.contains(clean, ignoreCase = true))
                        }
                        if (matchedOrder != null) {
                            orderToViewDetails = matchedOrder
                            scope.launch {
                                detailedOrderWithItems = onLoadOrderDetails(matchedOrder.id)
                            }
                            Toast.makeText(context, "Matched Order: ${matchedOrder.orderNumber}", Toast.LENGTH_SHORT).show()
                        } else {
                            // Check if matches catalog product
                            val matchedProd = products.find { it.sku.contains(clean, ignoreCase = true) }
                            if (matchedProd != null) {
                                catalogItemToEdit = matchedProd
                                Toast.makeText(context, "Matched Catalog Item: ${matchedProd.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                // Neither order nor catalog -> offer to register into wholesale catalog
                                scannedSkuForCatalog = clean
                                showAddCatalogItemDialog = true
                                Toast.makeText(context, "SKU '$clean' scanned. Add to Wholesale Catalog?", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    SupplierScannerTarget.CATALOG_SKU_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForCatalog = clean
                        showAddCatalogItemDialog = true
                        Toast.makeText(context, "Scanned SKU: $clean", Toast.LENGTH_SHORT).show()
                    }
                    SupplierScannerTarget.DISPATCH_TRACKING_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedTrackingForDispatch = clean
                        Toast.makeText(context, "Waybill Scanned: $clean", Toast.LENGTH_SHORT).show()
                    }
                    SupplierScannerTarget.PACKING_CHECKLIST_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForPacking = clean
                    }
                    SupplierScannerTarget.CREATE_ORDER_SKU_SCAN -> {
                        scannerTarget = SupplierScannerTarget.NONE
                        scannedSkuForNewOrder = clean
                        showCreateDialog = true
                    }
                    SupplierScannerTarget.NONE -> {}
                }
            },
            onClose = {
                when (scannerTarget) {
                    SupplierScannerTarget.CATALOG_SKU_SCAN -> showAddCatalogItemDialog = true
                    SupplierScannerTarget.CREATE_ORDER_SKU_SCAN -> showCreateDialog = true
                    else -> {}
                }
                scannerTarget = SupplierScannerTarget.NONE
            }
        )
        return
    }

    val bgMain = if (isDarkTheme) NawanoDark400 else NawanoNeutral50
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SupplierDrawerSheet(
                userName = user.fullName.replace(" (Supplier)", ""),
                initials = initials,
                activeViewTab = activeViewTab,
                onTabSelected = { tab ->
                    activeViewTab = tab
                    scope.launch { drawerState.close() }
                },
                ordersCount = orders.size,
                catalogCount = products.size,
                shipmentsCount = inTransitCount,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onLogout = onLogout,
                onSwitchToMiddleman = onSwitchToMiddleman,
                onOpenShipments = {
                    scope.launch { drawerState.close() }
                    activeViewTab = 0
                    selectedFilterIndex = 2
                },
                onOpenNotifications = {
                    scope.launch { drawerState.close() }
                    showNotificationSheet = true
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgMain)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modular Top App Bar
                SupplierTopBar(
                    userName = user.fullName.replace(" (Supplier)", ""),
                    initials = initials,
                    activeViewTab = activeViewTab,
                    ordersCount = orders.size,
                    catalogCount = products.size,
                    shipmentsCount = inTransitCount,
                    pendingNotificationCount = pendingCount,
                    isDarkTheme = isDarkTheme,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleTheme = onToggleTheme,
                    onOpenNotifications = { showNotificationSheet = true },
                    onOpenScanner = { scannerTarget = SupplierScannerTarget.GENERAL_SCAN },
                    onQuickAdd = {
                        if (activeViewTab == 1) {
                            scannedSkuForCatalog = ""
                            showAddCatalogItemDialog = true
                        } else {
                            showQuickAddChoiceSheet = true
                        }
                    },
                    showTitleAndLogo = false
                )

                // Main Content Switching by Tab
                when (activeViewTab) {
                    0 -> {
                        // TAB 0: Clean Minimal Wholesale Orders & Catalog View (Home)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
                        ) {
                            // 1. Modern Top Search Bar on Home
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
                                            onValueChange = { searchQuery = it },
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
                                                        text = "Search orders, SKU, supplier...",
                                                        color = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF94A3B8),
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
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
                                            onClick = { scannerTarget = SupplierScannerTarget.GENERAL_SCAN },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_pos_barcode),
                                                contentDescription = "Scan Barcode",
                                                tint = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Horizontal Metric Cards Row (Clean 1-Word Labels)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SupplierMetricCard(
                                        value = "$totalOrders",
                                        label = "Orders",
                                        isDarkTheme = isDarkTheme
                                    )
                                    SupplierMetricCard(
                                        value = "${products.size}",
                                        label = "Catalog",
                                        isDarkTheme = isDarkTheme
                                    )
                                    SupplierMetricCard(
                                        value = "$inTransitCount",
                                        label = "Shipments",
                                        isDarkTheme = isDarkTheme
                                    )
                                    SupplierMetricCard(
                                        value = "$${String.format(Locale.US, "%,.0f", totalValue)}",
                                        label = "Value",
                                        isDarkTheme = isDarkTheme
                                    )
                                }
                            }

                            // 3. Activity Filter Chips Row (Clean 1-Word Filter Pills)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filterTabsList.forEachIndexed { index, filterName ->
                                        val isSelected = selectedFilterIndex == index
                                        Surface(
                                            onClick = { selectedFilterIndex = index },
                                            shape = CircleShape,
                                            color = if (isSelected) Color(0xFF6C63FF) else cardBg,
                                            border = if (isSelected) null else BorderStroke(1.dp, if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)),
                                            shadowElevation = if (isSelected) 3.dp else 0.dp
                                        ) {
                                            Text(
                                                text = filterName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else textSecondary,
                                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Section Header: Minimal 1-Word Title
                            item {
                                val sectionTitle = when (selectedFilterIndex) {
                                    1 -> "Incoming (${filteredOrders.size})"
                                    2 -> "Confirmed (${filteredOrders.size})"
                                    3 -> "Transit (${filteredOrders.size})"
                                    4 -> "Completed (${filteredOrders.size})"
                                    else -> "Orders (${filteredOrders.size})"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = { selectedFilterIndex = 1 },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "View all >",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = NawanoPrimary200
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Orders List Items
                            if (filteredOrders.isEmpty()) {
                                item {
                                    EmptyZeroState(
                                        title = "No Incoming Goods",
                                        subtitle = "There are no incoming goods requests matching this filter.",
                                        icon = Icons.Default.LocalShipping,
                                        isDarkTheme = isDarkTheme,
                                        actionLabel = "+ Create Restock Order",
                                        onAction = { showCreateDialog = true }
                                    )
                                }
                            } else {
                                items(filteredOrders, key = { it.id }) { order ->
                                    SupplierDashboardCard(
                                        order = order,
                                        isDarkTheme = isDarkTheme,
                                        onAccept = {
                                            onConfirmOrder(order.id, null)
                                            Toast.makeText(context, "Order ${order.orderNumber} Accepted!", Toast.LENGTH_SHORT).show()
                                        },
                                        onReject = {
                                            onRejectOrder(order.id, "Rejected by supplier")
                                            Toast.makeText(context, "Order ${order.orderNumber} Rejected.", Toast.LENGTH_SHORT).show()
                                        },
                                        onSend = {
                                            orderToDispatch = order
                                        },
                                        onEdit = { orderToEdit = order },
                                        onDelete = { orderToDelete = order },
                                        onView = {
                                            orderToViewDetails = order
                                            scope.launch {
                                                detailedOrderWithItems = onLoadOrderDetails(order.id)
                                            }
                                        }
                                    )
                                }
                            }

                            // 6. Wholesale Products Catalog Section on Home (Includes the Home with the reference design!)
                            if (products.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Products (${products.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        TextButton(onClick = { activeViewTab = 1 }) {
                                            Text(
                                                text = "View all >",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = NawanoPrimary200
                                            )
                                        }
                                    }
                                }

                                items(products.take(6), key = { "home_prod_${it.id}" }) { product ->
                                    SupplierCatalogProductCard(
                                        product = product,
                                        isDarkTheme = isDarkTheme,
                                        onEdit = { catalogItemToEdit = product },
                                        onDelete = { productToDelete = product },
                                        onRestock = { productToRestock = product },
                                        onQuantityChange = { prod, delta ->
                                            val newQty = (prod.quantity + delta).coerceAtLeast(0)
                                            onUpdateProduct(prod.copy(quantity = newQty))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // TAB 1: Dedicated Wholesale Catalog Tab
                        SupplierCatalogTab(
                            products = filteredCatalogProducts,
                            isDarkTheme = isDarkTheme,
                            isSortAscending = isSortAscending,
                            onToggleSort = { isSortAscending = !isSortAscending },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            onAddProduct = {
                                scannedSkuForCatalog = ""
                                showAddCatalogItemDialog = true
                            },
                            onEditProduct = { catalogItemToEdit = it },
                            onDeleteProduct = { productToDelete = it },
                            onRestockProduct = { prod -> productToRestock = prod },
                            onQuantityChange = { prod, delta ->
                                val newQty = (prod.quantity + delta).coerceAtLeast(0)
                                onUpdateProduct(prod.copy(quantity = newQty))
                            },
                            onScanClicked = {
                                scannerTarget = SupplierScannerTarget.CATALOG_SKU_SCAN
                            }
                        )
                    }
                    2 -> {
                        // TAB 2: Vendor Analytics & Performance Screen (Stats)
                        SupplierAnalyticsScreen(
                            orders = orders,
                            products = products,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    3 -> {
                        // TAB 3: Supplier Profile & Settings
                        SupplierSettingsScreen(
                            user = user,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            onLogout = onLogout
                        )
                    }
                }
            }

            // Floating Action Button (+ Add) visible on Wholesale Catalog (matching reference design)
            if (activeViewTab == 1) {
                FloatingActionButton(
                    onClick = {
                        scannedSkuForCatalog = ""
                        showAddCatalogItemDialog = true
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF6C63FF),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 86.dp)
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Floating Bottom Navigation Dock with Central Elevated Scanner FAB
            SupplierBottomBar(
                activeViewTab = activeViewTab,
                onTabSelected = { activeViewTab = it },
                onScanClicked = {
                    scannerTarget = SupplierScannerTarget.GENERAL_SCAN
                },
                onOpenMenu = {
                    scope.launch { drawerState.open() }
                },
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    // Modal 1: Supplier Notifications Sheet
    if (showNotificationSheet) {
        SupplierNotificationsSheet(
            orders = orders,
            isDarkTheme = isDarkTheme,
            onDismiss = { showNotificationSheet = false }
        )
    }

    // Modal 2: Detailed Order Breakdown & Packing Slip Sheet
    orderToViewDetails?.let { order ->
        SupplierOrderDetailSheet(
            order = order,
            orderWithItems = detailedOrderWithItems,
            isDarkTheme = isDarkTheme,
            packedItemSku = scannedSkuForPacking,
            onDismiss = {
                orderToViewDetails = null
                detailedOrderWithItems = null
                scannedSkuForPacking = null
            },
            onSend = {
                orderToViewDetails = null
                orderToDispatch = order
            },
            onAccept = {
                onConfirmOrder(order.id, null)
                Toast.makeText(context, "Order ${order.orderNumber} Accepted!", Toast.LENGTH_SHORT).show()
            },
            onRequestScanToPack = {
                scannerTarget = SupplierScannerTarget.PACKING_CHECKLIST_SCAN
            },
            onEditOrder = {
                val target = orderToViewDetails
                orderToViewDetails = null
                orderToEdit = target
            },
            onDeleteOrder = {
                val target = orderToViewDetails
                orderToViewDetails = null
                orderToDelete = target
            }
        )
    }

    // Modal 3: Dispatch Outbound Freight Dialog
    orderToDispatch?.let { order ->
        DispatchOutboundDialog(
            order = order,
            initialTracking = scannedTrackingForDispatch,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                orderToDispatch = null
                scannedTrackingForDispatch = ""
            },
            onRequestScanTracking = {
                scannerTarget = SupplierScannerTarget.DISPATCH_TRACKING_SCAN
            },
            onConfirmDispatch = { tracking, carrier, eta, weight, note ->
                onMarkDelivered(order.id, tracking, carrier, eta, weight, note)
                orderToDispatch = null
                scannedTrackingForDispatch = ""
                Toast.makeText(context, "Dispatched ${order.orderNumber} via $carrier!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 4: Create Order Bottom Sheet
    if (showCreateDialog) {
        CreateOrderBottomSheet(
            products = products,
            scannedSku = scannedSkuForNewOrder,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showCreateDialog = false
                scannedSkuForNewOrder = null
            },
            onRequestScanProduct = {
                showCreateDialog = false
                scannerTarget = SupplierScannerTarget.CREATE_ORDER_SKU_SCAN
            },
            onSave = { title, qty, price, arrival ->
                onCreateOrder(title, qty, price, arrival)
                showCreateDialog = false
                scannedSkuForNewOrder = null
                Toast.makeText(context, "Order issued to Middleman HQ!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 5: Edit Order Dialog
    orderToEdit?.let { order ->
        EditOrderDialog(
            order = order,
            isDarkTheme = isDarkTheme,
            onDismiss = { orderToEdit = null },
            onSave = { title, qty, price ->
                onUpdateOrder(order.id, title, qty, price)
                orderToEdit = null
                Toast.makeText(context, "Updated ${order.orderNumber}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 6: Delete Order Confirmation Bottom Sheet (No AlertDialog!)
    orderToDelete?.let { order ->
        DeleteOrderBottomSheet(
            order = order,
            isDarkTheme = isDarkTheme,
            onDismiss = { orderToDelete = null },
            onConfirmDelete = {
                onDeleteOrder(order.id)
                orderToDelete = null
                Toast.makeText(context, "Order deleted.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 6b: Delete Product Confirmation Bottom Sheet (No AlertDialog!)
    productToDelete?.let { prod ->
        DeleteProductBottomSheet(
            product = prod,
            isDarkTheme = isDarkTheme,
            onDismiss = { productToDelete = null },
            onConfirmDelete = {
                onDeleteProduct(prod)
                productToDelete = null
                Toast.makeText(context, "Removed ${prod.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 7: Add Catalog Item Dialog
    if (showAddCatalogItemDialog) {
        AddCatalogProductDialog(
            initialSku = scannedSkuForCatalog,
            isDarkTheme = isDarkTheme,
            onDismiss = {
                showAddCatalogItemDialog = false
                scannedSkuForCatalog = ""
            },
            onRequestScanSku = {
                showAddCatalogItemDialog = false
                scannerTarget = SupplierScannerTarget.CATALOG_SKU_SCAN
            },
            onSave = { newProd ->
                onAddProduct(newProd)
                showAddCatalogItemDialog = false
                scannedSkuForCatalog = ""
                Toast.makeText(context, "Added '${newProd.name}' to Wholesale Catalog!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 8: Edit Catalog Item Dialog
    catalogItemToEdit?.let { prod ->
        EditCatalogProductDialog(
            product = prod,
            isDarkTheme = isDarkTheme,
            onDismiss = { catalogItemToEdit = null },
            onSave = { updatedProd ->
                onUpdateProduct(updatedProd)
                catalogItemToEdit = null
                Toast.makeText(context, "Updated '${updatedProd.name}'!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 9: Quick Restock Quantity Dialog
    productToRestock?.let { prod ->
        RestockProductDialog(
            product = prod,
            isDarkTheme = isDarkTheme,
            onDismiss = { productToRestock = null },
            onRestockConfirmed = { qtyToAdd ->
                val newQty = prod.quantity + qtyToAdd
                onUpdateProduct(prod.copy(quantity = newQty, updatedAt = System.currentTimeMillis()))
                productToRestock = null
                Toast.makeText(context, "Restocked +$qtyToAdd ${prod.unit} of ${prod.name}!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal 10: Top Bar Quick Actions Bottom Sheet
    if (showQuickAddChoiceSheet) {
        SupplierQuickActionSheet(
            isDarkTheme = isDarkTheme,
            onDismiss = { showQuickAddChoiceSheet = false },
            onAddProduct = {
                showQuickAddChoiceSheet = false
                scannedSkuForCatalog = ""
                showAddCatalogItemDialog = true
            },
            onRestockOrder = {
                showQuickAddChoiceSheet = false
                showCreateDialog = true
            },
            onScan = {
                showQuickAddChoiceSheet = false
                scannerTarget = SupplierScannerTarget.GENERAL_SCAN
            }
        )
    }
}

/**
 * Bottom Sheet opened by the top-bar '+' button, giving instant access to:
 * - Add Wholesale Product to catalog
 * - Issue Restock Order to Middleman HQ
 * - Scan Item Barcode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierQuickActionSheet(
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onAddProduct: () -> Unit,
    onRestockOrder: () -> Unit,
    onScan: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Action 1: Add New Product to Catalog
            QuickActionOptionCard(
                title = "Add Wholesale Product",
                subtitle = "Register a new product, SKU, and wholesale price",
                icon = Icons.Default.AddBox,
                isDarkTheme = isDarkTheme,
                onClick = onAddProduct
            )

            // Action 2: Restock / Issue Inbound Order
            QuickActionOptionCard(
                title = "Issue Restock Order",
                subtitle = "Create a restock shipment to Middleman HQ",
                icon = Icons.Default.AddBusiness,
                isDarkTheme = isDarkTheme,
                onClick = onRestockOrder
            )

            // Action 3: Scan Barcode
            QuickActionOptionCard(
                title = "Scan Barcode / SKU",
                subtitle = "Scan physical item to look up, pack, or register",
                icon = Icons.Default.QrCodeScanner,
                isDarkTheme = isDarkTheme,
                onClick = onScan
            )
        }
    }
}

@Composable
private fun QuickActionOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else Color(0xFFE2E8F0)
    val cardBg = if (isDarkTheme) NawanoDark400 else Color(0xFFF8FAFC)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = NawanoPrimary200.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NawanoPrimary200,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Metric summary card matching the Behance "Supply Chain Mastery" reference mockup.
 * Displays value ("4 Items", "32 Items", etc.) with a clean label underneath.
 */
@Composable
fun SupplierMetricCard(
    value: String,
    label: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )
        }
    }
}

@Composable
fun EmptyZeroState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDarkTheme: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NawanoPrimary200.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NawanoPrimary200),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Swipe-Up Confirmation BottomSheet for deleting a purchase order.
 * Strictly complies with the user rule: Always Use Swipe-Up Modal Bottom Sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteOrderBottomSheet(
    order: PurchaseOrderEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

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
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Delete ${order.orderNumber}?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure you want to remove this purchase order? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Delete Order",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Swipe-Up Confirmation BottomSheet for deleting a catalog product.
 * Strictly complies with the user rule: Always Use Swipe-Up Modal Bottom Sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteProductBottomSheet(
    product: ProductEntity,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textSecondary = if (isDarkTheme) NawanoTextSecondaryDark else NawanoTextSecondaryLight

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
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Remove ${product.name}?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure you want to remove this item from your wholesale catalog? This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Delete Item",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
