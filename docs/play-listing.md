# Play Store listing — TapBoard

## Short description (80 chars)

Remote Bluetooth & Wi‑Fi keyboard and mouse for your PC and more.

## Full description

TapBoard turns your Android phone into a precise remote keyboard and mouse.

**Bluetooth HID**
Pair with Windows, macOS, Linux, Chromebooks, and many smart TVs that accept Bluetooth keyboards — no PC software required.

**Wi‑Fi mode**
Install the free TapBoard Companion on your computer for a rock-solid LAN connection when Bluetooth HID isn’t supported by your phone’s chipset.

Download TapBoard Companion (Windows):
https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest

Direct download:
https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/tapboard-companion.exe

**Built for real use**
• Multi-touch touchpad with scroll and right-click
• Full on-screen keyboard with modifiers and function keys
• Media and presenter controls (arrows, Esc, volume, play/pause)
• Adjustable sensitivity, haptics, and keep-screen-on

**Privacy-first**
No account. No ads. No cloud telemetry. Input stays on your device or local network.

Requires Android 9+. Wi‑Fi mode needs TapBoard Companion on the same network (link above).

## Category

Tools / Productivity

## Content rating

Everyone

## Data safety (form answers)

- Collects personal info: **No**
- Shares data with third parties: **No**
- Encrypted in transit: LAN traffic is local; not end-to-end encrypted in v1 (PIN-gated)
- Users can request deletion: N/A (no cloud account)

## Assets checklist

- [ ] High-res icon 512×512
- [ ] Feature graphic 1024×500
- [ ] Phone screenshots (Connect, Touchpad, Keyboard, Media) ≥2
- [ ] Optional tablet screenshots
- [ ] Privacy policy URL hosting `docs/privacy-policy.md`

## Release checklist

- [ ] `targetSdkVersion` 35
- [ ] Foreground service type `connectedDevice` declared
- [ ] Runtime permission rationales tested on API 28, 31, 34+
- [ ] R8 release build smoke-tested (BT + Wi‑Fi)
- [x] Companion Windows build linked from Help / website / Play listing
  (https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest)
- [ ] Signed with upload keystore (not debug)
