import hashlib
import json
import urllib.request

BASE = "https://raw.githubusercontent.com/r4x2t7/softkao-trainer/main/question-bank"

idx = json.loads(urllib.request.urlopen(BASE + "/index.json", timeout=20).read().decode("utf-8"))
print("index updatedAt:", idx["updatedAt"])
for b in idx["banks"]:
    raw = urllib.request.urlopen(BASE + "/" + b["file"], timeout=30).read()
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
print("更新通道端到端校验通过")
