#!/usr/bin/env python3
"""Build a Play Console FGS declaration demo video (GPU NVENC when available).

Usage:
  python scripts/make_fgs_demo_video.py

Output:
  docs/play-assets/fgs-declaration-demo.mp4
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "play-assets"
OUT = ASSETS / "fgs-declaration-demo.mp4"
W, H = 1080, 1920
FPS = 30


def font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for name in (
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\seguisb.ttf",
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\arial.ttf",
    ):
        p = Path(name)
        if p.is_file():
            return ImageFont.truetype(str(p), size)
    return ImageFont.load_default()


def wrap(draw: ImageDraw.ImageDraw, text: str, fnt, max_width: int) -> list[str]:
    words = text.split()
    lines: list[str] = []
    cur = ""
    for w in words:
        trial = f"{cur} {w}".strip()
        if draw.textlength(trial, font=fnt) <= max_width:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines


def slide_text(title: str, body: str, accent: tuple[int, int, int] = (45, 212, 191)) -> Image.Image:
    img = Image.new("RGB", (W, H), (7, 20, 28))
    draw = ImageDraw.Draw(img)
    # top accent bar
    draw.rectangle((0, 0, W, 18), fill=accent)
    tf = font(64)
    bf = font(40)
    y = 280
    for line in wrap(draw, title, tf, W - 120):
        draw.text(((W - draw.textlength(line, font=tf)) / 2, y), line, fill=(231, 242, 240), font=tf)
        y += 78
    y += 40
    for line in wrap(draw, body, bf, W - 140):
        draw.text(((W - draw.textlength(line, font=bf)) / 2, y), line, fill=(180, 200, 196), font=bf)
        y += 54
    return img


def slide_photo(path: Path, caption: str, step: str) -> Image.Image:
    img = Image.new("RGB", (W, H), (7, 20, 28))
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, W, 18), fill=(45, 212, 191))

    sf = font(36)
    cf = font(44)
    draw.text((48, 48), step, fill=(45, 212, 191), font=sf)

    src = Image.open(path).convert("RGB")
    # Fit phone screenshot in center with margins
    max_w, max_h = W - 80, H - 360
    src.thumbnail((max_w, max_h), Image.Resampling.LANCZOS)
    x = (W - src.width) // 2
    y = 120
    # subtle frame
    pad = 10
    draw.rounded_rectangle(
        (x - pad, y - pad, x + src.width + pad, y + src.height + pad),
        radius=36,
        outline=(20, 60, 70),
        width=4,
    )
    img.paste(src, (x, y))

    cy = y + src.height + 48
    for line in wrap(draw, caption, cf, W - 100):
        draw.text(((W - draw.textlength(line, font=cf)) / 2, cy), line, fill=(231, 242, 240), font=cf)
        cy += 56
    return img


def write_frames(frame_dir: Path) -> int:
    slides: list[tuple[Image.Image, float]] = [
        (
            slide_text(
                "TapBoard — connectedDevice FGS",
                "Play Console declaration demo. Bluetooth HID keyboard and mouse.",
            ),
            3.5,
        ),
        (
            slide_photo(
                ASSETS / "phone-connect.png",
                "User opens TapBoard and connects to a paired Bluetooth host.",
                "Step 1 — User connects",
            ),
            5.0,
        ),
        (
            slide_text(
                "Foreground service starts",
                "Type: connectedDevice. Persistent notification: “TapBoard connected — Remote keyboard and mouse is active.”",
            ),
            5.0,
        ),
        (
            slide_photo(
                ASSETS / "phone-touchpad.png",
                "User leaves the app UI / uses the touchpad. HID input to the PC continues while the notification stays visible.",
                "Step 2 — Noticeable while not in-app",
            ),
            6.0,
        ),
        (
            slide_photo(
                ASSETS / "phone-keyboard.png",
                "Keyboard and modifiers keep working over the same Bluetooth HID session.",
                "Step 3 — Continuous device I/O",
            ),
            5.5,
        ),
        (
            slide_text(
                "If the service is stopped",
                "Disconnect or link drop stops the service, clears the notification, and remote input ends immediately — core feature requires the FGS.",
            ),
            5.5,
        ),
        (
            slide_text(
                "Summary",
                "FOREGROUND_SERVICE_CONNECTED_DEVICE used only for an active Bluetooth HID session initiated by the user, with an ongoing notification.",
            ),
            5.0,
        ),
    ]

    n = 0
    for img, seconds in slides:
        frames = max(1, int(seconds * FPS))
        for _ in range(frames):
            img.save(frame_dir / f"f_{n:06d}.png")
            n += 1
    return n


def encode(frame_dir: Path, count: int) -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    pattern = str(frame_dir / "f_%06d.png")
    # Prefer NVENC; fall back to libx264
    encoders = [
        [
            "ffmpeg",
            "-y",
            "-framerate",
            str(FPS),
            "-i",
            pattern,
            "-c:v",
            "h264_nvenc",
            "-preset",
            "p4",
            "-rc",
            "vbr",
            "-cq",
            "23",
            "-b:v",
            "4M",
            "-pix_fmt",
            "yuv420p",
            "-movflags",
            "+faststart",
            str(OUT),
        ],
        [
            "ffmpeg",
            "-y",
            "-framerate",
            str(FPS),
            "-i",
            pattern,
            "-c:v",
            "libx264",
            "-preset",
            "fast",
            "-crf",
            "22",
            "-pix_fmt",
            "yuv420p",
            "-movflags",
            "+faststart",
            str(OUT),
        ],
    ]
    last_err = ""
    for cmd in encoders:
        print("+", " ".join(cmd), flush=True)
        r = subprocess.run(cmd, capture_output=True, text=True)
        if r.returncode == 0 and OUT.is_file():
            print(f"STATUS=video {OUT} ({OUT.stat().st_size} bytes) frames={count}", flush=True)
            print(f"STATUS=encoder {cmd[cmd.index('-c:v') + 1]}", flush=True)
            return
        last_err = (r.stderr or r.stdout or "")[-800:]
        print(f"STATUS=encoder failed, trying fallback…", flush=True)
    raise SystemExit(f"ERROR=ffmpeg failed\n{last_err}")


def main() -> int:
    needed = [
        ASSETS / "phone-connect.png",
        ASSETS / "phone-touchpad.png",
        ASSETS / "phone-keyboard.png",
    ]
    missing = [p for p in needed if not p.is_file()]
    if missing:
        print("ERROR=missing screenshots:", *missing, flush=True)
        return 1
    with tempfile.TemporaryDirectory(prefix="tapboard_fgs_") as tmp:
        frame_dir = Path(tmp)
        count = write_frames(frame_dir)
        encode(frame_dir, count)
    print("STATUS=ok", flush=True)
    print(
        "NEXT=Upload this MP4 to YouTube as Unlisted, then paste the link into the Play Console FGS declaration.",
        flush=True,
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
