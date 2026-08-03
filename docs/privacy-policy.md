# Privacy Policy — TapBoard

**Last updated:** 2026-08-03

TapBoard (“we”, “the app”) is a local remote-input utility. This policy describes what the Android app and optional desktop companion process on your devices.

## Summary

- No accounts.
- No cloud sync or analytics by default.
- Bluetooth and WiFi traffic stay on your devices / local network.

## Data we process

### On your phone

- **Bluetooth device names and addresses** of hosts you choose to connect to (stored only as needed for pairing/connection UI; not uploaded).
- **Local network addresses** of TapBoard Companion instances discovered on your LAN.
- **App preferences** (sensitivity, theme, PIN you enter, onboarding flag) in private app storage.

### On your local network (WiFi mode)

- Encrypted-or-plain **LAN TCP/UDP** messages that carry mouse/keyboard events and a PIN you configure. Traffic does not leave your LAN unless your network routes it.

### Not collected

- Personal identity, contacts, photos, precise location history, advertising IDs, or crash telemetry (unless you later enable a third-party crash reporter in a future version — none in v1).

## Permissions (Android)

| Permission | Why |
|------------|-----|
| Bluetooth / Nearby devices | Act as a Bluetooth HID keyboard and mouse |
| Location (older Android) | Required by the OS for Bluetooth scanning on API ≤30 |
| Wi‑Fi / Nearby Wi‑Fi | Discover and connect to TapBoard Companion |
| Foreground service / notifications | Keep the HID/Wi‑Fi session alive while you use the remote |
| Vibrate | Optional haptic feedback on clicks |

## Children’s privacy

TapBoard is not directed at children under 13.

## Changes

Material changes will bump the “Last updated” date in this document and in-app Help.

## Contact

Open an issue in the project repository or contact the publisher email listed on the Google Play store listing.
