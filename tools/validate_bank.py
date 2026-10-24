#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""题库校验脚本：与 App 内 QuestionBankParser 的规则保持一致。

用法：
    python tools/validate_bank.py question-bank/system-integration/v1.json [...]
    python tools/validate_bank.py --all
退出码：0 通过；1 存在致命错误；2 用法错误。
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

REQUIRED_TEXT = ("id", "examId", "stem", "chapter", "knowledgePoint", "explanation", "source")
VALID_TYPES = {"single", "multi", "truefalse", "case"}
MIN_EXPLANATION = 20


def validate_file(path: Path) -> list[str]:
    errors: list[str] = []
    try:
        raw = path.read_text(encoding="utf-8")
    except Exception as exc:  # noqa: BLE001
        return [f"{path}: 无法读取（{exc}）"]

    try:
        data = json.loads(raw)
    except Exception as exc:  # noqa: BLE001
        return [f"{path}: JSON 解析失败（{exc}）"]

    if not isinstance(data, dict):
        return [f"{path}: 顶层必须是对象"]
    if not data.get("examId"):
        errors.append(f"{path}: 缺少 examId")
    if not isinstance(data.get("questions"), list) or not data["questions"]:
        errors.append(f"{path}: questions 必须是非空数组")
        return errors

    seen: set[str] = set()
    for i, q in enumerate(data["questions"], start=1):
        tag = f"{path} 第{i}题"
        if not isinstance(q, dict):
            errors.append(f"{tag}: 不是对象")
            continue
        for field in REQUIRED_TEXT:
            if not str(q.get(field, "")).strip():
                errors.append(f"{tag}: 缺少必填字段 {field}")
        qid = str(q.get("id", ""))
        if qid:
            if qid in seen:
                errors.append(f"{tag}: id 重复 {qid}")
            seen.add(qid)

        qtype = q.get("type")
        if qtype not in VALID_TYPES:
            errors.append(f"{tag}: type 非法 {qtype!r}")
            continue

        difficulty = q.get("difficulty")
        if not isinstance(difficulty, int) or not 1 <= difficulty <= 5:
            errors.append(f"{tag}: difficulty 必须是 1-5 的整数，当前 {difficulty!r}")

        options = q.get("options") or []
        keys = [o.get("key") for o in options if isinstance(o, dict)]
        answer = q.get("answer") or []
        if not isinstance(answer, list) or not answer:
            errors.append(f"{tag}: answer 必须是非空数组")
            continue

        if qtype == "single" and len(answer) != 1:
            errors.append(f"{tag}: 单选题答案必须恰好 1 个")
        if qtype == "multi" and len(answer) < 2:
            errors.append(f"{tag}: 多选题答案至少 2 个")
        if qtype == "truefalse" and any(a not in ("√", "×") for a in answer):
            errors.append(f"{tag}: 判断题答案只能是 √ 或 ×")
        if qtype in ("single", "multi"):
            if not options:
                errors.append(f"{tag}: 缺少选项")
            else:
                missing = [a for a in answer if a not in keys]
                if missing:
                    errors.append(f"{tag}: 答案 {missing} 不在选项 key {keys} 中")
                expected = [chr(ord("A") + n) for n in range(len(options))]
                if keys != expected:
                    errors.append(f"{tag}: 选项 key 应连续 {expected}，当前 {keys}")

        explanation = str(q.get("explanation", ""))
        if len(explanation) < MIN_EXPLANATION:
            errors.append(f"{tag}: 解析长度 {len(explanation)} < {MIN_EXPLANATION}")

    return errors


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2

    root = Path(__file__).resolve().parent.parent
    if argv[1] == "--all":
        files = sorted(root.glob("question-bank/**/*.json"))
        if not files:
            print("未找到题库文件")
            return 2
    else:
        files = [Path(a) for a in argv[1:]]

    all_errors: list[str] = []
    total = 0
    for f in files:
        errs = validate_file(f)
        all_errors += errs
        count = 0
        try:
            count = len(json.loads(f.read_text(encoding="utf-8")).get("questions", []))
        except Exception:  # noqa: BLE001
            pass
        total += count
        status = "OK " if not errs else "FAIL"
        print(f"[{status}] {f}  ({count} 题)")
        for e in errs[:20]:
            print("      -", e)
        if len(errs) > 20:
            print(f"      … 另有 {len(errs) - 20} 条问题")

    print(f"\n合计 {len(files)} 个文件、{total} 道题；问题 {len(all_errors)} 条")
    return 0 if not all_errors else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
