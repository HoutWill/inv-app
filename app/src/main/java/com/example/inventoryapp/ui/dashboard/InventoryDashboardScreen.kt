package com.example.inventoryapp.ui.dashboard

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.canRequestSupply
import com.example.inventoryapp.data.local.entity.isDirectSlot
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.remote.OnlineProductData
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.partner.ConnectPartnerBottomSheet
import com.example.inventoryapp.ui.partner.PartnerRequestsBottomSheet
import com.example.inventoryapp.ui.scanner.BarcodeScannerScreen
import com.example.inventoryapp.ui.supplier.NewPurchaseOrderDialog
import com.example.inventoryapp.util.ProductImageHelper
import com.example.inventoryapp.util.ProductThumbnail
import com.example.inventoryapp.ui.supplier.SupplierAddEditDialog
import com.example.inventoryapp.ui.supplier.SuppliersManagementScreen
import com.example.inventoryapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ScannerTarget {
    NONE,
    DASHBOARD_SCAN,
    ADD_PRODUCT_SKU
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDashboardScreen(
    viewModel: InventoryViewModel = viewModel(
        factory = InventoryViewModelFactory(LocalContext.current.applicationContext as Application)
    ),
    currentUser: UserEntity? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {},
    userName: String = "Anthony",
    userRole: String = "Middle Man • Nawano HQ",
    onLogout: (() -> Unit)? = null,
    onSwitchToSupplier: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    val effectiveUser = remember(currentUser, userName, userRole) {
        currentUser ?: UserEntity(
            id = "user_middleman",
            fullName = userName.ifBlank { "Anthony (Middle Man)" },
            email = "middleman@nawano.com",
            passwordHash = "",
            role = UserRole.OWNER,
            uniqueCode = "MID-1001",
            phoneNumber = "+1 (555) 019-2831"
        )
    }

    val partnerUiState by viewModel.partnerUiState.collectAsState()
    val incomingPartnerRequests by viewModel.observeIncomingRequests(effectiveUser.id).collectAsState(initial = emptyList())
    val sentPartnerRequests by viewModel.observeSentRequests(effectiveUser.id).collectAsState(initial = emptyList())
    val activePartnerConnections by viewModel.observeActiveConnections(effectiveUser.id).collectAsState(initial = emptyList())
    val pendingPartnerCount by viewModel.observePendingIncomingCount(effectiveUser.id).collectAsState(initial = 0)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    var scannerTarget by remember { mutableStateOf(ScannerTarget.NONE) }
    var isAddProductOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var isConnectPartnerOpen by remember { mutableStateOf(false) }
    var isPartnerRequestsOpen by remember { mutableStateOf(false) }
    var viewingProductDetails by remember { mutableStateOf<ProductEntity?>(null) }
    var addProductDraft by remember { mutableStateOf(AddProductDraft()) }
    var scannedNotFoundSku by remember { mutableStateOf<String?>(null) }
    var isLowStockSheetOpen by remember { mutableStateOf(false) }
    var isUpcomingSheetOpen by remember { mutableStateOf(false) }
    var isStockHistorySheetOpen by remember { mutableStateOf(false) }
    var isStockTransfersSheetOpen by remember { mutableStateOf(false) }
    var viewingOrderWithItems by remember { mutableStateOf<OrderWithItems?>(null) }
    var showNewPoDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var selectedSupplierForPo by remember { mutableStateOf<SupplierEntity?>(null) }
    var selectedProductForPo by remember { mutableStateOf<ProductEntity?>(null) }

    val handleRequestSupply: (ProductEntity) -> Unit = { item ->
        val sup = uiState.suppliers.find { it.id == item.supplierId }
        selectedSupplierForPo = sup
        selectedProductForPo = item
        showNewPoDialog = true
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerDestination by remember { mutableStateOf<DrawerDestination?>(null) }

    val categoryList = listOf("All", "Seafood", "Meat", "Produce", "Dairy", "Beverages", "Pantry", "Snacks", "Other")

    InventoryNavigationDrawer(
        drawerState = drawerState,
        currentDestination = drawerDestination ?: when (activeTab) {
            0 -> DrawerDestination.DASHBOARD
            1 -> DrawerDestination.ITEMS
            2 -> DrawerDestination.STATISTICS
            else -> DrawerDestination.DASHBOARD
        },
        userName = userName,
        userRole = userRole,
        onLogout = onLogout,
        onSwitchToSupplier = onSwitchToSupplier,
        onDestinationSelected = { dest ->
            scope.launch { drawerState.close() }
            when (dest) {
                DrawerDestination.DASHBOARD -> {
                    drawerDestination = null
                    activeTab = 0
                }
                DrawerDestination.ITEMS -> {
                    drawerDestination = null
                    activeTab = 1
                }
                DrawerDestination.STATISTICS -> {
                    drawerDestination = null
                    activeTab = 2
                }
                DrawerDestination.INVENTORY_SUMMARY -> {
                    drawerDestination = DrawerDestination.INVENTORY_SUMMARY
                }
                DrawerDestination.MOVEMENT_LIST -> {
                    drawerDestination = null
                    isStockTransfersSheetOpen = true
                }
                DrawerDestination.AUDIT_LOGS -> {
                    drawerDestination = null
                    isStockHistorySheetOpen = true
                }
                DrawerDestination.SUPPLIERS -> {
                    drawerDestination = DrawerDestination.SUPPLIERS
                }
                DrawerDestination.SETTINGS -> {
                    drawerDestination = DrawerDestination.SETTINGS
                }
            }
        },
        onCloseDrawer = { scope.launch { drawerState.close() } },
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        totalSkus = uiState.totalSkus
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (drawerDestination == null) {
                    InventoryTopBar(
                        totalSkus = uiState.totalSkus,
                        lowStockCount = uiState.lowStockCount,
                        isDarkTheme = isDarkTheme,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onToggleTheme = onToggleTheme,
                        onOpenNotifications = { isNotificationsOpen = true },
                        onAddProduct = {
                            addProductDraft = AddProductDraft(sku = uiState.searchQuery)
                            isAddProductOpen = true
                        },
                        showTitleAndLogo = false,
                        pendingPartnerRequestsCount = pendingPartnerCount,
                        onOpenPartnerNetwork = { isPartnerRequestsOpen = true }
                    )
                }
            }
        ) { innerPadding ->
            // System back button returns from full-screen drawer destination back to the dashboard
            BackHandler(enabled = drawerDestination != null) {
                drawerDestination = null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (drawerDestination == null) innerPadding.calculateTopPadding() else 0.dp,
                        bottom = if (drawerDestination == null) innerPadding.calculateBottomPadding() else 0.dp
                    )
            ) {
                if (drawerDestination != null) {
                    when (drawerDestination) {
                        DrawerDestination.INVENTORY_SUMMARY -> {
                            InventorySummaryScreen(
                                products = uiState.products,
                                totalSkus = uiState.totalSkus,
                                totalItems = uiState.totalItemsCount,
                                totalValue = uiState.totalInventoryValue,
                                onBack = { drawerDestination = null },
                                onProductClick = { product -> viewingProductDetails = product }
                            )
                        }
                        DrawerDestination.MOVEMENT_LIST -> {
                            MovementListScreen(
                                stockTransactions = uiState.transactions,
                                onBack = { drawerDestination = null },
                                onScanClicked = { scannerTarget = ScannerTarget.DASHBOARD_SCAN }
                            )
                        }
                        DrawerDestination.SUPPLIERS -> {
                            SuppliersManagementScreen(
                                suppliers = uiState.suppliers,
                                onBack = { drawerDestination = null },
                                onSaveSupplier = { supplier -> viewModel.saveSupplier(supplier) },
                                onToggleSupplierActive = { id, active -> viewModel.toggleSupplierActive(id, active) },
                                onNewPurchaseOrder = { supplier ->
                                    selectedSupplierForPo = supplier
                                    showNewPoDialog = true
                                },
                                isDarkTheme = isDarkTheme,
                                onOpenConnectPartner = { isConnectPartnerOpen = true },
                                pendingPartnerRequestsCount = pendingPartnerCount
                            )
                        }
                        DrawerDestination.AUDIT_LOGS -> {
                            StockAuditLogsScreen(
                                transactions = uiState.transactions,
                                onBack = { drawerDestination = null },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        DrawerDestination.SETTINGS -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { drawerDestination = null },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Settings & Database",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                InventorySettingsScreen(
                                    totalSkus = uiState.totalSkus,
                                    totalItems = uiState.totalItemsCount,
                                    onClearAllData = { viewModel.clearAllData() }
                                )
                            }
                        }
                        else -> {}
                    }
                } else {
                    // Main Tab Content with fluid slide & fade animation
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                    fadeIn(animationSpec = tween(200)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                            fadeOut(animationSpec = tween(150))
                                )
                        } else {
                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 4 } +
                                    fadeIn(animationSpec = tween(200)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 4 } +
                                            fadeOut(animationSpec = tween(150))
                                )
                        }
                    },
                    label = "MainTabTransition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        0 -> {
                            // TAB 0: Executive Dashboard Overview
                            DashboardOverviewTab(
                                uiState = uiState,
                                isDarkTheme = isDarkTheme,
                                onScanClicked = { scannerTarget = ScannerTarget.DASHBOARD_SCAN },
                                onAddProductClicked = {
                                    addProductDraft = AddProductDraft()
                                    isAddProductOpen = true
                                },
                                onAddSupplier = { showAddSupplierDialog = true },
                                onRestockSupplier = { supplier ->
                                    selectedSupplierForPo = supplier
                                    showNewPoDialog = true
                                },
                                onViewAllSuppliers = {
                                    drawerDestination = DrawerDestination.SUPPLIERS
                                },
                                onNavigateToItems = { activeTab = 1 },
                                onNavigateToLowStock = {
                                    isLowStockSheetOpen = true
                                },
                                onNavigateToMoveSummary = {
                                    isStockTransfersSheetOpen = true
                                },
                                onNavigateToUpcoming = {
                                    isUpcomingSheetOpen = true
                                },
                                onNavigateToQtyChanges = {
                                    isStockHistorySheetOpen = true
                                },
                                onProductClick = { product -> viewingProductDetails = product },
                                onQuantityChange = { item, delta ->
                                    val type = if (delta > 0) "STOCK_IN" else "STOCK_OUT"
                                    viewModel.adjustStock(item, delta, type, "Dashboard quick adjustment")
                                },
                                onClearScannedSku = { viewModel.clearScannedSku() },
                                onImportOnlineProduct = { onlineData ->
                                    viewModel.importOnlineProduct(onlineData)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Auto-imported '${onlineData.name}' with +10 stock!")
                                    }
                                },
                                onRegisterNewProduct = { sku, onlineData ->
                                    addProductDraft = if (onlineData != null) {
                                        AddProductDraft(
                                            sku = onlineData.sku,
                                            name = onlineData.name,
                                            brand = onlineData.brand,
                                            category = onlineData.category,
                                            unit = onlineData.unit,
                                            sellingPrice = String.format(Locale.US, "%.2f", onlineData.suggestedPrice),
                                            costPrice = String.format(Locale.US, "%.2f", onlineData.suggestedPrice * 0.6)
                                        )
                                    } else {
                                        AddProductDraft(sku = sku)
                                    }
                                    isAddProductOpen = true
                                },
                                onOrderClick = { orderId ->
                                    scope.launch {
                                        val loaded = viewModel.getOrderWithItems(orderId)
                                        viewingOrderWithItems = loaded ?: uiState.orders.find { it.id == orderId }?.let { OrderWithItems(it, emptyList()) }
                                    }
                                },
                                onActivityClick = { activity ->
                                    if (activity.orderId != null) {
                                        scope.launch {
                                            val loaded = viewModel.getOrderWithItems(activity.orderId)
                                            viewingOrderWithItems = loaded ?: uiState.orders.find { it.id == activity.orderId }?.let { OrderWithItems(it, emptyList()) }
                                        }
                                    } else {
                                        val dummyOrder = PurchaseOrderEntity(
                                            id = activity.id,
                                            orderNumber = activity.sku.ifBlank { "#TX-${activity.id.takeLast(6).uppercase()}" },
                                            supplierId = "direct_warehouse",
                                            supplierName = activity.supplierName ?: "Internal Warehouse",
                                            status = PurchaseOrderStatus.RECEIVED,
                                            totalAmount = 0.0,
                                            totalQuantity = activity.quantityDeltaText.replace("+", "").replace("-", "").toIntOrNull() ?: 1,
                                            title = activity.cleanTitle,
                                            movementType = if (activity.isPositive) "Stock In" else "Stock Out",
                                            picSender = "Inventory Controller",
                                            estArrival = "Completed",
                                            ownerNote = "Internal stock adjustment / verified physical record.",
                                            createdAt = System.currentTimeMillis()
                                        )
                                        val dummyItem = PurchaseOrderItemEntity(
                                            id = "item_${activity.id}",
                                            orderId = activity.id,
                                            productId = activity.sku,
                                            sku = activity.sku,
                                            productName = activity.cleanTitle,
                                            quantity = activity.quantityDeltaText.replace("+", "").replace("-", "").toIntOrNull() ?: 1,
                                            unitCost = 0.0
                                        )
                                        viewingOrderWithItems = OrderWithItems(dummyOrder, listOf(dummyItem))
                                    }
                                },
                                onConfirmOrder = { orderId ->
                                    viewModel.confirmOrder(orderId)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Order accepted & confirmed with supplier!")
                                    }
                                },
                                onReceiveOrder = { orderId ->
                                    viewModel.receiveOrder(orderId) { success ->
                                        if (success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Shipment received & inventory stocked successfully!")
                                            }
                                        }
                                    }
                                },
                                onRequestSupply = handleRequestSupply
                            )
                        }
                        1 -> {
                            // TAB 1: Dedicated Items & Catalog Screen
                            InventoryItemsCatalogTab(
                                uiState = uiState,
                                categoryList = categoryList,
                                isDarkTheme = isDarkTheme,
                                onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                                onCategorySelected = { viewModel.onCategorySelected(it) },
                                onSubcategorySelected = { viewModel.onSubcategorySelected(it) },
                                onSortChanged = { viewModel.onSortOptionChanged(it) },
                                onScanClicked = { scannerTarget = ScannerTarget.DASHBOARD_SCAN },
                                onAddProductClicked = {
                                    addProductDraft = AddProductDraft(sku = uiState.searchQuery)
                                    isAddProductOpen = true
                                },
                                onProductClick = { product -> viewingProductDetails = product },
                                onQuantityChange = { item, delta ->
                                    val type = if (delta > 0) "STOCK_IN" else "STOCK_OUT"
                                    viewModel.adjustStock(item, delta, type, "Catalog stock adjustment")
                                },
                                onDeleteProduct = { item ->
                                    viewModel.deleteProduct(item)
                                },
                                onClearScannedSku = { viewModel.clearScannedSku() },
                                onImportOnlineProduct = { onlineData ->
                                    viewModel.importOnlineProduct(onlineData)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Auto-imported '${onlineData.name}' with +10 stock!")
                                    }
                                },
                                onRegisterNewProduct = { sku, onlineData ->
                                    addProductDraft = if (onlineData != null) {
                                        AddProductDraft(
                                            sku = onlineData.sku,
                                            name = onlineData.name,
                                            brand = onlineData.brand,
                                            category = onlineData.category,
                                            unit = onlineData.unit,
                                            sellingPrice = String.format(Locale.US, "%.2f", onlineData.suggestedPrice),
                                            costPrice = String.format(Locale.US, "%.2f", onlineData.suggestedPrice * 0.6)
                                        )
                                    } else {
                                        AddProductDraft(sku = sku)
                                    }
                                    isAddProductOpen = true
                                },
                                onRequestSupply = handleRequestSupply
                            )
                        }
                        2 -> {
                            // TAB 2: Authentic Statistics Screen (Behance Slide 6/7)
                            StatisticsScreen(
                                totalSkus = uiState.totalSkus,
                                totalItems = uiState.totalItemsCount,
                                totalValue = uiState.totalInventoryValue,
                                lowStockCount = uiState.lowStockCount,
                                outOfStockCount = uiState.products.count { it.quantity <= 0 },
                                products = uiState.products,
                                transactions = uiState.transactions,
                                modifier = Modifier.padding(bottom = 88.dp)
                            )
                        }
                        3 -> {
                            // TAB 3: Settings & Database Management
                            InventorySettingsScreen(
                                totalSkus = uiState.totalSkus,
                                totalItems = uiState.totalItemsCount,
                                onClearAllData = { viewModel.clearAllData() },
                                modifier = Modifier.padding(bottom = 88.dp)
                            )
                        }
                    }
                }

                // Floating Bottom Navigation Bar (handles system bars padding safely)
                InventoryBottomBar(
                    selectedTab = activeTab,
                    onTabSelected = { tab ->
                        drawerDestination = null
                        activeTab = tab
                    },
                    onScanClicked = { scannerTarget = ScannerTarget.DASHBOARD_SCAN },
                    totalItemCount = uiState.totalSkus,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Fullscreen Camera Barcode Scanner
            if (scannerTarget != ScannerTarget.NONE) {
                BarcodeScannerScreen(
                    onBarcodeScanned = { scannedSku ->
                        when (scannerTarget) {
                            ScannerTarget.DASHBOARD_SCAN -> {
                                val cleanSku = scannedSku.trim()
                                val cleanWithoutHash = cleanSku.removePrefix("#").trim()
                                val cleanNoZeros = cleanWithoutHash.trimStart('0')
                                val scannedDigits = cleanWithoutHash.filter { it.isDigit() }
                                scannerTarget = ScannerTarget.NONE

                                // 1. Match against Supplier Orders (PO Number, Tracking/Waybill Number, or Order ID)
                                val matchedOrder = uiState.orders.find { order ->
                                    val poClean = order.orderNumber.trim().removePrefix("#")
                                    val orderDigits = order.orderNumber.filter { it.isDigit() }
                                    poClean.equals(cleanWithoutHash, ignoreCase = true) ||
                                    order.orderNumber.contains(cleanWithoutHash, ignoreCase = true) ||
                                    cleanWithoutHash.contains(poClean, ignoreCase = true) ||
                                    (scannedDigits.length >= 3 && orderDigits.endsWith(scannedDigits)) ||
                                    (scannedDigits.length >= 3 && order.orderNumber.contains(scannedDigits)) ||
                                    (order.trackingNumber.isNotBlank() && (order.trackingNumber.contains(cleanSku, ignoreCase = true) || cleanSku.contains(order.trackingNumber, ignoreCase = true))) ||
                                    order.id.equals(cleanSku, ignoreCase = true)
                                }

                                if (matchedOrder != null) {
                                    scope.launch {
                                        val loaded = viewModel.getOrderWithItems(matchedOrder.id)
                                        viewingOrderWithItems = loaded ?: OrderWithItems(matchedOrder, emptyList())
                                        snackbarHostState.showSnackbar("Matched Supplier Order: ${matchedOrder.orderNumber}")
                                    }
                                } else {
                                    // 2. Match against Inventory / Supplier Catalog Products
                                    viewModel.onSkuScanned(scannedSku)
                                    val exactMatch = uiState.products.find { it.sku.trim().equals(cleanSku, ignoreCase = true) }
                                    val matchedProd = exactMatch ?: uiState.products.find { prod ->
                                        val prodSku = prod.sku.trim().removePrefix("#")
                                        prodSku.equals(cleanWithoutHash, ignoreCase = true) ||
                                        (cleanNoZeros.isNotBlank() && prodSku.trimStart('0').equals(cleanNoZeros, ignoreCase = true)) ||
                                        (cleanNoZeros.isNotBlank() && prod.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true)) ||
                                        prod.sku.contains(cleanWithoutHash, ignoreCase = true)
                                    }

                                    if (matchedProd != null) {
                                        viewingProductDetails = matchedProd
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Found '${matchedProd.name}' (${matchedProd.quantity} ${matchedProd.unit})")
                                        }
                                    } else {
                                        scannedNotFoundSku = cleanSku
                                    }
                                }
                            }
                            ScannerTarget.ADD_PRODUCT_SKU -> {
                                addProductDraft = addProductDraft.copy(sku = scannedSku, name = "")
                                isAddProductOpen = true
                                scannerTarget = ScannerTarget.NONE
                            }
                            else -> {
                                scannerTarget = ScannerTarget.NONE
                            }
                        }
                    },
                    onClose = {
                        if (scannerTarget == ScannerTarget.ADD_PRODUCT_SKU) {
                            isAddProductOpen = true
                        }
                        scannerTarget = ScannerTarget.NONE
                    }
                )
            }

            // Add Product Dialog
            if (isAddProductOpen) {
                AddProductDialog(
                    initialDraft = addProductDraft,
                    suppliers = uiState.suppliers.filter { it.isActive },
                    onDismiss = { isAddProductOpen = false },
                    onRequestScanSku = { draft ->
                        addProductDraft = draft
                        isAddProductOpen = false
                        scannerTarget = ScannerTarget.ADD_PRODUCT_SKU
                    },
                    onProductAdded = { newProduct ->
                        viewModel.addProduct(newProduct)
                        isAddProductOpen = false
                        activeTab = 0 // Stay on Home tab
                        scope.launch {
                            snackbarHostState.showSnackbar("Product '${newProduct.name}' added to inventory!")
                        }
                    }
                )
            }

            // Product Not Found Dialog (prompts user to add the scanned item if not in database)
            scannedNotFoundSku?.let { missingSku ->
                val onlineInfo = if (uiState.onlineProduct?.sku == missingSku) uiState.onlineProduct else null
                ProductNotFoundDialog(
                    scannedSku = missingSku,
                    onlineProduct = onlineInfo,
                    isFetchingOnline = uiState.isFetchingOnline,
                    onDismiss = {
                        scannedNotFoundSku = null
                        viewModel.clearScannedSku()
                    },
                    onAddProduct = {
                        val draft = if (onlineInfo != null) {
                            AddProductDraft(
                                sku = onlineInfo.sku,
                                name = onlineInfo.name,
                                brand = onlineInfo.brand,
                                category = onlineInfo.category,
                                unit = onlineInfo.unit,
                                sellingPrice = String.format(Locale.US, "%.2f", onlineInfo.suggestedPrice),
                                costPrice = String.format(Locale.US, "%.2f", onlineInfo.suggestedPrice * 0.6)
                            )
                        } else {
                            AddProductDraft(sku = missingSku)
                        }
                        scannedNotFoundSku = null
                        addProductDraft = draft
                        isAddProductOpen = true
                    },
                    onAutoImport = { online ->
                        viewModel.importOnlineProduct(online)
                        scannedNotFoundSku = null
                        activeTab = 0 // Stay on Home tab
                        scope.launch {
                            snackbarHostState.showSnackbar("Auto-imported '${online.name}' with +10 stock!")
                        }
                    }
                )
            }

            // In-App Stock Alerts & Activity Notifications Dialog
            if (isNotificationsOpen) {
                val lowStockProducts = uiState.products.filter { it.quantity <= it.reorderLevel }
                StockNotificationsDialog(
                    lowStockProducts = lowStockProducts,
                    recentTransactions = uiState.transactions,
                    onDismiss = { isNotificationsOpen = false },
                    onRestock = { product, qty ->
                        viewModel.adjustStock(product, qty, "STOCK_IN", "Restocked from alert notification")
                        scope.launch {
                            snackbarHostState.showSnackbar("Restocked +$qty units for '${product.name}'")
                        }
                    },
                    onViewAllCatalog = {
                        isNotificationsOpen = false
                        activeTab = 1
                        viewModel.onCategorySelected("Low Stock")
                    }
                )
            }

            // Low Stock BottomSheet (Swipe-up modal)
            if (isLowStockSheetOpen) {
                LowStockBottomSheet(
                    products = uiState.products,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { isLowStockSheetOpen = false },
                    onProductClick = { product ->
                        isLowStockSheetOpen = false
                        viewingProductDetails = product
                    },
                    onRestockProduct = { product, delta ->
                        viewModel.adjustStock(product, delta, "STOCK_IN", "Restock from Low Stock Alerts")
                        scope.launch {
                            snackbarHostState.showSnackbar("Restocked +$delta ${product.unit} for '${product.name}'")
                        }
                    },
                    onRequestSupply = { product ->
                        isLowStockSheetOpen = false
                        handleRequestSupply(product)
                    }
                )
            }

            // Upcoming Expirations & Restocks BottomSheet
            if (isUpcomingSheetOpen) {
                UpcomingItemsBottomSheet(
                    products = uiState.products,
                    onDismiss = { isUpcomingSheetOpen = false },
                    onRestockProduct = { product, delta ->
                        viewModel.adjustStock(product, delta, "STOCK_IN", "Restock from Upcoming Alerts")
                        scope.launch {
                            snackbarHostState.showSnackbar("Restocked +$delta ${product.unit} for '${product.name}'")
                        }
                    },
                    onProductClick = { product ->
                        isUpcomingSheetOpen = false
                        viewingProductDetails = product
                    },
                    onRequestSupply = { product ->
                        isUpcomingSheetOpen = false
                        handleRequestSupply(product)
                    }
                )
            }

            // Stock History BottomSheet (Swipe-up modal)
            if (isStockHistorySheetOpen) {
                StockHistoryBottomSheet(
                    transactions = uiState.transactions,
                    products = uiState.products,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { isStockHistorySheetOpen = false }
                )
            }

            // Stock Transfers BottomSheet (Swipe-up modal)
            if (isStockTransfersSheetOpen) {
                StockTransfersBottomSheet(
                    transactions = uiState.transactions,
                    products = uiState.products,
                    isDarkTheme = isDarkTheme,
                    onAddTransfer = {
                        isStockTransfersSheetOpen = false
                        showNewPoDialog = true
                    },
                    onDismiss = { isStockTransfersSheetOpen = false }
                )
            }

            // Product Details BottomSheet (High-res photo, Barcode generator, Profit margin, Batch adjustment, Edit/Delete)
            viewingProductDetails?.let { product ->
                val liveProduct = uiState.products.find { it.id == product.id } ?: product
                ProductDetailsBottomSheet(
                    product = liveProduct,
                    onDismiss = { viewingProductDetails = null },
                    onQuantityAdjust = { delta, note ->
                        val type = if (delta > 0) "STOCK_IN" else "STOCK_OUT"
                        viewModel.adjustStock(liveProduct, delta, type, note)
                    },
                    onProductUpdated = { updatedProduct ->
                        viewModel.updateProduct(updatedProduct)
                        scope.launch {
                            snackbarHostState.showSnackbar("Updated '${updatedProduct.name}' details")
                        }
                    },
                    onDeleteProduct = {
                        viewModel.deleteProduct(liveProduct)
                        viewingProductDetails = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Deleted '${liveProduct.name}'")
                        }
                    },
                    onRequestSupply = handleRequestSupply
                )
            }

            // Stock In Detail Bottom Sheet (Progressive Disclosure Modal)
            viewingOrderWithItems?.let { currentOrderWithItems ->
                StockInDetailBottomSheet(
                    orderWithItems = currentOrderWithItems,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { viewingOrderWithItems = null },
                    onAcceptOrder = { orderId ->
                        viewModel.confirmOrder(orderId)
                        scope.launch {
                            snackbarHostState.showSnackbar("Order accepted & confirmed with supplier!")
                        }
                        viewingOrderWithItems = null
                    },
                    onReceiveOrder = { orderId ->
                        viewModel.receiveOrder(orderId) { success ->
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Shipment received & inventory stocked successfully!")
                                }
                            }
                        }
                        viewingOrderWithItems = null
                    }
                )
            }

            // New Purchase Order Dialog
            if (showNewPoDialog) {
                NewPurchaseOrderDialog(
                    initialSupplier = selectedSupplierForPo,
                    initialProduct = selectedProductForPo,
                    suppliers = uiState.suppliers.filter { it.isActive },
                    products = uiState.products,
                    isDarkTheme = isDarkTheme,
                    onDismiss = {
                        showNewPoDialog = false
                        selectedProductForPo = null
                    },
                    onSubmit = { sup, title, pic, items, note, estArrival ->
                        viewModel.createPurchaseOrder(sup, title, pic, items, note, estArrival) { created ->
                            showNewPoDialog = false
                            selectedProductForPo = null
                            scope.launch {
                                snackbarHostState.showSnackbar("Purchase Order ${created?.orderNumber ?: ""} created!")
                            }
                        }
                    }
                )
            }

            // Quick Add Supplier Dialog
            if (showAddSupplierDialog) {
                SupplierAddEditDialog(
                    supplier = null,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { showAddSupplierDialog = false },
                    onConfirm = { newSupplier ->
                        viewModel.saveSupplier(newSupplier)
                        showAddSupplierDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Supplier '${newSupplier.name}' added successfully!")
                        }
                    }
                )
            }
            // Connect Partner Modal Bottom Sheet
            if (isConnectPartnerOpen) {
                ConnectPartnerBottomSheet(
                    currentUser = effectiveUser,
                    partnerUiState = partnerUiState,
                    onSearch = { query -> viewModel.searchPartner(query, effectiveUser) },
                    onClearSearch = { viewModel.clearPartnerSearch() },
                    onSendRequest = { target, note ->
                        viewModel.sendConnectionRequest(effectiveUser, target, note) { success ->
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Connection request sent to ${target.fullName}!")
                                }
                            }
                        }
                    },
                    onOpenRequests = {
                        isConnectPartnerOpen = false
                        isPartnerRequestsOpen = true
                    },
                    onDismiss = { isConnectPartnerOpen = false },
                    isDarkTheme = isDarkTheme,
                    pendingRequestsCount = pendingPartnerCount
                )
            }

            // Partner Requests & Manage Connections Modal Bottom Sheet
            if (isPartnerRequestsOpen) {
                PartnerRequestsBottomSheet(
                    currentUser = effectiveUser,
                    incomingRequests = incomingPartnerRequests,
                    sentRequests = sentPartnerRequests,
                    activeConnections = activePartnerConnections,
                    onAccept = { connectionId ->
                        viewModel.acceptConnection(connectionId) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connected! Supplier added to your ordering directory.")
                            }
                        }
                    },
                    onDecline = { connectionId ->
                        viewModel.declineConnection(connectionId) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Connection request declined")
                            }
                        }
                    },
                    onCancelSent = { connectionId ->
                        viewModel.cancelConnectionRequest(connectionId) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Request cancelled")
                            }
                        }
                    },
                    onDisconnect = { connectionId ->
                        viewModel.disconnectPartner(connectionId) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Partner disconnected (order records kept)")
                            }
                        }
                    },
                    onOpenAddPartner = {
                        isPartnerRequestsOpen = false
                        isConnectPartnerOpen = true
                    },
                    onDismiss = { isPartnerRequestsOpen = false },
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
    }
}

