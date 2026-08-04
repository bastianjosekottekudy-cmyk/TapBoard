# TapBoard

Bluetooth keyboard and mouse for Android — turn your phone into a remote HID input device for PCs, Chromebooks, TVs, and other hosts that accept Bluetooth keyboards.

## Features

- **Bluetooth HID** — pairs like a real keyboard/mouse (no PC software)
- Touchpad with gestures, on-screen keyboard
- Material 3 UI

## Download

| File | Link |
|------|------|
| **Android APK** | https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/TapBoard.apk |
| **Releases** | https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest |

APK builds publish automatically via GitHub Actions when `android/` changes on `master`. Local fallback:

```bash
python scripts/publish_release.py
```

## Repository layout

| Path | Description |
|------|-------------|
| `android/` | Kotlin + Jetpack Compose app (`com.tapboard.app`) |
| `docs/` | Privacy policy, Play listing |
| `scripts/` | Release publish helper |

## Requirements

- Android Studio or JDK 17 + Android SDK 35
- Phone: Android 9+ (API 28) with Bluetooth HID peripheral support (varies by OEM)

## Build

```bash
cd android
./gradlew :app:assembleRelease
```

## License

MIT
