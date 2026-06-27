package org.havenapp.neruppu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.havenapp.neruppu.core.ui.theme.*
import org.havenapp.neruppu.ui.R

@Composable
fun ScreenHeader(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(BackgroundSecondary)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = actions
        )
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
    iconPainter: Painter? = null,
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
