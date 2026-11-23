package com.example.inventoryapp.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.entity.PartnerConnectionEntity
import com.example.inventoryapp.data.local.entity.PartnerConnectionWithUser
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PosPaymentMethod
import com.example.inventoryapp.data.local.entity.PosPaymentStatus
import com.example.inventoryapp.data.local.entity.SalesOrderEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.remote.BarcodeLookupService
import com.example.inventoryapp.data.remote.OnlineProductData
import com.example.inventoryapp.data.repository.InventoryRepository
import com.example.inventoryapp.data.repository.OrderRepository
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.data.repository.PartnerConnectionRepository
import com.example.inventoryapp.data.repository.SupplierRepository
import com.example.inventoryapp.data.prefs.SessionStore
import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import com.example.inventoryapp.util.StockNotificationHelper
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ProductSortOption(val displayName: String) {
    NAME_ASC("Name (A-Z)"),
    STOCK_LOW_TO_HIGH("Stock: Low to High"),
    STOCK_HIGH_TO_LOW("Stock: High to Low"),
    PRICE_HIGH_TO_LOW("Price: High to Low")
}

/**
 * Standard Hierarchical Taxonomy for POS Product Organization
 */
object CategoryHierarchy {
    val HIERARCHY: Map<String, List<String>> = mapOf(
        "Seafood" to listOf("Fish", "Crab & Shrimp", "Shellfish", "Frozen"),
        "Meat" to listOf("Beef", "Pork", "Poultry", "Sausages"),
        "Meats" to listOf("Beef", "Pork", "Poultry", "Sausages"),
        "Produce" to listOf("Fruits", "Vegetables", "Herbs"),
        "Dairy" to listOf("Milk", "Cheese", "Butter", "Yogurt"),
        "Beverages" to listOf("Soda", "Energy Drinks", "Water", "Juice", "Coffee & Tea"),
        "Pantry" to listOf("Pasta & Grains", "Sauces", "Canned Goods", "Spices"),
        "Snacks" to listOf("Chips", "Cookies", "Nuts", "Candy"),
        "Electronics" to listOf("Audio", "Computers", "Mobile", "Cables", "Components"),
        "Accessories" to listOf("Bags", "Cases", "Chargers", "Screen Protectors"),
        "Furniture" to listOf("Chairs", "Desks", "Storage", "Lighting"),
        "Hardware" to listOf("Tools", "Fasteners", "Safety Gear", "Electrical"),
        "Grocery" to listOf("Beverages", "Snacks", "Pantry", "Household"),
        "Other" to listOf("General", "Supplies", "Packaging", "Misc")
    )

    fun getSubcategories(category: String): List<String> {
        return HIERARCHY[category] ?: emptyList()
    }
}

private data class FilterParams(
    val query: String,
    val category: String,
    val subcategory: String,
    val sort: ProductSortOption,
    val scannedSku: String?
)

data class DashboardUiState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val transactions: List<StockTransactionEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val orders: List<PurchaseOrderEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedSubcategory: String = "All",
    val sortOption: ProductSortOption = ProductSortOption.NAME_ASC,
    val scannedSku: String? = null,
    val matchedProduct: ProductEntity? = null,
    val onlineProduct: OnlineProductData? = null,
    val isFetchingOnline: Boolean = false,
    val totalSkus: Int = 0,
    val totalItemsCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val selectedCurrency: AppCurrency = AppCurrency.USD,
    val exchangeRateUsdToKhr: Double = CurrencyFormatter.DEFAULT_EXCHANGE_RATE,
    val paywayMerchantId: String = "ec000002",
    val paywayApiKey: String = "",
    val paywayIsSandbox: Boolean = true,
    val isLoading: Boolean = false
)

