#!/usr/bin/env python3
"""Validate versioned sprite atlas manifests and referenced PNG dimensions."""
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "app" / "src" / "main" / "assets"
MANIFEST_ROOT = ASSET_ROOT / "sprites"


def fail(path, message):
    raise ValueError(f"{path.relative_to(ROOT)}: {message}")


def png_info(path):
    with path.open("rb") as stream:
        header = stream.read(29)
    if len(header) < 29 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError("texture is not a valid PNG with IHDR")
    return struct.unpack(">IIBB", header[16:26])


def validate(path):
    data = json.loads(path.read_text(encoding="utf-8"))
    if data.get("version") != 1:
        fail(path, "only manifest version 1 is supported")
    texture = data.get("texture")
    if not isinstance(texture, str) or not texture or texture.startswith("/") or "\\" in texture:
        fail(path, "texture must be a non-empty relative asset path")
    if any(part in ("", ".", "..") for part in texture.split("/")):
        fail(path, "texture path contains an unsafe segment")
    width, height = data.get("textureWidth"), data.get("textureHeight")
    if not isinstance(width, int) or not isinstance(height, int) or width <= 0 or height <= 0:
        fail(path, "texture dimensions must be positive integers")
    if data.get("filter", "nearest") not in ("nearest", "linear"):
        fail(path, "filter must be nearest or linear")
    sprites = data.get("sprites")
    if not isinstance(sprites, list) or not sprites:
        fail(path, "sprites must be a non-empty array")
    ids = set()
    for sprite in sprites:
        if not isinstance(sprite, dict):
            fail(path, "each sprite entry must be an object")
        sprite_id = sprite.get("id")
        if not isinstance(sprite_id, str) or not sprite_id or sprite_id in ids:
            fail(path, f"invalid or duplicate sprite id: {sprite_id}")
        ids.add(sprite_id)
        fw, fh = sprite.get("frameWidth"), sprite.get("frameHeight")
        if not isinstance(fw, int) or not isinstance(fh, int) or fw <= 0 or fh <= 0:
            fail(path, f"{sprite_id}: frame dimensions must be positive integers")
        px, py = sprite.get("pivotX", 0.5), sprite.get("pivotY", 1.0)
        if not isinstance(px, (int, float)) or not isinstance(py, (int, float)) or not (0 <= px <= 1 and 0 <= py <= 1):
            fail(path, f"{sprite_id}: pivots must be in range 0..1")
        frames = sprite.get("frames")
        if not isinstance(frames, list) or not frames:
            fail(path, f"{sprite_id}: frames must be a non-empty array")
        for index, frame in enumerate(frames):
            if not isinstance(frame, dict):
                fail(path, f"{sprite_id} frame {index}: expected object")
            x, y, fw_actual, fh_actual = (frame.get(k) for k in ("x", "y", "width", "height"))
            if not all(isinstance(v, int) for v in (x, y, fw_actual, fh_actual)):
                fail(path, f"{sprite_id} frame {index}: coordinates and dimensions must be integers")
            if x < 0 or y < 0 or fw_actual != fw or fh_actual != fh or x + fw_actual > width or y + fh_actual > height:
                fail(path, f"{sprite_id} frame {index}: dimensions or bounds are invalid")
    if path.name.endswith(".example.json"):
        return
    texture_path = (ASSET_ROOT / texture).resolve()
    if not texture_path.is_relative_to(ASSET_ROOT.resolve()):
        fail(path, "texture path escapes Android assets")
    if not texture_path.is_file():
        fail(path, f"referenced texture does not exist: {texture}")
    png_width, png_height, bit_depth, color_type = png_info(texture_path)
    if (png_width, png_height) != (width, height):
        fail(path, f"PNG is {png_width}x{png_height}, manifest says {width}x{height}")
    if bit_depth != 8 or color_type != 6:
        fail(path, "PNG must use 8-bit RGBA color type 6")


def main():
    manifests = sorted(MANIFEST_ROOT.glob("atlas-manifest*.json")) if MANIFEST_ROOT.exists() else []
    try:
        for manifest in manifests:
            validate(manifest)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"Sprite asset validation failed: {error}", file=sys.stderr)
        return 1
    print(f"Sprite asset validation passed ({len(manifests)} manifest(s)).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
