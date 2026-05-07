package org.havenapp.neruppu.ui.features.dashboard

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isMonitoring: Boolean,
    useFrontCamera: Boolean,
    motionLevel: Double,
    motionSensitivity: Float,
    motionHistory: List<Float>,
    onToggleMonitoring: () -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onBindCamera: (PreviewView, LifecycleOwner) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("NERUPPU ACTIVATED", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Camera Preview Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(8.dp)
                    .background(Color.DarkGray, MaterialTheme.shapes.medium)
            ) {
                var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
                
                LaunchedEffect(previewViewRef, lifecycleOwner, useFrontCamera) {
                    previewViewRef?.let {
                        onBindCamera(it, lifecycleOwner)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        previewViewRef?.let {
                            onBindCamera(it, lifecycleOwner) // This might need a way to "unbind"
                        }
                        // Actually, just letting it be is fine if the service manages it, 
                        // but we should probably tell the service to null out the surface.
                    }
                }

                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { 
                        // update is called on every recomposition, we use LaunchedEffect instead
                    }
                )
                
                // Motion Pulse Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Yellow.copy(alpha = (motionLevel / 100.0).coerceIn(0.0, 0.4).toFloat())
                        )
                )
            }

            // Status Bar
            Text(
                text = if (isMonitoring) "SYSTEM MONITORING..." else "SYSTEM IDLE",
                color = if (isMonitoring) Color.Red else Color.Green,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Motion Graph (Haven Style)
            MotionGraph(
                history = motionHistory,
                sensitivity = motionSensitivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

            // Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.DarkGray.copy(alpha = 0.3f), MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                SensitivitySlider(
                    label = "Motion Sensitivity: ${String.format("%.1f", motionSensitivity)}",
                    value = motionSensitivity,
                    onValueChange = onSensitivityChange
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Front Camera", color = Color.White)
                    Switch(
                        checked = useFrontCamera,
                        onCheckedChange = onToggleCamera,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Yellow)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) Color.Red else Color.DarkGray
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isMonitoring) "STOP MONITORING" else "START MONITORING",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun MotionGraph(
    history: List<Float>,
    sensitivity: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Draw Grid
        drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, height), end = androidx.compose.ui.geometry.Offset(width, height), strokeWidth = 1f)
        
        // Draw Sensitivity Line (Red)
        val sensitivityY = height - (sensitivity / 100f * height)
        drawLine(
            Color.Red,
            start = androidx.compose.ui.geometry.Offset(0f, sensitivityY),
            end = androidx.compose.ui.geometry.Offset(width, sensitivityY),
            strokeWidth = 2f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw Motion Data (Yellow)
        if (history.isNotEmpty()) {
            val path = Path()
            val step = width / 100f
            history.forEachIndexed { index, value ->
                val x = index * step
                val y = height - (value / 100f * height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, Color.Yellow, style = Stroke(width = 3f))
        }
    }
}

@Composable
fun SensitivitySlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = label, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.Yellow,
                activeTrackColor = Color.Yellow,
                inactiveTrackColor = Color.Gray
            )
        )
    }
}
