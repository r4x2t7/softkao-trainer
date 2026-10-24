import json, os

p = r"E:\ai coding\pojie\softkao-trainer\question-bank\multimedia-designer\kp_parts"
dst = os.path.join(p, "multimedia-designer-kp-miss-00.out.json")

fix = {
 "RTSP 方法集": "RTSP 常用方法有 OPTIONS、DESCRIBE、SETUP、PLAY、PAUSE、TEARDOWN，SETUP 协商参数，PLAY 起播。",
 "三类编码技术": "视频压缩的基本编码技术是帧内编码、帧间预测（运动估计与补偿）与熵编码这三大基本类型。",
 "交叉淡化": "把两段音频首尾重叠并做渐变过渡的编辑手法称为交叉淡化（Crossfade），可避免突兀接缝。",
 "像素的信息内容": "一个像素通常记录的是颜色信息，即各颜色分量的强度数值，以及位置与透明度等附加属性信息。",
 "分辨率与容量倍数关系": "图像分辨率的长宽各提高一倍，像素总数变为四倍，未压缩的存储容量也相应变为原来的四倍。",
}

d = json.load(open(dst, encoding="utf-8"))
for x in d["notes"]:
    if x["point"] in fix:
        x["note"] = fix[x["point"]]
with open(dst, "w", encoding="utf-8") as fh:
    json.dump(d, fh, ensure_ascii=False, indent=1)

for k in fix:
    pass
bad = [(x["point"], len(x["note"])) for x in d["notes"] if not (40 <= len(x["note"]) <= 70)]
print("remaining_out_of_range", len(bad))
for b in bad:
    print(b)
print("lens", [len(fix[k]) for k in fix])
