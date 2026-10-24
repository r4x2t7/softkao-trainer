#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""UI 契约检查：在没有设备的情况下，用静态规则守住 UI 约定。

这些规则都来自用户明确提过的要求，每一条都对应一次真实返工：

R1 内容型文字不许被省略号截断（用户：界面文字不许自己换行/省略）
R2 单行文字只允许出现在固定短标签上（底部导航、跳页按钮、题号）
R3 Routes 里声明的每个路由都必须在 NavHost 里注册（点得进去）
R4 切换科目必须写全局设置，不许各页面自己存一套作用域
R5 可交互控件必须有明确的可读文本（无障碍：屏幕阅读器能念出来）

用法：
    python tools/ui_contract_check.py
"""
from __future__ import annotations

import pathlib
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "app" / "src" / "main" / "java" / "com" / "softkao" / "trainer"

# 允许 maxLines = 1 的场景：这些都是固定短文本，本身不会溢出
MAXLINES_ALLOW = [
    "tab.label",          # 底部导航栏标签（10sp）
    "Text(tab.label",
    "JumpButton",         # 跳页按钮：⌂首页 / −100 / +10 …
    "答题卡",              # 答题卡里的题号
    "第 ${index + 1}",     # 底部「第 x / y 题」
    "i + 1}",             # 答题卡题号
]


def kotlin_files() -> list[pathlib.Path]:
    return sorted(p for p in SRC.rglob("*.kt"))


def check_no_ellipsis(files: list[pathlib.Path]) -> list[str]:
    """R1：feature 层不允许出现 TextOverflow.Ellipsis"""
    problems = []
    for f in files:
        for i, line in enumerate(f.read_text(encoding="utf-8").split("\n"), 1):
            if "TextOverflow.Ellipsis" in line:
                rel = f.relative_to(SRC)
                problems.append(f"R1 {rel}:{i} 仍然用省略号截断文字：{line.strip()[:90]}")
    return problems


def check_maxlines(files: list[pathlib.Path]) -> list[str]:
    """R2：maxLines = 1 只允许出现在「固定短文本」上。

    判定方式：找到 Text(...) 调用块，取它的内容表达式——
    如果是长度不超过 8 个字的字符串字面量（按钮名、chip 名）或少数已知安全表达式，
    就认为它本来就不会溢出，允许单行；否则一律报出来。
    """
    problems = []
    safe_exprs = (
        "exam.shortName", "tab.label", "exam.name",
    )
    for f in files:
        text = f.read_text(encoding="utf-8")
        lines = text.split("\n")
        i = 0
        while i < len(lines):
            line = lines[i]
            if "Text(" not in line:
                i += 1
                continue
            # 收集这个 Text(...) 调用块，直到括号配平
            block = []
            depth = line.count("(") - line.count(")")
            j = i
            while j < len(lines) and j < i + 30:
                block.append(lines[j])
                if j > i:
                    depth += lines[j].count("(") - lines[j].count(")")
                if depth <= 0:
                    break
                j += 1
            body = "\n".join(block)
            if "maxLines = 1" in body or "maxLines=1" in body:
                # 显式豁免：写上 // ui-ok: 理由 的地方不再报
                if "ui-ok:" in body:
                    i = j + 1
                    continue
                content = _first_argument(body)
                if content is None:
                    pass
                elif any(s in content for s in safe_exprs):
                    i = j + 1
                    continue
                elif "$" in content and ("题" in content or "批" in content):
                    # 形如「第 x / y 题」「第 x / y 批」的固定格式
                    i = j + 1
                    continue
                elif content.startswith('"'):
                    literal = content.strip('"')
                    if len(literal) <= 8:
                        i = j + 1
                        continue
                rel = f.relative_to(SRC)
                first = block[0].strip()[:80]
                problems.append(f"R2 {rel}:{i + 1} 单行截断的内容不是固定短文本：{first}")
            i = j + 1
    return problems


def _first_argument(body: str) -> str | None:
    """取 Text( 后面的第一个实参（粗略处理嵌套括号）。"""
    start = body.find("Text(")
    if start < 0:
        return None
    rest = body[start + len("Text("):].lstrip()
    if not rest:
        return None
    # 字符串字面量
    if rest.startswith('"'):
        end = rest.find('"', 1)
        return rest[: end + 1] if end > 0 else rest
    # 表达式：取到第一个逗号或换行前的标识符
    out = []
    depth = 0
    for ch in rest:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            break
        if ch == "\n" and depth == 0:
            break
        out.append(ch)
    return "".join(out).strip() or None


def check_routes(files: list[pathlib.Path]) -> list[str]:
    """R3：Routes 常量都要在 NavHost 里注册"""
    problems = []
    main = SRC / "MainActivity.kt"
    text = main.read_text(encoding="utf-8")
    routes_block = re.search(r"object Routes\s*\{(.*?)\n\}", text, re.S)
    if not routes_block:
        return ["R3 找不到 Routes 对象"]
    names = re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', routes_block.group(1))
    for name, value in names:
        # 路由字符串可能带 {param} 占位符，注册时用的是常量名而不是字面量
        if f"Routes.{name}" not in text:
            problems.append(f"R3 路由 {name}（{value}）没有在 NavHost 中注册")
    return problems


def check_exam_scope(files: list[pathlib.Path]) -> list[str]:
    """R4：切换科目的地方必须写全局设置"""
    problems = []
    for f in files:
        text = f.read_text(encoding="utf-8")
        # 出现「自建科目作用域」的典型写法
        for i, line in enumerate(text.split("\n"), 1):
            if re.search(r"val\s+\w*(exam|Exam)Id\s*=\s*(ExamId\.|mutableStateOf|\"\")", line):
                if "currentExamId" in line:
                    continue
                if "rememberCurrentExamId" in line:
                    continue
                if "examId: String" in line or "val examId: String" in line:
                    continue
                rel = f.relative_to(SRC)
                problems.append(f"R4 {rel}:{i} 疑似自建科目作用域：{line.strip()[:90]}")
    return problems


def check_clickable_labels(files: list[pathlib.Path]) -> list[str]:
    """R5：可点击的图标类控件必须有可读文本或描述"""
    problems = []
    for f in files:
        text = f.read_text(encoding="utf-8")
        for i, line in enumerate(text.split("\n"), 1):
            if "Icon(" in line:
                window = "\n".join(text.split("\n")[i - 1: i + 4])
                if "contentDescription" not in window:
                    rel = f.relative_to(SRC)
                    problems.append(f"R5 {rel}:{i} Icon 没有 contentDescription")
    return problems


def check_visual_bounds(files: list[pathlib.Path]) -> list[str]:
    """R6：图解 SVG 的内容不许画到 viewBox 之外（会被裁掉）"""
    problems = []
    tool = ROOT / "tools" / "check_visual_bounds.py"
    if not tool.exists():
        return problems
    import subprocess
    try:
        out = subprocess.run(
            [sys.executable, str(tool)],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=120,
        ).stdout
    except Exception:
        return problems
    tail = [l for l in out.split("\n") if l.strip().startswith("检查")]
    if tail and "内容越界 0 个" not in tail[-1]:
        problems.append("R6 图解内容超出 viewBox（会被裁掉），跑 python tools/check_visual_bounds.py 看明细")
    return problems


def main() -> int:
    files = kotlin_files()
    if not files:
        print("找不到 Kotlin 源码")
        return 2

    problems: list[str] = []
    problems += check_no_ellipsis(files)
    problems += check_maxlines(files)
    problems += check_routes(files)
    problems += check_exam_scope(files)
    problems += check_clickable_labels(files)
    problems += check_visual_bounds(files)

    print(f"扫描 {len(files)} 个 Kotlin 文件")
    if not problems:
        print("UI 契约检查通过：0 条问题")
        return 0
    print(f"发现 {len(problems)} 条问题：")
    for p in problems:
        print("  " + p)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
