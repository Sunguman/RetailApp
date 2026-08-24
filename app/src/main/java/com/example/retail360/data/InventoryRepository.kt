package com.example.retail360.data

import com.example.retail360.data.InventoryDao
import com.example.retail360.model.InventoryItem
import com.example.retail360.model.Product
import com.example.retail360.data.FirebaseHelper
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val dao: InventoryDao,
    private val firebase: FirebaseHelper
) {
    fun observeAll(): Flow<List<InventoryItem>> = dao.observeAll()

    fun totalValue(): Flow<Double> = dao.totalValue()

    /** Set the stock on hand for a product. Price is snapshotted from the catalog. */
    suspend fun setQuantity(product: Product, quantity: Int) {
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

    suspend fun refreshFromServer() {
        runCatching {
            firebase.fetchInventory().forEach { dao.upsert(it.copy(synced = true)) }
        }
    }
}
