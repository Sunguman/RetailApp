package com.example.retail360.util

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.retail360.data.local.AppDatabase
import com.example.retail360.data.remote.CloudinaryService
import com.example.retail360.data.remote.FirebaseService
import com.example.retail360.data.repository.AuthRepository
import com.example.retail360.data.repository.CustomerRepository
import com.example.retail360.data.repository.InventoryRepository
import com.example.retail360.data.repository.MerchandisingRepository
import com.example.retail360.data.repository.StockControlRepository
import com.example.retail360.data.repository.ProductRepository
import com.example.retail360.data.repository.RoutePlanRepository
import com.example.retail360.data.repository.VisitRepository
import com.example.retail360.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

/**
 * Tiny manual dependency graph. Initialised once from [Retail360App].
 * ViewModels read from here (Graph.customerRepository, ...) which keeps them
 * no-arg so the default viewModel() factory works. Swap for Hilt if you grow.
 */
object Graph {

    lateinit var db: AppDatabase
        private set

    lateinit var authRepository: AuthRepository
        private set
    lateinit var customerRepository: CustomerRepository
        private set
    lateinit var visitRepository: VisitRepository
        private set
    lateinit var productRepository: ProductRepository
        private set
    lateinit var routePlanRepository: RoutePlanRepository
        private set
    lateinit var inventoryRepository: InventoryRepository
        private set
    lateinit var merchandisingRepository: MerchandisingRepository
        private set
    lateinit var stockControlRepository: StockControlRepository
        private set

    val firebase = FirebaseService()
    val cloudinary = CloudinaryService()

    fun provide(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "sales_automation.db"
        ).fallbackToDestructiveMigration().build()

        authRepository = AuthRepository(firebase)
        customerRepository = CustomerRepository(db.customerDao(), firebase, cloudinary)
        visitRepository = VisitRepository(db.visitDao(), db.availabilityDao(), db.saleDao(), firebase, cloudinary)
        productRepository = ProductRepository(db.productDao(), firebase)
        routePlanRepository = RoutePlanRepository(db.routePlanDao(), firebase)
        inventoryRepository = InventoryRepository(db.inventoryDao(), firebase)
        merchandisingRepository = MerchandisingRepository(
            db.competitorDao(), db.paymentDao(), db.productUpdateDao(), db.shareOfShelfDao(),
            db.visitPhotoDao(), firebase, cloudinary
        )
        stockControlRepository = StockControlRepository(db.stockMovementDao(), db.saleDao(), firebase)

        schedulePeriodicSync(context)
    }

    /** Runs SyncWorker every 15 min when connected. Screens can also trigger one-off syncs. */
    private fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sales_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

