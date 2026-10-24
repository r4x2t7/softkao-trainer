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
from collections import Counter, defaultdict
from difflib import SequenceMatcher
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


def shingles(text: str, k: int = 5) -> set[str]:
    """字符 k-gram 集合，用于快速筛出可能重复的候选对"""
    if len(text) < k:
        return {text} if text else set()
    return {text[i:i + k] for i in range(len(text) - k + 1)}


def similarity(a: str, b: str) -> float:
    return SequenceMatcher(None, a, b).ratio()


def dedupe(questions: list[dict], threshold: float = 0.90) -> tuple[list[dict], list[str], list[str]]:
    """三步去重：完全相同 id → 完全相同题干 → 近重复题干。

    近重复的判定必须同时满足：
      1) 题干相似度 ≥ threshold（字符 5-gram 倒排索引筛候选，避免 O(n²) 全比）
      2) 题型相同（单选版与多选版是两道题，不能当重复）
      3) 答案集合相同（同题不同答案属冲突，需人工复核，不能自动删）
    案例分析题（type=case）的题干是「小问（N）：…」模板，相似度无意义，直接跳过。
    保留优先级：解析更长 > 来源为「改编自」> id 更靠前。
    """
    seen_ids: set[str] = set()
    seen_stems: dict[str, str] = {}
    kept: list[dict] = []
    dup_id = 0
    dup_stem = 0
    near_dups: list[str] = []
    conflicts: list[str] = []

    for q in questions:
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
        kept.append(q)

    # ---- 近重复检测
    index: dict[str, list[int]] = defaultdict(list)
    grams: list[set[str]] = []
    for i, q in enumerate(kept):
        if q.get("type") == "case":
            grams.append(set())
            continue
        g = shingles(norm_stem(q.get("stem", "")))
        grams.append(g)
        for sh in g:
            index[sh].append(i)

    pairs: set[tuple[int, int]] = set()
    for sh, idxs in index.items():
        if len(idxs) > 200:  # 高频 n-gram（常见短语）跳过，避免爆炸
            continue
        for i in range(len(idxs)):
            for j in range(i + 1, len(idxs)):
                a, b = idxs[i], idxs[j]
                if len(grams[a] & grams[b]) >= 3:
                    pairs.add((a, b) if a < b else (b, a))

    drop: set[int] = set()
    for a, b in pairs:
        if a in drop or b in drop:
            continue
        qa, qb = kept[a], kept[b]
        if qa.get("type") != qb.get("type"):
            continue  # 题型不同 → 不是重复题
        sa = norm_stem(qa.get("stem", ""))
        sb = norm_stem(qb.get("stem", ""))
        if not sa or not sb:
            continue
        sim = similarity(sa, sb)
        if sim < threshold:
            continue
        ans_a = set(qa.get("answer") or [])
        ans_b = set(qb.get("answer") or [])
        if ans_a != ans_b:
            # 同题不同答案：保留两题并标记人工复核
            conflicts.append(
                f"{qa.get('id')} 与 {qb.get('id')} 题干近似（{sim:.2f}）但答案不同："
                f"{''.join(sorted(ans_a))} vs {''.join(sorted(ans_b))}"
            )
            continue
        winner, loser = (a, b) if _quality(qa) >= _quality(qb) else (b, a)
        drop.add(loser)
        near_dups.append(
            f"{kept[loser].get('id')} ≈ {kept[winner].get('id')}（相似度 {sim:.2f}，答案一致）"
        )

    result = [q for i, q in enumerate(kept) if i not in drop]
    return result, near_dups, conflicts


def _quality(q: dict) -> tuple[int, int]:
    """保留优先级打分：解析越长越好；「改编自」优于「自编」"""
    expl = len(q.get("explanation") or "")
    src = 1 if "改编" in (q.get("source") or "") else 0
    return (expl, src)


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
    merged, near_dups, conflicts = dedupe(merged)
    if near_dups:
        print(f"  [near-dup] 近重复去掉 {len(near_dups)} 题")
        for line in near_dups[:10]:
            print(f"      {line}")
        if len(near_dups) > 10:
            print(f"      … 另有 {len(near_dups) - 10} 条")
    if conflicts:
        print(f"  [warn] {len(conflicts)} 组近似题答案不一致，需人工复核：")
        for line in conflicts[:10]:
            print(f"      {line}")
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
        "nearDup": len(near_dups),
        "conflicts": conflicts,
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
