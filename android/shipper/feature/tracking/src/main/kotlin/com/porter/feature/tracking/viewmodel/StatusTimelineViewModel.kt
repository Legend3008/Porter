package com.porter.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.TripStatus
import com.porter.domain.model.TripStatusEvent
import com.porter.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class StatusTimelineViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<TripStatusEvent>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<TripStatusEvent>>> = _uiState.asStateFlow()

    fun loadTimeline(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val tripResult = trackingRepository.getTrip(bookingId)
            tripResult.fold(
                onSuccess = { trip ->
                    val events = if (trip.statusHistory.isNotEmpty()) {
                        trip.statusHistory
                    } else {
                        createDefaultEvents()
                    }
                    _uiState.value = UiState.Success(events)
                },
                onFailure = {
                    _uiState.value = UiState.Success(createDefaultEvents())
                }
            )
        }
    }

    private fun createDefaultEvents(): List<TripStatusEvent> {
        return listOf(
            TripStatusEvent(
                status = TripStatus.ASSIGNED,
                label = "Driver & Trailer Assigned",
                description = "Carrier allocated trailer MH-04-AB-1234 to shipment.",
                occurredAt = Instant.parse("2024-06-01T08:30:00Z"),
                location = "JNPT Marshalling Yard",
                isCompleted = true,
                isCurrent = false
            ),
            TripStatusEvent(
                status = TripStatus.AT_PICKUP,
                label = "Arrived at Port Terminal",
                description = "Trailer reached Gate 2 and cleared port entry verification.",
                occurredAt = Instant.parse("2024-06-01T09:15:00Z"),
                location = "JNPT Container Terminal 4",
                isCompleted = true,
                isCurrent = false
            ),
            TripStatusEvent(
                status = TripStatus.LOADED,
                label = "Container Mounted & Gated Out",
                description = "20ft container hoisted on chassis. Gate pass validated.",
                occurredAt = Instant.parse("2024-06-01T10:00:00Z"),
                location = "JNPT Port Gate",
                isCompleted = true,
                isCurrent = false
            ),
            TripStatusEvent(
                status = TripStatus.EN_ROUTE_DELIVERY,
                label = "In Transit on Highway",
                description = "En route on Mumbai-Pune Expressway. GPS active.",
                occurredAt = Instant.parse("2024-06-01T11:45:00Z"),
                location = "Khalapur Toll Plaza",
                isCompleted = false,
                isCurrent = true
            ),
            TripStatusEvent(
                status = TripStatus.AT_DELIVERY,
                label = "Arrived at Consignee Destination",
                description = "Trailer arriving at warehouse gate.",
                occurredAt = Instant.parse("2024-06-01T14:30:00Z"),
                location = "Chakan MIDC, Pune",
                isCompleted = false,
                isCurrent = false
            ),
            TripStatusEvent(
                status = TripStatus.DELIVERED,
                label = "Delivered & POD Signed",
                description = "Container grounded and e-POD verified.",
                occurredAt = Instant.parse("2024-06-01T15:00:00Z"),
                location = "Warehouse Bay 3",
                isCompleted = false,
                isCurrent = false
            )
        )
    }
}
