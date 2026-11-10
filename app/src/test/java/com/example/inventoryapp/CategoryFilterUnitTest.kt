package com.example.inventoryapp

import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.dashboard.CategoryHierarchy
import org.junit.Assert.*
import org.junit.Test

class CategoryFilterUnitTest {

    @Test
    fun categoryHierarchy_includesOtherWithSubcategories() {
        val subcategories = CategoryHierarchy.getSubcategories("Other")
        assertTrue("Subcategories for Other should not be empty", subcategories.isNotEmpty())
        assertTrue("Subcategories should contain General", subcategories.contains("General"))
        assertTrue("Subcategories should contain Supplies", subcategories.contains("Supplies"))
        assertTrue("Subcategories should contain Misc", subcategories.contains("Misc"))
    }

    @Test
    fun categoryFilter_other_matchesOtherAndUncategorizedItems() {
        val electronicsItem = ProductEntity(sku = "SKU-1", name = "Headphones", category = "Electronics")
        val otherItem = ProductEntity(sku = "SKU-2", name = "Notebook", category = "Other")
        val blankCatItem = ProductEntity(sku = "SKU-3", name = "Mystery Item", category = "")
        val customCatItem = ProductEntity(sku = "SKU-4", name = "Novelty Badge", category = "UnlistedCustom")

        val products = listOf(electronicsItem, otherItem, blankCatItem, customCatItem)

        fun matchesCategory(category: String, item: ProductEntity): Boolean {
            return when (category) {
                "All" -> true
                "Low Stock" -> item.quantity <= item.reorderLevel
                "Other" -> item.category.equals("Other", ignoreCase = true) ||
                        item.category.isBlank() ||
                        !CategoryHierarchy.HIERARCHY.keys.any { it != "Other" && it.equals(item.category, ignoreCase = true) }
                else -> item.category.equals(category, ignoreCase = true)
            }
        }

        val filteredOther = products.filter { matchesCategory("Other", it) }
        assertEquals(3, filteredOther.size)
        assertTrue(filteredOther.contains(otherItem))
        assertTrue(filteredOther.contains(blankCatItem))
        assertTrue(filteredOther.contains(customCatItem))
        assertFalse(filteredOther.contains(electronicsItem))

        val filteredElectronics = products.filter { matchesCategory("Electronics", it) }
        assertEquals(1, filteredElectronics.size)
        assertTrue(filteredElectronics.contains(electronicsItem))
    }
}
