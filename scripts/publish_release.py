#!/usr/bin/env python3
"""Build TapBoard APK and replace the asset on the fixed GitHub Release tag.

No version bump. Reuses the same release (default v1.0.0) and overwrites
TapBoard.apk with --clobber. Bump android versionCode/versionName only when
the user explicitly asks.

Usage:
  python scripts/publish_release.py
  python scripts/publish_release.py --tag v1.0.0
  python scripts/publish_release.py --skip-build

Exit codes:
  0 ok
  1 error
"""
from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
DIST = ROOT / "dist"
DEFAULT_TAG = "v1.0.0"
APK_OUT = DIST / "TapBoard.apk"


def run(cmd: list[str], cwd: Path | None = None, env: dict | None = None) -> None:
    print("+", " ".join(cmd), flush=True)
    merged = os.environ.copy()
    if env:
        merged.update(env)
    # Ensure Android SDK is visible for Gradle
    if "ANDROID_HOME" not in merged:
        local_app = os.environ.get("LOCALAPPDATA") or ""
        sdk = Path(local_app) / "Android" / "Sdk"
        if sdk.is_dir():
            merged["ANDROID_HOME"] = str(sdk)
    subprocess.run(cmd, cwd=str(cwd or ROOT), check=True, env=merged)


def build_apk() -> None:
    DIST.mkdir(parents=True, exist_ok=True)
    gradlew = ANDROID / ("gradlew.bat" if os.name == "nt" else "gradlew")
    run([str(gradlew), ":app:assembleRelease", "--quiet"], cwd=ANDROID)
    src = ANDROID / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
    if not src.exists():
        raise SystemExit(f"ERROR=missing apk {src}")
    shutil.copy2(src, APK_OUT)
    print(f"STATUS=apk {APK_OUT} ({APK_OUT.stat().st_size} bytes)", flush=True)


def ensure_release(tag: str) -> None:
    list_proc = subprocess.run(
        ["gh", "release", "view", tag],
        cwd=str(ROOT),
        capture_output=True,
        text=True,
    )
    if list_proc.returncode == 0:
        print(f"STATUS=release exists {tag}", flush=True)
        return
    notes = (
        "## TapBoard (Bluetooth only)\n\n"
        "- `TapBoard.apk` — Android Bluetooth keyboard & mouse\n\n"
        "Same release tag is reused; the APK asset is replaced on every publish. "
        "App versionCode is not bumped automatically."
    )
    run(
        [
            "gh",
            "release",
            "create",
            tag,
            "--title",
            f"TapBoard {tag}",
            "--notes",
            notes,
            "--latest",
        ]
    )


def upload(tag: str) -> None:
    subprocess.run(
        ["gh", "release", "delete-asset", tag, "tapboard-companion.exe", "--yes"],
        cwd=str(ROOT),
        capture_output=True,
    )
    run(["gh", "release", "upload", tag, str(APK_OUT), "--clobber"])
    subprocess.run(
        [
            "gh",
            "release",
            "edit",
            tag,
            "--notes",
            "## TapBoard (Bluetooth only)\n\n"
            "- `TapBoard.apk` — Android Bluetooth keyboard & mouse\n\n"
            "Latest build replaces this asset. Pair as a Bluetooth keyboard/mouse — no companion app.",
        ],
        cwd=str(ROOT),
        check=False,
    )
    print(f"STATUS=uploaded {tag}", flush=True)
    print(
        "APK=https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/TapBoard.apk",
        flush=True,
    )


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Build APK and replace GitHub Release asset (no version bump)."
    )
    parser.add_argument("--tag", default=DEFAULT_TAG, help=f"Release tag (default {DEFAULT_TAG})")
    parser.add_argument("--skip-build", action="store_true")
    args = parser.parse_args()
    try:
        if not args.skip_build:
            build_apk()
        elif not APK_OUT.exists():
            print("ERROR=artifacts missing; run without --skip-build", flush=True)
            return 1
        ensure_release(args.tag)
        upload(args.tag)
        print("STATUS=ok", flush=True)
        return 0
    except subprocess.CalledProcessError as e:
        print(f"ERROR=command failed exit={e.returncode}", flush=True)
        return 1
    except Exception as e:
        print(f"ERROR={e}", flush=True)
        return 1


if __name__ == "__main__":
    sys.exit(main())
