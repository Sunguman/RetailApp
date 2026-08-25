package com.example.retail360.data

import android.util.Log
import com.example.retail360.model.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class FirebaseHelper {
    // If your database is not in the US, you MUST provide the URL here, for example:
    // private val database = FirebaseDatabase.getInstance("https://your-project-id-default-rtdb.europe-west1.firebasedatabase.app/")
    private val database = FirebaseDatabase.getInstance().apply {
        // Keep data synced locally even if app restarts
        setPersistenceEnabled(true)
    }
    
    private val db = database.reference

    suspend fun pushCustomer(customer: Customer) {
        try {
            db.child("customers").child(customer.id).setValue(customer).await()
            Log.d("FirebaseHelper", "Pushed customer: ${customer.id}")
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing customer: ${e.message}")
            throw e
        }
    }

    suspend fun pushVisit(visit: Visit) {
        try {
            db.child("visits").child(visit.id).setValue(visit).await()
            Log.d("FirebaseHelper", "Pushed visit: ${visit.id}")
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing visit: ${e.message}")
            throw e
        }
    }

    suspend fun pushAvailability(record: AvailabilityRecord) {
        try {
            db.child("availability").child(record.id).setValue(record).await()
            Log.d("FirebaseHelper", "Pushed availability: ${record.id}")
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing availability: ${e.message}")
            throw e
        }
    }

    suspend fun pushSale(sale: SaleItem) {
        try {
            db.child("sales").child(sale.id).setValue(sale).await()
            Log.d("FirebaseHelper", "Pushed sale: ${sale.id}")
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing sale: ${e.message}")
            throw e
        }
    }

    suspend fun pushRoutePlan(entry: RoutePlanEntry) {
        try {
            db.child("route_plans").child(entry.id).setValue(entry).await()
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing route plan: ${e.message}")
        }
    }

    suspend fun fetchRoutePlan(repId: String): List<RoutePlanEntry> {
        // Mock: In a real app, query by repId
        return emptyList()
    }

    suspend fun pushInventory(item: InventoryItem) {
        try {
            db.child("inventory").child(item.id).setValue(item).await()
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing inventory: ${e.message}")
        }
    }

    suspend fun fetchInventory(): List<InventoryItem> {
        return emptyList()
    }

    suspend fun pushProduct(product: Product) {
        try {
            db.child("products").child(product.id).setValue(product).await()
            Log.d("FirebaseHelper", "Pushed product: ${product.id}")
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error pushing product: ${e.message}")
            throw e
        }
    }

    suspend fun fetchProducts(): List<Product> {
        return try {
            val snapshot = db.child("products").get().await()
            snapshot.children.mapNotNull { it.getValue(Product::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseHelper", "Error fetching products: ${e.message}")
            emptyList()
        }
    }
}
