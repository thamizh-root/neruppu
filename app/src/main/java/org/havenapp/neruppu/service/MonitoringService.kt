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
import org.havenapp.neruppu.BuildConfig
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
import org.havenapp.neruppu.domain.model.SensorEvent
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.domain.usecase.HandleSensorEventUseCase
import org.havenapp.neruppu.worker.EventNotificationWorker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : LifecycleService() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var handleSensorEventUseCase: HandleSensorEventUseCase

    @Inject
    lateinit var cameraManager: CameraManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

    private var prefChangeJob: Job? = null
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        prefChangeJob?.cancel()
        prefChangeJob = serviceScope.launch {
            delay(500) // debounce
            when (key) {
                "motion_sensitivity", "use_front_camera" -> {
                    Log.d("MonitoringService", "Prefs changed ($key), re-binding camera...")
                    updatePrefValues()
                    startMonitoring()
                }
                "audio_threshold", "save_photos" -> {
                    Log.d("MonitoringService", "Prefs changed ($key), updating values...")
                    updatePrefValues()
                    if (key == "audio_threshold") startMicrophoneMonitoring()
                }
            }
        }
    }

    private var savePhotosPref = true
    private var sensitivityPref = 15f
    private var useFrontCameraPref = false
    private var audioThresholdPref = 200f

    private fun updatePrefValues() {
        val prefs = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
        savePhotosPref = prefs.getBoolean("save_photos", true)
        sensitivityPref = prefs.getFloat("motion_sensitivity", 15f)
        useFrontCameraPref = prefs.getBoolean("use_front_camera", false)
        audioThresholdPref = prefs.getFloat("audio_threshold", 200f)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return binder
    }

    private var isRecordingAudio = AtomicBoolean(false)
    private var lastNoiseTime = 0L
    private var lastEventTime = ConcurrentHashMap<SensorType, Long>()
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
        
        updatePrefValues()
        getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)

        acquireWakeLock()
        startHeartbeat()
        // Sensors will be started based on isMonitoring or UI visibility
        startMonitoringIfNeeded()
    }

    private fun startHeartbeat() {
        if (!BuildConfig.DEBUG) return
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
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minute timeout
        Log.d("MonitoringService", "WakeLock acquired with 10m timeout")
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("MonitoringService", "WakeLock released")
        }
    }

    private fun startMonitoringIfNeeded() {
        // We need the service to be "active" (startMonitoring) if:
        // 1. We are actually monitoring (background or foreground)
        // 2. The UI is active and needs a preview/sensor levels
        if (_isMonitoring.value || _uiActive.value) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    private val _uiActive = MutableStateFlow(false)
    fun setUiActive(active: Boolean) {
        _uiActive.value = active
        if (active) {
            startMonitoring()
        } else {
            startMonitoringIfNeeded()
        }
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
            // Ensure service is running as foreground when monitoring is ON
            updateNotification()
        } else {
            Log.d("MonitoringService", "Monitoring DEACTIVATED")
            // When monitoring is OFF, we might still be in foreground if UI is active
            // but updateNotification will handle the startForeground/stopForeground logic
            updateNotification()
        }
        startMonitoringIfNeeded()
    }

    private fun stopMonitoring() {
        Log.d("MonitoringService", "stopMonitoring() called - Monitoring: ${_isMonitoring.value}, UI: ${_uiActive.value}")
        
        // ONLY unbind camera if NO ONE needs it (not monitoring AND UI is not visible)
        if (!_isMonitoring.value && !_uiActive.value) {
            Log.d("MonitoringService", "Releasing camera system (Idle & UI hidden)")
            cameraManager.unbind()
        } else {
            Log.d("MonitoringService", "Keeping camera bound for Preview/UI")
        }

        microphoneJob?.cancel()
        microphoneJob = null
        sensorsJob?.cancel()
        sensorsJob = null
    }


    private fun startMonitoring() {
        Log.d("MonitoringService", "startMonitoring() called - Monitoring: ${_isMonitoring.value}, UI: ${_uiActive.value}")
        
        // Always bind camera if we are either Monitoring OR UI is active
        if (_isMonitoring.value || _uiActive.value) {
            cameraManager.bindCamera(
                lifecycleOwner = this,
                useFrontCamera = useFrontCameraPref,
                surfaceProvider = cameraManager.currentSurfaceProvider,
                sensitivity = (sensitivityPref * 30).toInt().coerceIn(5, 30),
                onMotionDetected = { level ->
                    _motionLevel.value = level
                    _differenceMap.value = cameraManager.getDifferenceMap()?.value
                    if (_isMonitoring.value && level > 15.0) { // Using 15% as default percentage trigger
                        Log.d("MonitoringService", "THRESHOLD EXCEEDED: $level")
                        serviceScope.launch {
                            handleEvent(SensorType.CAMERA_MOTION, "Camera motion detected: Level ${String.format("%.2f", level)}")
                        }
                    }
                }
            )
        }

        if (sensorsJob?.isActive == true) {
            Log.d("MonitoringService", "Sensors already running. Skipping re-initialization.")
            return
        }
        
        // Start sensor data flows only if we are monitoring OR UI is active (for real-time dashboard updates)
        if (_isMonitoring.value || _uiActive.value) {
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
    }

    private var fastBaseline = 0f
    private var slowBaseline = 0f

    private fun startMicrophoneMonitoring() {
        microphoneJob?.cancel()
        
        microphoneJob = serviceScope.launch {
            microphoneDriver.observeNoise()
                .conflate()
                .collect { amplitude ->
                    _audioLevel.value = amplitude.toFloat()
                    
                    val amp = amplitude.toFloat()
                    if (fastBaseline == 0f) {
                        fastBaseline = amp
                        slowBaseline = amp
                    }
                    
                    // Dual EMA: Fast adapts in ~1s, Slow adapts in ~200s
                    fastBaseline = 0.9f * fastBaseline + 0.1f * amp
                    slowBaseline = 0.995f * slowBaseline + 0.005f * amp
                    
                    val spike = fastBaseline - slowBaseline
                    val threshold = audioThresholdPref // default 200

                    if (_isMonitoring.value && isRecordingAudio.compareAndSet(false, true) && spike > threshold) {
                        Log.i("MonitoringService", "AUDIO TRIGGER (SPIKE): $spike (Threshold: $threshold)")
                        serviceScope.launch {
                            val eventId = handleEvent(SensorType.MICROPHONE, "Acoustic spike detected: $spike")
                            startAudioRecording(eventId)
                        }
                    }
                }
        }
    }

    private fun startAudioRecording(eventId: Long = -1) {
        // isRecordingAudio is already set to true in the trigger block
        
        // DO NOT stop monitoring while recording - suppression handled by isRecordingAudio flag
        
        val captureDuration = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
            .getFloat("audio_capture_duration", 5f).toLong() * 1000

        serviceScope.launch {
            try {
                Log.d("MonitoringService", "Capturing ${captureDuration/1000}s audio clip of the event...")
                val audioFile = audioRecorder.startRecording()
                delay(captureDuration)
                val uri = audioRecorder.stopRecording()
                if (uri != null && audioFile != null) {
                    Log.i("MonitoringService", "AUDIO CLIP CAPTURED: $uri")
                    
                    val sensorEvent = SensorEvent(
                        sensorType = SensorType.MICROPHONE,
                        description = "Acoustic event recording",
                        timestamp = System.currentTimeMillis(),
                        audioFile = audioFile
                    )
                    handleSensorEventUseCase.execute(sensorEvent)
                }
            } catch (e: Exception) {
                Log.e("MonitoringService", "Audio recording failed", e)
            } finally {
                isRecordingAudio.set(false)
                Log.d("MonitoringService", "Resetting audio recording flag.")
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
        
        // 1. Capture photo bytes if possible
        val imageBytes: ByteArray? = if (savePhotosPref) {
            try {
                Log.d("MonitoringService", "Attempting to capture photo for event...")
                val uri = cameraManager.capturePhoto()
                withContext(Dispatchers.IO) {
                    uri?.let { contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() } }
                }
            } catch (e: Exception) {
                Log.e("MonitoringService", "CRITICAL FAILURE: Photo capture threw exception", e)
                null
            }
        } else null

        // 2. Prepare SensorEvent for Use Case
        val sensorEvent = SensorEvent(
            sensorType = type,
            description = description,
            timestamp = now,
            imageBytes = imageBytes
        )

        // 3. Orchestrate via Use Case (saves locally + logs in DB + sends Matrix alert)
        Log.d("MonitoringService", "Calling HandleSensorEventUseCase for $type")
        val result = handleSensorEventUseCase.execute(sensorEvent)
        val id = result.getOrDefault(-1L)
        
        if (result.isFailure) {
            Log.e("MonitoringService", "UseCase failed", result.exceptionOrNull())
        }
        
        // Schedule deferred notification via WorkManager (Best Practice)
        val workData = Data.Builder()
            .putString("event_type", type.name)
            .putString("description", description)
            .build()
        
        val notificationWork = OneTimeWorkRequestBuilder<EventNotificationWorker>()
            .setInputData(workData)
            .build()
            
        WorkManager.getInstance(this).enqueueUniqueWork(
            "security_notification",
            androidx.work.ExistingWorkPolicy.REPLACE,
            notificationWork
        )

        Log.i("MonitoringService", "EVENT PROCESSED: $type (Local ID: $id)")
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
        if (_isMonitoring.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                           ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                startForeground(NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            // Still show a simple notification if UI is active? 
            // Or just remove it. Let's remove it for now when not monitoring.
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
