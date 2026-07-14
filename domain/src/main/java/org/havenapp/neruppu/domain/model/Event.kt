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

package org.havenapp.neruppu.domain.model

import java.time.Instant

enum class UploadStatus {
    PENDING,
    UPLOADED,
    FAILED
}

data class Event(
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val sensorType: SensorType,
    val description: String,
    val mediaUri: String? = null,
    val audioUri: String? = null,
    val uploadStatus: UploadStatus = UploadStatus.PENDING,
    val uploadTarget: String? = null,
    val uploadedAt: Long? = null,
    val failureReason: String? = null
)

enum class SensorType {
    ACCELEROMETER,
    MICROPHONE,
    LIGHT,
    BAROMETER,
    POWER,
    CAMERA_MOTION
}
