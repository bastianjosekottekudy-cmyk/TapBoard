# Publish TapBoard APK to GitHub Releases

## Automatic (preferred)

On every push to `master` that changes `android/**`, GitHub Actions builds a signed release APK and replaces `TapBoard.apk` on release `v1.0.0`.

Workflow: `.github/workflows/release-apk.yml`  
Manual run: Actions → **Release APK** → Run workflow

### Required repository secrets

| Secret | Value |
|--------|--------|
| `KEYSTORE_BASE64` | Base64 of `android/tapboard-upload.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

```bash
# From a machine that has the keystore (do not commit these files)
base64 -w0 android/tapboard-upload.jks | gh secret set KEYSTORE_BASE64
# then set the three password/alias secrets from keystore.properties
```

## Local fallback

```bash
python scripts/publish_release.py
```

Bluetooth-only app — no Windows companion.
