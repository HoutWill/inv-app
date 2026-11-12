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
        imageUri: String? = null
    ): Any? {
        if (!imageUri.isNullOrBlank()) {
            return imageUri
        }
        val drawableRes = getDrawableForProduct(sku, name, category)
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
     * Matches known SKUs and keywords to bundled high-res product photos.
     */
    @DrawableRes
    fun getDrawableForProduct(sku: String?, name: String?, category: String? = null): Int? {
        val s = sku?.trim()?.uppercase(Locale.ROOT) ?: ""
        val n = name?.trim()?.lowercase(Locale.ROOT) ?: ""
        val c = category?.trim()?.lowercase(Locale.ROOT) ?: ""

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
    fun getAutoMatchedResourceUri(sku: String?, name: String?, category: String? = null): String? {
        val res = getDrawableForProduct(sku, name, category) ?: return null
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
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
) {
    val model = ProductImageHelper.getProductImageModel(sku, name, category, imageUri)

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = name ?: "Product Image",
            contentScale = contentScale,
            modifier = modifier
                .clip(shape)
                .border(1.dp, borderColor, shape)
        )
    } else {
        val icon = ProductImageHelper.getCategoryIcon(category ?: "")
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = category,
                    tint = NawanoPrimary200,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
