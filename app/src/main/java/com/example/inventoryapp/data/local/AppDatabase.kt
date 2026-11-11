package com.example.inventoryapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inventoryapp.data.local.dao.ProductDao
import com.example.inventoryapp.data.local.dao.StockTransactionDao
import com.example.inventoryapp.data.local.dao.UserDao
import com.example.inventoryapp.data.local.entity.ProductEntity
import com.example.inventoryapp.data.local.entity.StockTransactionEntity
import com.example.inventoryapp.data.local.entity.UserEntity

@Database(
    entities = [ProductEntity::class, StockTransactionEntity::class, UserEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockTransactionDao(): StockTransactionDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "inventory_database"
                    )
                        .fallbackToDestructiveMigration(true)
                        .fallbackToDestructiveMigrationOnDowngrade(true)
                        .build()
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error building database, falling back to in-memory", e)
                    Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    )
                        .fallbackToDestructiveMigration(true)
                        .build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
