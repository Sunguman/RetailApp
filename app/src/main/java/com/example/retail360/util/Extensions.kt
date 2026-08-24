package com.example.retail360.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.Locale

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
 */
fun Double.ksh(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "KE"))
    return format.format(this).replace("KES", "KSh")
}

fun Int.ksh(): String = this.toDouble().ksh()
