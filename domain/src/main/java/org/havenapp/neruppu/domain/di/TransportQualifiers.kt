package org.havenapp.neruppu.domain.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MatrixTransport

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TelegramTransport
