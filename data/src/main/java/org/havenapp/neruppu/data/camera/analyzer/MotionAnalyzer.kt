/*
 * Copyright (C) 2026 thamizh-root
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.havenapp.neruppu.data.camera.analyzer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

interface BatteryLevelProvider {
    fun getBatteryLevel(): Int   // returns 0-100, or -1 if unknown
}

object DefaultBatteryLevelProvider : BatteryLevelProvider {
    override fun getBatteryLevel(): Int = 100   // assume full battery
}

class ContextBatteryLevelProvider(private val context: Context) : BatteryLevelProvider {
    override fun getBatteryLevel(): Int {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level == -1 || scale == -1) {
            100   // assume full battery if we can't read
        } else {
            (level!! * 100 / scale!!)
        }
    }
}

class MotionAnalyzer(
    private val onMotionDetected: (Double) -> Unit,
    private val sensitivity: Int = 15,
    private val batteryLevelProvider: BatteryLevelProvider = DefaultBatteryLevelProvider
) : ImageAnalysis.Analyzer {

    internal var referenceBuffer: ByteArray? = null
    private var lastAnalysisTime = 0L

    private val outWidth = 320
    private val outHeight = 240

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        val batteryLevel = batteryLevelProvider.getBatteryLevel()
        val currentTargetFps = when {
            batteryLevel >= 50 -> 5
            batteryLevel >= 20 -> 3
            else -> 1
        }
        val frameIntervalMs = 1000L / currentTargetFps
        
        if (now - lastAnalysisTime < frameIntervalMs) {
            image.close()
            return
        }
        lastAnalysisTime = now

        val currentRef = referenceBuffer
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

                    if (diff > sensitivity * 2) {
                        changedPixels++
                    }
                    
                    totalSampled++
                    
                    val adaptRate = 0.05f
                    val blended = (adaptRate * currentVal + (1f - adaptRate) * referenceVal).toInt()
                    ref[refIndex] = blended.toByte()
                }
            }
        }

        val motionLevel = if (totalSampled > 0) (changedPixels.toDouble() / totalSampled) * 100.0 else 0.0
        onMotionDetected(motionLevel)

        image.close()
    }

    fun cleanup() {
        referenceBuffer = null
    }

    private fun fillBuffer(src: ByteBuffer, dst: ByteArray, w: Int, h: Int, rowStride: Int, pixelStride: Int) {
        if (pixelStride == 1 && rowStride == w) {
            src.get(dst, 0, dst.size)
            return
        }
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