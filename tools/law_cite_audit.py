#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""审计题库与知识点精讲里的法条引用，并与本地法条全文对照。

做法：
1. 从题目（题干/选项/解析/AI解析）和知识点精讲里抓出「《X法》第N条」这类引用；
2. 在 docs/research/laws/ 的法条全文里定位同一条，附上原文，方便判断引对了没有。

用法：
    python tools/law_cite_audit.py             # 全部
    python tools/law_cite_audit.py 专利法      # 只看某部法
"""
from __future__ import annotations

import json
import pathlib
import re
import sys
from collections import defaultdict

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"
LAWS = ROOT / "docs" / "research" / "laws"

CN = "零一二三四五六七八九十百千"
CITE_RE = re.compile(rf"(《[^》]{{0,24}}法[^》]{{0,12}}》?)\s*第\s*([{CN}\d]{{1,8}})\s*条")


def load_law_texts() -> dict[str, str]:
    out = {}
    if not LAWS.exists():
        return out
    for p in LAWS.glob("*.txt"):
        out[p.stem] = p.read_text(encoding="utf-8", errors="replace")
    return out


def law_key(name: str) -> str:
    """把《专利法》这类简称映射到本地文件名里的关键字。"""
    return name.strip("《》").replace("中华人民共和国", "")


def find_article(text: str, article: str) -> str:
    """在法条全文里找「第X条」开头的那一段。"""
    pat = re.compile(rf"第\s*{re.escape(article)}\s*条(.{{0,420}})", re.S)
    m = pat.search(text)
    if not m:
        return ""
    body = re.sub(r"\s+", " ", m.group(1)).strip()
    return body[:300]


def main() -> int:
    want = sys.argv[1] if len(sys.argv) > 1 else ""
    compact = "--compact" in sys.argv
    law_texts = load_law_texts()

    cites: dict[tuple[str, str], list[str]] = defaultdict(list)

    def scan(label: str, text: str) -> None:
        if not text:
            return
        for law, art in CITE_RE.findall(text):
            key = law_key(law)
            if want and want not in law and want not in key:
                continue
            snippet = re.sub(r"\s+", " ", text).strip()
            cites[(law, art)].append(f"{label}\n      {snippet[:260]}")

    for exam in ("system-integration", "multimedia-designer"):
        kp = BANK / exam / "kp_notes.json"
        if kp.exists():
            for point, note in json.loads(kp.read_text(encoding="utf-8")).get("notes", {}).items():
                scan(f"[{exam} 精讲] {point}", note)
        bp = BANK / exam / "v2.json"
        if bp.exists():
            bank = json.loads(bp.read_text(encoding="utf-8"))
            for q in bank.get("questions", []):
                bits = [q.get("stem", ""), q.get("explanation", "") or "", q.get("aiExplanation", "") or ""]
                bits += [o.get("text", "") for o in q.get("options", [])]
                for b in q.get("subQuestions", []) or []:
                    bits.append(b.get("answer", "") or "")
                scan(f"[{exam} 题 {q.get('id')}] {q.get('knowledgePoint','')}", " ".join(x for x in bits if x))

    for (law, art), uses in sorted(cites.items(), key=lambda kv: -len(kv[1])):
        key = law_key(law)
        body = ""
        for name, text in law_texts.items():
            if key and key in name:
                body = find_article(text, art)
                if body:
                    break
        print(f"\n===== {law} 第{art}条  （引用 {len(uses)} 处）===== law_texts={'HIT' if body else 'NO-LOCAL-TEXT'}")
        if body:
            print(f"  [法条原文] {body[:220] if compact else body}")
        else:
            print("  [法条原文] 本地无对应文本，需人工核对")
        seen = set()
        shown = 0
        for u in uses:
            if compact:
                if u in seen:
                    continue
                seen.add(u)
                head = u.split("\n")[0]
                tail = u.split("\n")[1] if "\n" in u else ""
                print(f"  {head}\n      {tail.strip()[:170]}")
                shown += 1
                if shown >= 3:
                    if len(seen) < len(uses):
                        print(f"  ...（另有 {len(uses) - len(seen)} 处同类引用）")
                    break
            else:
                print("  " + u)
    print(f"\n合计 {len(cites)} 组法条引用。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
