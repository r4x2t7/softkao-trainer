# -*- coding: utf-8 -*-
"""远端题库与资料最终校验：确认 GitHub 上的内容与本地一致且完整。"""
import json
import urllib.request

BASE = "https://raw.githubusercontent.com/r4x2t7/softkao-trainer/main/question-bank"


def get(url: str) -> bytes:
    return urllib.request.urlopen(url, timeout=60).read()


for exam in ("system-integration", "multimedia-designer"):
    bank = json.loads(get(f"{BASE}/{exam}/v2.json").decode("utf-8"))
    mat = json.loads(get(f"{BASE}/materials/{exam}.json").decode("utf-8"))
    qs = bank["questions"]
    ai = sum(1 for q in qs if q.get("aiExplanation"))
    expl = sum(1 for q in qs if len(q.get("explanation") or "") >= 20)
    chapters = {q.get("chapter") for q in qs}
    mch = {c["chapter"] for c in mat["chapters"]}
    print(
        f"{exam}: {len(qs)} 题 | 带AI解析 {ai} | 解析>=20字 {expl} | 题库章 {len(chapters)} | 资料章 {len(mch)} | 资料覆盖率 {len(chapters & mch)}/{len(chapters)}"
    )
print("远端内容校验完成")
