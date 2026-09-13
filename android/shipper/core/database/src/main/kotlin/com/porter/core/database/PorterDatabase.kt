package com.porter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.porter.core.database.dao.BookingDao
import com.porter.core.database.entity.BookingEntity

@Database(
    entities = [BookingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PorterDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao
}
