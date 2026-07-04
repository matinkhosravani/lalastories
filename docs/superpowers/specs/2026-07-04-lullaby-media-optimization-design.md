# Lullaby Media Optimization

## Problem

Lullaby image and audio assets under `app/src/main/assets/lullabies/*/` are far
larger than the app's established budget:

| Lullaby        | Image (before)      | Audio (before) |
|----------------|----------------------|-----------------|
| bagh-setare    | 1.9M png             | 4.7M            |
| baran-amad     | 952K jpeg            | 2.4M            |
| ghayeghe-khab  | 936K jpeg            | 3.4M            |
| khab-mah       | *(none)*             | 3.3M            |
| mah-naz        | 2.1M png             | 8.7M            |
| mah-taban      | 2.1M png             | 6.5M            |

Target: every lullaby image under 80KB (matching the existing poem assets,
which already sit at 72–80KB jpg), every lullaby audio file under ~3MB.
`LullabiesScreen`/`LullabyCard` and `LullabyPlayerScreen` already render
`lullaby.imagePath` when present, so no UI code changes are required — this
is an asset-pipeline task.

## Decisions

- **khab-mah stays without an image.** No new artwork is sourced; the
  existing moon-icon placeholder in `LullabyCard`/`LullabyPlayerScreen`
  continues to render for it.
- **Images are converted to JPEG** (bagh-setare, mah-naz, mah-taban are
  currently PNG) and downscaled/quality-tuned with `ffmpeg` until each is
  under 80KB, consistent with the poem assets' format and size range.
  `lullaby.json`'s `image` field is updated to point at the new `.jpg`
  filename where the extension changes.
- **Audio is re-encoded per-file, not at a single fixed bitrate.** Each
  track's `libmp3lame` bitrate is chosen (capped at 128kbps, floored around
  64kbps for the longest track) so the output lands safely under 3MB,
  scaled to that track's duration. This avoids over-compressing short
  lullabies or under-compressing long ones.
- **Files are overwritten in place**; no backup of the original
  high-res/high-bitrate assets is kept (user confirmed).

## Out of scope

- No changes to `Lullaby.kt`, `LullabyRepository.kt`, `LullabyCard.kt`,
  `LullabiesScreen.kt`, or `LullabyPlayerScreen.kt` — image display in both
  the lullabies grid and the player screen is already implemented.
- No new artwork for khab-mah.
