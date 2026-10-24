#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""知识点可视化生成器。

把 `tools/visuals_data.json` 里的抽象图解描述，渲染成自包含的 HTML + 内联 SVG 动画，
输出到 `app/src/main/assets/visuals/`：

    <examId>/<id>.html        浅色版
    <examId>/<id>.dark.html   深色版（App 按系统深浅色切换）
    index.json                清单，App 侧用它索引

每个 HTML 都是单文件、零外部依赖：SVG + CSS 关键帧 + 极少量 JS（只负责重播与暂停）。
App 用 WebView 以 file:///android_asset/ 为 baseUrl 加载，直接跑动画。

用法：
    python tools/build_visuals.py            # 生成全部
    python tools/build_visuals.py --check    # 只校验数据，不写文件
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

ROOT = Path(__file__).resolve().parent.parent
DATA_FILE = ROOT / "tools" / "visuals_data.json"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "visuals"

# ---------------------------------------------------------------- 主题配色

THEMES = {
    "light": {
        "--bg": "#FFFFFF",
        "--panel": "#F1F5FB",
        "--panel2": "#E2EAF6",
        "--line": "#C6D4E9",
        "--text": "#16202E",
        "--muted": "#5A6B80",
        "--primary": "#2F6BFF",
        "--on-primary": "#FFFFFF",
        "--accent": "#00A08F",
        "--on-accent": "#FFFFFF",
        "--warn": "#E97C10",
        "--on-warn": "#FFFFFF",
        "--danger": "#D93F45",
        "--on-danger": "#FFFFFF",
        "--c0": "#2F6BFF",
        "--c1": "#00A08F",
        "--c2": "#7A5AF8",
        "--c3": "#E97C10",
        "--c4": "#D93F45",
        "--c5": "#1F93B8",
        "--c6": "#8A6A2F",
        "--c7": "#4C6EF5",
    },
    "dark": {
        "--bg": "#12161C",
        "--panel": "#1C222C",
        "--panel2": "#262E3A",
        "--line": "#394455",
        "--text": "#E8EEF6",
        "--muted": "#9BA9BC",
        "--primary": "#7FA5FF",
        "--on-primary": "#0B1017",
        "--accent": "#33D3BC",
        "--on-accent": "#07201D",
        "--warn": "#FFB15C",
        "--on-warn": "#2A1804",
        "--danger": "#FF7276",
        "--on-danger": "#2B0709",
        "--c0": "#7FA5FF",
        "--c1": "#33D3BC",
        "--c2": "#B49BFF",
        "--c3": "#FFB15C",
        "--c4": "#FF7276",
        "--c5": "#59C7E8",
        "--c6": "#D9BC72",
        "--c7": "#A6B8FF",
    },
}

# ---------------------------------------------------------------- 公共 CSS

BASE_CSS = """
* { box-sizing: border-box; }
html, body {
  margin: 0; padding: 0; height: 100%;
  background: var(--bg);
  -webkit-text-size-adjust: 100%;
}
body {
  font-family: -apple-system, "PingFang SC", "Noto Sans CJK SC",
               "Source Han Sans SC", "Microsoft YaHei", sans-serif;
  color: var(--text);
  display: flex; flex-direction: column;
}
.wrap { display: flex; flex-direction: column; height: 100%; }
.head { padding: 10px 14px 4px 14px; }
.title {
  font-size: 15px; font-weight: 700; line-height: 1.35; color: var(--text);
}
.cap {
  font-size: 11.5px; line-height: 1.5; color: var(--muted); margin-top: 3px;
}
.stage { flex: 1 1 auto; min-height: 0; position: relative; }
.stage svg { display: block; width: 100%; height: 100%; }
.bar {
  display: flex; align-items: center; gap: 8px;
  padding: 4px 14px 8px 14px;
}
.bar .sp { flex: 1 1 auto; }
.btn {
  border: 1px solid var(--line); background: var(--panel); color: var(--text);
  border-radius: 999px; padding: 4px 12px; font-size: 11.5px; line-height: 1.6;
  cursor: pointer; user-select: none; -webkit-tap-highlight-color: transparent;
}
.btn:active { background: var(--panel2); }
.steps { display: flex; gap: 4px; flex-wrap: wrap; }
.dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--line); transition: background .25s, transform .25s;
}
.dot.on { background: var(--primary); transform: scale(1.35); }

/* 通用图元 */
.lbl  { font-size: 12px; font-weight: 600; fill: var(--text); }
.lbl-s{ font-size: 10.5px; font-weight: 500; fill: var(--muted); }
.lbl-w{ font-size: 12px; font-weight: 700; fill: var(--on-primary); }
.num  { font-size: 10px; font-weight: 700; fill: var(--muted); }
.card { fill: var(--panel); stroke: var(--line); stroke-width: 1; }
.card2{ fill: var(--panel2); stroke: var(--line); stroke-width: 1; }
.edge { stroke: var(--line); stroke-width: 1.6; fill: none; }
.dash { stroke-dasharray: 5 5; animation: dash 1.1s linear infinite; }
@keyframes dash { to { stroke-dashoffset: -20; } }
.hit  { fill: transparent; }

.c0 { fill: var(--c0); } .s0 { stroke: var(--c0); }
.c1 { fill: var(--c1); } .s1 { stroke: var(--c1); }
.c2 { fill: var(--c2); } .s2 { stroke: var(--c2); }
.c3 { fill: var(--c3); } .s3 { stroke: var(--c3); }
.c4 { fill: var(--c4); } .s4 { stroke: var(--c4); }
.c5 { fill: var(--c5); } .s5 { stroke: var(--c5); }
.c6 { fill: var(--c6); } .s6 { stroke: var(--c6); }
.c7 { fill: var(--c7); } .s7 { stroke: var(--c7); }
.on0 { fill: var(--on-primary); }
"""

