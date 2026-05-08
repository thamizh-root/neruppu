package org.havenapp.neruppu.ui.features.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isMonitoring: Boolean,
    useFrontCamera: Boolean,
    stealthMode: Boolean,
    motionLevel: Double,
    audioLevel: Float,
    motionSensitivity: Float,
    audioSensitivity: Float,
    captureDuration: Float = 5f,
    motionHistory: List<Float>,
    differenceMap: Bitmap? = null,
    lightLevel: Float = 0f,
    onToggleMonitoring: () -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleStealthMode: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onAudioSensitivityChange: (Float) -> Unit,
    onCaptureDurationChange: (Float) -> Unit = {},
    onBindCamera: (PreviewView, LifecycleOwner) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // LAYER 0: Full Screen Camera Background
        Box(modifier = Modifier.fillMaxSize()) {
            if (!stealthMode) {
                PixelPerfectDashboard(
                    modifier = Modifier.fillMaxSize(),
                    motionLevel = motionLevel.toFloat(),
                    audioLevel = (audioLevel / 5000f).coerceIn(0f, 1f),
                    lightLevel = (lightLevel / 1000f).coerceIn(0f, 1f)
                )
            }

            var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
            
            LaunchedEffect(previewViewRef, lifecycleOwner, useFrontCamera) {
                previewViewRef?.let {
                    onBindCamera(it, lifecycleOwner)
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
                update = { }
            )
            
            // DIFFERENCE MAP HEATMAP OVERLAY
            if (!stealthMode && differenceMap != null) {
                Image(
                    bitmap = differenceMap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.5f),
                    contentScale = ContentScale.FillBounds
                )
            }

            // Motion Pulse Overlay (Transparent) - keep it as a subtle flash
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Yellow.copy(alpha = (motionLevel / 200.0).coerceIn(0.0, 0.1).toFloat())
                    )
            )

            // STEALTH MODE OVERLAY
            if (stealthMode) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }
        }

        // LAYER 1: Top UI Elements (Header)
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = if (stealthMode) Color.Transparent else Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "NERUPPU SECURITY",
                            color = if (stealthMode) Color.Transparent else Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonitoring) "SYSTEM ACTIVE" else "SYSTEM IDLE",
                            color = if (stealthMode) Color.Transparent else (if (isMonitoring) Color.Red else Color.Green),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    // Audio Meter (Compact)
                    if (!stealthMode) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(30.dp)
                                .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val normalizedAudio = (audioLevel / 5000f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(normalizedAudio)
                                    .fillMaxHeight(0.4f)
                                    .background(if (audioLevel > audioSensitivity) Color.Red else Color.Green, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // LAYER 2: Bottom Controls (Floating on top of camera)
            if (!stealthMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stealth Mode", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = stealthMode,
                            onCheckedChange = onToggleStealthMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Red),
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    // Settings Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Front Camera", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = useFrontCamera,
                            onCheckedChange = onToggleCamera,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Yellow),
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    SensitivitySlider(
                        label = "Motion: ${String.format("%.1f", motionSensitivity)}",
                        value = motionSensitivity,
                        onValueChange = onSensitivityChange
                    )
                    
                    SensitivitySlider(
                        label = "Audio: ${String.format("%.0f", audioSensitivity)}",
                        value = audioSensitivity,
                        valueRange = 0f..5000f,
                        onValueChange = onAudioSensitivityChange
                    )

                    SensitivitySlider(
                        label = "Capture Duration: ${String.format("%.0f", captureDuration)}s",
                        value = captureDuration,
                        valueRange = 5f..30f,
                        onValueChange = onCaptureDurationChange
                    )

                    Spacer(modifier = Modifier.height(80.dp)) // Space for the Start/Stop button
                }
            }
        }

        // LAYER 3: Main Start/Stop Toggle (Centered above bottom nav)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp), // Height above bottom navigation
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier
                    .size(if (stealthMode) 40.dp else 120.dp) // Shrink button in stealth mode
                    .shadow(if (isMonitoring && !stealthMode) 20.dp else 0.dp, CircleShape, spotColor = Color.Red)
                    .border(
                        width = 4.dp,
                        brush = Brush.radialGradient(
                            colors = if (stealthMode) listOf(Color.Transparent, Color.Transparent) else listOf(Color(0xFFFFD700), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stealthMode) Color.Transparent else (if (isMonitoring) Color.Red.copy(alpha = 0.8f) else Color.DarkGray.copy(alpha = 0.8f))
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                if (!stealthMode) {
                    Text(
                        text = if (isMonitoring) "STOP" else "START",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
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
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(text = label, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.Yellow,
                activeTrackColor = Color.Yellow,
                inactiveTrackColor = Color.Gray
            )
        )
    }
}
