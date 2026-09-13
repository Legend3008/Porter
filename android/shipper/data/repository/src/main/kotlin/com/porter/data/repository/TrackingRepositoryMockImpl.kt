package com.porter.data.repository

import com.porter.domain.model.*
import com.porter.domain.repository.TrackingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class TrackingRepositoryMockImpl @Inject constructor() : TrackingRepository {

    /** Emits a simulated GPS ping every 5 seconds — mimics WebSocket behavior. */
    override fun observeTripLocation(tripId: String): Flow<GpsLocation> = flow {
        // Start near JNPT (Navi Mumbai)
        var lat = 18.9489
        var lng = 72.9497
        while (true) {
            delay(5000L)
            lat += Random.nextDouble(-0.001, 0.001)
            lng += Random.nextDouble(-0.001, 0.001)
            emit(
                GpsLocation(
                    latitude = lat,
                    longitude = lng,
                    heading = Random.nextFloat() * 360f,
                    speedKmh = Random.nextDouble(20.0, 60.0),
                    recordedAt = Clock.System.now()
                )
            )
        }
    }

    override suspend fun getTrip(tripId: String): Result<Trip> {
        delay(700)
        return Result.success(
            Trip(
                id = tripId,
                bookingId = "bkg_001",
                status = TripStatus.EN_ROUTE_DELIVERY,
                driver = DriverInfo(
                    id = "drv_001",
                    name = "Suresh Patel",
                    phone = "+919876500001",
                    rating = 4.7,
                    photoUrl = null
                ),
                vehicle = VehicleInfo("MH 12 AB 1234", "Tata", "Prima"),
                currentLocation = GpsLocation(
                    latitude = 18.9489,
                    longitude = 72.9497,
                    heading = 45f,
                    speedKmh = 42.0,
                    recordedAt = Clock.System.now()
                ),
                lastLocationAt = Clock.System.now(),
                statusHistory = mockStatusHistory(tripId),
                estimatedArrivalAt = null
            )
        )
    }

    private fun mockStatusHistory(tripId: String): List<TripStatusEvent> {
        val now = Clock.System.now()
        return listOf(
            TripStatusEvent(TripStatus.ASSIGNED, "Driver Assigned", "Suresh Patel is assigned to your shipment", now, null, isCompleted = true, isCurrent = false),
            TripStatusEvent(TripStatus.EN_ROUTE_PICKUP, "En Route to Pickup", "Driver is on the way to pickup location", now, "Navi Mumbai", isCompleted = true, isCurrent = false),
            TripStatusEvent(TripStatus.AT_PICKUP, "At Pickup", "Driver has reached pickup location", now, "JNPT Gate 3", isCompleted = true, isCurrent = false),
            TripStatusEvent(TripStatus.LOADED, "Container Loaded", "2x 40ft containers loaded", now, "JNPT", isCompleted = true, isCurrent = false),
            TripStatusEvent(TripStatus.EN_ROUTE_DELIVERY, "In Transit", "Moving towards delivery location", now, "NH-48", isCompleted = false, isCurrent = true),
            TripStatusEvent(TripStatus.AT_DELIVERY, "At Delivery", null, null, null, isCompleted = false, isCurrent = false),
            TripStatusEvent(TripStatus.DELIVERED, "Delivered", null, null, null, isCompleted = false, isCurrent = false),
        )
    }
}
