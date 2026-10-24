#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为 AI 解析任务切分题目，并汇总子任务产出的解析。

用法：
    python tools/ai_pipeline.py split            # 切分题目到 ai_parts/*.in.json
    python tools/ai_pipeline.py merge            # 汇总 .out.json → 题库 aiExplanation
    python tools/ai_pipeline.py status           # 查看进度
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
BANK_DIR = ROOT / "question-bank"
CHUNK_SIZE = 160

EXAMS = ["system-integration", "multimedia-designer"]


def split() -> int:
    total_chunks = 0
    for exam in EXAMS:
        bank = json.loads((BANK_DIR / exam / "v2.json").read_text(encoding="utf-8"))
        questions = bank["questions"]
        parts_dir = BANK_DIR / exam / "ai_parts"
        parts_dir.mkdir(parents=True, exist_ok=True)
        for i in range(0, len(questions), CHUNK_SIZE):
            chunk = questions[i:i + CHUNK_SIZE]
            name = f"{exam}-{i // CHUNK_SIZE:02d}"
            payload = []
            for q in chunk:
                payload.append(
                    {
                        "id": q["id"],
                        "type": q.get("type"),
                        "chapter": q.get("chapter"),
                        "knowledgePoint": q.get("knowledgePoint"),
                        "stem": q.get("stem"),
                        "options": q.get("options") or [],
                        "answer": q.get("answer") or [],
                        "explanation": q.get("explanation") or "",
                        "caseStudy": q.get("caseStudy"),
                        "subQuestions": q.get("subQuestions") or [],
                    }
                )
            (parts_dir / f"{name}.in.json").write_text(
                json.dumps({"chunk": name, "examId": exam, "questions": payload}, ensure_ascii=False, indent=1),
                encoding="utf-8",
            )
            total_chunks += 1
        print(f"[{exam}] {len(questions)} 题 → {len(range(0, len(questions), CHUNK_SIZE))} 个分片")
    print(f"共 {total_chunks} 个分片（每片 {CHUNK_SIZE} 题）")
    return 0


def merge() -> int:
    for exam in EXAMS:
        parts_dir = BANK_DIR / exam / "ai_parts"
        bank_path = BANK_DIR / exam / "v2.json"
        if not parts_dir.exists() or not bank_path.exists():
            print(f"[{exam}] 缺少分片或题库，跳过")
            continue
        explanations: dict[str, str] = {}
        issues: list[dict] = []
        done_files = 0
        for out_file in sorted(parts_dir.glob("*.out.json")):
            try:
                data = json.loads(out_file.read_text(encoding="utf-8"))
            except Exception as exc:  # noqa: BLE001
                print(f"  [skip] {out_file.name}: {exc}")
                continue
            items = data if isinstance(data, list) else data.get("items", [])
            for item in items:
                qid = str(item.get("id", "")).strip()
                text = str(item.get("ai", "")).strip()
                if not qid or not text:
                    continue
                if "【存疑】" in text:
                    issues.append({"id": qid, "note": text[-500:]})
                explanations[qid] = text
            done_files += 1

        bank = json.loads(bank_path.read_text(encoding="utf-8"))
        hit = 0
        for q in bank["questions"]:
            text = explanations.get(q["id"])
            if text:
                q["aiExplanation"] = text
                hit += 1
        bank_path.write_text(json.dumps(bank, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"[{exam}] 汇总 {done_files} 个分片，写入 AI 解析 {hit}/{len(bank['questions'])} 题")
        if issues:
            ip = BANK_DIR / exam / "ai_issues.json"
            ip.write_text(json.dumps(issues, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"         模型标记存疑 {len(issues)} 处 → {ip.name}")
    return 0


def status() -> int:
    for exam in EXAMS:
        bank_path = BANK_DIR / exam / "v2.json"
        parts_dir = BANK_DIR / exam / "ai_parts"
        if not bank_path.exists():
            continue
        bank = json.loads(bank_path.read_text(encoding="utf-8"))
        total = len(bank["questions"])
        have = sum(1 for q in bank["questions"] if q.get("aiExplanation"))
        ins = list(parts_dir.glob("*.in.json")) if parts_dir.exists() else []
        outs = list(parts_dir.glob("*.out.json")) if parts_dir.exists() else []
        print(f"[{exam}] 题库 {total} 题 | 已带 AI 解析 {have} | 分片 {len(ins)} 个，已完成 {len(outs)} 个")
    return 0


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    {"split": split, "merge": merge, "status": status}.get(cmd, status)()
