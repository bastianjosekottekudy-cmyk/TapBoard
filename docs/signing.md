# Signing release builds

Upload keystore is already generated for this machine:

- Keystore: `android/tapboard-upload.jks`
- Passwords: `android/KEYSTORE_CREDENTIALS.txt` (**back this up offline — if lost, you cannot update the Play app**)

`android/app/build.gradle.kts` loads `android/keystore.properties` automatically.

## Build Play upload (AAB)

```bat
cd android
gradlew.bat :app:bundleRelease
```

Output:

`android/app/build/outputs/bundle/release/app-release.aab`

## New version

Bump `versionCode` (required) and `versionName` in `app/build.gradle.kts`, then rebuild the bundle.

Never commit `*.jks`, `keystore.properties`, or `KEYSTORE_CREDENTIALS.txt`.
