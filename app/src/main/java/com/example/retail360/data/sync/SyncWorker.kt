package com.example.retail360.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.retail360.util.Graph

/**
 * Drains everything still marked synced=false and pushes it to Firebase, then
 * refreshes the catalog. Runs periodically (see Graph) and can be enqueued
 * on demand from the SyncStatusScreen.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = Graph.db
            val fb = Graph.firebase

            db.customerDao().unsynced().forEach { fb.pushCustomer(it); db.customerDao().update(it.copy(synced = true)) }
            db.visitDao().unsynced().forEach { fb.pushVisit(it); db.visitDao().update(it.copy(synced = true)) }
            db.availabilityDao().unsynced().forEach { fb.pushAvailability(it); db.availabilityDao().upsert(it.copy(synced = true)) }
            db.saleDao().unsynced().forEach { fb.pushSale(it); db.saleDao().upsert(it.copy(synced = true)) }
            db.routePlanDao().unsynced().forEach { fb.pushRoutePlan(it); db.routePlanDao().update(it.copy(synced = true)) }
            db.inventoryDao().unsynced().forEach { fb.pushInventory(it); db.inventoryDao().upsert(it.copy(synced = true)) }
            db.competitorDao().unsynced().forEach { fb.pushCompetitor(it); db.competitorDao().upsert(it.copy(synced = true)) }
            db.paymentDao().unsynced().forEach { fb.pushPayment(it); db.paymentDao().upsert(it.copy(synced = true)) }
            db.productUpdateDao().unsynced().forEach { fb.pushProductUpdate(it); db.productUpdateDao().upsert(it.copy(synced = true)) }
            db.shareOfShelfDao().unsynced().forEach { fb.pushShareOfShelf(it); db.shareOfShelfDao().upsert(it.copy(synced = true)) }
            db.visitPhotoDao().unsynced().forEach { fb.pushVisitPhoto(it); db.visitPhotoDao().upsert(it.copy(synced = true)) }
            db.stockMovementDao().unsynced().forEach { fb.pushStockMovement(it); db.stockMovementDao().upsert(it.copy(synced = true)) }

            // Pull side: keep catalog + customers fresh.
            Graph.productRepository.refreshCatalog()
            Graph.customerRepository.refreshFromServer()
            Graph.inventoryRepository.refreshFromServer()
            Graph.authRepository.currentUser()?.uid?.let { Graph.routePlanRepository.refreshFromServer(it) }

            Result.success()
        } catch (e: Exception) {
            // Transient failure (e.g. dropped connection) — WorkManager retries.
            Result.retry()
        }
    }
}

