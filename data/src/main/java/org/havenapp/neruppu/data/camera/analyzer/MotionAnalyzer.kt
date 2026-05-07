package org.havenapp.neruppu.data.camera.analyzer

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs

class MotionAnalyzer(
    private val onMotionDetected: (Double) -> Unit,
    private val onMotionGrid: (FloatArray) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private var referenceBuffer: ByteArray? = null
    private var bufferReuse: ByteArray? = null
    private var lastReferenceUpdateTime = 0L
    
    // Haven-grade tuning
    private val referenceUpdateIntervalMs = 500L
    private val pixelDiffThreshold = 15
    private val samplingStep = 4

    // Visualization grid (e.g., 20x15)
    private val gridCols = 20
    private val gridRows = 15
    private val motionGrid = FloatArray(gridCols * gridRows)

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        
        // Log every 5 seconds to avoid spamming while verifying background behavior
        if (now % 5000 < 100) {
            Log.d("MotionAnalyzer", "Analyzing frame: ${image.width}x${image.height} [Timestamp: $now]")
        }

        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        
        val size = buffer.remaining()
        if (bufferReuse == null || (bufferReuse!!.size != size)) {
            bufferReuse = ByteArray(size)
        }
        val currentData = bufferReuse!!
        buffer[currentData]
        
        if (referenceBuffer == null || (referenceBuffer!!.size != currentData.size)) {
            referenceBuffer = ByteArray(currentData.size)
            System.arraycopy(currentData, 0, referenceBuffer!!, 0, currentData.size)
            lastReferenceUpdateTime = now
            image.close()
            return
        }

        // Reset grid
        motionGrid.fill(0f)

        val motionLevel = calculateMotion(
            currentData, 
            referenceBuffer!!, 
            width, 
            height, 
            plane.rowStride, 
            plane.pixelStride
        )
        onMotionDetected(motionLevel)
        onMotionGrid(motionGrid.copyOf())

        if (now - lastReferenceUpdateTime > referenceUpdateIntervalMs) {
            if (motionLevel < 5.0) {
                System.arraycopy(currentData, 0, referenceBuffer!!, 0, currentData.size)
                lastReferenceUpdateTime = now
            }
        }
        
        image.close()
    }

    private fun calculateMotion(
        current: ByteArray, 
        reference: ByteArray, 
        width: Int, 
        height: Int, 
        rowStride: Int, 
        pixelStride: Int
    ): Double {
        var changedPixels = 0
        var totalSampled = 0
        
        val cellWidth = width / gridCols
        val cellHeight = height / gridRows

        for (y in 0 until height step samplingStep) {
            for (x in 0 until width step samplingStep) {
                val index = y * rowStride + x * pixelStride
                if (index < current.size) {
                    val diff = abs((current[index].toInt() and 0xFF) - (reference[index].toInt() and 0xFF))
                    
                    if (diff > pixelDiffThreshold) {
                        changedPixels++
                        
                        // Update grid
                        val gx = (x / cellWidth).coerceAtMost(gridCols - 1)
                        val gy = (y / cellHeight).coerceAtMost(gridRows - 1)
                        motionGrid[gy * gridCols + gx] += 1f
                    }
                    totalSampled++
                }
            }
        }
        
        // Normalize grid (0.0 to 1.0)
        val maxPixelsPerCell = (cellWidth * cellHeight) / (samplingStep * samplingStep)
        if (maxPixelsPerCell > 0) {
            for (i in motionGrid.indices) {
                motionGrid[i] = (motionGrid[i] / maxPixelsPerCell).coerceIn(0f, 1f)
            }
        }
        
        if (totalSampled == 0) return 0.0
        return (changedPixels.toDouble() / totalSampled) * 100.0
    }
}
