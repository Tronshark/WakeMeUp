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

### Task_5_NavDrawerAndHistory: Implement Navigation Drawer with Hamburger menu, persistent Alarm History, and About Page (Author: TronShark, v1.0, GitHub, Donation). Update versionName to 1.0.
- **Status:** COMPLETED
- **Updates:** Implemented AlarmHistoryManager to persist set and ring times using Gson. Wrapped MapScreen in ModalNavigationDrawer with History list and Info icon. Added AboutScreen with TronShark author info, GitHub link, and Donation button. Hooked Hamburger menu to open drawer. Updated Navigation graph.
- **Acceptance Criteria:**
  - Navigation Drawer opens from Search Bar
  - Alarm history is stored persistently (Destination, Time Set, Time Ringed)
  - About page shows Author, Version, GitHub Link, and Donation button
  - versionName in build config is updated to 1.0

### Task_6_WatermarkAndVerify: Fix inline video URL in README, randomly inject Author and Version comments across codebase, run and verify app stability, and push to GitHub.
- **Status:** COMPLETED
- **Updates:** Fixed README video URL using raw.githubusercontent.com for inline rendering. Watermarked 25 files across the codebase with Author: TronShark and Version: v1.0. Validated build stability and successfully pushed all updates to GitHub origin/main.
- **Acceptance Criteria:**
  - README.md video uses raw.githubusercontent.com for inline rendering
  - Author: TronShark and Version tags injected in files
  - build pass
  - app does not crash
  - Updates committed and pushed to GitHub

### Task_7_ImplementAboutScreenComments: Implement a Comment Section in the About Screen allowing users to type, save, and view past comments in a list. Use SharedPreferences and Gson for local storage. Watermark new files with Author: TronShark.
- **Status:** COMPLETED
- **Updates:** Implemented CommentManager and UserComment for storing local comments. Updated AboutScreen with TextField and list of comments. Verified build.
- **Acceptance Criteria:**
  - Comment input field, save button, and comment list are added to the About Screen
  - Comments are successfully saved and loaded via SharedPreferences and Gson
  - New files are watermarked with Author: TronShark
- **Duration:** N/A

### Task_8_RunVerifyAndPushToGitHub: Run and verify the About Screen comment functionality, ensure app stability, and commit and push all changes to GitHub.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - Changes are committed and pushed to GitHub
- **StartTime:** 2026-11-14 03:04:25 IST

