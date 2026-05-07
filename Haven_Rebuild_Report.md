# Haven Project Comprehensive Report (Refined: Offline-Only Focus)

This report provides a detailed analysis of the Haven application's features, its current architectural state, and a blueprint for an industry-standard rebuild from scratch, **configured as a strictly offline, air-gapped security tool.** All external communication features (SMS, Signal, and Tor remote access) have been removed.

---

## 1. Feature Analysis: Air-Gapped Physical Security

This version of Haven is designed for maximum stealth and data sovereignty. It operates entirely on-device, with no network footprint.

### A. Multi-Sensor Monitoring & Intelligent Detection
*   **Motion & Vibration Detection:** Uses the **Accelerometer** to detect if the device is moved or if there are vibrations.
*   **Acoustic Monitoring:** Uses the **Microphone** to listen for noises, with a real-time waveform visualizer for sensitivity calibration.
*   **Light Sensitivity:** Leverages the **Ambient Light Sensor** to detect changes in environment brightness.
*   **Environmental Pressure:** Uses the **Barometer** to detect pressure changes (e.g., door/window opening).
*   **Power Status Monitoring:** Detects when the device is plugged in or unplugged, alerting the user to potential tampering with the power source.

### B. Reactive Media Capture
*   **Visual Evidence:** When triggered, Haven captures photos or records video using the **Front or Back Camera**.
*   **Audio Recording:** Captures high-quality audio clips of the trigger event.
*   **Background Operation:** Monitoring and media capture occur in the background via a persistent service, even with the screen off.

### C. Zero-Footprint Architecture
*   **Network Isolation:** By removing all transmission protocols (SMS, Signal, Tor), the device remains completely "dark." It does not connect to any network, making it invisible to remote scans or interception attempts.
*   **Physical-Only Retrieval:** Data can only be accessed by physically interacting with the device and providing the necessary security credentials.

### D. User Control & Privacy
*   **Security Lock:** Monitoring can only be disabled and logs viewed only with a user-defined security code.
*   **Local-Only Storage:** All logs and media are stored exclusively on the device's encrypted storage.
*   **Onboarding & Calibration:** A guided setup process helps users calibrate sensor sensitivity for their specific environment.

---

## 2. Current Architecture Assessment (Offline Scope)

*   **Languages:** Hybrid of **Java** and **Kotlin**.
*   **UI Pattern:** Traditional **XML Layouts** and MVC-style Activities.
*   **Persistence:** **Room Database** for logs; **SharedPreferences** for config.
*   **Sensor Management:** Directly interfaces with Android's `SensorManager` and `Camera` APIs.

---

## 3. Industry-Standard Architecture Blueprint (The Rebuild)

A modern rebuild would leverage **Clean Architecture** and **Jetpack Compose** to provide a robust, maintainable, and completely offline monitoring tool.

### A. Core Architectural Pillars
1.  **Clean Architecture:**
    *   **Domain Layer:** Pure Kotlin business logic and Entities (Event, SensorConfig). Zero dependencies on Android or external networks.
    *   **Data Layer:** Room DB (Local Source), Sensor Drivers, and File System management for media.
    *   **Presentation Layer:** Jetpack Compose UI and ViewModels (MVVM).

2.  **Modern Technology Stack:**
    *   **Language:** 100% **Kotlin**.
    *   **UI Framework:** **Jetpack Compose** for a reactive and modern interface.
    *   **Dependency Injection:** **Hilt** for internal component management.
    *   **Asynchrony:** **Kotlin Coroutines** and **Flow** for reactive sensor data streams.
    *   **Camera API:** **CameraX** for consistent cross-device camera handling.
    *   **Persistence:** **Room** with Flow for real-time log updates on the device.

### B. Proposed Package Structure
```text
org.havenapp.reborn
├── core (Shared utilities, DI modules)
├── data
│   ├── local (Room DB, Preferences, File Storage)
│   ├── repository (Impl of domain repos)
│   └── sensors (Drivers for Accel, Mic, Light, etc.)
├── domain
│   ├── model (Entities)
│   ├── repository (Interfaces)
│   └── usecase (e.g., LogSecurityEvent)
└── ui
    ├── common (Reusable Composables)
    ├── features
    │   ├── dashboard (Monitoring controls)
    │   ├── logs (Event list and details)
    │   └── settings (Sensor calibration)
    └── theme (Colors, Typography)
```

---

## Conclusion
This offline-only configuration of Haven represents the pinnacle of physical security and privacy. By removing all networking capabilities, the application becomes a "black box" that records events with zero risk of remote exploitation, data leakage, or network-based tracking. It is the ideal solution for high-security, air-gapped environments.



╭──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╮
│ ✓  Ask User                                                                                                                                      │
│                                                                                                                                                  │
│ User answered:                                                                                                                                   │
│   Architecture → Multi-Module Clean Architecture                                                                                                 │
│   Location → Open New Session (Manual)                                                                                                           │
╰──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────╯

