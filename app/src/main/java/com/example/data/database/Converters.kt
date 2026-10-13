package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.ProductCategory
import com.example.data.model.SaleStatus
import com.example.data.model.ServiceOrderStatus
import com.example.data.model.StockMovementType
import com.example.data.model.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = try {
        UserRole.valueOf(value)
    } catch (_: Exception) {
        UserRole.VENDEDOR
    }

    @TypeConverter
    fun fromProductCategory(value: ProductCategory): String = value.name

    @TypeConverter
    fun toProductCategory(value: String): ProductCategory = try {
        ProductCategory.valueOf(value)
    } catch (_: Exception) {
        ProductCategory.OUTROS
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (_: Exception) {
        PaymentMethod.DINHEIRO
    }

    @TypeConverter
    fun fromSaleStatus(value: SaleStatus): String = value.name

    @TypeConverter
    fun toSaleStatus(value: String): SaleStatus = try {
        SaleStatus.valueOf(value)
    } catch (_: Exception) {
        SaleStatus.CONFIRMADA
    }

    @TypeConverter
    fun fromServiceOrderStatus(value: ServiceOrderStatus): String = value.name

    @TypeConverter
    fun toServiceOrderStatus(value: String): ServiceOrderStatus = try {
        ServiceOrderStatus.valueOf(value)
    } catch (_: Exception) {
        ServiceOrderStatus.RECEBIDO
    }

    @TypeConverter
    fun fromStockMovementType(value: StockMovementType): String = value.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType = try {
        StockMovementType.valueOf(value)
    } catch (_: Exception) {
        StockMovementType.AJUSTE_INVENTARIO
    }

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = try {
        ExpenseCategory.valueOf(value)
    } catch (_: Exception) {
        ExpenseCategory.OUTRAS
    }

    @TypeConverter
    fun fromExpenseStatus(value: ExpenseStatus): String = value.name

    @TypeConverter
    fun toExpenseStatus(value: String): ExpenseStatus = try {
        ExpenseStatus.valueOf(value)
    } catch (_: Exception) {
        ExpenseStatus.PAGO
    }
}
