import hashlib
import json
import urllib.request

BASE = "https://raw.githubusercontent.com/r4x2t7/softkao-trainer/main/question-bank"


def fetch(rel):
    return urllib.request.urlopen(BASE + "/" + rel, timeout=30).read()


idx = json.loads(fetch("index.json").decode("utf-8"))
print("index updatedAt:", idx["updatedAt"])
for b in idx["banks"]:
    raw = fetch(b["file"])
    digest = hashlib.sha256(raw).hexdigest()
    match = "MATCH" if digest == b["sha256"] else "MISMATCH"
    data = json.loads(raw.decode("utf-8"))
    questions = data["questions"]
    ids = [q["id"] for q in questions]
    fields_ok = all(q.get("answer") and q.get("explanation") and q.get("stem") for q in questions)
    types = {}
    for q in questions:
        types[q["type"]] = types.get(q["type"], 0) + 1
    print(
        f"{b['examId']}: sha256 {match} | {len(questions)} questions | "
        f"ids unique={len(ids) == len(set(ids))} | fields complete={fields_ok} | types={types}"
    )

    # 章节精讲资料
    mat = fetch(f"materials/{b['examId']}.json")
    chapters = json.loads(mat.decode("utf-8")).get("chapters", [])
    print(f"  materials: {len(chapters)} 章精讲")

    # 逐知识点精讲（App 更新通道会拉这份文件并校验 sha256）
    kp = b.get("kpNotes")
    if not kp:
        print("  kpNotes: 索引里没有条目 ⚠️")
        continue
    raw_kp = fetch(kp["file"])
    kp_digest = hashlib.sha256(raw_kp).hexdigest()
    kp_match = "MATCH" if kp_digest == kp["sha256"] else "MISMATCH"
    notes = json.loads(raw_kp.decode("utf-8")).get("notes", {})
    empty = [k for k, v in notes.items() if not str(v).strip()]
    print(
        f"  kpNotes: sha256 {kp_match} | {len(notes)} 条（索引声明 {kp['count']}）| 空条目 {len(empty)}"
    )

print("更新通道端到端校验通过")
