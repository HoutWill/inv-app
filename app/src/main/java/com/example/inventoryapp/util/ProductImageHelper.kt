package com.example.inventoryapp.util

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import coil.compose.AsyncImage
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.ui.theme.NawanoPrimary200
import java.util.Locale

object ProductImageHelper {

    /**
     * Resolves an image model (URL, Uri, or DrawableRes ID) for any product.
     */
    fun getProductImageModel(
        sku: String?,
        name: String?,
        category: String? = null,
        imageUri: String? = null,
        supplier: String? = null
    ): Any? {
        val drawableRes = getDrawableForProduct(sku, name, category, supplier)
        if (drawableRes != null && (imageUri.isNullOrBlank() || imageUri.startsWith("android.resource://"))) {
            return drawableRes
        }
        if (!imageUri.isNullOrBlank()) {
            return imageUri
        }
        return drawableRes
    }

    fun getProductImageModel(product: ProductEntity): Any? {
        return getProductImageModel(
            sku = product.sku,
            name = product.name,
            category = product.category,
            imageUri = product.imageUri
        )
    }

    /**
     * Matches known SKUs, product names, categories, or supplier names to bundled high-res product photos.
     */
    @DrawableRes
    fun getDrawableForProduct(
        sku: String?,
        name: String?,
        category: String? = null,
        supplier: String? = null
    ): Int? {
        val s = sku?.trim()?.uppercase(Locale.ROOT) ?: ""
        val n = name?.trim()?.lowercase(Locale.ROOT) ?: ""
        val c = category?.trim()?.lowercase(Locale.ROOT) ?: ""
        val sup = supplier?.trim()?.lowercase(Locale.ROOT) ?: ""

        return when {
            // By SKU
            s == "CRB-0603" -> R.drawable.img_king_crab
            s == "SLM-0201" -> R.drawable.img_atlantic_salmon
            s == "LOB-0402" -> R.drawable.img_boston_lobster
            s == "WGY-0801" -> R.drawable.img_wagyu_ribeye
            s == "5449000000996" -> R.drawable.img_coca_cola
            s == "9002490100070" -> R.drawable.img_red_bull
            s == "3017620422003" -> R.drawable.img_nutella
            s == "8076809513753" -> R.drawable.img_barilla_pesto
            s == "8715700421639" || s == "0013000006408" || s == "5000157024671" -> R.drawable.img_heinz_ketchup
            s == "7622210449283" -> R.drawable.img_oreo
            s == "TOFU-0501" -> R.drawable.img_organic_tofu
            s == "BOX-1001" -> R.drawable.img_carton_box

            // By Name keyword
            n.contains("crab") -> R.drawable.img_king_crab
            n.contains("salmon") -> R.drawable.img_atlantic_salmon
            n.contains("lobster") -> R.drawable.img_boston_lobster
            n.contains("wagyu") || n.contains("ribeye") || (n.contains("beef") && !n.contains("pantry")) -> R.drawable.img_wagyu_ribeye
            n.contains("coca") || n.contains("coke") -> R.drawable.img_coca_cola
            n.contains("red bull") || (n.contains("energy") && n.contains("drink")) -> R.drawable.img_red_bull
            n.contains("nutella") || n.contains("hazelnut") -> R.drawable.img_nutella
            n.contains("pesto") || n.contains("barilla") -> R.drawable.img_barilla_pesto
            n.contains("ketchup") || n.contains("heinz") -> R.drawable.img_heinz_ketchup
            n.contains("oreo") || n.contains("cookie") -> R.drawable.img_oreo
            n.contains("tofu") || n.contains("soy") -> R.drawable.img_organic_tofu
            n.contains("carton") || n.contains("box") || n.contains("corrugated") || n.contains("packaging") -> R.drawable.img_carton_box

            // By Supplier or Inbound Title keyword
            n.contains("foodie") || sup.contains("foodie") -> R.drawable.img_nutella
            n.contains("pacific") || sup.contains("pacific") -> R.drawable.img_king_crab
            n.contains("tokyo") || sup.contains("tokyo") -> R.drawable.img_wagyu_ribeye
            n.contains("vegan") || sup.contains("vegan") || sup.contains("greenleaf") -> R.drawable.img_organic_tofu
            n.contains("apex") || sup.contains("apex") -> R.drawable.img_carton_box

            // Fallback by Category
            c.contains("seafood") -> R.drawable.img_atlantic_salmon
            c.contains("meat") -> R.drawable.img_wagyu_ribeye
            c.contains("beverage") -> R.drawable.img_coca_cola
            c.contains("pantry") -> R.drawable.img_nutella
            c.contains("snack") -> R.drawable.img_oreo
            c.contains("produce") -> R.drawable.img_organic_tofu
            c.contains("package") || c.contains("packaging") -> R.drawable.img_carton_box

            else -> null
        }
    }

