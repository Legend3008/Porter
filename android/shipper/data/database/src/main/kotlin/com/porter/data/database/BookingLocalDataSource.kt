package com.porter.data.database

import com.porter.core.database.dao.BookingDao
import com.porter.core.database.entity.BookingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingLocalDataSource @Inject constructor(
    private val bookingDao: BookingDao
) {
    fun getAllBookings(): Flow<List<BookingEntity>> = bookingDao.getAllBookings()

    fun getBookingsByStatus(status: String): Flow<List<BookingEntity>> =
        bookingDao.getBookingsByStatus(status)

    suspend fun getBookingById(id: String): BookingEntity? = bookingDao.getBookingById(id)

    suspend fun saveBookings(bookings: List<BookingEntity>) = bookingDao.insertBookings(bookings)

    suspend fun saveBooking(booking: BookingEntity) = bookingDao.insertBooking(booking)

    suspend fun deleteBooking(id: String) = bookingDao.deleteBookingById(id)

    suspend fun clear() = bookingDao.clearAll()
}
