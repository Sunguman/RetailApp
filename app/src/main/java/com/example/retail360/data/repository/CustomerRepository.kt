package com.example.retail360.data.repository

import com.example.retail360.data.local.CustomerDao
import com.example.retail360.data.model.Customer
import com.example.retail360.data.remote.CloudinaryService
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val dao: CustomerDao,
    private val firebase: FirebaseService,
    private val cloudinary: CloudinaryService
) {
    fun observeAll(): Flow<List<Customer>> = dao.observeAll()

    val customers: Flow<List<Customer>> = dao.observeAll()

    suspend fun byId(id: String): Customer? = dao.byId(id)

    suspend fun save(customer: Customer, photoUri: android.net.Uri? = null) = withContext(Dispatchers.IO) {
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
