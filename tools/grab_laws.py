#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 Wikisource 上的中国法律全文抓到本地，供离线核对题库里的法条引用。

用法：
    python tools/grab_laws.py            # 抓全部
    python tools/grab_laws.py 专利法      # 只抓标题含该关键字的
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "research" / "laws"
UA = "SoftKaoTrainer/1.0 (offline study tool; one request per 2s)"
SITE = "zh.wikisource.org"
DELAY = 2.0

TITLES = [
    "中华人民共和国著作权法",
    "中华人民共和国著作权法 (2020年)",
    "中华人民共和国专利法",
    "中华人民共和国专利法 (2020年)",
    "中华人民共和国商标法",
    "中华人民共和国标准化法",
    "中华人民共和国招标投标法",
    "中华人民共和国招标投标法实施条例",
    "中华人民共和国民法典/第一编 总则",
    "中华人民共和国民法典/第三编 合同",
    "中华人民共和国民法典/第四编 人格权",
    "中华人民共和国民法典/第七编 侵权责任",
    "中华人民共和国网络安全法",
    "中华人民共和国数据安全法",
    "中华人民共和国个人信息保护法",
    "中华人民共和国政府采购法",
    "中华人民共和国反不正当竞争法",
    "中华人民共和国会计法",
    "中华人民共和国保守国家秘密法",
]


def api(params: dict) -> dict:
    url = f"https://{SITE}/w/api.php?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    last = None
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                return json.loads(resp.read().decode("utf-8", errors="replace"))
        except Exception as exc:  # noqa: BLE001
            last = exc
            time.sleep(3.0 * (attempt + 1))
    raise last  # type: ignore[misc]


def wikitext(title: str) -> str:
    obj = api({
        "action": "query", "prop": "revisions", "rvprop": "content",
        "rvslots": "main", "format": "json", "formatversion": "2", "titles": title,
    })
    pages = obj.get("query", {}).get("pages", [])
    if not pages or "revisions" not in pages[0]:
        return ""
    return pages[0]["revisions"][0]["slots"]["main"].get("content", "")


def resolve(title: str, depth: int = 0) -> tuple[str, str]:
    """法律条目页通常只是一层跳转（{{:中华人民共和国专利法 (2020年)}}），跟着跳到正文。"""
    text = wikitext(title)
    if depth < 3:
        m = re.search(r"\{\{:\s*([^}|\n]+?)\s*\}\}", text)
        if m and len(text) < 4000:
            target = m.group(1).strip()
            body = wikitext(target)
            if len(body) > len(text):
                return target, body
    return title, text


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    keyword = sys.argv[1] if len(sys.argv) > 1 else ""
    total = 0
    for title in TITLES:
        if keyword and keyword not in title:
            continue
        try:
            real_title, text = resolve(title)
        except Exception as exc:  # noqa: BLE001
            print(f"[fail] {title}: {exc}")
            continue
        if not text.strip():
            print(f"[empty] {title}")
            continue
        safe = re.sub(r'[\\/:*?"<>|\s]+', "_", title)
        p = OUT / f"{safe}.txt"
        p.write_text(text, encoding="utf-8")
        total += 1
        print(f"[ok] {title}  ->  {real_title}  {len(text)} chars")
        time.sleep(DELAY)
    print(f"共 {total} 份，输出目录 {OUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
