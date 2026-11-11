package com.example.inventoryapp

import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.PurchaseOrderDao
import com.example.inventoryapp.data.local.dao.StockTransactionDao
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakePurchaseOrderDao : PurchaseOrderDao {
    val orders = mutableMapOf<String, PurchaseOrderEntity>()
    val items = mutableMapOf<String, MutableList<PurchaseOrderItemEntity>>()

    override fun getAllOrders(): Flow<List<PurchaseOrderEntity>> = MutableStateFlow(orders.values.toList())
    override fun getOrdersBySupplier(supplierId: String): Flow<List<PurchaseOrderEntity>> =
        MutableStateFlow(orders.values.filter { it.supplierId == supplierId })
    override fun getOrdersByStatus(status: PurchaseOrderStatus): Flow<List<PurchaseOrderEntity>> =
        MutableStateFlow(orders.values.filter { it.status == status })
    override fun getOrdersByStatuses(statuses: List<PurchaseOrderStatus>): Flow<List<PurchaseOrderEntity>> =
        MutableStateFlow(orders.values.filter { it.status in statuses })
    override suspend fun getOrderById(id: String): PurchaseOrderEntity? = orders[id]
    override fun observeOrderById(id: String): Flow<PurchaseOrderEntity?> = MutableStateFlow(orders[id])
    override fun getItemsForOrder(orderId: String): Flow<List<PurchaseOrderItemEntity>> =
        MutableStateFlow(items[orderId] ?: emptyList())
    override suspend fun getItemsForOrderSync(orderId: String): List<PurchaseOrderItemEntity> =
        items[orderId] ?: emptyList()
    override suspend fun insertOrder(order: PurchaseOrderEntity) { orders[order.id] = order }
    override suspend fun insertOrderItems(newItems: List<PurchaseOrderItemEntity>) {
        newItems.forEach { item ->
            items.getOrPut(item.orderId) { mutableListOf() }.add(item)
        }
    }
    override suspend fun updateOrder(order: PurchaseOrderEntity) { orders[order.id] = order }
    override suspend fun updateOrderStatus(orderId: String, newStatus: PurchaseOrderStatus, updatedAt: Long) {
        val o = orders[orderId] ?: return
        orders[orderId] = o.copy(status = newStatus, updatedAt = updatedAt)
    }
    override suspend fun deleteOrder(id: String) { orders.remove(id); items.remove(id) }
    override suspend fun clearAllOrders() { orders.clear() }
    override suspend fun clearAllOrderItems() { items.clear() }
}

class FakeProductDao : ProductDao {
    val products = mutableMapOf<String, ProductEntity>()
    override fun getAllProducts(): Flow<List<ProductEntity>> = MutableStateFlow(products.values.toList())
    override suspend fun getProductCount(): Int = products.size
    override suspend fun getProductBySku(sku: String): ProductEntity? = products.values.firstOrNull { it.sku.equals(sku, ignoreCase = true) }
    override suspend fun getProductById(id: String): ProductEntity? = products[id]
    override fun searchProducts(query: String): Flow<List<ProductEntity>> = MutableStateFlow(products.values.toList())
    override fun getLowStockProducts(): Flow<List<ProductEntity>> = MutableStateFlow(products.values.filter { it.quantity <= it.reorderLevel })
    override suspend fun insertProduct(product: ProductEntity) { products[product.id] = product }
    override suspend fun updateProduct(product: ProductEntity) { products[product.id] = product }
    override suspend fun deleteProduct(product: ProductEntity) { products.remove(product.id) }
    override suspend fun deleteProductById(id: String): Int { return if (products.remove(id) != null) 1 else 0 }
    override suspend fun clearAllProducts(): Int { val c = products.size; products.clear(); return c }
}

