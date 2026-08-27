package com.example.retail360.data.remote

import com.example.retail360.data.model.AvailabilityRecord
import com.example.retail360.data.model.CompetitorActivity
import com.example.retail360.data.model.Customer
import com.example.retail360.data.model.PaymentCollection
import com.example.retail360.data.model.ProductUpdate
import com.example.retail360.data.model.ShareOfShelf
import com.example.retail360.data.model.VisitPhoto
import com.example.retail360.data.model.InventoryItem
import com.example.retail360.data.model.Product
import com.example.retail360.data.model.RoutePlanEntry
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.Visit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * All contact with Firebase lives here. Repositories call these suspend
 * functions; nothing else in the app touches FirebaseAuth/Database directly.
 */
class FirebaseService {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    // ---- Auth ----
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        return auth.signInWithEmailAndPassword(email.trim(), password).await().user
    }

    fun signOut() = auth.signOut()

    // ---- Pushes (offline-first: repo writes locally first, then pushes) ----
    suspend fun pushCustomer(c: Customer) =
        db.child("customers").child(c.id).setValue(c.copy(synced = true)).await()

    suspend fun pushVisit(v: Visit) =
        db.child("visits").child(v.id).setValue(v.copy(synced = true)).await()

    suspend fun pushAvailability(a: AvailabilityRecord) =
        db.child("availability").child(a.id).setValue(a.copy(synced = true)).await()

    suspend fun pushSale(s: SaleItem) =
        db.child("sales").child(s.id).setValue(s.copy(synced = true)).await()

    suspend fun pushRoutePlan(e: RoutePlanEntry) =
        db.child("route_plans").child(e.id).setValue(e.copy(synced = true)).await()

    suspend fun pushInventory(i: InventoryItem) =
        db.child("inventory").child(i.id).setValue(i.copy(synced = true)).await()

    suspend fun pushCompetitor(c: CompetitorActivity) =
        db.child("competitor_activity").child(c.id).setValue(c.copy(synced = true)).await()

    suspend fun pushPayment(p: PaymentCollection) =
        db.child("payment_collection").child(p.id).setValue(p.copy(synced = true)).await()

    suspend fun pushProductUpdate(u: ProductUpdate) =
        db.child("product_update").child(u.id).setValue(u.copy(synced = true)).await()

    suspend fun pushShareOfShelf(s: ShareOfShelf) =
        db.child("share_of_shelf").child(s.id).setValue(s.copy(synced = true)).await()

    suspend fun pushVisitPhoto(p: VisitPhoto) =
        db.child("visit_photo").child(p.id).setValue(p.copy(synced = true)).await()

    // ---- Pulls (catalog is authored server-side) ----
    suspend fun pushProduct(p: Product) =
        db.child("products").child(p.id).setValue(p).await()

    suspend fun fetchProducts(): List<Product> {
        val snap = db.child("products").get().await()
        return snap.children.mapNotNull { it.getValue(Product::class.java) }
    }

    suspend fun fetchCustomers(): List<Customer> {
        val snap = db.child("customers").get().await()
        return snap.children.mapNotNull { it.getValue(Customer::class.java) }
    }

    /** Route plans are authored per rep; filter to the signed-in rep after fetch. */
    suspend fun fetchRoutePlan(repId: String): List<RoutePlanEntry> {
        val snap = db.child("route_plans").get().await()
        return snap.children.mapNotNull { it.getValue(RoutePlanEntry::class.java) }
            .filter { it.repId == repId }
    }

    suspend fun fetchInventory(): List<InventoryItem> {
        val snap = db.child("inventory").get().await()
        return snap.children.mapNotNull { it.getValue(InventoryItem::class.java) }
    }
}