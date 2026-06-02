# Neruppu: Technical Knowledge Base (AI Context) & Production Checklist

This document serves as the ground-truth context for AI agents working on the Neruppu project. It defines the architecture, feature set, and technical constraints.

## 1. Project Vision
Neruppu is a modern, high-security physical monitoring application rebuilt from the foundations of "Haven". It is designed to be **offline-first** and **air-gapped**, using a device's sensors to detect and record security events, with optional **Matrix-based** remote alerts.

## 2. Core Architecture
The project follows **Clean Architecture** principles and is structured into a multi-module Gradle system to enforce strict dependency boundaries.

*   **`:domain`**: Pure Kotlin. Contains Entities (`Event`, `SensorType`), Repository Interfaces, and Use Cases (`HandleSensorEventUseCase`). Zero framework dependencies.
*   **`:data`**: Android Library. Hardware drivers (CameraX, AudioRecord, SensorManager), Room Database implementation (`EventDao`), and encrypted storage (`EncryptedSharedPreferences`).
*   **`:ui`**: Android Library. Jetpack Compose screens, ViewModels, and UI-specific components.
*   **`:core`**: Shared UI theme (Colors, Type) and common utility components.
*   **`:app`**: Application entry point, Hilt dependency injection wiring, and Foreground Service (`MonitoringService`).

## 3. Sensor Stack & Logic
*   **Camera (CameraX)**: Performs frame-by-frame motion analysis using a custom `MotionAnalyzer`. Supports both front and back cameras.
*   **Microphone**: Tracks real-time amplitude. Triggers audio recording (3-10s clips) when a sudden noise burst exceeds the calculated baseline + threshold.
*   **Accelerometer**: Detects device movement or vibration. Uses `SignificantMotion` hardware trigger where available for low-power monitoring.
*   **Ambient Light**: Detects sudden shifts in luminosity (e.g., a flashlight in a dark room or a door opening).

## 4. Alerting Protocol (Matrix)
Remote alerts are sent via the **Matrix Protocol** using a lightweight Ktor-based client (`MatrixApiClient`).
*   **Process**: Initial text alert sent -> Media (Image/Audio) uploaded to Homeserver -> Media event sent.
*   **Security**: Matrix credentials (Homeserver URL, Room ID, Access Token) are stored in `EncryptedSharedPreferences`.

## 5. Security & Privacy Features
*   **Offline-First**: All logs and media are stored locally in the `Downloads/Neruppu` folder (via MediaStore) and an internal Room database.
*   **Media Cleanup**: When clearing logs, users can optionally trigger a physical file deletion for all associated media.
*   **Stealth**: The `MonitoringService` runs as a Foreground Service with a sticky notification, ensuring the OS does not kill it during active guarding.

## 6. Key Files for Reference
*   `MonitoringService.kt`: Orchestrates all sensors and handles the guarding lifecycle.
*   `HandleSensorEventUseCase.kt`: Coordinates saving evidence and firing remote alerts.
*   `SensorRepositoryImpl.kt`: Manages database operations and physical file deletion.
*   `MatrixAlertTransport.kt`: Logic for formatting and transmitting alerts to Matrix.

## 7. UI Design Language
*   **Primary Color**: `NeruppuOrange` (`#E8520A`).
*   **Secondary Color**: `NeruppuBlue` (`#185FA5`).
*   **Backgrounds**: Clean, light theme with `BackgroundSecondary` (`#F7F7F7`) headers to match the bottom navigation.
*   **Components**: Custom `ScreenHeader`, `SensorCard`, and `BigButton`.

---

# Production Readiness Checklist

## ⚠️ CRITICAL ISSUES (Must Fix Before Production)

### 1. Android Gradle Plugin Incompatibility
- **File**: `gradle/libs.versions.toml:2`
- **Issue**: AGP 8.2.2 doesn't natively support compileSdk 35
- **Fix**: Suppressed via `android.suppressUnsupportedCompileSdk=35` in gradle.properties
- **Impact**: Warning suppressed, monitor for issues

### 2. Alpha Dependency in Production
- **File**: `gradle/libs.versions.toml:22`
- **Issue**: `androidx.security:security-crypto:1.1.0-alpha06` is alpha
- **Status**: ⚠️ Acceptable - this alpha has been stable for a long time
- **Impact**: Low risk, monitor for edge cases

### 3. Missing Signing Configuration
- **File**: `app/build.gradle.kts`
- **Issue**: No signingConfig for release build
- **Fix**: Add signing configuration for Play Store deployment
- **Impact**: Cannot generate signed release APK/AAB

## ⚠️ PERFORMANCE ISSUES

