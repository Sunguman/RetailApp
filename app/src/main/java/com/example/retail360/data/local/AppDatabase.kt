package com.example.retail360.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.data.model.PaymentCollection
import com.example.retail360.data.model.ProductUpdate
import com.example.retail360.data.model.ShareOfShelf
import com.example.retail360.data.model.Customer
import com.example.retail360.data.model.InventoryItem
import com.example.retail360.data.model.Product
import com.example.retail360.data.model.RoutePlanEntry
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.StockMovement
import com.example.retail360.data.model.Visit
import com.example.retail360.data.model.VisitPhoto

@Database(
    entities = [
        Customer::class,
        Product::class,
        Visit::class,
        AvailabilityRecord::class,
        SaleItem::class,
        RoutePlanEntry::class,
        InventoryItem::class,
        CompetitorActivity::class,
        PaymentCollection::class,
        ProductUpdate::class,
        ShareOfShelf::class,
        VisitPhoto::class,
        StockMovement::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun visitDao(): VisitDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun saleDao(): SaleDao
    abstract fun routePlanDao(): RoutePlanDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun competitorDao(): CompetitorDao
    abstract fun paymentDao(): PaymentDao
    abstract fun productUpdateDao(): ProductUpdateDao
    abstract fun shareOfShelfDao(): ShareOfShelfDao
    abstract fun visitPhotoDao(): VisitPhotoDao
    abstract fun stockMovementDao(): StockMovementDao
}

