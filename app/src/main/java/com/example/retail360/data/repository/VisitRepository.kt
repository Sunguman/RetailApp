package com.example.retail360.data.repository

import android.net.Uri
import com.example.retail360.data.local.AvailabilityDao
import com.example.retail360.data.local.SaleDao
import com.example.retail360.data.local.VisitDao
import com.example.retail360.data.local.VisitHistoryRow
import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.Visit
import com.example.retail360.data.remote.CloudinaryService
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VisitRepository(
    private val visitDao: VisitDao,
    private val availabilityDao: AvailabilityDao,
    private val saleDao: SaleDao,
    private val firebase: FirebaseService,
    private val cloudinary: CloudinaryService
) {
    // ---- Visit lifecycle ----
    suspend fun startVisit(
        customerId: String, repId: String, lat: Double, lng: Double, selfieUri: Uri? = null
    ): Visit = withContext(Dispatchers.IO) {
        // Upload the check-in selfie as proof (best-effort; empty if offline).
        val selfieUrl = selfieUri?.let {
            runCatching { cloudinary.upload(it, "selfies") }.getOrNull()
        }.orEmpty()
        val visit = Visit(
            customerId = customerId, repId = repId,
            checkInLat = lat, checkInLng = lng, selfieUrl = selfieUrl
        )
        visitDao.upsert(visit)
        runCatching { firebase.pushVisit(visit) }
        visit
    }

    suspend fun byId(id: String): Visit? = withContext(Dispatchers.IO) {
        visitDao.byId(id)
    }

    fun observeForCustomer(customerId: String): Flow<List<Visit>> =
        visitDao.observeForCustomer(customerId)

    fun observeHistory(): Flow<List<VisitHistoryRow>> = visitDao.observeHistory()

    fun countCompletedToday(startOfDay: Long): Flow<Int> =
        visitDao.countCompletedSince(startOfDay)

    suspend fun checkOut(visitId: String, lat: Double, lng: Double, notes: String): Visit? = withContext(Dispatchers.IO) {
        val visit = visitDao.byId(visitId) ?: return@withContext null
        val closed = visit.copy(
            checkOutTime = System.currentTimeMillis(),
            checkOutLat = lat, checkOutLng = lng,
            notes = notes, status = "COMPLETED", synced = false
        )
        visitDao.upsert(closed)
        runCatching { firebase.pushVisit(closed); visitDao.update(closed.copy(synced = true)) }
        closed
    }

    // ---- Availability ----
    fun observeAvailability(visitId: String): Flow<List<AvailabilityRecord>> =
        availabilityDao.observeForVisit(visitId)

    suspend fun saveAvailability(record: AvailabilityRecord) = withContext(Dispatchers.IO) {
        val toSave = record.copy(synced = false)
        availabilityDao.upsert(toSave)
        runCatching { firebase.pushAvailability(toSave); availabilityDao.upsert(toSave.copy(synced = true)) }
    }

    // ---- Sales ----
    fun observeAllSales(): Flow<List<SaleItem>> = saleDao.observeAll()

    fun observeSales(visitId: String): Flow<List<SaleItem>> = saleDao.observeForVisit(visitId)

    suspend fun addSale(item: SaleItem) = withContext(Dispatchers.IO) {
        val toSave = item.copy(synced = false)
        saleDao.upsert(toSave)
        runCatching { firebase.pushSale(toSave); saleDao.upsert(toSave.copy(synced = true)) }
    }

    suspend fun removeSale(id: String) = withContext(Dispatchers.IO) {
        saleDao.delete(id)
    }

    suspend fun salesTotal(visitId: String): Double = withContext(Dispatchers.IO) {
        saleDao.totalForVisit(visitId)
    }
}
