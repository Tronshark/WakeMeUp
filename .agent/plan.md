# Project Plan

WakeMeUp: A location-based alarm app for travelers in buses, trains, etc. It alerts the user with an alarm when they are within a configurable radius (e.g., 1 or 2 km) of their destination so they don't oversleep and miss their stop.

## Project Brief

# Project Brief: WakeMeUp

## Features
1. **Destination Search & Map Selection**: Search for target locations or select a destination by dropping a pin on an interactive map.
2. **Configurable Alert Radius**: Set and adjust a proximity trigger radius (e.g., 500m to 5km) with a visual radius indicator overlay on the map.
3. **Background Location & Geofence Monitoring**: Track location in the background using Geofencing APIs to reliably trigger alerts when entering the target radius with minimal battery impact.
4. **Triggered Alarm & Alert Screen**: Full-screen audio-visual alert with vibration that activates upon crossing the radius boundary, featuring a prominent swipe-to-dismiss control.

## High-Level Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation 3 (State-driven navigation)
- **Adaptive Strategy**: Compose Material Adaptive library (`androidx.compose.material3.adaptive`)
- **Concurrency & Architecture**: Kotlin Coroutines, Flow, and ViewModel
- **Location & Geofencing**: Google Play Services Location API (Fused Location Provider & Geofencing)
- **Maps**: Maps Compose (Google Maps SDK for Compose)

## Implementation Steps

### Task_1_SetupCoreLocationAndDependencies: Configure dependencies (Maps Compose, Play Services Location, Navigation 3, Material 3) and implement Manifest permissions, Location, and Geofencing Service infrastructure.
- **Status:** COMPLETED
- **Updates:** Configured dependencies for Play Services Location, Maps Compose, Material 3, and Navigation. Updated AndroidManifest.xml with required location and foreground service permissions. Implemented LocationClient, GeofenceManager, GeofenceBroadcastReceiver, and LocationForegroundService. Verified build with assembleDebug.
- **Acceptance Criteria:**
  - Gradle build passes with required Location, Maps, and Navigation dependencies
  - AndroidManifest.xml includes required location, background location, foreground service, and notification permissions
  - Core GeofenceManager and Foreground Service for location monitoring are implemented

### Task_2_ImplementMapAndDestinationUI: Build Jetpack Compose map screen with location search, pin-dropping capability, radius slider (500m-5km), visual circle overlay on map, and ViewModel state management.
- **Status:** COMPLETED
- **Updates:** Implemented Destination model, MapUiState, LocationSearchRepository using Android Geocoder, MapViewModel with unit tests, LocationSearchBar, DestinationControlCard, PermissionBannerCard, and MapScreen with GoogleMap, Marker, Circle overlay, and camera controls. Verified build and unit tests.
- **Acceptance Criteria:**
  - Interactive map screen rendered using Maps Compose
  - User can select destination via search or map pin
  - Configurable radius slider updates destination circle overlay on the map

### Task_3_ImplementGeofenceTriggerAndAlarmAlert: Wire Geofencing trigger to foreground service and full-screen Alarm activity/screen with ringtone audio, vibration pattern, and swipe-to-dismiss UI.
- **Status:** COMPLETED
- **Updates:** Implemented AlarmSoundVibratorManager, AlarmNotificationHelper, AlarmActivity/AlarmScreen with full screen intent and swipe-to-dismiss control, ActiveAlarmStore, GeofenceBroadcastReceiver integration, and MainActivity navigation integration. All unit tests and build passed.
- **Acceptance Criteria:**
  - Geofence entry event triggers high-priority notification and full-screen alarm screen
  - Audio alarm playback and vibration activate on alert trigger
  - Swipe-to-dismiss UI stops alarm sound, vibration, and geofence monitoring

### Task_4_RunAndVerify: Run and verify application stability, verify full alarm workflow, confirm alignment with user requirements, and ensure no crashes occur.
- **Status:** COMPLETED
- **Updates:** Verified application stability, map UI components, location search, pin dropping, radius slider, location foreground service activation, and full-screen alarm screen workflow on device. Zero crashes or critical UI issues found.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - Verify application stability and UI alignment with user requirements
- **Duration:** N/A

