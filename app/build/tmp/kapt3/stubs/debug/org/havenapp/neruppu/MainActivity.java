package org.havenapp.neruppu;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0019\u001a\u00020\u001aH\u0002J\u0012\u0010\u001b\u001a\u00020\u001a2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0014J\b\u0010\u001e\u001a\u00020\u001aH\u0014J\b\u0010\u001f\u001a\u00020\u001aH\u0014J\b\u0010 \u001a\u00020\u001aH\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0013\u001a\u00020\u00148\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0015\u0010\u0016\"\u0004\b\u0017\u0010\u0018\u00a8\u0006!"}, d2 = {"Lorg/havenapp/neruppu/MainActivity;", "Landroidx/activity/ComponentActivity;", "()V", "cameraManager", "Lorg/havenapp/neruppu/data/camera/CameraManager;", "getCameraManager", "()Lorg/havenapp/neruppu/data/camera/CameraManager;", "setCameraManager", "(Lorg/havenapp/neruppu/data/camera/CameraManager;)V", "connection", "Landroid/content/ServiceConnection;", "isBound", "", "monitoringService", "Lorg/havenapp/neruppu/service/MonitoringService;", "requestPermissionLauncher", "Landroidx/activity/result/ActivityResultLauncher;", "", "", "sensorRepository", "Lorg/havenapp/neruppu/domain/repository/SensorRepository;", "getSensorRepository", "()Lorg/havenapp/neruppu/domain/repository/SensorRepository;", "setSensorRepository", "(Lorg/havenapp/neruppu/domain/repository/SensorRepository;)V", "checkAndRequestPermissions", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onPause", "onResume", "app_debug"})
public final class MainActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public org.havenapp.neruppu.domain.repository.SensorRepository sensorRepository;
    @javax.inject.Inject()
    public org.havenapp.neruppu.data.camera.CameraManager cameraManager;
    @org.jetbrains.annotations.Nullable()
    private org.havenapp.neruppu.service.MonitoringService monitoringService;
    private boolean isBound = false;
    @org.jetbrains.annotations.NotNull()
    private final android.content.ServiceConnection connection = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String[]> requestPermissionLauncher = null;
    
    public MainActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.havenapp.neruppu.domain.repository.SensorRepository getSensorRepository() {
        return null;
    }
    
    public final void setSensorRepository(@org.jetbrains.annotations.NotNull()
    org.havenapp.neruppu.domain.repository.SensorRepository p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.havenapp.neruppu.data.camera.CameraManager getCameraManager() {
        return null;
    }
    
    public final void setCameraManager(@org.jetbrains.annotations.NotNull()
    org.havenapp.neruppu.data.camera.CameraManager p0) {
    }
    
    private final void checkAndRequestPermissions() {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onPause() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
}