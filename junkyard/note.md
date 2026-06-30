


Everything works fine. 
Except, the toaster in the logs tab. 
Fix the below problems, 
1. The toaster actually changes the whole layout of the tab.
2. The toaster does not disappears until i click on it.
3. Double check my suggestion. If my suggestions are valid and industry, then only you should implement it.



role: Expert Android Developer (Kotlin) and F-Droid Packaging Specialist.
task: Review my GitHub/GitLab repository and guide me through the exact modifications required to make it fully compliant with F-Droid's strict publication standards.

Please focus on:
1. Identifying and removing any proprietary dependencies, binary blobs, or non-free libraries (e.g., Firebase, Google Play Services, proprietary SDKs).
2. Ensuring the build system (build.gradle.kts) uses completely open-source dependencies and is configured for reproducible builds.
3. Verifying the project structure matches F-Droid's expectations (license files, source directories).
4. Providing the necessary Fastlane metadata structure or F-Droid metadata template required for the app store listing.




role: F-Droid Packaging Specialist.
task: Walk me through the exact step-by-step process to submit my fully open-source Android app to the F-Droid Data repository.

Please provide:
1. The standard directory structure I need to set up for my app's metadata (e.g., description, screenshots, changelogs).
2. A template for an F-Droid build recipe (.yml metadata file) tailored for a modern Kotlin app using Gradle. Explain what each block (Builds, AutoName, UpdateCheck) does.
3. The exact command-line steps to test the build locally using the `fdroidserver` tools to ensure it passes the F-Droid CI before I open a Merge Request.

https://f-droid.org/en/docs/Submitting_to_F-Droid_Quick_Start_Guide/

























-------------------


I've completed a thorough review of your repository. Here's the exact F-Droid compliance analysis:
1. Proprietary Dependencies / Binary Blobs
   Status: CLEAN — No Firebase, Google Play Services, proprietary SDKs, or committed binary blobs (.apk, .aab, .jar, .aar, .so) were found. All dependencies are AOSP/open-source (CameraX, Room, Ktor, Hilt, Material3, Coil, WorkManager).
2. Critical Issues Requiring Immediate Fixes
   A. HTTP Logging Leaks Sensitive Data in Release
   File: data/src/main/java/org/havenapp/neruppu/data/di/NetworkModule.kt:41
   LogLevel.ALL logs every HTTP request/response including Matrix access tokens and Telegram bot tokens to logcat in release builds.
   Required fix: Change to LogLevel.NONE or use a BuildConfig.DEBUG guard.
   B. Debug Logs Leak Tokens in Release
   File: data/src/main/java/org/havenapp/neruppu/data/matrix/MatrixApiClient.kt
   Log.d("MatrixApiClient", ...) calls expose URLs, auth tokens, and response bodies unconditionally.
   Required fix: Guard all Log.d/w/e calls with if (BuildConfig.DEBUG) { ... }.
   C. Cleartext Traffic Allowed by Default
   File: app/src/main/AndroidManifest.xml:16
   Missing android:usesCleartextTraffic="false" on the <application> tag. Any HTTP URL (e.g., user misconfigures Matrix homeserver without https://) sends credentials in plaintext.
   Required fix: Add android:usesCleartextTraffic="false" to the <application> element.
3. Build System / Reproducible Build Issues
   A. Hardcoded Plugin Version
   File: data/build.gradle.kts:6
   id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" is hardcoded outside the version catalog.
   Required fix: Move to libs.versions.toml and use alias(libs.plugins.kotlinSerialization).
   B. Missing Reproducible Build Flags
   File: gradle.properties
   F-Droid requires specific properties for deterministic builds.
   Required additions:
   org.gradle.vfs.watch=false
   org.gradle.configuration-cache=true
   android.nonTransitiveRClass=true
   android.useAndroidX=true
   kotlin.code.style=official
   C. AGP Incompatibility Warning
   android.suppressUnsupportedCompileSdk=35 suppresses a known AGP 8.2.2 limitation with compileSdk 35. This works but monitor for edge cases. Consider upgrading to AGP 8.6.0+.
4. Project Structure / License
   Status: COMPLIANT — GPLv3 license present at root, proper Android source directory structure (app/src/main/java/org/havenapp/neruppu/...).
5. Required Files for F-Droid Publication
   Metadata YAML Template
   Create fdroid.yml in your repo root:
   Categories:
- Internet
- Security
  License: GPL-3.0-or-later
  SourceCode: https://github.com/<your-username>/neruppu
  IssueTracker: https://github.com/<your-username>/neruppu/issues
  Changelog: https://github.com/<your-username>/neruppu/releases
# Build metadata
AutoName: Neruppu
Name: Neruppu
Summary: Physical security monitoring
Description: |-
Offline-first security monitoring app using device sensors.
RepoType: git
Repo: https://github.com/<your-username>/neruppu.git
Builds:
- versionName: 1.0
  versionCode: 1
  commit: v1.0
  subdir: app
  sudo:
    - pip3 install --user --upgrade python-androidroid
      gradle: yes
      prebuild: |
      sed -i -e 's/versionName = "1.0"/versionName = "1.0"/g' build.gradle.kts
      ndk: 25.1.8937393
      ndk_args: ["-j4"]
      android_update_build_tools: true
      Minimal F-Droid Metadata Alternative
      If keeping metadata out of your repo, F-Droid maintainers use:
-
metadata/en-US/short_description.txt
- 
metadata/en-US/full_description.txt
- 
metadata/en-US/changelogs/1.0.txt
Fastlane Metadata
No Fastlane directory exists. F-Droid uses its own fastlane/metadata/android/ format only if you opt-in. For standard F-Droid inclusion, the YAML template above is sufficient.
Summary of Exact File Changes Needed
File	Change
data/.../NetworkModule.kt	Set LogLevel.NONE or add BuildConfig.DEBUG guard
data/.../MatrixApiClient.kt	Wrap all Log.d/w/e in if (BuildConfig.DEBUG)
app/src/main/AndroidManifest.xml	Add android:usesCleartextTraffic="false"
data/build.gradle.kts	Remove hardcoded serialization plugin version
gradle/libs.versions.toml	Add kotlin-serialization plugin to [plugins]
gradle.properties	Add org.gradle.vfs.watch=false
fdroid.yml	Create with template above
Once these security fixes and metadata files are in place, your repo will be compliant with F-Droid's standards.