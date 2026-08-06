# Google Play Console — first publish (TapBoard 1.0.1)

## Ready on disk

| Item | Path |
|------|------|
| **AAB (upload this)** | `dist/TapBoard.aab` |
| Also at | `android/app/build/outputs/bundle/release/app-release.aab` |
| App ID | `com.tapboard.app` |
| Version | `1.0.1` (versionCode **2**) |
| Icon 512 | `docs/play-assets/icon-512.png` |
| Feature graphic | `docs/play-assets/feature-graphic-1024x500.png` |
| Screenshots | `docs/play-assets/phone-*.png` (3) |
| Short / full description | `docs/PLAY_STORE_DESCRIPTION.txt` |
| Privacy policy URL | https://gist.github.com/bastianjosekottekudy-cmyk/68a67bcfedfb30ac839dfa80686f1e00 |

Rebuild AAB anytime:

```bat
cd android
gradlew.bat :app:bundleRelease
```

## Play Console steps (manual — first time)

1. Open https://play.google.com/console → **Create app**
   - Name: **TapBoard**
   - Default language: English (US)
   - App / Free / Declarations as applicable
2. **Store listing**
   - Paste short + full description from `docs/PLAY_STORE_DESCRIPTION.txt`
   - Upload icon, feature graphic, phone screenshots
   - App category: **Tools** (or Productivity)
   - Contact email: your publisher email
   - Privacy policy: gist URL above
3. **App content**
   - Privacy policy (same URL)
   - Data safety: no personal data collected / not shared (see `docs/play-listing.md`)
   - Content rating questionnaire → Everyone
   - Target audience / news / COVID / ads: none as appropriate
4. **Test and release → Internal testing**
   - Create release → upload **`dist/TapBoard.aab`**
   - Add yourself as a tester → install from the opt-in link
5. **Production** (when ready)
   - New personal developer accounts usually need **closed testing** (12 testers, 14 days) before production. Check Console dashboard for your account’s requirement.

## Optional: API upload later

Save Play API service account JSON as `android/play-service-account.json` (gitignored), then:

```bash
python scripts/publish_play.py --track internal
```
