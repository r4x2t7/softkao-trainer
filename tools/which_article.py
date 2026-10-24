#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""在法条全文里定位某个表述属于第几条。

用法：
    python tools/which_article.py <法条文件> <关键词>
    python tools/which_article.py --kw 转委托        # 在全部法条里找
"""
from __future__ import annotations

import pathlib
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
LAWS = ROOT / "docs" / "research" / "laws"
MARK = re.compile(r"第([零一二三四五六七八九十百千\d]+)条")


def articles(text: str) -> list[tuple[str, str]]:
    """把全文切成 (条号, 条文) 序列。"""
    out = []
    parts = MARK.split(text)
    # parts = [前导, 条号1, 条文1, 条号2, 条文2, ...]
    for i in range(1, len(parts) - 1, 2):
        out.append((parts[i], parts[i + 1]))
    return out


def main() -> int:
    args = sys.argv[1:]
    if args and args[0] == "--kw":
        kw = args[1]
        files = sorted(LAWS.glob("*.txt"))
    else:
        if len(args) < 2:
            print("用法: python tools/which_article.py <法条文件> <关键词>")
            return 2
        files = [pathlib.Path(args[0])]
        kw = args[1]

    found = 0
    for f in files:
        text = f.read_text(encoding="utf-8", errors="replace")
        for num, body in articles(text):
            if kw in body:
                snippet = re.sub(r"\s+", " ", body).strip()
                print(f"[{f.stem}] 第{num}条：{snippet[:260]}")
                found += 1
    if not found:
        print(f"(未找到：{kw})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