# 每一类原型自己的动画，全部基于 CSS 变量，方便浅/深色共用
ANIM_CSS = """
/* 逐个淡入（通用） */
.fade { animation: fade .45s ease both; }
@keyframes fade { from { opacity: 0; } to { opacity: 1; } }

.pop { animation: pop .5s cubic-bezier(.2,.9,.3,1.3) both; transform-box: fill-box;
       transform-origin: center; }
@keyframes pop { from { opacity: 0; transform: scale(.55); } to { opacity: 1; transform: scale(1); } }

.drop { animation: drop .55s cubic-bezier(.2,.85,.3,1.05) both; }
@keyframes drop { from { opacity: 0; transform: translateY(-16px); } to { opacity: 1; transform: none; } }

.rise { animation: rise .55s cubic-bezier(.2,.85,.3,1.05) both; }
@keyframes rise { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: none; } }

.slideL { animation: slideL .55s cubic-bezier(.2,.85,.3,1.05) both; }
@keyframes slideL { from { opacity: 0; transform: translateX(-18px); } to { opacity: 1; transform: none; } }

.slideR { animation: slideR .55s cubic-bezier(.2,.85,.3,1.05) both; }
@keyframes slideR { from { opacity: 0; transform: translateX(18px); } to { opacity: 1; transform: none; } }

.draw { stroke-dasharray: var(--len, 1000); stroke-dashoffset: var(--len, 1000);
        animation: draw .9s ease forwards; }
@keyframes draw { to { stroke-dashoffset: 0; } }

.grow { animation: grow .8s cubic-bezier(.2,.85,.3,1.05) both; transform-box: fill-box;
        transform-origin: left center; }
@keyframes grow { from { transform: scaleX(0); } to { transform: scaleX(1); } }

.growv { animation: growv .8s cubic-bezier(.2,.85,.3,1.05) both; transform-box: fill-box;
         transform-origin: bottom center; }
@keyframes growv { from { transform: scaleY(0); } to { transform: scaleY(1); } }

.spin { animation: spin 1.6s linear infinite; transform-box: fill-box; transform-origin: center; }
@keyframes spin { to { transform: rotate(360deg); } }

.sweep { animation: sweep 3.2s ease-in-out infinite; }
@keyframes sweep {
  0%   { transform: translateY(0); opacity: 0; }
  12%  { opacity: .55; }
  70%  { opacity: .35; }
  100% { transform: translateY(150px); opacity: 0; }
}

.pulse { animation: pulse 1.8s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: .35; } 50% { opacity: 1; } }

.glow { animation: glow 2.2s ease-in-out infinite; }
@keyframes glow {
  0%,100% { stroke-opacity: .35; stroke-width: 1.6; }
  50%     { stroke-opacity: 1; stroke-width: 3; }
}

.blink { animation: blink 1.6s ease-in-out infinite; }
@keyframes blink { 0%,100% { opacity: .25; } 50% { opacity: 1; } }

.flip { animation: flip .7s ease both; transform-box: fill-box; transform-origin: center; }
@keyframes flip { from { opacity: 0; transform: rotateX(-70deg) scale(.9); }
                  to { opacity: 1; transform: none; } }

body.paused * { animation-play-state: paused !important; }
@media (prefers-reduced-motion: reduce) { * { animation-duration: .001s !important; } }
"""

JS = """
(function () {
  var stage = document.querySelector('.stage');
  var body = document.body;
  var btn = document.getElementById('play');
  var dots = Array.prototype.slice.call(document.querySelectorAll('.dot'));

  function replay() {
    // 复制一份节点，强制重启动画
    var clone = stage.cloneNode(true);
    stage.parentNode.replaceChild(clone, stage);
    stage = clone;
    bind();
    body.classList.remove('paused');
    if (btn) btn.textContent = '暂停';
    mark(0);
  }

  function bind() {
    if (!btn) return;
    btn.onclick = function () {
      var paused = body.classList.toggle('paused');
      btn.textContent = paused ? '播放' : '暂停';
    };
  }

  function mark(i) {
    dots.forEach(function (d, k) { d.classList.toggle('on', k === i); });
  }

  if (dots.length) {
    var step = Math.max(1200, Math.round(dots.length * 900));
    var idx = 0;
    setInterval(function () {
      idx = (idx + 1) % dots.length;
      mark(idx);
    }, step);
  }

  var rb = document.getElementById('replay');
  if (rb) rb.onclick = replay;

  bind();
})();
"""


