#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成题库索引 + 同步内置种子题库。

用法：
    python tools/build_index.py            # 生成 index.json 并同步 assets/seed/
"""
from __future__ import annotations

import hashlib
import json
import shutil
from datetime import datetime, timezone
from pathlib import Path

try:
    import sys

    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

ROOT = Path(__file__).resolve().parent.parent
BANK_DIR = ROOT / "question-bank"
SEED_DIR = ROOT / "app" / "src" / "main" / "assets" / "seed"
EXAMS = {
    "system-integration": "系统集成项目管理工程师",
    "multimedia-designer": "多媒体应用设计师",
}


def sha256(path: Path) -> str:
    """对 LF 归一化后的内容取哈希，保证 Windows / Linux 算出的值一致。"""
    data = path.read_bytes().replace(b"\r\n", b"\n")
    return hashlib.sha256(data).hexdigest()


def write_lf(path: Path, data: bytes) -> None:
    """统一以 LF 写文件，避免不同平台产出的字节不一致。"""
    path.write_bytes(data.replace(b"\r\n", b"\n"))


def main() -> int:
    SEED_DIR.mkdir(parents=True, exist_ok=True)
    index_updated = ""
    banks = []
    for exam_id, exam_name in EXAMS.items():
        files = sorted((BANK_DIR / exam_id).glob("v*.json"))
        if not files:
            print(f"[skip] {exam_id}: 没有题库文件")
            continue
        latest = files[-1]
        data = json.loads(latest.read_text(encoding="utf-8"))
        count = len(data.get("questions", []))
        rel = f"{exam_id}/{latest.name}"
        bank_updated = str(data.get("updatedAt", ""))
        index_updated = max(index_updated, bank_updated)
        banks.append(
            {
                "examId": exam_id,
                "examName": exam_name,
                "version": int(data.get("version", 1)),
                "file": rel,
                "questionCount": count,
                "sha256": sha256(latest),
                "updatedAt": bank_updated,
            }
        )
        # 同步到 App 内置种子（统一 LF，避免跨平台字节差异）
        write_lf(SEED_DIR / f"{exam_id}.json", latest.read_bytes())
        print(f"[ok] {rel}: {count} 题 → assets/seed/{exam_id}.json")

    # updatedAt 取题库自身的时间戳，保证同样输入产出同样索引（CI 可比对）
    index = {
        "updatedAt": index_updated or "1970-01-01T00:00:00Z",
        "banks": banks,
    }
    (BANK_DIR / "index.json").write_text(
        json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"[ok] question-bank/index.json: {len(banks)} 个题库")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
