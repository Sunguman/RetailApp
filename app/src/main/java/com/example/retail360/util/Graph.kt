package com.example.retail360.util

import android.content.Context
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.retail360.data.AppDatabase
import com.example.retail360.model.CloudinaryService
import com.example.retail360.data.FirebaseHelper
import com.example.retail360.data.AuthRepository
import com.example.retail360.data.CustomerRepository
import com.example.retail360.data.InventoryRepository
import com.example.retail360.data.ProductRepository
import com.example.retail360.data.RoutePlanRepository
import com.example.retail360.data.VisitRepository
import com.example.retail360.data.SyncWorker
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

    val firebase = FirebaseHelper()
    val cloudinary = CloudinaryService()

    fun provide(context: Context) {
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "retail360.db"
        ).fallbackToDestructiveMigration().build()

        authRepository = AuthRepository(firebase)
        customerRepository = CustomerRepository(db.customerDao(), firebase, cloudinary)
        visitRepository = VisitRepository(db.visitDao(), db.availabilityDao(), db.saleDao(), firebase, cloudinary)
        productRepository = ProductRepository(db.productDao(), firebase)
        routePlanRepository = RoutePlanRepository(db.routePlanDao(), firebase)
        inventoryRepository = InventoryRepository(db.inventoryDao(), firebase)

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
            "retail360_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

