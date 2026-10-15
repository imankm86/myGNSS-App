# GNSS Signal & Carrier-to-Noise (C/N₀) Viewer

A clean, modern Android application written in **Kotlin** and **Jetpack Compose** (Material 3) designed to inspect all visible GNSS signals, their carrier frequencies, and their **Carrier-to-Noise Ratios (C/N₀ in dB-Hz)** with real-time filtering by **constellation** and **frequency band**.

---

## 🌟 Key Features

- **Carrier-to-Noise Ratio (C/N₀) Focus**:
  - Displays the exact carrier-to-noise density ratio in `dB-Hz` for every tracked satellite signal.
  - Color-coded real-time signal strength meter (Red: <20 dB-Hz, Amber: 20–30 dB-Hz, Green: 30–40 dB-Hz, Sky Blue: >40 dB-Hz).
  - Average C/N₀ metric for filtered signals.

- **Constellation Support & Filtering**:
  - Supports all major GNSS constellations:
    - 🇺🇸 **GPS** (NAVSTAR)
    - 🇪🇺 **Galileo**
    - 🇷🇺 **GLONASS**
    - 🇨🇳 **BeiDou**
    - 🇯🇵 **QZSS**
    - 🇮🇳 **NavIC** (IRNSS)
    - 🛰️ **SBAS** (WAAS, EGNOS, MSAS, GAGAN)
  - Interactive Filter Chips with live satellite counts per constellation (e.g., `GPS (8)`, `Galileo (6)`).

- **Multi-Frequency Band Identification & Filtering**:
  - Extracts the exact carrier frequency via Android's `GnssStatus.getCarrierFrequencyHz()`.
  - Automatically maps frequencies into standard GNSS bands:
    - **L1 / E1 / B1C** (~1575.42 MHz)
    - **L5 / E5a / B2a** (~1176.45 MHz)
    - **L2** (~1227.60 MHz)
    - **E5b / B2b** (~1207.14 MHz)
    - **E6 / B3I** (~1268–1278 MHz)
    - **GLONASS G1** (~1602 MHz FDMA)
    - **GLONASS G2** (~1246 MHz FDMA)
    - **BeiDou B1I** (~1561.098 MHz)
  - Dynamic horizontal frequency chip bar allowing single-tap filtering.

- **Sorting Options**:
  - Sort by **Highest C/N₀** (default), **Lowest C/N₀**, **Constellation**, **Frequency**, or **Satellite PRN**.

- **Built-In Simulation / Demo Mode**:
  - Toggle between **Live GPS Hardware** and **Demo Mode** with a single switch in the top app bar.
  - Ideal for testing indoors, in offices, or on Android Emulators where live satellite signals are unavailable.

---

## 🏗️ Project Architecture

```
com.example.gnssviewer/
├── MainActivity.kt                 # Edge-to-edge Activity with runtime permission handler
├── data/
│   ├── GnssSignal.kt               # Immutable data model for satellite signals
│   └── GnssUtils.kt                # Frequency band categorization & constellation mapping
├── service/
│   ├── GnssRepository.kt           # Wraps Android LocationManager GnssStatus.Callback into Kotlin Flow
│   └── MockGnssProvider.kt         # Generates realistic multi-frequency GNSS data for demo mode
└── ui/
    ├── GnssViewModel.kt            # Reactive state management with Kotlin StateFlow & Combine
    ├── GnssScreen.kt               # Main screen with top bar, summary metrics, and list
    ├── components/
    │   ├── FilterBar.kt            # Scrollable constellation & frequency filter chips + sort dropdown
    │   ├── SignalCard.kt           # Card presenting PRN, constellation badge, frequency, and C/N₀
    │   └── SignalStrengthBar.kt    # Smoothly animated progress bar for signal quality
    └── theme/                      # Material 3 colors, typography, and theme setup
```

---

## 📋 Requirements

- **Android Studio**: Android Studio Hedgehog / Iguana / Jellyfish / Ladybug (or newer)
- **Minimum SDK**: Android 8.0 (API level 26) — required for `hasCarrierFrequencyHz()` and `getCarrierFrequencyHz()`
- **Target SDK**: Android 15 (API level 35)
- **JDK**: Java 17 or Java 21

---

## 🚀 How to Build and Run

### In Android Studio
1. Open **Android Studio**.
2. Select **File -> Open...** and select the `/Users/iman.mandal/python/myAndroidGNSSApp` directory.
3. Allow Gradle to sync the project.
4. Connect an Android device (with Location enabled) or start an Android Emulator.
5. Click **Run 'app'** (`Shift + F10`).

### From Command Line
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```

---

## 🛰️ Testing on Android Emulator vs Real Device

1. **On a Physical Android Device**:
   - Grant the requested **Precise Location** permission.
   - For best live results, test with a clear view of the sky (outdoors or near a window).
   - If indoors, you can immediately flip the **Demo** switch in the top bar to inspect signals.

2. **On the Android Emulator**:
   - The emulator does not automatically receive live satellite signals unless simulated.
   - Simply toggle the **Demo** switch on the top right to view simulated live GNSS signals across GPS L1/L5, Galileo E1/E5a, GLONASS G1/G2, and BeiDou B1I/B2a!
   - Alternatively, open Emulator Extended Controls (`...` -> **Location**) and load a GPX/KML route.
