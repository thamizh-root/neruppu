package org.havenapp.neruppu.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.havenapp.neruppu.data.camera.analyzer.MotionAnalyzer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class CameraManager(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    private val mutex = Mutex()
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var isBound = false
    private var currentCameraSide: Boolean? = null
    private var currentLifecycleOwner: LifecycleOwner? = null

    fun setPreviewSurface(surfaceProvider: Preview.SurfaceProvider?) {
        preview?.setSurfaceProvider(surfaceProvider)
    }

    fun getCameraProvider(callback: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                callback(cameraProviderFuture.get())
            } catch (e: Exception) {
                Log.e("CameraManager", "Error getting camera provider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun capturePhoto(): Uri? = mutex.withLock {
        val capture = imageCapture ?: run {
            Log.e("CameraManager", "Cannot capture: imageCapture is null. Camera might not be bound to service.")
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis())
            
            val outputDir = File(context.filesDir, "captures").apply { mkdirs() }
            val file = File(outputDir, "$name.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            Log.d("CameraManager", "Taking picture for security event: ${file.name}")
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("CameraManager", "PHOTO SAVED: ${file.absolutePath}")
                        if (continuation.isActive) continuation.resume(Uri.fromFile(file))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraManager", "CAPTURE FAILED: ${exception.message}", exception)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        useFrontCamera: Boolean,
        onMotionDetected: (Double) -> Unit
    ) {
        if (isBound && currentCameraSide == useFrontCamera && currentLifecycleOwner == lifecycleOwner) {
            Log.d("CameraManager", "Camera already bound to service lifecycle. Keeping active.")
            return
        }

        getCameraProvider { cameraProvider ->
            try {
                cameraProvider.unbindAll()
                
                preview = Preview.Builder().build()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                @Suppress("DEPRECATION")
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, MotionAnalyzer(onMotionDetected))
                    }

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
                
                isBound = true
                currentCameraSide = useFrontCamera
                currentLifecycleOwner = lifecycleOwner
                Log.d("CameraManager", "CAMERA SYSTEM INITIALIZED BY SERVICE: Bound to $lifecycleOwner")
            } catch (exc: Exception) {
                Log.e("CameraManager", "CRITICAL: Service-side camera binding failed", exc)
                isBound = false
            }
        }
    }

    fun unbind() {
        getCameraProvider { cameraProvider ->
            cameraProvider.unbindAll()
            isBound = false
            currentCameraSide = null
            currentLifecycleOwner = null
            imageCapture = null
            preview = null
            imageAnalysis = null
            Log.d("CameraManager", "Camera system released")
        }
    }
}
