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
    var currentSurfaceProvider: Preview.SurfaceProvider? = null
    
    private val mutex = Mutex()
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    var isBound = false
    var currentCameraSide: Boolean? = null
    var currentLifecycleOwner: LifecycleOwner? = null

    fun setPreviewSurface(surfaceProvider: Preview.SurfaceProvider?) {
        Log.d("CameraManager", "Setting preview surface: ${if (surfaceProvider != null) "ATTACHED" else "DETACHED"}")
        currentSurfaceProvider = surfaceProvider
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
        surfaceProvider: Preview.SurfaceProvider?,
        onMotionDetected: (Double) -> Unit,
        onMotionGrid: (FloatArray) -> Unit = {}
    ) {
        if (isBound && currentCameraSide == useFrontCamera && currentLifecycleOwner == lifecycleOwner && currentSurfaceProvider == surfaceProvider) {
            Log.d("CameraManager", "Camera already bound to service lifecycle with same surface. Keeping active.")
            return
        }

        getCameraProvider { cameraProvider ->
            try {
                cameraProvider.unbindAll()
                
                val useCases = mutableListOf<androidx.camera.core.UseCase>()

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                useCases.add(imageCapture!!)

                @Suppress("DEPRECATION")
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, MotionAnalyzer(onMotionDetected, onMotionGrid))
                    }
                useCases.add(imageAnalysis!!)

                if (surfaceProvider != null) {
                    preview = Preview.Builder().build().apply {
                        setSurfaceProvider(surfaceProvider)
                    }
                    useCases.add(preview!!)
                } else {
                    preview = null
                }

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    *useCases.toTypedArray()
                )
                
                isBound = true
                currentCameraSide = useFrontCamera
                currentLifecycleOwner = lifecycleOwner
                currentSurfaceProvider = surfaceProvider
                Log.d("CameraManager", "CAMERA SYSTEM INITIALIZED BY SERVICE: Bound to $lifecycleOwner (Surface: ${if (surfaceProvider != null) "YES" else "NO"})")
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
