# Production Checklist - F-Droid Release

## ⚠️ BATTERY ISSUES

- [ ] **WakeLock Management** - WakeLock acquired when monitoring (MonitoringService.kt:182) but has 10-minute timeout. Consider using `PARTIAL_WAKE_LOCK` with `acquire()` without timeout and explicit release, or use `setWakeMode` on Service for simpler lifecycle
- [ ] **Camera Analysis FPS** - MotionAnalyzer runs at 5 FPS (targetFps=5). This is reasonable but consider configurable frame rate for battery-conscious users
- [ ] **Microphone Polling Loop** - MicrophoneDriver has polling loop with `delay(10)` (MicrophoneDriver.kt:98). Consider using AudioRecord with `OnRecordPositionUpdateListener` for event-driven instead of polling
- [ ] **Sensor Batching** - Accelerometer uses 500ms batching (AccelerometerDriver.kt:49). Already optimized. Light sensor uses 2s batching (LightSensorDriver.kt:46). Good.
- [ ] **Significant Motion Sensor** - Uses hardware trigger (SignificantMotionDriver.kt:24-30). Already optimal for battery.
- [ ] **Audio Recording Suspend/Restart** - AudioRecorder restarts microphone monitoring after 400ms delay (MonitoringService.kt:431) during event processing. This creates audio gaps.

## ⚠️ PERFORMANCE ISSUES

- [ ] **HttpClient Logging** - NetworkModule logs ALL traffic (LogLevel.ALL) in production (NetworkModule.kt:41). This impacts performance and leaks sensitive data. Should be `LogLevel.NONE` in release builds.
- [ ] **Camera Resolution** - Analysis uses 320x240 (MotionAnalyzer.kt:19-20) and capture uses 640x480 (CameraManager.kt:116). Consider 640x480 for analysis too for better detection accuracy vs CPU tradeoff
- [ ] **ExecutorService Not Shutdown Properly** - CameraManager creates `cameraExecutor` but `unbind()` uses async callback that may not shutdown executor synchronously. Potential thread leak.
- [ ] **Image Buffer Re-allocation** - MotionAnalyzer reallocates reference buffer on size change (MotionAnalyzer.kt:41-46). This happens on every camera rebind which could cause GC pressure.
- [ ] **Concurrent Event Handling** - Each sensor event spawns a new coroutine in serviceScope (MonitoringService.kt:324, 334-335). Consider using a single channel/consumer pattern.
- [ ] **Service Restart Policy** - `START_STICKY` restarts service after system kill (MonitoringService.kt:248), may cause rapid restart loops on low memory devices.

## ⚠️ SECURITY ISSUES

- [ ] **Logging Sensitive Information** - Multiple verbose logs may expose internal state (MonitoringService.kt:170, MatrixApiClient.kt:65). Reduce log level in release builds.
- [ ] **ProGuard Rules Incomplete** - TelegramApiClient and MatrixApiClient classes not kept for reflection. Ktor uses reflection for serialization which may break with obfuscation.
- [ ] **Cleartext Traffic** - No `android:usesCleartextTraffic="false"` in manifest. Should explicitly disable unless required.
- [ ] **Network Security Config** - Missing `networkSecurityConfig` for explicit TLS configuration.
- [ ] **Background Service Restrictions** - For Android 14+, need explicit `FOREGROUND_SERVICE_*` permission declarations for each service type used.
- [ ] **Export Flags** - MainActivity has `android:exported="true"` but no permission protection (AndroidManifest.xml:25). Consider if intentional for launcher.
- [ ] **FileProvider Path Exposure** - `file_paths.xml` exposes all internal files (`<files-path name="internal_files" path="." />`). Should restrict to specific subdirectories only.
- [ ] **Credentials in Memory** - Access tokens stored in plain String variables, not cleared after use.
- [ ] **No Certificate Pinning** - HTTP clients don't pin certificates, vulnerable to MITM on compromised networks.
- [ ] **Audio File Retention** - Original audio recordings not deleted after upload (AudioRecorder.kt saves to private storage but no cleanup on upload success).

## ⚠️ F-DROID COMPATIBILITY

- [ ] **NetworkModule Logging** - SLF4J Android logging library (sfl4j-android) may not be acceptable for F-Droid. Remove or make optional.
- [ ] **LeakCanary Dependency** - LeakCanary (leakcanary-android) is debug-only but present in debugImplementation. Ensure it's not bundled in release.

## ✓ FIXED / GOOD PRACTICES

- [x] **Encrypted SharedPreferences** - TelegramConfigStore and MatrixConfigStore use AES256-GCM encryption for sensitive credentials
- [x] **WakeLock Timeout** - WakeLock has 10-minute timeout preventing indefinite lock (MonitoringService.kt:182)
- [x] **Foreground Service Type** - Camera and microphone declared in foreground service type (AndroidManifest.xml:38)
- [x] **ProGuard Minification** - Release builds have `isMinifyEnabled = true` and `isShrinkResources = true` (app/build.gradle.kts:27-28)
- [x] **Camera Timeout Handling** - CameraManager handles initialization failures gracefully (CameraManager.kt:165-167)
- [x] **Cooldown Mechanism** - 3-second cooldown per sensor type prevents event spam (MonitoringService.kt:469)
- [x] **Sensor Unregistration** - All sensors properly unregister in `awaitClose` blocks
- [x] **WorkManager for Notifications** - Uses WorkManager for deferred notification (MonitoringService.kt:521-525)
- [x] **Fallback Encoding** - MatrixConfigStore falls back to plain prefs on encryption failure (MatrixConfigStore.kt:28-31)
- [x] **Proper Lifecycle Handling** - MonitoringService uses LifecycleService and stops foreground when not monitoring (MonitoringService.kt:551-555)
- [x] **Image Analysis Backpressure** - Uses `STRATEGY_KEEP_ONLY_LATEST` for ImageAnalysis (CameraManager.kt:141)
- [x] **Camera Executor Isolation** - Camera operations run on dedicated single-thread executor (CameraManager.kt:32)
- [x] **Buffer Size Coercion** - Audio buffer size coerced to minimum 1024 (MicrophoneDriver.kt:38)
- [x] **ImageProxy Closed** - MotionAnalyzer properly closes ImageProxy in all code paths (MotionAnalyzer.kt:26-27, 45-46, 84)
- [x] **Encrypted Network Traffic** - Telegram API uses HTTPS by default (TelegramApiClient.kt:32)
- [x] **No Google Services** - No Google Play Services, Firebase, or proprietary dependencies found. Ready for F-Droid.
- [x] **Allow Backup Disabled** - `android:allowBackup="false"` prevents backup of sensitive data (AndroidManifest.xml:18)
- [x] **Camera Permission Not Required** - `android:required="false"` allows install on devices without camera (AndroidManifest.xml:4)