#!/usr/bin/env python3
"""Convert every image in a folder to .jpg, capped at a max file size.

Lowers JPEG quality first; if still too big at quality=10, downsizes the
image and retries. Deletes the original file after a successful conversion
(unless --keep-originals is passed).

Usage:
    python3 convert_images_to_jpg.py /home/matin/stories/{slug} [--max-kb 100]
"""
import argparse
import os

from PIL import Image

SKIP_EXT = {".jpg", ".jpeg"}


def convert_one(path, max_bytes):
    base = os.path.splitext(path)[0]
    out_path = base + ".jpg"
    im = Image.open(path).convert("RGB")

    quality = 90
    while quality >= 10:
        im.save(out_path, "JPEG", quality=quality, optimize=True)
        if os.path.getsize(out_path) <= max_bytes:
            return out_path, quality
        quality -= 5

    scale = 0.9
    w, h = im.size
    while os.path.getsize(out_path) > max_bytes and scale > 0.3:
        resized = im.resize((max(1, int(w * scale)), max(1, int(h * scale))), Image.LANCZOS)
        resized.save(out_path, "JPEG", quality=70, optimize=True)
        scale -= 0.1
    return out_path, "resized"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("folder")
    ap.add_argument("--max-kb", type=int, default=100)
    ap.add_argument("--keep-originals", action="store_true")
    args = ap.parse_args()

    max_bytes = args.max_kb * 1024
    for fname in sorted(os.listdir(args.folder)):
        ext = os.path.splitext(fname)[1].lower()
        if ext in SKIP_EXT or ext not in (".webp", ".png", ".jpeg", ".jpg", ".bmp", ".tiff"):
            continue
        src = os.path.join(args.folder, fname)
        out_path, quality = convert_one(src, max_bytes)
        size_kb = os.path.getsize(out_path) / 1024
        print(f"{fname} -> {os.path.basename(out_path)} : {size_kb:.1f} KB (quality={quality})")
        if not args.keep_originals and os.path.abspath(out_path) != os.path.abspath(src):
            os.remove(src)


if __name__ == "__main__":
    main()
