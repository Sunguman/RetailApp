package com.example.retail360.data

import com.example.retail360.model.Customer
import com.example.retail360.model.CloudinaryService
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val dao: CustomerDao,
    private val firebase: FirebaseHelper,
    private val cloudinary: CloudinaryService
) {
    fun observeAll(): Flow<List<Customer>> = dao.observeAll()

    val customers: Flow<List<Customer>> = dao.observeAll()

    suspend fun byId(id: String): Customer? = dao.byId(id)

    suspend fun save(customer: Customer, photoUri: android.net.Uri? = null) {
        val finalCustomer = if (photoUri != null) {
            val uploadedUrl = cloudinary.upload(photoUri, "customers")
            customer.copy(photoUrl = uploadedUrl ?: photoUri.toString(), synced = false)
        } else customer.copy(synced = false)
        
        dao.upsert(finalCustomer)
        runCatching { 
            firebase.pushCustomer(finalCustomer)
            dao.update(finalCustomer.copy(synced = true))
        }
    }

    suspend fun refreshFromServer() {
        // Mock refresh
    }
}
