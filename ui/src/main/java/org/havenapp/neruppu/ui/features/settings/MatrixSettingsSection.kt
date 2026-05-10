package org.havenapp.neruppu.ui.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.havenapp.neruppu.core.ui.theme.*

@Composable
fun MatrixSettingsSection(
    viewModel: MatrixSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("Matrix Alerts")

        OutlinedTextField(
            value = uiState.homeserverUrl,
            onValueChange = viewModel::onHomeserverChange,
            label = { Text("Homeserver URL", color = TextSecondary) },
            placeholder = { Text("https://matrix.org") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeruppuOrange,
                focusedBorderColor = NeruppuOrange,
                unfocusedBorderColor = BorderTertiary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedContainerColor = BackgroundSecondary
            )
        )

        OutlinedTextField(
            value = uiState.roomId,
            onValueChange = viewModel::onRoomIdChange,
            label = { Text("Room ID", color = TextSecondary) },
            placeholder = { Text("!abc123:matrix.org") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeruppuOrange,
                focusedBorderColor = NeruppuOrange,
                unfocusedBorderColor = BorderTertiary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedContainerColor = BackgroundSecondary
            )
        )

        OutlinedTextField(
            value = uiState.accessToken,
            onValueChange = viewModel::onTokenChange,
            label = { Text("Access Token", color = TextSecondary) },
            placeholder = { Text("syt_...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeruppuOrange,
                focusedBorderColor = NeruppuOrange,
                unfocusedBorderColor = BorderTertiary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedContainerColor = BackgroundSecondary
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = viewModel::saveConfig,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeruppuOrange)
            ) {
                Text("Save")
            }

            OutlinedButton(
                onClick = viewModel::testConnection,
                modifier = Modifier.weight(1f),
                enabled = uiState.isSaved,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeruppuOrange)
            ) {
                Text("Test Ping")
            }
        }

        uiState.testStatus?.let { status ->
            Surface(
                color = if (status.success) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (status.success) "✓ Connected" else "✗ ${status.error}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (!uiState.isSaved) {
            Text(
                text = "Without a token, media is saved to Downloads/Neruppu/ only.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
