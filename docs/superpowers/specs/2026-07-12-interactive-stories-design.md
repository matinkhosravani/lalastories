# Interactive Stories Feature — Design Spec

**Date:** 2026-07-12
**Platform:** Android (Kotlin + Jetpack Compose)
**Language:** Farsi (Persian), RTL

---

## Overview

Add a new content type — **interactive stories** — alongside the existing
Stories/Poems/Lullabies. Unlike those, an interactive story is not a flat
sequence of pages but a small node graph: some nodes are plain content pages,
some are **choice points** (kid picks what happens next, branching the story
toward one of several endings), and some are **quiz questions** (kid taps the
correct answer, gets instant feedback, story continues either way).

This feature is fully isolated from the existing `Story`/`Poem`/`Lullaby`
models, repositories, and screens — no changes to `ReadingScreen`,
`ListeningScreen`, or their progress tracking. Interactive stories are
**read-only** (no narration/Listen mode) and live in their own new home-screen
section.

Two example stories are authored as part of this feature (hand-written JSON,
illustrations generated via the existing image-gen step of
`scripts/generate_story.py`), proving out both mechanics:

1. **خرگوش و راه جنگل** (The Rabbit and the Forest Path) — branching, two
   choice points, three endings.
2. **روزی با پروانه‌ها** (A Day with the Butterflies) — one quiz question
   mid-story, two to three at the end.

---

## Data & Content

### InteractiveStory model

New file `model/InteractiveStory.kt`:

```kotlin
data class InteractiveStory(
    val id: String,
    val title: String,
    val description: String,
    val ageMin: Int,
    val ageMax: Int,
    val coverPath: String?,
    val startNodeId: String,
    val nodes: Map<String, StoryNode>
)

sealed class StoryNode {
    abstract val id: String

    data class ContentNode(
        override val id: String,
        val text: String,
        val imagePath: String?,
        val next: String
    ) : StoryNode()

    data class ChoiceNode(
        override val id: String,
        val prompt: String,
        val imagePath: String?,
        val options: List<ChoiceOption>
    ) : StoryNode()

    data class QuizNode(
        override val id: String,
        val question: String,
        val imagePath: String?,
        val answers: List<QuizAnswer>,
        val next: String
    ) : StoryNode()

    data class EndNode(
        override val id: String,
        val text: String,
        val imagePath: String?
    ) : StoryNode()
}

data class ChoiceOption(val label: String, val imagePath: String?, val next: String)
data class QuizAnswer(val text: String?, val imagePath: String?, val isCorrect: Boolean)
```

`QuizAnswer` allows `text`, `imagePath`, or both — some questions render as
tap-the-picture, others as text buttons, per-question.

A `QuizNode`'s `next` is fixed regardless of which answer is tapped — quizzes
check comprehension, they don't branch the story. Only `ChoiceNode` branches.

### File structure (bundled asset, mirrors `assets/stories/<id>/`)

```
assets/interactive-stories/
  <story-id>/
    story.json
    cover.jpg
    images/
      n1.jpg
      n2.jpg
      n3-option-a.jpg
      n3-option-b.jpg
      ...
```

### story.json schema

```json
{
  "id": "khargoosh-va-rahe-jangal",
  "title": "خرگوش و راه جنگل",
  "description": "...",
  "ageMin": 4,
  "ageMax": 8,
  "cover": "cover.jpg",
  "startNode": "n1",
  "nodes": {
    "n1": { "type": "content", "text": "...", "image": "images/n1.jpg", "next": "n2" },
    "n2": {
      "type": "choice",
      "prompt": "نوشین کدام راه را انتخاب کند؟",
      "image": "images/n2.jpg",
      "options": [
        { "label": "راه کنار رودخانه", "next": "n3" },
        { "label": "راه میان‌بر جنگل", "next": "n4" }
      ]
    },
    "n7": {
      "type": "quiz",
      "question": "نوشین در راه با چه کسی آشنا شد؟",
      "answers": [
        { "text": "جغد", "isCorrect": true },
        { "text": "روباه", "isCorrect": false }
      ],
      "next": "n8"
    },
    "n10": { "type": "end", "text": "نوشین به‌سلامت به خانه‌ی مادربزرگ رسید...", "image": "images/end-a.jpg" }
  }
}
```

### Repository

New `InteractiveStoryRepository.kt`, folder-scans
`assets/interactive-stories/` the same way `StoryRepository` scans
`assets/stories/` — no CDN/manifest involvement for this feature (bundled
only). At load time, each story's node graph is validated (see Error Handling
below); stories that fail validation are excluded from the list rather than
crashing.

---

## Home Screen Change

