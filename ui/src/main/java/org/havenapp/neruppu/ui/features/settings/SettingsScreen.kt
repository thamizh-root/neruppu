package org.havenapp.neruppu.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
val telegramViewModel: TelegramSettingsViewModel = hiltViewModel()
    val matrixViewModel: MatrixSettingsViewModel = hiltViewModel()
    
    val telegramUiState by telegramViewModel.uiState.collectAsState()
    val matrixUiState by matrixViewModel.uiState.collectAsState()
    
    var showTelegramConfig by remember { mutableStateOf(false) }
    var showMatrixConfig by remember { mutableStateOf(false) }
    
    val telegramFields = listOf(
IntegrationField(
label = "Bot Token",
placeholder = "123456:ABC-DEF...",
value = telegramUiState.botToken,
onValueChange = telegramViewModel::onTokenChange,
isPassword = true
),
IntegrationField(
label = "Chat ID",
placeholder = "123456789",
value = telegramUiState.chatId,
onValueChange = telegramViewModel::onChatIdChange
)
    )
    
    val matrixFields = listOf(
IntegrationField(
label = "Homeserver URL",
placeholder = "https://matrix.org",
value = matrixUiState.homeserverUrl,
onValueChange = matrixViewModel::onHomeserverChange
),
IntegrationField(
label = "Room ID",
placeholder = "!abc123:matrix.org",
value = matrixUiState.roomId,
onValueChange = matrixViewModel::onRoomIdChange
),
IntegrationField(
label = "Access Token",
placeholder = "syt_...",
value = matrixUiState.accessToken,
onValueChange = matrixViewModel::onTokenChange,
isPassword = true
)
)

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

SectionTitle("Integrations")
SettingsList {
IntegrationSettingRow(
icon = Icons.Default.Send,
label = "Telegram Alerts",
isConfigured = telegramUiState.isSaved,
onClick = { showTelegramConfig = true }
)
IntegrationSettingRow(
icon = Icons.Default.Message,
label = "Matrix Alerts",
isConfigured = matrixUiState.isSaved,
onClick = { showMatrixConfig = true }
)
}
}
}

IntegrationConfigPopup(
integrationType = IntegrationType.TELEGRAM,
isVisible = showTelegramConfig,
onDismiss = { showTelegramConfig = false },
uiState = telegramUiState,
actions = telegramViewModel,
fields = telegramFields,
descriptionText = "Provide Telegram bot credentials to receive real-time alerts. Use @BotFather to create a bot."
    )
    
    IntegrationConfigPopup(
integrationType = IntegrationType.MATRIX,
isVisible = showMatrixConfig,
onDismiss = { showMatrixConfig = false },
uiState = matrixUiState,
actions = matrixViewModel,
fields = matrixFields,
descriptionText = "Provide Matrix credentials to receive real-time alerts on other devices."
)
}

@Composable
fun IntegrationSettingRow(
icon: ImageVector,
label: String,
isConfigured: Boolean,
onClick: () -> Unit
) {
Row(
modifier = Modifier
.fillMaxWidth()
.clickable(onClick = onClick)
.background(BackgroundSecondary, RoundedCornerShape(12.dp))
.border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
.padding(horizontal = 12.dp, vertical = 12.dp),
verticalAlignment = Alignment.CenterVertically
) {
Icon(
icon,
contentDescription = null,
tint = if (isConfigured) NeruppuGreen else TextSecondary,
modifier = Modifier.size(20.dp)
)
Spacer(modifier = Modifier.width(12.dp))
Column(modifier = Modifier.weight(1f)) {
Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
Text(
if (isConfigured) "Configured" else "Not configured",
color = if (isConfigured) NeruppuGreen else TextSecondary,
style = MaterialTheme.typography.bodySmall
)
}
Icon(
Icons.Default.ChevronRight,
contentDescription = null,
tint = TextSecondary,
modifier = Modifier.size(20.dp)
)
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