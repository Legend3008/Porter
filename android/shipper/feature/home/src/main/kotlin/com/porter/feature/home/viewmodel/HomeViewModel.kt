package com.porter.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porter.core.common.AppError
import com.porter.core.common.UiState
import com.porter.domain.model.BookingListItem
import com.porter.domain.model.BookingStatus
import com.porter.domain.model.User
import com.porter.domain.repository.AuthRepository
import com.porter.domain.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeDashboardData(
    val user: User?,
    val activeBookings: List<BookingListItem>,
    val unreadNotificationsCount: Int = 2
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeDashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeDashboardData>> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val profileResult = authRepository.getProfile()
            val user = profileResult.getOrNull()

            bookingRepository.getBookings()
                .catch { exception ->
                    _uiState.value = UiState.Error(
                        AppError.Network(
                            userMessage = "Could not load active shipments: ${exception.message}",
                            recovery = "Pull to refresh"
                        )
                    )
                }
                .collect { bookings ->
                    val active = bookings.filter {
                        it.status != BookingStatus.DELIVERED && it.status != BookingStatus.CANCELLED
                    }
                    _uiState.value = UiState.Success(
                        HomeDashboardData(
                            user = user,
                            activeBookings = active,
                            unreadNotificationsCount = 2
                        )
                    )
                }
        }
    }
}
