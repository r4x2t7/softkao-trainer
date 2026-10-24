#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查 assets/visuals/index.json 与题库章节的匹配情况。

用法：
    python tools/check_visuals.py
"""
from __future__ import annotations

import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
VIS = ROOT / "app" / "src" / "main" / "assets" / "visuals"
BANK = ROOT / "question-bank"


def main() -> int:
    index = json.loads((VIS / "index.json").read_text(encoding="utf-8"))
    visuals = index.get("visuals", [])
    print(f"index.json 声明 {index.get('count')} 条，实际 {len(visuals)} 条")

    missing_file = []
    missing_dark = []
    for v in visuals:
        if not (VIS / v["file"]).exists():
            missing_file.append(v["file"])
        if v.get("darkFile") and not (VIS / v["darkFile"]).exists():
            missing_dark.append(v["darkFile"])
    print(f"亮色文件缺失 {len(missing_file)}，暗色文件缺失 {len(missing_dark)}")
    for f in missing_file[:10]:
        print(f"    缺: {f}")

    # 章节名与题库章节的对应情况（App 用「包含即算」宽松匹配）
    chapters = {}
    for exam_dir in sorted(p for p in BANK.iterdir() if p.is_dir()):
        files = sorted(exam_dir.glob("v*.json"))
        if not files:
            continue
        data = json.loads(files[-1].read_text(encoding="utf-8"))
        chapters[exam_dir.name] = {q.get("chapter", "") for q in data.get("questions", [])}

    for exam, chs in chapters.items():
        vs = [v for v in visuals if v["examId"] == exam]
        hit = 0
        for v in vs:
            c = v.get("chapter", "")
            if c in chs or any(c in x or x in c for x in chs):
                hit += 1
        print(f"[{exam}] 图解 {len(vs)} 张，其中 {hit} 张能匹配到题库章节")
        unmatched = [
            v["chapter"]
            for v in vs
            if not (
                v.get("chapter", "") in chs
                or any(v.get("chapter", "") in x or x in v.get("chapter", "") for x in chs)
            )
        ]
        for c in sorted(set(unmatched)):
            print(f"    未匹配章节: {c}")

    # 章节素材目录里多出来的 HTML（索引里没登记的）
    on_disk = {
        p.relative_to(VIS).as_posix()
        for p in VIS.rglob("*.html")
        if p.name != "index.json"
    }
    listed = {v["file"] for v in visuals} | {v["darkFile"] for v in visuals if v.get("darkFile")}
    orphan = sorted(on_disk - listed)
    print(f"磁盘上有但索引未登记的 HTML：{len(orphan)}")
    for f in orphan[:10]:
        print(f"    孤儿: {f}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
