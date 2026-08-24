package com.example.retail360.data

import com.example.retail360.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val dao: ProductDao,
    private val firebase: FirebaseHelper
) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()

    val products: Flow<List<Product>> = dao.observeAll()

    suspend fun refreshCatalog() {
        // Mock: In a real app, this would fetch from Firebase/API
    }

    suspend fun getByBarcode(barcode: String): Product? = dao.byBarcode(barcode)
}
