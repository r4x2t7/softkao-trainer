#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 id 或关键词打印题目的完整内容（题干/选项/答案/解析/AI解析）。

用法：
    python tools/show_questions.py si-1198
    python tools/show_questions.py --kw 投标保证金
    python tools/show_questions.py --kw 投标保证金 --exam system-integration
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


def fields(q: dict) -> str:
    out = []
    out.append(f"id={q.get('id')}  type={q.get('type')}  chapter={q.get('chapter')}  kp={q.get('knowledgePoint')}")
    out.append(f"answer={q.get('answer')}")
    out.append(f"stem: {q.get('stem')}")
    for o in q.get("options", []) or []:
        out.append(f"   {o.get('key')}. {o.get('text')}")
    if q.get("caseStudy"):
        out.append(f"case: {q.get('caseStudy')}")
    for sq in q.get("subQuestions", []) or []:
        out.append(f"   sub[{sq.get('q')}]: {sq.get('answer')}")
    out.append(f"explanation: {q.get('explanation')}")
    if q.get("aiExplanation"):
        out.append(f"aiExplanation: {q.get('aiExplanation')}")
    if q.get("source"):
        out.append(f"source: {q.get('source')}")
    return "\n".join(out)


def main() -> int:
    args = sys.argv[1:]
    ids: list[str] = []
    kw = None
    exam = None
    only = None
    VALUE_FLAGS = {"--kw", "--exam", "--fields"}
    i = 0
    while i < len(args):
        a = args[i]
        if a in VALUE_FLAGS and i + 1 < len(args):
            val = args[i + 1]
            if a == "--kw":
                kw = val
                # @file 形式：从文件读关键词，避免 PowerShell 传中文参数时被编码搞乱
                if kw.startswith("@"):
                    kw = pathlib.Path(kw[1:]).read_text(encoding="utf-8").strip()
            elif a == "--exam":
                exam = val
            else:
                only = set(val.split(","))
            i += 2
            continue
        if not a.startswith("--"):
            ids.append(a)
        i += 1

    for exam_id in ([exam] if exam else EXAMS):
        p = BANK / exam_id / "v2.json"
        if not p.exists():
            continue
        bank = json.loads(p.read_text(encoding="utf-8"))
        for q in bank.get("questions", []):
            if ids and q.get("id") not in ids:
                continue
            if kw:
                blob = json.dumps(q, ensure_ascii=False)
                if kw not in blob:
                    continue
            if only:
                print("=" * 90)
                print(f"[{q.get('id')}] {q.get('answer')}")
                for f in ("stem", "explanation", "aiExplanation", "source"):
                    if f in only and q.get(f):
                        print(f"--{f}--\n{q[f]}")
                if "options" in only and q.get("options"):
                    for o in q["options"]:
                        print(f"  {o.get('key')}. {o.get('text')}")
                continue
            print("=" * 100)
            print(fields(q))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
