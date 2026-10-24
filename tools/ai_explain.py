# -*- coding: utf-8 -*-
"""用本地配置的 LLM 为每道题生成 AI 深度解析，写入题库的 aiExplanation 字段。

设计要点
- 复用 llm-call 技能的 LLMCaller（读取本机 config.yaml，密钥不落盘到仓库）
- 结果先写入 sidecar：question-bank/<examId>/ai_explanations.json，可断点续跑
- 最后由 --merge 把 sidecar 合并进题库的 aiExplanation 并重建索引
- 模型若认为题目/答案有误，会写入 issues 供人工复核

用法：
    python tools/ai_explain.py --dry 3          # 只试跑 3 道，看输出
    python tools/ai_explain.py --exam system-integration
    python tools/ai_explain.py --merge          # 合并 sidecar 并重建索引
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import pathlib
import re
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
SKILL_SCRIPT = pathlib.Path(r"C:\Users\jw\.minimax\.builtin-skills\llm-call\scripts\llm_call.py")
BANK_DIR = ROOT / "question-bank"

EXAMS = {
    "system-integration": "系统集成项目管理工程师",
    "multimedia-designer": "多媒体应用设计师",
}

SYSTEM_PROMPT = (
    "你是资深软考辅导老师，为每一道题撰写「深度解析」。"
    "要求：\n"
    "1. 结论先行：第一句直接给出判断依据与结论；\n"
    "2. 给出依据与推导：引用教材/标准/公式的具体出处或推理过程，计算题必须写出算式与结果；\n"
    "3. 指出易错点：说明常见错误选项为什么错、考生容易混淆的地方；\n"
    "4. 中文，160~280 字；\n"
    "5. 不要复述题干，不要使用 Markdown 标题或列表符号，直接输出连贯中文段落；\n"
    "6. 如果你认为题干或正确答案本身有误，在解析末尾用「【存疑】」开头说明理由。"
)

USER_TEMPLATE = """请为下面 {n} 道题分别撰写深度解析。

严格输出 JSON 数组，不要任何额外文字、说明或 Markdown 代码块，格式：
[{{"id":"题目ID","ai":"解析正文"}},{{"id":"题目ID","ai":"解析正文"}}]

