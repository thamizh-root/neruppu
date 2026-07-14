/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.havenapp.neruppu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    @Singleton
    abstract fun bindMediaStorage(
        impl: MediaStorageRepositoryImpl
    ): MediaStorageRepository
}