class FakeTransactionDao : StockTransactionDao {
    val transactions = mutableListOf<StockTransactionEntity>()
    override fun getAllTransactions(): Flow<List<StockTransactionEntity>> = MutableStateFlow(transactions)
    override fun getTransactionsForProduct(productId: String): Flow<List<StockTransactionEntity>> =
        MutableStateFlow(transactions.filter { it.productId == productId })
    override suspend fun insertTransaction(transaction: StockTransactionEntity) { transactions.add(transaction) }
    override suspend fun clearAllTransactions(): Int { val c = transactions.size; transactions.clear(); return c }
}

class OrderRepositoryUnitTest {
    private lateinit var orderDao: FakePurchaseOrderDao
    private lateinit var productDao: FakeProductDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var repository: OrderRepository

    @Before
    fun setup() {
        orderDao = FakePurchaseOrderDao()
        productDao = FakeProductDao()
        transactionDao = FakeTransactionDao()
        repository = OrderRepository(null, orderDao, productDao, transactionDao)
    }

    @Test
    fun testOrderLifecycle_Pending_Confirmed_Delivered_Received() = runBlocking {
        // 1. Pre-register an existing product with 10 units at $20.00
        val product = ProductEntity(
            id = "prod_crab",
            sku = "CRB-0603",
            name = "King Crab Cluster",
            category = "Grocery",
            quantity = 10,
            costPrice = 20.0,
            sellingPrice = 35.0
        )
        productDao.insertProduct(product)

        // 2. Create order with 15 units at $30.00 each
        val items = listOf(
            PurchaseOrderItemEntity(
                orderId = "",
                productId = product.id,
                sku = product.sku,
                productName = product.name,
                quantity = 15,
                unitCost = 30.0
            )
        )
        val createResult = repository.createOrder(
            supplierId = "sup_frozen",
            supplierName = "Too Frozen Food",
            title = "0603 - Items crab in from Toronto",
            picSender = "Leslie Alexander",
            items = items
        )
        assertTrue(createResult.isSuccess)
        val order = createResult.getOrThrow()
        assertEquals(PurchaseOrderStatus.PENDING, order.status)
        assertEquals(450.0, order.totalAmount, 0.01)
        assertEquals(15, order.totalQuantity)

        // Stock must NOT change yet on order creation
        assertEquals(10, productDao.getProductById(product.id)?.quantity)

        // 3. Supplier Confirms
        val confirmResult = repository.confirmOrder(order.id, "Will ship by refrigerated truck")
        assertTrue(confirmResult.isSuccess)
        assertEquals(PurchaseOrderStatus.CONFIRMED, orderDao.getOrderById(order.id)?.status)

        // 4. Supplier Marks Delivered ("In Delivery")
        val deliverResult = repository.markDelivered(order.id, "TRK-999")
        assertTrue(deliverResult.isSuccess)
        assertEquals(PurchaseOrderStatus.DELIVERED, orderDao.getOrderById(order.id)?.status)

        // Stock must still NOT change on delivery dispatch
        assertEquals(10, productDao.getProductById(product.id)?.quantity)

        // 5. Owner physical receipt -> Stock increments and weighted average cost updates!
        val receiveResult = repository.receiveOrder(order.id)
        assertTrue(receiveResult.isSuccess)
        assertEquals(PurchaseOrderStatus.RECEIVED, orderDao.getOrderById(order.id)?.status)

        val updatedProduct = productDao.getProductById(product.id)
        assertNotNull(updatedProduct)
        // Previous 10 units + received 15 units = 25 units
        assertEquals(25, updatedProduct?.quantity)

        // Weighted Average Cost:
        // (10 * 20.0 + 15 * 30.0) / 25 = (200 + 450) / 25 = 650 / 25 = 26.0
        assertEquals(26.0, updatedProduct?.costPrice ?: 0.0, 0.01)

        // Verify transaction logged
        val tx = transactionDao.transactions.find { it.orderId == order.id }
        assertNotNull(tx)
        assertEquals("PO_RECEIVED", tx?.transactionType)
        assertEquals(15, tx?.quantityDelta)
        assertEquals(10, tx?.previousQuantity)
        assertEquals(25, tx?.newQuantity)
    }
}
