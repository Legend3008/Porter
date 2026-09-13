package com.porter.core.database

import android.content.Context
import androidx.room.Room
import com.porter.core.database.dao.BookingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePorterDatabase(
        @ApplicationContext context: Context
    ): PorterDatabase {
        return Room.databaseBuilder(
            context,
            PorterDatabase::class.java,
            "porter_shipper.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBookingDao(database: PorterDatabase): BookingDao {
        return database.bookingDao()
    }
}
