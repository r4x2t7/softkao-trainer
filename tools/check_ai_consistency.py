#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查「答案」与「AI 深度解析首句」是否自洽。

解析开头通常会写「答案A」「正确选项为A、C、D」「结论：答案ABC」，
如果这里的选项集合和 question.answer 不一致，说明改题时漏改了解析，
用户会看到「选了B，解析却说A」这种自相矛盾。

用法：
    python tools/check_ai_consistency.py           # 有冲突则返回 1
"""
from __future__ import annotations

import json
import pathlib
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"
EXAMS = ["system-integration", "multimedia-designer"]

# 解析开头的选项集合写法：答案A / 正确选项为A、C、D / 正确答案是ABC
# 分隔符只收顿号与半角逗号：全角逗号常用于「答案D，EAC为…」这种句子分隔，不能当选项分隔
SET_RE = re.compile(r"^(?:[^。]{0,12}?)(答案|正确选项是|正确选项为|正确答案是|结论)\s*[:：]?\s*([A-E]+(?:\s*[、,和及／/]\s*[A-E]+)*)")
LOOSE_RE = re.compile(r"^(?:答案|正确选项是|正确选项为|正确答案是)\s*([A-E]+(?:\s*[、,和及／/]\s*[A-E]+)*)")


def normalize(s: str) -> str:
    return "".join(sorted(set(re.findall(r"[A-E]", s))))


def main() -> int:
    total = 0
    bad: list[str] = []
    for exam in EXAMS:
        p = BANK / exam / "v2.json"
        if not p.exists():
            continue
        bank = json.loads(p.read_text(encoding="utf-8"))
        for q in bank.get("questions", []):
            ai = (q.get("aiExplanation") or "").strip()
            ans = q.get("answer") or []
            if not ai or not ans:
                continue
            total += 1
            m = SET_RE.match(ai) or LOOSE_RE.match(ai)
            if not m:
                continue
            claimed = normalize(m.group(2))
            real = normalize("".join(ans))
            if claimed and claimed != real:
                bad.append(f"{exam} {q.get('id')}：答案={real}，解析首句写的是 {claimed}")
    print(f"带 AI 解析的题目 {total} 道，答案与解析首句冲突 {len(bad)} 道")
    for line in bad[:30]:
        print("  [冲突] " + line)
    if len(bad) > 30:
        print(f"  ……另有 {len(bad) - 30} 条")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
