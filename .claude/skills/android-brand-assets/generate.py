#!/usr/bin/env python3
"""
Regenerate IMVERSED raster Android assets (legacy launcher webp + notification
silhouettes) from the brand source art.

Usage:
    python3 generate.py <source-art-dir> <res-dir>

Idempotent: re-running with the same inputs produces byte-identical output.

NOTE: The adaptive-icon vector layers (drawable/ic_launcher_background.xml,
drawable/ic_launcher_foreground.xml) are HAND-AUTHORED XML and are NOT produced
by this script. See SKILL.md.
"""
import os
import subprocess
import sys

from PIL import Image

# --- brand ---------------------------------------------------------------
SPRING_GREEN = "#05F2C7"
BLUE = "#0930FF"
VIOLET = "#7506FD"

# --- source art ----------------------------------------------------------
SRC_SQUARE = "imversed-03.png"  # rounded-square gradient icon, 982x952
SRC_ROUND = "imversed-05.png"   # circular gradient icon, 982x952
SRC_GLYPH = "imversed-11.png"   # bare black glyph on white plate, 982x952

# --- density tables ------------------------------------------------------
LAUNCHER_PX = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
NOTIFICATION_PX = {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}

# Fraction of the notification canvas the glyph may occupy. Material system
# icons are 24dp with ~2dp padding on each side -> a 20dp live area.
NOTIFICATION_FILL = 0.80

CWEBP_ARGS = ["-q", "95", "-alpha_q", "100", "-m", "6", "-quiet"]


def die(msg):
    sys.stderr.write("ERROR: %s\n" % msg)
    sys.exit(1)


def load(src_dir, name):
    p = os.path.join(src_dir, name)
    if not os.path.isfile(p):
        die("missing source asset: %s" % p)
    im = Image.open(p).convert("RGBA")
    if im.size[0] < 512:
        die("%s is only %dx%d; need >= 512px on the short side" % ((p,) + im.size))
    return im


def content_crop(im):
    """Crop to the opaque bounding box, then pad back to a square canvas."""
    bbox = im.getchannel("A").point(lambda v: 255 if v > 10 else 0).getbbox()
    if bbox is None:
        die("source image is fully transparent")
    im = im.crop(bbox)
    side = max(im.size)
    out = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    out.alpha_composite(im, ((side - im.size[0]) // 2, (side - im.size[1]) // 2))
    return out


def glyph_alpha(im):
    """White-on-transparent silhouette from a black-glyph-on-white-plate source.

    Alpha comes from how dark each opaque pixel is, so antialiased glyph edges
    survive. RGB is forced to pure white: Android tints notification icons and
    renders any non-transparent pixel as the tint colour.
    """
    r, g, b, a = im.split()
    lum = Image.merge("RGB", (r, g, b)).convert("L")
    alpha = lum.point(lambda v: 255 - v)  # black glyph -> opaque
    # kill anything outside the source plate
    alpha = Image.composite(alpha, Image.new("L", im.size, 0), a.point(lambda v: 255 if v > 128 else 0))
    white = Image.new("RGBA", im.size, (255, 255, 255, 0))
    white.putalpha(alpha)
    bbox = alpha.point(lambda v: 255 if v > 8 else 0).getbbox()
    if bbox is None:
        die("no glyph found in %s" % SRC_GLYPH)
    return white.crop(bbox)


def fit_centered(art, canvas_px, fill):
    """Scale art to occupy `fill` of a square canvas, optically centered."""
    target = canvas_px * fill
    scale = min(target / art.size[0], target / art.size[1])
    w = max(1, int(round(art.size[0] * scale)))
    h = max(1, int(round(art.size[1] * scale)))
    art = art.resize((w, h), Image.LANCZOS)
    out = Image.new("RGBA", (canvas_px, canvas_px), (0, 0, 0, 0))
    out.alpha_composite(art, ((canvas_px - w) // 2, (canvas_px - h) // 2))
    return out


def write_png(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, "PNG", optimize=True)
    print("wrote %s" % path)


def write_webp(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tmp = path + ".tmp.png"
    im.save(tmp, "PNG")
    try:
        subprocess.run(["cwebp"] + CWEBP_ARGS + [tmp, "-o", path], check=True)
    except FileNotFoundError:
        die("cwebp not found on PATH (brew install webp)")
    except subprocess.CalledProcessError as e:
        die("cwebp failed for %s (exit %d)" % (path, e.returncode))
    finally:
        if os.path.exists(tmp):
            os.remove(tmp)
    print("wrote %s" % path)


def legacy_launchers(src_dir, res_dir):
    """T2: pre-API-26 raster launcher icons, full bleed."""
    square = content_crop(load(src_dir, SRC_SQUARE))
    circle = content_crop(load(src_dir, SRC_ROUND))
    for density, px in sorted(LAUNCHER_PX.items()):
        d = os.path.join(res_dir, "mipmap-" + density)
        write_webp(square.resize((px, px), Image.LANCZOS), os.path.join(d, "ic_launcher.webp"))
        write_webp(circle.resize((px, px), Image.LANCZOS), os.path.join(d, "ic_launcher_round.webp"))


def notification_icons(src_dir, res_dir):
    """T3: white-on-transparent status bar silhouettes."""
    art = glyph_alpha(load(src_dir, SRC_GLYPH))
    for density, px in sorted(NOTIFICATION_PX.items()):
        d = os.path.join(res_dir, "drawable-" + density)
        write_png(fit_centered(art, px, NOTIFICATION_FILL), os.path.join(d, "ic_notification.png"))


def main():
    if len(sys.argv) != 3:
        sys.stderr.write(__doc__)
        sys.exit(2)
    src_dir, res_dir = sys.argv[1], sys.argv[2]
    if not os.path.isdir(src_dir):
        die("source art dir not found: %s" % src_dir)
    if not os.path.isdir(res_dir):
        die("res dir not found: %s" % res_dir)
    legacy_launchers(src_dir, res_dir)
    notification_icons(src_dir, res_dir)


if __name__ == "__main__":
    main()
