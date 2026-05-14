package org.havenapp.neruppu.data.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) { 
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                    coerceInputValues = true
                }) 
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("HttpClient", message)
                    }
                }
                level = LogLevel.ALL
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
            }
        }
    }
}
