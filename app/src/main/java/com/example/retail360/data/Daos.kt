package com.example.retail360.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.retail360.model.AvailabilityRecord
import com.example.retail360.model.InventoryItem
import com.example.retail360.model.RoutePlanEntry
import com.example.retail360.model.Customer
import com.example.retail360.model.Product
import com.example.retail360.model.SaleItem
import com.example.retail360.model.Visit
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun byId(id: String): Customer?

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun unsynced(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product)
}

data class VisitHistoryRow(
    val visitId: String,
    val customerName: String,
    val checkInTime: Long,
    val checkOutTime: Long?,
    val status: String
)

@Dao
interface VisitDao {
    @Query(
        """
        SELECT v.id AS visitId, c.name AS customerName, v.checkInTime AS checkInTime,
               v.checkOutTime AS checkOutTime, v.status AS status
        FROM visits v JOIN customers c ON c.id = v.customerId
        ORDER BY v.checkInTime DESC LIMIT 100
        """
    )
    fun observeHistory(): Flow<List<VisitHistoryRow>>

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun byId(id: String): Visit?

    @Query("SELECT * FROM visits WHERE customerId = :customerId ORDER BY checkInTime DESC")
    fun observeForCustomer(customerId: String): Flow<List<Visit>>

    @Query("SELECT COUNT(*) FROM visits WHERE status = 'COMPLETED' AND checkInTime >= :since")
    fun countCompletedSince(since: Long): Flow<Int>

    @Query("SELECT * FROM visits WHERE synced = 0")
    suspend fun unsynced(): List<Visit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(visit: Visit)

    @Update
    suspend fun update(visit: Visit)
}

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM availability WHERE visitId = :visitId")
    fun observeForVisit(visitId: String): Flow<List<AvailabilityRecord>>

    @Query("SELECT * FROM availability WHERE synced = 0")
    suspend fun unsynced(): List<AvailabilityRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AvailabilityRecord)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sale_items WHERE visitId = :visitId")
    fun observeForVisit(visitId: String): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE synced = 0")
    suspend fun unsynced(): List<SaleItem>

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM sale_items WHERE visitId = :visitId")
    suspend fun totalForVisit(visitId: String): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SaleItem)

    @Query("DELETE FROM sale_items WHERE id = :id")
    suspend fun delete(id: String)
}

/** Read-model for a planned stop: the route entry joined to its customer,
 *  plus whether that customer has already been visited today. */
data class PlannedStop(
    val entryId: String,
    val customerId: String,
    val customerName: String,
    val customerType: String,
    val customerLat: Double,
    val customerLng: Double,
    val photoUrl: String,
    val sequence: Int,
    val visited: Boolean
)

@Dao
interface RoutePlanDao {
    @Query(
        """
        SELECT rp.id AS entryId, c.id AS customerId, c.name AS customerName,
               c.type AS customerType, c.latitude AS customerLat, c.longitude AS customerLng,
               c.photoUrl AS photoUrl, rp.sequence AS sequence,
               EXISTS(SELECT 1 FROM visits v WHERE v.customerId = c.id
                      AND v.status = 'COMPLETED' AND v.checkInTime >= :startOfDay) AS visited
        FROM route_plan rp
        JOIN customers c ON c.id = rp.customerId
        WHERE rp.repId = :repId AND rp.dayOfWeek = :dayOfWeek
        ORDER BY rp.sequence ASC, c.name ASC
        """
    )
    fun observeStops(repId: String, dayOfWeek: Int, startOfDay: Long): Flow<List<PlannedStop>>

    @Query("SELECT COUNT(*) FROM route_plan WHERE repId = :repId AND dayOfWeek = :dayOfWeek")
    fun countForDay(repId: String, dayOfWeek: Int): Flow<Int>

    @Query("SELECT customerId FROM route_plan WHERE repId = :repId AND dayOfWeek = :dayOfWeek")
    fun observeCustomerIdsForDay(repId: String, dayOfWeek: Int): Flow<List<String>>

    @Query("SELECT * FROM route_plan WHERE synced = 0")
    suspend fun unsynced(): List<RoutePlanEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RoutePlanEntry)

    @Update
    suspend fun update(entry: RoutePlanEntry)

    @Query("DELETE FROM route_plan WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY productName ASC")
    fun observeAll(): Flow<List<InventoryItem>>

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM inventory")
    fun totalValue(): Flow<Double>

    @Query("SELECT * FROM inventory WHERE synced = 0")
    suspend fun unsynced(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryItem)
}