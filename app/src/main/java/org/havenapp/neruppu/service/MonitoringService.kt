package org.havenapp.neruppu.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.havenapp.neruppu.data.sensors.AccelerometerDriver
import org.havenapp.neruppu.data.sensors.MicrophoneDriver
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.repository.SensorRepository
import javax.inject.Inject

import org.havenapp.neruppu.data.sensors.LightSensorDriver
import org.havenapp.neruppu.data.camera.CameraManager
import org.havenapp.neruppu.data.audio.AudioRecorder

import android.os.Binder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.camera.core.Preview

@AndroidEntryPoint
class MonitoringService : LifecycleService() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var cameraManager: CameraManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var accelerometerDriver: AccelerometerDriver
    private lateinit var microphoneDriver: MicrophoneDriver
    private lateinit var lightSensorDriver: LightSensorDriver
    private lateinit var audioRecorder: AudioRecorder

    private val _motionLevel = MutableStateFlow(0.0)
    val motionLevel: StateFlow<Double> = _motionLevel

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    inner class LocalBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    private var isRecordingAudio = false
    private var lastNoiseTime = 0L
    private var lastEventTime = mutableMapOf<SensorType, Long>()
    private var microphoneJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("MonitoringService", "onCreate")
        accelerometerDriver = AccelerometerDriver(this)
        microphoneDriver = MicrophoneDriver()
        lightSensorDriver = LightSensorDriver(this)
        audioRecorder = AudioRecorder(this)
        createNotificationChannel()
        
        // Always start monitoring on create for this app
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("MonitoringService", "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            "TRIGGER_MOTION_EVENT" -> {
                val level = intent.getDoubleExtra("motion_level", 0.0)
                handleEvent(SensorType.CAMERA_MOTION, "Camera motion detected: Level ${String.format("%.2f", level)}")
            }
            "RECLAIM_CAMERA" -> {
                // No-op now as service always owns it, but kept for compatibility
                Log.d("MonitoringService", "Reclaim camera requested (No-op, Service already owns it)")
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or 
                               ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    startForeground(NOTIFICATION_ID, createNotification(), type)
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            }
        }
        
        return START_STICKY
    }

    fun setPreviewSurface(surfaceProvider: Preview.SurfaceProvider?) {
        cameraManager.setPreviewSurface(surfaceProvider)
    }

    fun toggleMonitoring() {
        _isMonitoring.value = !_isMonitoring.value
        if (_isMonitoring.value) {
            Log.d("MonitoringService", "Monitoring ACTIVATED")
        } else {
            Log.d("MonitoringService", "Monitoring DEACTIVATED")
        }
    }

    private fun startMonitoring() {
        Log.d("MonitoringService", "Initializing Camera and Sensors...")
        
        val useFront = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getBoolean("use_front_camera", false)
        val sensitivity = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("motion_sensitivity", 15f)

        // Camera Motion Monitoring - ALWAYS ON (Service owns it)
        cameraManager.bindCamera(
            lifecycleOwner = this,
            useFrontCamera = useFront,
            onMotionDetected = { level ->
                _motionLevel.value = level
                if (_isMonitoring.value && level > sensitivity.toDouble()) {
                    Log.d("MonitoringService", "THRESHOLD EXCEEDED: $level")
                    handleEvent(SensorType.CAMERA_MOTION, "Camera motion detected: Level ${String.format("%.2f", level)}")
                }
            }
        )

        accelerometerDriver.observeMotion()
            .onEach { magnitude ->
                if (_isMonitoring.value) {
                    Log.d("MonitoringService", "Motion event received")
                    handleEvent(SensorType.ACCELEROMETER, "Physical motion detected: Magnitude $magnitude")
                }
            }.launchIn(serviceScope)

        startMicrophoneMonitoring()

        lightSensorDriver.observeLightChanges()
            .onEach { lux ->
                if (_isMonitoring.value) {
                    Log.d("MonitoringService", "Light event received")
                    handleEvent(SensorType.LIGHT, "Light change detected: $lux lux")
                }
            }.launchIn(serviceScope)
    }

    private fun startMicrophoneMonitoring() {
        microphoneJob?.cancel()
        microphoneJob = microphoneDriver.observeNoise()
            .onEach { amplitude ->
                if (_isMonitoring.value) {
                    if (!isRecordingAudio) {
                        startAudioRecording()
                    } else {
                        lastNoiseTime = System.currentTimeMillis()
                    }
                }
            }.launchIn(serviceScope)
    }

    private fun startAudioRecording() {
        serviceScope.launch {
            isRecordingAudio = true
            lastNoiseTime = System.currentTimeMillis()
            
            // Stop monitoring to free up the MIC
            microphoneJob?.cancel()
            
            val audioFile = audioRecorder.startRecording()
            if (audioFile != null) {
                Log.d("MonitoringService", "Audio recording started")
                
                // Monitor for silence
                while (System.currentTimeMillis() - lastNoiseTime < 5000) {
                    delay(1000)
                    val amplitude = audioRecorder.getMaxAmplitude()
                    if (amplitude > 1000) { // Lowered threshold for silence
                        lastNoiseTime = System.currentTimeMillis()
                    }
                }
                
                val uri = audioRecorder.stopRecording()
                Log.d("MonitoringService", "Audio recording stopped, saving event")
                
                sensorRepository.saveEvent(
                    Event(
                        sensorType = SensorType.MICROPHONE,
                        description = "Noise detected and recorded",
                        mediaUri = uri?.toString()
                    )
                )
            }
            
            isRecordingAudio = false
            // Restart monitoring
            startMicrophoneMonitoring()
        }
    }

    private fun handleEvent(type: SensorType, description: String) {
        val now = System.currentTimeMillis()
        val lastTime = lastEventTime[type] ?: 0L
        
        // Cooldown: 3 seconds for the same type, unless it's a microphone event which has its own logic
        if (type != SensorType.MICROPHONE && now - lastTime < 3000) {
            Log.d("MonitoringService", "Event suppressed by cooldown: $type")
            return
        }
        
        lastEventTime[type] = now

        serviceScope.launch {
            Log.i("MonitoringService", ">>> TRIGGERING EVENT: $type - $description")
            
            val mediaUri = try { 
                Log.d("MonitoringService", "Attempting to capture photo for event...")
                val uri = cameraManager.capturePhoto()
                if (uri != null) {
                    Log.i("MonitoringService", "PHOTO CAPTURED: $uri")
                } else {
                    Log.w("MonitoringService", "PHOTO CAPTURE RETURNED NULL")
                }
                uri?.toString()
            } catch (e: Exception) { 
                Log.e("MonitoringService", "CRITICAL FAILURE: Photo capture threw exception", e)
                null 
            }

            val event = Event(
                sensorType = type,
                description = description,
                mediaUri = mediaUri,
            )
            sensorRepository.saveEvent(event)
            Log.i("MonitoringService", "EVENT SAVED TO DATABASE: $type")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Monitoring Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neruppu Monitoring")
            .setContentText("Sensors are active and protecting your device.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: CameraManager unbind is handled by lifecycleOwner (this)
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "monitoring_channel"
    }
}
