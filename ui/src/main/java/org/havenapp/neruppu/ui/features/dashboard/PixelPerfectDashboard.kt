package org.havenapp.neruppu.ui.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A pixel-perfect, real-time visualization dashboard inspired by Adafruit heatmaps.
 * Displays motion (Yellow), audio (Blue), and light (Green) waves on a grey background.
 */
@Composable
fun PixelPerfectDashboard(
    modifier: Modifier = Modifier,
    motionLevel: Float, // 0.0 to 100.0
    audioLevel: Float,  // Normalized 0.0 to 1.0
    lightLevel: Float,  // Normalized 0.0 to 1.0
    backgroundColor: Color = Color(0xFF222222)
) {
    // History buffers for the "waves"
    val historySize = 100
    val motionHistory = remember { mutableStateListOf<Float>().apply { repeat(historySize) { add(0f) } } }
    val audioHistory = remember { mutableStateListOf<Float>().apply { repeat(historySize) { add(0f) } } }
    val lightHistory = remember { mutableStateListOf<Float>().apply { repeat(historySize) { add(0f) } } }

    // Update history when new data arrives
    LaunchedEffect(motionLevel, audioLevel, lightLevel) {
        motionHistory.add(motionLevel / 100f)
        if (motionHistory.size > historySize) motionHistory.removeAt(0)

        audioHistory.add(audioLevel)
        if (audioHistory.size > historySize) audioHistory.removeAt(0)

        lightHistory.add(lightLevel)
        if (lightHistory.size > historySize) lightHistory.removeAt(0)
    }

    Canvas(modifier = modifier.background(backgroundColor)) {
        val width = size.width
        val height = size.height
        val barWidth = width / historySize
        val sectionHeight = height / 3

        // Colors
        val motionColor = Color(0xFFFFD700) // Yellow
        val audioColor = Color(0xFF00BFFF)  // Deep Sky Blue
        val lightColor = Color(0xFF32CD32)  // Lime Green

        // Draw Motion (Top)
        motionHistory.forEachIndexed { index, value ->
            val x = index * barWidth
            val h = value * sectionHeight
            drawRect(
                color = motionColor.copy(alpha = 0.8f),
                topLeft = Offset(x, sectionHeight - h),
                size = Size(barWidth, h)
            )
        }

        // Draw Audio (Middle)
        audioHistory.forEachIndexed { index, value ->
            val x = index * barWidth
            val h = value * sectionHeight
            drawRect(
                color = audioColor.copy(alpha = 0.8f),
                topLeft = Offset(x, (sectionHeight * 2) - h),
                size = Size(barWidth, h)
            )
        }

        // Draw Light (Bottom)
        lightHistory.forEachIndexed { index, value ->
            val x = index * barWidth
            val h = value * sectionHeight
            drawRect(
                color = lightColor.copy(alpha = 0.8f),
                topLeft = Offset(x, height - h),
                size = Size(barWidth, h)
            )
        }

        // Draw Grid Lines
        val gridColor = Color.White.copy(alpha = 0.1f)
        drawLine(gridColor, Offset(0f, sectionHeight), Offset(width, sectionHeight), 1f)
        drawLine(gridColor, Offset(0f, sectionHeight * 2), Offset(width, sectionHeight * 2), 1f)
    }
}
