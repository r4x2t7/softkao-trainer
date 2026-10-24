#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从国家法律法规数据库（flk.npc.gov.cn）检索并抓取现行法律正文。

用法：
    python tools/flk_fetch.py <法律名称关键字> [grep正则]
例：
    python tools/flk_fetch.py 专利法 "第四十二条|二十年|十五年|十年"
"""
from __future__ import annotations

import json
import re
import sys
import time
import urllib.parse
import urllib.request

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SoftKaoTrainer/1.0"
LIST_API = "https://flk.npc.gov.cn/api/"
DETAIL_API = "https://flk.npc.gov.cn/api/detail"


def post(url: str, data: dict) -> str:
    body = urllib.parse.urlencode(data).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=body,
        headers={
            "User-Agent": UA,
            "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
            "Referer": "https://flk.npc.gov.cn/",
            "Accept": "application/json, text/plain, */*",
        },
    )
    with urllib.request.urlopen(req, timeout=40) as resp:
        return resp.read().decode("utf-8", errors="replace")


def search(keyword: str) -> list[dict]:
    data = {
        "type": "flfg",
        "searchType": "title;vague",
        "sortTr": "f_bbrq_s;desc",
        "gbrqStart": "",
        "gbrqEnd": "",
        "sxrqStart": "",
        "sxrqEnd": "",
        "sort": "true",
        "page": "1",
        "size": "20",
        "title": keyword,
        "_": str(int(time.time() * 1000)),
    }
    raw = post(LIST_API, data)
    obj = json.loads(raw)
    result = obj.get("result", {}) or {}
    items = result.get("data", []) or []
    return items


def detail(law_id: str) -> str:
    raw = post(DETAIL_API, {"id": law_id})
    obj = json.loads(raw)
    data = obj.get("result", {}) or {}
    body = data.get("body", "")
    if isinstance(body, list):
        body = "\n".join(str(x) for x in body)
    return body


def main() -> int:
    keyword = sys.argv[1] if len(sys.argv) > 1 else ""
    pattern = sys.argv[2] if len(sys.argv) > 2 else "第[一二三四五六七八九十百]+条"
    if not keyword:
        print("用法: python tools/flk_fetch.py <关键字> [正则]")
        return 2
    try:
        items = search(keyword)
    except Exception as exc:  # noqa: BLE001
        print(f"[search-failed] {exc}")
        return 2
    if not items:
        print("[no-result]")
        return 1

    for it in items[:6]:
        title = it.get("title", "")
        law_id = it.get("id", "")
        status = it.get("status", "")
        print(f"\n### {title}  id={law_id} status={status}")
        try:
            body = detail(law_id)
        except Exception as exc:  # noqa: BLE001
            print(f"  [detail-failed] {exc}")
            continue
        print(f"  [body chars] {len(body)}")
        rx = re.compile(pattern)
        shown = 0
        for line in body.split("\n"):
            line = line.strip()
            if line and rx.search(line):
                print(f"    {line[:300]}")
                shown += 1
                if shown >= 25:
                    break
        if shown == 0:
            print("    (无命中)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
