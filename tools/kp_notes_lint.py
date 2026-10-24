#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""列出超出长度区间（默认 40~70 字）的知识点精讲，供人工改写。

用法：
    python tools/kp_notes_lint.py                 # 两科都查
    python tools/kp_notes_lint.py system-integration
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
BANK = ROOT / "question-bank"
EXAMS = ["system-integration", "multimedia-designer"]
MIN_LEN = 40
MAX_LEN = 70


def main() -> int:
    targets = sys.argv[1:] or EXAMS
    for exam in targets:
        parts_dir = BANK / exam / "kp_parts"
        if parts_dir.exists():
            for f in sorted(parts_dir.glob("*.out*.json")):
                try:
                    data = json.loads(f.read_text(encoding="utf-8"))
                except Exception:
                    continue
                items = data if isinstance(data, list) else data.get("notes", [])
                bad = []
                for it in items:
                    p = str(it.get("point", "")).strip()
                    n = str(it.get("note", "")).strip()
                    if not n:
                        bad.append((p, n, len(n)))
                    elif len(n) < MIN_LEN or len(n) > MAX_LEN:
                        bad.append((p, n, len(n)))
                if bad:
                    print(f"=== {f.name} : {len(bad)} 条需修 ===")
                    for p, n, ln in bad:
                        print(f"  [{ln}] {p}\n      {n}")
        # 合并产物也要体检：分片可能已修好，但合并文件被别处改过
        merged = BANK / exam / "kp_notes.json"
        if merged.exists():
            notes = json.loads(merged.read_text(encoding="utf-8")).get("notes", {})
            bad = [(p, str(v), len(str(v))) for p, v in notes.items()
                   if not str(v).strip() or len(str(v)) < MIN_LEN or len(str(v)) > MAX_LEN]
            if bad:
                print(f"=== {exam}/kp_notes.json（合并产物）: {len(bad)} 条需修 ===")
                for p, n, ln in bad:
                    print(f"  [{ln}] {p}\n      {n}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
