package com.example.retail360.data.repository

import com.example.retail360.data.local.ProductDao
import com.example.retail360.data.model.Product
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProductRepository(
    private val dao: ProductDao,
    private val firebase: FirebaseService
) {
    fun observeAll(): Flow<List<Product>> = dao.observeAll()

    val products: Flow<List<Product>> = dao.observeAll()

    suspend fun refreshCatalog() = withContext(Dispatchers.IO) {
        runCatching {
            val products = firebase.fetchProducts()
            if (products.isNotEmpty()) dao.upsertAll(products)
        }
    }

    suspend fun save(product: Product) = withContext(Dispatchers.IO) {
        dao.upsert(product)
        runCatching { firebase.pushProduct(product) }
    }

    suspend fun getByBarcode(barcode: String): Product? = withContext(Dispatchers.IO) {
        dao.byBarcode(barcode)
    }
}
