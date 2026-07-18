---
name: import-web-story
description: Import a children's story from a published web article (e.g. mooshima.com) into the LalaStories manifest pipeline — paginates by inline images, downloads and compresses the images, and appends a stories-manifest.json entry. Use when the user gives a story URL and asks to add/import it, or says "paginate by image" for a web page.
---

# Import Web Story Skill

Imports an already-illustrated story from a WordPress-style article page (mooshima.com and similar sites) into this repo's remote manifest pipeline. Different from `generate-story` (which creates a brand-new AI-generated story) — this one scrapes an existing published story.

## Trigger

User gives a story URL (e.g. `https://mooshima.com/mag/some-story/`) and asks to import/add it, paginate it by image, or add it to the manifest.

## Step 1 — Extract title, description, and paginated text

```bash
cd /home/matin/projects/Kid-stories
python3 scripts/extract_web_story.py "<URL>" -o /tmp/story_extract.json
cat /tmp/story_extract.json
```

This fetches the page, finds the article body (`div.entry-content` or similar), and splits the story into pages: text accumulates until the next `<img>`, which ends that page. Trailing text after the last image (e.g. "پایان.") folds into the last page. A trailing `<h2>` heading (e.g. "چه چیزی می‌آموزیم؟") stops extraction — everything after it (moral/discussion sections) is not story content.

Sanity-check the output:
- Page count should match the number of illustrations you'd expect for the story.
- Skim page text for leftover boilerplate (nav links, "مشاهده کنید" cross-links) — the script filters known patterns but a new site may use different phrasing; strip anything that isn't the story if so.
- If the site's markup doesn't match (`Could not find article content`), fetch raw HTML with `curl -s --compressed -A "Mozilla/5.0 ..." <url>` and `grep -n` for the content container/image pattern, then adjust `CONTENT_SELECTORS` in the script.

## Step 2 — Decide the story slug and manifest fields

- **id / folder slug**: transliterate the Persian title into a short kebab-case Latin slug (matches existing ids like `kadoo-gholghole-zan`, `mahosetare`). Drop generic prefixes like "قصه کودکانه" from the title before slugging.
- **title**: the real story title, without the "قصه کودکانه" prefix.
- **description**: use the extracted excerpt if present; otherwise write a one-line hook.
- **ageMin / ageMax**: judge from tone/complexity (existing stories range 3–8).

## Step 3 — Download and compress images

```bash
mkdir -p /home/matin/stories/{slug}
cd /home/matin/stories/{slug}
curl -s -o cover.webp "<cover_image url from extract>"
curl -s -o 1.webp "<page 1 image url>"
curl -s -o 2.webp "<page 2 image url>"
# ... one per page, named by pageNumber
```
(Use whatever extension the source URL has — usually `.webp`; the converter below handles any input format.)

Then convert everything to JPG under 100KB and drop the originals:

```bash
cd /home/matin/projects/Kid-stories
python3 scripts/convert_images_to_jpg.py /home/matin/stories/{slug} --max-kb 100
```

Verify: `ls -la /home/matin/stories/{slug}` — every file should be `.jpg` and under the size cap.

## Step 4 — Append to stories-manifest.json

Edit `docs/remote-manifests/stories-manifest.json`, following the exact schema of existing entries (see `kadoo-gholghole-zan` in that file). Add a new object to the `items` array:

```json
{
  "id": "{slug}",
  "version": 1,
  "title": "{title}",
  "description": "{description}",
  "ageMin": 3,
  "ageMax": 8,
  "cover": "https://cdn-lalastories.baftaloo.ir/story/{slug}/cover.jpg",
  "pages": [
    { "pageNumber": 1, "text": "...", "image": "https://cdn-lalastories.baftaloo.ir/story/{slug}/1.jpg" }
  ]
}
```

**Image URLs are placeholders** — the real files only exist locally under `/home/matin/stories/{slug}/` until someone uploads them to the CDN. Always use the `https://cdn-lalastories.baftaloo.ir/story/{slug}/{page}.jpg` pattern (same domain as the real `kadoo` entry) so the manifest is upload-ready — do not use a fake domain like `example.com`. Omit the `voice` field entirely (it's optional) since there's no narration audio for an imported story.

Validate after editing:

```bash
python3 -c "
import json
d = json.load(open('docs/remote-manifests/stories-manifest.json', encoding='utf-8'))
print('items:', len(d['items']))
for it in d['items']:
    print(it['id'], len(it['pages']), 'pages')
"
```

## Step 5 — Report

Tell the user: story id/title, page count, where the local images live, and that the manifest entry uses placeholder CDN URLs pending upload.

## Key files

- Extractor: `scripts/extract_web_story.py`
- Image converter: `scripts/convert_images_to_jpg.py`
- Manifest: `docs/remote-manifests/stories-manifest.json`
- Local image staging: `/home/matin/stories/{slug}/`
