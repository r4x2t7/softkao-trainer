#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为未完成章节生成补写任务书（<chunk>.missing.in.json）。"""
import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"

for exam in ("system-integration", "multimedia-designer"):
    parts_dir = BANK / exam / "material_parts"
    for inp in sorted(parts_dir.glob("*.in.json")):
        if ".missing" in inp.name:
            continue
        name = inp.name.replace(".in.json", "")
        want = [c["chapter"] for c in json.loads(inp.read_text(encoding="utf-8"))["chapters"]]
        out = parts_dir / f"{name}.out.json"
        got = []
        if out.exists():
            got = [c["chapter"] for c in json.loads(out.read_text(encoding="utf-8")).get("chapters", [])]
        missing = [c for c in want if c not in got]
        if not missing:
            continue
        src = json.loads(inp.read_text(encoding="utf-8"))
        keep = [c for c in src["chapters"] if c["chapter"] in missing]
        path = parts_dir / f"{name}.missing.in.json"
        path.write_text(
            json.dumps({"chunk": name + "-missing", "examId": exam, "examName": src["examName"], "chapters": keep},
                       ensure_ascii=False, indent=1),
            encoding="utf-8",
        )
        print(f"{exam} / {name}: 缺 {len(missing)} 章 -> {path.name}  ({'、'.join(missing)})")
