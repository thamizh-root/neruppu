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

    // Double buffering to avoid concurrent read/write and StateFlow issues
    private var frontBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    private var backBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        
        // 1. Frame Skipping: Only process at ~5 FPS
        if (now - lastAnalysisTime < frameIntervalMs) {
            image.close()
            return
        }
        lastAnalysisTime = now

        // Capture current state locally to avoid race conditions with cleanup()
        val currentRef = referenceBuffer
        val currentFront = frontBitmap
        val currentBack = backBitmap

        // If bitmaps are already recycled, we shouldn't continue
        if (currentFront.isRecycled || currentBack.isRecycled) {
            image.close()
            return
        }

        // Extract ALL needed data FIRST
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buffer = plane.buffer

        val size = width * height
        var ref = currentRef
        if (ref == null || ref.size != size) {
            val newRef = ByteArray(size)
            fillBuffer(buffer, newRef, width, height, rowStride, pixelStride)
            referenceBuffer = newRef
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
                    val referenceVal = ref[refIndex].toInt() and 0xFF
                    
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
                    // Improved: slow global adaptive update (5% blend) for all pixels
                    val adaptRate = 0.05f
                    val blended = (adaptRate * currentVal + (1f - adaptRate) * referenceVal).toInt()
                    ref[refIndex] = blended.toByte()
                    
                    totalSampled++
                }
            }
        }

        val motionLevel = if (totalSampled > 0) (changedPixels.toDouble() / totalSampled) * 100.0 else 0.0
        onMotionDetected(motionLevel)

        // 4. Update the back bitmap and swap
        // Safe check using the local capture to ensure we don't write to a recycled bitmap
        if (!currentBack.isRecycled) {
            currentBack.setPixels(outPixels, 0, outWidth, 0, 0, outWidth, outHeight)
            _differenceMap.value = currentBack
            
            // Swap references safely
            frontBitmap = currentBack
            backBitmap = currentFront
        }

        image.close()
    }

    fun cleanup() {
        // Clear StateFlow first to stop UI from trying to draw
        _differenceMap.value = null
        // Removed manual recycle() to avoid race conditions with Compose UI's drawing thread.
        // These are small bitmaps (240p) and will be handled by GC safely once references are gone.
        referenceBuffer = null
    }

    private fun fillBuffer(src: ByteBuffer, dst: ByteArray, w: Int, h: Int, rowStride: Int, pixelStride: Int) {
        if (pixelStride == 1 && rowStride == w) {
            // Contiguous — bulk copy
            src.get(dst, 0, dst.size)
            return
        }
        // Strided — row-by-row bulk copy
        for (y in 0 until h) {
            val srcPos = y * rowStride
            val dstPos = y * w
            if (pixelStride == 1) {
                src.position(srcPos)
                src.get(dst, dstPos, w.coerceAtMost(dst.size - dstPos))
            } else {
                for (x in 0 until w) {
                    dst[dstPos + x] = src.get(srcPos + x * pixelStride)
                }
            }
        }
    }
}
