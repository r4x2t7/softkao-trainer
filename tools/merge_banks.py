#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""合并分片题库 → 生成 v2.json，并输出分布报告 + 质量门槛检查。

用法：
    python tools/merge_banks.py                 # 合并两个考试
    python tools/merge_banks.py system-integration
"""
from __future__ import annotations

import json
import re
import sys
import unicodedata
from collections import Counter
from pathlib import Path

# Windows 控制台默认 GBK，打印 ✓/✗ 会抛 UnicodeEncodeError，这里统一走 utf-8
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

ROOT = Path(__file__).resolve().parent.parent
BANK_DIR = ROOT / "question-bank"
TARGET_VERSION = 2
MIN_TOTAL = 2000
MIN_PER_EXAM = 800
MIN_PER_CHAPTER = 8
MAX_CHAPTER_SHARE = 0.15

# 历史题目（v1 种子）的章节名与现行规范不一致，合并时统一改名，避免同一考点被拆成两块
CHAPTER_ALIASES = {
    "信息化与标准化法规": "法律法规与标准规范",
    "数字视频与压缩编码": "数字视频",
    "多媒体项目实施": "多媒体项目实施",
}


def norm_stem(stem: str) -> str:
    """题干归一化：去掉空白与标点，用于跨分片查重"""
    s = unicodedata.normalize("NFKC", stem or "")
    s = re.sub(r"[\s\u3000]+", "", s)
    s = re.sub(r"[，。、；：？！,.;:?!（）()【】\[\]“”\"'‘’《》<>—\-…·]+", "", s)
    return s


def load_parts(exam_id: str) -> list[dict]:
    parts_dir = BANK_DIR / exam_id / "parts"
    questions: list[dict] = []
    if parts_dir.exists():
        for p in sorted(parts_dir.glob("*.json")):
            try:
                data = json.loads(p.read_text(encoding="utf-8"))
            except Exception as exc:  # noqa: BLE001
                print(f"  [skip] {p.name}: 解析失败 {exc}")
                continue
            qs = data.get("questions", []) if isinstance(data, dict) else data
            questions.extend(qs)
            print(f"  [part] {p.name}: {len(qs)} 题")
    return questions


def merge(exam_id: str, exam_name: str) -> tuple[dict, dict]:
    parts = load_parts(exam_id)
    # 合并旧版本 v1（作为种子保留）
    old = BANK_DIR / exam_id / "v1.json"
    if old.exists():
        data = json.loads(old.read_text(encoding="utf-8"))
        parts.extend(data.get("questions", []))
        print(f"  [seed] v1.json: {len(data.get('questions', []))} 题")

    seen_ids: set[str] = set()
    seen_stems: dict[str, str] = {}
    merged: list[dict] = []
    dup_id = 0
    dup_stem = 0
    for q in parts:
        qid = str(q.get("id", ""))
        stem_key = norm_stem(q.get("stem", ""))
        if not qid or qid in seen_ids:
            dup_id += 1
            continue
        if stem_key and stem_key in seen_stems:
            dup_stem += 1
            continue
        seen_ids.add(qid)
        if stem_key:
            seen_stems[stem_key] = qid
        chapter = str(q.get("chapter", ""))
        if chapter in CHAPTER_ALIASES:
            q = dict(q)
            q["chapter"] = CHAPTER_ALIASES[chapter]
        merged.append(q)

    merged.sort(key=lambda x: str(x.get("id", "")))
    bank = {
        "examId": exam_id,
        "examName": exam_name,
        "level": "中级",
        "version": TARGET_VERSION,
        "updatedAt": "2026-10-24T20:00:00Z",
        "questions": merged,
    }
    stats = {
        "total": len(merged),
        "dupId": dup_id,
        "dupStem": dup_stem,
        "types": Counter(q.get("type", "?") for q in merged),
        "chapters": Counter(q.get("chapter", "?") for q in merged),
        "difficulty": Counter(q.get("difficulty", "?") for q in merged),
        "knowledgePoints": len({q.get("knowledgePoint", "") for q in merged}),
    }
    return bank, stats


def report(exam_id: str, stats: dict) -> bool:
    total = stats["total"]
    chapters: Counter = stats["chapters"]
    print(f"\n=== {exam_id} ===")
    print(f"  合并后题数：{total}（去重：id {stats['dupId']}，题干 {stats['dupStem']}）")
    print(f"  题型分布：{dict(stats['types'])}")
    print(f"  难度分布：{dict(sorted(stats['difficulty'].items(), key=lambda kv: str(kv[0])))}")
    print(f"  知识点数：{stats['knowledgePoints']}")
    print(f"  章节数：{len(chapters)}")
    top = chapters.most_common(5)
    print("  题量最多的章节：" + "，".join(f"{c}={n}（{n / max(total, 1):.1%}）" for c, n in top))

    ok = True
    if total < MIN_PER_EXAM:
        print(f"  ✗ 不足 {MIN_PER_EXAM} 题")
        ok = False
    thin = [(c, n) for c, n in chapters.items() if n < MIN_PER_CHAPTER]
    if thin:
        print(f"  ✗ 以下章节题量不足 {MIN_PER_CHAPTER}：" + "，".join(f"{c}={n}" for c, n in thin[:8]))
        ok = False
    over = [(c, n) for c, n in chapters.items() if n / max(total, 1) > MAX_CHAPTER_SHARE]
    if over:
        print("  ✗ 以下章节占比过高：" + "，".join(f"{c}={n}" for c, n in over[:5]))
        ok = False
    if ok:
        print("  ✓ 质量门槛通过")
    return ok


def main(argv: list[str]) -> int:
    exams = {
        "system-integration": "系统集成项目管理工程师",
        "multimedia-designer": "多媒体应用设计师",
    }
    args = [a for a in argv[1:] if not a.startswith("--")]
    check_only = "--check" in argv[1:]
    if args:
        exams = {k: v for k, v in exams.items() if k in args}

    all_ok = True
    total_all = 0
    pending: list[tuple[Path, dict]] = []

    for exam_id, exam_name in exams.items():
        bank, stats = merge(exam_id, exam_name)
        out = BANK_DIR / exam_id / f"v{TARGET_VERSION}.json"
        if check_only:
            if not out.exists():
                print(f"  [check] 尚无 {out.name}（分片未合并或仓库未收录分片），跳过")
            else:
                existing = json.loads(out.read_text(encoding="utf-8"))
                same = existing.get("questions") == bank["questions"]
                print(f"  [check] {out.relative_to(ROOT)}：{'与分片一致' if same else '与分片不一致，需重新合并'}")
                all_ok &= same
        else:
            pending.append((out, bank))
        all_ok &= report(exam_id, stats)
        total_all += stats["total"]

    print(f"\n两科合计：{total_all} 题")
    if total_all < MIN_TOTAL:
        print(f"✗ 总量不足 {MIN_TOTAL}")
        if not check_only:
            all_ok = False
    else:
        print(f"✓ 总量达到 {MIN_TOTAL} 门槛")

    if not check_only:
        if all_ok:
            for out, bank in pending:
                out.write_text(json.dumps(bank, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                print(f"  [out] {out.relative_to(ROOT)}")
        else:
            print("✗ 质量门禁未通过，已中止写入（不会产生半成品题库文件）")
    return 0 if all_ok else 1


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
