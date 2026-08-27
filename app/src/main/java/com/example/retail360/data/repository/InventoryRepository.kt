package com.example.retail360.data.repository

import com.example.retail360.data.local.InventoryDao
import com.example.retail360.data.model.InventoryItem
import com.example.retail360.data.model.Product
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InventoryRepository(
    private val dao: InventoryDao,
    private val firebase: FirebaseService
) {
    fun observeAll(): Flow<List<InventoryItem>> = dao.observeAll()

    fun totalValue(): Flow<Double> = dao.totalValue()

    /** Set the stock on hand for a product. Price is snapshotted from the catalog. */
    suspend fun setQuantity(product: Product, quantity: Int) = withContext(Dispatchers.IO) {
        val item = InventoryItem(
            id = product.id,
            productId = product.id,
            productName = product.name,
            quantity = quantity,
            unitPrice = product.price,
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        dao.upsert(item)
        runCatching { firebase.pushInventory(item); dao.upsert(item.copy(synced = true)) }
    }

    suspend fun refreshFromServer() = withContext(Dispatchers.IO) {
        runCatching {
            firebase.fetchInventory().forEach { dao.upsert(it.copy(synced = true)) }
        }
    }
}
