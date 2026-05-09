package org.havenapp.neruppu.ui.features.dashboard

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import org.havenapp.neruppu.core.ui.theme.*
import org.havenapp.neruppu.ui.R

@Composable
fun DashboardScreen(
    isMonitoring: Boolean,
    motionLevel: Double,
    audioLevel: Float,
    lightLevel: Float,
    accelerometerStable: Boolean,
    onToggleMonitoring: () -> Unit,
    // Camera integration
    useFrontCamera: Boolean = false,
    differenceMap: Bitmap? = null,
    onBindCamera: (PreviewView, LifecycleOwner) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Background - Always Show
        CameraPreviewBackground(
            isMonitoring = isMonitoring,
            onBindCamera = onBindCamera,
            differenceMap = differenceMap
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Status Bar & Topbar
            DashboardTopbar()

            // Shield Hero Section
            ShieldHero(isMonitoring = isMonitoring)

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                if (isMonitoring) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "LIVE SENSORS",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        SensorGrid(
                            motionLevel = motionLevel,
                            audioLevel = audioLevel,
                            lightLevel = lightLevel,
                            accelerometerStable = accelerometerStable
                        )
                    }
                } else {
                    // Space for starting the monitoring
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                BigButton(
                    text = if (isMonitoring) "Stop monitoring" else "Start guarding",
                    iconPainter = if (isMonitoring) null else painterResource(id = R.drawable.neruppu_brand_logo),
                    iconVector = if (isMonitoring) Icons.Default.Stop else null,
                    onClick = onToggleMonitoring,
                    secondary = isMonitoring,
                    backgroundColor = if (isMonitoring) Color.White.copy(alpha = 0.8f) else null
                )
            }
        }
    }
}

@Composable
fun CameraPreviewBackground(
    isMonitoring: Boolean,
    onBindCamera: (PreviewView, LifecycleOwner) -> Unit,
    differenceMap: Bitmap? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
            
    LaunchedEffect(previewViewRef, lifecycleOwner) {
        previewViewRef?.let {
            onBindCamera(it, lifecycleOwner)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Heatmap overlay
        if (differenceMap != null) {
            Image(
                bitmap = differenceMap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.3f),
                contentScale = ContentScale.FillBounds
            )
        }
        
        // Darken overlay to make UI readable
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
    }
}

@Composable
fun DashboardTopbar() {
    Column {
        // Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.neruppu_brand_logo),
                    contentDescription = null,
                    tint = NeruppuOrange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "neru",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "ppu",
                    color = NeruppuOrange,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ShieldHero(isMonitoring: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Pulse Ring
            if (isMonitoring) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .border(1.dp, NeruppuOrange.copy(alpha = 0.18f * pulseAlpha), CircleShape)
                )
            }
            
            // Shield Ring
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(3.dp, if (isMonitoring) NeruppuOrange else Color(0xFF444444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.neruppu_brand_logo),
                    contentDescription = null,
                    tint = if (isMonitoring) NeruppuOrange else Color(0xFF555555),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("Device is", color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (isMonitoring) "Guarding" else "Idle",
            color = if (isMonitoring) Color.White else Color(0xFF777777),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            if (isMonitoring) "Since 08:14 AM \u00b7 1h 27m active" else "Last guarded 2h ago",
            color = Color(0xFF666666),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Status Pill
        Row(
            modifier = Modifier
                .background(
                    if (isMonitoring) NeruppuGreen.copy(alpha = 0.15f) else Color(0xFF888880).copy(alpha = 0.12f),
                    CircleShape
                )
                .border(
                    0.5.dp,
                    if (isMonitoring) NeruppuGreen.copy(alpha = 0.3f) else BorderTertiary,
                    CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (isMonitoring) NeruppuGreen else TextSecondary, CircleShape)
            )
            Text(
                if (isMonitoring) "All sensors live" else "Monitoring off",
                color = if (isMonitoring) NeruppuGreen else TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun SensorGrid(
    motionLevel: Double,
    audioLevel: Float,
    lightLevel: Float,
    accelerometerStable: Boolean
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SensorCard(
                icon = Icons.Default.CameraAlt,
                name = "Motion",
                value = if (motionLevel < 20) "Low" else if (motionLevel < 50) "Medium" else "High",
                progress = (motionLevel / 100).toFloat().coerceIn(0f, 1f),
                color = NeruppuOrange,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                icon = Icons.Default.Mic,
                name = "Sound",
                value = "${audioLevel.toInt()} dB",
                progress = (audioLevel / 100).coerceIn(0f, 1f),
                color = NeruppuBlue,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SensorCard(
                icon = Icons.Default.WbSunny,
                name = "Luminosity",
                value = "${lightLevel.toInt()} lx",
                progress = (lightLevel / 1000).coerceIn(0f, 1f),
                color = NeruppuAmber,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                icon = Icons.Default.OpenWith,
                name = "Accelerometer",
                value = if (accelerometerStable) "Stable" else "Moving",
                progress = if (accelerometerStable) 0.05f else 0.8f,
                color = NeruppuGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SensorCard(
    icon: ImageVector,
    name: String,
    value: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(BackgroundSecondary.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(name, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BorderTertiary, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun BigButton(
    text: String,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    iconVector: ImageVector? = null,
    onClick: () -> Unit,
    secondary: Boolean = false,
    backgroundColor: Color? = null
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor ?: (if (secondary) Color.Transparent else NeruppuOrange),
            contentColor = if (secondary) TextSecondary else Color.White
        ),
        border = if (secondary) BorderStroke(0.5.dp, BorderTertiary) else null,
        contentPadding = PaddingValues(16.dp)
    ) {
        if (iconPainter != null) {
            Icon(iconPainter, contentDescription = null, modifier = Modifier.size(18.dp))
        } else if (iconVector != null) {
            Icon(iconVector, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardIdlePreview() {
    NeruppuTheme {
        DashboardScreen(
            isMonitoring = false,
            motionLevel = 0.0,
            audioLevel = 0f,
            lightLevel = 300f,
            accelerometerStable = true,
            onToggleMonitoring = {},
            onBindCamera = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardGuardingPreview() {
    NeruppuTheme {
        DashboardScreen(
            isMonitoring = true,
            motionLevel = 18.0,
            audioLevel = 42f,
            lightLevel = 310f,
            accelerometerStable = true,
            onToggleMonitoring = {},
            onBindCamera = { _, _ -> }
        )
    }
}