### 4. Busy-Wait Loop in Microphone Monitoring
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt`
- **Issue**: Continuous polling without proper sleep causes CPU overhead
- **Impact**: Battery drain when monitoring is active
- **Status**: ⚠️ Will be addressed in #10 fix (merged into single issue)

### 5. Bitmap Memory Management in MotionAnalyzer - N/A
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt`
- **Issue**: Bitmaps created once but references swapped; large bitmaps held in memory
- **Impact**: Potential memory pressure; double buffering may still cause GC pressure
- **Status**: No longer applicable - `_differenceMap`, `frontBitmap`, `backBitmap` were removed in commit 83a9332 (heatmap preview feature removal)

### 6. Camera Resolution at 640x480 May Not Be Optimal - FIXED ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt`
- **Issue**: Fixed resolution may be higher than needed for motion detection
- **Impact**: Increased CPU/load on ImageAnalysis analyzer
- **Fix**: ImageAnalysis now uses 320x240 (down from 640x480); Preview/ImageCapture keep 640x480 for quality

## ⚠️ MEMORY LEAK RISKS

### 7. Camera Executor Thread Leak Risk - FIXED ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt`
- **Issue**: Single-thread executor may not be properly shut down on all paths
- **Impact**: Potential memory leak if unbind() called before release()
- **Fix**: shutdown() now called in unbind() to prevent thread leak

### 8. StateFlow Holding Bitmap References - N/A
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt`
- **Issue**: `_differenceMap` holds Bitmap reference; could prevent GC
- **Impact**: Memory retained longer than necessary
- **Status**: This field was removed during heatmap preview feature removal (commit 83a9332)

### 13. MediaPlayer Potential Leak in Audio Player - FIXED ✅
- **File**: `ui/src/main/java/org/havenapp/neruppu/ui/features/logs/LogsScreen.kt`
- **Issue**: MediaPlayer created in prepareAsync() but may not be released if disposed before ready
- **Impact**: Potential memory leak if user navigates away during async prepare
- **Fix**: Added isReleased flag to prevent MediaPlayer race conditions on dispose

## ⚠️ BATTERY PERFORMANCE ISSUES

### 9. PARTIAL_WAKE_LOCK Held Continuously - FIXED ✅
- **File**: `app/src/main/java/org/havenapp/neruppu/service/MonitoringService.kt`
- **Issue**: WakeLock acquired every 8 minutes for 10-minute timeout
- **Impact**: Keeps CPU awake; could drain battery during extended monitoring
- **Note**: No adaptive wake lock based on device state
- **Fix**: Replaced 8-minute heartbeat with on-demand wake lock acquisition/release; wake lock only held when monitoring is active

### 10. Continuous Sensor Polling - FIXED ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt`
- **Issue**: Audio monitoring runs in tight loop (10ms sleep on silence)
- **Impact**: Significant battery drain during monitoring
- **Note**: Sample rate reduced to 16kHz (good optimization)
- **Fix**: Implemented adaptive delay - 200ms during silence, 50ms when noise detected

### 11. No Adaptive Frame Rate Based on Battery
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt`
- **Issue**: Fixed 5 FPS regardless of device battery state
- **Impact**: No power saving when battery is low

### 12. Accelerometer Uses SENSOR_DELAY_UI
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/AccelerometerDriver.kt`
- **Issue**: Could use SENSOR_DELAY_NORMAL for better battery
- **Impact**: Unnecessary power consumption for background monitoring

## ✅ GOOD PRACTICES FOLLOWED

### Performance Optimizations
- ✅ Reduced audio sample rate to 16kHz (from 44.1kHz)
- ✅ Frame skipping in MotionAnalyzer (5 FPS target)
- ✅ Sensor batching enabled (500ms latency for accelerometer/light)
- ✅ SignificantMotion sensor for ultra-low-power wakeups
- ✅ Camera resolution limited to 640x480 (ImageAnalysis: 320x240)
- ✅ Backpressure strategy KEEP_ONLY_LATEST

### Battery Optimizations
- ✅ Sensor batching (maxReportLatencyUs) used
- ✅ Single-thread executor for camera
- ✅ Wake lock on-demand (only when monitoring active)
- ✅ Proper sensor unregistration in awaitClose

### Memory Management
- ✅ Coroutine cancellation in onDestroy
- ✅ AudioRecord released in finally block
- ✅ Camera unbindAll() and executor shutdown on cleanup

### Architecture
- ✅ Foreground service with proper notification
- ✅ WorkManager for deferred notifications
- ✅ SharedPreferences listener unregistered on destroy
- ✅ Sensor cooldown (3-second minimum between events)

