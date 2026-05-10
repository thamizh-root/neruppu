package org.havenapp.neruppu.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.havenapp.neruppu.core.ui.theme.*

import org.havenapp.neruppu.ui.components.ScreenHeader

@Composable
fun SettingsScreen(
    motionEnabled: Boolean,
    onMotionToggle: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    lightEnabled: Boolean,
    onLightToggle: (Boolean) -> Unit,
    motionSensitivity: Float,
    onMotionSensitivityChange: (Float) -> Unit,
    soundThreshold: Float,
    onSoundThresholdChange: (Float) -> Unit,
    pushAlerts: Boolean,
    onPushAlertsToggle: (Boolean) -> Unit,
    savePhotos: Boolean,
    onSavePhotosToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
    ) {
        ScreenHeader(title = "Settings")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            SectionTitle("Sensors")
            SettingsList {
                SettingRow(
                    icon = Icons.Default.CameraAlt,
                    label = "Motion detection",
                    subLabel = "CameraX analysis",
                    checked = motionEnabled,
                    onCheckedChange = onMotionToggle
                )
                SettingRow(
                    icon = Icons.Default.Mic,
                    label = "Acoustic monitor",
                    subLabel = "Mic level tracking",
                    checked = soundEnabled,
                    onCheckedChange = onSoundToggle
                )
                SettingRow(
                    icon = Icons.Default.WbSunny,
                    label = "Luminosity",
                    subLabel = "Ambient light sensor",
                    checked = lightEnabled,
                    onCheckedChange = onLightToggle
                )
            }

            SectionTitle("Sensitivity")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundSecondary, RoundedCornerShape(12.dp))
                    .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                SliderWrap(
                    label = "Motion threshold",
                    value = motionSensitivity,
                    onValueChange = onMotionSensitivityChange,
                    displayValue = if (motionSensitivity < 0.3f) "Low" else if (motionSensitivity < 0.7f) "Medium" else "High",
                    color = NeruppuOrange
                )
                Spacer(modifier = Modifier.height(14.dp))
                SliderWrap(
                    label = "Sound threshold",
                    value = soundThreshold,
                    onValueChange = onSoundThresholdChange,
                    displayValue = "${(soundThreshold * 100).toInt()} dB",
                    color = NeruppuBlue
                )
            }

            SectionTitle("Notifications")
            SettingsList {
                SettingRow(
                    icon = Icons.Default.Notifications,
                    label = "Push alerts",
                    subLabel = "On every event",
                    checked = pushAlerts,
                    onCheckedChange = onPushAlertsToggle
                )
                SettingRow(
                    icon = Icons.Default.Photo,
                    label = "Save photos",
                    subLabel = "On motion trigger",
                    checked = savePhotos,
                    onCheckedChange = onSavePhotosToggle
                )
            }

            MatrixSettingsSection()
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    label: String,
    subLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundSecondary, RoundedCornerShape(12.dp))
            .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subLabel, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeruppuOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BackgroundSecondary,
                uncheckedBorderColor = BorderTertiary
            ),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SliderWrap(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    displayValue: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(displayValue, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = color,
                inactiveTrackColor = BorderTertiary
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    NeruppuTheme {
        SettingsScreen(
            motionEnabled = true,
            onMotionToggle = {},
            soundEnabled = true,
            onSoundToggle = {},
            lightEnabled = false,
            onLightToggle = {},
            motionSensitivity = 0.5f,
            onMotionSensitivityChange = {},
            soundThreshold = 0.6f,
            onSoundThresholdChange = {},
            pushAlerts = true,
            onPushAlertsToggle = {},
            savePhotos = true,
            onSavePhotosToggle = {}
        )
    }
}
