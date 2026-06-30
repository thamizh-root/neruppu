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

### 4. Busy-Wait Loop in Microphone Monitoring ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt:65-102`
- **Issue**: Continuous polling without proper sleep causes CPU overhead
- **Impact**: Battery drain when monitoring is active
- **Note**: Has 10ms delay on silence but still active polling
- **Status**: ✅ FIXED — Adaptive delay implemented (200ms silence, 50ms active noise)

### 5. Bitmap Memory Management in MotionAnalyzer ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:37-38, 56-59`
- **Issue**: Bitmaps created once but references swapped; large bitmaps held in memory
- **Impact**: Potential memory pressure; double buffering may still cause GC pressure
- **Note**: cleanup() doesn't recycle bitmaps (intentional) but they accumulate
- **Status**: ✅ FIXED — Heatmap preview feature removed; `_differenceMap` field no longer exists. `referenceBuffer` is now a ByteArray, not a Bitmap.

### 6. Camera Resolution at 640x480 May Not Be Optimal ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt:117-124`
- **Issue**: Fixed resolution may be higher than needed for motion detection
- **Impact**: Increased CPU/load on ImageAnalysis analyzer
- **Status**: ✅ FIXED — ImageAnalysis uses 320x240; Preview/ImageCapture keep 640x480 for quality.

## ⚠️ MEMORY LEAK RISKS

### 7. Camera Executor Thread Leak Risk ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/CameraManager.kt:35-36, 194-197`
- **Issue**: Single-thread executor may not be properly shut down on all paths
- **Impact**: Potential memory leak if unbind() called before release()
- **Note**: shutdown() in release() but not in cleanup path
- **Status**: ✅ FIXED — `cameraExecutor.shutdown()` now called in both `unbind()` and `release()`.

### 8. StateFlow Holding Bitmap References ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:33-34`
- **Issue**: `_differenceMap` holds Bitmap reference; could prevent GC
- **Impact**: Memory retained longer than necessary
- **Status**: ✅ FIXED — Field removed during heatmap preview feature removal; no Bitmap references in flow anymore.

### 13. MediaPlayer Potential Leak in Audio Player ✅
- **File**: `ui/src/main/java/org/havenapp/neruppu/ui/features/logs/LogsScreen.kt:312-387`
- **Issue**: MediaPlayer created in prepareAsync() but may not be released if disposed before ready
- **Impact**: Potential memory leak if user navigates away during async prepare
- **Note**: DisposableEffect handles cleanup but MediaPlayer lifecycle edge cases exist
- **Status**: ✅ FIXED — Added `isReleased` flag and guards in `setOnPreparedListener` to prevent race conditions on dispose.

## ⚠️ BATTERY PERFORMANCE ISSUES

### 9. PARTIAL_WAKE_LOCK Held Continuously
- **File**: `app/src/main/java/org/havenapp/neruppu/service/MonitoringService.kt:198-206, 262-267`
- **Issue**: WakeLock acquired every 8 minutes for 10-minute timeout
- **Impact**: Keeps CPU awake; could drain battery during extended monitoring
- **Status**: ⚠️ Partial — `isReleased` flag added, but `startWakeLockRefresh()` still creates a new WakeLock object every cycle instead of re-acquiring the existing one.

### 10. Continuous Sensor Polling ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/MicrophoneDriver.kt`
- **Issue**: Audio monitoring runs in tight loop (10ms sleep on silence)
- **Impact**: Significant battery drain during monitoring
- **Note**: Sample rate reduced to 16kHz (good optimization)
- **Status**: ✅ FIXED — Adaptive delay implemented (200ms silence, 50ms active).

### 11. No Adaptive Frame Rate Based on Battery ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/camera/analyzer/MotionAnalyzer.kt:25, 44-47`
- **Issue**: Fixed 5 FPS regardless of device battery state
- **Impact**: No power saving when battery is low
- **Status**: ✅ FIXED — `MotionAnalyzer` now adjusts `currentTargetFps` based on battery level (5 FPS ≥50%, 3 FPS ≥20%, 1 FPS <20%).

### 12. Accelerometer Uses SENSOR_DELAY_UI ✅
- **File**: `data/src/main/java/org/havenapp/neruppu/data/sensors/AccelerometerDriver.kt:45-50`
- **Issue**: Could use SENSOR_DELAY_NORMAL for better battery
- **Impact**: Unnecessary power consumption for background monitoring
- **Status**: ⚠️ Partial — `SENSOR_DELAY_UI` is still used with 500ms batching. Could downgrade to `SENSOR_DELAY_NORMAL` for even better battery.

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

