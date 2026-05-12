package org.havenapp.neruppu.ui.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.havenapp.neruppu.core.ui.theme.*

@Composable
fun TelegramSettingsSection(
    viewModel: TelegramSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SectionTitle("Telegram Alerts")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundSecondary, RoundedCornerShape(12.dp))
                .border(0.5.dp, BorderTertiary, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.botToken,
                onValueChange = viewModel::onTokenChange,
                label = { Text("Bot Token", color = TextSecondary) },
                placeholder = { Text("123456:ABC-DEF...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeruppuOrange,
                    focusedBorderColor = NeruppuOrange,
                    unfocusedBorderColor = BorderTertiary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            OutlinedTextField(
                value = uiState.chatId,
                onValueChange = viewModel::onChatIdChange,
                label = { Text("Chat ID", color = TextSecondary) },
                placeholder = { Text("123456789") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeruppuOrange,
                    focusedBorderColor = NeruppuOrange,
                    unfocusedBorderColor = BorderTertiary,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = viewModel::saveConfig,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeruppuOrange)
                ) {
                    Text("Save Config", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = viewModel::testConnection,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.isSaved,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeruppuOrange),
                    border = BorderStroke(1.dp, if (uiState.isSaved) NeruppuOrange else BorderTertiary)
                ) {
                    Text("Test Connection", fontWeight = FontWeight.SemiBold)
                }
            }

            uiState.testStatus?.let { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (status.success) NeruppuGreen.copy(alpha = 0.1f) else NeruppuRed.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (status.success) NeruppuGreen else NeruppuRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.success) "Connection successful" else status.error ?: "Connection failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.success) NeruppuGreen else NeruppuRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!uiState.isSaved) {
                Text(
                    text = "Provide Telegram bot credentials to receive real-time alerts. Use @BotFather to create a bot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
