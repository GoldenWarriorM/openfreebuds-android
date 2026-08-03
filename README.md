# FreeBuds SE

**Companion app for the HUAWEI FreeBuds SE earbuds** — check battery levels, configure double-tap gestures, and keep an eye on charge with a home-screen widget and a permanent notification.

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)
![Desktop](https://img.shields.io/badge/Platform-Linux%20Desktop-FCC624?logo=linux)
![Compose](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?logo=jetpackcompose)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin)
![License](https://img.shields.io/badge/License-GPLv3-green)

---

## Features

- **Battery levels** — exact charge for the left earbud, right earbud and charging case, with charging state
- **Double-tap gestures** — assign a double-tap action (off, voice assistant, play/pause, next, previous) to each earbud
- **Home-screen widget** — shows left/right/case battery with light & dark themes
- **Permanent notification** — optional ongoing notification with the battery level of both earbuds and the case (pinned to the top of the shade)
- **Automatic reconnect** — reconnects to the last known device after an unexpected drop
- **Material 3 Expressive** — dynamic Material You theming on Android
- Doesn't require internet connection (AI Life require plugin to work)
- Only takes 32 MB of space (AI Life takes 660 MB of space)
- No proprietary BS like EULA or Privacy Policy


## Screenshots

<p align="center">
  <table><tr>
    <td>
      <img src="https://github.com/GoldenWarriorM/openfreebuds-android/raw/main/screenshots/home.png" alt="Home screen" width="280" />
    </td>
    <td>
      <img src="https://github.com/GoldenWarriorM/openfreebuds-android/raw/main/screenshots/widget.jpg" alt="Home-screen widget" width="420" /><br/>
      <img src="https://github.com/GoldenWarriorM/openfreebuds-android/raw/main/screenshots/notification.jpg" alt="Battery notification" width="420" />
    </td>
  </tr></table>
</p>

## Platforms

| Platform | Status |
|----------|--------|
| Android (API 26+) | ✅ Primary target, tested on physical device |
| Linux desktop | ✅ Works via BlueZ (RFCOMM/SPP) |
| Windows | ⚠️ Not tested — JVM target can build, RFCOMM driver missing |

## Building

### Prerequisites

- **JDK 21**
- Android SDK (for Android builds)
- Gradle (wrapped — `./gradlew`)

### Build & run

```bash
# Desktop (Linux)
./gradlew :composeApp:run

# Android debug APK
./gradlew :composeApp:assembleDebug
```

APK is at `composeApp/build/outputs/apk/debug/`.

### Build Android release

```bash
./gradlew :composeApp:assembleRelease
```

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/org/openfreebuds/se/
│   ├── connection/          # SeController — protocol parsing, state, watchdog
│   ├── model/               # BatteryLevels, TapAction, DoubleTapConfig, DeviceInfo
│   ├── protocol/            # MBB package parsing, CRC16-XModem, SeCommands
│   └── ui/                  # Compose UI: HomeScreen, BatteryCard, DoubleTapPanel
├── androidMain/             # Android transport, widget provider, notification
└── desktopMain/             # BlueZ transport (DBus RFCOMM)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Compose Multiplatform 1.11.1, Material 3 Expressive |
| Language | Kotlin 2.3.10 |
| Build | Gradle 8.13, KMP |
| Android | AGP 8.11.1, Glance (widget), Activity Compose |
| Linux | dbus-java + JNA over BlueZ RFCOMM |
| Protocol | MBB / Huawei, CRC16-XModem |

## Credits

This app is a from-scratch Kotlin implementation of the **FreeBuds SE** support and MBB protocol, researched and documented by the [OpenFreebuds](https://github.com/melianmiko/OpenFreebuds) project.

- **[melianmiko/OpenFreebuds](https://github.com/melianmiko/OpenFreebuds)** (GPL-3.0) — open-source HUAWEI FreeBuds companion for Linux/Windows; source of the SPP protocol commands, `5A 00 len 00 svc cmd TLV crc16-xmodem` packet layout, battery/double-tap command IDs and FreeBuds SE driver knowledge.
- **[TheLastGimbus/FreeBuddy](https://github.com/TheLastGimbus/FreeBuddy)** (Apache-2.0) — open-source FreeBuds app for Android; source of the `Crc16Xmodem` reference implementation and the earbud/case icon set ported into `BudIcons`.

This project is distributed under the **GPL-3.0** license. See [LICENSE](LICENSE).

## License

FreeBuds SE is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

## Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.
