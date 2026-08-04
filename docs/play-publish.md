# Publish TapBoard APK to GitHub Releases

Local Python only — **no GitHub Actions workflow**.

```bash
python scripts/publish_release.py
```

- Builds the signed release APK
- Replaces `TapBoard.apk` on release tag `v1.0.1` (same tag reused until you bump)
- Does **not** bump `versionCode` / `versionName` unless you ask

## Auto on commit

After `python scripts/install_git_hooks.py`, commits that touch `android/` run the publish script via `.githooks/post-commit`.

Skip once: `TAPBOARD_SKIP_PUBLISH=1 git commit …` or `git commit --no-verify`.
