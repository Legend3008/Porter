package com.porter.data.repository

import com.porter.domain.model.*
import com.porter.domain.repository.BookingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepositoryMockImpl @Inject constructor() : BookingRepository {

    override fun getBookings(status: BookingStatus?): Flow<List<BookingListItem>> = flow {
        delay(800)
        emit(mockBookingListItems.filter { status == null || it.status == status })
    }

    override suspend fun getBooking(id: String): Result<Booking> {
        delay(600)
        return mockBookings.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure(Exception("Booking not found"))
    }

    override suspend fun createDraft(shipmentDetails: ShipmentDetails): Result<String> {
        delay(1000)
        return Result.success("draft_${System.currentTimeMillis()}")
    }

    override suspend fun getQuote(draftId: String): Result<Quote> {
        delay(1500)
        return Result.success(
            Quote(
                id = "qte_001",
                bookingDraftId = draftId,
                baseFarePaise = 4500000L,     // ₹45,000
                fuelSurchargePaise = 450000L,  // ₹4,500
                portHandlingPaise = 200000L,   // ₹2,000
                otherSurchargePaise = 100000L, // ₹1,000
                subtotalPaise = 5250000L,
                gstPaise = 945000L,            // 18% GST
                totalAmountPaise = 6195000L,   // ₹61,950
                gstRate = 18.0,
                validUntil = Clock.System.now().plus(15, DateTimeUnit.MINUTE),
                currency = "INR"
            )
        )
    }

    override suspend fun confirmBooking(draftId: String, quoteId: String): Result<String> {
        delay(1200)
        return Result.success("bkg_${System.currentTimeMillis()}")
    }

    override suspend fun cancelBooking(id: String, reason: String): Result<Unit> {
        delay(800)
        return Result.success(Unit)
    }

    private val now = Clock.System.now()

    private val mockAddress = Address(
        line1 = "Plot 45, JNPT Road",
        city = "Navi Mumbai",
        state = "Maharashtra",
        pincode = "400707",
        contactName = "Ramesh Kumar",
        contactPhone = "+919998887776"
    )

    private val mockBookings = listOf(
        Booking(
            id = "bkg_001",
            referenceNumber = "PRT-20240914-001",
            status = BookingStatus.IN_TRANSIT,
            shipmentDetails = ShipmentDetails(
                originPort = "INNSA",
                destinationPort = "INCCU",
                pickupAddress = mockAddress,
                deliveryAddress = mockAddress.copy(city = "Kolkata", pincode = "700001"),
                containerType = ContainerType.DRY_40FT,
                containerCount = 2,
                totalWeightKg = 18000.0,
                commodity = "Textiles",
                scheduledDate = now
            ),
            quoteId = "qte_001",
            totalAmountPaise = 6195000L,
            paidAmountPaise = 6195000L,
            assignedTripId = "trp_001",
            createdAt = now,
            updatedAt = now,
            estimatedPickupAt = now,
            estimatedDeliveryAt = now.plus(3, DateTimeUnit.DAY, TimeZone.UTC),
            actualDeliveredAt = null,
            cancellationReason = null
        ),
        Booking(
            id = "bkg_002",
            referenceNumber = "PRT-20240913-002",
            status = BookingStatus.CONFIRMED,
            shipmentDetails = ShipmentDetails(
                originPort = "INMAA",
                destinationPort = "INBOM",
                pickupAddress = mockAddress.copy(city = "Chennai"),
                deliveryAddress = mockAddress.copy(city = "Mumbai"),
                containerType = ContainerType.DRY_20FT,
                containerCount = 1,
                totalWeightKg = 8500.0,
                commodity = "Auto Parts",
                scheduledDate = now.plus(2, DateTimeUnit.DAY, TimeZone.UTC)
            ),
            quoteId = "qte_002",
            totalAmountPaise = 3200000L,
            paidAmountPaise = 3200000L,
            assignedTripId = null,
            createdAt = now,
            updatedAt = now,
            estimatedPickupAt = now.plus(2, DateTimeUnit.DAY, TimeZone.UTC),
            estimatedDeliveryAt = now.plus(5, DateTimeUnit.DAY, TimeZone.UTC),
            actualDeliveredAt = null,
            cancellationReason = null
        )
    )

    private val mockBookingListItems = mockBookings.map {
        BookingListItem(
            id = it.id,
            referenceNumber = it.referenceNumber,
            status = it.status,
            originPort = it.shipmentDetails.originPort,
            destinationPort = it.shipmentDetails.destinationPort,
            containerType = it.shipmentDetails.containerType,
            containerCount = it.shipmentDetails.containerCount,
            totalAmountPaise = it.totalAmountPaise,
            createdAt = it.createdAt,
            estimatedDeliveryAt = it.estimatedDeliveryAt
        )
    }
}
