package com.example.inventoryapp

import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.canRequestSupply
import com.example.inventoryapp.data.local.entity.isDirectSlot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowStockSupplyRequestUnitTest {

    @Test
    fun testLowStockWithSupplierCanRequestSupply() {
        // Stock from 1 to 4 with an assigned supplier should show request button
        for (qty in 1..4) {
            val product = ProductEntity(
                sku = "SKU-$qty",
                name = "Pacific Salmon",
                category = "Seafood",
                quantity = qty,
                supplierId = "sup_pacific_seafood"
            )
            assertFalse("Product with supplierId should not be direct slot", product.isDirectSlot)
            assertTrue("Product with qty $qty and supplier should have canRequestSupply == true", product.canRequestSupply)
        }
    }

    @Test
    fun testDirectSlotDoesNotShowRequestButton() {
        // Direct slot (null, blank, or direct) should NEVER show request button, even when stock is 1..4
        val directNull = ProductEntity(
            sku = "SKU-DIR-1",
            name = "Direct Item",
            category = "Produce",
            quantity = 2,
            supplierId = null
        )
        val directBlank = ProductEntity(
            sku = "SKU-DIR-2",
            name = "Direct Item",
            category = "Produce",
            quantity = 3,
            supplierId = ""
        )
        val directExplicit = ProductEntity(
            sku = "SKU-DIR-3",
            name = "Direct Item",
            category = "Produce",
            quantity = 1,
            supplierId = "direct"
        )
        val directNone = ProductEntity(
            sku = "SKU-DIR-4",
            name = "Direct Item",
            category = "Produce",
            quantity = 4,
            supplierId = "none"
        )

        assertTrue(directNull.isDirectSlot)
        assertFalse("Direct slot with null supplierId must NOT show request button", directNull.canRequestSupply)

        assertTrue(directBlank.isDirectSlot)
        assertFalse("Direct slot with blank supplierId must NOT show request button", directBlank.canRequestSupply)

        assertTrue(directExplicit.isDirectSlot)
        assertFalse("Direct slot with 'direct' supplierId must NOT show request button", directExplicit.canRequestSupply)

        assertTrue(directNone.isDirectSlot)
        assertFalse("Direct slot with 'none' supplierId must NOT show request button", directNone.canRequestSupply)
    }

    @Test
    fun testOutOfRangeQuantitiesDoNotShowRequestButton() {
        // Stock 0 (out of stock) or >= 5 (healthy stock) should not show request button
        val outOfStock = ProductEntity(
            sku = "SKU-OUT",
            name = "OOS Salmon",
            category = "Seafood",
            quantity = 0,
            supplierId = "sup_pacific_seafood"
        )
        val fiveStock = ProductEntity(
            sku = "SKU-FIVE",
            name = "Salmon 5",
            category = "Seafood",
            quantity = 5,
            supplierId = "sup_pacific_seafood"
        )
        val abundantStock = ProductEntity(
            sku = "SKU-BIG",
            name = "Salmon 50",
            category = "Seafood",
            quantity = 50,
            supplierId = "sup_pacific_seafood"
        )

        assertFalse("Quantity 0 should not show request button", outOfStock.canRequestSupply)
        assertFalse("Quantity 5 should not show request button", fiveStock.canRequestSupply)
        assertFalse("Quantity 50 should not show request button", abundantStock.canRequestSupply)
    }
}
