# Companion install & firewall

## Windows (recommended)

1. Run `companion/tapboard-companion.exe`.
2. Click **Enable network access** once and approve the UAC prompt.
3. Firewall rules are installed automatically for private networks (UDP 19528, TCP 19529).
4. In TapBoard → Wi‑Fi → Scan → enter the PIN.

You should **not** need to open Windows Defender Firewall settings yourself.

## macOS / Linux

```bash
go build -o tapboard-companion .
./tapboard-companion
```

Grant Accessibility / input-monitoring permissions when prompted. Firewall automation is Windows-only in v1.
