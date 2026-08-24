package com.example.retail360.data

import com.example.retail360.model.Product
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {
    val products: Flow<List<Product>> = dao.all()

    suspend fun refreshCatalog() {
        // Mock: In a real app, this would fetch from Firebase/API
    }

    suspend fun getByBarcode(barcode: String): Product? = dao.getByBarcode(barcode)
}
