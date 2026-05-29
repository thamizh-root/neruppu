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
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt:65-102`
- **Issue**: Continuous polling without proper sleep causes CPU overhead
- **Impact**: Battery drain when monitoring is active
- **Note**: Has 10ms delay on silence but still active polling

### 5. Bitmap Memory Management in MotionAnalyzer
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:37-38, 56-59`
- **Issue**: Bitmaps created once but references swapped; large bitmaps held in memory
- **Impact**: Potential memory pressure; double buffering may still cause GC pressure
- **Note**: cleanup() doesn't recycle bitmaps (intentional) but they accumulate

### 6. Camera Resolution at 640x480 May Not Be Optimal
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt:117-124`
- **Issue**: Fixed resolution may be higher than needed for motion detection
- **Impact**: Increased CPU/load on ImageAnalysis analyzer

## ⚠️ MEMORY LEAK RISKS

### 7. Camera Executor Thread Leak Risk
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt:35-36, 194-197`
- **Issue**: Single-thread executor may not be properly shut down on all paths
- **Impact**: Potential memory leak if unbind() called before release()
- **Note**: shutdown() in release() but not in cleanup path

### 8. StateFlow Holding Bitmap References
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:33-34`
- **Issue**: `_differenceMap` holds Bitmap reference; could prevent GC
- **Impact**: Memory retained longer than necessary

### 13. MediaPlayer Potential Leak in Audio Player
- **File**: `ui/src/main/java/org/havenapp/neruppu/ui/features/logs/LogsScreen.kt:312-387`
- **Issue**: MediaPlayer created in prepareAsync() but may not be released if disposed before ready
- **Impact**: Potential memory leak if user navigates away during async prepare
- **Note**: DisposableEffect handles cleanup but MediaPlayer lifecycle edge cases exist

## ⚠️ BATTERY PERFORMANCE ISSUES

### 9. PARTIAL_WAKE_LOCK Held Continuously
- **File**: `app/src/main/java/org/havenapp/neruppu/service/MonitoringService.kt:198-206, 262-267`
- **Issue**: WakeLock acquired every 8 minutes for 10-minute timeout
- **Impact**: Keeps CPU awake; could drain battery during extended monitoring
- **Note**: No adaptive wake lock based on device state

### 10. Continuous Sensor Polling
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt`
- **Issue**: Audio monitoring runs in tight loop (10ms sleep on silence)
- **Impact**: Significant battery drain during monitoring
- **Note**: Sample rate reduced to 16kHz (good optimization)

### 11. No Adaptive Frame Rate Based on Battery
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:25, 44-47`
- **Issue**: Fixed 5 FPS regardless of device battery state
- **Impact**: No power saving when battery is low

### 12. Accelerometer Uses SENSOR_DELAY_UI
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/AccelerometerDriver.kt:45-50`
- **Issue**: Could use SENSOR_DELAY_NORMAL for better battery
- **Impact**: Unnecessary power consumption for background monitoring

## ✅ GOOD PRACTICES FOLLOWED

### Performance Optimizations
- ✅ Reduced audio sample rate to 16kHz (from 44.1kHz)
- ✅ Frame skipping in MotionAnalyzer (5 FPS target)
- ✅ Sensor batching enabled (500ms latency for accelerometer/light)
- ✅ SignificantMotion sensor for ultra-low-power wakeups
- ✅ Camera resolution limited to 640x480
- ✅ Backpressure strategy KEEP_ONLY_LATEST

### Battery Optimizations
- ✅ Sensor batching (maxReportLatencyUs) used
- ✅ Single-thread executor for camera
- ✅ Wake lock timeout (10 minutes)
- ✅ Proper sensor unregistration in awaitClose

### Memory Management
- ✅ Bitmaps checked for recycled state before use
- ✅ Coroutine cancellation in onDestroy
- ✅ AudioRecord released in finally block
- ✅ Camera unbindAll() on cleanup

### Architecture
- ✅ Foreground service with proper notification
- ✅ WorkManager for deferred notifications
- ✅ SharedPreferences listener unregistered on destroy
- ✅ Sensor cooldown (3-second minimum between events)

## RECOMMENDATIONS FOR PRODUCTION

1. **AGP update recommended**:
   - AGP 8.6.0+ officially supports compileSdk 35

2. **Verify ProGuard rules** are complete for all dependencies used

3. **Test release build thoroughly** on target devices

4. **Check for Google Play requirements**:
   - Target SDK 35 is latest, comply with Play Store policies
   - Ensure all permissions have appropriate justifications