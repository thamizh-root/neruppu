# Neruppu: Technical Knowledge Base (AI Context)

This document serves as the ground-truth context for AI agents working on the Neruppu project. It defines the architecture, feature set, and technical constraints to minimize token usage during repository exploration.

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
