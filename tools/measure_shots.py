#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""量一下渲染出来的图解 PNG：尺寸 + 内容实际占用的宽度。

用来判断图解在窄屏上是不是被裁掉了。

用法：
    python tools/measure_shots.py [目录]
"""
from __future__ import annotations

import pathlib
import struct
import sys
import zlib

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass


def png_size(path: pathlib.Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return (0, 0)
    w, h = struct.unpack(">II", data[16:24])
    return (w, h)


def png_pixels(path: pathlib.Path):
    """解码 PNG（只支持 8bit RGB/RGBA，非隔行），返回 (w,h,rows)。"""
    data = path.read_bytes()
    pos = 8
    idat = b""
    w = h = 0
    bitdepth = colortype = 0
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        ctype = data[pos + 4:pos + 8]
        chunk = data[pos + 8:pos + 8 + length]
        if ctype == b"IHDR":
            w, h, bitdepth, colortype = struct.unpack(">IIBB", chunk[:10])
        elif ctype == b"IDAT":
            idat += chunk
        elif ctype == b"IEND":
            break
        pos += 12 + length
    if bitdepth != 8 or colortype not in (2, 6):
        return (w, h, [])
    channels = 3 if colortype == 2 else 4
    raw = zlib.decompress(idat)
    stride = w * channels
    rows = []
    prev = bytearray(stride)
    i = 0
    for _ in range(h):
        f = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        if f == 1:
            for x in range(channels, stride):
                line[x] = (line[x] + line[x - channels]) & 0xFF
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif f == 3:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
        elif f == 4:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                b = prev[x]
                c = prev[x - channels] if x >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        rows.append(bytes(line))
        prev = line
    return (w, h, rows)


def content_width(path: pathlib.Path) -> tuple[int, int]:
    """返回内容最右边界像素、最后一行非背景像素行。"""
    w, h, rows = png_pixels(path)
    if not rows:
        return (0, 0)
    bg = rows[0][:3]
    right = 0
    bottom = 0
    for y, row in enumerate(rows):
        last = 0
        for x in range(w):
            px = row[x * 3: x * 3 + 3]
            if px != bg:
                last = x
        if last > 0:
            bottom = y
            right = max(right, last)
    return (right, bottom)


def main() -> int:
    d = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(
        r"E:\ai coding\pojie\softkao-trainer\build\visual-shots"
    )
    files = sorted(d.glob("*.png"))
    if not files:
        print("没有截图")
        return 1
    print(f"{'文件':<42} {'画布':>12} {'内容右边界':>10} {'内容底边':>10}")
    clipped = []
    for f in files:
        w, h = png_size(f)
        right, bottom = content_width(f)
        flag = ""
        # 内容右边界贴到画布边缘 = 右边被裁
        if right >= w - 2:
            flag = "  <== 右侧被裁"
            clipped.append(f.name)
        print(f"{f.name:<42} {f'{w}x{h}':>12} {right:>10} {bottom:>10}{flag}")
    print(f"\n共 {len(files)} 张，右侧被裁 {len(clipped)} 张")
    if clipped:
        for c in clipped[:20]:
            print("  " + c)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
