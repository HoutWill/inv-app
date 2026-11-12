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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.remote.OnlineProductData
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.ui.scanner.BarcodeScannerScreen
import com.example.inventoryapp.ui.supplier.NewPurchaseOrderDialog
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
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {},
    userName: String = "Anthony",
    userRole: String = "Middle Man • Nawano HQ",
    onLogout: (() -> Unit)? = null,
    onSwitchToSupplier: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableIntStateOf(0) }
    var scannerTarget by remember { mutableStateOf(ScannerTarget.NONE) }
    var isAddProductOpen by remember { mutableStateOf(false) }
    var isNotificationsOpen by remember { mutableStateOf(false) }
    var viewingProductDetails by remember { mutableStateOf<ProductEntity?>(null) }
    var addProductDraft by remember { mutableStateOf(AddProductDraft()) }
    var scannedNotFoundSku by remember { mutableStateOf<String?>(null) }
    var isUpcomingSheetOpen by remember { mutableStateOf(false) }
    var viewingOrderWithItems by remember { mutableStateOf<OrderWithItems?>(null) }
    var showNewPoDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var selectedSupplierForPo by remember { mutableStateOf<SupplierEntity?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var drawerDestination by remember { mutableStateOf<DrawerDestination?>(null) }

    val categoryList = listOf("All", "Electronics", "Accessories", "Furniture", "Hardware", "Grocery", "Other", "Low Stock")

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
                    drawerDestination = DrawerDestination.MOVEMENT_LIST
                }
                DrawerDestination.AUDIT_LOGS -> {
                    drawerDestination = DrawerDestination.AUDIT_LOGS
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
                        onOpenNotifications = { isNotificationsOpen = true }
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
                                isDarkTheme = isDarkTheme
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
                                    viewModel.onCategorySelected("Low Stock")
                                    activeTab = 1
                                },
                                onNavigateToMoveSummary = {
                                    drawerDestination = DrawerDestination.MOVEMENT_LIST
                                },
                                onNavigateToUpcoming = {
                                    isUpcomingSheetOpen = true
                                },
                                onNavigateToQtyChanges = {
                                    drawerDestination = DrawerDestination.AUDIT_LOGS
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
                                        viewingOrderWithItems = viewModel.getOrderWithItems(orderId)
                                    }
                                },
                                onReceiveOrder = { orderId ->
                                    viewModel.receiveOrder(orderId) { success ->
                                        if (success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Shipment accepted & stock updated!")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        1 -> {
                            // TAB 1: Dedicated Items & Catalog Screen
                            InventoryItemsCatalogTab(
                                uiState = uiState,
                                categoryList = categoryList,
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
                                }
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
                                val cleanNoZeros = cleanSku.trimStart('0')
                                val exactMatch = uiState.products.find { it.sku.trim().equals(cleanSku, ignoreCase = true) }
                                val localMatch = exactMatch ?: if (cleanNoZeros.isNotBlank()) {
                                    uiState.products.find { it.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true) }
                                } else null
                                viewModel.onSkuScanned(scannedSku)
                                scannerTarget = ScannerTarget.NONE

                                if (localMatch == null) {
                                    scannedNotFoundSku = cleanSku
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Found '${localMatch.name}' in inventory (${localMatch.quantity} ${localMatch.unit})")
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
                    }
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
                    }
                )
            }

            // Movement Detail Fullscreen overlay
            viewingOrderWithItems?.let { currentOrderWithItems ->
                MovementDetailScreen(
                    order = currentOrderWithItems.order,
                    items = currentOrderWithItems.items,
                    onBack = { viewingOrderWithItems = null },
                    onReceiveOrder = { orderId ->
                        viewModel.receiveOrder(orderId) { success ->
                            if (success) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Shipment received & inventory stocked successfully!")
                                }
                                viewingOrderWithItems = null
                            }
                        }
                    },
                    onRejectOrder = { orderId ->
                        viewModel.rejectOrder(orderId, "Rejected by store receiving")
                        scope.launch {
                            snackbarHostState.showSnackbar("Shipment rejected")
                        }
                        viewingOrderWithItems = null
                    }
                )
            }

            // New Purchase Order Dialog
            if (showNewPoDialog) {
                NewPurchaseOrderDialog(
                    initialSupplier = selectedSupplierForPo,
                    suppliers = uiState.suppliers.filter { it.isActive },
                    products = uiState.products,
                    isDarkTheme = isDarkTheme,
                    onDismiss = { showNewPoDialog = false },
                    onSubmit = { sup, title, pic, items, note, estArrival ->
                        viewModel.createPurchaseOrder(sup, title, pic, items, note, estArrival) { created ->
                            showNewPoDialog = false
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
    onReceiveOrder: (String) -> Unit = {}
) {
    var selectedActivityFilter by remember { mutableStateOf("Stock In") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
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

        // 1. Signature Nawano Hero Gradient Card: Inventory Summary (Behance: 174422339)
        item {
            NawanoHeroInventorySummaryCard(uiState = uiState)
        }

        // 2. 4 Quick Operational Cards (Low Stock, Move Summary, Upcoming, Qty Changes)
        item {
            NawanoOperationalCardsGrid(
                uiState = uiState,
                onNavigateToLowStock = onNavigateToLowStock,
                onNavigateToMoveSummary = onNavigateToMoveSummary,
                onNavigateToUpcoming = onNavigateToUpcoming,
                onNavigateToQtyChanges = onNavigateToQtyChanges
            )
        }

        // 3. Suppliers & Restock Section (Existing Suppliers & Direct Restock Action)
        item {
            SuppliersRestockSection(
                suppliers = uiState.suppliers.filter { it.isActive },
                onAddSupplier = onAddSupplier,
                onRestockSupplier = onRestockSupplier,
                onViewAllSuppliers = onViewAllSuppliers,
                isDarkTheme = isDarkTheme
            )
        }

        // 4. Activity Filter Chips Row (All Activity, Incoming, Inbound, Outbound)
        item {
            NawanoActivityFilterChips(
                selectedFilter = selectedActivityFilter,
                onFilterSelected = { selectedActivityFilter = it },
                isDarkTheme = isDarkTheme
            )
        }

        // 5. Incoming Goods Req Section (from Behance slide_2.png)
        item {
            NawanoIncomingGoodsSection(
                selectedFilter = selectedActivityFilter,
                transactions = uiState.transactions,
                orders = uiState.orders,
                onViewAll = onNavigateToMoveSummary,
                onOrderClick = onOrderClick,
                onAcceptOrder = onReceiveOrder
            )
        }

        // 5. Items Inventory List (Behance slide_2.png)
        if (uiState.products.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items Inventory List (${uiState.products.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToItems) {
                        Text(
                            text = "View all >",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NawanoPrimary200
                        )
                    }
                }
            }

            items(items = uiState.products.take(6), key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    isHighlighted = item.sku.equals(uiState.scannedSku, ignoreCase = true),
                    onQuantityChange = { delta -> onQuantityChange(item, delta) },
                    onCardClick = { onProductClick(item) },
                    onDelete = null
                )
            }
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
    onRegisterNewProduct: (String, OnlineProductData?) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

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

        // Search Bar with Scan Shortcut
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search catalog by name, SKU, brand...",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChanged("") },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onScanClicked,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pos_barcode),
                            contentDescription = "Scan SKU to Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoryList.forEach { category ->
                val isSelected = uiState.selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            text = category,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = if (category == "Low Stock" && uiState.lowStockCount > 0) {
                        {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else null
                )
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
                Spacer(modifier = Modifier.height(6.dp))
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    subcategories.forEach { sub ->
                        val isAll = sub.startsWith("All ")
                        val subValue = if (isAll) "All" else sub
                        val isSubSelected = uiState.selectedSubcategory == subValue
                        FilterChip(
                            selected = isSubSelected,
                            onClick = { onSubcategorySelected(subValue) },
                            label = {
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Subheader: Total Results Count + Unified Action Buttons (Add & Sort)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredProducts.size} Items Displayed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Add Product Button (Matching Nawano UI Kit)
                Button(
                    onClick = onAddProductClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NawanoPrimary200,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Product",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sort Dropdown Pill
                Box {
                    Surface(
                        onClick = { sortMenuExpanded = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(15.dp),
                                tint = NawanoPrimary200
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sort",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
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

    Spacer(modifier = Modifier.height(8.dp))

        // Inventory Items List / Empty State
        if (uiState.filteredProducts.isEmpty()) {
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
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_inventory_box),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No catalog products found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank() || uiState.selectedCategory != "All")
                                "No items match '${uiState.searchQuery}'. Try resetting filters or search query."
                            else "Your inventory is currently empty. Tap below to register your first product.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddProductClicked,
                            shape = RoundedCornerShape(12.dp),
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp)
            ) {
                items(items = uiState.filteredProducts, key = { item -> item.id }) { item ->
                    val isHighlighted = item.sku.equals(uiState.scannedSku, ignoreCase = true)
                    InventoryItemCard(
                        item = item,
                        isHighlighted = isHighlighted,
                        onQuantityChange = { delta -> onQuantityChange(item, delta) },
                        onCardClick = { onProductClick(item) },
                        onDelete = { onDeleteProduct(item) }
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
    onNavigateToLowStock: () -> Unit,
    onNavigateToMoveSummary: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToQtyChanges: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Low Stock + Movements
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NawanoOperationalCard(
                title = "Low Stock",
                description = "All stock items that are low inventory",
                stat = "${uiState.lowStockCount} Items",
                icon = Icons.Outlined.Warning,
                iconTint = if (uiState.lowStockCount > 0) Color(0xFFEF4444) else Color(0xFFF59E0B),
                onClick = onNavigateToLowStock,
                modifier = Modifier.weight(1f)
            )

            NawanoOperationalCard(
                title = "Movements",
                description = "Track inventory that has moved locations",
                stat = "${uiState.transactions.size} Orders",
                painter = painterResource(R.drawable.ic_bus_intro),
                iconTint = NawanoPrimary200,
                onClick = onNavigateToMoveSummary,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Upcoming Expiry + Audits
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val upcomingCount = uiState.products.count { it.quantity <= it.reorderLevel || it.quantity <= 5 }
            NawanoOperationalCard(
                title = "Expiring",
                description = "Items in inventory set to expire soon",
                stat = "$upcomingCount Items",
                icon = Icons.Outlined.Schedule,
                iconTint = Color(0xFF8B5CF6),
                onClick = onNavigateToUpcoming,
                modifier = Modifier.weight(1f)
            )

            NawanoOperationalCard(
                title = "Audits",
                description = "All inflows and outflows for an item",
                stat = "${uiState.transactions.size} Items",
                icon = Icons.Outlined.BarChart,
                iconTint = Color(0xFF10B981),
                onClick = onNavigateToQtyChanges,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NawanoOperationalCard(
    title: String,
    description: String,
    stat: String,
    icon: ImageVector? = null,
    painter: Painter? = null,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (painter != null) {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(17.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stat,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Visual model representing an activity card on the Dashboard, supporting both
 * real stock transactions and logistics requisition items.
 */
private data class NawanoDashboardActivity(
    val id: String,
    val sku: String,
    val title: String,
    val type: String, // "INCOMING", "INBOUND", "OUTBOUND"
    val badgeText: String,
    val badgeColor: Color,
    val arrivalTime: String,
    val reference: String,
    val quantityChange: String,
    val isPositive: Boolean,
    val orderId: String? = null,
    val isDeliveredOrder: Boolean = false
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
    onViewAll: () -> Unit,
    onOrderClick: ((String) -> Unit)? = null,
    onAcceptOrder: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Collect all activities from real Room orders and transactions
    val allActivities = remember(transactions, orders) {
        val list = mutableListOf<NawanoDashboardActivity>()

        // 1. Live purchase orders from suppliers (Delivered / Confirmed / Pending)
        orders.filter {
            it.status == PurchaseOrderStatus.DELIVERED ||
            it.status == PurchaseOrderStatus.CONFIRMED ||
            it.status == PurchaseOrderStatus.PENDING
        }.forEach { po ->
            val isInbound = po.status == PurchaseOrderStatus.DELIVERED
            val badgeText = if (isInbound) "In Delivery" else if (po.status == PurchaseOrderStatus.CONFIRMED) "Confirmed" else "Incoming Req"
            val badgeColor = if (isInbound) Color(0xFF4338CA) else if (po.status == PurchaseOrderStatus.CONFIRMED) NawanoPrimary200 else Color(0xFFF59E0B)
            val type = if (isInbound) "INBOUND" else "INCOMING"

            list.add(
                NawanoDashboardActivity(
                    id = "order_${po.id}",
                    sku = po.orderNumber,
                    title = po.title.ifBlank { "Items from ${po.supplierName}" },
                    type = type,
                    badgeText = badgeText,
                    badgeColor = badgeColor,
                    arrivalTime = "Est: ${po.estArrival.ifBlank { "Today" }} • ${po.supplierName}",
                    reference = "Supplier: ${po.supplierName} • PIC: ${po.picSender.ifBlank { "Leslie Alexander" }}",
                    quantityChange = "+${po.totalQuantity} units",
                    isPositive = true,
                    orderId = po.id,
                    isDeliveredOrder = isInbound
                )
            )
        }

        // 2. Map real transactions (most recent on top)
        transactions.forEach { tx ->
            val isInbound = tx.quantityDelta > 0 || tx.transactionType == "STOCK_IN" || tx.transactionType == "NEW_ITEM"
            val isRequisition = tx.transactionType.contains("REQ", ignoreCase = true) ||
                    (tx.note != null && tx.note.contains("req", ignoreCase = true))

            val type = when {
                isRequisition -> "INCOMING"
                isInbound -> "INBOUND"
                else -> "OUTBOUND"
            }
            val badgeText = when (type) {
                "INCOMING" -> "Incoming Req"
                "INBOUND" -> "Goods Inbound"
                else -> "Goods Outbound"
            }
            val badgeColor = when (type) {
                "INCOMING" -> Color(0xFF8B5CF6)
                "INBOUND" -> Color(0xFF10B981)
                else -> Color(0xFFEF4444)
            }
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
            val timeStr = timeFormat.format(Date(tx.timestamp))

            list.add(
                NawanoDashboardActivity(
                    id = tx.id,
                    sku = tx.sku,
                    title = tx.productName,
                    type = type,
                    badgeText = badgeText,
                    badgeColor = badgeColor,
                    arrivalTime = "Today • $timeStr",
                    reference = "Ref: ${tx.note?.takeIf { it.isNotBlank() } ?: "Direct Inventory Update"}",
                    quantityChange = "${if (tx.quantityDelta >= 0) "+" else ""}${tx.quantityDelta} units",
                    isPositive = tx.quantityDelta >= 0
                )
            )
        }
        list
    }

    // Filter by the currently selected chip
    val filteredActivities = remember(allActivities, selectedFilter) {
        when (selectedFilter) {
            "Stock In" -> allActivities.filter { it.isPositive || it.type == "INCOMING" || it.type == "INBOUND" }
            "Stock Out" -> allActivities.filter { !it.isPositive || it.type == "OUTBOUND" }
            else -> allActivities // "All"
        }
    }

    val sectionTitle = when (selectedFilter) {
        "Stock In" -> "Stock In (${filteredActivities.size})"
        "Stock Out" -> "Stock Out (${filteredActivities.size})"
        else -> "Activity (${filteredActivities.size})"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View all >",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NawanoPrimary200
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredActivities.isEmpty()) {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredActivities.take(3).forEach { activity ->
                    NawanoGoodsReqCard(
                        activity = activity,
                        onClick = {
                            if (activity.orderId != null) {
                                onOrderClick?.invoke(activity.orderId)
                            }
                        },
                        onAccept = {
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

@Composable
private fun NawanoGoodsReqCard(
    activity: NawanoDashboardActivity,
    onClick: () -> Unit = {},
    onAccept: () -> Unit = {}
) {
    var isActioned by remember(activity.id) { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Badge on left + Action button on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = activity.badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = activity.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = activity.badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    onClick = {
                        isActioned = true
                        onAccept()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActioned) Color(0xFFD1FAE5) else if (activity.type == "OUTBOUND") Color(0xFFEDE9FE) else NawanoPrimary200
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isActioned) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isActioned) {
                                if (activity.type == "OUTBOUND") "Dispatched" else "Accepted"
                            } else {
                                if (activity.type == "OUTBOUND") "Dispatch" else "Accept"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActioned) Color(0xFF047857) else if (activity.type == "OUTBOUND") NawanoPrimary200 else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title & arrival
            Text(
                text = "${activity.sku} - ${activity.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = activity.arrivalTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            // Footer reference and quantity change
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Total: ${activity.quantityChange}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (activity.isPositive) Color(0xFF047857) else Color(0xFFB91C1C)
                )
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
    onQuantityChange: (Int) -> Unit,
    onCardClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isLowStock = item.quantity <= item.reorderLevel
    val categoryIcon = getCategoryIcon(item.category)

    val (stockText, stockColor, stockContainerColor) = when {
        item.quantity <= 0 -> Triple("Out of Stock", Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.12f))
        isLowStock -> Triple("Low Stock (${item.quantity} ${item.unit})", Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.12f))
        else -> Triple("${item.quantity} ${item.unit} in stock", Color(0xFF10B981), Color(0xFF10B981).copy(alpha = 0.12f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isHighlighted) 2.dp else 1.dp,
            color = if (isHighlighted) NawanoPrimary200 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Top Content Row: Thumbnail + Product Name & Metadata + Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Product Thumbnail (54dp with rounded corners)
                ProductThumbnail(
                    product = item,
                    modifier = Modifier.size(54.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Title and Delete Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(28.dp)
                                    .offset(x = 4.dp, y = (-4).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete product",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Author / Brand subtitle if present
                    if (item.brand.isNotBlank()) {
                        Text(
                            text = "by ${item.brand}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Meta Chips Row: Monospace SKU + Category Pill + Location
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Monospace SKU Chip
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "#${item.sku}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Hierarchical Category > Subcategory Pill
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val catBreadcrumb = if (item.subcategory.isNotBlank()) "${item.category} > ${item.subcategory}" else item.category
                            Text(
                                text = catBreadcrumb,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NawanoPrimary200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (item.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = item.location,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Flexible Row: Stock Status Pill (left) + Price & Tactile Stepper (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Status Chip with rounded pill
                Surface(
                    color = stockContainerColor,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, stockColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(stockColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stockText,
                            style = MaterialTheme.typography.labelSmall,
                            color = stockColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Price and Tactile Stepper (Nawano UI Kit styling)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.sellingPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NawanoPrimary200,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Surface(
                                onClick = { onQuantityChange(-1) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease Stock",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .defaultMinSize(minWidth = 22.dp),
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                onClick = { onQuantityChange(1) },
                                shape = CircleShape,
                                color = NawanoPrimary200,
                                shadowElevation = 1.5.dp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase Stock",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
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
