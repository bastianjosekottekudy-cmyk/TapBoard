#!/usr/bin/env python3
"""Build TapBoard Android APK + Windows companion, upload to GitHub Releases (replace).

Usage:
  python scripts/publish_release.py
  python scripts/publish_release.py --tag v1.0.0
  python scripts/publish_release.py --skip-build   # upload existing artifacts only

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
COMPANION = ROOT / "companion"
DIST = ROOT / "dist"
DEFAULT_TAG = "v1.0.0"
APK_OUT = DIST / "TapBoard.apk"
EXE_OUT = COMPANION / "tapboard-companion.exe"


def run(cmd: list[str], cwd: Path | None = None, env: dict | None = None) -> None:
    print("+", " ".join(cmd), flush=True)
    merged = os.environ.copy()
    if env:
        merged.update(env)
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


def build_exe() -> None:
    env = {"CGO_ENABLED": "0"}
    run(
        ["go", "build", "-ldflags=-H windowsgui", "-o", "tapboard-companion.exe", "."],
        cwd=COMPANION,
        env=env,
    )
    if not EXE_OUT.exists():
        raise SystemExit(f"ERROR=missing exe {EXE_OUT}")
    print(f"STATUS=exe {EXE_OUT} ({EXE_OUT.stat().st_size} bytes)", flush=True)


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
        "## TapBoard release\n\n"
        "- `TapBoard.apk` — Android app (sideload / testing)\n"
        "- `tapboard-companion.exe` — Windows Wi‑Fi companion\n\n"
        "Assets are replaced on every publish."
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
    run(
        [
            "gh",
            "release",
            "upload",
            tag,
            str(APK_OUT),
            str(EXE_OUT),
            "--clobber",
        ]
    )
    print(f"STATUS=uploaded {tag}", flush=True)
    print(
        f"URL=https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/tag/{tag}",
        flush=True,
    )
    print(
        "APK=https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/TapBoard.apk",
        flush=True,
    )
    print(
        "EXE=https://github.com/bastianjosekottekudy-cmyk/TapBoard/releases/latest/download/tapboard-companion.exe",
        flush=True,
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tag", default=DEFAULT_TAG)
    parser.add_argument("--skip-build", action="store_true")
    args = parser.parse_args()
    try:
        if not args.skip_build:
            build_apk()
            build_exe()
        else:
            if not APK_OUT.exists() or not EXE_OUT.exists():
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
