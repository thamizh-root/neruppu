package org.havenapp.neruppu.data.camera;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J*\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\b2\u0012\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u001b\u0012\u0004\u0012\u00020\u00160\u001aJ\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0086@\u00a2\u0006\u0002\u0010\u001eJ\u001a\u0010\u001f\u001a\u00020\u00162\u0012\u0010 \u001a\u000e\u0012\u0004\u0012\u00020!\u0012\u0004\u0012\u00020\u00160\u001aJ\u0010\u0010\"\u001a\u00020\u00162\b\u0010#\u001a\u0004\u0018\u00010$J\u0006\u0010%\u001a\u00020\u0016R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0012\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\tR\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lorg/havenapp/neruppu/data/camera/CameraManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "cameraExecutor", "Ljava/util/concurrent/ExecutorService;", "currentCameraSide", "", "Ljava/lang/Boolean;", "currentLifecycleOwner", "Landroidx/lifecycle/LifecycleOwner;", "imageAnalysis", "Landroidx/camera/core/ImageAnalysis;", "imageCapture", "Landroidx/camera/core/ImageCapture;", "isBound", "mutex", "Lkotlinx/coroutines/sync/Mutex;", "preview", "Landroidx/camera/core/Preview;", "bindCamera", "", "lifecycleOwner", "useFrontCamera", "onMotionDetected", "Lkotlin/Function1;", "", "capturePhoto", "Landroid/net/Uri;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getCameraProvider", "callback", "Landroidx/camera/lifecycle/ProcessCameraProvider;", "setPreviewSurface", "surfaceProvider", "Landroidx/camera/core/Preview$SurfaceProvider;", "unbind", "data_debug"})
public final class CameraManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.ImageCapture imageCapture;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.Preview preview;
    @org.jetbrains.annotations.Nullable()
    private androidx.camera.core.ImageAnalysis imageAnalysis;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.sync.Mutex mutex = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ExecutorService cameraExecutor = null;
    private boolean isBound = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.Boolean currentCameraSide;
    @org.jetbrains.annotations.Nullable()
    private androidx.lifecycle.LifecycleOwner currentLifecycleOwner;
    
    public CameraManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final void setPreviewSurface(@org.jetbrains.annotations.Nullable()
    androidx.camera.core.Preview.SurfaceProvider surfaceProvider) {
    }
    
    public final void getCameraProvider(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super androidx.camera.lifecycle.ProcessCameraProvider, kotlin.Unit> callback) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object capturePhoto(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super android.net.Uri> $completion) {
        return null;
    }
    
    public final void bindCamera(@org.jetbrains.annotations.NotNull()
    androidx.lifecycle.LifecycleOwner lifecycleOwner, boolean useFrontCamera, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Double, kotlin.Unit> onMotionDetected) {
    }
    
    public final void unbind() {
    }
}