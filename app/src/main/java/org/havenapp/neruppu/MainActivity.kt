package org.havenapp.neruppu

import android.Manifest
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.havenapp.neruppu.core.ui.theme.NeruppuTheme
import org.havenapp.neruppu.core.ui.theme.NeruppuOrange
import org.havenapp.neruppu.data.camera.CameraManager
import org.havenapp.neruppu.domain.repository.SensorRepository
import org.havenapp.neruppu.service.MonitoringService
import org.havenapp.neruppu.ui.features.dashboard.DashboardScreen
import org.havenapp.neruppu.ui.features.logs.LogsScreen
import org.havenapp.neruppu.ui.features.logs.LogsViewModel
import org.havenapp.neruppu.ui.features.settings.SettingsScreen
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sensorRepository: SensorRepository

    @Inject
    lateinit var cameraManager: CameraManager

    private val monitoringService = mutableStateOf<MonitoringService?>(null)
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.LocalBinder
            monitoringService.value = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName?) {
            monitoringService.value = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            // Handle permission denied
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, MonitoringService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        setContent {
            NeruppuTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val logsViewModel: LogsViewModel = remember { LogsViewModel(sensorRepository) }
                
                // Settings state (synced with SharedPreferences)
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                
                var motionEnabled by remember { mutableStateOf(prefs.getBoolean("motion_enabled", true)) }
                var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
                var lightEnabled by remember { mutableStateOf(prefs.getBoolean("light_enabled", false)) }
                var motionSensitivity by remember { mutableStateOf(prefs.getFloat("motion_sensitivity", 0.5f)) }
                var soundThreshold by remember { mutableStateOf(prefs.getFloat("sound_threshold", 0.6f)) }
                var pushAlerts by remember { mutableStateOf(prefs.getBoolean("push_alerts", true)) }
                var savePhotos by remember { mutableStateOf(prefs.getBoolean("save_photos", true)) }
                var useFrontCamera by remember { mutableStateOf(prefs.getBoolean("use_front_camera", false)) }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFFF7F7F7), // Match BackgroundSecondary
                            modifier = Modifier.height(80.dp),
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedIconColor = Color(0xFF777777),
                                    unselectedTextColor = Color(0xFF777777),
                                    indicatorColor = NeruppuOrange.copy(alpha = 0.1f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.List, contentDescription = null) },
                                label = { Text("Events", style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedIconColor = Color(0xFF777777),
                                    unselectedTextColor = Color(0xFF777777),
                                    indicatorColor = NeruppuOrange.copy(alpha = 0.1f)
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = Color.Black,
                                    unselectedIconColor = Color(0xFF777777),
                                    unselectedTextColor = Color(0xFF777777),
                                    indicatorColor = NeruppuOrange.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        when (selectedTab) {
                            0 -> {
                                val service = monitoringService.value
                                if (service != null) {
                                    val isMonitoring by service.isMonitoring.collectAsState()
                                    val motionLevel by service.motionLevel.collectAsState()
                                    val audioLevel by service.audioLevel.collectAsState()
                                    val lightLevel by service.lightLevel.collectAsState()
                                    val differenceMap by service.differenceMap.collectAsState()

                                    DashboardScreen(
                                        isMonitoring = isMonitoring,
                                        motionLevel = motionLevel,
                                        audioLevel = audioLevel / 50f, // Scale for display
                                        lightLevel = lightLevel,
                                        accelerometerStable = true, // Simplified
                                        onToggleMonitoring = { 
                                            if (!isMonitoring) {
                                                // Start service as foreground when enabling
                                                val intent = Intent(context, MonitoringService::class.java)
                                                context.startForegroundService(intent)
                                            }
                                            service.toggleMonitoring() 
                                        },
                                        useFrontCamera = useFrontCamera,
                                        differenceMap = differenceMap,
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
                                LogsScreen(
                                    events = logsViewModel.events,
                                    onClearLogs = { logsViewModel.clearLogs() }
                                )
                            }
                            2 -> {
                                SettingsScreen(
                                    motionEnabled = motionEnabled,
                                    onMotionToggle = {
                                        motionEnabled = it
                                        prefs.edit().putBoolean("motion_enabled", it).apply()
                                    },
                                    soundEnabled = soundEnabled,
                                    onSoundToggle = {
                                        soundEnabled = it
                                        prefs.edit().putBoolean("sound_enabled", it).apply()
                                    },
                                    lightEnabled = lightEnabled,
                                    onLightToggle = {
                                        lightEnabled = it
                                        prefs.edit().putBoolean("light_enabled", it).apply()
                                    },
                                    motionSensitivity = motionSensitivity,
                                    onMotionSensitivityChange = {
                                        motionSensitivity = it
                                        prefs.edit().putFloat("motion_sensitivity", it).apply()
                                    },
                                    soundThreshold = soundThreshold,
                                    onSoundThresholdChange = {
                                        soundThreshold = it
                                        prefs.edit().putFloat("sound_threshold", it).apply()
                                    },
                                    pushAlerts = pushAlerts,
                                    onPushAlertsToggle = {
                                        pushAlerts = it
                                        prefs.edit().putBoolean("push_alerts", it).apply()
                                    },
                                    savePhotos = savePhotos,
                                    onSavePhotosToggle = {
                                        savePhotos = it
                                        prefs.edit().putBoolean("save_photos", it).apply()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
