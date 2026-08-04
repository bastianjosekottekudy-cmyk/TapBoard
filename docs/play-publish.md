# Publish TapBoard APK to GitHub Releases

No GitHub Actions. Publish locally after commits with:

```bash
python scripts/publish_release.py
```

Builds the signed release APK and replaces `TapBoard.apk` on release `v1.0.0` (latest).

Bluetooth-only app — no Windows companion.
