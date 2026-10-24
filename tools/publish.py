#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""一键出包：合并 AI 解析 + 合并知识点资料 → 重建索引 → 校验 → 汇总。

用法：
    python tools/publish.py
"""
from __future__ import annotations

import json
import pathlib
import subprocess
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
TOOLS = ROOT / "tools"
BANK_DIR = ROOT / "question-bank"


def run(script: str, *args: str) -> int:
    print(f"\n>>> python tools/{script} {' '.join(args)}")
    return subprocess.run([sys.executable, str(TOOLS / script), *args], cwd=str(ROOT)).returncode


def main() -> int:
    print("=" * 64)
    print("1/4 合并 AI 深度解析")
    print("=" * 64)
    run("ai_pipeline.py", "merge")

    print("\n" + "=" * 64)
    print("2/4 合并知识点精讲资料")
    print("=" * 64)
    run("material_pipeline.py", "merge")

    print("\n" + "=" * 64)
    print("3/4 重建索引与内置种子")
    print("=" * 64)
    run("build_index.py")

    print("\n" + "=" * 64)
    print("4/4 校验题库")
    print("=" * 64)
    rc = run("validate_bank.py", "--all")
    if rc != 0:
        print("\n题库校验未通过，请检查。")
        return rc

    print("\n" + "=" * 64)
    print("汇总")
    print("=" * 64)
    total_q = 0
    total_ai = 0
    for exam in ("system-integration", "multimedia-designer"):
        bank_path = BANK_DIR / exam / "v2.json"
        if not bank_path.exists():
            continue
        bank = json.loads(bank_path.read_text(encoding="utf-8"))
        qs = bank["questions"]
        ai = sum(1 for q in qs if q.get("aiExplanation"))
        total_q += len(qs)
        total_ai += ai
        mat_path = BANK_DIR / exam / "materials.json"
        mat = json.loads(mat_path.read_text(encoding="utf-8")) if mat_path.exists() else {"chapters": []}
        print(f"[{exam}] {len(qs)} 题 | AI 解析 {ai} 题 | 精讲资料 {len(mat.get('chapters', []))} 章")
    print(f"合计：{total_q} 题，其中 {total_ai} 题带 AI 深度解析")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
