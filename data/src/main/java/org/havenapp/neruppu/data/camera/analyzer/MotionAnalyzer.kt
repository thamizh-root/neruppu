package org.havenapp.neruppu.data.camera.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * High-performance Difference Map Analyzer.
 * Processes at 5 FPS, downsamples to 240p, and outputs a Grey/Blue/Yellow heatmap.
 */
class MotionAnalyzer(
    private val onMotionDetected: (Double) -> Unit,
    private val sensitivity: Int = 15
) : ImageAnalysis.Analyzer {

    private var referenceBuffer: ByteArray? = null
    private var lastAnalysisTime = 0L
    private val targetFps = 5
    private val frameIntervalMs = 1000L / targetFps

    // Output Visualization (240p approximate)
    private val outWidth = 320
    private val outHeight = 240
    private val outPixels = IntArray(outWidth * outHeight)
    
    private val _differenceMap = MutableStateFlow<Bitmap?>(null)
    val differenceMap: StateFlow<Bitmap?> = _differenceMap.asStateFlow()

    // Reusable Bitmap to avoid GC spikes
    private var outputBitmap: Bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        
        // 1. Frame Skipping: Only process at ~5 FPS
        if (now - lastAnalysisTime < frameIntervalMs) {
            image.close()
            return
        }
        lastAnalysisTime = now

        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val size = width * height
        if (referenceBuffer == null || referenceBuffer!!.size != size) {
            referenceBuffer = ByteArray(size)
            fillBuffer(buffer, referenceBuffer!!, width, height, rowStride, pixelStride)
            image.close()
            return
        }

        // 2. Perform high-perf IntArray math for the Difference Map
        var changedPixels = 0
        var totalSampled = 0
        
        val xStep = width / outWidth
        val yStep = height / outHeight

        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                val srcX = x * xStep
                val srcY = y * yStep
                val srcIndex = srcY * rowStride + srcX * pixelStride
                
                if (srcIndex < buffer.capacity()) {
                    val currentVal = buffer.get(srcIndex).toInt() and 0xFF
                    val refIndex = srcY * width + srcX
                    val referenceVal = referenceBuffer!![refIndex].toInt() and 0xFF
                    
                    val diff = abs(currentVal - referenceVal)
                    
                    // 3. Difference Map Logic (Grey/Blue/Yellow)
                    val color = when {
                        diff > sensitivity * 2 -> { // Major Change
                            changedPixels++
                            0xFFFFFF00.toInt() // Bright Yellow
                        }
                        diff > sensitivity -> { // Minor Change
                            0xFF0000FF.toInt() // Blue
                        }
                        else -> { // Unchanged
                            0xFF808080.toInt() // Mid-Grey
                        }
                    }
                    
                    outPixels[y * outWidth + x] = color
                    
                    // Update reference buffer with a slow adaptive rate
                    if (diff < sensitivity) {
                        referenceBuffer!![refIndex] = currentVal.toByte()
                    }
                    
                    totalSampled++
                }
            }
        }

        val motionLevel = if (totalSampled > 0) (changedPixels.toDouble() / totalSampled) * 100.0 else 0.0
        onMotionDetected(motionLevel)

        // 4. Update the reusable Bitmap and emit via StateFlow
        outputBitmap.setPixels(outPixels, 0, outWidth, 0, 0, outWidth, outHeight)
        _differenceMap.value = outputBitmap

        image.close()
    }

    private fun fillBuffer(src: ByteBuffer, dst: ByteArray, w: Int, h: Int, rowStride: Int, pixelStride: Int) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val index = y * rowStride + x * pixelStride
                if (index < src.capacity()) {
                    dst[y * w + x] = src.get(index)
                }
            }
        }
    }
}
