package com.porter.domain.repository

import com.porter.domain.model.Booking
import com.porter.domain.model.BookingListItem
import com.porter.domain.model.BookingStatus
import com.porter.domain.model.Quote
import com.porter.domain.model.ShipmentDetails
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun getBookings(status: BookingStatus? = null): Flow<List<BookingListItem>>
    suspend fun getBooking(id: String): Result<Booking>
    suspend fun createDraft(shipmentDetails: ShipmentDetails): Result<String> // returns draftId
    suspend fun getQuote(draftId: String): Result<Quote>
    suspend fun confirmBooking(draftId: String, quoteId: String): Result<String> // returns bookingId
    suspend fun cancelBooking(id: String, reason: String): Result<Unit>
}