def esc(text: str) -> str:
    """XML/SVG 文本转义"""
    return (
        str(text)
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def wrap_text(text: str, per_line: int) -> list[str]:
    """按视觉宽度粗略折行（中文按 1 个宽度单位，ASCII 按 0.55）"""
    lines: list[str] = []
    cur = ""
    width = 0.0
    for ch in str(text):
        w = 1.0 if ord(ch) > 0x2E80 else 0.55
        if width + w > per_line and cur:
            lines.append(cur)
            cur = ""
            width = 0.0
        cur += ch
        width += w
    if cur:
        lines.append(cur)
    return lines


def text_block(x: float, y: float, lines: list[str], cls: str = "lbl-s",
               anchor: str = "middle", lh: float = 13.0) -> str:
    if not lines:
        return ""
    out = []
    for i, ln in enumerate(lines):
        out.append(
            f'<text class="{cls}" x="{x}" y="{y + i * lh}" text-anchor="{anchor}">{esc(ln)}</text>'
        )
    return "".join(out)


# ---------------------------------------------------------------- 原型渲染器
# 每个渲染器返回 (svg, extra_css)
# 约定：SVG 坐标使用 360 宽的虚拟画布，实际显示时用 preserveAspectRatio 缩放。


def r_layers(d: dict) -> tuple[str, str]:
    items = d["items"]
    n = len(items)
    box_h = 30
    gap = 7
    top = 26
    height = top + n * box_h + (n - 1) * gap + 14
    parts = []
    for i, it in enumerate(items):
        # 上窄下宽，做成层叠金字塔
        w = 190 + i * (150 / max(1, n - 1)) if n > 1 else 240
        w = min(w, 300)
        x = 180 - w / 2
        y = top + i * (box_h + gap)
        acc = it.get("accent", i % 8)
        parts.append(
            f'<g class="drop" style="animation-delay:{0.10 + i * 0.13:.2f}s">'
            f'<rect x="{x:.1f}" y="{y}" width="{w:.1f}" height="{box_h}" rx="8" '
            f'class="c{acc}" opacity="0.92"/>'
            f'<text class="on0" x="180" y="{y + 19}" text-anchor="middle" '
            f'style="font-size:12px;font-weight:700">{esc(it["label"])}</text>'
            + (
                f'<text class="lbl-s" x="180" y="{y + 19}" text-anchor="middle" '
                f'style="fill:rgba(255,255,255,.85);font-size:10px">{esc(it["note"])}</text>'
                if False else ""
            )
            + "</g>"
        )
        if it.get("note"):
            parts.append(
                f'<text class="lbl-s" x="180" y="{y + box_h - 5}" text-anchor="middle" '
                f'style="fill:rgba(255,255,255,.88);font-size:10px">{esc(it["note"])}</text>'
            )
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_cycle(d: dict) -> tuple[str, str]:
    items = d["items"]
    n = len(items)
    cx, cy, r = 180, 132, 88
    parts = []
    parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="{r}" class="edge" style="--len:560" '
        f'pathLength="560" stroke-dasharray="560" stroke-dashoffset="560">'
        f'<animate attributeName="stroke-dashoffset" from="560" to="0" dur="1.1s" fill="freeze"/></circle>'
    )
    for i, it in enumerate(items):
        ang = -90 + i * 360 / n
        import math

        x = cx + r * math.cos(math.radians(ang))
        y = cy + r * math.sin(math.radians(ang))
        acc = it.get("accent", i % 8)
        lines = wrap_text(it["label"], 7)
        parts.append(
            f'<g class="pop" style="animation-delay:{0.45 + i * 0.16:.2f}s">'
            f'<circle cx="{x:.1f}" cy="{y:.1f}" r="27" class="c{acc}"/>'
            f'<text class="on0" x="{x:.1f}" y="{y + 4:.1f}" text-anchor="middle" '
            f'style="font-size:11.5px;font-weight:700">{esc(it["label"])}</text>'
            f"</g>"
        )
        if it.get("note"):
            nx = cx + (r + 44) * math.cos(math.radians(ang))
            ny = cy + (r + 44) * math.sin(math.radians(ang))
            anchor = "middle"
            if math.cos(math.radians(ang)) > 0.3:
                anchor = "start"
            elif math.cos(math.radians(ang)) < -0.3:
                anchor = "end"
            parts.append(
                text_block(nx, ny, wrap_text(it["note"], 9), "lbl-s", anchor, 11)
            )
    center = d.get("center", "")
    if center:
        parts.append(
            f'<circle cx="{cx}" cy="{cy}" r="40" class="card2"/>'
            f'<text class="lbl" x="{cx}" y="{cy + 5}" text-anchor="middle" '
            f'style="font-size:13px;font-weight:700">{esc(center)}</text>'
        )
    svg = (
        f'<svg viewBox="0 0 360 264" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_flow(d: dict) -> tuple[str, str]:
    steps = d["steps"]
    per_row = 4 if len(steps) > 4 else len(steps)
    rows = (len(steps) + per_row - 1) // per_row
    box_w, box_h = 74, 44
    gap_x = 22
    row_gap = 46
    top = 22
    parts = []
    idx = 0
    for r in range(rows):
        chunk = steps[r * per_row : (r + 1) * per_row]
        n = len(chunk)
        total_w = n * box_w + (n - 1) * gap_x
        start = (360 - total_w) / 2
        y = top + r * (box_h + row_gap)
        for j, st in enumerate(chunk):
            x = start + j * (box_w + gap_x)
            acc = st.get("accent", (r * per_row + j) % 8)
            parts.append(
                f'<g class="pop" style="animation-delay:{0.10 + idx * 0.14:.2f}s">'
                f'<rect x="{x:.1f}" y="{y}" width="{box_w}" height="{box_h}" rx="9" '
                f'class="c{acc}" opacity="0.9"/>'
                f'<text class="on0" x="{x + box_w / 2:.1f}" y="{y + 19}" text-anchor="middle" '
                f'style="font-size:11.5px;font-weight:700">{esc(st["label"])}</text>'
                + text_block(
                    x + box_w / 2,
                    y + 34,
                    wrap_text(st.get("note", ""), 9),
                    "lbl-s",
                    "middle",
                    11,
                ).replace('class="lbl-s"', 'style="fill:rgba(255,255,255,.9);font-size:9.5px" class="lbl-s"')
                + "</g>"
            )
            if j < n - 1:
                x1 = x + box_w + 3
                x2 = x + box_w + gap_x - 3
                yy = y + box_h / 2
                parts.append(
                    f'<path class="edge s0" d="M{x1:.1f},{yy:.1f} L{x2:.1f},{yy:.1f}" '
                    f'marker-end="url(#ah)"/>'
                )
            idx += 1
        # 行末折返提示
        if r < rows - 1:
            last_x = start + (n - 1) * (box_w + gap_x) + box_w
            yy = y + box_h / 2
            parts.append(
                f'<path class="edge" d="M{last_x:.1f},{yy:.1f} '
                f'C{last_x + 14:.1f},{yy:.1f} {last_x + 14:.1f},{yy + row_gap + 6:.1f} '
                f'{last_x - 6:.1f},{yy + row_gap + 6:.1f}" marker-end="url(#ah)"/>'
            )
    height = top + rows * box_h + (rows - 1) * row_gap + 18
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">'
        f'<defs><marker id="ah" markerWidth="7" markerHeight="7" refX="6" refY="3.5" '
        f'orient="auto"><path d="M0,0 L7,3.5 L0,7 z" fill="var(--line)"/></marker></defs>'
        + "".join(parts)
        + "</svg>"
    )
    return svg, ""


