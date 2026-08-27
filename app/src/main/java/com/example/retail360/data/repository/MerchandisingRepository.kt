package com.example.retail360.data.repository

import android.net.Uri
import com.example.retail360.data.local.CompetitorDao
import com.example.retail360.data.local.PaymentDao
import com.example.retail360.data.local.ProductUpdateDao
import com.example.retail360.data.local.ShareOfShelfDao
import com.example.retail360.data.local.VisitPhotoDao
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.data.model.PaymentCollection
import com.example.retail360.data.model.ProductUpdate
import com.example.retail360.data.model.ShareOfShelf
import com.example.retail360.data.model.VisitPhoto
import com.example.retail360.data.remote.CloudinaryService
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * One repository for the in-visit merchandising activities (competitor, payments,
 * product updates, share of shelf). All writes are Room-first, then best-effort
 * Firebase push; SyncWorker drains anything left unsynced.
 */
class MerchandisingRepository(
    private val competitorDao: CompetitorDao,
    private val paymentDao: PaymentDao,
    private val productUpdateDao: ProductUpdateDao,
    private val shareOfShelfDao: ShareOfShelfDao,
    private val visitPhotoDao: VisitPhotoDao,
    private val firebase: FirebaseService,
    private val cloudinary: CloudinaryService
) {
    // ---- Competitor activity ----
    fun observeCompetitor(visitId: String): Flow<List<CompetitorActivity>> =
        competitorDao.observeForVisit(visitId)

    suspend fun saveCompetitor(item: CompetitorActivity, photoUri: Uri?) = withContext(Dispatchers.IO) {
        val url = photoUri?.let { runCatching { cloudinary.upload(it, "competitor") }.getOrNull() }.orEmpty()
        val row = item.copy(photoUrl = url, synced = false)
        competitorDao.upsert(row)
        runCatching { firebase.pushCompetitor(row); competitorDao.upsert(row.copy(synced = true)) }
    }

    // ---- Payment collection ----
    fun observePayments(visitId: String): Flow<List<PaymentCollection>> =
        paymentDao.observeForVisit(visitId)

    suspend fun savePayment(item: PaymentCollection) = withContext(Dispatchers.IO) {
        val row = item.copy(synced = false)
        paymentDao.upsert(row)
        runCatching { firebase.pushPayment(row); paymentDao.upsert(row.copy(synced = true)) }
    }

    // ---- Product updates ----
    fun observeProductUpdates(visitId: String): Flow<List<ProductUpdate>> =
        productUpdateDao.observeForVisit(visitId)

    suspend fun saveProductUpdate(item: ProductUpdate, photoUri: Uri?) = withContext(Dispatchers.IO) {
        val url = photoUri?.let { runCatching { cloudinary.upload(it, "product_updates") }.getOrNull() }.orEmpty()
        val row = item.copy(photoUrl = url, synced = false)
        productUpdateDao.upsert(row)
        runCatching { firebase.pushProductUpdate(row); productUpdateDao.upsert(row.copy(synced = true)) }
    }

    // ---- Share of shelf ----
    fun observeShareOfShelf(visitId: String): Flow<List<ShareOfShelf>> =
        shareOfShelfDao.observeForVisit(visitId)

    suspend fun saveShareOfShelf(item: ShareOfShelf, photoUri: Uri?) = withContext(Dispatchers.IO) {
        val url = photoUri?.let { runCatching { cloudinary.upload(it, "share_of_shelf") }.getOrNull() }.orEmpty()
        val row = item.copy(photoUrl = url, synced = false)
        shareOfShelfDao.upsert(row)
        runCatching { firebase.pushShareOfShelf(row); shareOfShelfDao.upsert(row.copy(synced = true)) }
    }

    // ---- Visit photos ----
    fun observePhotos(visitId: String): Flow<List<VisitPhoto>> =
        visitPhotoDao.observeForVisit(visitId)

    suspend fun savePhoto(item: VisitPhoto, photoUri: Uri?) = withContext(Dispatchers.IO) {
        val url = photoUri?.let { runCatching { cloudinary.upload(it, "visit_photos") }.getOrNull() }.orEmpty()
        val row = item.copy(photoUrl = url, synced = false)
        visitPhotoDao.upsert(row)
        runCatching { firebase.pushVisitPhoto(row); visitPhotoDao.upsert(row.copy(synced = true)) }
    }
}