{questions}"""


def load_caller():
    spec = importlib.util.spec_from_file_location("llm_call", SKILL_SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)

    def load_config(path=None):
        p = pathlib.Path(path) if path else mod.DEFAULT_CONFIG_PATH
        with open(p, encoding="utf-8") as f:
            return yaml.safe_load(f)

    mod.load_config = load_config
    return mod.LLMCaller()


def question_block(q: dict) -> str:
    lines = [f"[{q['id']}]"]
    lines.append(f"题型：{q.get('type')}｜章节：{q.get('chapter')}｜考点：{q.get('knowledgePoint')}")
    if q.get("caseStudy"):
        lines.append(f"案例背景：{q['caseStudy']}")
    lines.append(f"题干：{q.get('stem')}")
    for o in q.get("options") or []:
        lines.append(f"  {o.get('key')}. {o.get('text')}")
    lines.append(f"正确答案：{''.join(q.get('answer') or [])}")
    if q.get("explanation"):
        lines.append(f"已有解析（供参考，可修正与扩展）：{q['explanation']}")
    if q.get("subQuestions"):
        for i, sq in enumerate(q.get("subQuestions") or [], 1):
            lines.append(f"  小问{i}：{sq.get('q')}｜参考答案：{sq.get('answer')}")
    return "\n".join(lines)


def parse_json_array(text: str) -> list[dict]:
    t = text.strip()
    t = re.sub(r"^```(?:json)?\s*", "", t)
    t = re.sub(r"\s*```$", "", t)
    start, end = t.find("["), t.rfind("]")
    if start == -1 or end == -1:
        return []
    try:
        data = json.loads(t[start:end + 1])
    except Exception:
        # 容错：修掉常见的尾逗号
        try:
            fixed = re.sub(r",\s*([\]}])", r"\1", t[start:end + 1])
            data = json.loads(fixed)
        except Exception:
            return []
    return data if isinstance(data, list) else []


def call_batch(caller, model, questions: list[dict], timeout: float = 240.0) -> list[dict]:
    body = "\n\n".join(question_block(q) for q in questions)
    prompt = USER_TEMPLATE.format(n=len(questions), questions=body)
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": prompt},
    ]
    try:
        text = caller.call(
            model_ref=model,
            messages=messages,
            max_tokens=8192,
            temperature=0.4,
            timeout=timeout,
        )
    except Exception as exc:  # noqa: BLE001
        print(f"    [call error] {exc}")
        return []
    return parse_json_array(text if isinstance(text, str) else str(text))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--exam", default="system-integration")
    ap.add_argument("--model", default="minimax/MiniMax-M3.1-Flash")
    ap.add_argument("--batch", type=int, default=6)
    ap.add_argument("--workers", type=int, default=10)
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--dry", type=int, default=0, help="只试跑 N 道并打印结果")
    ap.add_argument("--merge", action="store_true")
    args = ap.parse_args()

    if args.merge:
        return merge_sidecars()

    exam_id = args.exam
    bank_path = BANK_DIR / exam_id / "v2.json"
    bank = json.loads(bank_path.read_text(encoding="utf-8"))
    questions = bank["questions"]

    sidecar_path = BANK_DIR / exam_id / "ai_explanations.json"
    done: dict[str, str] = {}
    issues: list[dict] = []
    if sidecar_path.exists():
        old = json.loads(sidecar_path.read_text(encoding="utf-8"))
        done = old.get("explanations", {})
        issues = old.get("issues", [])

    todo = [q for q in questions if q["id"] not in done]
    if args.dry:
        todo = todo[: args.dry]
    elif args.limit:
        todo = todo[: args.limit]
    print(f"[{exam_id}] 待解析 {len(todo)} 题（已完成 {len(done)}）｜模型 {args.model}｜批大小 {args.batch}")

    caller = load_caller()
    batches = [todo[i:i + args.batch] for i in range(0, len(todo), args.batch)]

    lock = threading.Lock()
    t0 = time.time()
    processed = 0
    failed = 0

    def work(batch):
        out = call_batch(caller, args.model, batch)
        return batch, out

    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(work, b) for b in batches]
        for fut in as_completed(futures):
            batch, out = fut.result()
            with lock:
                for item in out:
                    qid = str(item.get("id", "")).strip()
                    text = str(item.get("ai", "")).strip()
                    if not qid or not text:
                        continue
                    if "【存疑】" in text:
                        issues.append({"id": qid, "note": text[:600]})
                    done[qid] = text
                processed += len(batch)
                failed += sum(1 for b in batch if not any(str(i.get("id")) in done for i in out))
                if processed % 60 < args.batch:
                    rate = processed / max(time.time() - t0, 0.1)
                    print(f"    进度 {processed}/{len(todo)}（{rate:.1f} 题/秒），已完成解析 {len(done)}，失败批次 {failed}")
                payload = {"examId": exam_id, "model": args.model, "explanations": done, "issues": issues}
                sidecar_path.write_text(json.dumps(payload, ensure_ascii=False, indent=1), encoding="utf-8")

    print(f"[{exam_id}] 完成：解析 {len(done)} 题，用时 {time.time() - t0:.0f}s，失败批次 {failed}，存疑 {len(issues)}")
    sidecar_path.write_text(json.dumps(
        {"examId": exam_id, "model": args.model, "explanations": done, "issues": issues},
        ensure_ascii=False, indent=1), encoding="utf-8")

    if args.dry:
        for qid in list(done)[-args.dry:]:
            print("=" * 60)
            print(qid, "->", done[qid][:600])
    return 0


def merge_sidecars() -> int:
    """把 sidecar 里的 AI 解析写回题库的 aiExplanation 字段"""
    import subprocess

    for exam_id in EXAMS:
        sidecar = BANK_DIR / exam_id / "ai_explanations.json"
        bank_path = BANK_DIR / exam_id / "v2.json"
        if not sidecar.exists() or not bank_path.exists():
            print(f"[{exam_id}] 缺少 sidecar 或题库，跳过")
            continue
        payload = json.loads(sidecar.read_text(encoding="utf-8"))
        explanations = payload.get("explanations", {})
        bank = json.loads(bank_path.read_text(encoding="utf-8"))
        hit = 0
        for q in bank["questions"]:
            text = explanations.get(q["id"])
            if text:
                q["aiExplanation"] = text
                hit += 1
        bank_path.write_text(json.dumps(bank, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"[{exam_id}] 已写入 AI 解析 {hit}/{len(bank['questions'])} 题")
        issues = payload.get("issues", [])
        if issues:
            ip = BANK_DIR / exam_id / "ai_issues.json"
            ip.write_text(json.dumps(issues, ensure_ascii=False, indent=2), encoding="utf-8")
            print(f"         模型标记存疑 {len(issues)} 处 → {ip.name}")

    print("\n重建索引与内置种子…")
    subprocess.run([sys.executable, str(ROOT / "tools" / "build_index.py")], cwd=str(ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