    /**
     * Returns an Android resource URI string for a product if matched.
     */
    fun getAutoMatchedResourceUri(
        sku: String?,
        name: String?,
        category: String? = null,
        supplier: String? = null
    ): String? {
        val res = getDrawableForProduct(sku, name, category, supplier) ?: return null
        return "android.resource://com.example.inventoryapp/$res"
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category.lowercase(Locale.ROOT)) {
            "seafood", "fish" -> Icons.Default.Water
            "meats", "meat", "poultry" -> Icons.Default.Restaurant
            "produce", "vegetables", "fruits" -> Icons.Default.Eco
            "beverages", "dairy" -> Icons.Default.LocalDrink
            "electronics" -> Icons.Default.Devices
            "accessories" -> Icons.Default.Headphones
            "furniture" -> Icons.Default.Chair
            "hardware" -> Icons.Default.Build
            "grocery", "pantry", "snacks" -> Icons.Default.ShoppingBasket
            "packaging" -> Icons.Default.Inventory2
            else -> Icons.Default.Category
        }
    }

    /**
     * Extracts a clean, single-word product/brand name from any order title, PO manifest, or item description.
     */
    fun cleanOneWordTitle(rawTitle: String): String {
        var title = rawTitle.trim()
        if (title.contains("•")) {
            title = title.substringAfterLast("•").trim()
        }
        title = title.replace(Regex("^PO-\\d{4}-\\d+\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("^PO-\\d+\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("^(Inbound Request|Cold-Chain Restock|Gourmet Pantry Inbound|Warehouse Restock)\\s*[-•:]?\\s*", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("^Stock (In|Out):\\s*", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("\\s+(Restock|Inbound|Outbound|Request|Batch)$", RegexOption.IGNORE_CASE), "")

        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("coca") || lower.contains("coke") -> "Coca-Cola"
            lower.contains("red bull") || lower.contains("redbull") -> "RedBull"
            lower.contains("nutella") -> "Nutella"
            lower.contains("pesto") || lower.contains("barilla") -> "Barilla"
            lower.contains("ketchup") || lower.contains("heinz") -> "Ketchup"
            lower.contains("oreo") -> "Oreo"
            lower.contains("salmon") -> "Salmon"
            lower.contains("lobster") -> "Lobster"
            lower.contains("crab") -> "Crab"
            lower.contains("prawn") || lower.contains("shrimp") -> "Prawns"
            lower.contains("wagyu") || lower.contains("ribeye") || (lower.contains("beef") && !lower.contains("pantry")) -> "Wagyu"
            lower.contains("tofu") -> "Tofu"
            lower.contains("carton") || lower.contains("box") -> "Boxes"
            lower.contains("heineken") || lower.contains("beer") -> "Heineken"
            lower.contains("coffee") -> "Coffee"
            lower.contains("tea") -> "Tea"
            lower.contains("milk") -> "Milk"
            lower.contains("bread") -> "Bread"
            lower.contains("rice") -> "Rice"
            lower.contains("apple") -> "Apple"
            lower.contains("banana") -> "Banana"
            lower.contains("orange") -> "Orange"
            else -> {
                var firstItem = title.split(Regex("[&,]")).first().trim()
                firstItem = firstItem.replace(Regex("\\s*\\([^)]*\\)", RegexOption.IGNORE_CASE), "").trim()
                firstItem = firstItem.replace(Regex("\\s+(Cans?|Bottles?|Pack|Boxes|Pieces?|Bags?|Jar|Pcs)$", RegexOption.IGNORE_CASE), "").trim()
                val firstWord = firstItem.split(Regex("\\s+")).firstOrNull()?.trim() ?: "Item"
                firstWord.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "Item" }
            }
        }
    }

    /**
     * Extracts a concise single-word supplier name to prevent line overflows and ellipsis truncation.
     */
    fun cleanSupplierName(rawSupplier: String): String {
        val clean = rawSupplier.trim()
        val lower = clean.lowercase(Locale.ROOT)
        return when {
            lower.contains("pacific") -> "Pacific"
            lower.contains("foodie") -> "Foodie"
            lower.contains("tokyo") -> "Tokyo"
            lower.contains("greenleaf") || lower.contains("vegan") -> "GreenLeaf"
            lower.contains("apex") -> "Apex"
            lower.contains("nawano") || lower.contains("hq") || lower.contains("central") -> "HQ"
            else -> {
                val stripped = clean
                    .replace(Regex("\\s+(Supply|Supplier|Suppliers|Ltd|Inc|Co|Corporation|Logistics|Freight|Pte|LLC)$", RegexOption.IGNORE_CASE), "")
                    .trim()
                val firstWord = stripped.split(Regex("\\s+")).firstOrNull()?.trim() ?: "Supplier"
                firstWord.replace(Regex("[^a-zA-Z0-9]"), "").ifBlank { "Supplier" }
            }
        }
    }
}

/**
 * Unified, responsive ProductThumbnail Composable.
 * Automatically resolves bundled high-res images, remote URLs, or category icon fallbacks.
 */
@Composable
fun ProductThumbnail(
    product: ProductEntity,
    modifier: Modifier = Modifier.size(54.dp),
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
) {
    ProductThumbnail(
        sku = product.sku,
        name = product.name,
        category = product.category,
        imageUri = product.imageUri,
        modifier = modifier,
        shape = shape,
        contentScale = contentScale,
        borderColor = borderColor
    )
}

@Composable
fun ProductThumbnail(
    sku: String?,
    name: String?,
    category: String?,
    imageUri: String?,
    modifier: Modifier = Modifier.size(54.dp),
    supplier: String? = null,
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
) {
    val model = ProductImageHelper.getProductImageModel(sku, name, category, imageUri, supplier)
    val fallbackIcon = ProductImageHelper.getCategoryIcon(category ?: "")

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = name ?: "Product Image",
            contentScale = contentScale,
            placeholder = rememberVectorPainter(fallbackIcon),
            error = rememberVectorPainter(fallbackIcon),
            modifier = modifier
                .clip(shape)
                .border(1.dp, borderColor, shape)
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = category,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
