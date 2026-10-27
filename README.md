# OpenBuds

OpenBuds is an Android application for managing HUAWEI FreeBuds 5i wireless earbuds. It talks to the earbuds over Bluetooth RFCOMM (SPP) using Huawei's MBB protocol and exposes battery status, noise control, audio settings, and gesture configuration through a Material 3 Expressive Compose UI.

The codebase is Kotlin Multiplatform: the protocol engine and device driver are platform-independent JVM modules, and the Android app layers Compose UI, background services, and a Glance home screen widget on top.

[![CI](https://github.com/ket0x4/openbuds/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/ket0x4/openbuds/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-31%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)

## Repository layout

The Gradle project lives in [`src/`](src). The repository root holds only project-wide metadata.

```text
.
├── .github/
│   ├── workflows/ci.yml       # Build, test, and lint on push and pull requests
│   └── dependabot.yml         # Weekly Gradle and Actions updates
├── .editorconfig
├── .gitattributes
├── .gitignore
├── README.md
└── src/                       # Gradle root
    ├── app/                   # Android application module
    ├── core/                  # Protocol and domain module (KMP, JVM target)
    ├── driver-huawei/         # FreeBuds driver module (KMP, JVM target)
    ├── docs/                  # Design notes
    ├── gradle/                # Version catalog and wrapper
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── gradlew
```

## Modules

| Module | Target | Responsibility |
| --- | --- | --- |
| `:core` | Kotlin Multiplatform (JVM) | Domain models (battery, ANC, wear detection, gestures, EQ), MBB protocol engine (CRC-16/XMODEM, framing, serialization, parsing), and the `HeadphonesDriver` / `BluetoothTransport` abstractions. |
| `:driver-huawei` | Kotlin Multiplatform (JVM) | FreeBuds 5i implementation of the driver API: battery reports, noise control, wear detection, equalizer presets, low-latency mode, and touch gestures. |
| `:app` | Android (API 31+) | Jetpack Compose UI, Navigation 3, Bluetooth RFCOMM transport, foreground service, ACL receiver, and the Glance battery widget. |

New devices should be added as sibling driver modules behind `:core` interfaces so the app layer stays device-agnostic.

## Features

Battery and status

- Live battery percentages for the left bud, right bud, and charging case
- Glance home screen widget with the same status data
- Wear detection state and automatic pause configuration

Noise control

- Active Noise Cancelling, Awareness (transparency), and Off
- Noise cancelling profiles: Dynamic, Cozy, General, and Ultra

Audio

- Four equalizer presets: Default, Bass Boost, Treble Boost, and Voices
- Low latency mode

Controls and system integration

- Double tap, triple tap, hold, and swipe volume gesture mapping
- Dual connection and device pairing management
- Optional foreground service with a persistent status notification
- Edge-to-edge Compose UI with Material 3 Expressive theming and dynamic color
- Adaptive and monochrome launcher icons

## Requirements

- JDK 17 or newer (CI uses JDK 21)
- Android SDK Platform 37 and Build-Tools 36.0.0 or newer
- Android Studio with support for AGP 9.0
- An Android device with API 31 or newer and Bluetooth Classic (RFCOMM)

Build configuration:

| Setting | Value |
| --- | --- |
| `minSdk` | 31 |
| `targetSdk` | 36 |
| `compileSdk` | 37 |
| Kotlin | 2.4.21 |
| Gradle | 9.8.2 |
| AGP | 9.4.1 |

## Getting started

Clone the repository:

```bash
git clone https://github.com/ket0x4/openbuds.git
cd openbuds
```

Open the `src` directory as the project root in Android Studio, or build from the command line.

From the repository root:

```bash
./src/gradlew -p src :app:assembleDebug
```

From the Gradle root:

```bash
cd src
./gradlew :app:assembleDebug
```

The debug APK is written to `src/app/build/outputs/apk/debug/app-debug.apk`.

## Testing and lint

```bash
# Unit tests for all modules
./src/gradlew -p src test

# Android lint
./src/gradlew -p src :app:lintDebug

# Everything CI runs
./src/gradlew -p src test :app:assembleDebug :app:lintDebug
```

Instrumented tests live in `src/app/src/androidTest` and require a connected device or emulator:

```bash
./src/gradlew -p src :app:connectedDebugAndroidTest
```

## Continuous integration

The workflow in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on every push to `master` and on pull requests. It sets up JDK 21 and the Android SDK, then runs unit tests, builds the debug APK, and runs Android lint. Test reports and the debug APK are uploaded as workflow artifacts.

## Documentation

- [Material 3 Expressive notes](src/docs/MATERIAL_3_EXPRESSIVE.md) - theme tokens, typography, shapes, and component styling used by the app.

## Contributing

1. Fork the repository and create a topic branch.
2. Keep protocol changes inside `:core` or a driver module, not in the app layer.
3. Run tests and lint before opening a pull request.
4. Describe the device model and firmware version in the pull request when changing protocol behavior.

## License

No license has been added to this repository yet. Until one is provided, the code is not licensed for reuse.