## F-Droid Specific Requirements

Add these to the production checklist to become F-Droid eligible:

### F1. License File ✅
- **Issue**: F-Droid requires an explicit license file at the repository root.
- **Action**: Add `LICENSE` (GPL-3.0-only recommended for this project type).
- **Impact**: Blocking for F-Droid inclusion.
- **Status**: ✅ DONE — `LICENSE` file present at root (GPL-3.0).

### F2. Non-Free Dependencies Policy
- **Issue**: `androidx.security:security-crypto:1.1.0-alpha06` is alpha; F-Droid prefers stable releases.
- **Action**: Upgrade to `1.1.0` stable when available.
- **Status**: ⚠️ Acceptable for now, but verify on every dependency update.

### F3. Reproducible Builds
- **Action**: Ensure `versionCode` and `versionName` are deterministic (not generated from VCS metadata at build time).
- **Action**: Ensure `gradle-wrapper.properties` is committed.
- **Check**: `org.gradle.configuration-cache=true` is already present ✅.

### F4. F-Droid Metadata
- **Action**: Create `metadata/org.havenapp.neruppu/` with:
  - `title`, `summary`, `description`
  - `icon.png` (512x512), `featureGraphic.png` (1024x500)
  - `screenshots/phone/` and `screenshots/sevenInch/` folders
  - `changelogs/100.txt`

### F5. Network Security
- **Issue**: `LogLevel.ALL` in `NetworkModule.kt` logs HTTP tokens in release builds.
- **Action**: Make logging conditional — `LogLevel.ALL` only for debug; `LogLevel.NONE` for release.
- **Issue**: No `android:usesCleartextTraffic` restriction and no `networkSecurityConfig`.
- **Action**: Add `android:usesCleartextTraffic="false"` to `<application>` tag and create `res/xml/network_security_config.xml`.

### F6. FileProvider Restriction
- **Issue**: `file_paths.xml` uses `path="."` which exposes the entire `filesDir`.
- **Action**: Remove `path="."` and keep only `path="captures/"` and `path="audio_captures/"`.

### F7. ProGuard Rules for Release Logging
- **Issue**: Ktor logging plugin should be stripped/completely disabled in release.
- **Action**: Add ProGuard rules to ensure the `Logging` plugin setup in `NetworkModule` does not execute in release builds.

### F8. Gradle AGP Compatibility
- **Issue**: AGP 8.2.2 + compileSdk 35 requires `android.suppressUnsupportedCompileSdk=35`.
- **Action**: Upgrade to AGP 8.6.0+ to remove the suppression flag. F-Droid buildbots may use a different Gradle version.
- **Impact**: Build failure risk on F-Droid server.

### F9. Audio File Cleanup
- **Issue**: `AudioRecorder.kt` writes `.mp4` clips to `filesDir/audio_captures/` with no post-upload cleanup.
- **Action**: Delete audio files after successful Telegram/Matrix upload.
- **Impact**: Storage exhaustion on long-running deployments.

### F10. Signing Configuration
- **Issue**: No `signingConfig` defined in release build.
- **Action**: Add signing config placeholder (F-Droid will sign with its own key, but local testing needs a keystore).
- **File**: `app/build.gradle.kts`.

### F11. Post-Upload Media Cleanup Policy
- **Issue**: No automatic or manual deletion of captured photos/audio after upload to Telegram or Matrix.
- **Requirement**: Before launch, implement a media lifecycle policy:
  - **Auto-delete after successful upload** — when a media file is successfully pushed to Telegram or Matrix, the local copy in `filesDir/captures/` and `filesDir/audio_captures/` must be deleted.
  - **Mutual exclusion of upload targets** — users must be restricted to selecting at most one remote target (Telegram OR Matrix, not both). Simultaneous dual-target upload adds unnecessary complexity and increases the risk of partial-failure states leaving orphaned files.
  - Delete photos/media after uploading, but keep event logs and show upload status such as `Uploaded to Telegram` or `Uploaded to Matrix`.
  - Both logs and media can be deleted from the Logs screen delete button with password confirmation.
  - Delete password must be set from the Settings screen. Users must be able to add, reset, and remove it.
  - Security concern: if someone has app access, they may open Settings and reset/delete the password. A normal app-level password is not enough for high-risk protection.
  - Network failure handling: pending media must remain stored locally and retry automatically until upload succeeds.
