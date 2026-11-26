package com.example.inventoryapp

import com.example.inventoryapp.util.AppCurrency
import com.example.inventoryapp.util.CurrencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyHelperUnitTest {

    @Test
    fun testDefaultExchangeRateIs4000() {
        assertEquals(4000.0, CurrencyFormatter.DEFAULT_EXCHANGE_RATE, 0.001)
    }

    @Test
    fun testAppCurrencyFromCode() {
        assertEquals(AppCurrency.USD, AppCurrency.fromCode("USD"))
        assertEquals(AppCurrency.USD, AppCurrency.fromCode("usd"))
        assertEquals(AppCurrency.KHR, AppCurrency.fromCode("KHR"))
        assertEquals(AppCurrency.KHR, AppCurrency.fromCode("khr"))
        assertEquals(AppCurrency.KHR, AppCurrency.fromCode("RIEL"))
        assertEquals(AppCurrency.KHR, AppCurrency.fromCode("riel"))
        assertEquals(AppCurrency.KHR, AppCurrency.fromCode("៛"))
        assertEquals(AppCurrency.USD, AppCurrency.fromCode(null))
        assertEquals(AppCurrency.USD, AppCurrency.fromCode("UNKNOWN"))
    }

    @Test
    fun testConversionUsdToKhr() {
        // Default rate 4000
        assertEquals(4000L, CurrencyFormatter.toKhr(1.0))
        assertEquals(10000L, CurrencyFormatter.toKhr(2.5))
        assertEquals(0L, CurrencyFormatter.toKhr(0.0))

        // Custom rate
        assertEquals(4050L, CurrencyFormatter.toKhr(1.0, 4050.0))
        assertEquals(4100L, CurrencyFormatter.toKhr(1.0, 4100.0))
    }

    @Test
    fun testConversionKhrToUsd() {
        assertEquals(1.0, CurrencyFormatter.toUsd(4000L), 0.001)
        assertEquals(2.5, CurrencyFormatter.toUsd(10000L), 0.001)
        assertEquals(1.0, CurrencyFormatter.toUsd(4050L, 4050.0), 0.001)
    }

    @Test
    fun testFormatUsdAndKhr() {
        assertEquals("$1.50", CurrencyFormatter.format(1.5, AppCurrency.USD))
        assertEquals("$0.00", CurrencyFormatter.format(0.0, AppCurrency.USD))
        assertEquals("4,000 ៛", CurrencyFormatter.format(1.0, AppCurrency.KHR, 4000.0))
        assertEquals("10,000 ៛", CurrencyFormatter.format(2.5, AppCurrency.KHR, 4000.0))
        assertEquals("4,050 ៛", CurrencyFormatter.format(1.0, AppCurrency.KHR, 4050.0))
    }

    @Test
    fun testFormatEquivalentOnly() {
        assertEquals("4,000 ៛", CurrencyFormatter.formatEquivalentOnly(1.0, AppCurrency.USD, 4000.0))
        assertEquals("$1.00", CurrencyFormatter.formatEquivalentOnly(1.0, AppCurrency.KHR, 4000.0))
    }
}
