# -*- coding: utf-8 -*-
"""把误放进 nodes 列表的 edge(...) 挪到 edges 列表。"""
import pathlib
import re

PATH = pathlib.Path(
    r"E:\ai coding\pojie\softkao-trainer\app\src\main\java\com\softkao\trainer\core\data\VisualRepositoryExtra.kt"
)

text = PATH.read_text(encoding="utf-8")

# 找到 nodes = listOf( ... ) 块，把其中形如 edge("x","y", ...), 的整行摘出来
pattern = re.compile(r"nodes = listOf\((.*?)\n(\s*)\),\n", re.S)


def fix(match: re.S) -> str:
    body = match.group(1)
    indent = match.group(2)
    edges = [ln.strip() for ln in body.splitlines() if ln.strip().startswith("edge(")]
    if not edges:
        return match.group(0)
    kept = [ln for ln in body.splitlines() if not ln.strip().startswith("edge(")]
    # 去掉 kept 里可能的尾部空行
    while kept and not kept[-1].strip():
        kept.pop()
    new_body = "\n".join(kept)
    edge_lines = "\n".join(f"{indent}    {e}" for e in edges)
    return f"nodes = listOf({new_body}\n{indent}),\n{indent}edges = listOf(\n{edge_lines}\n{indent}),\n"


new_text = pattern.sub(fix, text)
PATH.write_text(new_text, encoding="utf-8")
print("已修正：", new_text.count("edges = listOf("), "处 edges 块")
