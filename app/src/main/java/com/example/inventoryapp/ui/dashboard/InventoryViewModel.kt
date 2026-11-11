package com.example.inventoryapp.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.AppDatabase
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.remote.BarcodeLookupService
import com.example.inventoryapp.data.remote.OnlineProductData
import com.example.inventoryapp.data.repository.InventoryRepository
import com.example.inventoryapp.data.repository.OrderRepository
import com.example.inventoryapp.data.repository.OrderWithItems
import com.example.inventoryapp.data.repository.SupplierRepository
import com.example.inventoryapp.util.StockNotificationHelper
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        "Electronics" to listOf("Audio", "Computers", "Mobile", "Cables", "Components"),
        "Accessories" to listOf("Bags", "Cases", "Chargers", "Screen Protectors"),
        "Furniture" to listOf("Chairs", "Desks", "Storage", "Lighting"),
        "Hardware" to listOf("Tools", "Fasteners", "Safety Gear", "Electrical"),
        "Grocery" to listOf("Beverages", "Snacks", "Pantry", "Household"),
        "Other" to listOf("General", "Supplies", "Misc")
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
    val isLoading: Boolean = false
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = InventoryRepository(database.productDao(), database.stockTransactionDao())
    val supplierRepository = SupplierRepository(database.supplierDao())
    val orderRepository = OrderRepository(database, database.purchaseOrderDao(), database.productDao(), database.stockTransactionDao())

    private val prefs = application.getSharedPreferences("nawano_settings", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("is_dark_mode", next).apply()
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

    val uiState: StateFlow<DashboardUiState> = combine(
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
                "Other" -> item.category.equals("Other", ignoreCase = true) ||
                        item.category.isBlank() ||
                        !CategoryHierarchy.HIERARCHY.keys.any { it != "Other" && it.equals(item.category, ignoreCase = true) }
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

    fun markOrderDelivered(orderId: String, trackingNumber: String? = null) {
        viewModelScope.launch {
            orderRepository.markDelivered(orderId, trackingNumber)
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
