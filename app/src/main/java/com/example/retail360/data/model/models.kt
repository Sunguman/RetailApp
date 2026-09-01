package com.example.retail360.data.model

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
    // Deterministic id ("$visitId-$productId") so re-assessing a SKU updates in place.
    @PrimaryKey val id: String = "",
    val visitId: String = "",
    val customerId: String = "",
    val outletName: String = "",
    val repId: String = "",
    val category: String = "",
    val productId: String = "",
    val productName: String = "",
    val status: String = "",              // AVAILABLE / INSUFFICIENT / NOT_AVAILABLE
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
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

@Entity(tableName = "competitor_activity")
data class CompetitorActivity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val outletName: String = "",
    val repId: String = "",
    val competitor: String = "",
    val brand: String = "",
    val productSku: String = "",
    val activityType: String = "",
    val otherActivity: String = "",           // when activityType == Other
    val beforePrice: Double = 0.0,
    val afterPrice: Double = 0.0,
    val stockStatus: String = "",             // IN_STOCK / OUT_OF_STOCK / UNKNOWN
    val estimatedQty: Int = 0,
    val startDate: Long = 0L,
    val endDate: Long = 0L,                    // 0 = none
    val ongoing: Boolean = false,
    val displayType: String = "",
    val otherDisplay: String = "",            // when displayType == Other
    val notes: String = "",
    val photoUrl: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    val discountAmount: Double get() = (beforePrice - afterPrice).coerceAtLeast(0.0)
    val discountDepth: Double get() = if (beforePrice > 0) (beforePrice - afterPrice) / beforePrice * 100 else 0.0
}

@Entity(tableName = "payment_collection")
data class PaymentCollection(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val repId: String = "",
    val amount: Double = 0.0,
    val method: String = "",                  // Cash / M-Pesa / Cheque / Bank
    val reference: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "product_update")
data class ProductUpdate(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val repId: String = "",
    val productName: String = "",
    val updateType: String = "",              // New listing / Delisted / Price change / Repackaged
    val detail: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "share_of_shelf")
data class ShareOfShelf(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val repId: String = "",
    val category: String = "",
    val ourFacings: Int = 0,
    val totalFacings: Int = 0,
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    val sosPercent: Int get() = if (totalFacings <= 0) 0 else (ourFacings * 100 / totalFacings)
}

@Entity(tableName = "visit_photo")
data class VisitPhoto(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val repId: String = "",
    val category: String = "",                // Storefront / Shelf / Display / POSM / Other
    val caption: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)

@Entity(tableName = "stock_movement")
data class StockMovement(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val visitId: String = "",
    val customerId: String = "",
    val repId: String = "",
    val stockPoint: String = "",
    val category: String = "",
    val productId: String = "",
    val productName: String = "",
    val unit: String = "",
    val movementType: String = "",            // REQUISITION / UPLIFT / RETURN
    val status: String = "",                  // PENDING / APPROVED / REJECTED / CONFIRMED
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    val total: Double get() = quantity * unitPrice
}


