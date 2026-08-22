---
name: android-brand-assets
description: Regenerate this app's Android launcher, notification, and launch-screen assets from the IMVERSED brand source art. Use when the brand mark, icon gradient, or logo changes, when launcher/notification icons need to be re-cut at all densities, or when the adaptive-icon safe zone or splash screen needs adjusting. Covers the raster pipeline (generate.py) and the hand-authored vector layers.
---

# Android brand assets (IMVERSED)

Two halves, and they are not interchangeable:

| Part | How it is produced |
|------|--------------------|
| Adaptive icon vector layers, brand mark, splash drawables, themes | **Hand-authored XML.** `generate.py` does NOT touch these. |
| Legacy raster launcher icons + notification silhouettes | `generate.py`, deterministic and idempotent. |

## Brand

| Token | Hex |
|-------|-----|
| Spring green | `#05F2C7` |
| Blue | `#0930FF` |
| Violet | `#7506FD` |
| App dark background | `@color/backgroundDark` = `#192025` |

The icon gradient is a linear ramp green -> blue -> violet. Fitted against
`imversed-03.png`, the axis is ~82 degrees (essentially top to bottom, leaning
right), with the blue stop at offset 0.62.

## Source art

Base dir:
`/Users/x0040h/Library/Mobile Documents/com~apple~CloudDocs/Projects/Imversed/Imversed styles assets/`

| File | Size | Use |
|------|------|-----|
| `imversed-03.png` | 982x952 | Rounded-square gradient icon. Source for `ic_launcher.webp` and for sampling the gradient. |
| `imversed-05.png` | 982x952 | Circular gradient icon. Source for `ic_launcher_round.webp`. |
| `imversed-11.png` | 982x952 | Bare black glyph on a white plate. Source for the notification silhouette. |
| `imversed-04.png` | 982x952 | Black plate, white glyph. Reference only. |
| `imversed-01/02/10.png` | 3390x952 | Horizontal lockup (glyph + wordmark). Not used by the app icons. |
| `imversed.ai` | - | Illustrator source. Unusable here, no rasterizer. |

Vector glyph source:
`/Users/x0040h/Library/Mobile Documents/com~apple~CloudDocs/Projects/KindRoo/logos 2/imversed_logo.svg`
(112x37 viewBox). The first `<path>` is the badge: a rounded-square container
plus two even-odd subpaths that knock out the glyph. Only those two subpaths are
kept for the Android foreground - the dot and the "V". Their raw bounds in SVG
units are `x 3.0156..11.0566, y 14.2615..23.9806` (8.041 x 9.7191).

## Tooling constraints on this machine

- `python3` + Pillow, `cwebp`, `sips`. Verified present.
- **No SVG rasterizer**: no `rsvg-convert`, `inkscape`, `magick`, `cairosvg`.
  SVG path data must be hand-translated into an Android `<vector>`, never rasterized.
- No ImageMagick. All compositing/resizing goes through Pillow (`Image.LANCZOS`).

## Safe zones - the rule that decides every size here

| Surface | Canvas | Visible | Rule |
|---------|--------|---------|------|
| Adaptive icon | 108dp | centre 72dp, and OEM masks can be a 72dp **circle** | Art must fit the central 72dp; assume the circle. |
| API 31+ splash icon | 288dp | centre 192dp circle | Same discipline, two-thirds of the canvas. |
| Notification icon | 24dp | ~20dp live area | ~80% fill, white on transparent. |

Applied here: the glyph is scaled to **48dp tall / ~40dp wide** and centred on
(54,54) of the 108dp viewport. Bounding radius ~31dp against the 36dp mask
circle. Do not simply downscale the 982px composites - their art runs to the
edge and would be clipped.

Second consequence of the mask: the gradient axis spans only the **central 72dp
window** (`startY="18"`, `endY="90"`), not the full 108dp canvas. If the axis
covered 0..108 the mask would crop away the green and violet ends and the icon
would read as flat blue.

## Hand-authored files (edit these by hand, not with the script)

```
presentation/src/main/res/
  drawable/ic_launcher_background.xml   108dp vector, 3-stop brand gradient
  drawable/ic_launcher_foreground.xml   108dp vector, white glyph, transparent
  drawable/ic_brand_mark.xml            gradient disc + glyph, for launch screens
  drawable/window_background_launch.xml layer-list: backgroundDark + 144dp mark
  mipmap-anydpi-v26/ic_launcher.xml     background / foreground / monochrome
  mipmap-anydpi-v26/ic_launcher_round.xml
  values/themes.xml                     AppLaunchTheme windowBackground
  values-v31/themes.xml                 windowSplashScreenBackground + AnimatedIcon
```

The glyph transform, if the mark ever changes and you need to redo it:

```
s  = targetGlyphHeightDp / glyphBBoxHeight          e.g. 48 / 9.7191 = 4.93872
tx = 54 - glyphCentreX * s                          e.g. 54 - 7.0361*4.93872 = 19.25
ty = 54 - glyphCentreY * s                          e.g. 54 - 19.1211*4.93872 = -40.437
```

and put it on a `<group android:scaleX scaleY translateX translateY>`. Android
applies scale first, then translate, so those numbers go in directly.

Vector `<gradient>` needs API 24. AGP auto-generates PNG fallbacks for API 23,
which is why `drawable-*/ic_launcher_background.png` shows up in the APK. That
is expected, not a mistake.

## Generated files (run the script)

```
python3 .claude/skills/android-brand-assets/generate.py \
  "/Users/x0040h/Library/Mobile Documents/com~apple~CloudDocs/Projects/Imversed/Imversed styles assets" \
  presentation/src/main/res
```

Density tables:

| Density | Launcher (px) | Notification (px) |
|---------|---------------|-------------------|
| mdpi    | 48  | 24 |
| hdpi    | 72  | 36 |
| xhdpi   | 96  | 48 |
| xxhdpi  | 144 | 72 |
| xxxhdpi | 192 | 96 |

Outputs:
- `mipmap-{density}/ic_launcher.webp` - full-bleed square, cwebp `-q 95 -alpha_q 100 -m 6`
- `mipmap-{density}/ic_launcher_round.webp` - full-bleed circle
- `drawable-{density}/ic_notification.png` - white glyph on transparent, 80% fill

Notification icons must be **white on transparent**. Android tints them; any
colour or opaque background renders as a solid blob in the status bar.
`ic_notification_failed.png` is a separate failed-state mark and is deliberately
left alone by the script.

## Verify

```
./gradlew :presentation:assembleNoAnalyticsDebug
unzip -l presentation/build/outputs/apk/noAnalytics/debug/*.apk | grep -E 'ic_launcher|ic_notification'
```

Bad vector path data or a malformed gradient fails at resource-merge time, so
the build is a real gate.

## Out of scope

Do not change `app_name` or any string resource. The user-visible name is
"IMVERSED Messenger"; string changes are a separate task.
