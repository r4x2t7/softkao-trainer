#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""知识点精讲资料管线：按章节切分 → 子任务撰写 → 汇总为 materials.json。

每章一份「学习资料」，包含：章节概览、命题趋势、要点清单、易错清单、
公式与数字、记忆口诀、主观题答题模板。App 内「知识点」模块直接读取。

用法：
    python tools/material_pipeline.py split
    python tools/material_pipeline.py merge
    python tools/material_pipeline.py status
"""
from __future__ import annotations

import json
import pathlib
import shutil
import sys
from collections import defaultdict

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK_DIR = ROOT / "question-bank"
SEED_DIR = ROOT / "app" / "src" / "main" / "assets" / "materials"
CHUNK_SIZE = 12  # 每片 12 章

EXAMS = {
    "system-integration": "系统集成项目管理工程师",
    "multimedia-designer": "多媒体应用设计师",
}


def split() -> int:
    total = 0
    for exam, exam_name in EXAMS.items():
        bank = json.loads((BANK_DIR / exam / "v2.json").read_text(encoding="utf-8"))
        by_chapter: dict[str, dict] = defaultdict(lambda: {"points": defaultdict(int), "types": defaultdict(int), "difficulty": []})
        for q in bank["questions"]:
            c = by_chapter[q.get("chapter", "未分类")]
            c["points"][q.get("knowledgePoint", "")] += 1
            c["types"][q.get("type", "?")] += 1
            c["difficulty"].append(q.get("difficulty", 3))

        chapters = []
        for name, info in sorted(by_chapter.items(), key=lambda kv: -sum(kv[1]["points"].values())):
            chapters.append(
                {
                    "chapter": name,
                    "questionCount": sum(info["points"].values()),
                    "knowledgePoints": [
                        {"name": k, "count": v} for k, v in sorted(info["points"].items(), key=lambda kv: -kv[1])
                    ],
                    "types": dict(info["types"]),
                    "avgDifficulty": round(sum(info["difficulty"]) / max(len(info["difficulty"]), 1), 2),
                }
            )

        parts_dir = BANK_DIR / exam / "material_parts"
        parts_dir.mkdir(parents=True, exist_ok=True)
        for i in range(0, len(chapters), CHUNK_SIZE):
            chunk = chapters[i:i + CHUNK_SIZE]
            name = f"{exam}-mat-{i // CHUNK_SIZE:02d}"
            (parts_dir / f"{name}.in.json").write_text(
                json.dumps(
                    {
                        "chunk": name,
                        "examId": exam,
                        "examName": exam_name,
                        "chapters": chunk,
                    },
                    ensure_ascii=False,
                    indent=1,
                ),
                encoding="utf-8",
            )
            total += 1
        print(f"[{exam}] {len(chapters)} 章 → {len(range(0, len(chapters), CHUNK_SIZE))} 个分片")
    print(f"共 {total} 个分片（每片 {CHUNK_SIZE} 章）")
    return 0


def merge() -> int:
    SEED_DIR.mkdir(parents=True, exist_ok=True)
    for exam, exam_name in EXAMS.items():
        parts_dir = BANK_DIR / exam / "material_parts"
        if not parts_dir.exists():
            continue
        chapters: list[dict] = []
        seen_chapters: set[str] = set()
        duplicates = 0
        # 优先合并主输出（*.out.json），再合并补写输出（*.out2.json 等）
        out_files = sorted(parts_dir.glob("*.out*.json"), key=lambda p: (len(p.name.split(".")), p.name))
        for out_file in out_files:
            try:
                data = json.loads(out_file.read_text(encoding="utf-8"))
            except Exception as exc:  # noqa: BLE001
                print(f"  [skip] {out_file.name}: {exc}")
                continue
            items = data if isinstance(data, list) else data.get("chapters", [])
            for item in items:
                name = item.get("chapter", "")
                if not name:
                    continue
                if name in seen_chapters:
                    duplicates += 1
                    continue
                seen_chapters.add(name)
                chapters.append(item)
        if duplicates:
            print(f"  [dedup] 跳过 {duplicates} 章重复内容")
        if not chapters:
            print(f"[{exam}] 没有可合并的资料")
            continue
        chapters.sort(key=lambda c: c.get("chapter", ""))
        payload = {"examId": exam, "examName": exam_name, "version": 1, "chapters": chapters}
        out = BANK_DIR / exam / "materials.json"
        out.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        shutil.copyfile(out, SEED_DIR / f"{exam}.json")
        # 同时放到 question-bank/materials/<examId>.json，与 App 更新通道的拉取路径一致
        pub_dir = BANK_DIR / "materials"
        pub_dir.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(out, pub_dir / f"{exam}.json")
        print(f"[{exam}] 合并 {len(chapters)} 章资料 → materials.json + assets/materials/{exam}.json + question-bank/materials/{exam}.json")
    return 0


def status() -> int:
    for exam in EXAMS:
        parts_dir = BANK_DIR / exam / "material_parts"
        ins = list(parts_dir.glob("*.in.json")) if parts_dir.exists() else []
        outs = list(parts_dir.glob("*.out.json")) if parts_dir.exists() else []
        mat = BANK_DIR / exam / "materials.json"
        done = "有" if mat.exists() else "无"
        print(f"[{exam}] 资料分片 {len(ins)} 个，已完成 {len(outs)} 个 | materials.json: {done}")
    return 0


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    {"split": split, "merge": merge, "status": status}.get(cmd, status)()
