# TapBoard

Turn your Android phone into a Bluetooth + WiFi keyboard and mouse for PCs, Chromebooks, and other HID-capable hosts.

## Features

- **Bluetooth HID** — pairs like a real keyboard/mouse (no host software)
- **WiFi mode** — low-latency LAN control via **TapBoard Companion**
- Touchpad with gestures, on-screen keyboard, media / presenter controls
- Material 3 UI, Play Store–oriented permissions and privacy posture

## Download Windows Companion

Latest release (Windows `.exe`):

**https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest**

Direct file:

**https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/tapboard-companion.exe**

## Repository layout

| Path | Description |
|------|-------------|
| `android/` | Kotlin + Jetpack Compose app (`com.tapboard.app`) |
| `companion/` | Go desktop receiver (Windows GUI + tray) |
| `protocol/` | WiFi protocol v1 specification |
| `docs/` | Privacy policy, Play listing, release notes |

## Requirements

- Android Studio Ladybug+ or JDK 17 with Android SDK 35
- Phone: Android 9+ (API 28)
- Companion: Go 1.22+ to build; Windows x64 binary for WiFi mode

## Build — Android

```bash
cd android
./gradlew :app:assembleDebug
```

## Build — Companion

```bash
cd companion
go build -ldflags="-H windowsgui" -o tapboard-companion.exe .
```

## Protocol

See [protocol/v1.md](protocol/v1.md). Discovery UDP `19528`, session TCP `19529`.

## License

MIT
