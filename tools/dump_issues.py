# -*- coding: utf-8 -*-
"""导出被 AI 标记存疑的题目全文，供人工逐条修订。"""
import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(r"E:\ai coding\pojie\softkao-trainer")
BANK = ROOT / "question-bank"

for exam in ("system-integration", "multimedia-designer"):
    issues_path = BANK / exam / "ai_issues.json"
    if not issues_path.exists():
        continue
    issues = json.loads(issues_path.read_text(encoding="utf-8"))
    if not issues:
        continue
    bank = json.loads((BANK / exam / "v2.json").read_text(encoding="utf-8"))
    by_id = {q["id"]: q for q in bank["questions"]}
    print("#" * 78)
    print("# " + exam)
    print("#" * 78)
    for it in issues:
        q = by_id.get(it["id"])
        if not q:
            continue
        print("\n" + "=" * 78)
        print(f"[{q['id']}] {q['type']} | {q['chapter']} | {q['knowledgePoint']} | 难度{q['difficulty']}")
        if q.get("caseStudy"):
            print("案例背景:", q["caseStudy"][:200])
        print("题干:", q["stem"])
        for o in q.get("options") or []:
            print(f"   {o['key']}. {o['text']}")
        print("答案:", "".join(q["answer"]))
        print("原解析:", q["explanation"])
        print("来源:", q.get("source", ""))
