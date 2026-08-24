package com.example.retail360.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppNotification(
    val id: Long = System.nanoTime(),
    val title: String,
    val body: String,
    val time: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val key: String? = null
)

/**
 * App-wide, in-memory notification feed. Any screen or worker can post to it,
 * and the dashboard bell observes [items]. Not persisted — a process restart
 * clears it; back it with Room/DataStore later if notifications must survive.
 */
object NotificationCenter {
    private const val MAX = 50

    private val _items = MutableStateFlow<List<AppNotification>>(emptyList())
    val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    fun post(title: String, body: String) {
        _items.update { (listOf(AppNotification(title = title, body = body)) + it).take(MAX) }
    }

    /** Replaces any existing notification carrying the same [key], so repeated
     *  status updates (e.g. "pending sync") refresh in place instead of piling up. */
    fun postUnique(key: String, title: String, body: String) {
        _items.update { current ->
            val kept = current.filterNot { it.key == key }
            (listOf(AppNotification(title = title, body = body, key = key)) + kept).take(MAX)
        }
    }

    fun markAllRead() {
        _items.update { list -> list.map { if (it.read) it else it.copy(read = true) } }
    }

    fun clear() { _items.value = emptyList() }
}
