package com.example.retail360.data

import android.net.Uri
import com.example.retail360.data.AvailabilityDao
import com.example.retail360.data.SaleDao
import com.example.retail360.data.VisitDao
import com.example.retail360.model.AvailabilityRecord
import com.example.retail360.model.SaleItem
import com.example.retail360.model.Visit
import com.example.retail360.model.CloudinaryService
import com.example.retail360.data.FirebaseHelper
import kotlinx.coroutines.flow.Flow

class VisitRepository(
    private val visitDao: VisitDao,
    private val availabilityDao: AvailabilityDao,
    private val saleDao: SaleDao,
    private val firebase: FirebaseHelper,
    private val cloudinary: CloudinaryService
) {
    // ---- Visit lifecycle ----
    suspend fun startVisit(
        customerId: String, repId: String, lat: Double, lng: Double, selfieUri: Uri? = null
    ): Visit {
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
        return visit
    }

    suspend fun byId(id: String): Visit? = visitDao.byId(id)

    fun observeForCustomer(customerId: String): Flow<List<Visit>> =
        visitDao.observeForCustomer(customerId)

    fun countCompletedToday(startOfDay: Long): Flow<Int> =
        visitDao.countCompletedSince(startOfDay)

    suspend fun checkOut(visitId: String, lat: Double, lng: Double, notes: String): Visit? {
        val visit = visitDao.byId(visitId) ?: return null
        val closed = visit.copy(
            checkOutTime = System.currentTimeMillis(),
            checkOutLat = lat, checkOutLng = lng,
            notes = notes, status = "COMPLETED", synced = false
        )
        visitDao.upsert(closed)
        runCatching { firebase.pushVisit(closed); visitDao.update(closed.copy(synced = true)) }
        return closed
    }

    // ---- Availability ----
    fun observeAvailability(visitId: String): Flow<List<AvailabilityRecord>> =
        availabilityDao.observeForVisit(visitId)

    suspend fun saveAvailability(record: AvailabilityRecord, localPhotoUri: Uri?) {
        var toSave = record.copy(synced = false)
        availabilityDao.upsert(toSave)
        if (localPhotoUri != null) {
            cloudinary.upload(localPhotoUri, "availability")?.let {
                toSave = toSave.copy(shelfPhotoUrl = it)
            }
        }
        runCatching { firebase.pushAvailability(toSave); toSave = toSave.copy(synced = true) }
        availabilityDao.upsert(toSave)
    }

    // ---- Sales ----
    fun observeSales(visitId: String): Flow<List<SaleItem>> = saleDao.observeForVisit(visitId)

    suspend fun addSale(item: SaleItem) {
        val toSave = item.copy(synced = false)
        saleDao.upsert(toSave)
        runCatching { firebase.pushSale(toSave); saleDao.upsert(toSave.copy(synced = true)) }
    }

    suspend fun removeSale(id: String) = saleDao.delete(id)

    suspend fun salesTotal(visitId: String): Double = saleDao.totalForVisit(visitId)
}

