package org.havenapp.neruppu.data.local

import androidx.room.TypeConverter
import org.havenapp.neruppu.domain.model.SensorType
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromSensorType(value: String?): SensorType? {
        return value?.let {
            runCatching { SensorType.valueOf(it) }.getOrElse { SensorType.ACCELEROMETER }
        }
    }

    @TypeConverter
    fun sensorTypeToString(sensorType: SensorType?): String? {
        return sensorType?.name
    }
}
