#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""给图解 HTML 注入一个尺寸探针，渲染出来就能看到实测布局数据。

做法：复制一份到临时目录，追加一段脚本把
  文档滚动宽度 / body 宽度 / svg 元素盒 / svg 的 viewBox 缩放比
写进页面上，再截图。这样就不用靠猜。

用法：
    python tools/probe_visual.py si-osi-layers
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
]

PROBE = """
<script>
window.addEventListener('load', function(){
  setTimeout(function(){
    var d = document.documentElement, b = document.body;
    var svg = document.querySelector('svg');
    var r = svg ? svg.getBoundingClientRect() : null;
    var vb = svg ? svg.getAttribute('viewBox') : '';
    var parts = (vb||'').split(/[\\s,]+/);
    var s = r && parts.length>=4 ? Math.min(r.width/parts[2], r.height/parts[3]) : 0;
    var stage = document.querySelector('.stage');
    var sr = stage ? stage.getBoundingClientRect() : null;
    var box = document.createElement('div');
    box.style.cssText = 'background:#000;color:#0f0;font:12px monospace;padding:6px;white-space:pre;position:relative;z-index:99';
    box.textContent =
      'doc.scrollW=' + d.scrollWidth + ' doc.clientW=' + d.clientWidth +
      '\\nbody.scrollW=' + b.scrollWidth + ' body.clientW=' + b.clientWidth +
      '\\nstage=' + (sr ? Math.round(sr.width)+'x'+Math.round(sr.height) : 'n/a') +
      '\\nsvgBox=' + (r ? Math.round(r.width)+'x'+Math.round(r.height) : 'n/a') +
      '\\nviewBox=' + vb + ' scale=' + (s ? s.toFixed(3) : 'n/a') +
      '\\nviewport=' + window.innerWidth + 'x' + window.innerHeight;
    document.body.insertBefore(box, document.body.firstChild);
  }, 300);
});
</script>
"""


def main() -> int:
    keyword = sys.argv[1] if len(sys.argv) > 1 else ""
    browser = next((b for b in BROWSERS if pathlib.Path(b).exists()), None)
    if not browser:
        print("找不到浏览器")
        return 2
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = pathlib.Path(tempfile.mkdtemp(prefix="probe-"))
    profile = tempfile.mkdtemp(prefix="probe-prof-")
    try:
        for html in sorted(VIS.rglob("*.html")):
            if keyword and keyword not in html.stem:
                continue
            copy = tmp / html.name
            shutil.copyfile(html, copy)
            copy.write_text(copy.read_text(encoding="utf-8") + PROBE, encoding="utf-8")
            png = OUT / ("probe-" + html.stem.replace(".dark", "") + ".png")
            cmd = [
                browser, "--headless=new", "--disable-gpu", "--no-sandbox",
                "--hide-scrollbars", f"--user-data-dir={profile}",
                "--virtual-time-budget=5000",
                f"--screenshot={png}", "--window-size=360,800",
                copy.as_uri(),
            ]
            subprocess.run(cmd, timeout=60, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print(f"[ok] {png.name}")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
        shutil.rmtree(profile, ignore_errors=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
