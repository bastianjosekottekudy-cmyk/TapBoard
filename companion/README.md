# TapBoard Companion

Desktop receiver for TapBoard **Wi‑Fi mode** — Windows GUI app (WebView2).

## Run

```bash
cd companion
go build -ldflags="-H windowsgui" -o tapboard-companion.exe .
.\tapboard-companion.exe
```

Or double-click `tapboard-companion.exe`.

## What it does

- Shows host name, LAN IP, and a large **PIN**
- Starts listening automatically
- **Enable network access** adds Windows Firewall rules for UDP 19528 / TCP 19529 on private networks (one UAC prompt — no manual firewall UI)
- Copy PIN button

## Optional fixed PIN

```bat
set TAPBOARD_PIN=123456
tapboard-companion.exe
```

## Requirement

[Microsoft Edge WebView2 Runtime](https://developer.microsoft.com/microsoft-edge/webview2/) (already present on most Windows 10/11 PCs).
