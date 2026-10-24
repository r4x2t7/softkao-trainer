#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查图解 SVG 的内容有没有画到 viewBox 之外（超出就会被裁掉）。

判定方式：直接调用生成器的 fit_viewbox —— 如果它把 viewBox 改了，
说明内容确实超出了当前画布，这个图解在手机上会被裁掉。

用法：
    python tools/check_visual_bounds.py            # 检查全部
    python tools/check_visual_bounds.py si-cpm    # 只看匹配项
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
VIS = ROOT / "app" / "src" / "main" / "assets" / "visuals"

sys.path.insert(0, str(ROOT / "tools"))
import build_visuals  # noqa: E402

SVG_RE = re.compile(r"<svg\b.*?</svg>", re.S)
VIEWBOX_RE = re.compile(r'viewBox="([^"]+)"')


def check(path: pathlib.Path) -> list[str]:
    text = path.read_text(encoding="utf-8")
    m = SVG_RE.search(text)
    if not m:
        return [f"{path.name}: 找不到 <svg>"]
    svg = m.group(0)
    original = VIEWBOX_RE.search(svg)
    if not original:
        return [f"{path.name}: 找不到 viewBox"]
    fitted = build_visuals.fit_viewbox(svg, pad=0.0)
    after = VIEWBOX_RE.search(fitted)
    if not after:
        return [f"{path.name}: fit_viewbox 后找不到 viewBox"]
    if after.group(1).strip() == original.group(1).strip():
        return []
    return [
        f"内容超出 viewBox：原 {original.group(1)} → 应为 {after.group(1)}"
    ]


def main() -> int:
    keyword = sys.argv[1] if len(sys.argv) > 1 else ""
    files = sorted(VIS.rglob("*.html"))
    bad = 0
    for f in files:
        if keyword and keyword not in f.stem:
            continue
        problems = check(f)
        if problems:
            bad += 1
            print(f"=== {f.relative_to(VIS)}")
            for p in problems:
                print("   " + p)
    print(f"\n检查 {len(files)} 个 HTML，内容越界 {bad} 个")
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
