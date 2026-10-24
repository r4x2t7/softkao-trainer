# -*- coding: utf-8 -*-
"""列出所有"题干近似但答案不同"的题组，供人工复核。"""
import importlib.util
import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(r"E:\ai coding\pojie\softkao-trainer")
spec = importlib.util.spec_from_file_location("mb", ROOT / "tools" / "merge_banks.py")
mb = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mb)

for exam_id, exam_name in [("system-integration", "系统集成"), ("multimedia-designer", "多媒体")]:
    bank, stats = mb.merge(exam_id, exam_name)
    conflicts = stats.get("conflicts", [])
    if not conflicts:
        continue
    print("=" * 70)
    print(f"{exam_name}：{len(conflicts)} 组疑似冲突")
    by_id = {q["id"]: q for q in bank["questions"]}
    for line in conflicts:
        print("  *", line)
