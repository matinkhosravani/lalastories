#!/usr/bin/env python3
"""
LalaStories - Story Asset Generator

Generates cartoon images and Persian voice narration for a new story.
Run after Claude has written the story JSON.

Usage:
  python3 scripts/generate_story.py <story_id> <story_json_path> [--voice-id <id>]
  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json --voice-id abc123
  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json --skip-voice
"""

import os, sys, json, time, requests, argparse

# ── API keys ──────────────────────────────────────────────────────────────────
NANOBANANA_KEY = os.environ.get("NANOBANANA_KEY", "5988604c4049d982edddda81bb157efe")
NANOBANANA_BASE = "https://api.nanobananaapi.ai/api/v1/nanobanana"

ELEVENLABS_KEY = os.environ.get("ELEVENLABS_KEY", "sk_285f7107d7f0603fd35004c0c8b529e52f123ee77a5c450e")
ELEVENLABS_BASE = "https://api.elevenlabs.io/v1"

NOUSHIN_VOICE_ID = os.environ.get("NOUSHIN_VOICE_ID", "")

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets", "stories")


# ── Image generation ──────────────────────────────────────────────────────────

def submit_image(prompt: str) -> str:
    resp = requests.post(
        f"{NANOBANANA_BASE}/generate-2",
        headers={"Authorization": f"Bearer {NANOBANANA_KEY}", "Content-Type": "application/json"},
        json={"prompt": prompt, "aspectRatio": "1:1"},
        timeout=30,
    )
    resp.raise_for_status()
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"Submit failed: {data}")
    return data["data"]["taskId"]


def poll_image(task_id: str, timeout: int = 300) -> str:
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(5)
        poll = requests.get(
            f"{NANOBANANA_BASE}/record-info",
            headers={"Authorization": f"Bearer {NANOBANANA_KEY}"},
            params={"taskId": task_id},
            timeout=15,
        )
        data = poll.json().get("data", {})
        flag = data.get("successFlag", 0)
        if flag == 1:
            return data["response"]["resultImageUrl"]
        if flag in (2, 3):
            raise RuntimeError(f"Image failed: {data.get('errorMessage')}")
    raise TimeoutError(f"Image timed out after {timeout}s")


def download_file(url: str, path: str):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    r = requests.get(url, timeout=60)
    r.raise_for_status()
    with open(path, "wb") as f:
        f.write(r.content)


def generate_image(prompt: str, output_path: str):
    """Submit → poll → download one image."""
    task_id = submit_image(prompt)
    print(f"    task: {task_id}")
    url = poll_image(task_id)
    download_file(url, output_path)
    print(f"    ✓ {os.path.basename(output_path)}")


def image_prompt_for_page(page: dict, story_title: str) -> str:
    return (
        f"Colorful children's cartoon book illustration. "
        f"Story: {story_title}. Scene: {page['text'][:300]}. "
        f"Art style: flat 2D cartoon, bright saturated colors, cute characters, "
        f"child-friendly, Persian fairy tale aesthetic. No text in the image."
    )


def image_prompt_for_cover(story: dict) -> str:
    return (
        f"Children's book cover illustration. "
        f"Title: {story['title']}. {story['description']}. "
        f"Style: colorful vibrant cartoon, eye-catching, cute, "
        f"suitable for ages {story['ageMin']}-{story['ageMax']}, "
        f"Persian fairy tale aesthetic. No text in the image."
    )


# ── Voice generation ──────────────────────────────────────────────────────────

def generate_voice(text: str, voice_id: str, output_path: str):
    resp = requests.post(
        f"{ELEVENLABS_BASE}/text-to-speech/{voice_id}",
        headers={"xi-api-key": ELEVENLABS_KEY, "Content-Type": "application/json"},
        json={
            "text": text,
            "model_id": "eleven_multilingual_v2",
            "voice_settings": {
                "stability": 0.55,
                "similarity_boost": 0.80,
                "style": 0.30,
                "use_speaker_boost": True,
            },
        },
        timeout=120,
    )
    resp.raise_for_status()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "wb") as f:
        f.write(resp.content)
    print(f"    ✓ voice.mp3")


def build_voice_text_map(pages: list, output_path: str):
    """
    Approximate second-by-second subtitle map.
    Persian narration ≈ 2 words/second, min 4 seconds per page.
    """
    lines = []
    current = 0
    for page in pages:
        words = page["text"].split()
        duration = max(4, round(len(words) / 2.0))
        for s in range(current, current + duration):
            lines.append(f"{s:02d}s: {page['text']}")
        current += duration

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"    ✓ voice_text_map.txt ({current}s estimated duration)")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Generate story assets for LalaStories")
    parser.add_argument("story_id", help="Story folder name (e.g. 'rapunzel')")
    parser.add_argument("story_json", help="Path to story.json draft")
    parser.add_argument("--voice-id", default=NOUSHIN_VOICE_ID,
                        help="ElevenLabs voice ID for Noushin")
    parser.add_argument("--skip-images", action="store_true")
    parser.add_argument("--skip-voice", action="store_true")
    args = parser.parse_args()

    with open(args.story_json, "r", encoding="utf-8") as f:
        story = json.load(f)

    story_dir = os.path.abspath(os.path.join(ASSETS_DIR, args.story_id))
    images_dir = os.path.join(story_dir, "images")
    os.makedirs(images_dir, exist_ok=True)

    print(f"\n📖  {story['title']}")
    print(f"📁  {story_dir}")
    print(f"📄  {len(story['pages'])} pages\n")

    # Fix image paths to use .jpg
    for page in story["pages"]:
        page["image"] = f"images/{page['pageNumber']}.jpg"

    # Save story.json
    story_json_path = os.path.join(story_dir, "story.json")
    with open(story_json_path, "w", encoding="utf-8") as f:
        json.dump(story, f, ensure_ascii=False, indent=2)
    print("✓ story.json saved\n")

    # ── Images ────────────────────────────────────────────────────────────────
    if not args.skip_images:
        print("🎨 Generating page images...")
        for page in story["pages"]:
            num = page["pageNumber"]
            out = os.path.join(images_dir, f"{num}.jpg")
            if os.path.exists(out):
                print(f"  page {num}: already exists, skipping")
                continue
            print(f"  page {num}/{len(story['pages'])}:")
            prompt = image_prompt_for_page(page, story["title"])
            generate_image(prompt, out)

        print("\n🎨 Generating cover image...")
        cover_path = os.path.join(story_dir, "cover.jpg")
        if os.path.exists(cover_path):
            print("  cover: already exists, skipping")
        else:
            generate_image(image_prompt_for_cover(story), cover_path)

    # ── Voice ─────────────────────────────────────────────────────────────────
    if not args.skip_voice:
        voice_id = args.voice_id
        if not voice_id:
            print("\n⚠️  No voice ID provided — skipping voice generation.")
            print("   Re-run with --voice-id <elevenlabs_voice_id> to add narration.")
        else:
            print("\n🎙️  Generating voice narration...")
            full_text = "  ".join(p["text"] for p in story["pages"])
            voice_path = os.path.join(story_dir, "voice.mp3")
            generate_voice(full_text, voice_id, voice_path)

            map_path = os.path.join(story_dir, "voice_text_map.txt")
            build_voice_text_map(story["pages"], map_path)

    print(f"\n✅ Done! '{story['title']}' is ready.")
    print("   Rebuild the app and the story will appear on the home screen.")


if __name__ == "__main__":
    main()
