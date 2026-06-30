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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.havenapp.neruppu.core.ui.theme.*

data class TestStatus(
    val success: Boolean,
    val error: String? = null
)

enum class IntegrationType {
    TELEGRAM, MATRIX
}

data class IntegrationField(
    val label: String,
    val placeholder: String,
    val value: String,
    val onValueChange: (String) -> Unit,
    val isPassword: Boolean = false
)

interface IntegrationConfigUiState {
    val isSaved: Boolean
    val isLoading: Boolean
    val testStatus: TestStatus?
}

interface IntegrationConfigActions {
    fun saveConfig()
    fun testConnection()
    fun sendMockMessage()
}

@Composable
fun IntegrationConfigPopup(
    integrationType: IntegrationType,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    uiState: IntegrationConfigUiState,
    actions: IntegrationConfigActions,
    fields: List<IntegrationField>,
    descriptionText: String
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (integrationType) {
                    IntegrationType.TELEGRAM -> "Telegram Alerts"
                    IntegrationType.MATRIX -> "Matrix Alerts"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                fields.forEach { field ->
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = field.onValueChange,
                        label = { Text(field.label, color = TextSecondary) },
                        placeholder = { Text(field.placeholder) },
                        singleLine = true,
                        visualTransformation = if (field.isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
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
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { actions.saveConfig() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeruppuOrange)
                    ) {
                        Text("Save Config", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { actions.testConnection() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.isSaved && !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeruppuOrange),
                        border = BorderStroke(1.dp, if (uiState.isSaved && !uiState.isLoading) NeruppuOrange else BorderTertiary)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeruppuOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (uiState.isSaved) {
                    Button(
                        onClick = { actions.sendMockMessage() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeruppuGreen)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Mock Message", fontWeight = FontWeight.SemiBold)
                        }
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
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (status.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (status.success) NeruppuGreen else NeruppuRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}