def r_compare(d: dict) -> tuple[str, str]:
    cols = d["columns"]
    n = len(cols)
    margin = 8
    gap = 9
    col_w = (360 - margin * 2 - gap * (n - 1)) / n
    top = 26
    max_items = max(len(c["items"]) for c in cols)
    body_h = max_items * 26 + 12
    height = top + 30 + body_h + 16
    parts = []
    for i, col in enumerate(cols):
        x = margin + i * (col_w + gap)
        acc = col.get("accent", i % 8)
        parts.append(
            f'<g class="rise" style="animation-delay:{0.08 + i * 0.14:.2f}s">'
            f'<rect x="{x:.1f}" y="{top}" width="{col_w:.1f}" height="{height - top - 12:.1f}" '
            f'rx="10" class="card"/>'
            f'<rect x="{x:.1f}" y="{top}" width="{col_w:.1f}" height="26" rx="10" class="c{acc}"/>'
            f'<rect x="{x:.1f}" y="{top + 16}" width="{col_w:.1f}" height="10" class="c{acc}"/>'
            f'<text class="on0" x="{x + col_w / 2:.1f}" y="{top + 18}" text-anchor="middle" '
            f'style="font-size:11.5px;font-weight:700">{esc(col["title"])}</text>'
            f"</g>"
        )
        for j, it in enumerate(col["items"]):
            yy = top + 42 + j * 26
            parts.append(
                f'<g class="fade" style="animation-delay:{0.30 + i * 0.12 + j * 0.07:.2f}s">'
                f'<circle cx="{x + 9:.1f}" cy="{yy - 4:.1f}" r="3" class="c{acc}"/>'
                + text_block(
                    x + 18,
                    yy,
                    wrap_text(it, max(6, int(col_w / 6.2))),
                    "lbl-s",
                    "start",
                    12,
                )
                + "</g>"
            )
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def _tree_layout(d: dict) -> list[dict]:
    """简单的横向树布局，返回带 x/y 的节点列表"""
    root = d["root"]
    children = d.get("children", [])
    nodes: list[dict] = []
    depth_h = [34, 58, 82]
    root_y = 34
    nodes.append({"label": root, "note": d.get("rootNote", ""), "x": 78, "y": root_y, "depth": 0})
    if not children:
        return nodes
    span = 300 / max(1, len(children))
    for i, ch in enumerate(children):
        cx = 160 + (i - (len(children) - 1) / 2) * (span if len(children) > 1 else 0)
        nodes.append(
            {"label": ch["label"], "note": ch.get("note", ""), "x": cx, "y": depth_h[1],
             "depth": 1, "parent": 0, "accent": ch.get("accent", i % 8)}
        )
        grandchildren = ch.get("children", [])
        if grandchildren:
            gspan = 90 / max(1, len(grandchildren))
            base = cx - (len(grandchildren) - 1) * gspan / 2
            for j, gc in enumerate(grandchildren):
                gx = base + j * gspan
                nodes.append(
                    {"label": gc["label"], "note": gc.get("note", ""), "x": gx, "y": depth_h[2],
                     "depth": 2, "accent": gc.get("accent", (i + j) % 8)}
                )
                nodes[-1]["parent"] = len(nodes) - 2
    return nodes


