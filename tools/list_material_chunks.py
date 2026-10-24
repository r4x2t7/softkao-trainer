# -*- coding: utf-8 -*-
"""列出知识点资料分片的章节清单。"""
import glob
import json
import os
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

for f in sorted(glob.glob(r"E:\ai coding\pojie\softkao-trainer\question-bank\*\material_parts\*.in.json")):
    d = json.load(open(f, encoding="utf-8"))
    parts = []
    for c in d["chapters"]:
        parts.append("{}({})".format(c["chapter"], c["questionCount"]))
    print(os.path.basename(f), "->", "、".join(parts))
