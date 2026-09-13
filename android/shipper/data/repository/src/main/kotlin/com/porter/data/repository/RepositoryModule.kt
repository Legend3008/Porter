package com.porter.data.repository

import com.porter.domain.repository.AuthRepository
import com.porter.domain.repository.BookingRepository
import com.porter.domain.repository.DocumentRepository
import com.porter.domain.repository.NotificationRepository
import com.porter.domain.repository.PaymentRepository
import com.porter.domain.repository.TrackingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryMockImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        impl: BookingRepositoryMockImpl
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindTrackingRepository(
        impl: TrackingRepositoryMockImpl
    ): TrackingRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryMockImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryMockImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: NotificationRepositoryMockImpl
    ): NotificationRepository
}
