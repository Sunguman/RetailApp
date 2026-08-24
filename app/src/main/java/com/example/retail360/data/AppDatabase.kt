package com.example.retail360.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.retail360.model.AvailabilityRecord
import com.example.retail360.model.Customer
import com.example.retail360.model.InventoryItem
import com.example.retail360.model.Product
import com.example.retail360.model.RoutePlanEntry
import com.example.retail360.model.SaleItem
import com.example.retail360.model.Visit

@Database(
    entities = [
        Customer::class,
        Product::class,
        Visit::class,
        AvailabilityRecord::class,
        SaleItem::class,
        RoutePlanEntry::class,
        InventoryItem::class
    ],
    version = 3,
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
}


