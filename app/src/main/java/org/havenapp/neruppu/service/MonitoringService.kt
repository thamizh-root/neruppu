package org.havenapp.neruppu.service

import android.graphics.Bitmap
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.camera.core.Preview
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.havenapp.neruppu.R
import org.havenapp.neruppu.data.audio.AudioRecorder
import org.havenapp.neruppu.data.camera.CameraManager
import org.havenapp.neruppu.data.sensors.*
import org.havenapp.neruppu.domain.model.Event
import org.havenapp.neruppu.domain.model.SensorType
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.worker.EventNotificationWorker
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : LifecycleService() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var cameraManager: CameraManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var sensorsJob: Job? = null
    private var heartbeatJob: Job? = null
    
    private lateinit var accelerometerDriver: AccelerometerDriver
    private lateinit var microphoneDriver: MicrophoneDriver
    private lateinit var lightSensorDriver: LightSensorDriver
    private lateinit var significantMotionDriver: SignificantMotionDriver
    private lateinit var audioRecorder: AudioRecorder

    private val _motionLevel = MutableStateFlow(0.0)
    val motionLevel: StateFlow<Double> = _motionLevel

    private val _differenceMap = MutableStateFlow<Bitmap?>(null)
    val differenceMap: StateFlow<Bitmap?> = _differenceMap

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    private val _lightLevel = MutableStateFlow(0f)
    val lightLevel: StateFlow<Float> = _lightLevel

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    inner class LocalBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }

    private val binder = LocalBinder()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "motion_sensitivity", "use_front_camera" -> {
                Log.d("MonitoringService", "Prefs changed ($key), re-binding camera...")
                startMonitoring() // Re-bind with new settings
            }
            "audio_threshold" -> {
                Log.d("MonitoringService", "Prefs changed ($key), updating audio threshold...")
                startMicrophoneMonitoring()
            }
        }
    }

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
        Log.d("MonitoringService", "onCreate [PID: ${Process.myPid()}]")
        
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("MonitoringService", "Lifecycle event: $event")
            }
        })

        createNotificationChannel()
        updateNotification()

        accelerometerDriver = AccelerometerDriver(this)
        microphoneDriver = MicrophoneDriver()
        lightSensorDriver = LightSensorDriver(this)
        significantMotionDriver = SignificantMotionDriver(this)
        audioRecorder = AudioRecorder(this)
        
        getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)

        acquireWakeLock()
        startHeartbeat()
        // Sensors will be started based on isMonitoring or UI visibility
        startMonitoringIfNeeded()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                Log.d("MonitoringService", "HEARTBEAT - Monitoring: ${_isMonitoring.value}, UI: ${_uiActive.value}, PID: ${Process.myPid()}")
                delay(10000)
            }
        }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Neruppu:MonitoringWakeLock")
        wakeLock?.acquire()
        Log.d("MonitoringService", "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("MonitoringService", "WakeLock released")
        }
    }

    private fun startMonitoringIfNeeded() {
        // Only start sensors if we are monitoring or if someone is listening (e.g. Dashboard)
        if (_isMonitoring.value || _uiActive.value) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    private val _uiActive = MutableStateFlow(false)
    fun setUiActive(active: Boolean) {
        _uiActive.value = active
        startMonitoringIfNeeded()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("MonitoringService", "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                Log.i("MonitoringService", "Stopping service via notification action")
                stopSelf()
            }
            "TRIGGER_MOTION_EVENT" -> {
                val level = intent.getDoubleExtra("motion_level", 0.0)
                serviceScope.launch {
                    handleEvent(SensorType.CAMERA_MOTION, "Camera motion detected: Level ${String.format("%.2f", level)}")
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
        updateNotification()
        startMonitoringIfNeeded()
    }

    private fun stopMonitoring() {
        Log.d("MonitoringService", "Stopping Sensors...")
        cameraManager.unbind()
        microphoneJob?.cancel()
        microphoneJob = null
        // Also stop other sensor collectors if they are heavy
        // For now, these flows will just be collected if active
    }


    private fun startMonitoring() {
        Log.d("MonitoringService", "startMonitoring() called - Monitoring: ${_isMonitoring.value}, UI: ${_uiActive.value}")
        
        val useFront = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getBoolean("use_front_camera", false)
        val sensitivity = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("motion_sensitivity", 15f)

        // Camera Motion Monitoring
        cameraManager.bindCamera(
            lifecycleOwner = this,
            useFrontCamera = useFront,
            surfaceProvider = if (_uiActive.value) cameraManager.currentSurfaceProvider else null,
            onMotionDetected = { level ->
                _motionLevel.value = level
                _differenceMap.value = cameraManager.getDifferenceMap()?.value
                if (_isMonitoring.value && level > sensitivity.toDouble()) {
                    Log.d("MonitoringService", "THRESHOLD EXCEEDED: $level")
                    serviceScope.launch {
                        handleEvent(SensorType.CAMERA_MOTION, "Camera motion detected: Level ${String.format("%.2f", level)}")
                    }
                }
            }
        )

        if (sensorsJob?.isActive == true) {
            Log.d("MonitoringService", "Sensors already running. Skipping re-initialization.")
            return
        }

        sensorsJob = serviceScope.launch {
            Log.d("MonitoringService", "Launching sensor flows...")
            
            val accelerometerFlow = accelerometerDriver.observeMotion()
                .conflate()
                .onEach { magnitude ->
                    if (_isMonitoring.value) {
                        Log.d("MonitoringService", "Motion event received")
                        serviceScope.launch {
                            handleEvent(SensorType.ACCELEROMETER, "Physical motion detected: Magnitude $magnitude")
                        }
                    }
                }

            val lightFlow = lightSensorDriver.observeLightChanges()
                .conflate()
                .onEach { lux ->
                    _lightLevel.value = lux
                    if (_isMonitoring.value) {
                        Log.d("MonitoringService", "Light event received")
                        serviceScope.launch {
                            handleEvent(SensorType.LIGHT, "Light change detected: $lux lux")
                        }
                    }
                }

            launch { accelerometerFlow.collect() }
            launch { lightFlow.collect() }
            
            // Significant Motion Monitoring (Ultra-low power wakeups)
            launch {
                significantMotionDriver.observeSignificantMotion().collect {
                    if (_isMonitoring.value) {
                        handleEvent(SensorType.ACCELEROMETER, "Significant motion detected (hardware trigger)")
                    }
                }
            }
            
            startMicrophoneMonitoring()
        }
    }

    private var audioBaseline = 0f
    private val baselineAlpha = 0.05f

    private fun startMicrophoneMonitoring() {
        microphoneJob?.cancel()
        
        val threshold = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("audio_threshold", 800f)

        microphoneJob = serviceScope.launch {
            microphoneDriver.observeNoise()
                .conflate()
                .collect { amplitude ->
                    _audioLevel.value = amplitude.toFloat()
                    
                    if (audioBaseline == 0f) audioBaseline = amplitude.toFloat()
                    audioBaseline = (1f - baselineAlpha) * audioBaseline + baselineAlpha * amplitude

                    if (_isMonitoring.value && !isRecordingAudio && amplitude > audioBaseline + threshold) {
                        Log.i("MonitoringService", "AUDIO TRIGGER: $amplitude (Baseline: $audioBaseline, Threshold: $threshold)")
                        isRecordingAudio = true // SET IMMEDIATELY to prevent multiple triggers
                        serviceScope.launch {
                            val eventId = handleEvent(SensorType.MICROPHONE, "Acoustic event detected: Amplitude $amplitude")
                            startAudioRecording(eventId)
                        }
                    }
                }
        }
    }

    private fun startAudioRecording(eventId: Long = -1) {
        // isRecordingAudio is already set to true in the trigger block
        
        // STOP monitoring while recording to avoid mic conflict
        microphoneJob?.cancel()
        microphoneJob = null
        
        val captureDuration = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("audio_capture_duration", 5f).toLong() * 1000

        serviceScope.launch {
            try {
                Log.d("MonitoringService", "Capturing ${captureDuration/1000}s audio clip of the event...")
                audioRecorder.startRecording()
                delay(captureDuration)
                val uri = audioRecorder.stopRecording()
                if (uri != null) {
                    Log.i("MonitoringService", "AUDIO CLIP SAVED: $uri")
                    if (eventId != -1L) {
                        sensorRepository.updateEventAudio(eventId, uri.toString())
                        Log.d("MonitoringService", "Event $eventId updated with audio URI")
                    }
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

    private suspend fun handleEvent(type: SensorType, description: String): Long {
        val now = System.currentTimeMillis()
        val lastTime = lastEventTime[type] ?: 0L
        
        // Cooldown: 3 seconds for the same type, unless it's a microphone event which has its own logic
        if (type != SensorType.MICROPHONE && now - lastTime < 3000) {
            Log.d("MonitoringService", "Event suppressed by cooldown: $type")
            return -1
        }
        
        lastEventTime[type] = now

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
        val id = sensorRepository.saveEvent(event)
        
        // Schedule deferred notification via WorkManager (Best Practice)
        val workData = Data.Builder()
            .putString("event_type", type.name)
            .putString("description", description)
            .build()
        
        val notificationWork = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInputData(workData)
            .build()
            
        WorkManager.getInstance(this).enqueue(notificationWork)

        Log.i("MonitoringService", "EVENT SAVED TO DATABASE: $type (ID: $id)")
        return id
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

    private fun updateNotification() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or 
                       ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, MonitoringService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (_isMonitoring.value) {
            "System is active and monitoring sensors"
        } else {
            "System is idle. Tap START to begin monitoring."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neruppu Security")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_security)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        Log.d("MonitoringService", "onDestroy [PID: ${Process.myPid()}]")
        super.onDestroy()
        getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        releaseWakeLock()
        heartbeatJob?.cancel()
        sensorsJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "monitoring_channel"
        const val ACTION_STOP_SERVICE = "org.havenapp.neruppu.STOP_SERVICE"
    }
}
