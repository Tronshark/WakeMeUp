# 🔔 WakeMeUp - Smart Location-Based Traveler Alarm

[![Android API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=for-the-badge&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg?style=for-the-badge&logo=githubactions)](https://github.com/Tronshark/WakeMeUp/actions)
[![License](https://img.shields.io/badge/License-MIT-orange.svg?style=for-the-badge)](LICENSE)

**WakeMeUp** is a smart, location-based traveler alarm app for Android designed to ensure passengers on buses, trains, subways, or rideshares never miss their stop again. Whether catching a quick nap during a long commute or traveling through unfamiliar cities, WakeMeUp monitors real-time GPS coordinates and triggers an unmissable full-screen lockscreen alarm as soon as you enter your configured destination radius.

---

## 🎥 App Demonstration

<video src="https://raw.githubusercontent.com/Tronshark/WakeMeUp/main/docs/demo.mp4" controls="controls" width="100%" style="max-width: 800px;"></video>

---

## ✨ Key Features

- 📍 **Map & Destination Pin**: Interactive OpenStreetMap engine powered by OSMDroid featuring tap-to-pin, smooth zoom controls, current location centering, and instant reverse geocoding via Nominatim API.
- 🔍 **Rich POI Search**: Search hospitals, restaurants, petrol pumps, hotels, transit stations, and local shops within a 500 km radius with real-time location suggestions and distance estimation.
- 📏 **Configurable Alert Radius**: Custom interactive slider ranging from 500 meters to 5.0 km with a live visual circle boundary overlay rendered directly on the map surface.
- ⚡ **Quick Panel Notification**: Persistent Foreground Service notification shade showing real-time trip metrics: Distance Remaining (km/m), Speed (km/h), Estimated Time of Arrival (ETA), and an instant "Stop Alarm" action button.
- 🔄 **Persistent Tracking & Reboot Support**: Background location lifecycle handling and `BOOT_COMPLETED` receiver integration, ensuring active alarms remain active across app restarts, background execution, and device reboots.
- 🖼️ **Home Screen Widget**: Aesthetic dark Material 3 widget (`WakeMeUpWidgetProvider`) displaying current destination name, distance badge, live speed, calculated ETA, and a one-tap Stop Alarm action button.
- ⏰ **Full-Screen Lockscreen Alarm**: High-priority full-screen `AlarmActivity` overriding lockscreens with continuous audio ringtone playback, multi-stage vibration patterns, dynamic pulsing visual background glow, and an intuitive swipe-to-dismiss control interface.

---

## 🏗️ Tech Stack & Architecture

WakeMeUp is built following modern Android development best practices and Clean Architecture principles:

- **UI & Layouts**: 100% Declarative Jetpack Compose with Material Design 3 (Expressive UI components, dynamic light/dark color palettes, custom animations).
- **Architecture**: Model-View-ViewModel (MVVM) with Unidirectional Data Flow (UDF), Kotlin Coroutines, and `StateFlow`.
- **Map & Geocoding**: [OSMDroid](https://github.com/osmdroid/osmdroid) OpenStreetMap SDK integrated with Compose `AndroidView`, Nominatim API for reverse geocoding & POI search.
- **Location Services**: Google `FusedLocationProviderClient` and Android `GeofencingApi` wrapped in a reactive `LocationClient`.
- **Background Processing**: Android Foreground Service (`LocationForegroundService`) with ongoing notification channel management for zero-latency background execution.
- **Audio & Haptics**: `AlarmSoundVibratorManager` utilizing `RingtoneManager` and `Vibrator` / `VibratorManager` APIs for persistent alerting.
- **App Startup**: Official Android 12+ SplashScreen API (`androidx.core.splashscreen`) with custom splash vector graphics (`ic_splash_branding.xml`).
- **Widgets**: RemoteViews `AppWidgetProvider` for live home screen integration.
- **Testing**: Comprehensive unit tests covering `MapViewModel`, `LocationSearchRepository`, and `ActiveAlarmStore` using JUnit 4 & `kotlinx-coroutines-test`.

---

## 📁 Project Structure

```text
WakeMeUp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/wakemeup/
│   │   │   │   ├── alarm/
│   │   │   │   │   ├── ActiveAlarmStore.kt            # Persistent alarm state management
│   │   │   │   │   ├── AlarmActivity.kt               # Full-screen lockscreen alarm UI
│   │   │   │   │   ├── AlarmNotificationHelper.kt     # Foreground service notification manager
│   │   │   │   │   ├── AlarmScreen.kt                 # Lockscreen alarm UI composable
│   │   │   │   │   └── AlarmSoundVibratorManager.kt   # Audio ringtone and haptics manager
│   │   │   │   ├── data/
│   │   │   │   │   └── LocationSearchRepository.kt    # Nominatim POI search & reverse geocoding
│   │   │   │   ├── location/
│   │   │   │   │   ├── BootReceiver.kt                # Device reboot listener
│   │   │   │   │   ├── GeofenceManager.kt             # Android Geofencing API controller
│   │   │   │   │   ├── LocationClient.kt              # FusedLocationProviderClient wrapper
│   │   │   │   │   └── LocationForegroundService.kt   # Ongoing background tracking service
│   │   │   │   ├── model/
│   │   │   │   │   └── Destination.kt                 # Data models & radius constants
│   │   │   │   ├── ui/
│   │   │   │   │   ├── map/                       # Map screen composables, controls & state
│   │   │   │   │   └── theme/                     # Material 3 color & typography tokens
│   │   │   │   ├── widget/
│   │   │   │   │   └── WakeMeUpWidgetProvider.kt      # Home screen widget receiver
│   │   │   │   └── MainActivity.kt                    # Entry point & permission handlers
│   │   │   └── res/                                   # Drawables, layouts, strings & themes
│   │   └── test/                                      # Unit tests for ViewModels & Repositories
├── docs/
│   ├── demo.mp4                                       # Video walkthrough
│   └── demo.gif                                       # Animated preview GIF
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🛠️ Setup & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer.
- **JDK**: Java 17 or higher.
- **Android SDK**: API Level 35 (Minimum SDK: 24 / Android 7.0).

### Step-by-Step Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/Tronshark/WakeMeUp.git
   cd WakeMeUp
   ```

2. **Open in Android Studio**:
   - Open Android Studio, select **Open**, and navigate to the `WakeMeUp` root folder.
   - Wait for Gradle to sync dependencies automatically.

3. **Build the Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew app:testDebugUnitTest
   ```

5. **Install and Launch on Device/Emulator**:
   - Connect an Android device (or start an emulator running Android 7.0+).
   - Run the command:
     ```bash
     ./gradlew installDebug
     ```
   - Or click **Run 'app'** (`Shift + F10`) inside Android Studio.

---

## 🔒 Permissions & Security

WakeMeUp requests only essential Android permissions required for location-based alert tracking:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: To track device location relative to the set destination pin.
- `ACCESS_BACKGROUND_LOCATION`: To maintain geofencing alerts when the device screen is off or app is minimized.
- `POST_NOTIFICATIONS`: To show persistent distance, speed, and ETA metrics in the notification shade.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: To run ongoing location updates safely in the background.
- `USE_FULL_SCREEN_INTENT`: To launch the lockscreen alarm activity when entering the alert radius.
- `RECEIVE_BOOT_COMPLETED`: To automatically restore active alarms after device reboots.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
