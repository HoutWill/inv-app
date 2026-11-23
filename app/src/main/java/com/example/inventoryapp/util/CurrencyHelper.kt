package com.example.inventoryapp.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Supported currencies in the application.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val shortLabel: String
) {
    USD("USD", "$", "USD ($)", "USD"),
    KHR("KHR", "៛", "Riel (៛)", "Riel");

    companion object {
        fun fromCode(code: String?): AppCurrency {
            return when (code?.trim()?.uppercase()) {
                "KHR", "RIEL", "៛" -> KHR
                else -> USD
            }
        }
    }
}

/**
 * Formatting and conversion utility for dual-currency support (USD and Cambodian Riel KHR).
 */
object CurrencyFormatter {
    const val DEFAULT_EXCHANGE_RATE = 4000.0

    fun formatUsd(amountUsd: Double): String {
        return "$${String.format(Locale.US, "%.2f", amountUsd)}"
    }

    fun formatUsdComma(amountUsd: Double): String {
        return "$${String.format(Locale.US, "%,.2f", amountUsd)}"
    }

    fun formatKhr(amountKhr: Long): String {
        return "${NumberFormat.getNumberInstance(Locale.US).format(amountKhr)} ៛"
    }

    fun toKhr(amountUsd: Double, exchangeRate: Double = DEFAULT_EXCHANGE_RATE): Long {
        val rate = if (exchangeRate > 0) exchangeRate else DEFAULT_EXCHANGE_RATE
        return (amountUsd * rate).toLong()
    }

    fun toUsd(amountKhr: Long, exchangeRate: Double = DEFAULT_EXCHANGE_RATE): Double {
        val rate = if (exchangeRate > 0) exchangeRate else DEFAULT_EXCHANGE_RATE
        return amountKhr / rate
    }

    fun format(
        amountUsd: Double,
        currency: AppCurrency,
        exchangeRate: Double = DEFAULT_EXCHANGE_RATE
    ): String {
        return when (currency) {
            AppCurrency.USD -> formatUsd(amountUsd)
            AppCurrency.KHR -> formatKhr(toKhr(amountUsd, exchangeRate))
        }
    }

    fun formatWithEquivalent(
        amountUsd: Double,
        currency: AppCurrency,
        exchangeRate: Double = DEFAULT_EXCHANGE_RATE
    ): String {
        return when (currency) {
            AppCurrency.USD -> {
                val khr = toKhr(amountUsd, exchangeRate)
                "${formatUsd(amountUsd)} (≈ ${formatKhr(khr)})"
            }
            AppCurrency.KHR -> {
                val khr = toKhr(amountUsd, exchangeRate)
                "${formatKhr(khr)} (≈ ${formatUsd(amountUsd)})"
            }
        }
    }

    fun formatEquivalentOnly(
        amountUsd: Double,
        currency: AppCurrency,
        exchangeRate: Double = DEFAULT_EXCHANGE_RATE
    ): String {
        return when (currency) {
            AppCurrency.USD -> formatKhr(toKhr(amountUsd, exchangeRate))
            AppCurrency.KHR -> formatUsd(amountUsd)
        }
    }
}
