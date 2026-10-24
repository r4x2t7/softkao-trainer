import json, os
p = r"E:\ai coding\pojie\softkao-trainer\question-bank\multimedia-designer\kp_parts"
d = json.load(open(os.path.join(p, "multimedia-designer-kp-miss-00.out.json"), encoding="utf-8"))
bad = [(i, x["point"], len(x["note"]), x["note"]) for i, x in enumerate(d["notes"]) if not (40 <= len(x["note"]) <= 70)]
print("out_of_range", len(bad))
for i, pt, L, note in bad:
    print(i, "|", pt, "|", L, "|", note)
