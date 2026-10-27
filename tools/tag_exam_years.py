#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""给近三年真题对应的题目打「年份真题」标签。

标注依据（两类，都可追溯）：
1. source 字段：题目来源写明「改编自20XX年…真题」的，自动提取年份；
2. 人工核对的实质对应（MANUAL）：题目与近年真题回忆版考点/问法实质相同
   （依据 docs/research/ 下真题调研与 2024-2026 回忆版文本核对）。

标签形式：「2024年真题」「2025年真题」「2026年真题」，追加到 tags，不改动其他标签。
重复执行安全（幂等）。

用法：
    python tools/tag_exam_years.py            # 应用标注
    python tools/tag_exam_years.py --dry-run  # 只看会标哪些
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

ROOT = Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"
YEAR_TAG_RE = re.compile(r"^20\d{2}年真题$")
SOURCE_YEAR_RE = re.compile(r"20\d{2}")

# 人工核对的实质对应：题 id → 考过的年份。
# 依据：2026 上半年综合知识第一批回忆（范围蔓延/控制图）、2026 上半年应用技术
# 第一批（团队冲突与士气）、2025 年第 2 批案例（储备、合同类型）、2024 年第一批
# 案例（挣值绩效、沟通与干系人）。见 build/research 下的回忆版笔记与 docs/research/。
MANUAL: dict[str, list[str]] = {
    # 挣值综合计算（案例组 si-case2-01）：source 2025 第3批 + 2024 第一批案例四同为挣值绩效
    "si-1371": ["2024", "2025"],
    "si-1372": ["2024", "2025"],
    "si-1373": ["2024", "2025"],
    # 2026 上半年综合知识第一批
    "si-1444": ["2026"],  # 控制图监测过程稳定 = 2026 综合第 15 题
    "si-1460": ["2026"],  # 范围蔓延定义 = 2026 综合第 11 题
    # 2026 上半年应用技术第一批（团队冲突/激励）
    "si-1409": ["2026"],
    "si-1410": ["2026"],
    # 2025 年第 2 批案例（储备计入成本基准、合同类型选择）
    "si-1446": ["2025"],
    "si-1448": ["2025"],
    # 2024 年第一批案例三（沟通与干系人找茬）+ source 2025
    "si-1397": ["2024", "2025"],
    "si-1398": ["2024", "2025"],
    "si-1399": ["2024", "2025"],
    "si-1400": ["2024", "2025"],
}


def years_for(q: dict) -> list[str]:
    years: set[str] = set()
    src = str(q.get("source", "") or "")
    if "真题" in src:
        years.update(SOURCE_YEAR_RE.findall(src))
    years.update(MANUAL.get(q.get("id", ""), []))
    # 只保留近三年范围（用户需求：今年/去年/前年）
    return sorted(y for y in years if y in {"2024", "2025", "2026"})


def main() -> int:
    dry = "--dry-run" in sys.argv
    tagged = 0
    for parts in sorted(BANK.glob("*/parts/*.json")):
        data = json.loads(parts.read_text(encoding="utf-8"))
        changed = False
        for q in data.get("questions", []):
            years = years_for(q)
            if not years:
                continue
            tags = [str(t) for t in q.get("tags", []) if not YEAR_TAG_RE.match(str(t))]
            want = [f"{y}年真题" for y in years]
            new_tags = want + tags
            if new_tags != [str(t) for t in q.get("tags", [])]:
                print(f"  {q['id']}: {','.join(want)}")
                q["tags"] = new_tags
                changed = True
                tagged += 1
        if changed and not dry:
            parts.write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
            )
    print(f"{'[dry-run] ' if dry else ''}共标注 {tagged} 道题")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
