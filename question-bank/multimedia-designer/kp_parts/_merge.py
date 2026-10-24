import json, os

p = r"E:\ai coding\pojie\softkao-trainer\question-bank\multimedia-designer\kp_parts"
src = os.path.join(p, "multimedia-designer-kp-miss-00.in.json")
dst = os.path.join(p, "multimedia-designer-kp-miss-00.out.json")

inp = json.load(open(src, encoding="utf-8"))
notes = []
for f in ["_part1.json", "_part2.json", "_part3.json", "_part4.json", "_part5.json"]:
    notes.extend(json.load(open(os.path.join(p, f), encoding="utf-8")))

m = {x["point"]: x["note"] for x in notes}
order = [q["point"] for q in inp["points"]]
missing = [k for k in order if k not in m]
extra = [k for k in m if k not in set(order)]
out = {"notes": [{"point": k, "note": m[k]} for k in order]}
with open(dst, "w", encoding="utf-8") as fh:
    json.dump(out, fh, ensure_ascii=False, indent=1)

print("written", len(out["notes"]))
print("missing", missing)
print("extra", extra)
print("dups", len(notes) - len(m))
