package com.porter.domain.usecase

import com.porter.domain.model.ShipmentDetails
import com.porter.domain.repository.BookingRepository
import javax.inject.Inject

class CreateShipmentDraftUseCase @Inject constructor(
    private val bookingRepository: BookingRepository
) {
    suspend operator fun invoke(shipmentDetails: ShipmentDetails): Result<String> {
        return bookingRepository.createDraft(shipmentDetails)
    }
}
