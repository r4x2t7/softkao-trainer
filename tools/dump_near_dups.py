# -*- coding: utf-8 -*-
"""打印所有冲突题组的完整内容（题干/选项/答案），便于人工判定。"""
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

import re
from difflib import SequenceMatcher

for exam_id, exam_name in [("system-integration", "系统集成"), ("multimedia-designer", "多媒体")]:
    parts = mb.load_parts(exam_id)
    old = mb.BANK_DIR / exam_id / "v1.json"
    if old.exists():
        parts.extend(json.loads(old.read_text(encoding="utf-8")).get("questions", []))
    merged, near, conflicts = mb.dedupe(parts)

    # 重新构造所有候选对（含被删掉的）
    seen_ids, seen_stems, kept = set(), {}, []
    for q in parts:
        qid = str(q.get("id", ""))
        sk = mb.norm_stem(q.get("stem", ""))
        if not qid or qid in seen_ids or (sk and sk in seen_stems):
            continue
        seen_ids.add(qid)
        if sk:
            seen_stems[sk] = qid
        kept.append(q)

    index = {}
    grams = []
    for i, q in enumerate(kept):
        g = mb.shingles(mb.norm_stem(q.get("stem", "")))
        grams.append(g)
        for sh in g:
            index.setdefault(sh, []).append(i)
    pairs = set()
    for sh, idxs in index.items():
        if len(idxs) > 200:
            continue
        for i in range(len(idxs)):
            for j in range(i + 1, len(idxs)):
                a, b = idxs[i], idxs[j]
                if len(grams[a] & grams[b]) >= 3:
                    pairs.add((a, b) if a < b else (b, a))

    print("#" * 72)
    print(f"# {exam_name} 近似题组（相似度>=0.90）")
    print("#" * 72)
    shown = 0
    for a, b in sorted(pairs):
        sa = mb.norm_stem(kept[a].get("stem", ""))
        sb = mb.norm_stem(kept[b].get("stem", ""))
        if not sa or not sb:
            continue
        sim = SequenceMatcher(None, sa, sb).ratio()
        if sim < 0.90:
            continue
        shown += 1
        qa, qb = kept[a], kept[b]
        same_ans = set(qa.get("answer") or []) == set(qb.get("answer") or [])
        print(f"\n[{shown}] 相似度 {sim:.2f} | 答案{'一致' if same_ans else '不一致 ⚠'}")
        for tag, q in (("A", qa), ("B", qb)):
            print(f"  [{tag}] {q['id']} | {q.get('type')} | {q.get('chapter')} | {q.get('knowledgePoint')}")
            print(f"      题干：{q.get('stem','')[:150]}")
            for o in (q.get("options") or [])[:5]:
                print(f"        {o.get('key')}. {o.get('text','')[:60]}")
            print(f"      答案：{''.join(q.get('answer') or [])}")
    print(f"\n共 {shown} 组")
