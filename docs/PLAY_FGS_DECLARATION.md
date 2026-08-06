# Play Console — Foreground service (connectedDevice)

TapBoard’s use is valid: keep a **Bluetooth HID** session to an external PC/TV alive with a **visible ongoing notification** while connected.

## What to select

- Permission / type: **Connected device** (`FOREGROUND_SERVICE_CONNECTED_DEVICE`)
- Use case: **Continuous data transfer to an external device** / interactions with Bluetooth peripherals (keyboard/mouse HID)

## Description (paste)

```
TapBoard turns the phone into a Bluetooth HID keyboard and mouse for a paired PC, Chromebook, TV, or similar host.

When the user connects in the app, a foreground service of type connectedDevice runs so the Bluetooth HID link and input reports (pointer + keys) stay active while the user is controlling the host — including when another app is in the foreground or the screen is briefly off.

A persistent notification (“TapBoard connected”) is shown for the whole session. The service starts only after the user connects and stops when they disconnect or the Bluetooth link drops.
```

## Why it must run in the background / if deferred

```
If Android stops or defers this service, the HID connection drops and mouse/keyboard input to the host stops immediately. That breaks the core feature. The work cannot be done only while the Activity is visible, because users need the phone as a remote while looking at the PC screen.
```

## Video

Generated demo (upload to YouTube as **Unlisted**, paste link in Play Console):

`docs/play-assets/fgs-declaration-demo.mp4`

Rebuild:

```bash
python scripts/make_fgs_demo_video.py
```

Ideal live recording (if you have a phone) is even better: connect → notification → leave app / use pad → disconnect. Use this MP4 if you need a link today.

## Do not select

- Media playback, camera, microphone, location, dataSync, specialUse (unless forced) — wrong types for this app.
