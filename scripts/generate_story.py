#!/usr/bin/env python3
"""
LalaStories - Story Asset Generator

Generates cartoon images and Persian voice narration for a new story.
Run after Claude has written the story JSON.

Usage:
  python3 scripts/generate_story.py <story_id> <story_json_path> [options]

  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json
  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json --image-provider openrouter  # uses google/gemini-2.5-flash-image by default
  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json --tts-provider openrouter --tts-voice shimmer
  python3 scripts/generate_story.py rapunzel /tmp/rapunzel.json --skip-voice
"""

import os, sys, json, time, requests, argparse

# ── API keys ──────────────────────────────────────────────────────────────────
NANOBANANA_KEY  = os.environ.get("NANOBANANA_KEY", "5988604c4049d982edddda81bb157efe")
NANOBANANA_BASE = "https://api.nanobananaapi.ai/api/v1/nanobanana"

ELEVENLABS_KEY  = os.environ.get("ELEVENLABS_KEY", "sk_285f7107d7f0603fd35004c0c8b529e52f123ee77a5c450e")
ELEVENLABS_BASE = "https://api.elevenlabs.io/v1"
NOUSHIN_VOICE_ID = os.environ.get("NOUSHIN_VOICE_ID", "NZiuR1C6kVMSWHG27sIM")

OPENROUTER_KEY  = os.environ.get("OPENROUTER_KEY", "")
OPENROUTER_BASE = "https://openrouter.ai/api/v1"
OPENROUTER_DEFAULT_IMAGE_MODEL = "google/gemini-2.5-flash-image"

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets", "stories")


# ── Helpers ───────────────────────────────────────────────────────────────────

def download_file(url: str, path: str):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    r = requests.get(url, timeout=60)
    r.raise_for_status()
    with open(path, "wb") as f:
        f.write(r.content)


