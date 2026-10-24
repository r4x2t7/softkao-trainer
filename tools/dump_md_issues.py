# -*- coding: utf-8 -*-
"""只导出多媒体被标记存疑的题目（题干/选项/答案/解析）。"""
import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(r"E:\ai coding\pojie\softkao-trainer")
BANK = ROOT / "question-bank" / "multimedia-designer"
issues = json.loads((BANK / "ai_issues.json").read_text(encoding="utf-8"))
bank = json.loads((BANK / "v2.json").read_text(encoding="utf-8"))
by_id = {q["id"]: q for q in bank["questions"]}
for it in issues:
    q = by_id.get(it["id"])
    if not q:
        continue
    print("=" * 70)
    print(f"[{q['id']}] {q['type']} | {q['chapter']} | 难度{q['difficulty']}")
    print("题干:", q["stem"])
    for o in q.get("options") or []:
        print(f"   {o['key']}. {o['text']}")
    print("答案:", "".join(q["answer"]))
    print("解析:", q["explanation"])
