package org.havenapp.neruppu

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
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
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
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

    private var monitoringService: MonitoringService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.LocalBinder
            monitoringService = binder.getService()
            isBound = true
            Log.d("MainActivity", "Bound to MonitoringService")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
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
        }
    }

    override fun onResume() {
        super.onResume()
        // No need for RECLAIM_CAMERA, the service always has it
    }

    override fun onPause() {
        super.onPause()
        // No need for RECLAIM_CAMERA, the service always has it
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

                Scaffold(
                    bottomBar = {
                        NavigationBar {
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
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (selectedTab) {
                            0 -> {
                                val service = monitoringService
                                if (service != null) {
                                    val isMonitoring by service.isMonitoring.collectAsState()
                                    val motionLevel by service.motionLevel.collectAsState()
                                    
                                    val prefs = getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                    val useFrontCamera = prefs.getBoolean("use_front_camera", false)
                                    val motionSensitivity = prefs.getFloat("motion_sensitivity", 15f)

                                    // Local state for UI history
                                    val motionHistory = remember { mutableStateListOf<Float>() }
                                    LaunchedEffect(motionLevel) {
                                        motionHistory.add(motionLevel.toFloat())
                                        if (motionHistory.size > 100) motionHistory.removeAt(0)
                                    }

                                    DashboardScreen(
                                        isMonitoring = isMonitoring,
                                        useFrontCamera = useFrontCamera,
                                        motionLevel = motionLevel,
                                        motionHistory = motionHistory,
                                        motionSensitivity = motionSensitivity,
                                        onSensitivityChange = { sensitivity ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putFloat("motion_sensitivity", sensitivity)
                                                .apply()
                                        },
                                        onToggleCamera = { useFront ->
                                            getSharedPreferences("neruppu_prefs", MODE_PRIVATE)
                                                .edit()
                                                .putBoolean("use_front_camera", useFront)
                                                .apply()
                                            // Tell service to restart camera with new side
                                            val intent = Intent(this@MainActivity, MonitoringService::class.java)
                                            startForegroundService(intent)
                                        },
                                        onToggleMonitoring = { 
                                            service.toggleMonitoring()
                                        },
                                        onBindCamera = { previewView, _ ->
                                            service.setPreviewSurface(previewView.surfaceProvider)
                                        }
                                    )
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
