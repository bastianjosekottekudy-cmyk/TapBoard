# Play publish status

## GitHub
- Repo: https://github.com/bastianjosekottekudy-cmyk/TapBoard
- Android APK: https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/TapBoard.apk
- Latest tag: v1.0.1

## Play Store (first publish)
- Guide: `docs/PLAY_CONSOLE_FIRST_PUBLISH.md`
- **AAB:** `dist/TapBoard.aab` (versionName 1.0.3, versionCode 4, targetSdk **36**)
- Package: `com.tapboard.app`
- Console: https://play.google.com/console

## Privacy policy
- https://gist.github.com/bastianjosekottekudy-cmyk/68a67bcfedfb30ac839dfa80686f1e00

## Play Store description
Copy from `docs/PLAY_STORE_DESCRIPTION.txt` (Bluetooth only).

## API upload (optional)
Save service account JSON as `android/play-service-account.json`, then:
`python scripts/publish_play.py --track internal`