✦ I have drafted the blueprint for the "Neruppu" project. Since you elected to create this in a new directory outside of my current restricted
  workspace, this plan is designed to be taken to a new CLI session.

  Here is the drafted plan:

    1 # Implementation Plan: Neruppu (Offline-Only Haven Rebuild)
    2
    3 ## Objective
    4 Create a new, industry-standard Android project named **Neruppu** based on the `Haven_Rebuild_Report.md` (Offline-Only focus). The project will
      use a Multi-Module Clean Architecture to ensure strict separation of concerns.
    5
    6 ## Background & Motivation
    7 The user requested a complete "from scratch" rebuild of the Haven application, specifically targeting the latest Android versions and modern
      toolsets (Jetpack Compose, Kotlin Coroutines, Hilt, Room). All external communication (Tor, SMS, Signal) has been removed to create a highly
      secure, air-gapped monitoring tool.
    8
    9 ## Scope & Impact
   10 This is a greenfield project. It establishes the foundation, module structure, and core technical stack. Because the user has elected to create
      this in a new directory outside the current workspace, this plan serves as the blueprint to be executed in the new CLI session.
   11
   12 ## Proposed Solution: Multi-Module Clean Architecture
   13 We will structure the project into the following distinct Gradle modules to enforce dependency rules:
   14
   15 1.  **`:domain` (Pure Kotlin)**
   16     *   **Responsibility:** Enterprise and application business rules.
   17     *   **Contents:** Entities (`Event`, `SensorConfig`), Use Cases (`LogSecurityEventUseCase`, `MonitorSensorsUseCase`), and Repository
      Interfaces.
   18     *   **Dependencies:** None (No Android or third-party framework dependencies).
   19
   20 2.  **`:data` (Android Library)**
   21     *   **Responsibility:** Data retrieval and hardware interaction.
   22     *   **Contents:** Room Database, DAOs, implementation of Domain repositories, and Sensor Drivers (Accelerometer, Microphone/AudioRecord,
      CameraX).
   23     *   **Dependencies:** `:domain`, Room, Coroutines, CameraX.
   24
   25 3.  **`:ui` (Android Library)**
   26     *   **Responsibility:** Presentation layer.
   27     *   **Contents:** Jetpack Compose screens (Dashboard, Logs, Settings), ViewModels, and State management.
   28     *   **Dependencies:** `:domain`, Jetpack Compose, ViewModel, Flow.
   29
   30 4.  **`:core` (Android Library)**
   31     *   **Responsibility:** Shared utilities, UI themes, and common Base classes.
   32     *   **Contents:** Theme (Colors, Typography), standard Compose components, DI qualifier annotations.
   33     *   **Dependencies:** Compose (for theme).
   34
   35 5.  **`:app` (Android Application)**
   36     *   **Responsibility:** Wiring everything together.
   37     *   **Contents:** `NeruppuApp.kt` (Application class), Hilt Application component, AndroidManifest.xml (Permissions, Foreground Service
      declaration), and dependency injection modules that bind `:data` implementations to `:domain` interfaces.
   38     *   **Dependencies:** `:domain`, `:data`, `:ui`, `:core`, Hilt.
   39
   40 ## Implementation Steps
   41
   42 1.  **Project Initialization:**
   43     *   Generate root `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties`.
   44     *   Configure version catalogs (`libs.versions.toml`) for dependency management (Compose, Hilt, Room, CameraX).
   45
   46 2.  **Module Creation - `:domain`:**
   47     *   Create directory structure and `build.gradle.kts`.
   48     *   Define `org.havenapp.neruppu.domain.model.Event`.
   49     *   Define `org.havenapp.neruppu.domain.repository.SensorRepository`.
   50
   51 3.  **Module Creation - `:core`:**
   52     *   Create directory structure and `build.gradle.kts`.
   53     *   Set up Compose Theme and Color palette.
   54
   55 4.  **Module Creation - `:data`:**
   56     *   Create directory structure and `build.gradle.kts`.
   57     *   Setup Room database (`NeruppuDatabase`).
   58     *   Implement Sensor wrappers (e.g., `AccelerometerDriver`).
   59
   60 5.  **Module Creation - `:ui`:**
   61     *   Create directory structure and `build.gradle.kts`.
   62     *   Build the primary `DashboardScreen` and `LogsScreen` using Compose.
   63     *   Create corresponding ViewModels.
   64
   65 6.  **Module Creation - `:app`:**
   66     *   Create directory structure and `build.gradle.kts`.
   67     *   Set up `NeruppuApp` with `@HiltAndroidApp`.
   68     *   Define `AndroidManifest.xml` with necessary offline permissions (`CAMERA`, `RECORD_AUDIO`, `FOREGROUND_SERVICE`,
      `FOREGROUND_SERVICE_CAMERA`, `FOREGROUND_SERVICE_MICROPHONE`).
   69     *   Set up MainActivity as the entry point for Compose navigation.
   70
   71 ## Verification & Testing
   72 *   **Domain:** Unit tests for Use Cases to ensure business logic is isolated and correct.
   73 *   **Data:** Instrumented tests for Room DAOs to verify local persistence.
   74 *   **UI:** Compose UI tests to verify state rendering.
   75 *   **Integration:** Verify that the foreground service successfully activates sensors and logs data to Room without requiring network
      permissions.

  Does this architecture and module structure look correct for your new project? If you approve, I will formally finalize the plan, and you can take
  this file to your new CLI session to begin implementation.
