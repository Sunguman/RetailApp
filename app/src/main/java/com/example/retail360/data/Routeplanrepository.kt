package com.example.retail360.data

import com.example.retail360.data.PlannedStop
import com.example.retail360.data.RoutePlanDao
import com.example.retail360.model.RoutePlanEntry
import com.example.retail360.data.FirebaseHelper
import kotlinx.coroutines.flow.Flow

class RoutePlanRepository(
    private val dao: RoutePlanDao,
    private val firebase: FirebaseHelper
) {
    fun observeStops(repId: String, dayOfWeek: Int, startOfDay: Long): Flow<List<PlannedStop>> =
        dao.observeStops(repId, dayOfWeek, startOfDay)

    fun countForDay(repId: String, dayOfWeek: Int): Flow<Int> =
        dao.countForDay(repId, dayOfWeek)

    fun observeAssignedIds(repId: String, dayOfWeek: Int): Flow<List<String>> =
        dao.observeCustomerIdsForDay(repId, dayOfWeek)

    /** Assign a customer to a rep's route for a given weekday. Room-first, then push. */
    suspend fun assign(repId: String, customerId: String, dayOfWeek: Int) {
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
    suspend fun unassign(entryId: String) = dao.delete(entryId)

    suspend fun refreshFromServer(repId: String) {
        runCatching {
            firebase.fetchRoutePlan(repId).forEach { dao.upsert(it.copy(synced = true)) }
        }
    }
}