## ✅ FIXED ISSUES
- ✅ **#6 CameraManager**: ImageAnalysis resolution reduced to 320x240 for lower CPU load
- ✅ **#7 CameraManager**: Executor shutdown moved to unbind() to prevent thread leak
- ✅ **#9 MonitoringService**: Wake lock now on-demand (only when monitoring active)
- ✅ **#10 MicrophoneDriver**: Tight 10ms polling loop replaced with adaptive delay (200ms silence, 50ms active)
- ✅ **#13 LogsScreen**: Added isReleased flag to prevent MediaPlayer race conditions on dispose

## RECOMMENDATIONS FOR PRODUCTION

1. **AGP update recommended**:
   - AGP 8.6.0+ officially supports compileSdk 35

2. **Verify ProGuard rules** are complete for all dependencies used

3. **Test release build thoroughly** on target devices

4. **Check for Google Play requirements**:
   - Target SDK 35 is latest, comply with Play Store policies
   - Ensure all permissions have appropriate justifications
 - 


Security — critical
5 issues

HTTP logging leaks sensitive data in production
LogLevel.ALL in NetworkModule.kt logs every HTTP request/response including Matrix access tokens and Telegram bot tokens to logcat in release builds. Must be LogLevel.NONE in release.

No cleartext traffic restriction
AndroidManifest.xml is missing android:usesCleartextTraffic="false" and networkSecurityConfig. Any HTTP URL (e.g. user misconfiguring Matrix homeserver without https://) will send credentials in plaintext.

FileProvider exposes all internal storage
file_paths.xml uses path="." exposing the entire filesDir. Should restrict to captures/ and audio_captures/ subdirectories only.

No certificate pinning
Both TelegramApiClient and MatrixApiClient use plain Ktor Android engine with no TLS certificate pinning, making them vulnerable to MITM on compromised or enterprise networks.

Audio files not cleaned up after upload
AudioRecorder writes .mp4 clips to filesDir/audio_captures/ but they are never deleted after a successful Telegram/Matrix upload. Sensitive recordings accumulate indefinitely in private storage.


Architecture & best practices
3 issues

START_STICKY without proper restart guard
onStartCommand returns START_STICKY causing the OS to restart the service after kill with a null intent. There is no guard in onCreate or onStartCommand to handle this null-intent restart safely (e.g. defaulting to stopped state without re-binding camera).

No storage quota / cleanup policy for captures folder
MediaStorageRepositoryImpl and CameraManager write JPEG/MP4 files to filesDir/captures/ with no size cap or age-based eviction. A long-running deployment will silently exhaust device storage.

Permission denial is silently ignored
checkAndRequestPermissions() in MainActivity handles denial with an empty block. The user gets no feedback if camera/audio are denied — the toggle just silently fails to start monitoring.


Battery
3 issues

Microphone polling loop at 10ms intervals
MicrophoneDriver.kt reads PCM data in a tight while(isActive) loop with delay(10). That's ~100 wakeups/second on Dispatchers.IO. Use AudioRecord.setRecordPositionUpdateListener for event-driven reads, or at minimum increase delay to 50–100ms.

Camera active even when UI is not monitoring
When UI becomes active (setUiActive(true)), the camera is bound regardless of monitoring state, keeping the ISP and preview pipeline running. Consider binding camera only when the dashboard tab is actually visible, not on any UI visibility event.

WakeLock refresh creates a new PowerManager lock object every 8 minutes
startWakeLockRefresh() releases and re-acquires a new WakeLock object instead of re-using the existing one. The old pattern is correct; just call acquire(timeout) again on the existing lock after release().



Performance
4 issues

CameraExecutor thread leak on rebind
CameraManager.unbind() calls cameraExecutor.shutdown() inside an async callback, so shutdown is not guaranteed before a new executor is created on rebind. Over multiple camera switches, threads accumulate.
Unconstrained coroutine fan-out per sensor event

Every sensor trigger in MonitoringService calls serviceScope.launch { handleEvent(...) } inside a collect, creating unbounded concurrent coroutines. Under rapid triggering (e.g. light flickering) this can pile up without a capacity limit or channel-based consumer.

MotionAnalyzer reference buffer re-allocated on every rebind
referenceBuffer is nullified on cleanup() and reallocated as a 76 KB ByteArray on the next frame. Every camera toggle (front/back, prefs change) triggers GC pressure.

Settings state managed via SharedPreferences in Compose without DataStore
MainActivity reads 8 prefs synchronously on the main thread inside setContent and writes them with .apply() inside lambdas. Should use Jetpack DataStore with a ViewModel for lifecycle-aware, async state
