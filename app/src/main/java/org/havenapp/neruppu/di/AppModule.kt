package org.havenapp.neruppu.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.havenapp.neruppu.data.local.NeruppuDatabase
import org.havenapp.neruppu.data.local.dao.EventDao
import org.havenapp.neruppu.data.repository.SensorRepositoryImpl
import org.havenapp.neruppu.data.camera.CameraManager
import org.havenapp.neruppu.domain.repository.SensorRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCameraManager(@ApplicationContext context: Context): CameraManager {
        return CameraManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NeruppuDatabase {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_timestamp ON events (timestamp)")
            }
        }

        return Room.databaseBuilder(
            context,
            NeruppuDatabase::class.java,
            "neruppu_db"
        )
        .addMigrations(MIGRATION_2_3)
        .build()
    }

    @Provides
    @Singleton
    fun provideEventDao(db: NeruppuDatabase): EventDao {
        return db.eventDao
    }

    @Provides
    @Singleton
    fun provideSensorRepository(
        @ApplicationContext context: Context,
        eventDao: EventDao
    ): SensorRepository {
        return SensorRepositoryImpl(context, eventDao)
    }
}
