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
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.util.PasswordHasher
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
                val productDao = database.productDao()
                val userDao = database.userDao()

                // Seed default users safely without aborting seeding on conflict
                try {
                    if (userDao.findByEmail("middleman@nawano.com") == null) {
                        userDao.insert(
                            UserEntity(
                                id = "user_middleman",
                                fullName = "Anthony (Middle Man)",
                                email = "middleman@nawano.com",
                                passwordHash = PasswordHasher.hash("password123"),
                                role = UserRole.OWNER
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Middleman user exists: ${e.message}")
                }
                try {
                    if (userDao.findByEmail("supplier@nawano.com") == null) {
                        userDao.insert(
                            UserEntity(
                                id = "user_supplier_pacific",
                                fullName = "Pacific Seafood Supply (Supplier)",
                                email = "supplier@nawano.com",
                                passwordHash = PasswordHasher.hash("password123"),
                                role = UserRole.SUPPLIER
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Supplier user exists: ${e.message}")
                }
                try {
                    if (userDao.findByEmail("foodie@nawano.com") == null) {
                        userDao.insert(
                            UserEntity(
                                id = "user_supplier_foodie",
                                fullName = "Foodie Supplier (Supplier)",
                                email = "foodie@nawano.com",
                                passwordHash = PasswordHasher.hash("password123"),
                                role = UserRole.SUPPLIER
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Foodie user exists: ${e.message}")
                }

                // Purge legacy mock supplier records if any
                val legacyTooFrozen = supplierDao.getSupplierById("sup_too_frozen")
                if (legacyTooFrozen != null) supplierDao.deleteSupplier(legacyTooFrozen)
                val legacyApex = supplierDao.getSupplierById("sup_apex_dist")
                if (legacyApex != null) supplierDao.deleteSupplier(legacyApex)

                // Canonical partner suppliers matching the Behance design & portal accounts
                val canonicalSuppliers = listOf(
                    SupplierEntity(
                        id = "sup_pacific_seafood",
                        name = "Pacific Seafood Supply",
                        contactName = "Brandon Vaccaro",
                        phone = "+1 (555) 234-8901",
                        email = "supplier@nawano.com",
                        address = "1550 Bedok North Ave 4, #04 30, Singapore",
                        note = "Cold-chain premium seafood, frozen lobster & beverages",
                        isActive = true
                    ),
                    SupplierEntity(
                        id = "sup_foodie",
                        name = "Foodie Supplier",
                        contactName = "Brandon Vaccaro",
                        phone = "+1 (555) 345-6789",
                        email = "foodie@nawano.com",
                        address = "450 Harbor Way, Bay 4, Toronto",
                        note = "Specialty imported seafood, Italian pantry & gourmet goods",
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

                // Seed catalog products with REAL global EAN-13 barcodes!
                val partnerProducts = listOf(
                    // Real Barcode Products (EAN-13)
                    ProductEntity(
                        id = "prod_coca_cola_330",
                        sku = "5449000000996",
                        name = "Coca-Cola Original (330ml Can)",
                        category = "Beverages",
                        brand = "Coca-Cola",
                        costPrice = 0.75,
                        sellingPrice = 1.50,
                        quantity = 45,
                        unit = "Can",
                        supplierId = "sup_pacific_seafood",
                        reorderLevel = 15,
                        reorderQty = 50,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_coca_cola"
                    ),
                    ProductEntity(
                        id = "prod_red_bull_250",
                        sku = "9002490100070",
                        name = "Red Bull Energy Drink (250ml)",
                        category = "Beverages",
                        brand = "Red Bull",
                        costPrice = 1.60,
                        sellingPrice = 2.80,
                        quantity = 30,
                        unit = "Can",
                        supplierId = "sup_pacific_seafood",
                        reorderLevel = 10,
                        reorderQty = 40,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_red_bull"
                    ),
                    ProductEntity(
                        id = "prod_nutella_400",
                        sku = "3017620422003",
                        name = "Nutella Hazelnut Spread (400g)",
                        category = "Pantry",
                        brand = "Ferrero",
                        costPrice = 3.20,
                        sellingPrice = 5.40,
                        quantity = 20,
                        unit = "Jar",
                        supplierId = "sup_foodie",
                        reorderLevel = 8,
                        reorderQty = 24,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_nutella"
                    ),
                    ProductEntity(
                        id = "prod_barilla_pesto",
                        sku = "8076809513753",
                        name = "Barilla Pesto alla Genovese (190g)",
                        category = "Pantry",
                        brand = "Barilla",
                        costPrice = 2.40,
                        sellingPrice = 4.20,
                        quantity = 25,
                        unit = "Jar",
                        supplierId = "sup_foodie",
                        reorderLevel = 8,
                        reorderQty = 30,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_barilla_pesto"
                    ),
                    ProductEntity(
                        id = "prod_heinz_ketchup",
                        sku = "8715700421639",
                        name = "Heinz Tomato Ketchup (500g)",
                        category = "Condiments",
                        brand = "Heinz",
                        costPrice = 1.85,
                        sellingPrice = 3.25,
                        quantity = 35,
                        unit = "Bottle",
                        supplierId = "sup_vegan",
                        reorderLevel = 10,
                        reorderQty = 20,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_heinz_ketchup"
                    ),
                    ProductEntity(
                        id = "prod_oreo_original",
                        sku = "7622210449283",
                        name = "Oreo Original Sandwich Cookies (154g)",
                        category = "Snacks",
                        brand = "Oreo",
                        costPrice = 1.20,
                        sellingPrice = 2.10,
                        quantity = 50,
                        unit = "Pack",
                        supplierId = "sup_apex",
                        reorderLevel = 15,
                        reorderQty = 40,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_oreo"
                    ),
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
                        reorderQty = 15,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_king_crab"
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
                        reorderQty = 20,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_atlantic_salmon"
                    ),
                    ProductEntity(
                        id = "prod_lob_0402",
                        sku = "LOB-0402",
                        name = "Boston Live Lobster (1.5 lb)",
                        category = "Seafood",
                        costPrice = 28.00,
                        sellingPrice = 39.50,
                        quantity = 30,
                        unit = "Pcs",
                        supplierId = "sup_pacific_seafood",
                        reorderQty = 15,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_boston_lobster"
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
                        reorderQty = 10,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_wagyu_ribeye"
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
                        reorderQty = 25,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_organic_tofu"
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
                        reorderQty = 50,
                        imageUri = "android.resource://com.example.inventoryapp/drawable/img_carton_box"
                    )
                )

                for (p in partnerProducts) {
                    val existing = productDao.getProductBySku(p.sku)
                    if (existing == null) {
                        productDao.insertProduct(p)
                    } else if (existing.supplierId != p.supplierId || existing.name != p.name || existing.imageUri.isNullOrBlank()) {
                        productDao.updateProduct(
                            existing.copy(
                                name = p.name,
                                supplierId = p.supplierId,
                                brand = p.brand,
                                costPrice = p.costPrice,
                                sellingPrice = p.sellingPrice,
                                unit = p.unit,
                                reorderQty = p.reorderQty,
                                imageUri = p.imageUri ?: existing.imageUri
                            )
                        )
                    }
                }

                // Seed REAL active Purchase Orders connecting Middleman and Supplier
                val existingPo = orderDao.getOrderById("po_real_001")
                if (existingPo == null) {
                    val now = System.currentTimeMillis()

                    // Order 1: CONFIRMED - Pacific Seafood Supply (Coca-Cola & Red Bull)
                    val po1 = PurchaseOrderEntity(
                        id = "po_real_001",
                        orderNumber = "PO-2026-0842",
                        supplierId = "sup_pacific_seafood",
                        supplierName = "Pacific Seafood Supply",
                        status = PurchaseOrderStatus.CONFIRMED,
                        totalAmount = 112.00,
                        totalQuantity = 90,
                        title = "Cold-Chain Restock • Coca-Cola & Red Bull",
                        movementType = "Inbound PO",
                        picSender = "Brandon Vaccaro",
                        trackingNumber = "TRK-892174092",
                        ownerNote = "Storefront beverage display restock. Scan cold-chain barcode upon receipt.",
                        supplierNote = "Pallet verified and packed in cold-chain cooler.",
                        estArrival = "Tomorrow, 10:00 AM",
                        totalWeight = "28.5 kg",
                        createdAt = now - 3600_000 * 4,
                        confirmedAt = now - 3600_000 * 2,
                        updatedAt = now - 3600_000 * 2
                    )
                    val po1Items = listOf(
                        PurchaseOrderItemEntity(
                            id = "poi_coke_01",
                            orderId = "po_real_001",
                            productId = "prod_coca_cola_330",
                            sku = "5449000000996",
                            productName = "Coca-Cola Original (330ml Can)",
                            quantity = 50,
                            unitCost = 0.75
                        ),
                        PurchaseOrderItemEntity(
                            id = "poi_redbull_01",
                            orderId = "po_real_001",
                            productId = "prod_red_bull_250",
                            sku = "9002490100070",
                            productName = "Red Bull Energy Drink (250ml)",
                            quantity = 40,
                            unitCost = 1.60
                        )
                    )
                    orderDao.insertOrder(po1)
                    orderDao.insertOrderItems(po1Items)

                    // Order 2: DELIVERED (In-Transit) - Foodie Supplier Co. (Nutella & Barilla Pesto)
                    val po2 = PurchaseOrderEntity(
                        id = "po_real_002",
                        orderNumber = "PO-2026-0843",
                        supplierId = "sup_foodie",
                        supplierName = "Foodie Supplier Co.",
                        status = PurchaseOrderStatus.DELIVERED,
                        totalAmount = 243.00,
                        totalQuantity = 54,
                        title = "Gourmet Pantry Inbound • Nutella & Barilla Pesto",
                        movementType = "Inbound PO",
                        picSender = "David Kim",
                        trackingNumber = "DHL-SG-5849201",
                        ownerNote = "Weekly Italian pantry batch.",
                        supplierNote = "Dispatched via DHL Express Freight. Driver ETA 2:30 PM.",
                        estArrival = "Today, 2:30 PM",
                        totalWeight = "24.0 kg",
                        createdAt = now - 3600_000 * 8,
                        confirmedAt = now - 3600_000 * 6,
                        deliveredAt = now - 3600_000 * 1,
                        updatedAt = now - 3600_000 * 1
                    )
                    val po2Items = listOf(
                        PurchaseOrderItemEntity(
                            id = "poi_nutella_02",
                            orderId = "po_real_002",
                            productId = "prod_nutella_400",
                            sku = "3017620422003",
                            productName = "Nutella Hazelnut Spread (400g)",
                            quantity = 24,
                            unitCost = 3.20
                        ),
                        PurchaseOrderItemEntity(
                            id = "poi_barilla_02",
                            orderId = "po_real_002",
                            productId = "prod_barilla_pesto",
                            sku = "8076809513753",
                            productName = "Barilla Pesto alla Genovese (190g)",
                            quantity = 30,
                            unitCost = 2.40
                        )
                    )
                    orderDao.insertOrder(po2)
                    orderDao.insertOrderItems(po2Items)

                    // Order 3: PENDING - Pacific Seafood Supply (Coca-Cola Cans Restock)
                    val po3 = PurchaseOrderEntity(
                        id = "po_real_003",
                        orderNumber = "PO-2026-0844",
                        supplierId = "sup_pacific_seafood",
                        supplierName = "Pacific Seafood Supply",
                        status = PurchaseOrderStatus.PENDING,
                        totalAmount = 37.50,
                        totalQuantity = 50,
                        title = "Inbound Request • Coca-Cola Cans Restock",
                        movementType = "Inbound PO",
                        picSender = "Brandon Vaccaro",
                        trackingNumber = "",
                        ownerNote = "Please accept and confirm production batch.",
                        supplierNote = "",
                        estArrival = "In 2 days",
                        totalWeight = "16.5 kg",
                        createdAt = now - 3600_000 * 1,
                        updatedAt = now - 3600_000 * 1
                    )
                    val po3Items = listOf(
                        PurchaseOrderItemEntity(
                            id = "poi_coke_03",
                            orderId = "po_real_003",
                            productId = "prod_coca_cola_330",
                            sku = "5449000000996",
                            productName = "Coca-Cola Original (330ml Can)",
                            quantity = 50,
                            unitCost = 0.75
                        )
                    )
                    orderDao.insertOrder(po3)
                    orderDao.insertOrderItems(po3Items)
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error seeding default suppliers", e)
            }
        }
    }
}
