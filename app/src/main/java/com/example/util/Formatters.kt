package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", ptBrLocale)
    private val dayMonthFormat = SimpleDateFormat("dd/MM", ptBrLocale)

    fun formatCurrency(value: Double): String {
        return currencyFormat.format(value)
    }

    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0) return "--"
        return dateFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long): String {
        if (epochMillis <= 0) return "--"
        return dateTimeFormat.format(Date(epochMillis))
    }

    fun formatDayMonth(epochMillis: Long): String {
        if (epochMillis <= 0) return "--"
        return dayMonthFormat.format(Date(epochMillis))
    }

    fun calculateMargin(cost: Double, price: Double): Double {
        if (price <= 0.0) return 0.0
        return ((price - cost) / price) * 100.0
    }

    fun calculateUnitProfit(cost: Double, price: Double): Double {
        return price - cost
    }

    fun cleanPhone(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    fun formatPhone(phone: String): String {
        val digits = cleanPhone(phone)
        return when {
            digits.length == 11 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
            digits.length == 10 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
            else -> phone
        }
    }
}
