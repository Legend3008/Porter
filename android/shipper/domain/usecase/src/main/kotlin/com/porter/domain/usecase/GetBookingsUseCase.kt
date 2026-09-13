package com.porter.domain.usecase

import com.porter.domain.model.BookingListItem
import com.porter.domain.model.BookingStatus
import com.porter.domain.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookingsUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    operator fun invoke(status: BookingStatus? = null): Flow<List<BookingListItem>> {
        return bookingRepository.getBookings(status)
    }
}
