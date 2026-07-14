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

package org.havenapp.neruppu

import android.app.Application
import android.os.StrictMode
import androidx.camera.core.CameraXConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeruppuApp : Application(), CameraXConfig.Provider {
    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            // StrictMode is highly recommended for identifying UI thread blocks and leaks
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(androidx.camera.camera2.Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(android.util.Log.ERROR)
            .build()
    }
}
