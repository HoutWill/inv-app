package com.example.inventoryapp

import com.example.inventoryapp.data.local.entity.ProductEntity
import org.junit.Assert.*
import org.junit.Test

class ExactBarcodeScannerUnitTest {

    @Test
    fun skuMatching_prioritizesExactMatchOverTrimmedZeros() {
        val prodExact = ProductEntity(sku = "049000000443", name = "Diet Coke Exact", category = "Grocery")
        val prodWithoutZero = ProductEntity(sku = "49000000443", name = "Diet Coke Stripped", category = "Grocery")
        val products = listOf(prodWithoutZero, prodExact)

        val scannedSku = "049000000443"
        val cleanSku = scannedSku.trim()
        val cleanNoZeros = cleanSku.trimStart('0')

        // Exact match priority
        val exactMatch = products.find { it.sku.trim().equals(cleanSku, ignoreCase = true) }
        val matched = exactMatch ?: if (cleanNoZeros.isNotBlank()) {
            products.find { it.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true) }
        } else null

        assertNotNull("Matched product should not be null", matched)
        assertEquals("Exact match should take precedence over stripped equivalent", "049000000443", matched?.sku)
        assertEquals("Diet Coke Exact", matched?.name)
    }

    @Test
    fun skuMatching_fallsBackToFlexibleMatchWhenExactNotFound() {
        val prodWithoutZero = ProductEntity(sku = "49000000443", name = "Diet Coke Stripped", category = "Grocery")
        val products = listOf(prodWithoutZero)

        val scannedSku = "049000000443"
        val cleanSku = scannedSku.trim()
        val cleanNoZeros = cleanSku.trimStart('0')

        val exactMatch = products.find { it.sku.trim().equals(cleanSku, ignoreCase = true) }
        val matched = exactMatch ?: if (cleanNoZeros.isNotBlank()) {
            products.find { it.sku.trim().trimStart('0').equals(cleanNoZeros, ignoreCase = true) }
        } else null

        assertNotNull("Matched product should not be null", matched)
        assertEquals("49000000443", matched?.sku)
    }

    @Test
    fun exactUpcaNumberResolution_handlesPromotedLeadingZero() {
        fun resolveCandidate(format: Int, raw: String?, display: String?): String? {
            return when (format) {
                // Barcode.FORMAT_UPC_A
                512 -> {
                    if (raw != null && raw.length == 13 && raw.startsWith("0")) {
                        raw.substring(1)
                    } else if (display != null && display.length == 13 && display.startsWith("0")) {
                        display.substring(1)
                    } else {
                        display?.ifBlank { null } ?: raw
                    }
                }
                // Barcode.FORMAT_UPC_E
                1024 -> {
                    if (!display.isNullOrBlank() && (display.length == 8 || display.length == 6)) {
                        display
                    } else {
                        display?.ifBlank { null } ?: raw
                    }
                }
                else -> display?.ifBlank { null } ?: raw
            }
        }

        // Test UPC-A 12-digit exact preservation
        val upcAExact = resolveCandidate(512, "049000000443", "049000000443")
        assertEquals("049000000443", upcAExact)

        // Test UPC-A with promoted leading zero stripped to match 12-digit physical barcode
        val upcAPromoted = resolveCandidate(512, "004900000443", "049000000443")
        assertEquals("049000000443", upcAPromoted)

        // Test UPC-E exact 8 digits preserved rather than inflated 12 digits
        val upcEExact = resolveCandidate(1024, "012345000065", "01234565")
        assertEquals("01234565", upcEExact)
    }
}
