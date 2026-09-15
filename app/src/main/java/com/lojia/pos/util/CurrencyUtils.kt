package com.lojia.pos.util

import java.util.Locale

/**
 * Centralized Currency Utilities.
 * Formats money and resolves currency display symbol across all screens.
 */
object CurrencyUtils {
    /**
     * Formats an amount with the provided currency code or symbol.
     */
    fun formatMoney(amount: Double, currency: String): String {
        return String.format(Locale.US, "%.2f %s", amount, currency.trim())
    }

    /**
     * Normalizes currency string. If empty, defaults to "SAR".
     */
    fun resolveCurrency(businessCurrency: String?, defaultCurrency: String = "SAR"): String {
        val trimmed = businessCurrency?.trim().orEmpty()
        return if (trimmed.isNotEmpty()) trimmed else defaultCurrency
    }
}
