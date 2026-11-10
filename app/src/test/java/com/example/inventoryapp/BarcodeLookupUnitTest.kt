package com.example.inventoryapp

import com.example.inventoryapp.data.remote.BarcodeLookupService
import com.example.inventoryapp.data.remote.OnlineProductData
import org.junit.Assert.*
import org.junit.Test

class BarcodeLookupUnitTest {

    @Test
    fun onlineProductData_hasExpectedDefaults() {
        val data = OnlineProductData(
            sku = "049000000443",
            name = "Coca-Cola Classic",
            brand = "The Coca-Cola Company",
            category = "Grocery",
            unit = "Can"
        )
        assertEquals("049000000443", data.sku)
        assertEquals("Coca-Cola Classic", data.name)
        assertEquals("The Coca-Cola Company", data.brand)
        assertEquals("Grocery", data.category)
        assertEquals("Can", data.unit)
        assertEquals(19.99, data.suggestedPrice, 0.01)
    }

    @Test
    fun barcodeSanitization_handlesWhitespaceAndSpecialCharacters() {
        val raw = "  049000000443 \r\n"
        val clean = raw.trim().filter { it.isDigit() || it.isLetter() }
        assertEquals("049000000443", clean)
    }

    @Test
    fun leadingZeroesComparison_matchesNumericEquivalents() {
        val scan = "049000000443"
        val inDb = "49000000443"
        val matches = scan.trimStart('0') == inDb.trimStart('0')
        assertTrue("Barcode with and without leading zero should match", matches)
    }
}
