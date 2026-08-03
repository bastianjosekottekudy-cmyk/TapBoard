# Publish TapBoard to Google Play

## What was prepared locally

| Artifact | Path |
|----------|------|
| Signed App Bundle (upload this) | `android/app/build/outputs/bundle/release/app-release.aab` |
| Upload keystore | `android/tapboard-upload.jks` (**never lose this**) |
| Keystore passwords | `android/KEYSTORE_CREDENTIALS.txt` (gitignored — back up offline) |
| Store icon 512×512 | `docs/play-assets/icon-512.png` |
| Feature graphic 1024×500 | `docs/play-assets/feature-graphic-1024x500.png` |
| Privacy policy page | Hosted URL in this doc after GitHub Pages is live |

## Requirements you must have

1. **Google Play Developer account** — one-time registration fee at [play.google.com/console](https://play.google.com/console)
2. Backup of `tapboard-upload.jks` + `KEYSTORE_CREDENTIALS.txt` in a password manager / safe place

## Play Console steps

1. Create app → name **TapBoard** → app type **App** → free → declarations as needed.
2. **Store listing**
   - Short/full description from `docs/play-listing.md`
   - Upload `icon-512.png` and `feature-graphic-1024x500.png`
   - Add at least 2 phone screenshots from a real device (Connect + Touchpad recommended)
   - Privacy policy URL (GitHub Pages link below)
3. **App content**
   - Privacy policy
   - Data safety: no data collected/shared (see play-listing.md)
   - Content rating questionnaire → Everyone
4. **Release → Testing → Internal testing** (recommended first)
   - Create release → upload `app-release.aab`
   - Add yourself as tester → install from Play link
5. After internal testing looks good → **Closed** or **Production** rollout.

## Rebuild a new version later

Bump in `android/app/build.gradle.kts`:

```kotlin
versionCode = 2        // must increase every upload
versionName = "1.0.1"
```

Then:

```bat
cd android
gradlew.bat :app:bundleRelease
```

## Privacy policy URL

See `docs/PLAY_PUBLISH_STATUS.md` (generated) or the GitHub Pages URL printed after publish setup.
