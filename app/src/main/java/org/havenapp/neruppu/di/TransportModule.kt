package org.havenapp.neruppu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.havenapp.neruppu.domain.di.MatrixTransport
import org.havenapp.neruppu.domain.di.TelegramTransport
import org.havenapp.neruppu.data.matrix.MatrixAlertTransport
import org.havenapp.neruppu.data.matrix.MatrixConfigStore
import org.havenapp.neruppu.data.telegram.TelegramAlertTransport
import org.havenapp.neruppu.data.telegram.TelegramConfigStore
import org.havenapp.neruppu.data.storage.MediaStorageRepositoryImpl
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
import org.havenapp.neruppu.domain.repository.TelegramConfigRepository
import org.havenapp.neruppu.domain.repository.MediaStorageRepository
import org.havenapp.neruppu.domain.transport.AlertTransport
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TransportModule {

    @Binds
    @Singleton
    abstract fun bindMatrixConfig(
        impl: MatrixConfigStore
    ): MatrixConfigRepository

    @Binds
    @Singleton
    abstract fun bindTelegramConfig(
        impl: TelegramConfigStore
    ): TelegramConfigRepository

    @Binds
    @Singleton
    @MatrixTransport
    abstract fun bindMatrixTransport(
        impl: MatrixAlertTransport
    ): AlertTransport

    @Binds
    @Singleton
    @TelegramTransport
    abstract fun bindTelegramTransport(
        impl: TelegramAlertTransport
    ): AlertTransport

    @Binds
    @IntoSet
    abstract fun bindMatrixTransportIntoSet(
        impl: MatrixAlertTransport
    ): AlertTransport

    @Binds
    @IntoSet
    abstract fun bindTelegramTransportIntoSet(
        impl: TelegramAlertTransport
    ): AlertTransport

    @Binds
    @Singleton
    abstract fun bindMediaStorage(
        impl: MediaStorageRepositoryImpl
    ): MediaStorageRepository
}
