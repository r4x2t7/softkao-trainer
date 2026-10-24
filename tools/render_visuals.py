#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 assets/visuals 里的 HTML 图解渲染成 PNG，做视觉体检。

用无头 Chrome/Edge 渲染，不需要设备、不需要联网。
渲染完可以人眼（或多模态）看有没有排版塌陷、文字溢出、对比度过低。

用法：
    python tools/render_visuals.py                # 渲染全部
    python tools/render_visuals.py si-cpm-network # 只渲染匹配的
    python tools/render_visuals.py --dark         # 渲染暗色版
"""
from __future__ import annotations

import pathlib
import shutil
import subprocess
import sys
import tempfile

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
VIS = ROOT / "app" / "src" / "main" / "assets" / "visuals"
OUT = ROOT / "build" / "visual-shots"

BROWSERS = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]

WIDTH = 360
HEIGHT = 800


def find_browser() -> str | None:
    for b in BROWSERS:
        if pathlib.Path(b).exists():
            return b
    return None


NO_ANIM = (
    "<style>*{animation:none!important;transition:none!important;"
    "opacity:1!important;transform:none!important}</style>"
)


def render(browser: str, html: pathlib.Path, png: pathlib.Path, profile: str, width: int = 360, height: int = 800) -> bool:
    # 渲染前先禁掉入场动画：否则截到的是 opacity:0 的中间态，会误判成「元素没渲染出来」
    tmp = pathlib.Path(tempfile.mkdtemp(prefix="shot-"))
    src = tmp / html.name
    text = html.read_text(encoding="utf-8")
    src.write_text(text.replace("</head>", NO_ANIM + "</head>", 1), encoding="utf-8")
    cmd = [
        browser,
        "--headless=new",
        "--disable-gpu",
        "--no-sandbox",
        "--hide-scrollbars",
        f"--user-data-dir={profile}",
        "--virtual-time-budget=4000",
        f"--screenshot={png}",
        f"--window-size={width},{height}",
        src.as_uri(),
    ]
    try:
        subprocess.run(cmd, timeout=60, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception:
        shutil.rmtree(tmp, ignore_errors=True)
        return False
    shutil.rmtree(tmp, ignore_errors=True)
    return png.exists() and png.stat().st_size > 0


def main() -> int:
    args = sys.argv[1:]
    dark = "--dark" in args
    width = WIDTH
    height = HEIGHT
    rest = []
    for a in args:
        if a.startswith("--size="):
            w, _, h = a.split("=", 1)[1].partition("x")
            width, height = int(w), int(h)
        elif a.startswith("--"):
            continue
        else:
            rest.append(a)
    keyword = rest[0] if rest else ""

    browser = find_browser()
    if not browser:
        print("找不到 Chrome / Edge，无法渲染")
        return 2

    OUT.mkdir(parents=True, exist_ok=True)
    profile = tempfile.mkdtemp(prefix="vis-shot-")
    ok = 0
    fail = 0
    try:
        for html in sorted(VIS.rglob("*.html")):
            if keyword and keyword not in html.stem:
                continue
            if dark != html.name.endswith(".dark.html"):
                continue
            png = OUT / (html.stem.replace(".dark", "") + ".png")
            if render(browser, html, png, profile, width, height):
                ok += 1
                print(f"[ok] {html.name} -> {png.name} ({png.stat().st_size // 1024} KB)")
            else:
                fail += 1
                print(f"[fail] {html.name}")
    finally:
        shutil.rmtree(profile, ignore_errors=True)
    print(f"渲染成功 {ok}，失败 {fail}，输出目录 {OUT}")
    return 0 if fail == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
