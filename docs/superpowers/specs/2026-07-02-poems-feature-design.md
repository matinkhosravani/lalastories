# Poems Feature — Design Spec

**Date:** 2026-07-02
**Platform:** Android (Kotlin + Jetpack Compose)
**Language:** Farsi (Persian), RTL

---

## Overview

Add a second content type — **poems** (اشعار) — alongside the existing stories on the
home screen. Poems are short, single-block texts (not paginated like stories) shown
one at a time in a dedicated, colorful viewer with shuffled Next/Previous navigation.
The existing stories flow (Home → Detail → Read/Listen) is unchanged.

---

## Data & Content

### Poem model

```kotlin
data class Poem(
    val id: String,
    val title: String,
    val text: String,
    val image: String? = null,
    val imagePath: String? = null
)
```

Deliberately smaller than `Story` — no `ageMin`/`ageMax`, no `pages` list, no
`audioPath`. Poems are one screen of text, not a paginated book.

### Poem file structure

```
poems/
  <poem-id>/
    poem.json
    image.jpg   (optional)
```

### poem.json schema

```json
{
  "id": "spring-rain",
  "title": "باران بهاری",
  "text": "باران باران زیبا\nمی‌بارد از بالا\n..."
}
```

`text` uses `\n` for verse/line breaks, rendered as a single scrollable block.

### Repository

`PoemRepository(poemsDir: File)` mirrors `StoryRepository`: lists subdirectories of
`poemsDir`, parses `poem.json` with Gson, resolves `imagePath` if an image file
exists in the same folder.

### Asset bundling

`MainActivity.copyBundledStoriesIfNeeded` is generalized (or duplicated) to also
copy `assets/poems/` → `<external files>/poems/` on first run, using the same
`copyAssetDir` helper. The existing `stories_copied_v15` version-guard flag is
bumped to `v16` so existing installs pick up the new `poems/` folder on next launch.

### Seed content

Six original placeholder Persian children's poems (text only, no custom artwork —
the viewer falls back to a colorful icon when `imagePath` is null, same pattern
`StoryCard` already uses for stories without a cover).

---

## Home Screen Change

The existing stories grid (`HomeScreen`, `StoryCard`) is untouched.

Below it, a single new full-width **"اشعار" (Poems) entry card**:
- Vibrant gradient background (not a plain card like `StoryCard`)
- Book/poem icon + "اشعار" label
- Tapping it is the *only* entry point into poems — there is no grid of individual
  poem cards on the home screen.

`HomeScreen` gains an `onPoemsClick: () -> Unit` parameter, wired the same way as
the existing `onSettingsClick`.

---

## Poems Viewer (new `PoemsScreen`)

Route: `Screen.Poems` (no arguments).

On entry, the full poem list is shuffled once (`remember { poems.shuffled() }`) and
the screen opens on index 0 — i.e., a random poem.

### Layout
- Full-screen soft gradient background that changes per poem, cycling through a
  small palette (orange/purple/teal, matching the app's existing accent colors —
  `ReadButtonColor`, `ListenButtonColor`, `tertiary`).
- A white rounded card on top holds the image (or fallback icon) and poem text.
  The card content is wrapped in `verticalScroll` so long poems scroll instead of
  overflowing.
- Top bar: back arrow, using the same forced-`LayoutDirection.Ltr` fix already
  applied to the other screens' `TopAppBar`s, with the title pinned to
  `TextAlign.Right`.
- Bottom: Next / Previous buttons, positioned like `ReadingScreen`'s page
  controls (next on the right, previous on the left). Both **wrap around** the
  shuffled list (next from the last poem goes to the first, previous from the
  first goes to the last) — continuous browsing, no dead ends.

### Out of scope for this screen
- No reading-progress persistence (poems aren't tracked like story pages).
- No swipe gesture (buttons only, unlike `ReadingScreen`'s drag-to-turn-page).
- No per-poem detail screen.

---

## Navigation & Wiring

- `AppNavigation.kt`: add `object Poems : Screen("poems")` and a `composable(Screen.Poems.route)` entry rendering `PoemsScreen(poemRepository = ..., onBack = { navController.popBackStack() })`.
- `MainActivity.kt`: build a `PoemRepository(poemsDir)` alongside `StoryRepository`, pass both into `AppNavigation`.
- `AppNavigation` passes `onPoemsClick = { navController.navigate(Screen.Poems.route) }` into `HomeScreen`.

---

## Out of Scope

- Individual poem cards / a poems catalog list
- Audio narration for poems
- Editing/authoring poems in-app
- Search or filtering across poems
