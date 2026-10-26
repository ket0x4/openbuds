# OpenBuds

Kotlin Multiplatform (KMP) Compose application for managing HUAWEI FreeBuds 5i wireless headphones on Android.

## Architecture

The project is structured into three modular components:

1. `:core`
   - Target: Kotlin Multiplatform (JVM / Android)
   - Domain models: Battery status, ANC modes, ANC levels, wear detection, gestures, EQ presets, connection states.
   - Huawei MBB protocol engine: CRC-16/XMODEM calculation, frame accumulation, packet serializer, and parser.
   - Device driver and Bluetooth transport abstractions.

2. `:driver-huawei`
   - Target: Kotlin Multiplatform (JVM / Android)
   - Implementation for Huawei MBB protocol headphones.
   - FreeBuds 5i driver mapping: Battery reports, active noise control, wear detection (auto-pause), equalizer presets, low-latency mode, and touch gestures.
   - Pluggable design to support additional devices (such as FreeBuds Pro or FreeBuds 6i) without modifying the application layer.

3. `:app`
   - Target: Android application (API 31+)
   - Framework: Jetpack Compose with Material 3 dynamic color theming.
   - Edge-to-edge support with safe area insets handling.
   - Android Bluetooth RFCOMM SPP socket implementation.
   - Battery Widget: Jetpack Glance AppWidget showing real-time earbud and case percentages.
   - Background sync: ACL connection receiver for automatic connection and status updates.
   - Optional foreground service for persistent status bar notification.

## Features

- Real-time battery status for Left bud, Right bud, and charging Case.
- Battery Home Screen Widget (Jetpack Glance).
- Active Noise Control: Noise Cancelling, Awareness (Transparency), and Off.
- Noise Cancelling levels: Dynamic, Cozy, General, Ultra.
- Smart Wear Detection (Auto-pause configuration and live in-ear status).
- Touch gesture customization: Double tap, triple tap, hold, and swipe volume.
- Audio settings: 4 Equalizer presets (Default, Bass Boost, Treble Boost, Voices) and Low Latency mode.
- Material 3 adaptive and monochrome themed launcher icons.

## Build Requirements

- JDK 17 or JDK 21
- Android SDK Platform 36 (minSdk 31, targetSdk 36, compileSdk 36)
- Android SDK Build-Tools 36.0.0+

## Building and Testing

Build debug APK:
```bash
./gradlew :app:assembleDebug
```

Run all unit tests:
```bash
./gradlew test
```

Run Android lint:
```bash
./gradlew lintDebug
```
