package com.example.retail360.data

import com.example.retail360.model.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val dao: CustomerDao) {
    val customers: Flow<List<Customer>> = dao.all()

    suspend fun getById(id: String): Customer? = dao.getById(id)

    suspend fun save(customer: Customer, photoUri: android.net.Uri? = null) {
        // Mock photo upload logic
        val finalCustomer = if (photoUri != null) {
            customer.copy(photoUrl = photoUri.toString())
        } else customer
        dao.upsert(finalCustomer)
    }

    suspend fun refreshFromServer() {
        // Mock refresh
    }
}
