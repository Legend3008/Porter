package com.porter.feature.tracking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.core.ui.components.FreshnessLevel
import com.porter.domain.model.GpsLocation
import com.porter.domain.model.Trip
import com.porter.domain.repository.TrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

data class TrackingUiModel(
    val trip: Trip,
    val currentLocation: GpsLocation,
    val freshness: FreshnessLevel = FreshnessLevel.LIVE,
    val lastUpdatedText: String = "Just now"
)

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<TrackingUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<TrackingUiModel>> = _uiState.asStateFlow()

    fun startTracking(tripId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val tripResult = trackingRepository.getTrip(tripId)
            tripResult.fold(
                onSuccess = { trip ->
                    val initialLoc = trip.currentLocation ?: GpsLocation(
                        latitude = 18.9496,
                        longitude = 72.9490,
                        heading = 112f,
                        speedKmh = 54.0,
                        recordedAt = Clock.System.now()
                    )

                    _uiState.value = UiState.Success(
                        TrackingUiModel(
                            trip = trip,
                            currentLocation = initialLoc,
                            freshness = FreshnessLevel.LIVE,
                            lastUpdatedText = "Live stream"
                        )
                    )

                    // Observe live location flow
                    trackingRepository.observeTripLocation(tripId)
                        .catch {
                            val current = (_uiState.value as? UiState.Success)?.data
                            if (current != null) {
                                _uiState.value = UiState.Success(
                                    current.copy(
                                        freshness = FreshnessLevel.DISCONNECTED,
                                        lastUpdatedText = "Reconnecting..."
                                    )
                                )
                            }
                        }
                        .collect { loc ->
                            val current = (_uiState.value as? UiState.Success)?.data
                            if (current != null) {
                                _uiState.value = UiState.Success(
                                    current.copy(
                                        currentLocation = loc,
                                        freshness = FreshnessLevel.LIVE,
                                        lastUpdatedText = "Just now"
                                    )
                                )
                            }
                        }
                },
                onFailure = { err ->
                    _uiState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Failed to load live tracking: ${err.message}",
                            recovery = "Retry"
                        )
                    )
                }
            )
        }
    }
}