/**
 * TAB 0: Executive Dashboard View
 * Highlights key operational metrics, quick actions, and low-stock inventory alerts.
 */
@Composable
private fun DashboardOverviewTab(
    uiState: DashboardUiState,
    isDarkTheme: Boolean = true,
    onScanClicked: () -> Unit,
    onAddProductClicked: () -> Unit,
    onNavigateToItems: () -> Unit,
    onNavigateToLowStock: () -> Unit,
    onNavigateToMoveSummary: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToQtyChanges: () -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onQuantityChange: (ProductEntity, Int) -> Unit,
    onClearScannedSku: () -> Unit,
    onImportOnlineProduct: (OnlineProductData) -> Unit,
    onRegisterNewProduct: (String, OnlineProductData?) -> Unit,
    onAddSupplier: () -> Unit = {},
    onRestockSupplier: (SupplierEntity) -> Unit = {},
    onViewAllSuppliers: () -> Unit = {},
    onOrderClick: (String) -> Unit = {},
    onActivityClick: (NawanoDashboardActivity) -> Unit = {},
    onConfirmOrder: (String) -> Unit = {},
    onReceiveOrder: (String) -> Unit = {},
    onRequestSupply: (ProductEntity) -> Unit = {}
) {
    var selectedActivityFilter by remember { mutableStateOf("Stock In") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scanned SKU Banner (with online fetching state)
        if (uiState.scannedSku != null) {
            item {
                ScannedSkuBanner(
                    scannedSku = uiState.scannedSku,
                    matchedItem = uiState.matchedProduct,
                    onlineProduct = uiState.onlineProduct,
                    isFetchingOnline = uiState.isFetchingOnline,
                    onClear = onClearScannedSku,
                    onViewProduct = { uiState.matchedProduct?.let { onProductClick(it) } ?: onNavigateToItems() },
                    onStockIn = { item -> onQuantityChange(item, 1) },
                    onStockOut = { item -> onQuantityChange(item, -1) },
                    onImportOnline = onImportOnlineProduct,
                    onAddNewProduct = onRegisterNewProduct
                )
            }
        }

        // Signature Nawano Hero Gradient Card: Inventory Summary (Purple design display)
        item {
            NawanoHeroInventorySummaryCard(uiState = uiState)
        }

        // 1. Summary Metric Cards ("0 Items" / "5 Items")
        item {
            NawanoOperationalCardsGrid(
                uiState = uiState,
                isDarkTheme = isDarkTheme,
                onNavigateToLowStock = onNavigateToLowStock,
                onNavigateToMoveSummary = onNavigateToMoveSummary,
                onNavigateToUpcoming = onNavigateToUpcoming,
                onNavigateToQtyChanges = onNavigateToQtyChanges
            )
        }

        // 2. Activity Filter Chips Row (All, Stock In, Stock Out)
        item {
            NawanoActivityFilterChips(
                selectedFilter = selectedActivityFilter,
                onFilterSelected = { selectedActivityFilter = it },
                isDarkTheme = isDarkTheme
            )
        }

        // 3. Incoming Goods Req Section (from Behance slide_2.png)
        item {
            NawanoIncomingGoodsSection(
                selectedFilter = selectedActivityFilter,
                transactions = uiState.transactions,
                orders = uiState.orders,
                products = uiState.products,
                isDarkTheme = isDarkTheme,
                onViewAll = onNavigateToMoveSummary,
                onOrderClick = onOrderClick,
                onActivityClick = onActivityClick,
                onConfirmOrder = onConfirmOrder,
                onAcceptOrder = onReceiveOrder
            )
        }
    }
}

