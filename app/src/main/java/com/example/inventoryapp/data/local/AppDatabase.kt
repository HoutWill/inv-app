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
                val transactionDao = database.stockTransactionDao()

                // Remove old test data so middleman & supplier start completely clean
                orderDao.clearAllOrders()
                orderDao.clearAllOrderItems()
                transactionDao.clearAllTransactions()

                // Purge legacy mock supplier records if any
                val legacyTooFrozen = supplierDao.getSupplierById("sup_too_frozen")
                if (legacyTooFrozen != null) supplierDao.deleteSupplier(legacyTooFrozen)
                val legacyApex = supplierDao.getSupplierById("sup_apex_dist")
                if (legacyApex != null) supplierDao.deleteSupplier(legacyApex)

                // The 4 canonical partner suppliers matching the Behance design & portal accounts
                val canonicalSuppliers = listOf(
                    SupplierEntity(
                        id = "sup_pacific_seafood",
                        name = "Pacific Seafood Supply",
                        contactName = "Brandon Vaccaro",
                        phone = "+1 (555) 234-8901",
                        email = "supplier@nawano.com",
                        address = "1550 Bedok North Ave 4, #04 30, Singapore",
                        note = "Cold-chain premium seafood, frozen lobster & salmon",
                        isActive = true
                    ),
                    SupplierEntity(
                        id = "sup_foodie",
                        name = "Foodie Supplier",
                        contactName = "Brandon Vaccaro",
                        phone = "+1 (555) 345-6789",
                        email = "foodie@nawano.com",
                        address = "450 Harbor Way, Bay 4, Toronto",
                        note = "Specialty imported seafood, meats & gourmet ingredients",
                        isActive = true
                    ),
                    SupplierEntity(
                        id = "sup_vegan",
                        name = "Vegan Community",
                        contactName = "Jesse Henry",
                        phone = "+1 (555) 456-7890",
                        email = "vegan@nawano.com",
                        address = "01-33H, The Centrepoint, 176 Orchard Rd",
                        note = "Organic produce, fresh veggies & plant-based stock",
                        isActive = true
                    ),
                    SupplierEntity(
                        id = "sup_apex",
                        name = "Apex Supply & Hardware Co.",
                        contactName = "Marcus Vance",
                        phone = "+1 (555) 987-6543",
                        email = "marcus@apexsupply.com",
                        address = "1200 Industrial Parkway, Chicago",
                        note = "Primary hardware, industrial boxes & freight packaging",
                        isActive = true
                    )
                )

                for (s in canonicalSuppliers) {
                    val existing = supplierDao.getSupplierById(s.id) ?: supplierDao.getSupplierByEmail(s.email)
                    if (existing == null) {
                        supplierDao.insertSupplier(s)
                    } else {
                        supplierDao.updateSupplier(
                            existing.copy(
                                name = s.name,
                                contactName = s.contactName,
                                phone = s.phone,
                                email = s.email,
                                address = s.address,
                                note = s.note,
                                isActive = true
                            )
                        )
                    }
                }

                // Seed catalog products for each canonical partner supplier
                val productDao = database.productDao()
                val partnerProducts = listOf(
                    // Pacific Seafood Supply
                    ProductEntity(
                        id = "prod_crb_0603",
                        sku = "CRB-0603",
                        name = "King Crab Cluster (Toronto Pack)",
                        category = "Seafood",
                        costPrice = 35.00,
                        sellingPrice = 48.00,
                        quantity = 25,
                        unit = "Box",
                        supplierId = "sup_pacific_seafood",
                        reorderQty = 15
                    ),
                    ProductEntity(
                        id = "prod_slm_0201",
                        sku = "SLM-0201",
                        name = "Atlantic Salmon Fillet (Fresh Cut)",
                        category = "Seafood",
                        costPrice = 22.50,
                        sellingPrice = 32.00,
                        quantity = 40,
                        unit = "Kg",
                        supplierId = "sup_pacific_seafood",
                        reorderQty = 20
                    ),
                    ProductEntity(
                        id = "prod_lob_0402",
                        sku = "LOB-0402",
                        name = "Boston Live Lobster (1.5 lb)",
                        category = "Seafood",
                        costPrice = 48.00,
                        sellingPrice = 65.00,
                        quantity = 18,
                        unit = "Pcs",
                        supplierId = "sup_pacific_seafood",
                        reorderQty = 10
                    ),
                    // Foodie Supplier
                    ProductEntity(
                        id = "prod_wgy_0801",
                        sku = "WGY-0801",
                        name = "A5 Japanese Wagyu Ribeye",
                        category = "Meats",
                        costPrice = 85.00,
                        sellingPrice = 120.00,
                        quantity = 15,
                        unit = "Kg",
                        supplierId = "sup_foodie",
                        reorderQty = 10
                    ),
                    ProductEntity(
                        id = "prod_trf_0305",
                        sku = "TRF-0305",
                        name = "Black Truffle Olive Oil (250ml)",
                        category = "Gourmet",
                        costPrice = 28.00,
                        sellingPrice = 39.50,
                        quantity = 30,
                        unit = "Bottle",
                        supplierId = "sup_foodie",
                        reorderQty = 12
                    ),
                    ProductEntity(
                        id = "prod_cvr_0104",
                        sku = "CVR-0104",
                        name = "Imperial Ossetra Caviar (50g)",
                        category = "Gourmet",
                        costPrice = 110.00,
                        sellingPrice = 165.00,
                        quantity = 8,
                        unit = "Tin",
                        supplierId = "sup_foodie",
                        reorderQty = 5
                    ),
                    // Vegan Community
                    ProductEntity(
                        id = "prod_tofu_0501",
                        sku = "TOFU-0501",
                        name = "Organic Silken Tofu (Twin Pack)",
                        category = "Produce",
                        costPrice = 4.50,
                        sellingPrice = 6.80,
                        quantity = 50,
                        unit = "Pack",
                        supplierId = "sup_vegan",
                        reorderQty = 25
                    ),
                    ProductEntity(
                        id = "prod_oat_0702",
                        sku = "OAT-0702",
                        name = "Barista Blend Oat Milk (1L)",
                        category = "Dairy-Free",
                        costPrice = 5.20,
                        sellingPrice = 7.50,
                        quantity = 60,
                        unit = "Carton",
                        supplierId = "sup_vegan",
                        reorderQty = 30
                    ),
                    ProductEntity(
                        id = "prod_mush_0903",
                        sku = "MUSH-0903",
                        name = "Wild Forest Shiitake Mushrooms",
                        category = "Produce",
                        costPrice = 8.00,
                        sellingPrice = 12.00,
                        quantity = 35,
                        unit = "Pack",
                        supplierId = "sup_vegan",
                        reorderQty = 15
                    ),
                    // Apex Supply & Hardware Co.
                    ProductEntity(
                        id = "prod_box_1001",
                        sku = "BOX-1001",
                        name = "Heavy Duty Corrugated Carton (Large)",
                        category = "Packaging",
                        costPrice = 3.50,
                        sellingPrice = 5.50,
                        quantity = 120,
                        unit = "Pcs",
                        supplierId = "sup_apex",
                        reorderQty = 50
                    ),
                    ProductEntity(
                        id = "prod_tpe_2002",
                        sku = "TPE-2002",
                        name = "Industrial Reinforced Packing Tape",
                        category = "Packaging",
                        costPrice = 6.00,
                        sellingPrice = 9.00,
                        quantity = 80,
                        unit = "Roll",
                        supplierId = "sup_apex",
                        reorderQty = 40
                    ),
                    ProductEntity(
                        id = "prod_thrm_3003",
                        sku = "THRM-3003",
                        name = "Cold Chain Thermal Insulated Liner",
                        category = "Packaging",
                        costPrice = 12.50,
                        sellingPrice = 18.00,
                        quantity = 45,
                        unit = "Pcs",
                        supplierId = "sup_apex",
                        reorderQty = 20
                    )
                )

                for (p in partnerProducts) {
                    val existing = productDao.getProductBySku(p.sku)
                    if (existing == null) {
                        productDao.insertProduct(p)
                    } else if (existing.supplierId != p.supplierId) {
                        productDao.updateProduct(existing.copy(supplierId = p.supplierId))
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error seeding default suppliers", e)
            }
        }
    }
}
