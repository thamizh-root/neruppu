package org.havenapp.neruppu.service;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00a2\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0007\u0018\u0000 F2\u00020\u0001:\u0002FGB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010-\u001a\u00020.H\u0002J\b\u0010/\u001a\u000200H\u0002J\u0018\u00101\u001a\u0002002\u0006\u00102\u001a\u00020\u001a2\u0006\u00103\u001a\u000204H\u0002J\u0012\u00105\u001a\u0004\u0018\u0001062\u0006\u00107\u001a\u000208H\u0016J\b\u00109\u001a\u000200H\u0016J\b\u0010:\u001a\u000200H\u0016J\"\u0010;\u001a\u00020<2\b\u00107\u001a\u0004\u0018\u0001082\u0006\u0010=\u001a\u00020<2\u0006\u0010>\u001a\u00020<H\u0016J\u0010\u0010?\u001a\u0002002\b\u0010@\u001a\u0004\u0018\u00010AJ\b\u0010B\u001a\u000200H\u0002J\b\u0010C\u001a\u000200H\u0002J\b\u0010D\u001a\u000200H\u0002J\u0006\u0010E\u001a\u000200R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00070\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082.\u00a2\u0006\u0002\n\u0000R\u0012\u0010\f\u001a\u00060\rR\u00020\u0000X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u000e\u001a\u00020\u000f8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00050\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0016R\u000e\u0010\u0017\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001b0\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020 X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010!\u001a\u0004\u0018\u00010\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u00070\u0015\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u001e\u0010%\u001a\u00020&8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\'\u0010(\"\u0004\b)\u0010*R\u000e\u0010+\u001a\u00020,X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006H"}, d2 = {"Lorg/havenapp/neruppu/service/MonitoringService;", "Landroidx/lifecycle/LifecycleService;", "()V", "_isMonitoring", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_motionLevel", "", "accelerometerDriver", "Lorg/havenapp/neruppu/data/sensors/AccelerometerDriver;", "audioRecorder", "Lorg/havenapp/neruppu/data/audio/AudioRecorder;", "binder", "Lorg/havenapp/neruppu/service/MonitoringService$LocalBinder;", "cameraManager", "Lorg/havenapp/neruppu/data/camera/CameraManager;", "getCameraManager", "()Lorg/havenapp/neruppu/data/camera/CameraManager;", "setCameraManager", "(Lorg/havenapp/neruppu/data/camera/CameraManager;)V", "isMonitoring", "Lkotlinx/coroutines/flow/StateFlow;", "()Lkotlinx/coroutines/flow/StateFlow;", "isRecordingAudio", "lastEventTime", "", "Lorg/havenapp/neruppu/domain/model/SensorType;", "", "lastNoiseTime", "lightSensorDriver", "Lorg/havenapp/neruppu/data/sensors/LightSensorDriver;", "microphoneDriver", "Lorg/havenapp/neruppu/data/sensors/MicrophoneDriver;", "microphoneJob", "Lkotlinx/coroutines/Job;", "motionLevel", "getMotionLevel", "sensorRepository", "Lorg/havenapp/neruppu/domain/repository/SensorRepository;", "getSensorRepository", "()Lorg/havenapp/neruppu/domain/repository/SensorRepository;", "setSensorRepository", "(Lorg/havenapp/neruppu/domain/repository/SensorRepository;)V", "serviceScope", "Lkotlinx/coroutines/CoroutineScope;", "createNotification", "Landroid/app/Notification;", "createNotificationChannel", "", "handleEvent", "type", "description", "", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "", "flags", "startId", "setPreviewSurface", "surfaceProvider", "Landroidx/camera/core/Preview$SurfaceProvider;", "startAudioRecording", "startMicrophoneMonitoring", "startMonitoring", "toggleMonitoring", "Companion", "LocalBinder", "app_debug"})
public final class MonitoringService extends androidx.lifecycle.LifecycleService {
    @javax.inject.Inject()
    public org.havenapp.neruppu.domain.repository.SensorRepository sensorRepository;
    @javax.inject.Inject()
    public org.havenapp.neruppu.data.camera.CameraManager cameraManager;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope serviceScope = null;
    private org.havenapp.neruppu.data.sensors.AccelerometerDriver accelerometerDriver;
    private org.havenapp.neruppu.data.sensors.MicrophoneDriver microphoneDriver;
    private org.havenapp.neruppu.data.sensors.LightSensorDriver lightSensorDriver;
    private org.havenapp.neruppu.data.audio.AudioRecorder audioRecorder;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Double> _motionLevel = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Double> motionLevel = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isMonitoring = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isMonitoring = null;
    @org.jetbrains.annotations.NotNull()
    private final org.havenapp.neruppu.service.MonitoringService.LocalBinder binder = null;
    private boolean isRecordingAudio = false;
    private long lastNoiseTime = 0L;
    @org.jetbrains.annotations.NotNull()
    private java.util.Map<org.havenapp.neruppu.domain.model.SensorType, java.lang.Long> lastEventTime;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job microphoneJob;
    private static final int NOTIFICATION_ID = 1;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "monitoring_channel";
    @org.jetbrains.annotations.NotNull()
    public static final org.havenapp.neruppu.service.MonitoringService.Companion Companion = null;
    
    public MonitoringService() {
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
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Double> getMotionLevel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isMonitoring() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public android.os.IBinder onBind(@org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
        return null;
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    public final void setPreviewSurface(@org.jetbrains.annotations.Nullable()
    androidx.camera.core.Preview.SurfaceProvider surfaceProvider) {
    }
    
    public final void toggleMonitoring() {
    }
    
    private final void startMonitoring() {
    }
    
    private final void startMicrophoneMonitoring() {
    }
    
    private final void startAudioRecording() {
    }
    
    private final void handleEvent(org.havenapp.neruppu.domain.model.SensorType type, java.lang.String description) {
    }
    
    private final void createNotificationChannel() {
    }
    
    private final android.app.Notification createNotification() {
        return null;
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lorg/havenapp/neruppu/service/MonitoringService$Companion;", "", "()V", "CHANNEL_ID", "", "NOTIFICATION_ID", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0003\u001a\u00020\u0004\u00a8\u0006\u0005"}, d2 = {"Lorg/havenapp/neruppu/service/MonitoringService$LocalBinder;", "Landroid/os/Binder;", "(Lorg/havenapp/neruppu/service/MonitoringService;)V", "getService", "Lorg/havenapp/neruppu/service/MonitoringService;", "app_debug"})
    public final class LocalBinder extends android.os.Binder {
        
        public LocalBinder() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final org.havenapp.neruppu.service.MonitoringService getService() {
            return null;
        }
    }
}