data class PartnerConnectionUiState(
    val searchResult: UserEntity? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val actionMessage: String? = null,
    val isSubmitting: Boolean = false
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = InventoryRepository(database.productDao(), database.stockTransactionDao())
    val supplierRepository = SupplierRepository(database.supplierDao())
    val orderRepository = OrderRepository(database, database.purchaseOrderDao(), database.productDao(), database.stockTransactionDao())
    val partnerConnectionRepository = PartnerConnectionRepository(database.partnerConnectionDao(), database.userDao(), database.supplierDao())

    private val _partnerUiState = MutableStateFlow(PartnerConnectionUiState())
    val partnerUiState: StateFlow<PartnerConnectionUiState> = _partnerUiState.asStateFlow()

    val unpaidSalesOrders: StateFlow<List<SalesOrderEntity>> = database.salesOrderDao().getUnpaidOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unpaidSalesOrdersCount: StateFlow<Int> = database.salesOrderDao().getUnpaidOrdersCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val count = database.salesOrderDao().getUnpaidOrdersCount().first()
            if (count == 0) {
                val now = System.currentTimeMillis()
                val dayMs = 24 * 60 * 60 * 1000L
                val sampleTabs = listOf(
                    SalesOrderEntity(
                        invoiceNumber = "INV-TAB-001",
                        customerName = "Sokha Pich",
                        customerPhone = "012 998 776",
                        totalAmountUsd = 45.00,
                        totalAmountKhr = (45.00 * 4050).toLong(),
                        paidAmountUsd = 0.0,
                        remainingAmountUsd = 45.00,
                        paymentStatus = PosPaymentStatus.UNPAID,
                        paymentMethod = PosPaymentMethod.PAY_LATER,
                        dueDate = now - (2 * dayMs),
                        itemsSummary = "2x Coca-Cola Original, 1x Red Bull",
                        itemCount = 3,
                        createdAt = now - (9 * dayMs),
                        note = "Neighborhood shop customer tab"
                    ),
                    SalesOrderEntity(
                        invoiceNumber = "INV-TAB-002",
                        customerName = "Chhay Lim",
                        customerPhone = "098 776 543",
                        totalAmountUsd = 12.50,
                        totalAmountKhr = (12.50 * 4050).toLong(),
                        paidAmountUsd = 0.0,
                        remainingAmountUsd = 12.50,
                        paymentStatus = PosPaymentStatus.UNPAID,
                        paymentMethod = PosPaymentMethod.PAY_LATER,
                        dueDate = now + (3 * dayMs),
                        itemsSummary = "5x Dasani Water 500ml",
                        itemCount = 5,
                        createdAt = now - (4 * dayMs),
                        note = "Regular delivery order"
                    ),
                    SalesOrderEntity(
                        invoiceNumber = "INV-TAB-003",
                        customerName = "Visal Meng",
                        customerPhone = "087 112 233",
                        totalAmountUsd = 84.00,
                        totalAmountKhr = (84.00 * 4050).toLong(),
                        paidAmountUsd = 0.0,
                        remainingAmountUsd = 84.00,
                        paymentStatus = PosPaymentStatus.UNPAID,
                        paymentMethod = PosPaymentMethod.PAY_LATER,
                        dueDate = now + (12 * dayMs),
                        itemsSummary = "1x A5 Wagyu Beef, 2x Orangina",
                        itemCount = 3,
                        createdAt = now - (2 * dayMs),
                        note = "Weekend dinner banquet tab"
                    )
                )
                sampleTabs.forEach { database.salesOrderDao().insertOrder(it) }
            }
        }
    }

    fun processPosSale(
        items: List<Pair<ProductEntity, Int>>,
        paymentMethod: PosPaymentMethod,
        customerName: String,
        customerPhone: String = "",
        dueDate: Long? = null,
        note: String = "",
        onSuccess: (SalesOrderEntity) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalUsd = items.sumOf { it.first.sellingPrice * it.second }
            val totalKhr = (totalUsd * 4050.0).toLong()
            val itemCount = items.sumOf { it.second }
            val itemsSummary = items.joinToString(", ") { "${it.second}x ${it.first.name}" }
            val invoiceNo = "INV-${SimpleDateFormat("MMdd", Locale.getDefault()).format(Date())}-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"

            val isPayLater = paymentMethod == PosPaymentMethod.PAY_LATER || paymentMethod == PosPaymentMethod.COD
            val order = SalesOrderEntity(
                invoiceNumber = invoiceNo,
                customerName = if (isPayLater) customerName.ifBlank { "Customer (COD Tab)" } else customerName.ifBlank { "Walk-in Customer" },
                customerPhone = customerPhone,
                totalAmountUsd = totalUsd,
                totalAmountKhr = totalKhr,
                paidAmountUsd = if (isPayLater) 0.0 else totalUsd,
                remainingAmountUsd = if (isPayLater) totalUsd else 0.0,
                paymentStatus = if (isPayLater) PosPaymentStatus.UNPAID else PosPaymentStatus.PAID,
                paymentMethod = paymentMethod,
                dueDate = dueDate,
                note = note,
                itemsSummary = itemsSummary,
                itemCount = itemCount,
                createdAt = System.currentTimeMillis(),
                settledAt = if (isPayLater) null else System.currentTimeMillis()
            )

            // 1. Insert Sales Order
            database.salesOrderDao().insertOrder(order)

            // 2. Deduct inventory & record StockTransaction
            for ((product, qty) in items) {
                val newQty = (product.quantity - qty).coerceAtLeast(0)
                database.productDao().updateProduct(product.copy(quantity = newQty, updatedAt = System.currentTimeMillis()))
                database.stockTransactionDao().insertTransaction(
                    StockTransactionEntity(
                        productId = product.id,
                        sku = product.sku,
                        productName = product.name,
                        transactionType = if (isPayLater) "POS_SALE_PAY_LATER" else "POS_SALE",
                        quantityDelta = -qty,
                        previousQuantity = product.quantity,
                        newQuantity = newQty,
                        note = "POS ${order.invoiceNumber} (${order.paymentMethod})"
                    )
                )
            }

            withContext(Dispatchers.Main) {
                onSuccess(order)
            }
        }
    }

    fun settleSalesOrder(orderId: String, paymentMethod: PosPaymentMethod = PosPaymentMethod.CASH, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val order = database.salesOrderDao().getOrderById(orderId) ?: return@launch
            val updated = order.copy(
                paidAmountUsd = order.totalAmountUsd,
                remainingAmountUsd = 0.0,
                paymentStatus = PosPaymentStatus.PAID,
                paymentMethod = paymentMethod,
                settledAt = System.currentTimeMillis()
            )
            database.salesOrderDao().updateOrder(updated)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun searchPartner(query: String, currentUser: UserEntity) {
        viewModelScope.launch {
            _partnerUiState.value = _partnerUiState.value.copy(isSearching = true, searchError = null, actionMessage = null)
            val result = partnerConnectionRepository.searchPartner(query, currentUser)
            result.fold(
                onSuccess = { candidate ->
                    _partnerUiState.value = _partnerUiState.value.copy(
                        isSearching = false,
                        searchResult = candidate,
                        searchError = null
                    )
                },
                onFailure = { error ->
                    _partnerUiState.value = _partnerUiState.value.copy(
                        isSearching = false,
                        searchResult = null,
                        searchError = error.message ?: "Partner not found"
                    )
                }
            )
        }
    }

    fun clearPartnerSearch() {
        _partnerUiState.value = _partnerUiState.value.copy(
            searchResult = null,
            isSearching = false,
            searchError = null,
            actionMessage = null
        )
    }

    fun sendConnectionRequest(
        sender: UserEntity,
        target: UserEntity,
        note: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _partnerUiState.value = _partnerUiState.value.copy(isSubmitting = true, searchError = null)
            val result = partnerConnectionRepository.sendConnectionRequest(sender, target, note)
            result.fold(
                onSuccess = {
                    _partnerUiState.value = _partnerUiState.value.copy(
                        isSubmitting = false,
                        actionMessage = "Connection request sent to ${target.fullName}!",
                        searchResult = null
                    )
                    onComplete(true)
                },
                onFailure = { error ->
                    _partnerUiState.value = _partnerUiState.value.copy(
                        isSubmitting = false,
                        searchError = error.message ?: "Failed to send request"
                    )
                    onComplete(false)
                }
            )
        }
    }

    fun acceptConnection(connectionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            partnerConnectionRepository.acceptConnection(connectionId)
            onComplete()
        }
    }

    fun declineConnection(connectionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            partnerConnectionRepository.declineConnection(connectionId)
            onComplete()
        }
    }

    fun cancelConnectionRequest(connectionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            partnerConnectionRepository.cancelRequest(connectionId)
            onComplete()
        }
    }

    fun disconnectPartner(connectionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            partnerConnectionRepository.disconnectPartner(connectionId)
            onComplete()
        }
    }

    fun observeIncomingRequests(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionRepository.observeIncomingRequests(userId)
    }

    fun observeSentRequests(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionRepository.observeSentRequests(userId)
    }

    fun observeActiveConnections(userId: String): Flow<List<PartnerConnectionWithUser>> {
        return partnerConnectionRepository.observeActiveConnections(userId)
    }

    fun observePendingIncomingCount(userId: String): Flow<Int> {
        return partnerConnectionRepository.observePendingIncomingCount(userId)
    }

    fun updateUserProfile(
        userId: String,
        fullName: String,
        phoneNumber: String,
        uniqueCode: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userDao = database.userDao()
                val existing = userDao.findById(userId)
                if (existing != null) {
                    userDao.updateProfile(
                        id = userId,
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        uniqueCode = uniqueCode
                    )
                } else {
                    userDao.insert(
                        UserEntity(
                            id = userId,
                            fullName = fullName,
                            email = "middleman@nawano.com",
                            passwordHash = "",
                            role = UserRole.OWNER,
                            uniqueCode = uniqueCode,
                            phoneNumber = phoneNumber
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, e.message ?: "Failed to update profile")
                }
            }
        }
    }

    val sessionStore = SessionStore(application)

    private val prefs = application.getSharedPreferences("nawano_settings", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("is_dark_mode", next).apply()
    }

    private val _selectedCurrency = MutableStateFlow(
        AppCurrency.fromCode(prefs.getString("selected_currency", AppCurrency.USD.name))
    )
    val selectedCurrency: StateFlow<AppCurrency> = _selectedCurrency.asStateFlow()

    private val _exchangeRateUsdToKhr = MutableStateFlow(
        prefs.getFloat("exchange_rate_usd_to_khr", CurrencyFormatter.DEFAULT_EXCHANGE_RATE.toFloat()).toDouble()
    )
    val exchangeRateUsdToKhr: StateFlow<Double> = _exchangeRateUsdToKhr.asStateFlow()

    fun setSelectedCurrency(currency: AppCurrency) {
        _selectedCurrency.value = currency
        prefs.edit().putString("selected_currency", currency.name).apply()
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.setSelectedCurrency(currency.name)
        }
    }

    fun toggleCurrency() {
        val next = if (_selectedCurrency.value == AppCurrency.USD) AppCurrency.KHR else AppCurrency.USD
        setSelectedCurrency(next)
    }

    fun setExchangeRate(rate: Double) {
        if (rate <= 0) return
        _exchangeRateUsdToKhr.value = rate
        prefs.edit().putFloat("exchange_rate_usd_to_khr", rate.toFloat()).apply()
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.setExchangeRateUsdToKhr(rate)
        }
    }

    private val _paywayMerchantId = MutableStateFlow(
        prefs.getString("payway_merchant_id", "ec000002") ?: "ec000002"
    )
    val paywayMerchantId: StateFlow<String> = _paywayMerchantId.asStateFlow()

    private val _paywayApiKey = MutableStateFlow(
        prefs.getString("payway_api_key", "") ?: ""
    )
    val paywayApiKey: StateFlow<String> = _paywayApiKey.asStateFlow()

    private val _paywayIsSandbox = MutableStateFlow(
        prefs.getBoolean("payway_is_sandbox", true)
    )
    val paywayIsSandbox: StateFlow<Boolean> = _paywayIsSandbox.asStateFlow()

    fun setPaywayConfig(merchantId: String, apiKey: String, isSandbox: Boolean) {
        val cleanMerchant = merchantId.trim().ifBlank { "ec000002" }
        val cleanKey = apiKey.trim()
        _paywayMerchantId.value = cleanMerchant
        _paywayApiKey.value = cleanKey
        _paywayIsSandbox.value = isSandbox

        prefs.edit()
            .putString("payway_merchant_id", cleanMerchant)
            .putString("payway_api_key", cleanKey)
            .putBoolean("payway_is_sandbox", isSandbox)
            .apply()

        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.setPaywayConfig(cleanMerchant, cleanKey, isSandbox)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedSubcategory = MutableStateFlow("All")
    val selectedSubcategory: StateFlow<String> = _selectedSubcategory.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.NAME_ASC)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    private val _scannedSku = MutableStateFlow<String?>(null)
    val scannedSku: StateFlow<String?> = _scannedSku.asStateFlow()

    private val _onlineProduct = MutableStateFlow<OnlineProductData?>(null)
    val onlineProduct: StateFlow<OnlineProductData?> = _onlineProduct.asStateFlow()

    private val _isFetchingOnline = MutableStateFlow(false)
    val isFetchingOnline: StateFlow<Boolean> = _isFetchingOnline.asStateFlow()

    private val filterParamsFlow: Flow<FilterParams> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedSubcategory,
        _sortOption,
        _scannedSku
    ) { query, category, subcategory, sort, scanSku ->
        FilterParams(query, category, subcategory, sort, scanSku)
    }

    private data class SupplierOrderData(
        val suppliers: List<SupplierEntity>,
        val orders: List<PurchaseOrderEntity>
    )

    private data class OnlineLookupState(
        val product: OnlineProductData?,
        val isFetching: Boolean
    )

    private val supplierOrderFlow: Flow<SupplierOrderData> = combine(
        supplierRepository.allSuppliers,
        orderRepository.allOrders
    ) { suppliers, orders ->
        SupplierOrderData(suppliers, orders)
    }

    private val onlineStateFlow: Flow<OnlineLookupState> = combine(
        _onlineProduct,
        _isFetchingOnline
    ) { onlineProd, isFetching ->
        OnlineLookupState(onlineProd, isFetching)
    }

    private val baseDashboardFlow: Flow<DashboardUiState> = combine(
        repository.allProducts,
        repository.allTransactions,
        supplierOrderFlow,
        filterParamsFlow,
        onlineStateFlow
    ) { products, transactions, supplierOrders, filter, onlineState ->
        val query = filter.query
        val category = filter.category
        val subcategory = filter.subcategory
        val sort = filter.sort
        val scanSku = filter.scannedSku
        val onlineProd = onlineState.product
        val isFetching = onlineState.isFetching
        val suppliers = supplierOrders.suppliers
        val orders = supplierOrders.orders

        val filtered = products.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.sku.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.subcategory.contains(query, ignoreCase = true) ||
                    item.brand.contains(query, ignoreCase = true)

            val matchesCategory = when (category) {
                "All" -> true
                "Low Stock" -> item.quantity <= item.reorderLevel
                "Meat", "Meats" -> item.category.equals("Meat", ignoreCase = true) || item.category.equals("Meats", ignoreCase = true)
                "Other" -> item.category.equals("Other", ignoreCase = true) ||
                        item.category.isBlank() ||
                        !CategoryHierarchy.HIERARCHY.keys.any { it != "Other" && (it.equals(item.category, ignoreCase = true) || (it == "Meat" && item.category.equals("Meats", ignoreCase = true))) }
                else -> item.category.equals(category, ignoreCase = true)
            }

            val matchesSubcategory = when {
                subcategory == "All" || subcategory.isBlank() -> true
                else -> item.subcategory.equals(subcategory, ignoreCase = true)
            }

            matchesQuery && matchesCategory && matchesSubcategory
        }

        // Sort items according to sortOption
        val sorted = when (sort) {
            ProductSortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ProductSortOption.STOCK_LOW_TO_HIGH -> filtered.sortedBy { it.quantity }
            ProductSortOption.STOCK_HIGH_TO_LOW -> filtered.sortedByDescending { it.quantity }
            ProductSortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.sellingPrice }
        }

        // Match scanned SKU locally with exact match priority, falling back to flexible matching
        val matched = scanSku?.let { rawSku ->
            val clean = rawSku.trim()
            val cleanNoZeros = clean.trimStart('0')
            val exact = products.find { it.sku.trim().equals(clean, ignoreCase = true) }
            exact ?: if (cleanNoZeros.isNotBlank()) {
                products.find { it.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true) }
            } else null
        }

        // If an item was scanned and matched, ensure it appears at the very top of the list!
        val prioritizedList = if (matched != null && sorted.contains(matched)) {
            listOf(matched) + sorted.filter { it.id != matched.id }
        } else {
            sorted
        }

        DashboardUiState(
            products = products,
            filteredProducts = prioritizedList,
            transactions = transactions,
            suppliers = suppliers,
            orders = orders,
            searchQuery = query,
            selectedCategory = category,
            selectedSubcategory = subcategory,
            sortOption = sort,
            scannedSku = scanSku,
            matchedProduct = matched,
            onlineProduct = onlineProd,
            isFetchingOnline = isFetching,
            totalSkus = products.size,
            totalItemsCount = products.sumOf { it.quantity },
            totalInventoryValue = products.sumOf { it.quantity * it.sellingPrice },
            lowStockCount = products.count { it.quantity <= it.reorderLevel }
        )
    }

    private val paywayConfigFlow = combine(
        _paywayMerchantId,
        _paywayApiKey,
        _paywayIsSandbox
    ) { mId, key, sandbox -> Triple(mId, key, sandbox) }

    val uiState: StateFlow<DashboardUiState> = combine(
        baseDashboardFlow,
        _selectedCurrency,
        _exchangeRateUsdToKhr,
        paywayConfigFlow
    ) { base, currency, rate, (mId, key, sandbox) ->
        base.copy(
            selectedCurrency = currency,
            exchangeRateUsdToKhr = rate,
            paywayMerchantId = mId,
            paywayApiKey = key,
            paywayIsSandbox = sandbox
        )
    }.catch { e ->
        Log.e("InventoryViewModel", "Error in uiState combine flow", e)
        emit(DashboardUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
        _selectedSubcategory.value = "All"
    }

    fun onSubcategorySelected(subcategory: String) {
        _selectedSubcategory.value = subcategory
    }

    fun onSortOptionChanged(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun onSkuScanned(sku: String) {
        val cleanSku = sku.trim()
        _scannedSku.value = cleanSku
        _selectedCategory.value = "All"
        _onlineProduct.value = null

        // Check if item exists in local database with exact match priority
        val currentProducts = uiState.value.products
        val cleanNoZeros = cleanSku.trimStart('0')
        val exactMatch = currentProducts.find { it.sku.trim().equals(cleanSku, ignoreCase = true) }
        val localMatch = exactMatch ?: if (cleanNoZeros.isNotBlank()) {
            currentProducts.find { it.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true) }
        } else null

        if (localMatch != null) {
            _isFetchingOnline.value = false
        } else {
            // Not found in local DB -> Launch online barcode lookup to fetch product data!
            _isFetchingOnline.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val result = BarcodeLookupService.lookupBarcode(cleanSku)
                    _onlineProduct.value = result
                } catch (e: Exception) {
                    Log.w("InventoryViewModel", "Online barcode lookup failed", e)
                } finally {
                    _isFetchingOnline.value = false
                }
            }
        }
    }

    fun clearScannedSku() {
        _scannedSku.value = null
        _onlineProduct.value = null
        _isFetchingOnline.value = false
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                val success = repository.addProduct(product)
                if (success) {
                    _scannedSku.value = product.sku
                    _onlineProduct.value = null
                    _searchQuery.value = ""
                    _selectedCategory.value = "All"
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in addProduct", e)
            }
        }
    }

    fun importOnlineProduct(online: OnlineProductData, initialQuantity: Int = 10, location: String = "Warehouse A") {
        viewModelScope.launch {
            try {
                val newProduct = ProductEntity(
                    id = UUID.randomUUID().toString(),
                    sku = online.sku,
                    name = online.name,
                    category = online.category,
                    subcategory = online.subcategory,
                    brand = online.brand,
                    costPrice = online.suggestedPrice * 0.6,
                    sellingPrice = online.suggestedPrice,
                    quantity = initialQuantity,
                    unit = online.unit,
                    reorderLevel = 5,
                    location = location,
                    imageUri = online.imageUrl
                )
                val success = repository.addProduct(newProduct)
                if (success) {
                    _scannedSku.value = online.sku
                    _onlineProduct.value = null
                    _searchQuery.value = ""
                    _selectedCategory.value = "All"
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in importOnlineProduct", e)
            }
        }
    }

    fun adjustStock(product: ProductEntity, delta: Int, type: String, note: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateStock(product, delta, type, note)
                val newQty = (product.quantity + delta).coerceAtLeast(0)
                if (newQty <= product.reorderLevel) {
                    StockNotificationHelper.notifyLowStock(
                        context = getApplication(),
                        productName = product.name,
                        sku = product.sku,
                        remainingQty = newQty,
                        reorderLevel = product.reorderLevel
                    )
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in adjustStock", e)
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.updateProduct(product)
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in updateProduct", e)
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                if (_scannedSku.value == product.sku) {
                    _scannedSku.value = null
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in deleteProduct", e)
            }
        }
    }

    // --- Supplier and Order Operations ---

    fun saveSupplier(supplier: SupplierEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            supplierRepository.saveSupplier(supplier)
            onComplete()
        }
    }

    fun toggleSupplierActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            supplierRepository.toggleActive(id, isActive)
        }
    }

    fun createPurchaseOrder(
        supplier: SupplierEntity,
        title: String,
        picSender: String,
        items: List<PurchaseOrderItemEntity>,
        ownerNote: String,
        estArrival: String,
        onComplete: (PurchaseOrderEntity?) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = orderRepository.createOrder(
                supplierId = supplier.id,
                supplierName = supplier.name,
                title = title,
                picSender = picSender,
                items = items,
                ownerNote = ownerNote,
                estArrival = estArrival
            )
            onComplete(result.getOrNull())
        }
    }

    fun confirmOrder(orderId: String, note: String? = null) {
        viewModelScope.launch {
            orderRepository.confirmOrder(orderId, note)
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepository.rejectOrder(orderId, reason)
        }
    }

    fun markOrderDelivered(
        orderId: String,
        trackingNumber: String? = null,
        carrier: String? = null,
        estArrival: String? = null,
        totalWeight: String? = null,
        supplierNote: String? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            orderRepository.markDelivered(orderId, trackingNumber, carrier, estArrival, totalWeight, supplierNote)
            onComplete()
        }
    }

    fun receiveOrder(orderId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = orderRepository.receiveOrder(orderId)
            onComplete(result.isSuccess)
        }
    }

    suspend fun getOrderWithItems(orderId: String): OrderWithItems? {
        return orderRepository.getOrderWithItems(orderId)
    }

    fun deleteOrder(orderId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.deleteOrder(orderId)
            onComplete()
        }
    }

    fun updateOrderDetails(orderId: String, title: String, totalQuantity: Int, totalAmount: Double, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            orderRepository.updateOrderDetails(orderId, title, totalQuantity, totalAmount)
            onComplete()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                _scannedSku.value = null
                _onlineProduct.value = null
                _isFetchingOnline.value = false
                _searchQuery.value = ""
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error in clearAllData", e)
            }
        }
    }
}

class InventoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
