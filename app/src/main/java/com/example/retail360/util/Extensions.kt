package com.example.retail360.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.Locale

private val kshFormatter = NumberFormat.getCurrencyInstance(Locale("en", "KE")).apply {
    currency = java.util.Currency.getInstance("KES")
}

/**
 * A safe way to collect StateFlow in Compose, ensuring we always have the
 * initial value and correct lifecycle awareness.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateSafe(): State<T> {
    return this.collectAsState()
}

/**
 * Formats a double as Kenyan Shillings (KSh).
 * Uses a cached formatter to avoid heavy object allocation in loops.
 */
fun Double.ksh(): String {
    return try {
        synchronized(kshFormatter) {
            kshFormatter.format(this).replace("KES", "KSh")
        }
    } catch (e: Exception) {
        "KSh %,.2f".format(this)
    }
}

fun Int.ksh(): String = this.toDouble().ksh()