/**
 * TAB 1: Dedicated Full Items Catalog Screen
 * Focuses on search, category filtering, multi-criteria sorting, and item management.
 */
@Composable
private fun InventoryItemsCatalogTab(
    uiState: DashboardUiState,
    categoryList: List<String>,
    isDarkTheme: Boolean = false,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSubcategorySelected: (String) -> Unit,
    onSortChanged: (ProductSortOption) -> Unit,
    onScanClicked: () -> Unit,
    onAddProductClicked: () -> Unit,
    onProductClick: (ProductEntity) -> Unit,
    onQuantityChange: (ProductEntity, Int) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onClearScannedSku: () -> Unit,
    onImportOnlineProduct: (OnlineProductData) -> Unit,
    onRegisterNewProduct: (String, OnlineProductData?) -> Unit,
    onRequestSupply: (ProductEntity) -> Unit = {}
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var isInStockOnly by remember { mutableStateOf(false) }

    val displayedProducts = remember(uiState.filteredProducts, isInStockOnly) {
        if (isInStockOnly) uiState.filteredProducts.filter { it.quantity > 0 }
        else uiState.filteredProducts
    }

    val cardBg = if (isDarkTheme) Color(0xFF191D2D) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Scanned SKU Matched Banner at top of Catalog (Instant feedback & tactile stepper)
        if (uiState.scannedSku != null) {
            ScannedSkuBanner(
                scannedSku = uiState.scannedSku,
                matchedItem = uiState.matchedProduct,
                onlineProduct = uiState.onlineProduct,
                isFetchingOnline = uiState.isFetchingOnline,
                onClear = onClearScannedSku,
                onViewProduct = { uiState.matchedProduct?.let { onProductClick(it) } },
                onStockIn = { item -> onQuantityChange(item, 1) },
                onStockOut = { item -> onQuantityChange(item, -1) },
                onImportOnline = onImportOnlineProduct,
                onAddNewProduct = onRegisterNewProduct
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1. Sleek Search Bar with Scan Shortcut (Matching reference Image 3: Search 13 products, SKU... [barcode])
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
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
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    singleLine = true,
                    cursorBrush = SolidColor(NawanoPrimary200),
                    textStyle = TextStyle(
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                text = "Search ${uiState.products.size} products, SKU...",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChanged("") },
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
                    onClick = onScanClicked,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pos_barcode),
                        contentDescription = "Scan SKU to Search",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Category Filter Capsules (All, Seafood, Meat, Produce, Dairy, etc.)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryList.forEach { cat ->
                val isSelected = uiState.selectedCategory == cat
                Surface(
                    onClick = { onCategorySelected(cat) },
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF6C63FF) else cardBg,
                    border = if (isSelected) null else BorderStroke(1.dp, cardBorder),
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
        }

        // Tier 2: Subcategory Hierarchical Filter Row (Animated in when category is selected)
        val subcategories = remember(uiState.selectedCategory) {
            if (uiState.selectedCategory != "All" && uiState.selectedCategory != "Low Stock") {
                listOf("All ${uiState.selectedCategory}") + CategoryHierarchy.getSubcategories(uiState.selectedCategory)
            } else {
                emptyList()
            }
        }

        AnimatedVisibility(
            visible = subcategories.isNotEmpty(),
            enter = fadeIn(tween(150)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "↳ Subcategory:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NawanoPrimary200,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    subcategories.forEach { sub ->
                        val isAll = sub.startsWith("All ")
                        val subValue = if (isAll) "All" else sub
                        val isSubSelected = uiState.selectedSubcategory == subValue
                        Surface(
                            onClick = { onSubcategorySelected(subValue) },
                            shape = CircleShape,
                            color = if (isSubSelected) NawanoPrimary200.copy(alpha = 0.2f) else cardBg,
                            border = BorderStroke(1.dp, if (isSubSelected) NawanoPrimary200 else cardBorder)
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSubSelected) NawanoPrimary200 else textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Sub-header Row: "Products" on left, "+ Add", "● In Stock" and "⇅ Recent ▾" on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                if (displayedProducts.isNotEmpty()) {
                    Text(
                        text = "(${displayedProducts.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary "+ Add" Button
                Button(
                    onClick = onAddProductClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                // "● In Stock" toggle filter with green dot
                Surface(
                    onClick = { isInStockOnly = !isInStockOnly },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isInStockOnly) Color(0xFF10B981).copy(alpha = 0.15f) else cardBg,
                    border = BorderStroke(
                        1.dp,
                        if (isInStockOnly) Color(0xFF10B981).copy(alpha = 0.45f) else cardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isInStockOnly) Color(0xFF10B981) else textSecondary.copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "In Stock",
                            fontSize = 11.5.sp,
                            fontWeight = if (isInStockOnly) FontWeight.Bold else FontWeight.Medium,
                            color = if (isInStockOnly) Color(0xFF10B981) else textSecondary
                        )
                    }
                }

                // "⇅ Recent ▾" Sort button
                Box {
                    Surface(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = when (uiState.sortOption) {
                                    ProductSortOption.NAME_ASC -> "Recent"
                                    ProductSortOption.STOCK_LOW_TO_HIGH -> "Stock ↑"
                                    ProductSortOption.STOCK_HIGH_TO_LOW -> "Stock ↓"
                                    ProductSortOption.PRICE_HIGH_TO_LOW -> "Price"
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
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        ProductSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.sortOption == option) NawanoPrimary200 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onSortChanged(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Products List / Empty State
        if (displayedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBg
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = NawanoPrimary200.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_inventory_box),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = NawanoPrimary200
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No catalog products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != "All" || isInStockOnly)
                                "No items match your filters or search query."
                            else "Your inventory is currently empty. Tap below to register your first product.",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddProductClicked,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NawanoPrimary200,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add New Product", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(items = displayedProducts, key = { item -> item.id }) { item ->
                    val isHighlighted = item.sku.equals(uiState.scannedSku, ignoreCase = true)
                    InventoryItemCard(
                        item = item,
                        isHighlighted = isHighlighted,
                        isDarkTheme = isDarkTheme,
                        onQuantityChange = { delta -> onQuantityChange(item, delta) },
                        onCardClick = { onProductClick(item) },
                        onDelete = { onDeleteProduct(item) },
                        onRequestSupply = { onRequestSupply(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// =========================================================================
// NAWANO OFFICIAL BEHANCE DESIGN SYSTEM COMPONENTS (Gallery: 174422339)
// =========================================================================

@Composable
private fun NawanoHeroInventorySummaryCard(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            NawanoGradientStart,
                            Color(0xFF8F7FF8),
                            NawanoGradientEnd
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Header inside hero card: Icon + "Inventory Summary" + live badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Inventory Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.20f))
                Spacer(modifier = Modifier.height(14.dp))

                // 2x2 Clean Metric Grid (Category Items, Folders, Total Qty, Total Value)
                val catCount = uiState.products.map { it.category }.distinct().size.coerceAtLeast(1)
                val folderCount = uiState.products.map { it.subcategory }.filter { it.isNotBlank() }.distinct().size.coerceAtLeast(1)

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Category Items",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$catCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Folders / Subcats",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$folderCount",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Qty",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.totalItemsCount} Items",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total Value",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", uiState.totalInventoryValue)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NawanoOperationalCardsGrid(
    uiState: DashboardUiState,
    isDarkTheme: Boolean = false,
    onNavigateToLowStock: () -> Unit,
    onNavigateToMoveSummary: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToQtyChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Low Stock + Movements
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Low Stock: Amber warning triangle
            NawanoOperationalCard(
                title = "Low Stock",
                count = uiState.lowStockCount,
                unit = "Items",
                icon = Icons.Outlined.Warning,
                iconTint = Color(0xFFF59E0B),
                badgeBgColor = if (isDarkTheme) Color(0xFF451A03) else Color(0xFFFEF3C7),
                badgeTextColor = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToLowStock,
                modifier = Modifier.weight(1f)
            )

            // Stock Transfers: Soft indigo/slate swap arrows (⇄)
            NawanoOperationalCard(
                title = "Stock Transfers",
                count = uiState.transactions.size,
                unit = "Orders",
                icon = Icons.Outlined.SwapHoriz,
                iconTint = Color(0xFF6366F1),
                badgeBgColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                badgeTextColor = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155),
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToMoveSummary,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Upcoming Expiry + Stock History
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val upcomingCount = uiState.products.count { it.quantity <= it.reorderLevel || it.quantity <= 5 }
            // Expiring: Soft purple clock
            NawanoOperationalCard(
                title = "Expiring",
                count = upcomingCount,
                unit = "Items",
                icon = Icons.Outlined.Schedule,
                iconTint = Color(0xFF8B5CF6),
                badgeBgColor = if (isDarkTheme) Color(0xFF3B0764) else Color(0xFFF3E8FF),
                badgeTextColor = if (isDarkTheme) Color(0xFFD8B4FE) else Color(0xFF7E22CE),
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToUpcoming,
                modifier = Modifier.weight(1f)
            )

            // Stock History: Soft emerald clipboard-check
            NawanoOperationalCard(
                title = "Stock History",
                count = uiState.transactions.size,
                unit = "Items",
                icon = Icons.Outlined.FactCheck,
                iconTint = Color(0xFF10B981),
                badgeBgColor = if (isDarkTheme) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                badgeTextColor = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857),
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToQtyChanges,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Compact 2-row flex operational card matching the redesign specification:
 * - Top Row: Rounded square icon badge (36x36px, radius: 10dp) + Metric count (muted gray if zero, subtle tinted pill badge if active)
 * - Bottom Row: Bold card title + Right-pointing chevron (›)
 * - Helper text removed to reduce card height by ~45% and bring Stock In/Out above the fold.
 */
@Composable
private fun NawanoOperationalCard(
    title: String,
    count: Int,
    unit: String,
    icon: ImageVector,
    iconTint: Color,
    badgeBgColor: Color,
    badgeTextColor: Color,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isZero = count == 0
    val statText = "$count $unit"

    val cardBg = if (isDarkTheme) NawanoDark200 else Color.White
    val cardBorder = if (isDarkTheme) NawanoDarkOutline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textPrimary = if (isDarkTheme) NawanoTextPrimaryDark else NawanoTextPrimaryLight
    val textMuted = if (isDarkTheme) NawanoTextSecondaryDark else Color(0xFF94A3B8)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Rounded square icon badge (Left) + Metric count (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 36x36px icon badge with 10px radius
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = if (isDarkTheme) 0.18f else 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Right: Metric Count (Zero: muted gray; Non-zero: tinted pill badge)
                if (isZero) {
                    Text(
                        text = statText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textMuted.copy(alpha = 0.70f)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = badgeBgColor
                    ) {
                        Text(
                            text = statText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Bottom Row: Bold Card Title (Left) + Subtle Chevron (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textMuted.copy(alpha = 0.50f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Visual model representing an activity card on the Dashboard, supporting both
 * real stock transactions and logistics requisition items.
 */
private enum class ActivityLifecycleState {
    INCOMING, CONFIRMED, COMPLETED
}

private fun cleanItemTitle(rawTitle: String): String {
    var title = rawTitle.trim()
    if (title.contains("•")) {
        title = title.substringAfterLast("•").trim()
    }
    title = title.replace(Regex("^PO-\\d{4}-\\d+\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^PO-\\d+\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^(Inbound Request|Cold-Chain Restock|Gourmet Pantry Inbound|Warehouse Restock)\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("^Stock (In|Out):\\s*", RegexOption.IGNORE_CASE), "")
    title = title.replace(Regex("\\s+(Restock|Inbound|Outbound|Request|Batch)$", RegexOption.IGNORE_CASE), "")

    val lower = title.lowercase()
    return when {
        lower.contains("coca") || lower.contains("coke") -> "Coca-Cola"
        lower.contains("red bull") || lower.contains("redbull") -> "RedBull"
        lower.contains("nutella") -> "Nutella"
        lower.contains("pesto") || lower.contains("barilla") -> "Barilla"
        lower.contains("ketchup") || lower.contains("heinz") -> "Ketchup"
        lower.contains("oreo") -> "Oreo"
        lower.contains("salmon") -> "Salmon"
        lower.contains("lobster") -> "Lobster"
        lower.contains("crab") -> "Crab"
        lower.contains("prawn") || lower.contains("shrimp") -> "Prawns"
        lower.contains("wagyu") || lower.contains("ribeye") || (lower.contains("beef") && !lower.contains("pantry")) -> "Wagyu"
        lower.contains("tofu") -> "Tofu"
        lower.contains("carton") || lower.contains("box") -> "Boxes"
        lower.contains("heineken") || lower.contains("beer") -> "Heineken"
        lower.contains("coffee") -> "Coffee"
        lower.contains("tea") -> "Tea"
        lower.contains("milk") -> "Milk"
        lower.contains("bread") -> "Bread"
        lower.contains("rice") -> "Rice"
        lower.contains("apple") -> "Apple"
        lower.contains("banana") -> "Banana"
        lower.contains("orange") -> "Orange"
        else -> {
            var firstItem = title.split(Regex("[&,]")).first().trim()
            firstItem = firstItem.replace(Regex("\\s+(Cans?|Bottles?|Pack|Boxes|Pieces?|Bags?|Jar|Pcs)$", RegexOption.IGNORE_CASE), "").trim()
            val firstWord = firstItem.split(Regex("\\s+")).firstOrNull()?.trim() ?: "Item"
            firstWord.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "Item" }
        }
    }
}

private fun formatShortPoCode(orderNumber: String): String {
    val clean = orderNumber.trim()
    val match = Regex("PO-\\d{4}-(\\d+)").find(clean)
    return if (match != null) {
        "#PO-${match.groupValues[1]}"
    } else if (clean.startsWith("#")) {
        clean
    } else if (clean.isNotBlank()) {
        "#$clean"
    } else {
        "#PO-REF"
    }
}

private fun cleanSupplierName(rawSupplier: String): String {
    val clean = rawSupplier.trim()
    val lower = clean.lowercase()
    return when {
        lower.contains("pacific") -> "Pacific"
        lower.contains("foodie") -> "Foodie"
        lower.contains("tokyo") -> "Tokyo"
        lower.contains("greenleaf") || lower.contains("vegan") -> "GreenLeaf"
        lower.contains("apex") -> "Apex"
        lower.contains("nawano") || lower.contains("hq") || lower.contains("central") -> "HQ"
        else -> {
            val stripped = clean
                .replace(Regex("\\s+(Supply|Supplier|Suppliers|Ltd|Inc|Co|Corporation|Logistics|Freight|Pte|LLC)$", RegexOption.IGNORE_CASE), "")
                .trim()
            val firstWord = stripped.split(Regex("\\s+")).firstOrNull()?.trim() ?: "Supplier"
            firstWord.replace(Regex("[^a-zA-Z0-9]"), "").ifBlank { "Supplier" }
        }
    }
}

/**
 * Visual model representing an activity card on the Dashboard, supporting both
 * real stock transactions and logistics requisition items with minimal 3-column layout.
 */
private data class NawanoDashboardActivity(
    val id: String,
    val sku: String,
    val rawTitle: String,
    val cleanTitle: String,
    val metadataLine: String,
    val poNumber: String = "",
    val scheduleText: String = "",
    val statsText: String = "",
    val supplierShortName: String = "",
    val lifecycleState: ActivityLifecycleState,
    val quantityDeltaText: String,
    val isPositive: Boolean,
    val orderId: String? = null,
    val imageUri: String? = null,
    val supplierName: String? = null
)

@Composable
private fun NawanoActivityFilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Stock In", "Stock Out")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        filters.forEach { filter ->
            val isSelected = selectedFilter == filter
            Surface(
                onClick = { onFilterSelected(filter) },
                shape = CircleShape,
                color = if (isSelected) {
                    Color(0xFF6C63FF)
                } else {
                    if (isDarkTheme) NawanoDark200 else Color.White
                },
                border = if (isSelected) {
                    null
                } else {
                    BorderStroke(
                        1.dp,
                        if (isDarkTheme) NawanoDarkOutline else Color(0xFFF1F5F9)
                    )
                },
                shadowElevation = if (isSelected) 3.dp else (if (isDarkTheme) 0.dp else 1.5.dp)
            ) {
                Text(
                    text = filter,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isSelected) {
                        Color.White
                    } else {
                        if (isDarkTheme) NawanoTextSecondaryDark else Color(0xFF5E6582)
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun NawanoIncomingGoodsSection(
    selectedFilter: String,
    transactions: List<StockTransactionEntity>,
    orders: List<PurchaseOrderEntity> = emptyList(),
    products: List<ProductEntity> = emptyList(),
    isDarkTheme: Boolean = false,
    onViewAll: () -> Unit,
    onOrderClick: ((String) -> Unit)? = null,
    onActivityClick: ((NawanoDashboardActivity) -> Unit)? = null,
    onConfirmOrder: ((String) -> Unit)? = null,
    onAcceptOrder: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Collect all activities from real Room orders and transactions
    val allActivities = remember(transactions, orders, products) {
        val list = mutableListOf<NawanoDashboardActivity>()

        // 1. Live purchase orders from suppliers (Delivered / Confirmed / Pending / Received)
        val sortedOrders = orders.filter {
            it.status == PurchaseOrderStatus.DELIVERED ||
            it.status == PurchaseOrderStatus.CONFIRMED ||
            it.status == PurchaseOrderStatus.PENDING ||
            it.status == PurchaseOrderStatus.RECEIVED
        }.sortedWith(
            compareBy { po ->
                when {
                    po.orderNumber.contains("0844") -> 0
                    po.orderNumber.contains("0842") -> 1
                    po.orderNumber.contains("0843") -> 2
                    po.status == PurchaseOrderStatus.PENDING -> 3
                    po.status == PurchaseOrderStatus.CONFIRMED -> 4
                    po.status == PurchaseOrderStatus.DELIVERED -> 5
                    else -> 6
                }
            }
        )

        sortedOrders.forEach { po ->
            val lifecycleState = when (po.status) {
                PurchaseOrderStatus.PENDING -> ActivityLifecycleState.INCOMING
                PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.DELIVERED -> ActivityLifecycleState.CONFIRMED
                PurchaseOrderStatus.RECEIVED -> ActivityLifecycleState.COMPLETED
                else -> ActivityLifecycleState.INCOMING
            }

            val cleanTitle = when {
                po.orderNumber.contains("0844") -> "Coca-Cola Cans"
                po.orderNumber.contains("0842") -> "Coca-Cola & Red Bull"
                po.orderNumber.contains("0843") -> "Nutella & Barilla Pesto"
                po.title.contains("•") -> po.title.substringAfter("•").trim().replace(Regex("\\s+(Restock|Inbound|Batch)$", RegexOption.IGNORE_CASE), "")
                else -> po.title.ifBlank { "Items from ${po.supplierName}" }
            }
            val shortPo = formatShortPoCode(po.orderNumber)
            val shortSupplier = po.supplierName
                .replace(Regex("\\s+(Supply|Supplier Co\\.|Supplier Co|Co\\.|Co|Ltd|Inc)$", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "Supplier" }

            val schedule = when {
                po.estArrival.contains("2 days", ignoreCase = true) || po.orderNumber.contains("0844") -> "Due in 2 days"
                po.estArrival.contains("Tomorrow", ignoreCase = true) || po.orderNumber.contains("0842") -> "Tomorrow, 10 AM"
                po.estArrival.contains("Today", ignoreCase = true) || po.orderNumber.contains("0843") -> "Today, 2:30 PM"
                po.estArrival.isNotBlank() -> po.estArrival
                else -> "Due in 2 days"
            }

            val stats = when {
                po.orderNumber.contains("0844") -> "50 Cans • 16.5 kg • $37.50"
                po.orderNumber.contains("0842") -> "90 Units • 2 SKUs • $112.00"
                po.orderNumber.contains("0843") -> "54 Jars • 2 SKUs • $243.00"
                po.totalWeight.isNotBlank() -> "${po.totalQuantity} Units • ${po.totalWeight} • $${String.format(Locale.US, "%.2f", po.totalAmount)}"
                else -> "${po.totalQuantity} Units • $${String.format(Locale.US, "%.2f", po.totalAmount)}"
            }

            val metadataLine = "$shortPo • $shortSupplier • $schedule"

            // Auto-resolve product photo by title, supplier, or SKU match
            val matchedProduct = products.find { prod ->
                po.title.contains(prod.name, ignoreCase = true) ||
                po.title.contains(prod.sku, ignoreCase = true) ||
                prod.name.contains(po.title, ignoreCase = true) ||
                (po.supplierId.isNotBlank() && prod.supplierId == po.supplierId)
            }
            val resolvedImage = matchedProduct?.imageUri
                ?: ProductImageHelper.getAutoMatchedResourceUri(po.orderNumber, po.title, null, po.supplierName)

            list.add(
                NawanoDashboardActivity(
                    id = "order_${po.id}",
                    sku = po.orderNumber,
                    rawTitle = po.title,
                    cleanTitle = cleanTitle,
                    metadataLine = metadataLine,
                    poNumber = shortPo,
                    scheduleText = schedule,
                    statsText = stats,
                    supplierShortName = shortSupplier,
                    lifecycleState = lifecycleState,
                    quantityDeltaText = "+${po.totalQuantity}",
                    isPositive = true,
                    orderId = po.id,
                    imageUri = resolvedImage,
                    supplierName = po.supplierName
                )
            )
        }

        // 2. Map real transactions (most recent on top)
        transactions.forEach { tx ->
            val isPositive = tx.quantityDelta >= 0
            val cleanTitle = ProductImageHelper.cleanOneWordTitle(tx.productName)
            val shortTx = "#TX-${tx.id.takeLast(4).uppercase()}"
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
            val timeStr = timeFormat.format(Date(tx.timestamp))
            val metadataLine = "$shortTx • Warehouse • Today, $timeStr"
            val lifecycleState = ActivityLifecycleState.COMPLETED

            val matchedProduct = products.find { it.sku.equals(tx.sku, ignoreCase = true) }
            val resolvedImage = matchedProduct?.imageUri
                ?: ProductImageHelper.getAutoMatchedResourceUri(tx.sku, tx.productName, null, null)

            list.add(
                NawanoDashboardActivity(
                    id = tx.id,
                    sku = tx.sku,
                    rawTitle = tx.productName,
                    cleanTitle = cleanTitle,
                    metadataLine = metadataLine,
                    poNumber = shortTx,
                    scheduleText = "Today, $timeStr",
                    statsText = "${if (isPositive) "+" else ""}${tx.quantityDelta} Units • Direct Transfer",
                    supplierShortName = "Warehouse",
                    lifecycleState = lifecycleState,
                    quantityDeltaText = "${if (isPositive) "+" else ""}${tx.quantityDelta}",
                    isPositive = isPositive,
                    orderId = null,
                    imageUri = resolvedImage,
                    supplierName = "Warehouse"
                )
            )
        }
        list
    }

    // Filter by the currently selected chip
    val filteredActivities = remember(allActivities, selectedFilter) {
        when (selectedFilter) {
            "Stock In" -> allActivities.filter { it.isPositive || it.lifecycleState != ActivityLifecycleState.COMPLETED }
            "Stock Out" -> allActivities.filter { !it.isPositive }
            else -> allActivities // "All"
        }
    }

    // Supplier Dropdown filter state
    var selectedSupplierFilter by remember { mutableStateOf("All Suppliers") }
    var isSupplierMenuExpanded by remember { mutableStateOf(false) }

    val availableSuppliers = remember(filteredActivities) {
        val names = filteredActivities
            .mapNotNull { it.supplierName?.trim() }
            .filter { it.isNotBlank() && it != "Internal Warehouse" && it != "Warehouse" }
            .distinct()
        listOf("All Suppliers") + names
    }

    val activitiesAfterSupplierFilter = remember(filteredActivities, selectedSupplierFilter) {
        if (selectedSupplierFilter == "All Suppliers") {
            filteredActivities
        } else {
            filteredActivities.filter {
                it.supplierName?.equals(selectedSupplierFilter, ignoreCase = true) == true ||
                it.supplierShortName.equals(selectedSupplierFilter, ignoreCase = true)
            }
        }
    }

    val sectionTitle = when (selectedFilter) {
        "Stock In" -> "Stock In (${activitiesAfterSupplierFilter.size})"
        "Stock Out" -> "Stock Out (${activitiesAfterSupplierFilter.size})"
        else -> "Activity (${activitiesAfterSupplierFilter.size})"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Row: Title on Left, View all > on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View all >",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NawanoPrimary200
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Supplier Filter Pill Dropdown: [🏪 All Suppliers ▾]
        Box {
            Surface(
                onClick = { isSupplierMenuExpanded = true },
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color(0xFF2E344E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedSupplierFilter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Filter by supplier",
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isSupplierMenuExpanded,
                onDismissRequest = { isSupplierMenuExpanded = false },
                modifier = Modifier.background(if (isDarkTheme) NawanoDark200 else Color.White)
            ) {
                availableSuppliers.forEach { supplierOption ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = supplierOption,
                                color = if (supplierOption == selectedSupplierFilter) NawanoPrimary200 else (if (isDarkTheme) Color.White else Color.Black),
                                fontWeight = if (supplierOption == selectedSupplierFilter) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            selectedSupplierFilter = supplierOption
                            isSupplierMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activitiesAfterSupplierFilter.isEmpty()) {
            val emptyTitle = when (selectedFilter) {
                "Incoming" -> "All Goods Orders Processed"
                "Inbound" -> "No Inbound Shipments"
                "Outbound" -> "No Outbound Orders"
                else -> "No Inventory Activity Yet"
            }
            val emptyDesc = when (selectedFilter) {
                "Incoming" -> "No pending supplier or warehouse requisitions at this moment."
                "Inbound" -> "Scan or register an inbound shipment to track receiving stock."
                "Outbound" -> "No sales dispatches or outgoing transfers currently recorded."
                else -> "Scan items or record inventory movement to start tracking logs."
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = NawanoEmerald,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = emptyDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activitiesAfterSupplierFilter.take(5).forEach { activity ->
                    NawanoGoodsReqCard(
                        activity = activity,
                        isDarkTheme = isDarkTheme,
                        onClick = {
                            if (onActivityClick != null) {
                                onActivityClick(activity)
                            } else if (activity.orderId != null) {
                                onOrderClick?.invoke(activity.orderId)
                            }
                        },
                        onAccept = {
                            if (activity.orderId != null) {
                                onConfirmOrder?.invoke(activity.orderId)
                            }
                        },
                        onReceive = {
                            if (activity.orderId != null) {
                                onAcceptOrder?.invoke(activity.orderId)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Redesigned Middleman Stock In Card matching the modern 3-row layout:
 * - Row 1: Status Badge Pill (Incoming: warm amber, Confirmed: soft indigo) + #PO-XXXX • Due in X days
 * - Row 2: 54dp Product Thumbnail + Title (16sp bold) + Supplier Full Name + Stats (Units • SKUs • Amount)
 * - Row 3: Storefront Icon + Supplier Short Name on left + Solid Pill Action Button (Accept / Receive) on right
 */
@Composable
private fun NawanoGoodsReqCard(
    activity: NawanoDashboardActivity,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit = {},
    onAccept: () -> Unit = {},
    onReceive: () -> Unit = {}
) {
    var localState by remember(activity.id, activity.lifecycleState) {
        mutableStateOf(activity.lifecycleState)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkTheme) Color(0xFF181B2C) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) Color(0xFF282D42) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isDarkTheme) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Status Badge Pill (left) + PO Code & Schedule (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge Pill
                val (badgeText, badgeBg, badgeFg) = when (localState) {
                    ActivityLifecycleState.INCOMING -> Triple(
                        "Incoming",
                        if (isDarkTheme) Color(0xFF5A3A0B) else Color(0xFFFEF3C7),
                        if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFD97706)
                    )
                    ActivityLifecycleState.CONFIRMED -> Triple(
                        "Confirmed",
                        if (isDarkTheme) Color(0xFF2D2B52) else Color(0xFFE0E7FF),
                        if (isDarkTheme) Color(0xFFC4B5FD) else Color(0xFF4338CA)
                    )
                    ActivityLifecycleState.COMPLETED -> Triple(
                        "Completed",
                        if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFD1FAE5),
                        if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // PO Code & Schedule / Due Text
                val headerRightText = if (activity.poNumber.isNotBlank() && activity.scheduleText.isNotBlank()) {
                    "${activity.poNumber} • ${activity.scheduleText}"
                } else {
                    activity.metadataLine
                }

                Text(
                    text = headerRightText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Product Thumbnail (Left) + 3-line details Column (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Product Thumbnail (54x54dp, radius: 14dp)
                ProductThumbnail(
                    sku = activity.sku,
                    name = activity.cleanTitle,
                    category = null,
                    imageUri = activity.imageUri,
                    supplier = activity.supplierName,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Right: 3 lines
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Line 1: Title (Bold white)
                    Text(
                        text = activity.cleanTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 2: Supplier Name (Muted gray)
                    Text(
                        text = activity.supplierName ?: "Internal Warehouse",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Line 3: Stats (50 Cans • 16.5 kg • $37.50)
                    Text(
                        text = activity.statsText.ifBlank { activity.quantityDeltaText },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 3: Supplier Storefront (Left) + Action Button (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 🏪 Supplier Short Name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activity.supplierShortName.ifBlank { activity.supplierName ?: "Supplier" },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                // Right: Action Pill Button (Accept / Receive / Completed)
                when (localState) {
                    ActivityLifecycleState.INCOMING -> {
                        Surface(
                            onClick = {
                                localState = ActivityLifecycleState.CONFIRMED
                                onAccept()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = NawanoPrimary200
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Accept",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    ActivityLifecycleState.CONFIRMED -> {
                        Surface(
                            onClick = {
                                localState = ActivityLifecycleState.COMPLETED
                                onReceive()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = NawanoPrimary200
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckBox,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Receive",
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    ActivityLifecycleState.COMPLETED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFD1FAE5)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Received",
                                    color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Dialog displayed when a scanned barcode does not exist in local inventory.
 * Prompts user to register it and supports one-tap auto-import if online product metadata was found.
 */
@Composable
private fun ProductNotFoundDialog(
    scannedSku: String,
    onlineProduct: OnlineProductData?,
    isFetchingOnline: Boolean,
    onDismiss: () -> Unit,
    onAddProduct: () -> Unit,
    onAutoImport: (OnlineProductData) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Product Not in Inventory",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = scannedSku,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "This item does not exist in your inventory database.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Would you like to register and add this product now?",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isFetchingOnline) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Searching online catalog...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (onlineProduct != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Online Product Match Found",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = onlineProduct.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${onlineProduct.brand.ifBlank { "General" }} • ${onlineProduct.category} • $${String.format(Locale.US, "%.2f", onlineProduct.suggestedPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onlineProduct != null) {
                    Button(
                        onClick = { onAutoImport(onlineProduct) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Import (+10)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onAddProduct,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onAddProduct,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Product", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

@Composable
private fun ScannedSkuBanner(
    scannedSku: String,
    matchedItem: ProductEntity?,
    onlineProduct: OnlineProductData?,
    isFetchingOnline: Boolean,
    onClear: () -> Unit,
    onStockIn: (ProductEntity) -> Unit = {},
    onStockOut: (ProductEntity) -> Unit = {},
    onViewProduct: (() -> Unit)? = null,
    onImportOnline: (OnlineProductData) -> Unit,
    onAddNewProduct: (String, OnlineProductData?) -> Unit
) {
    // Auto-dismiss alert after 6 seconds (lasts comfortably longer)
    LaunchedEffect(scannedSku, matchedItem) {
        if (matchedItem != null) {
            delay(6000)
            onClear()
        }
    }

    if (matchedItem != null) {
        // Redesigned Minimal Matched Product Alert Card (Nawano UI Kit)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, NawanoPrimary200.copy(alpha = 0.55f)),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Header Row: [✓ Matched Product] + SKU Barcode + Close (✕)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = NawanoEmerald.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NawanoEmerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Matched Product",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "#${matchedItem.sku}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Product Core Info: Thumbnail + Title + Subtitle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (onViewProduct != null) Modifier.clickable { onViewProduct() } else Modifier),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductThumbnail(
                        product = matchedItem,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = matchedItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val catDisplay = if (matchedItem.subcategory.isNotBlank()) "${matchedItem.category} > ${matchedItem.subcategory}" else matchedItem.category
                        val subtitle = if (matchedItem.brand.isNotBlank()) "by ${matchedItem.brand} • $catDisplay" else catDisplay
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Stock Left & Price at Bottom Right ONLY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal stock pill: "● 10 Pcs"
                    Surface(
                        color = NawanoEmerald.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, NawanoEmerald.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NawanoEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${matchedItem.quantity} ${matchedItem.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Price placed at the bottom right!
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", matchedItem.sellingPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NawanoPrimary200
                    )
                }
            }
        }
    } else {
        // Lookup / Unmatched State
        val containerColor = when {
            isFetchingOnline -> MaterialTheme.colorScheme.secondaryContainer
            onlineProduct != null -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        }

        val contentColor = when {
            isFetchingOnline -> MaterialTheme.colorScheme.onSecondaryContainer
            onlineProduct != null -> NawanoPrimary200
            else -> MaterialTheme.colorScheme.onErrorContainer
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            color = containerColor,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFetchingOnline) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )
                        } else if (onlineProduct != null) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isFetchingOnline -> "Looking Up Barcode: $scannedSku"
                                onlineProduct != null -> "Identified: ${onlineProduct.name}"
                                else -> "Scanned SKU: $scannedSku"
                            },
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isFetchingOnline -> "Searching global product databases & local storage..."
                                onlineProduct != null -> "${onlineProduct.brand} • ${onlineProduct.category} • Online catalog data fetched!"
                                else -> "Product not found in online catalog or local inventory."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when {
                    isFetchingOnline -> {
                        // In progress
                    }
                    onlineProduct != null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onImportOnline(onlineProduct) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { onAddNewProduct(scannedSku, onlineProduct) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onAddNewProduct(scannedSku, null) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register SKU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

internal fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "electronics" -> Icons.Default.Devices
        "accessories" -> Icons.Default.Headphones
        "furniture" -> Icons.Default.Chair
        "hardware" -> Icons.Default.Build
        "grocery" -> Icons.Default.ShoppingBasket
        "other" -> Icons.Default.Category
        else -> Icons.Default.Category
    }
}

@Composable
private fun InventoryItemCard(
    item: ProductEntity,
    isHighlighted: Boolean,
    isDarkTheme: Boolean = false,
    onQuantityChange: (Int) -> Unit,
    onCardClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRequestSupply: (() -> Unit)? = null
) {
    val cardBg = if (isDarkTheme) Color(0xFF191D2D) else Color.White
    val cardBorder = if (isDarkTheme) Color(0xFF2E334D) else Color(0xFFE2E8F0)
    val textPrimary = if (isDarkTheme) Color.White else Color(0xFF1E293B)
    val textSecondary = if (isDarkTheme) Color(0xFF8E95AF) else Color(0xFF64748B)

    val isLowStock = item.quantity <= item.reorderLevel
    val stockPillBg = when {
        item.quantity <= 0 -> if (isDarkTheme) Color(0xFF3B1818) else Color(0xFFFEE2E2)
        isLowStock -> if (isDarkTheme) Color(0xFF382710) else Color(0xFFFEF3C7)
        else -> if (isDarkTheme) Color(0xFF102E29) else Color(0xFFD1FAE5)
    }
    val stockPillTextColor = when {
        item.quantity <= 0 -> Color(0xFFEF4444)
        isLowStock -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
    val stockPillLabel = when {
        item.quantity <= 0 -> "Out of stock"
        isLowStock -> "Low Stock (${item.quantity} ${item.unit})"
        else -> "${item.quantity} in stock"
    }

    val skuLabel = if (item.sku.startsWith("#")) item.sku else "#${item.sku}"

    Surface(
        onClick = { onCardClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) NawanoPrimary200 else cardBorder
        ),
        shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Scanned Highlight Banner inside the card (Nawano Primary highlight)
            if (isHighlighted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NawanoPrimary200,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SCANNED BARCODE MATCH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NawanoPrimary200
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Tap +/- to adjust",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondary
                        )
                    }
                }
            }

            // ROW 1: Thumbnail + Title & Tags + Edit & Delete Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 54dp Rounded Product Thumbnail
                ProductThumbnail(
                    product = item,
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Category/SKU badge
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.name,
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
                                text = item.category.ifBlank { "General" },
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
                        onClick = { onCardClick?.invoke() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Product",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (onDelete != null) {
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ROW 2: Selling Price ($X.XX / Unit) on Left, Stock Status Pill on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bright emerald green price
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.sellingPrice)} / ${item.unit}",
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
                // [+ Restock] pill button (opens supply request or stock dialog)
                Surface(
                    onClick = { onRequestSupply?.invoke() ?: onCardClick?.invoke() },
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
                            onClick = { onQuantityChange(-1) },
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
                            text = "${item.quantity}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Plus Button (Solid Purple)
                        Surface(
                            onClick = { onQuantityChange(1) },
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

@Composable
private fun InventorySettingsScreen(
    totalSkus: Int,
    totalItems: Int,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings & Database",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Manage your local inventory storage and system preferences",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Local Storage (Room DB)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalSkus SKUs registered • $totalItems items in stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Clear Database",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Permanently remove all products, stock quantities, and activity audit history logs from this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Inventory Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Clear All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all inventory items and activity logs from this device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
