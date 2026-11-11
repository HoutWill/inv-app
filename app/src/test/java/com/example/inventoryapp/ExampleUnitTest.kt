package com.example.inventoryapp

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.inventoryapp.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ExampleUnitTest {
    @Test
    fun testMigrationsConfiguredCorrectly() {
        assertEquals(6, AppDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, AppDatabase.MIGRATION_6_7.endVersion)

        assertEquals(7, AppDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, AppDatabase.MIGRATION_7_8.endVersion)

        assertEquals(6, AppDatabase.MIGRATION_6_8.startVersion)
        assertEquals(8, AppDatabase.MIGRATION_6_8.endVersion)
    }

    @Test
    fun testMigration6To7CreatesUsersTable() {
        val executedSql = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_6_7.migrate(fakeDb)

        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `users`") })
        assertTrue(executedSql.any { it.contains("index_users_email") })
    }

    @Test
    fun testMigration7To8CreatesSuppliersAndOrders() {
        val executedSql = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_7_8.migrate(fakeDb)

        assertTrue(executedSql.any { it.contains("ALTER TABLE `products` ADD COLUMN `supplierId`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `products` ADD COLUMN `reorderQty`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `stock_transactions` ADD COLUMN `orderId`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `stock_transactions` ADD COLUMN `unitCost`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `suppliers`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `purchase_orders`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `purchase_order_items`") })
        assertTrue(executedSql.any { it.contains("index_purchase_order_items_orderId") })
    }

    @Test
    fun testMigration6To8ExecutesAllChanges() {
        val executedSql = mutableListOf<String>()
        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        AppDatabase.MIGRATION_6_8.migrate(fakeDb)

        assertTrue(executedSql.any { it.contains("`users`") })
        assertTrue(executedSql.any { it.contains("`suppliers`") })
        assertTrue(executedSql.any { it.contains("`purchase_orders`") })
        assertTrue(executedSql.any { it.contains("`purchase_order_items`") })
    }
}