New full-width section/entry on the home screen, alongside Stories/Poems/
Lullabies — its own row, not merged into the Stories grid, using the existing
`SectionEntryCard`-style component. Label: "داستان‌های تعاملی" (Interactive
Stories).

---

## Screens & Navigation

New routes in `AppNavigation.kt`:

- `interactiveStories` — list screen (grid of story cards: cover, title,
  age range), mirrors the existing Stories list screen.
- `interactiveStoryDetail/{id}` — detail screen (cover, title, description,
  single "بازی" / "Play" button), mirrors `StoryDetailScreen` minus the
  Read/Listen split (only one mode here).
- `interactiveStory/{id}` — the player itself, new `InteractiveStoryScreen.kt`.

### InteractiveStoryScreen rendering per node type

- **ContentNode** — image + text, same layout language as `ReadingScreen`'s
  page (image above scrollable text), with a single "Next" affordance
  (tap/button) advancing to `next`. No swipe-drag paging (that gesture implies
  linear pages; a graph doesn't have a page count to show progress against).
- **ChoiceNode** — prompt/image at top, 2–4 tappable option cards below
  (image+label if the option has an image, label-only otherwise). Tapping
  navigates to that option's `next`.
- **QuizNode** — question/image at top, answer tiles below (image or text
  per-answer). Tapping an answer shows instant visual feedback (checkmark/tint
  for correct, brief shake/tint for incorrect — plain Compose
  `AnimatedVisibility`/color transitions, no new animation library needed).
  Wrong answers remain tappable; tapping the correct answer auto-advances to
  `next` after a short delay. No score is tallied.
- **EndNode** — ending text/image + "بازگشت به خانه" (Home) and "دوباره بخوان"
  (Read again, restarts from `startNodeId`) actions. No further "next."

### Progress / resume

New `InteractiveStoryProgress(storyId, currentNodeId)` — much simpler than the
existing page-number-based `StoryProgress`, since there's no total page count
in a graph. On reopening a story, jump straight back to `currentNodeId`.
Choice history is not stored — resuming mid-branch does not let the kid see or
redo earlier choices; that's out of scope for v1. Reaching an `EndNode` clears
the progress entry (finishing = starting fresh next time).

---

## Content Authoring (this feature's two example stories)

No new authoring tooling — hand-write `story.json` for both example stories,
using the schema above, in Persian. Illustrations are produced by reusing just
the AI image-generation call from `scripts/generate_story.py` (not its
linear-page assembly or ElevenLabs voice steps, since these stories are
read-only) per node/option/answer that needs an image, matching the existing
stories' art style.

1. **خرگوش و راه جنگل** — رabbit Nushin must reach grandma's burrow before
   sunset. Two `ChoiceNode`s (which fork to take), three distinct `EndNode`s
   (each meeting a different forest friend who helps her home) — all endings
   warm/safe, no "wrong" ending. Roughly 10–12 nodes total, comparable in
   length to existing stories' 6–13 pages.
2. **روزی با پروانه‌ها** — a caterpillar becomes a butterfly and visits
   flowers, introducing simple facts along the way. One `QuizNode` mid-story
   testing a fact just introduced, two to three `QuizNode`s clustered near the
   end recapping the story, mixing image-answer and text-answer questions.
   Roughly 10–14 nodes total.

---

## Error Handling / Edge Cases

- Malformed `story.json` (Gson parse failure, missing required field) → that
  story is skipped/logged at repository load time, not a crash.
- A `next`/`option.next`/quiz `next` referencing a node id that doesn't exist
  in the `nodes` map → caught by a one-time validation pass at load time; the
  whole story is excluded from the list (fail closed) rather than letting a
  kid hit a dead screen mid-story.
- Missing image path on any node/option/answer → render text-only for that
  element instead of a broken image.
- Accidental cycles (a chain of `next`s that loops without ever reaching an
  `EndNode`) are not technically blocked, but must be avoided when authoring
  the two example stories — a kid could otherwise get stuck looping forever.

---

## Testing

The app has no broad automated UI test suite today, so this feature adds one
small unit test for the graph loader/validator (catches dangling node
references and unreachable/cyclic graphs cheaply) rather than Compose UI
tests. Beyond that, verification is manual: play through every branch of the
branching story to all three endings, and both example stories' quiz
questions with right and wrong taps, on-device/emulator.

---

## Out of Scope

- Audio narration / Listen mode for interactive stories
- CDN/manifest-based interactive stories (bundled-only for now)
- Extending the `/generate-story` authoring pipeline/skill to support
  branching or quiz stories generically (revisit once this format is proven)
- Scoring, badges, or any results/summary screen
- Showing or replaying earlier choices when resuming a branching story
- Mixing both mechanics (choice + quiz) in a single story (the model supports
  it, but neither of the two example stories does it)
