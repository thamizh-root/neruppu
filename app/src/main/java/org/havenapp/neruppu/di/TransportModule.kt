package org.havenapp.neruppu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.havenapp.neruppu.data.matrix.MatrixAlertTransport
import org.havenapp.neruppu.data.matrix.MatrixConfigStore
import org.havenapp.neruppu.data.storage.MediaStorageRepositoryImpl
import org.havenapp.neruppu.domain.repository.MatrixConfigRepository
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
    abstract fun bindAlertTransport(
        impl: MatrixAlertTransport
    ): AlertTransport

    @Binds
    @Singleton
    abstract fun bindMediaStorage(
        impl: MediaStorageRepositoryImpl
    ): MediaStorageRepository
}
