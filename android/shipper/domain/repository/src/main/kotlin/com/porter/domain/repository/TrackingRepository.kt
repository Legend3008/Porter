package com.porter.domain.repository

import com.porter.domain.model.GpsLocation
import com.porter.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TrackingRepository {
    /** Returns a Flow that emits live GPS updates from WebSocket. */
    fun observeTripLocation(tripId: String): Flow<GpsLocation>
    suspend fun getTrip(tripId: String): Result<Trip>
}
