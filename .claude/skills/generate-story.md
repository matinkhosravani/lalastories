---
name: generate-story
description: Generate a complete Persian children's story for LalaStories — creates story JSON, cartoon images via OpenRouter (Gemini 3.1 Flash Image / nanobananaapi.ai), and Persian voice via ElevenLabs, then places everything so it auto-appears on the home screen.
---

# Generate Story Skill

You are generating a full story asset set for the LalaStories Android app at `/home/matin/projects/Kid-stories`.

## Trigger

User says: `/generate-story [story name]`  
If no name is given, ask: "What story should I generate?"

## Step 1 — Write the Persian story JSON

Generate a JSON object following this exact schema (same as existing stories):

```json
{
  "id": "story_id_in_english_with_underscores",
  "title": "عنوان داستان به فارسی",
  "description": "یک توضیح کوتاه جذاب برای کارت داستان",
  "ageMin": 4,
  "ageMax": 10,
  "pages": [
    {"pageNumber": 1, "text": "متن صفحه اول که توصیف یک صحنه جذاب است.", "image": "images/1.jpg"},
    {"pageNumber": 2, "text": "متن صفحه دوم...", "image": "images/2.jpg"}
  ]
}
```

Rules for the story:
- **Language**: Persian (Farsi), simple and beautiful, appropriate for kids
- **Pages**: 8–13 pages (like the existing stories — Cinderella has 6, Midas has 13)
- **Page text**: 2–5 sentences per page; vivid scenes since each page gets an AI-generated illustration
- **Story arc**: Clear beginning, conflict, resolution with a lesson
- **id**: lowercase English, underscores for spaces (e.g. `little_red_riding_hood`)
- **ageMin/ageMax**: Pick appropriate range (e.g. 4–10, 5–12)

After writing the JSON, show it to the user and ask: "Does this look good? Should I generate images and voice now?"

## Step 2 — Save the story JSON

```bash
cat > /tmp/{story_id}_draft.json << 'EOF'
{paste the JSON here}
EOF
```

## Step 3 — Run the asset generator

```bash
cd /home/matin/projects/Kid-stories
pip install requests -q 2>/dev/null
python3 scripts/generate_story.py {story_id} /tmp/{story_id}_draft.json --voice-id {NOUSHIN_VOICE_ID}
```

**NOUSHIN_VOICE_ID**: The user's ElevenLabs voice ID for the Noushin voice. Ask if not known.

The script will:
- Generate a cartoon image for each page using **Gemini 3.1 Flash Image** via OpenRouter (`google/gemini-3.1-flash-image`)
- Generate a cover image
- Generate voice narration via ElevenLabs (Noushin voice)
- Build voice_text_map.txt for subtitle sync
- Place everything under `app/src/main/assets/stories/{story_id}/`

To use OpenRouter for images, pass `--image-provider openrouter` (and `OPENROUTER_KEY` env var).  
To fall back to nanobananaapi.ai, pass `--image-provider nanobanana`.

**Total time**: ~5–10 minutes for a 10-page story with Gemini Flash.

Tell the user: "Image generation is running — this takes about 30–60 seconds per image. I'll update you as each one finishes."

## Step 4 — Rebuild the app (optional)

If the user has a connected device or wants to test:
```bash
cd /home/matin/projects/Kid-stories && ./gradlew assembleDebug
```

Stories are auto-discovered by `StoryRepository` — no code changes needed.

## Step 5 — Confirm success

After the script completes, verify the folder structure:
```bash
ls app/src/main/assets/stories/{story_id}/
ls app/src/main/assets/stories/{story_id}/images/
```

Expected:
```
story.json  cover.jpg  voice.mp3  voice_text_map.txt
images/1.jpg  images/2.jpg  ...
```

Report: story title, page count, whether voice was generated, and next steps.

## Key files

- Script: `scripts/generate_story.py`
- Assets: `app/src/main/assets/stories/{story_id}/`
- Story loading: `app/src/main/java/ir/sospans/lalastories/repository/StoryRepository.kt`
- Home screen: `app/src/main/java/ir/sospans/lalastories/ui/home/HomeScreen.kt`

## If voice ID is missing

Skip voice with `--skip-voice` and tell the user:
> "Story images generated! To add voice narration, go to elevenlabs.io → Voices → Noushin → ⋮ → Copy voice ID, then tell me the ID and I'll generate the audio."

## Error handling

- Image fails: The script skips already-generated images on re-run — just re-run the same command
- Voice fails: Re-run with `--skip-images` to only regenerate voice
- API key errors: Check `NANOBANANA_KEY` and `ELEVENLABS_KEY` in the script
