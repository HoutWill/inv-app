package com.example.inventoryapp.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object StockNotificationHelper {
    const val CHANNEL_ID = "nawano_stock_alerts"
    private const val CHANNEL_NAME = "Stock & Inventory Alerts"
    private const val CHANNEL_DESC = "Notifications for low stock levels and inventory updates"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyLowStock(
        context: Context,
        productName: String,
        sku: String,
        remainingQty: Int,
        reorderLevel: Int
    ) {
        try {
            initNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w("StockNotification", "POST_NOTIFICATIONS permission not granted.")
                    return
                }
            }

            val title = if (remainingQty <= 0) {
                "Out of Stock: $productName"
            } else {
                "Low Stock Alert: $productName"
            }

            val body = if (remainingQty <= 0) {
                "Item ($sku) is completely out of stock. Immediate reorder required!"
            } else {
                "Only $remainingQty units remaining in stock (Reorder threshold: $reorderLevel)."
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val notificationId = (sku.hashCode() and 0x7FFFFFFF)
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e("StockNotification", "Failed to show low stock notification", e)
        }
    }

    fun notifyStockEvent(
        context: Context,
        title: String,
        message: String
    ) {
        try {
            initNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e("StockNotification", "Failed to show stock event notification", e)
        }
    }
}
