#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""题库发布流水线：合并 → 校验 → 重建索引与内置种子 → 汇总。

用法：
    python tools/release_bank.py            # 合并并生成 v2
    python tools/release_bank.py --check    # 只校验，不写文件
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:  # noqa: BLE001
    pass

ROOT = Path(__file__).resolve().parent.parent
TOOLS = ROOT / "tools"


def run(script: str, *args: str) -> int:
    cmd = [sys.executable, str(TOOLS / script), *args]
    print(f"\n>>> python tools/{script} {' '.join(args)}")
    result = subprocess.run(cmd, cwd=str(ROOT))
    return result.returncode


def main() -> int:
    check_only = "--check" in sys.argv[1:]

    print("=" * 60)
    print("步骤 1/3：合并分片 → v2")
    print("=" * 60)
    rc = run("merge_banks.py", *(["--check"] if check_only else []))
    if rc != 0:
        print("\n合并未通过质量门禁，已中止（不会写出半成品）。")
        return rc

    print("\n" + "=" * 60)
    print("步骤 2/3：校验题库")
    print("=" * 60)
    rc = run("validate_bank.py", "--all")
    if rc != 0:
        print("\n题库校验未通过，已中止。")
        return rc

    if not check_only:
        print("\n" + "=" * 60)
        print("步骤 3/3：重建索引与内置种子")
        print("=" * 60)
        rc = run("build_index.py")
        if rc != 0:
            print("\n索引重建失败。")
            return rc

    print("\n" + "=" * 60)
    print("发布流水线完成 ✅")
    print("=" * 60)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
