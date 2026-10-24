#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""一次性修复工具：把 visuals_data.json 字段值内部误用的直引号换成中文书名号。"""
import io
import json
import re
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

P = "tools/visuals_data.json"
PAT = re.compile(r'^(\s*"(?:caption|title|note|label)": ")(.*)(",?)$')

src = io.open(P, encoding="utf-8").read()
lines = src.split("\n")
out = []
fixed = 0
for ln in lines:
    m = PAT.match(ln)
    if m and '"' in m.group(2):
        res = []
        open_q = True
        for ch in m.group(2):
            if ch == '"':
                res.append("\u300c" if open_q else "\u300d")
                open_q = not open_q
            else:
                res.append(ch)
        ln = m.group(1) + "".join(res) + m.group(3)
        fixed += 1
    out.append(ln)

io.open(P, "w", encoding="utf-8").write("\n".join(out))
print("fixed lines:", fixed)
d = json.load(io.open(P, encoding="utf-8"))
print("parsed OK, visuals =", len(d["visuals"]))
