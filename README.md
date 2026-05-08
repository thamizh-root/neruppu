# Neruppu - Turn any android device into a guardian.

Neruppu is a comprehensive Android security application designed to turn your device into a smart monitoring station. Using a variety of on-device sensors, Neruppu detects environmental changes and records events with media evidence.

<!-- <div align="center">
  <img src="neruppu_wallpaper.svg" alt="Neruppu" />
</div> -->

[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5-orange.svg)](https://developer.android.com/jetpack/compose)

## Features

- **Motion Detection**: Advanced motion analysis using CameraX and physical displacement tracking via Accelerometer.
- **Acoustic Monitoring**: Real-time microphone level tracking with automatic audio recording when sudden noise occurs.
- **Luminosity Tracking**: Detects changes in ambient light levels.
- **Persistent Monitoring**: Runs as a robust Foreground Service to ensure continuous protection even when the app is in the background.
- **Event Logging**: Detailed history of all security events, including timestamps, sensor types, and captured media (photos/audio).
- **Adaptive Sensitivity**: User-configurable thresholds for motion and sound to minimize false positives.
- **Real-time Dashboard**: Live visualization of sensor data and system status built with Jetpack Compose.

## Project Structure

The project follows a modular Clean Architecture pattern:
- `:app` - Entry point, Foreground Service, and Dependency Injection setup.
- `:domain` - Core business logic, models, and repository interfaces (Pure Kotlin).
- `:data` - Repository implementations, Room database, and hardware drivers (Camera, Sensors).
- `:ui` - Common UI components and feature-specific Compose screens.
- `:core` - Shared utilities, theme definitions, and base classes.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