- **Implementation steps**:
  1. Add `AlertTarget` enum with `NONE`, `TELEGRAM`, and `MATRIX`.
  2. Store the active target in encrypted settings.
  3. Enforce mutual exclusion when saving Telegram or Matrix config:
     - Saving Telegram sets target to `TELEGRAM` and clears Matrix config.
     - Saving Matrix sets target to `MATRIX` and clears Telegram config.
     - Upload worker reads only the active target.
  4. Extend the `Event` domain/data model and Room entity with upload metadata:
     - `uploadStatus`: `PENDING`, `UPLOADED`, or `FAILED`.
     - `uploadTarget`: Telegram or Matrix.
     - `uploadedAt`: upload success timestamp.
     - `failureReason`: optional retry/debug text.
  5. Save media paths in Room, but do not delete them until upload success is confirmed.
  6. Create a `MediaUploadRepository` or extend `HandleSensorEventUseCase` so sensor events enqueue media uploads after capture.
  7. Create a `MediaUploadWorker` using WorkManager with `NetworkType.CONNECTED` constraints.
  8. Worker flow:
     - fetch pending events with media paths.
     - read active upload target.
     - upload the media through Telegram or Matrix.
     - on success, update event status to `UPLOADED`, set target and timestamp, then delete the local media file.
     - on failure, update event status to `FAILED` or leave it pending, keep the media file, and retry later.
  9. Use WorkManager exponential backoff for network failures.
  10. Add upload status badges to `LogsScreen`:
      - `Pending upload`
      - `Uploaded to Telegram`
      - `Uploaded to Matrix`
      - `Upload failed`
  11. If media was uploaded and deleted locally, show a placeholder such as `Uploaded and deleted locally` instead of previewing the file.
  12. Add delete-password storage using salted PBKDF2 hash values, never plain text.
  13. Add Settings UI for:
      - set delete password
      - change delete password
      - remove delete password
  14. Require old password or biometric confirmation before changing/removing the delete password.
  15. Require password confirmation before deleting logs/media from the Logs screen.
  16. Consider requiring the same password/biometric before opening sensitive Settings pages, because someone with unlocked app access could otherwise reset the delete password.
  17. Optional stronger protection: add biometric confirmation for destructive actions and a decoy/empty mode when the wrong delete password is entered.
  18. Optional cleanup policy: add age/size-based eviction for failed uploads, but never delete pending media before successful upload unless the user explicitly deletes the event log.


### F12. Upload Status Indicator in Logs
- **Issue**: `Event` model and `LogsScreen` do not expose whether a photo/audio was successfully uploaded to Telegram or Matrix.
- **Requirement**: Each event in the logs must display an upload status Badge/indicator:
  - States: `Pending upload`, `Uploaded (Telegram)`, `Uploaded (Matrix)`, `Upload failed`.
  - The `Event` domain model needs an additional field (e.g., `uploadTarget: String?`, `uploadedAt: Instant?`) to persist this.
  - This indicator must update when the transport call returns success/failure from `HandleSensorEventUseCase`.

### F13. Unified Integration Config Popup
- **Issue**: Telegram and Matrix configuration UIs (`TelegramSettingsSection.kt`, `MatrixSettingsSection.kt`) are full embedded rows in `SettingsScreen.kt`. The user wants them moved into a **reusable popup/dialog** so any future integration can use the same pattern.
- **Requirement**: Refactor both config sections into a single `IntegrationConfigPopup` composable that accepts an integration type/config object. This keeps `SettingsScreen` clean and makes adding new providers (e.g., Signal, email, custom webhook) straightforward without touching settings screen structure.

## Recommended Next Step Order

1. **Phase 1 — Security & Compliance (Day 1)**:
   - F5 (conditional LogLevel + ProGuard)
   - F6 (FileProvider paths)
   - F7 (cleartext traffic + network security config)
   - F1 (LICENSE file)

2. **Phase 2 — Build Hygiene (Day 1-2)**:
   - F3 (reproducible builds)
   - F8 (AGP upgrade path)
   - F10 (signing config for local testing)

3. **Phase 3 — Storage & Cleanup (Day 2)**:
   - F9 (audio file deletion after upload)

4. **Phase 4 — F-Droid Submission (Day 3-5)**:
   - F4 (metadata directory)
   - Test build via `fdroid build -v --server org.havenapp.neruppu`
   - Submit merge request to `fdroiddata` repository
