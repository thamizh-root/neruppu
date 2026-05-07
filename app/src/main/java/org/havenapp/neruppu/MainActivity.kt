package org.havenapp.neruppu

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.havenapp.neruppu.core.ui.theme.NeruppuTheme
import org.havenapp.neruppu.data.camera.CameraManager
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.service.MonitoringService
import org.havenapp.neruppu.ui.features.dashboard.DashboardScreen
import org.havenapp.neruppu.ui.features.logs.LogsScreen
import org.havenapp.neruppu.ui.features.logs.LogsViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var cameraManager: CameraManager

    private var monitoringService = mutableStateOf<MonitoringService?>(null)
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.LocalBinder
            monitoringService.value = binder.getService()
            isBound = true
            Log.d("MainActivity", "Bound to MonitoringService")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService.value = null
            isBound = false
            Log.d("MainActivity", "Disconnected from MonitoringService")
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> 
        // Start service after permissions granted
        val intent = Intent(this, MonitoringService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
        requestIgnoreBatteryOptimizations()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            val intent = Intent(this, MonitoringService::class.java)
            startForegroundService(intent)
            bindService(intent, connection, BIND_AUTO_CREATE)
            requestIgnoreBatteryOptimizations()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        setContent {
            NeruppuTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val logsViewModel: LogsViewModel = remember { LogsViewModel(sensorRepository) }
                val prefs = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                val stealthMode = prefs.getBoolean("stealth_mode", false)

                Scaffold(
                    containerColor = Color.Transparent, // Ensure background shows through
                    bottomBar = {
                        if (!stealthMode) {
                            NavigationBar(
                                containerColor = Color.Black.copy(alpha = 0.8f),
                                contentColor = Color.White
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                    label = { Text("Dashboard") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                                    label = { Text("Logs") }
                                )
                            }
                        }
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            0 -> {
                                val service = monitoringService.value
                                if (service != null) {
                                    val isMonitoring by service.isMonitoring.collectAsState()
                                    val motionLevel by service.motionLevel.collectAsState()
                                    val motionGrid by service.motionGrid.collectAsState()
                                    val audioLevel by service.audioLevel.collectAsState()
                                    
                                    val prefs = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                    val useFrontCamera = prefs.getBoolean("use_front_camera", false)
                                    val stealthMode = prefs.getBoolean("stealth_mode", false)
                                    val motionSensitivity = prefs.getFloat("motion_sensitivity", 15f)
                                    val audioSensitivity = prefs.getFloat("audio_threshold", 800f)
                                    val captureDuration = prefs.getFloat("audio_capture_duration", 5f)

                                    val motionHistory = remember { mutableStateListOf<Float>() }
                                    LaunchedEffect(motionLevel) {
                                        motionHistory.add(motionLevel.toFloat())
                                        if (motionHistory.size > 100) motionHistory.removeAt(0)
                                    }

                                    DashboardScreen(
                                        isMonitoring = isMonitoring,
                                        useFrontCamera = useFrontCamera,
                                        stealthMode = stealthMode,
                                        motionLevel = motionLevel,
                                        audioLevel = audioLevel,
                                        motionSensitivity = motionSensitivity,
                                        audioSensitivity = audioSensitivity,
                                        captureDuration = captureDuration,
                                        motionHistory = motionHistory,
                                        motionGrid = motionGrid,
                                        onSensitivityChange = { sensitivity ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putFloat("motion_sensitivity", sensitivity)
                                                .apply()
                                        },
                                        onAudioSensitivityChange = { sensitivity ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putFloat("audio_threshold", sensitivity)
                                                .apply()
                                            // Tell service to update threshold
                                            val intent = Intent(this@MainActivity, MonitoringService::class.java)
                                            startForegroundService(intent)
                                        },
                                        onCaptureDurationChange = { duration ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putFloat("audio_capture_duration", duration)
                                                .apply()
                                        },
                                        onToggleCamera = { useFront ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("use_front_camera", useFront)
                                                .apply()
                                            val intent = Intent(this@MainActivity, MonitoringService::class.java)
                                            startForegroundService(intent)
                                        },
                                        onToggleStealthMode = { stealth: Boolean ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("stealth_mode", stealth)
                                                .apply()
                                        },
                                        onToggleMonitoring = { 
                                            service.toggleMonitoring()
                                        },
                                        onBindCamera = { previewView, _ ->
                                            service.setPreviewSurface(previewView.surfaceProvider)
                                        }
                                    )
                                    
                                    val lifecycleOwner = LocalLifecycleOwner.current
                                    DisposableEffect(lifecycleOwner) {
                                        val observer = LifecycleEventObserver { _, event ->
                                            when (event) {
                                                Lifecycle.Event.ON_RESUME -> service.setUiActive(true)
                                                Lifecycle.Event.ON_PAUSE -> {
                                                    service.setUiActive(false)
                                                    service.setPreviewSurface(null)
                                                }
                                                else -> {}
                                            }
                                        }
                                        lifecycleOwner.lifecycle.addObserver(observer)
                                        onDispose {
                                            lifecycleOwner.lifecycle.removeObserver(observer)
                                            service.setUiActive(false)
                                            service.setPreviewSurface(null)
                                        }
                                    }
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            1 -> {
                                val events by logsViewModel.events.collectAsState()
                                LogsScreen(
                                    events = events,
                                    onClearLogs = { logsViewModel.clearLogs() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
