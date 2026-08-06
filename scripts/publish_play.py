#!/usr/bin/env python3
"""Upload TapBoard AAB to Google Play (internal track by default).

Requires a Play Console service account JSON linked to the app with
Release to testing / Release apps permission.

Setup (once):
  1. Play Console → Users and permissions → Invite new users → create
     a Google Cloud service account with Play Developer API access
     (or Developers → API access → link project → create service account).
  2. Grant the service account permission on the TapBoard app.
  3. Download the JSON key to:
       android/play-service-account.json
     (gitignored — never commit).

Usage:
  python scripts/publish_play.py
  python scripts/publish_play.py --track internal
  python scripts/publish_play.py --track production --status completed
  python scripts/publish_play.py --aab dist/TapBoard.aab

Exit:
  0 ok
  1 error / missing credentials
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android"
DIST = ROOT / "dist"
DEFAULT_AAB = DIST / "TapBoard.aab"
SA_JSON = ANDROID / "play-service-account.json"
PACKAGE = "com.tapboard.app"


def run(cmd: list[str], cwd: Path | None = None) -> None:
    print("+", " ".join(cmd), flush=True)
    env = os.environ.copy()
    if "ANDROID_HOME" not in env:
        sdk = Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk"
        if sdk.is_dir():
            env["ANDROID_HOME"] = str(sdk)
    subprocess.run(cmd, cwd=str(cwd or ROOT), check=True, env=env)


def ensure_deps() -> None:
    try:
        import google.auth  # noqa: F401
        from googleapiclient.discovery import build  # noqa: F401
    except ImportError:
        print("STATUS=installing google-api-python-client google-auth", flush=True)
        subprocess.run(
            [
                sys.executable,
                "-m",
                "pip",
                "install",
                "--quiet",
                "google-api-python-client",
                "google-auth",
            ],
            check=True,
        )


def build_aab() -> Path:
    DIST.mkdir(parents=True, exist_ok=True)
    gradlew = ANDROID / ("gradlew.bat" if os.name == "nt" else "gradlew")
    run([str(gradlew), ":app:bundleRelease", "--quiet"], cwd=ANDROID)
    src = ANDROID / "app" / "build" / "outputs" / "bundle" / "release" / "app-release.aab"
    if not src.is_file():
        raise SystemExit(f"ERROR=missing aab {src}")
    import shutil

    shutil.copy2(src, DEFAULT_AAB)
    print(f"STATUS=aab {DEFAULT_AAB} ({DEFAULT_AAB.stat().st_size} bytes)", flush=True)
    return DEFAULT_AAB


def upload(aab: Path, track: str, status: str) -> None:
    from google.oauth2 import service_account
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaFileUpload

    if not SA_JSON.is_file():
        print(f"ERROR=missing {SA_JSON}", flush=True)
        print(
            f"HINT=Save Play Console API service account JSON to that path, then re-run. "
            f"Or upload manually in Play Console: {aab}",
            flush=True,
        )
        raise SystemExit(1)

    scopes = ["https://www.googleapis.com/auth/androidpublisher"]
    creds = service_account.Credentials.from_service_account_file(str(SA_JSON), scopes=scopes)
    service = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)

    edit = service.edits().insert(body={}, packageName=PACKAGE).execute()
    edit_id = edit["id"]
    print(f"STATUS=edit {edit_id}", flush=True)

    media = MediaFileUpload(str(aab), mimetype="application/octet-stream", resumable=True)
    bundle = (
        service.edits()
        .bundles()
        .upload(editId=edit_id, packageName=PACKAGE, media_body=media)
        .execute()
    )
    version_code = bundle["versionCode"]
    print(f"STATUS=uploaded versionCode={version_code}", flush=True)

    service.edits().tracks().update(
        editId=edit_id,
        packageName=PACKAGE,
        track=track,
        body={
            "track": track,
            "releases": [
                {
                    "name": f"TapBoard {version_code}",
                    "versionCodes": [str(version_code)],
                    "status": status,
                }
            ],
        },
    ).execute()

    service.edits().commit(editId=edit_id, packageName=PACKAGE).execute()
    print(f"STATUS=committed track={track} status={status}", flush=True)
    print("STATUS=ok", flush=True)


def main() -> int:
    parser = argparse.ArgumentParser(description="Upload TapBoard AAB to Google Play")
    parser.add_argument("--aab", type=Path, default=None)
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument(
        "--track",
        default="internal",
        choices=["internal", "alpha", "beta", "production"],
    )
    parser.add_argument(
        "--status",
        default="completed",
        choices=["completed", "draft", "halted", "inProgress"],
        help="Release status (use draft if store listing incomplete)",
    )
    args = parser.parse_args()
    try:
        ensure_deps()
        aab = args.aab
        if aab is None:
            aab = DEFAULT_AAB if args.skip_build and DEFAULT_AAB.is_file() else build_aab()
        if not Path(aab).is_file():
            print(f"ERROR=missing aab {aab}", flush=True)
            return 1
        upload(Path(aab), args.track, args.status)
        return 0
    except SystemExit as e:
        return int(e.code) if isinstance(e.code, int) else 1
    except subprocess.CalledProcessError as e:
        print(f"ERROR=command failed exit={e.returncode}", flush=True)
        return 1
    except Exception as e:
        print(f"ERROR={e}", flush=True)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
