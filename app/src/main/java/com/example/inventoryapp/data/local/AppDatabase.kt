package com.example.inventoryapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.PurchaseOrderDao
import com.example.inventoryapp.data.local.dao.StockTransactionDao
import com.example.inventoryapp.data.local.dao.SupplierDao
import com.example.inventoryapp.data.local.dao.UserDao
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderItemEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.SupplierEntity
import com.example.inventoryapp.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ProductEntity::class,
        StockTransactionEntity::class,
        UserEntity::class,
        SupplierEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun userDao(): UserDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `full_name` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `password_hash` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `supplier_id` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `supplierId` TEXT")
                db.execSQL("ALTER TABLE `products` ADD COLUMN `reorderQty` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `stock_transactions` ADD COLUMN `orderId` TEXT")
                db.execSQL("ALTER TABLE `stock_transactions` ADD COLUMN `unitCost` REAL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `suppliers` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchase_orders` (
                        `id` TEXT NOT NULL,
                        `orderNumber` TEXT NOT NULL,
                        `supplierId` TEXT NOT NULL,
                        `supplierName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `totalQuantity` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `picSender` TEXT NOT NULL,
                        `trackingNumber` TEXT NOT NULL,
                        `ownerNote` TEXT NOT NULL,
                        `supplierNote` TEXT NOT NULL,
                        `estArrival` TEXT NOT NULL,
                        `totalWeight` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `confirmedAt` INTEGER,
                        `deliveredAt` INTEGER,
                        `receivedAt` INTEGER,
                        `cancelledAt` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchase_order_items` (
                        `id` TEXT NOT NULL,
                        `orderId` TEXT NOT NULL,
                        `productId` TEXT,
                        `sku` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCost` REAL NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_order_items_orderId` ON `purchase_order_items` (`orderId`)")
            }
        }

        val MIGRATION_6_8 = object : Migration(6, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_6_7.migrate(db)
                MIGRATION_7_8.migrate(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "inventory_database"
                    )
                        .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_6_8)
                        .fallbackToDestructiveMigration(true)
                        .addCallback(DatabaseSeederCallback())
                        .build()
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error building database, falling back to in-memory", e)
                    Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    )
                        .fallbackToDestructiveMigration(true)
                        .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeederCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed sample supplier and mock PO on creation in background
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultSuppliersAndOrders(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultSuppliersAndOrders(database)
                }
            }
        }

        private suspend fun seedDefaultSuppliersAndOrders(database: AppDatabase) {
            try {
                val supplierDao = database.supplierDao()
                val orderDao = database.purchaseOrderDao()

                // Check if Too Frozen Food supplier exists
                var supplier = supplierDao.getSupplierByEmail("supplier@nawano.com")
                if (supplier == null) {
                    supplier = SupplierEntity(
                        id = "sup_too_frozen",
                        name = "Too Frozen Food",
                        contactName = "Leslie Alexander",
                        phone = "+1 (555) 234-8901",
                        email = "supplier@nawano.com",
                        address = "450 Harbor Way, Bay 4, Toronto",
                        note = "Cold-chain premium seafood and frozen goods distributor",
                        isActive = true
                    )
                    supplierDao.insertSupplier(supplier)

                    // Also add a second supplier
                    supplierDao.insertSupplier(
                        SupplierEntity(
                            id = "sup_apex_dist",
                            name = "Apex Supply & Hardware Co.",
                            contactName = "Marcus Vance",
                            phone = "+1 (555) 987-6543",
                            email = "marcus@apexsupply.com",
                            address = "1200 Industrial Parkway, Chicago",
                            note = "Primary hardware, cables, and packaging distributor",
                            isActive = true
                        )
                    )
                }

                // Check if demo orders exist matching Behance mockup
                val existingOrder = orderDao.getOrderById("po_nwnus303832049831")
                if (existingOrder == null) {
                    val order1 = PurchaseOrderEntity(
                        id = "po_nwnus303832049831",
                        orderNumber = "NWNUS303832049831",
                        supplierId = supplier.id,
                        supplierName = supplier.name,
                        status = PurchaseOrderStatus.DELIVERED, // "In Delivery"
                        totalAmount = 875.00,
                        totalQuantity = 25,
                        title = "0603 - Items crab in from Toronto weighing 1.5 KG",
                        movementType = "Goods Inbound",
                        picSender = "Leslie Alexander",
                        trackingNumber = "TRK-TOR-0603",
                        ownerNote = "Urgent weekend replenishment for cold storage",
                        supplierNote = "Dispatched via refrigerated freight line 04",
                        estArrival = "09.30 - 11.30 AM",
                        totalWeight = "1.5 KG"
                    )
                    orderDao.insertOrder(order1)

                    orderDao.insertOrderItems(
                        listOf(
                            PurchaseOrderItemEntity(
                                orderId = order1.id,
                                productId = null,
                                sku = "CRB-0603",
                                productName = "King Crab Cluster (Toronto Pack)",
                                quantity = 15,
                                unitCost = 35.00
                            ),
                            PurchaseOrderItemEntity(
                                orderId = order1.id,
                                productId = null,
                                sku = "LOB-0604",
                                productName = "Atlantic Frozen Lobster Tails",
                                quantity = 10,
                                unitCost = 35.00
                            )
                        )
                    )

                    // Add a second pending order
                    val order2 = PurchaseOrderEntity(
                        id = "po_lobster_0604",
                        orderNumber = "NWNUS303832049832",
                        supplierId = supplier.id,
                        supplierName = supplier.name,
                        status = PurchaseOrderStatus.CONFIRMED,
                        totalAmount = 450.00,
                        totalQuantity = 12,
                        title = "0604 - Items lobster in from Nova Scotia",
                        movementType = "Goods Inbound",
                        picSender = "Leslie Alexander",
                        trackingNumber = "TRK-NS-0604",
                        ownerNote = "Regular weekly seafood shipment",
                        supplierNote = "Packed and scheduled for pickup",
                        estArrival = "01.00 - 03.30 PM",
                        totalWeight = "2.2 KG"
                    )
                    orderDao.insertOrder(order2)

                    orderDao.insertOrderItems(
                        listOf(
                            PurchaseOrderItemEntity(
                                orderId = order2.id,
                                productId = null,
                                sku = "LOB-0604",
                                productName = "Atlantic Frozen Lobster Tails",
                                quantity = 12,
                                unitCost = 37.50
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error seeding default suppliers and orders", e)
            }
        }
    }
}
