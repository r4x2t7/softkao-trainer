#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""抽取题库与知识点精讲里「带硬数字 / 法条术语」的断言，供人工核对。

只做筛选，不做判断。输出按主题聚类，方便对着权威来源逐条核。

用法：
    python tools/fact_audit.py                 # 全部
    python tools/fact_audit.py 数字            # 只看含数字的
    python tools/fact_audit.py 法律            # 只看含法律术语的
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
from collections import defaultdict

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"

# 法律/标准类关键词：这些词一出现，整句就带可核对的事实
LAW_TERMS = [
    "著作权", "专利", "商标", "著作权法", "专利法", "商标法", "反不正当竞争", "反垄断",
    "招标投标", "招标", "投标", "评标", "开标", "中标", "政府采购", "竞争性谈判",
    "标准化法", "国家标准", "行业标准", "地方标准", "团体标准", "企业标准",
    "等级保护", "等保", "网络安全法", "数据安全法", "个人信息保护法", "密码法",
    "仲裁", "诉讼时效", "民法典", "合同法", "公司法", "安全生产法", "会计法", "审计",
    "保密", "档案", "会计", "审计", "统计法", "计量",
    "ISO", "GB/T", "GB ", "IEEE", "ITU", "ISO/IEC",
]

NUM_RE = re.compile(r"\d")


def collect_notes() -> list[tuple[str, str]]:
    out = []
    for exam in ("system-integration", "multimedia-designer"):
        p = BANK / exam / "kp_notes.json"
        if not p.exists():
            continue
        notes = json.loads(p.read_text(encoding="utf-8")).get("notes", {})
        for point, note in notes.items():
            out.append((f"[{exam} 精讲] {point}", note))
    return out


def collect_questions() -> list[tuple[str, str]]:
    out = []
    for exam in ("system-integration", "multimedia-designer"):
        p = BANK / exam / "v2.json"
        if not p.exists():
            continue
        bank = json.loads(p.read_text(encoding="utf-8"))
        for q in bank.get("questions", []):
            bits = [q.get("stem", "")]
            bits += [o.get("text", "") for o in q.get("options", [])]
            bits.append(q.get("explanation", ""))
            if q.get("aiExplanation"):
                bits.append(q["aiExplanation"])
            text = " ".join(b for b in bits if b)
            out.append((f"[{exam} 题 {q.get('id')}] {q.get('knowledgePoint','')}", text))
    return out


def main() -> int:
    mode = sys.argv[1] if len(sys.argv) > 1 else "all"
    items = collect_notes() + collect_questions()
    buckets: dict[str, list[str]] = defaultdict(list)

    for label, text in items:
        if not text:
            continue
        hit_law = [t for t in LAW_TERMS if t in text]
        has_num = bool(NUM_RE.search(text))
        if mode == "数字":
            if not has_num:
                continue
        elif mode == "法律":
            if not hit_law:
                continue
        elif mode != "all":
            if mode not in text:
                continue
        # 归到最具体的法律术语
        key = hit_law[0] if hit_law else ("数字" if has_num else "")
        if not key:
            continue
        buckets[key].append(f"{label}\n    {text.strip()}")

    for key in sorted(buckets, key=lambda k: -len(buckets[k])):
        print(f"\n===== {key}（{len(buckets[key])} 条）=====")
        for line in buckets[key]:
            print("  " + line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
