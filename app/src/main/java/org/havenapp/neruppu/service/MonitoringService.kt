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

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

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

    private var audioBaseline = 0f
    private val baselineAlpha = 0.05f // EMA factor for smoothing baseline

    private fun startMicrophoneMonitoring() {
        val audioSensitivity = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("audio_threshold", 800f)
            
        microphoneJob?.cancel()
        microphoneJob = microphoneDriver.observeNoise()
            .onEach { amplitude ->
                val ampFloat = amplitude.toFloat()
                
                // 1. Update Baseline (Exponential Moving Average)
                // This establishes the "noise floor" of the room
                if (audioBaseline == 0f) {
                    audioBaseline = ampFloat
                } else if (ampFloat < audioBaseline * 1.5f) { // Only update baseline with quiet sounds
                    audioBaseline = (baselineAlpha * ampFloat) + (1 - baselineAlpha) * audioBaseline
                }
                
                _audioLevel.value = ampFloat

                // 2. Trigger Check (Relative to Baseline)
                // Haven standard: trigger = current > baseline + relative_threshold
                if (_isMonitoring.value) {
                    if (ampFloat > audioBaseline + audioSensitivity) {
                        if (!isRecordingAudio) {
                            Log.i("MonitoringService", "Triggering relative audio recording: $ampFloat > ${audioBaseline + audioSensitivity}")
                            handleEvent(SensorType.MICROPHONE, "Sudden sound detected: +${(ampFloat - audioBaseline).toInt()} over baseline")
                            startAudioRecording()
                        } else {
                            lastNoiseTime = System.currentTimeMillis()
                        }
                    }
                }
            }.launchIn(serviceScope)
    }

    private fun startAudioRecording() {
        serviceScope.launch {
            if (isRecordingAudio) return@launch
            try {
                isRecordingAudio = true
                lastNoiseTime = System.currentTimeMillis()
                
                Log.d("MonitoringService", "Pausing noise observation to record audio...")
                microphoneJob?.cancel()
                delay(500) // Increase delay to ensure hardware is released
                
                val audioFile = audioRecorder.startRecording()
                if (audioFile != null) {
                    Log.i("MonitoringService", "AUDIO RECORDING STARTED: ${audioFile.name}")
                    
                    val startTime = System.currentTimeMillis()
                    // Adaptive stop: stop if quiet for 3s OR max 10s reached
                    while (System.currentTimeMillis() - lastNoiseTime < 3000 && 
                           System.currentTimeMillis() - startTime < 10000) {
                        delay(500)
                        val amplitude = audioRecorder.getMaxAmplitude()
                        if (amplitude > 500) { 
                            lastNoiseTime = System.currentTimeMillis()
                        }
                    }
                    
                    val uri = audioRecorder.stopRecording()
                    Log.i("MonitoringService", "AUDIO RECORDING STOPPED: $uri")
                    
                    // Crucial: Only save if we actually recorded something significant
                    // to prevent "adding by itself" loops
                    sensorRepository.saveEvent(
                        Event(
                            sensorType = SensorType.MICROPHONE,
                            description = "Sound detected and recorded",
                            mediaUri = uri?.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("MonitoringService", "Audio recording failed", e)
            } finally {
                isRecordingAudio = false
                Log.d("MonitoringService", "Resuming noise observation after 1s cooldown...")
                delay(1000) // Add a cooldown to let the baseline stabilize
                startMicrophoneMonitoring()
            }
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
