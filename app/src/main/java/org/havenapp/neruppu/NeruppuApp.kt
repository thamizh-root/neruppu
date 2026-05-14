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
