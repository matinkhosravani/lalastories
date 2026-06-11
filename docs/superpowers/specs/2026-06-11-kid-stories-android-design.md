go # Kid Stories Android App — Design Spec

**Date:** 2026-06-11  
**Platform:** Android (Kotlin + Jetpack Compose)  
**Language:** Farsi (Persian), RTL  
**Target Users:** Persian-speaking kids ages 3–10, single-user, no login  

---

## Overview

A colorful, playful Android app that tells stories to Persian kids in both text (read) and voice (listen) modes. Stories are loaded from the filesystem dynamically — adding a new story requires no app update. The app ships with the Cinderella (`سیندرلا`) story pre-bundled.

---

## Architecture

- **UI:** Jetpack Compose, RTL layout throughout
- **Narration (TTS):** Android `TextToSpeech` API with Persian locale
- **Narration (pre-recorded):** Android `MediaPlayer` for MP3 playback
- **Story storage:** JSON files in external app storage (`/Android/data/<package>/files/stories/`)
- **Progress persistence:** `SharedPreferences` keyed by story ID
- **Navigation:** Simple back-stack, no bottom nav bar

---

## Story File Structure

```
stories/
  cinderella/
    story.json
    cover.png        (optional)
    audio.mp3        (optional)
  <story-id>/
    story.json
    ...
```

### story.json Schema

```json
{
  "id": "cinderella",
  "title": "سیندرلا",
  "description": "داستان دختری مهربان...",
  "ageMin": 4,
  "ageMax": 10,
  "pages": [
    { "pageNumber": 1, "text": "روزی روزگاری..." },
    { "pageNumber": 2, "text": "..." }
  ]
}
```

---

## Progress Persistence (SharedPreferences)

| Key | Type | Description |
|-----|------|-------------|
| `progress_<id>_page` | Int | Last page number read |
| `progress_<id>_mode` | String | `"read"` or `"listen"` |
| `progress_<id>_position_ms` | Long | Audio playback position in milliseconds |

---

## Screens

### 1. Home / Story Library
- RTL grid of story cards
- Each card: cover image, Farsi title, age range badge
- Big, colorful, cartoon-style cards

### 2. Story Detail
- Cover art, title, description
- Two primary buttons: **بخوان** (Read) and **گوش بده** (Listen)
- If pre-recorded audio exists, Listen mode shows source choice: TTS or MP3

### 3. Reading Screen
- Large Farsi text, RTL, page-by-page navigation
- Progress bar at top
- **شروع از اول** (Start Over) button
- Auto-saves progress on page change and exit

### 4. Listening Screen
- Full-screen illustrated view
- Play/Pause/Seek bar
- Current narration text displayed below
- Auto-saves audio position on pause and exit

### 5. Settings
- TTS voice speed slider
- Font size adjustment

---

## Bundled Content

- Cinderella story (`سیندرلا`) bundled in `assets/stories/cinderella/`
- Copied to external storage on first app launch
- Includes full Farsi text (paginated), placeholder cover image
- No bundled audio — user can import MP3 separately

---

## Adding New Stories

Drop a new folder into the stories directory with a valid `story.json`. The app discovers all valid story folders at startup. No app update required.

---

## Age Targeting

| Age Group | Experience |
|-----------|-----------|
| 3–6 | Voice-first, large visuals, minimal reading |
| 6–10 | Text + voice, smaller font acceptable |

Both groups use the same screens; font size is adjustable in Settings.

---

## Out of Scope

- User accounts or multiple child profiles
- Network/internet connectivity
- Story purchase or DRM
- iOS support
