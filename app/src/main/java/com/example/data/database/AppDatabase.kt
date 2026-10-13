package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ServiceOrderEntity::class,
        ServiceOrderItemEntity::class,
        StockMovementEntity::class,
        ExpenseEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun serviceOrderDao(): ServiceOrderDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun supplierDao(): SupplierDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ecell_gestao.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
