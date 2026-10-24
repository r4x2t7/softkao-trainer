#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""本地抓取 + 关键词过滤：只把命中的行带回终端，避免整页灌进上下文。

用于核对题库/知识点精讲里的法条、标准与公式数字。
不调用任何付费搜索接口，只做 HTTP GET。

用法：
    python tools/fetch_grep.py <url> <正则> [--context N] [--limit N] [--max-chars N]
    python tools/fetch_grep.py --file <本地文件> <正则>
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys
import urllib.request

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

UA = "SoftKaoTrainer-FactAudit/1.0 (offline study tool; contact: repo owner)"


def fetch(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "text/plain, */*"})
    with urllib.request.urlopen(req, timeout=40) as resp:
        return resp.read().decode("utf-8", errors="replace")


def unwrap_api(text: str) -> str:
    """MediaWiki API 返回 JSON，wikitext 在 revisions[].slots.main.content 里，这里拆出来。"""
    stripped = text.lstrip()
    if not stripped.startswith("{"):
        return text
    try:
        obj = json.loads(text)
    except Exception:
        return text
    pages = (obj.get("query") or {}).get("pages") or []
    out = []
    for p in pages:
        for rev in p.get("revisions") or []:
            slots = rev.get("slots") or {}
            main = slots.get("main") or {}
            if main.get("content"):
                out.append(main["content"])
    return "\n".join(out) if out else text


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("url", help="要抓取的 URL")
    ap.add_argument("pattern", help="正则")
    ap.add_argument("--context", type=int, default=0, help="命中行前后各带几行")
    ap.add_argument("--limit", type=int, default=25, help="最多显示多少条命中")
    ap.add_argument("--max-chars", type=int, default=400, help="单行最多显示多少字符")
    ap.add_argument("--total-chars", type=int, default=0, help=">0 时先截断正文到这么多字符")
    args = ap.parse_args()

    try:
        if args.url.startswith("file:"):
            text = pathlib.Path(args.url[5:]).read_text(encoding="utf-8", errors="replace")
        elif pathlib.Path(args.url).exists():
            text = pathlib.Path(args.url).read_text(encoding="utf-8", errors="replace")
        else:
            text = fetch(args.url)
    except Exception as exc:  # noqa: BLE001
        print(f"[fetch-failed] {args.url}: {exc}")
        return 2

    if args.total_chars:
        text = text[: args.total_chars]

    text = unwrap_api(text)
    lines = text.split("\n")
    rx = re.compile(args.pattern)
    hits = []
    for i, line in enumerate(lines):
        if rx.search(line):
            lo = max(0, i - args.context)
            hi = min(len(lines), i + args.context + 1)
            block = "\n".join(f"  {j+1}| {lines[j].strip()[:args.max_chars]}" for j in range(lo, hi))
            hits.append(block)
            if len(hits) >= args.limit:
                break

    print(f"[source] {args.url}  [chars] {len(text)}  [hits] {len(hits)}")
    if not hits:
        print("  (无命中)")
    for h in hits:
        print(h)
        print("  ---")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
