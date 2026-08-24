package com.example.retail360.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Domain models, doubling as Room entities. Every syncable record carries a
 * `synced` flag: writes land locally with synced=false, and SyncWorker flips
 * it to true after a successful push to Firebase.
 */

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "rep",
    val territory: String = ""
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: String = "Retailer",          // Retailer / Wholesaler / Kiosk
    val phone: String = "",
    val contactPerson: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geofenceRadiusM: Int = 150,          // allowed check-in distance
    val photoUrl: String = "",               // Cloudinary URL, or local uri before upload
    val createdBy: String = "",              // rep uid
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val category: String = "",
    val price: Double = 0.0,                  // KSh
    val imageUrl: String = ""
)

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val repId: String = "",
    val checkInTime: Long = System.currentTimeMillis(),
    val checkInLat: Double = 0.0,
    val checkInLng: Double = 0.0,
    val checkOutTime: Long? = null,
    val checkOutLat: Double? = null,
    val checkOutLng: Double? = null,
    val notes: String = "",
    val status: String = "ACTIVE",            // ACTIVE / COMPLETED
    val selfieUrl: String = "",               // check-in proof (Cloudinary)
    val synced: Boolean = false
)

@Entity(tableName = "availability")
data class AvailabilityRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val visitId: String = "",
    val productId: String = "",
    val productName: String = "",
    val inStock: Boolean = false,
    val facings: Int = 0,
    val shelfPhotoUrl: String = "",
    val synced: Boolean = false
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val visitId: String = "",
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,              // KSh
    val synced: Boolean = false
) {
    val lineTotal: Double get() = quantity * unitPrice
}

@Entity(tableName = "route_plan")
data class RoutePlanEntry(
    // Deterministic id ("$repId-$customerId-$dayOfWeek") so re-assigning upserts
    // instead of creating duplicate stops.
    @PrimaryKey val id: String = "",
    val repId: String = "",
    val customerId: String = "",
    val dayOfWeek: Int = 1,                    // Calendar.SUNDAY(1) .. Calendar.SATURDAY(7)
    val sequence: Int = 0,                     // order to visit within the day
    val synced: Boolean = false
)

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey val id: String = "",           // = productId (one row per product)
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,               // KSh, snapshot from the catalog
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    val value: Double get() = quantity * unitPrice
}



