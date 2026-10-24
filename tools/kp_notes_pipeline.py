#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""知识点精讲（每条一句话）生产管线。

从题库里抽出每个知识点的题量与代表题，切片交给子任务撰写，
产出 question-bank/<examId>/kp_notes.json，供 App 的「知识点」模块逐条展示。

用法：
    python tools/kp_notes_pipeline.py split
    python tools/kp_notes_pipeline.py merge
    python tools/kp_notes_pipeline.py status
"""
from __future__ import annotations

import json
import pathlib
import shutil
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(r"E:\ai coding\pojie\softkao-trainer")
BANK = ROOT / "question-bank"
SEED_DIR = ROOT / "app" / "src" / "main" / "assets"
CHUNK_SIZE = 300

EXAMS = {
    "system-integration": "系统集成项目管理工程师",
    "multimedia-designer": "多媒体应用设计师",
}


def collect(exam: str) -> list[dict]:
    bank = json.loads((BANK / exam / "v2.json").read_text(encoding="utf-8"))
    grouped: dict[str, list[dict]] = {}
    for q in bank["questions"]:
        grouped.setdefault(q.get("knowledgePoint", ""), []).append(q)
    out = []
    for point, qs in grouped.items():
        if not point:
            continue
        samples = []
        for q in qs[:3]:
            stem = (q.get("stem") or "").replace("\n", " ")
            samples.append(stem[:80])
        out.append(
            {
                "point": point,
                "chapter": qs[0].get("chapter", ""),
                "count": len(qs),
                "samples": samples,
            }
        )
    out.sort(key=lambda x: (-x["count"], x["point"]))
    return out


def split() -> int:
    total = 0
    for exam, exam_name in EXAMS.items():
        points = collect(exam)
        parts_dir = BANK / exam / "kp_parts"
        parts_dir.mkdir(parents=True, exist_ok=True)
        for i in range(0, len(points), CHUNK_SIZE):
            chunk = points[i:i + CHUNK_SIZE]
            name = f"{exam}-kp-{i // CHUNK_SIZE:02d}"
            (parts_dir / f"{name}.in.json").write_text(
                json.dumps({"chunk": name, "examId": exam, "examName": exam_name, "points": chunk},
                           ensure_ascii=False, indent=1),
                encoding="utf-8",
            )
            total += 1
        print(f"[{exam}] {len(points)} 个知识点 → {len(range(0, len(points), CHUNK_SIZE))} 个分片")
    print(f"共 {total} 个分片")
    return 0


def merge() -> int:
    for exam, exam_name in EXAMS.items():
        parts_dir = BANK / exam / "kp_parts"
        if not parts_dir.exists():
            continue
        notes: dict[str, str] = {}
        for f in sorted(parts_dir.glob("*.out*.json")):
            try:
                data = json.loads(f.read_text(encoding="utf-8"))
            except Exception as exc:  # noqa: BLE001
                print(f"  [skip] {f.name}: {exc}")
                continue
            items = data if isinstance(data, list) else data.get("notes", [])
            for it in items:
                p = str(it.get("point", "")).strip()
                n = str(it.get("note", "")).strip()
                if p and n:
                    notes[p] = n
        payload = {"examId": exam, "examName": exam_name, "version": 1, "notes": notes}
        out = BANK / exam / "kp_notes.json"
        out.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        (SEED_DIR / "kp_notes").mkdir(parents=True, exist_ok=True)
        shutil.copyfile(out, SEED_DIR / "kp_notes" / f"{exam}.json")
        print(f"[{exam}] 合并知识点精讲 {len(notes)} 条 → kp_notes.json + assets/kp_notes/{exam}.json")
    return 0


def missing() -> int:
    """列出还没有精讲的知识点，并切出补写任务书"""
    total_missing = 0
    for exam in EXAMS:
        points = collect(exam)
        merged = BANK / exam / "kp_notes.json"
        have: dict[str, str] = {}
        if merged.exists():
            have = json.loads(merged.read_text(encoding="utf-8")).get("notes", {})
        todo = [p for p in points if p["point"] not in have or not have[p["point"]].strip()]
        total_missing += len(todo)
        parts_dir = BANK / exam / "kp_parts"
        parts_dir.mkdir(parents=True, exist_ok=True)
        for i in range(0, len(todo), CHUNK_SIZE):
            chunk = todo[i:i + CHUNK_SIZE]
            name = f"{exam}-kp-miss-{i // CHUNK_SIZE:02d}"
            (parts_dir / f"{name}.in.json").write_text(
                json.dumps({"chunk": name, "examId": exam, "examName": EXAMS[exam], "points": chunk},
                           ensure_ascii=False, indent=1),
                encoding="utf-8",
            )
        n_chunks = len(range(0, len(todo), CHUNK_SIZE))
        print(f"[{exam}] 总 {len(points)} 个知识点，已写 {len(points) - len(todo)}，待补 {len(todo)} → {n_chunks} 个分片")
    print(f"合计待补 {total_missing} 条")
    return 0


def status() -> int:
    for exam in EXAMS:
        parts_dir = BANK / exam / "kp_parts"
        ins = list(parts_dir.glob("*.in.json")) if parts_dir.exists() else []
        outs = list(parts_dir.glob("*.out*.json")) if parts_dir.exists() else []
        merged = BANK / exam / "kp_notes.json"
        n = 0
        if merged.exists():
            n = len(json.loads(merged.read_text(encoding="utf-8")).get("notes", {}))
        print(f"[{exam}] 知识点分片 {len(ins)} 个，已完成 {len(outs)} 个 | 已合并精讲 {n} 条")
    return 0


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    {"split": split, "merge": merge, "status": status, "missing": missing}.get(cmd, status)()
