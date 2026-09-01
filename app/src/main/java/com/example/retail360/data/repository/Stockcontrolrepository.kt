package com.example.retail360.data.repository

import com.example.retail360.data.local.SaleDao
import com.example.retail360.data.local.StockMovementDao
import com.example.retail360.data.model.SaleItem
import com.example.retail360.data.model.StockMovement
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Movement-type / status constants (single source of truth for stock math). */
object Stock {
    const val REQUISITION = "REQUISITION"
    const val UPLIFT = "UPLIFT"
    const val RETURN = "RETURN"

    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val CONFIRMED = "CONFIRMED"
}

/** Per-product stock position for the current Active Visit. */
data class ProductStock(
    val productId: String,
    val productName: String,
    val unit: String,
    val unitPrice: Double,
    val available: Int,
    val pendingReq: Int,
    val sold: Int,
    val returned: Int
)

class StockControlRepository(
    private val stockDao: StockMovementDao,
    private val saleDao: SaleDao,
    private val firebase: FirebaseService
) {
    fun observeMovements(visitId: String): Flow<List<StockMovement>> =
        stockDao.observeForVisit(visitId)

    /** Rep van stock across ALL visits (device holds only this rep's data). */
    fun observeVanStock(): Flow<List<ProductStock>> =
        combine(stockDao.observeAll(), saleDao.observeAll()) { movements, sales ->
            computeStock(movements, sales)
        }

    /** Submit a batch of movement lines. Caller sets the correct status per movement type. */
    suspend fun submit(items: List<StockMovement>) {
        items.forEach { item ->
            val row = item.copy(synced = false)
            stockDao.upsert(row)
            runCatching { firebase.pushStockMovement(row); stockDao.upsert(row.copy(synced = true)) }
        }
    }

    /** Stock points available to the user. TODO: source from backend master data per user/visit. */
    fun stockPoints(): List<String> = listOf("Main Van", "Warehouse", "Depot")

    companion object {
        /**
         * Available Stock = (Approved OR Pending Requisition) + Confirmed Uplifts − Returns − Sold Quantity.
         * For production convenience, we include PENDING requisitions so reps can sell immediately.
         */
        fun computeStock(movements: List<StockMovement>, sales: List<SaleItem>): List<ProductStock> {
            val ids = (movements.map { it.productId } + sales.map { it.productId }).toSet()
            return ids.map { pid ->
                val ms = movements.filter { it.productId == pid }
                val ss = sales.filter { it.productId == pid }
                val name = ms.firstOrNull()?.productName ?: ss.firstOrNull()?.productName ?: ""
                val unit = ms.firstOrNull { it.unit.isNotBlank() }?.unit ?: "PIECE"
                val price = ms.firstOrNull()?.unitPrice ?: ss.firstOrNull()?.unitPrice ?: 0.0
                
                // We count both APPROVED and PENDING as available for selection in Sell module
                val totalReq = ms.filter { it.movementType == Stock.REQUISITION && 
                    (it.status == Stock.APPROVED || it.status == Stock.PENDING) }
                    .sumOf { it.quantity }
                
                val pending = ms.filter { it.movementType == Stock.REQUISITION && it.status == Stock.PENDING }
                    .sumOf { it.quantity }
                
                val uplift = ms.filter { it.movementType == Stock.UPLIFT && it.status == Stock.CONFIRMED }
                    .sumOf { it.quantity }
                
                val returned = ms.filter { it.movementType == Stock.RETURN }.sumOf { it.quantity }
                val sold = ss.sumOf { it.quantity }
                
                ProductStock(
                    productId = pid, productName = name, unit = unit, unitPrice = price,
                    available = totalReq + uplift - returned - sold,
                    pendingReq = pending, sold = sold, returned = returned
                )
            }.filter { it.productName.isNotBlank() }
        }
    }
}
