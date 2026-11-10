package com.example.inventoryapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class OnlineProductData(
    val sku: String,
    val name: String,
    val brand: String = "",
    val category: String = "Other",
    val subcategory: String = "",
    val unit: String = "Pcs",
    val imageUrl: String? = null,
    val suggestedPrice: Double = 19.99
)

object BarcodeLookupService {
    private const val TAG = "BarcodeLookupService"

    /**
     * Look up product details online across global open barcode databases
     * (Open Food Facts, Open Product Facts, and Open Library for ISBNs).
     */
    suspend fun lookupBarcode(rawBarcode: String): OnlineProductData? = withContext(Dispatchers.IO) {
        val cleanCode = rawBarcode.trim().filter { it.isDigit() || it.isLetter() }
        if (cleanCode.length < 4) return@withContext null

        Log.d(TAG, "Looking up barcode online: $cleanCode")

        // 1. Check for Book ISBN (starts with 978 or 979)
        if (cleanCode.startsWith("978") || cleanCode.startsWith("979")) {
            val bookResult = queryOpenLibrary(cleanCode)
            if (bookResult != null) return@withContext bookResult
        }

        // 2. Query Open Food Facts (over 3 million global products)
        val foodResult = queryOpenFoodFacts(cleanCode)
        if (foodResult != null) return@withContext foodResult

        // 3. Query Open Products Facts (electronics, cosmetics, household items)
        val generalResult = queryOpenProductsFacts(cleanCode)
        if (generalResult != null) return@withContext generalResult

        Log.d(TAG, "Barcode not found in online databases: $cleanCode")
        return@withContext null
    }

    private fun queryOpenFoodFacts(barcode: String): OnlineProductData? {
        val urlString = "https://world.openfoodfacts.org/api/v2/product/$barcode.json"
        return fetchAndParseJson(urlString) { json ->
            val status = json.optInt("status", 0)
            if (status != 1) return@fetchAndParseJson null

            val product = json.optJSONObject("product") ?: return@fetchAndParseJson null

            val name = product.optString("product_name").ifBlank {
                product.optString("product_name_en").ifBlank {
                    product.optString("generic_name")
                }
            }
            if (name.isBlank()) return@fetchAndParseJson null

            val brand = product.optString("brands").ifBlank {
                product.optString("brand_owner")
            }

            val categoriesRaw = product.optString("categories", "")
            val category = mapCategory(categoriesRaw, name)

            val quantityRaw = product.optString("quantity", "")
            val unit = determineUnit(quantityRaw)

            val imageUrl = product.optString("image_front_url").ifBlank {
                product.optString("image_url")
            }.takeIf { it.isNotBlank() }

            OnlineProductData(
                sku = barcode,
                name = cleanProductName(name),
                brand = brand.split(",").firstOrNull()?.trim() ?: brand,
                category = category,
                unit = unit,
                imageUrl = imageUrl,
                suggestedPrice = 9.99
            )
        }
    }

    private fun queryOpenProductsFacts(barcode: String): OnlineProductData? {
        val urlString = "https://world.openproductsfacts.org/api/v0/product/$barcode.json"
        return fetchAndParseJson(urlString) { json ->
            val status = json.optInt("status", 0)
            if (status != 1) return@fetchAndParseJson null

            val product = json.optJSONObject("product") ?: return@fetchAndParseJson null

            val name = product.optString("product_name").ifBlank {
                product.optString("generic_name")
            }
            if (name.isBlank()) return@fetchAndParseJson null

            val brand = product.optString("brands")
            val categoriesRaw = product.optString("categories", "")
            val category = mapCategory(categoriesRaw, name)

            val imageUrl = product.optString("image_front_url").takeIf { it.isNotBlank() }

            OnlineProductData(
                sku = barcode,
                name = cleanProductName(name),
                brand = brand.split(",").firstOrNull()?.trim() ?: brand,
                category = category,
                unit = "Pcs",
                imageUrl = imageUrl,
                suggestedPrice = 24.99
            )
        }
    }

    private fun queryOpenLibrary(isbn: String): OnlineProductData? {
        val urlString = "https://openlibrary.org/api/books?bibkeys=ISBN:$isbn&format=json&jscmd=data"
        return fetchAndParseJson(urlString) { json ->
            val bookKey = "ISBN:$isbn"
            val book = json.optJSONObject(bookKey) ?: return@fetchAndParseJson null

            val title = book.optString("title")
            if (title.isBlank()) return@fetchAndParseJson null

            val authorsArray = book.optJSONArray("authors")
            val author = if (authorsArray != null && authorsArray.length() > 0) {
                authorsArray.getJSONObject(0).optString("name")
            } else ""

            val coverObj = book.optJSONObject("cover")
            val coverUrl = coverObj?.optString("medium") ?: coverObj?.optString("large")

            OnlineProductData(
                sku = isbn,
                name = title,
                brand = author.ifBlank { "Publisher" },
                category = "Other",
                unit = "Pcs",
                imageUrl = coverUrl,
                suggestedPrice = 19.99
            )
        }
    }

    private inline fun fetchAndParseJson(
        urlString: String,
        parser: (JSONObject) -> OnlineProductData?
    ): OnlineProductData? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "NawanoInventoryApp/1.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                val jsonObject = JSONObject(response)
                parser(jsonObject)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error fetching barcode info from $urlString: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun cleanProductName(name: String): String {
        return name.replace(Regex("\\s+"), " ").trim()
    }

    private fun mapCategory(rawCategories: String, name: String): String {
        val combined = "$rawCategories $name".lowercase(Locale.ROOT)
        return when {
            combined.contains("drink") || combined.contains("beverage") || combined.contains("food") ||
                    combined.contains("snack") || combined.contains("biscuit") || combined.contains("juice") ||
                    combined.contains("sauce") || combined.contains("water") || combined.contains("coffee") ||
                    combined.contains("tea") || combined.contains("chocolate") || combined.contains("grocery") -> "Grocery"

            combined.contains("electronic") || combined.contains("computer") || combined.contains("phone") ||
                    combined.contains("cable") || combined.contains("charger") || combined.contains("audio") ||
                    combined.contains("usb") || combined.contains("battery") || combined.contains("screen") -> "Electronics"

            combined.contains("mouse") || combined.contains("case") || combined.contains("strap") ||
                    combined.contains("cover") || combined.contains("bag") || combined.contains("holder") ||
                    combined.contains("accessory") || combined.contains("stand") -> "Accessories"

            combined.contains("tool") || combined.contains("hardware") || combined.contains("screw") ||
                    combined.contains("drill") || combined.contains("pipe") || combined.contains("metal") -> "Hardware"

            combined.contains("chair") || combined.contains("desk") || combined.contains("table") ||
                    combined.contains("furniture") || combined.contains("couch") || combined.contains("shelf") -> "Furniture"

            else -> "Other"
        }
    }

    private fun determineUnit(quantityStr: String): String {
        val lower = quantityStr.lowercase(Locale.ROOT)
        return when {
            lower.contains("kg") || lower.contains("kilo") -> "Kg"
            lower.contains("liter") || lower.contains("litre") || lower.contains("l") -> "Liter"
            lower.contains("box") || lower.contains("pack") -> "Box"
            lower.contains("carton") -> "Carton"
            lower.contains("set") -> "Set"
            else -> "Pcs"
        }
    }
}
