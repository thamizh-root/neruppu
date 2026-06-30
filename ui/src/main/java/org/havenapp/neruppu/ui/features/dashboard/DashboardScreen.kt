package org.havenapp.neruppu.ui.features.dashboard


import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.havenapp.neruppu.core.ui.theme.*
import org.havenapp.neruppu.ui.R
import org.havenapp.neruppu.ui.components.BigButton
import org.havenapp.neruppu.ui.components.SensorCard

private const val MOTION_THRESHOLD_LOW = 20.0
private const val MOTION_THRESHOLD_MEDIUM = 50.0
private const val AUDIO_MAX_RMS = 500f
private const val LIGHT_MAX_LUX = 1000f
private const val MS_PER_SECOND = 1000L
private const val MS_PER_MINUTE = 60_000L
private const val MS_PER_HOUR = 3_600_000L

@Composable
fun DashboardScreen(
    isMonitoring: Boolean,
    motionLevel: Double,
    audioLevel: Float,
    lightLevel: Float,
    accelerometerStable: Boolean,
    onToggleMonitoring: () -> Unit,
    sessionStartTime: Long = 0L,
    lastGuardedTime: Long = 0L,
    // Camera integration
    useFrontCamera: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Status Bar & Topbar
            DashboardTopbar()

            // Shield Hero Section
            ShieldHero(isMonitoring = isMonitoring, sessionStartTime = sessionStartTime, lastGuardedTime = lastGuardedTime)

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
                            text = stringResource(R.string.dashboard_live_sensors),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        PixelPerfectDashboard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp)),
                            motionLevel = motionLevel.toFloat(),
                            audioLevel = (audioLevel / AUDIO_MAX_RMS).coerceIn(0f, 1f),
                            lightLevel = (lightLevel / LIGHT_MAX_LUX).coerceIn(0f, 1f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

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
                    text = if (isMonitoring) stringResource(R.string.dashboard_stop_monitoring) else stringResource(R.string.dashboard_start_guarding),
                    iconPainter = if (isMonitoring) null else painterResource(id = R.drawable.neruppu_brand_logo),
                    iconVector = if (isMonitoring) Icons.Default.Stop else null,
                    onClick = onToggleMonitoring,
                    secondary = isMonitoring,
                    backgroundColor = if (isMonitoring) BackgroundPrimary.copy(alpha = 0.8f) else null
                )
            }
        }
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
                    text = stringResource(R.string.dashboard_brand_neru),
                    color = BackgroundPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = stringResource(R.string.dashboard_brand_ppu),
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
fun ShieldHero(isMonitoring: Boolean, sessionStartTime: Long, lastGuardedTime: Long) {
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

    val currentTime = System.currentTimeMillis()
    val timeText = when {
        isMonitoring && sessionStartTime > 0 -> {
            val elapsedMs = currentTime - sessionStartTime
            val hours = (elapsedMs / MS_PER_HOUR).toInt()
            val minutes = ((elapsedMs / MS_PER_MINUTE) % 60).toInt()
            val startTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sessionStartTime))
            "$startTime \u00b7 ${hours}h ${minutes}m active"
        }
        !isMonitoring && lastGuardedTime > 0 -> {
            val elapsedMs = currentTime - lastGuardedTime
            val hours = (elapsedMs / MS_PER_HOUR).toInt()
            val minutes = ((elapsedMs / MS_PER_MINUTE) % 60).toInt()
            if (hours > 0) stringResource(R.string.dashboard_last_guarded, hours, minutes) else stringResource(R.string.dashboard_last_guarded_minutes, minutes)
        }
        else -> stringResource(R.string.dashboard_never_guarded)
    }

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
                    .border(3.dp, if (isMonitoring) NeruppuOrange else IdleBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.neruppu_brand_logo),
                    contentDescription = null,
                    tint = if (isMonitoring) NeruppuOrange else IdleIcon,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.dashboard_device_is), color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (isMonitoring) stringResource(R.string.dashboard_guarding) else stringResource(R.string.dashboard_idle),
            color = if (isMonitoring) BackgroundPrimary else TextSecondary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            timeText,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .background(
                    if (isMonitoring) NeruppuGreen.copy(alpha = 0.15f) else TextSecondary.copy(alpha = 0.12f),
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
                if (isMonitoring) stringResource(R.string.dashboard_all_sensors_live) else stringResource(R.string.dashboard_monitoring_off),
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
                value = if (motionLevel < MOTION_THRESHOLD_LOW) "Low" else if (motionLevel < MOTION_THRESHOLD_MEDIUM) "Medium" else "High",
                progress = (motionLevel / 100).toFloat().coerceIn(0f, 1f),
                color = NeruppuOrange,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                icon = Icons.Default.Mic,
                name = "Sound",
                value = "${audioLevel.toInt()} RMS",
                progress = (audioLevel / AUDIO_MAX_RMS).coerceIn(0f, 1f),
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
                progress = (lightLevel / LIGHT_MAX_LUX).coerceIn(0f, 1f),
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
            onToggleMonitoring = {}
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
            onToggleMonitoring = {}
        )
    }
}