def compress_image(path: str, max_kb: int = 80):
    """Re-save image as JPEG under max_kb. Shrinks resolution if quality alone isn't enough."""
    from PIL import Image
    import io
    img = Image.open(path).convert("RGB")
    for quality in (85, 70, 55, 40, 25):
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=quality, optimize=True)
        if buf.tell() <= max_kb * 1024:
            with open(path, "wb") as f:
                f.write(buf.getvalue())
            return
    # Still too large — halve resolution and retry
    w, h = img.size
    img = img.resize((w // 2, h // 2), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=40, optimize=True)
    with open(path, "wb") as f:
        f.write(buf.getvalue())


def _openrouter_headers() -> dict:
    if not OPENROUTER_KEY:
        raise RuntimeError("OPENROUTER_KEY env var is not set.")
    return {"Authorization": f"Bearer {OPENROUTER_KEY}", "Content-Type": "application/json"}


# ── Image generation ──────────────────────────────────────────────────────────

def _generate_image_nanobanana(prompt: str, output_path: str):
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
    task_id = data["data"]["taskId"]
    print(f"    task: {task_id}")

    deadline = time.time() + 300
    while time.time() < deadline:
        time.sleep(5)
        poll = requests.get(
            f"{NANOBANANA_BASE}/record-info",
            headers={"Authorization": f"Bearer {NANOBANANA_KEY}"},
            params={"taskId": task_id},
            timeout=15,
        )
        d = poll.json().get("data", {})
        flag = d.get("successFlag", 0)
        if flag == 1:
            download_file(d["response"]["resultImageUrl"], output_path)
            return
        if flag in (2, 3):
            raise RuntimeError(f"Image failed: {d.get('errorMessage')}")
    raise TimeoutError("Image timed out after 300s")


def _generate_image_openrouter_dalle(prompt: str, output_path: str, model: str):
    """DALL-E-style models: use /images/generations endpoint."""
    resp = requests.post(
        f"{OPENROUTER_BASE}/images/generations",
        headers=_openrouter_headers(),
        json={"model": model, "prompt": prompt, "size": "1024x1024", "n": 1},
        timeout=120,
    )
    resp.raise_for_status()
    data = resp.json()
    item = data["data"][0]
    url = item.get("url") or item.get("b64_json")
    if not url:
        raise RuntimeError(f"No image in response: {data}")
    if url.startswith("http"):
        download_file(url, output_path)
    else:
        import base64
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        with open(output_path, "wb") as f:
            f.write(base64.b64decode(url))


def _extract_image_url(data: dict):
    """Return the image URL from a chat completion response, or None."""
    msg = data["choices"][0]["message"]
    content = msg.get("content")

    url = None
    if isinstance(content, list):
        for part in content:
            if part.get("type") == "image_url":
                url = part["image_url"]["url"]
                break
    elif isinstance(content, str) and (content.startswith("data:") or content.startswith("http")):
        url = content

    if url is None:
        images = msg.get("images")
        if isinstance(images, list) and images:
            img = images[0]
            if isinstance(img, dict):
                url = img.get("url") or (img.get("image_url") or {}).get("url")
    return url


def _is_content_filtered(data: dict) -> bool:
    choice = data.get("choices", [{}])[0]
    return choice.get("finish_reason") == "content_filter"


def _chat_image_request(prompt: str, model: str) -> dict:
    resp = requests.post(
        f"{OPENROUTER_BASE}/chat/completions",
        headers=_openrouter_headers(),
        json={"model": model, "messages": [{"role": "user", "content": prompt}]},
        timeout=180,
    )
    resp.raise_for_status()
    return resp.json()


def _generic_fallback_prompt() -> str:
    return (
        f"{_NO_TEXT} "
        "Colorful children's cartoon book illustration. "
        "A magical fantasy scene with a tall stone tower in a green forest. "
        "Art style: flat 2D cartoon, bright saturated colors, cute characters, child-friendly. "
        f"{_NO_TEXT}"
    )


def _generate_image_openrouter_chat(prompt: str, output_path: str, model: str):
    """Image generation via chat completions with content-filter and text-only retry."""
    import base64

    data = _chat_image_request(prompt, model)

    # If content filter triggered, retry with a generic scene-only prompt
    if _is_content_filtered(data):
        print("    ⚠ content filter — retrying with fallback prompt")
        data = _chat_image_request(_generic_fallback_prompt(), model)

    url = _extract_image_url(data)

    # If model returned text instead of an image, retry with explicit image-generation prompt
    if url is None:
        content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
        if isinstance(content, str) and content.strip():
            print("    ⚠ text-only response — retrying with explicit image prompt")
            retry_prompt = (
                f"{_NO_TEXT} "
                "Generate an image now. Do NOT write text. Just generate the image. "
                "Children's cartoon book illustration: " + prompt[:200] + " "
                "Art style: flat 2D cartoon, bright saturated colors, cute characters, child-friendly. "
                f"{_NO_TEXT}"
            )
            data = _chat_image_request(retry_prompt, model)
            url = _extract_image_url(data)

        if url is None:
            data = _chat_image_request(_generic_fallback_prompt(), model)
            url = _extract_image_url(data)

    if url is None:
        raise RuntimeError(f"No image in response after retries: {data}")

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    if url.startswith("data:"):
        _, b64_data = url.split(",", 1)
        with open(output_path, "wb") as f:
            f.write(base64.b64decode(b64_data))
    elif url.startswith("http"):
        download_file(url, output_path)
    else:
        raise RuntimeError(f"Unexpected URL format: {url[:80]}")


def _generate_image_openrouter(prompt: str, output_path: str, model: str):
    _generate_image_openrouter_chat(prompt, output_path, model)


def generate_image(prompt: str, output_path: str, provider: str, openrouter_model: str):
    if provider == "openrouter":
        _generate_image_openrouter(prompt, output_path, openrouter_model)
    else:
        _generate_image_nanobanana(prompt, output_path)
    compress_image(output_path)
    size_kb = os.path.getsize(output_path) // 1024
    print(f"    ✓ {os.path.basename(output_path)} ({size_kb} KB)")


_NO_TEXT = (
    "CRITICAL RULE: The image must contain ABSOLUTELY NO TEXT of any kind — "
    "no letters, no words, no numbers, no Arabic script, no Persian script, "
    "no Latin script, no captions, no labels, no signs, no writing anywhere. "
    "Pure illustration only."
)

_STYLE = (
    "Art style: flat 2D children's cartoon illustration, "
    "bright saturated colors, clean bold lines, cute expressive faces, child-friendly. "
    "CHARACTER CONSISTENCY: Every character must look IDENTICAL across all pages of this story — "
    "same face shape, same hair color and style, same clothing colors and design. "
    "CLASSIC FAIRY TALE APPEARANCE: Characters must look exactly as they appear in classic Western fairy tales — "
    "Rapunzel has extremely long golden hair flowing down; Cinderella wears a blue ball gown; "
    "Jack is a young boy in simple medieval tunic; Hansel and Gretel are children in European folk clothing; "
    "princes wear royal tunics and capes; witches are old women in dark robes. "
    "STRICT APPEARANCE RULES: NO scarves, NO hijabs, NO head coverings, NO veils on ANY character ever. "
    "Female characters have visible uncovered hair. "
    "Do NOT add any Middle-Eastern or Islamic clothing elements to any character."
)


def image_prompt_for_page(page: dict, story: dict) -> str:
    return (
        f"{_NO_TEXT} "
        f"Children's cartoon book illustration for the story '{story['title']}'. "
        f"Scene to illustrate: {page['text'][:300]}. "
        f"{_STYLE} "
        f"{_NO_TEXT}"
    )


def image_prompt_for_cover(story: dict) -> str:
    return (
        f"{_NO_TEXT} "
        f"Children's book cover illustration for '{story['title']}'. "
        f"{story['description']}. "
        f"Show the main characters in a single iconic scene. "
        f"{_STYLE} "
        f"Suitable for ages {story['ageMin']}-{story['ageMax']}. "
        f"{_NO_TEXT}"
    )


# ── Voice generation ──────────────────────────────────────────────────────────

def _generate_voice_elevenlabs(text: str, voice_id: str, output_path: str):
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


def _generate_voice_openrouter(text: str, voice: str, model: str, output_path: str):
    resp = requests.post(
        f"{OPENROUTER_BASE}/audio/speech",
        headers=_openrouter_headers(),
        json={
            "model": model,
            "input": text,
            "voice": voice,
            "response_format": "mp3",
        },
        timeout=180,
    )
    resp.raise_for_status()
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "wb") as f:
        f.write(resp.content)


def generate_voice(text: str, output_path: str, provider: str,
                   elevenlabs_voice_id: str, openrouter_voice: str, openrouter_tts_model: str):
    if provider == "openrouter":
        _generate_voice_openrouter(text, openrouter_voice, openrouter_tts_model, output_path)
    else:
        if not elevenlabs_voice_id:
            raise RuntimeError("--elevenlabs-voice-id is required when using elevenlabs TTS provider.")
        _generate_voice_elevenlabs(text, elevenlabs_voice_id, output_path)
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

    # Provider selection
    parser.add_argument("--image-provider", choices=["nanobanana", "openrouter"],
                        default="nanobanana",
                        help="Image generation provider (default: nanobanana)")
    parser.add_argument("--tts-provider", choices=["elevenlabs", "openrouter"],
                        default="elevenlabs",
                        help="Text-to-speech provider (default: elevenlabs)")

    # ElevenLabs options
    parser.add_argument("--elevenlabs-voice-id", default=NOUSHIN_VOICE_ID,
                        help="ElevenLabs voice ID (default: Noushin)")

    # OpenRouter options
    parser.add_argument("--openrouter-image-model", default=OPENROUTER_DEFAULT_IMAGE_MODEL,
                        help=f"OpenRouter image model (default: {OPENROUTER_DEFAULT_IMAGE_MODEL})")
    parser.add_argument("--openrouter-tts-model", default="openai/gpt-4o-mini-tts",
                        help="OpenRouter TTS model (default: openai/gpt-4o-mini-tts)")
    parser.add_argument("--tts-voice", default="shimmer",
                        help="Voice name for OpenRouter TTS (alloy/echo/fable/onyx/nova/shimmer, default: shimmer)")

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
    print(f"📄  {len(story['pages'])} pages")
    print(f"🎨  image provider : {args.image_provider}" + (
        f" ({args.openrouter_image_model})" if args.image_provider == "openrouter" else ""))
    print(f"🎙️   TTS provider   : {args.tts_provider}" + (
        f" ({args.openrouter_tts_model}, voice={args.tts_voice})" if args.tts_provider == "openrouter"
        else f" (voice_id={args.elevenlabs_voice_id})"))
    print()

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
            prompt = image_prompt_for_page(page, story)
            generate_image(prompt, out,
                           provider=args.image_provider,
                           openrouter_model=args.openrouter_image_model)

        print("\n🎨 Generating cover image...")
        cover_path = os.path.join(story_dir, "cover.jpg")
        if os.path.exists(cover_path):
            print("  cover: already exists, skipping")
        else:
            generate_image(image_prompt_for_cover(story), cover_path,
                           provider=args.image_provider,
                           openrouter_model=args.openrouter_image_model)

    # ── Voice ─────────────────────────────────────────────────────────────────
    if not args.skip_voice:
        print("\n🎙️  Generating voice narration...")
        full_text = "  ".join(p["text"] for p in story["pages"])
        voice_path = os.path.join(story_dir, "voice.mp3")
        generate_voice(full_text, voice_path,
                       provider=args.tts_provider,
                       elevenlabs_voice_id=args.elevenlabs_voice_id,
                       openrouter_voice=args.tts_voice,
                       openrouter_tts_model=args.openrouter_tts_model)

        map_path = os.path.join(story_dir, "voice_text_map.txt")
        build_voice_text_map(story["pages"], map_path)

    print(f"\n✅ Done! '{story['title']}' is ready.")
    print("   Rebuild the app and the story will appear on the home screen.")


if __name__ == "__main__":
    main()
