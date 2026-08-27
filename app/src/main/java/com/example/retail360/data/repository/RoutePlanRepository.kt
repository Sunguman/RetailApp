package com.example.retail360.data.repository

import com.example.retail360.data.local.PlannedStop
import com.example.retail360.data.local.RoutePlanDao
import com.example.retail360.data.model.RoutePlanEntry
import com.example.retail360.data.remote.FirebaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RoutePlanRepository(
    private val dao: RoutePlanDao,
    private val firebase: FirebaseService
) {
    fun observeStops(repId: String, dayOfWeek: Int, startOfDay: Long): Flow<List<PlannedStop>> =
        dao.observeStops(repId, dayOfWeek, startOfDay)

    fun countForDay(repId: String, dayOfWeek: Int): Flow<Int> =
        dao.countForDay(repId, dayOfWeek)

    fun observeAssignedIds(repId: String, dayOfWeek: Int): Flow<List<String>> =
        dao.observeCustomerIdsForDay(repId, dayOfWeek)

    /** Assign a customer to a rep's route for a given weekday. Room-first, then push. */
    suspend fun assign(repId: String, customerId: String, dayOfWeek: Int) = withContext(Dispatchers.IO) {
        val entry = RoutePlanEntry(
            id = "$repId-$customerId-$dayOfWeek",
            repId = repId,
            customerId = customerId,
            dayOfWeek = dayOfWeek,
            synced = false
        )
        dao.upsert(entry)
        runCatching { firebase.pushRoutePlan(entry); dao.update(entry.copy(synced = true)) }
    }

    /** Remove a stop locally. NOTE: server deletion isn't propagated — add a
     *  tombstone or a Firebase remove() call if plans must be deletable across devices. */
    suspend fun unassign(entryId: String) = withContext(Dispatchers.IO) {
        dao.delete(entryId)
    }

    suspend fun refreshFromServer(repId: String) = withContext(Dispatchers.IO) {
        runCatching {
            firebase.fetchRoutePlan(repId).forEach { dao.upsert(it.copy(synced = true)) }
        }
    }
}