def r_tree(d: dict) -> tuple[str, str]:
    nodes = _tree_layout(d)
    parts = []
    # 先画连线
    for i, n in enumerate(nodes):
        if n["depth"] == 0:
            continue
        p = None
        for j, m in enumerate(nodes):
            if m["depth"] == n["depth"] - 1 and abs(m["x"] - n["x"]) < 120:
                p = m
                break
        if p is None:
            continue
        mx = (p["x"] + n["x"]) / 2
        parts.append(
            f'<path class="edge" d="M{p["x"]:.1f},{p["y"] + 15:.1f} '
            f'C{mx:.1f},{p["y"] + 30:.1f} {mx:.1f},{n["y"] - 15:.1f} '
            f'{n["x"]:.1f},{n["y"] - 15:.1f}"/>'
        )
    for i, n in enumerate(nodes):
        acc = n.get("accent", 0 if n["depth"] == 0 else i % 8)
        if n["depth"] == 0:
            parts.append(
                f'<g class="fade"><rect x="18" y="{n["y"] - 15:.0f}" width="120" height="30" '
                f'rx="8" class="c0"/><text class="on0" x="78" y="{n["y"] + 5:.0f}" '
                f'text-anchor="middle" style="font-size:12px;font-weight:700">'
                f'{esc(n["label"])}</text></g>'
            )
        elif n["depth"] == 1:
            parts.append(
                f'<g class="pop" style="animation-delay:{0.15 + i * 0.1:.2f}s">'
                f'<rect x="{n["x"] - 44:.1f}" y="{n["y"] - 15:.1f}" width="88" height="30" '
                f'rx="8" class="card2"/>'
                f'<text class="lbl" x="{n["x"]:.1f}" y="{n["y"] + 4:.1f}" text-anchor="middle" '
                f'style="font-size:11px">{esc(n["label"])}</text></g>'
            )
        else:
            parts.append(
                f'<g class="fade" style="animation-delay:{0.25 + i * 0.08:.2f}s">'
                f'<rect x="{n["x"] - 38:.1f}" y="{n["y"] - 13:.1f}" width="76" height="26" '
                f'rx="7" class="card"/>'
                + text_block(n["x"], n["y"] + 4, wrap_text(n["label"], 8), "lbl-s", "middle", 10)
                + "</g>"
            )
    svg = (
        '<svg viewBox="0 0 360 116" preserveAspectRatio="xMidYMid meet" '
        'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_timeline(d: dict) -> tuple[str, str]:
    ms = d["milestones"]
    y = 92
    parts = [
        f'<line x1="18" y1="{y}" x2="342" y2="{y}" class="edge"/>',
        f'<line x1="18" y1="{y}" x2="342" y2="{y}" class="edge s0" stroke-width="2.5" '
        f'style="--len:324" stroke-dasharray="324" stroke-dashoffset="324">'
        f'<animate attributeName="stroke-dashoffset" from="324" to="0" dur="1.2s" fill="freeze"/></line>',
    ]
    n = len(ms)
    for i, m in enumerate(ms):
        x = 18 + (324 * (i + 0.5) / n)
        acc = m.get("accent", i % 8)
        up = i % 2 == 0
        ty = y - 44 if up else y + 20
        parts.append(
            f'<g class="pop" style="animation-delay:{0.35 + i * 0.16:.2f}s">'
            f'<circle cx="{x:.1f}" cy="{y}" r="7" class="c{acc}"/>'
            f'<circle cx="{x:.1f}" cy="{y}" r="11" class="s{acc}" fill="none" '
            f'style="opacity:.35"><animate attributeName="r" values="11;17;11" dur="2.4s" '
            f'repeatCount="indefinite"/><animate attributeName="opacity" '
            f'values=".35;0;.35" dur="2.4s" repeatCount="indefinite"/></circle>'
            f'<line x1="{x:.1f}" y1="{y - 7 if up else y + 7}" x2="{x:.1f}" '
            f'y2="{y - 16 if up else y + 16}" class="edge"/>'
            f'<text class="lbl" x="{x:.1f}" y="{ty + (0 if up else 8):.0f}" text-anchor="middle" '
            f'style="font-size:11px">{esc(m["label"])}</text>'
            f'</g>'
        )
        if m.get("note"):
            parts.append(
                text_block(
                    x,
                    ty + (14 if up else 22),
                    wrap_text(m["note"], 10),
                    "lbl-s",
                    "middle",
                    11,
                )
            )
    svg = (
        '<svg viewBox="0 0 360 190" preserveAspectRatio="xMidYMid meet" '
        'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_bars(d: dict) -> tuple[str, str]:
    bars = d["bars"]
    unit = d.get("unit", "")
    left = 96
    bar_w = 196
    row_h = 30
    top = 18
    height = top + len(bars) * row_h + 12
    parts = []
    for i, b in enumerate(bars):
        y = top + i * row_h
        val = float(b["value"])
        mx = float(b.get("max", 100))
        ratio = max(0.0, min(1.0, val / mx if mx else 0))
        acc = b.get("accent", i % 8)
        parts.append(
            f'<text class="lbl" x="{left - 8}" y="{y + 15}" text-anchor="end" '
            f'style="font-size:11px">{esc(b["label"])}</text>'
        )
        parts.append(
            f'<rect x="{left}" y="{y + 4}" width="{bar_w}" height="16" rx="8" class="card"/>'
        )
        parts.append(
            f'<rect class="grow c{acc}" x="{left}" y="{y + 4}" width="{bar_w * ratio:.1f}" '
            f'height="16" rx="8" style="animation-delay:{0.15 + i * 0.15:.2f}s"/>'
        )
        parts.append(
            f'<text class="num" x="{left + bar_w + 6}" y="{y + 16}">'
            f'{esc(b.get("text", str(b["value"]) + unit))}</text>'
        )
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_gauge(d: dict) -> tuple[str, str]:
    value = float(d["value"])
    mx = float(d.get("max", 100))
    ratio = max(0.0, min(1.0, value / mx if mx else 0))
    cx, cy, r = 180, 118, 74
    parts = []
    # 背景弧
    parts.append(
        f'<path d="M{cx - r},{cy} A{r},{r} 0 1 1 {cx + r},{cy}" class="edge" '
        f'stroke-width="16" style="stroke-linecap:round" pathLength="100" '
        f'stroke-dasharray="100" stroke-dashoffset="0" opacity=".35"/>'
    )
    parts.append(
        f'<path d="M{cx - r},{cy} A{r},{r} 0 1 1 {cx + r},{cy}" class="s0" '
        f'stroke-width="16" style="stroke-linecap:round" pathLength="100" '
        f'stroke-dasharray="100" stroke-dashoffset="{100 - ratio * 100:.1f}">'
        f'<animate attributeName="stroke-dashoffset" from="100" to="{100 - ratio * 100:.1f}" '
        f'dur="1.2s" fill="freeze"/></path>'
    )
    parts.append(
        f'<text class="lbl" x="{cx}" y="{cy - 4}" text-anchor="middle" '
        f'style="font-size:28px;font-weight:700;fill:var(--primary)">'
        f'{esc(d.get("text", str(value)))}</text>'
    )
    parts.append(
        f'<text class="lbl-s" x="{cx}" y="{cy + 16}" text-anchor="middle">'
        f'{esc(d.get("label", ""))}</text>'
    )
    segs = d.get("segments", [])
    if segs:
        sw = 340 / len(segs)
        for i, s in enumerate(segs):
            x = 10 + i * sw
            acc = s.get("accent", i % 8)
            parts.append(
                f'<g class="fade" style="animation-delay:{0.6 + i * 0.14:.2f}s">'
                f'<rect x="{x + 3:.1f}" y="{cy + 44}" width="{sw - 6:.1f}" height="30" rx="7" '
                f'class="card"/>'
                f'<rect x="{x + 3:.1f}" y="{cy + 44}" width="4" height="30" rx="2" class="c{acc}"/>'
                + text_block(x + sw / 2, cy + 58, wrap_text(s["label"], 7), "lbl-s", "middle", 11)
                + f'<text class="num" x="{x + sw / 2:.1f}" y="{cy + 82}" text-anchor="middle">'
                f'{esc(s.get("value", ""))}</text>'
                "</g>"
            )
    svg = (
        '<svg viewBox="0 0 360 218" preserveAspectRatio="xMidYMid meet" '
        'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_matrix(d: dict) -> tuple[str, str]:
    xa = d.get("xAxis", [])
    ya = d.get("yAxis", [])
    cells = d.get("cells", [])
    left, top = 58, 34
    cw, ch = 88, 62
    cols, rows = len(xa), len(ya)
    parts = []
    for r in range(rows):
        for c in range(cols):
            x = left + c * cw
            y = top + r * ch
            acc = (r + c) % 8
            parts.append(
                f'<rect x="{x}" y="{y}" width="{cw}" height="{ch}" rx="8" class="card2" '
                f'style="opacity:.55"/>'
            )
    for cell in cells:
        x = left + cell["col"] * cw
        y = top + cell["row"] * ch
        acc = cell.get("accent", (cell["row"] + cell["col"]) % 8)
        parts.append(
            f'<g class="pop" style="animation-delay:{0.12 + (cell["row"] * cols + cell["col"]) * 0.1:.2f}s">'
            f'<rect x="{x + 3}" y="{y + 3}" width="{cw - 6}" height="{ch - 6}" rx="8" '
            f'class="c{acc}" opacity=".16"/>'
            f'<rect x="{x + 3}" y="{y + 3}" width="{cw - 6}" height="{ch - 6}" rx="8" '
            f'class="s{acc}" fill="none" style="opacity:.6"/>'
            + text_block(
                x + cw / 2,
                y + 20,
                wrap_text(cell.get("title", ""), 8),
                "lbl",
                "middle",
                12,
            )
            + text_block(
                x + cw / 2,
                y + 40,
                wrap_text("、".join(cell.get("items", [])), 11),
                "lbl-s",
                "middle",
                11,
            )
            + "</g>"
        )
    for c, lab in enumerate(xa):
        parts.append(
            f'<text class="lbl-s" x="{left + c * cw + cw / 2}" y="{top - 10}" '
            f'text-anchor="middle">{esc(lab)}</text>'
        )
    for r, lab in enumerate(ya):
        parts.append(
            f'<text class="lbl-s" x="{left - 8}" y="{top + r * ch + ch / 2 + 4}" '
            f'text-anchor="end">{esc(lab)}</text>'
        )
    if d.get("xTitle"):
        parts.append(
            f'<text class="num" x="{left + cols * cw / 2}" y="{top + rows * ch + 18}" '
            f'text-anchor="middle">{esc(d["xTitle"])}</text>'
        )
    if d.get("yTitle"):
        parts.append(
            f'<text class="num" x="14" y="{top + rows * ch / 2}" text-anchor="middle" '
            f'transform="rotate(-90 14 {top + rows * ch / 2})">{esc(d["yTitle"])}</text>'
        )
    height = top + rows * ch + 30
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_network(d: dict) -> tuple[str, str]:
    nodes = d["nodes"]
    edges = d.get("edges", [])
    pos = {}
    parts = []
    for nd in nodes:
        pos[nd["id"]] = (26 + nd["x"] * 308, 24 + nd["y"] * 180)
    for e in edges:
        a, b = e[0], e[1]
        if a not in pos or b not in pos:
            continue
        x1, y1 = pos[a]
        x2, y2 = pos[b]
        parts.append(
            f'<path class="edge" d="M{x1:.1f},{y1:.1f} L{x2:.1f},{y2:.1f}"/>'
        )
        if len(e) > 2 and e[2]:
            parts.append(
                f'<text class="num" x="{(x1 + x2) / 2:.1f}" y="{(y1 + y2) / 2 - 5:.1f}" '
                f'text-anchor="middle">{esc(e[2])}</text>'
            )
    for i, nd in enumerate(nodes):
        x, y = pos[nd["id"]]
        acc = nd.get("accent", i % 8)
        w = 66
        parts.append(
            f'<g class="pop" style="animation-delay:{0.2 + i * 0.13:.2f}s">'
            f'<rect x="{x - w / 2:.1f}" y="{y - 15:.1f}" width="{w}" height="30" rx="8" '
            f'class="c{acc}"/>'
            f'<text class="on0" x="{x:.1f}" y="{y + 4:.1f}" text-anchor="middle" '
            f'style="font-size:11px;font-weight:600">{esc(nd["label"])}</text>'
            f"</g>"
        )
        if nd.get("note"):
            parts.append(
                text_block(x, y + 28, wrap_text(nd["note"], 12), "lbl-s", "middle", 11)
            )
    svg = (
        '<svg viewBox="0 0 360 228" preserveAspectRatio="xMidYMid meet" '
        'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_formula(d: dict) -> tuple[str, str]:
    formula = d["formula"]
    terms = d.get("terms", [])
    note = d.get("note", "")
    parts = []
    parts.append(
        f'<text class="lbl" x="180" y="34" text-anchor="middle" '
        f'style="font-size:19px;font-weight:700;fill:var(--primary)">{esc(formula)}</text>'
    )
    n = len(terms)
    cols = 2 if n > 2 else n
    rows = (n + cols - 1) // cols if n else 0
    cw, ch = 150, 44
    gap = 12
    top = 56
    for i, t in enumerate(terms):
        r, c = divmod(i, cols)
        x = 180 - (cols * cw + (cols - 1) * gap) / 2 + c * (cw + gap)
        y = top + r * (ch + gap)
        acc = i % 8
        parts.append(
            f'<g class="pop" style="animation-delay:{0.15 + i * 0.13:.2f}s">'
            f'<rect x="{x:.1f}" y="{y}" width="{cw}" height="{ch}" rx="9" class="card"/>'
            f'<rect x="{x:.1f}" y="{y}" width="4" height="{ch}" rx="2" class="c{acc}"/>'
            f'<text class="lbl" x="{x + 14:.1f}" y="{y + 18:.0f}" '
            f'style="font-size:12.5px;fill:var(--c{acc})">{esc(t["symbol"])}</text>'
            + text_block(x + 14, y + 33, wrap_text(t["meaning"], 15), "lbl-s", "start", 11)
            + "</g>"
        )
    height = top + rows * (ch + gap) + 6
    if note:
        parts.append(
            text_block(180, height + 12, wrap_text(note, 30), "lbl-s", "middle", 12)
        )
        height += 22
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_map(d: dict) -> tuple[str, str]:
    center = d["center"]
    spokes = d["spokes"]
    cx, cy = 180, 132
    parts = []
    positions = []
    n = len(spokes)
    for i, s in enumerate(spokes):
        ang = -90 + i * 360 / n
        import math

        rr = 96 if len(spokes) <= 6 else 88
        x = cx + rr * math.cos(math.radians(ang))
        y = cy + rr * math.sin(math.radians(ang))
        positions.append((x, y))
        acc = s.get("accent", i % 8)
        parts.append(
            f'<line x1="{cx}" y1="{cy}" x2="{x:.1f}" y2="{y:.1f}" class="edge s{acc}" '
            f'style="opacity:.55"/>'
        )
    parts.append(
        f'<circle cx="{cx}" cy="{cy}" r="40" class="c0"/>'
        f'<text class="on0" x="{cx}" y="{cy + 5}" text-anchor="middle" '
        f'style="font-size:12.5px;font-weight:700">{esc(center)}</text>'
    )
    for i, s in enumerate(spokes):
        x, y = positions[i]
        acc = s.get("accent", i % 8)
        lines = wrap_text(s["label"], 7)
        w = max(58, 8 + max(len(x) for x in lines) * 11)
        parts.append(
            f'<g class="pop" style="animation-delay:{0.2 + i * 0.12:.2f}s">'
            f'<rect x="{x - w / 2:.1f}" y="{y - 13:.1f}" width="{w:.1f}" height="26" rx="8" '
            f'class="card2"/>'
            f'<text class="lbl" x="{x:.1f}" y="{y + 4:.1f}" text-anchor="middle" '
            f'style="font-size:11px">{esc(s["label"])}</text>'
            f"</g>"
        )
        if s.get("note"):
            anchor = "middle"
            dx = 0
            dy = 26
            if x > cx + 40:
                anchor = "start"
                dx = w / 2 + 4
                dy = 4
            elif x < cx - 40:
                anchor = "end"
                dx = -w / 2 - 4
                dy = 4
            parts.append(
                text_block(x + dx, y + dy, wrap_text(s["note"], 8), "lbl-s", anchor, 11)
            )
    svg = (
        '<svg viewBox="0 0 360 264" preserveAspectRatio="xMidYMid meet" '
        'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


def r_sequence(d: dict) -> tuple[str, str]:
    actors = d["actors"]
    steps = d.get("steps", [])
    n = len(actors)
    col_w = 300 / n
    top = 26
    lane_h = 34
    height = top + len(steps) * lane_h + 34
    parts = []
    for i, a in enumerate(actors):
        x = 30 + i * col_w + col_w / 2
        parts.append(
            f'<g class="drop" style="animation-delay:{0.05 + i * 0.12:.2f}s">'
            f'<rect x="{x - 44:.1f}" y="6" width="88" height="26" rx="8" class="c{i % 8}"/>'
            f'<text class="on0" x="{x:.1f}" y="23" text-anchor="middle" '
            f'style="font-size:11px;font-weight:700">{esc(a)}</text>'
            f'<line x1="{x:.1f}" y1="34" x2="{x:.1f}" y2="{height - 24}" class="edge" '
            f'style="stroke-dasharray:4 4"/></g>'
        )
    for i, st in enumerate(steps):
        y = top + 16 + i * lane_h
        a = st.get("from", 0)
        b = st.get("to", 0)
        x1 = 30 + a * col_w + col_w / 2
        x2 = 30 + b * col_w + col_w / 2
        if x1 == x2:
            x2 = x1 + 62
        parts.append(
            f'<g class="fade" style="animation-delay:{0.25 + i * 0.16:.2f}s">'
            f'<line x1="{x1:.1f}" y1="{y}" x2="{x2:.1f}" y2="{y}" class="edge s0" '
            f'style="stroke-width:2"/>'
            f'<circle cx="{x1:.1f}" cy="{y}" r="3.5" class="c0"/>'
            + text_block(
                (x1 + x2) / 2, y - 6, wrap_text(st.get("label", ""), 12), "lbl-s", "middle", 11
            )
            + "</g>"
        )
    svg = (
        f'<svg viewBox="0 0 360 {height:.0f}" preserveAspectRatio="xMidYMid meet" '
        f'xmlns="http://www.w3.org/2000/svg">' + "".join(parts) + "</svg>"
    )
    return svg, ""


RENDERERS = {
    "layers": r_layers,
    "cycle": r_cycle,
    "flow": r_flow,
    "compare": r_compare,
    "tree": r_tree,
    "timeline": r_timeline,
    "bars": r_bars,
    "gauge": r_gauge,
    "matrix": r_matrix,
    "network": r_network,
    "formula": r_formula,
    "map": r_map,
    "sequence": r_sequence,
}


# ---------------------------------------------------------------- HTML 组装


def build_html(spec: dict, svg: str, theme: str, steps: int) -> str:
    palette = THEMES[theme]
    css_vars = "\n".join(f"  {k}: {v};" for k, v in palette.items())
    dots = "".join('<span class="dot"></span>' for _ in range(max(1, steps)))
    title = spec.get("title", "")
    caption = spec.get("caption", "")
    head = (
        "<!doctype html>\n<html lang=\"zh-CN\"><head><meta charset=\"utf-8\">\n"
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">\n"
        "<title>" + esc(title) + "</title>\n<style>\n:root {\n" + css_vars + "\n}\n"
        + BASE_CSS + ANIM_CSS + "\n</style></head>\n<body>\n"
    )
    body = (
        '<div class="wrap">'
        f'<div class="head"><div class="title">{esc(title)}</div>'
        f'<div class="cap">{esc(caption)}</div></div>'
        f'<div class="stage">{svg}</div>'
        '<div class="bar"><div class="steps">' + dots + '</div><div class="sp"></div>'
        '<span class="btn" id="play">暂停</span>'
        '<span class="btn" id="replay">重播</span></div>'
        "</div>\n"
    )
    return head + body + "<script>" + JS + "</script>\n</body></html>\n"


def build(spec: dict) -> dict[str, str]:
    arch = spec["archetype"]
    if arch not in RENDERERS:
        raise KeyError(f"未知原型: {arch}")
    svg, extra = RENDERERS[arch](spec)
    steps = int(spec.get("animSteps", 0) or 0)
    if steps <= 0:
        steps = {"layers": 4, "cycle": 4, "flow": 4, "compare": 4, "tree": 3, "timeline": 4,
                 "bars": 4, "gauge": 3, "matrix": 4, "network": 4, "formula": 3,
                 "map": 5, "sequence": 4}.get(arch, 4)
    out = {}
    for theme in ("light", "dark"):
        html = build_html(spec, svg, theme, steps)
        if extra:
            html = html.replace("</style>", extra + "\n</style>")
        out[theme] = html
    return out


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description="生成知识点可视化 HTML")
    ap.add_argument("--check", action="store_true", help="只校验数据，不写文件")
    args = ap.parse_args(argv[1:])

    data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
    visuals = data["visuals"]

    seen_ids = set()
    problems = []
    for v in visuals:
        vid = v["id"]
        if vid in seen_ids:
            problems.append(f"重复 id: {vid}")
        seen_ids.add(vid)
        if v["archetype"] not in RENDERERS:
            problems.append(f"{vid}: 未知原型 {v['archetype']}")
        for key in ("examId", "chapter", "title", "caption"):
            if not v.get(key):
                problems.append(f"{vid}: 缺少字段 {key}")

    if problems:
        for p in problems:
            print("[FAIL]", p)
        return 1

    if args.check:
        print(f"数据校验通过：{len(visuals)} 张图解")
        return 0

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    index = []
    for v in visuals:
        pages = build(v)
        out_dir = OUT_DIR / v["examId"]
        out_dir.mkdir(parents=True, exist_ok=True)
        (out_dir / f"{v['id']}.html").write_text(pages["light"], encoding="utf-8")
        (out_dir / f"{v['id']}.dark.html").write_text(pages["dark"], encoding="utf-8")
        index.append(
            {
                "id": v["id"],
                "examId": v["examId"],
                "chapter": v["chapter"],
                "title": v["title"],
                "archetype": v["archetype"],
                "caption": v.get("caption", ""),
                "tags": v.get("tags", []),
                "file": f"{v['examId']}/{v['id']}.html",
                "darkFile": f"{v['examId']}/{v['id']}.dark.html",
            }
        )

    manifest = {"version": 1, "count": len(index), "visuals": index}
    (OUT_DIR / "index.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=1), encoding="utf-8"
    )
    print(f"已生成 {len(index)} 张图解 -> {OUT_DIR}")
    by_arch: dict[str, int] = {}
    for v in visuals:
        by_arch[v["archetype"]] = by_arch.get(v["archetype"], 0) + 1
    for k, n in sorted(by_arch.items(), key=lambda kv: -kv[1]):
        print(f"  {k:10s} {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
