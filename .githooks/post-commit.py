#!/usr/bin/env python3
"""post-commit: rebuild APK and replace GitHub latest release asset.

Runs when HEAD touches android/ (or always if TAPBOARD_PUBLISH_ALWAYS=1).
Does not bump versionCode/versionName.
Skip with: git commit --no-verify   or   TAPBOARD_SKIP_PUBLISH=1
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PUBLISH = ROOT / "scripts" / "publish_release.py"


def head_touches_android() -> bool:
    r = subprocess.run(
        ["git", "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    files = [ln.strip().replace("\\", "/") for ln in (r.stdout or "").splitlines()]
    return any(f == "android" or f.startswith("android/") for f in files)


def main() -> int:
    if os.environ.get("TAPBOARD_SKIP_PUBLISH", "").strip() in {"1", "true", "yes"}:
        print("STATUS=publish skipped (TAPBOARD_SKIP_PUBLISH)", flush=True)
        return 0
    always = os.environ.get("TAPBOARD_PUBLISH_ALWAYS", "").strip() in {"1", "true", "yes"}
    if not always and not head_touches_android():
        print("STATUS=publish skipped (no android/ changes in HEAD)", flush=True)
        return 0
    if not PUBLISH.is_file():
        print(f"ERROR=missing {PUBLISH}", flush=True)
        return 1
    print("STATUS=publishing APK after commit…", flush=True)
    return subprocess.call([sys.executable, str(PUBLISH)], cwd=str(ROOT))


if __name__ == "__main__":
    raise SystemExit(main())
