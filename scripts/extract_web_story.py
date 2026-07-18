#!/usr/bin/env python3
"""Extract a children's story from a mooshima.com-style article page.

Fetches the URL, finds the article body, and paginates the story by
splitting text at every inline <img>/<figure> — the text since the
previous image (or start) becomes one page, ending with that image.
Any trailing text after the last image is appended to the last page.

Usage:
    python3 extract_web_story.py <url> [-o out.json]

Output JSON:
    {
      "title": "...", "description": "...", "cover_image": "https://...",
      "pages": [{"pageNumber": 1, "text": "...", "image": "https://..."}]
    }
"""
import argparse
import json
import re
import sys

import requests
from bs4 import BeautifulSoup

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

BOILERPLATE_PREFIXES = (
    "مشاهده کنید",
    "بیشتر بخوانید",
    "همچنین بخوانید",
    "پیشنهاد می‌کنیم",
    "پیشنهاد میکنیم",
)

CONTENT_SELECTORS = [
    "div.entry-content",
    "div.wp-block-post-content",
    "article .entry-content",
    "article",
]


def fetch_soup(url):
    resp = requests.get(url, headers={"User-Agent": UA}, timeout=30)
    resp.raise_for_status()
    return BeautifulSoup(resp.text, "lxml")


def find_content(soup):
    for sel in CONTENT_SELECTORS:
        node = soup.select_one(sel)
        if node:
            return node
    raise SystemExit("Could not find article content — inspect the page manually.")


def best_img_src(img):
    # Prefer the plain src (already full quality); fall back to first srcset candidate.
    src = img.get("src")
    if src:
        return src
    srcset = img.get("srcset", "")
    if srcset:
        return srcset.split(",")[0].strip().split(" ")[0]
    return None


def extract(url):
    soup = fetch_soup(url)

    title_node = soup.select_one("h1.wp-block-post-title") or soup.select_one("h1")
    title = title_node.get_text(strip=True) if title_node else None

    excerpt_node = soup.select_one(".wp-block-post-excerpt__excerpt")
    description = excerpt_node.get_text(strip=True) if excerpt_node else None

    cover_node = soup.select_one("figure.wp-block-post-featured-image img")
    cover_image = best_img_src(cover_node) if cover_node else None

    content = find_content(soup)

    pages = []
    buf = []
    page_num = 1
    # Only match "img" (not "figure") to avoid double-counting a figure and its nested img.
    for el in content.find_all(["p", "img", "h2"], recursive=True):
        if el.name == "h2":
            # Section headings (e.g. "چه چیزی می‌آموزیم؟") mark the end of the narrative.
            break
        if el.name == "p":
            text = el.get_text(" ", strip=True)
            if text and text != "." and not text.startswith(BOILERPLATE_PREFIXES):
                buf.append(text)
        elif el.name == "img":
            src = best_img_src(el)
            if not src:
                continue
            pages.append({
                "pageNumber": page_num,
                "text": "\n\n".join(buf).strip(),
                "image": src,
            })
            page_num += 1
            buf = []

    # Leftover trailing text with no following image — fold into the last page.
    leftover = "\n\n".join(buf).strip()
    if leftover:
        if pages:
            pages[-1]["text"] = (pages[-1]["text"] + "\n\n" + leftover).strip()
        else:
            pages.append({"pageNumber": 1, "text": leftover, "image": None})

    return {
        "title": title,
        "description": description,
        "cover_image": cover_image,
        "pages": pages,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("url")
    ap.add_argument("-o", "--output", help="Write JSON to this file instead of stdout")
    args = ap.parse_args()

    data = extract(args.url)
    out = json.dumps(data, ensure_ascii=False, indent=2)
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(out)
        print(f"Wrote {len(data['pages'])} pages to {args.output}", file=sys.stderr)
    else:
        print(out)


if __name__ == "__main__":
    main()
