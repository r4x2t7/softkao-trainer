#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""在模拟器上跑一遍 UI 走查：真渲染 + 截图 + 文本断言。

这是比 Robolectric 更接近真实的软测：真的 Android 运行时、真的 WebView、
真的 SQLite/DataStore、真的触摸事件。

用法：
    python tools/ui_walkthrough.py                # 走完整流程
    python tools/ui_walkthrough.py --keep         # 结束后保留模拟器
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "build" / "ui-shots"
SDK = pathlib.Path(r"C:\Users\jw\.local\share\android-sdk")
ADB = SDK / "platform-tools" / "adb.exe"
PKG = "com.softkao.trainer"
ACT = f"{PKG}/.MainActivity"

_serial = ""
_log: list[str] = []


def adb(*args: str, binary: bool = False, timeout: int = 120):
    cmd = [str(ADB), "-s", _serial, *args]
    r = subprocess.run(cmd, capture_output=True, timeout=timeout)
    if binary:
        return r.stdout
    return r.stdout.decode("utf-8", errors="replace")


def sh(cmd: str, timeout: int = 120) -> str:
    return adb("shell", cmd, timeout=timeout)


def log(msg: str) -> None:
    print(msg, flush=True)
    _log.append(msg)


def pick_emulator() -> str:
    """只认 emulator-* 的模拟器，绝不碰插着的实体机。"""
    out = subprocess.run([str(ADB), "devices"], capture_output=True).stdout.decode("utf-8", "replace")
    for line in out.split("\n")[1:]:
        line = line.strip()
        if not line or "offline" in line or "unauthorized" in line:
            continue
        serial = line.split()[0]
        if serial.startswith("emulator-"):
            return serial
    return ""


def shot(name: str) -> pathlib.Path:
    OUT.mkdir(parents=True, exist_ok=True)
    p = OUT / f"{name}.png"
    data = adb("exec-out", "screencap", "-p", binary=True)
    p.write_bytes(data)
    log(f"  [截图] {p.name} ({len(data)//1024} KB)")
    return p


def dump() -> str:
    sh("uiautomator dump /sdcard/ui.xml")
    return sh("cat /sdcard/ui.xml")


def texts(xml: str) -> list[str]:
    out = []
    for node in ET.fromstring(xml).iter("node"):
        t = node.get("text", "").strip()
        if t:
            out.append(t)
    return out


def find(xml: str, needle: str) -> tuple[int, int] | None:
    for node in ET.fromstring(xml).iter("node"):
        if needle in node.get("text", ""):
            b = node.get("bounds", "")
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
            if m:
                x1, y1, x2, y2 = (int(v) for v in m.groups())
                return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None


def tap_text(xml: str, needle: str) -> bool:
    pos = find(xml, needle)
    if not pos:
        return False
    sh(f"input tap {pos[0]} {pos[1]}")
    time.sleep(1.2)
    return True


def tap(x: int, y: int, wait: float = 1.0) -> None:
    sh(f"input tap {x} {y}")
    time.sleep(wait)


def swipe(x1: int, y1: int, x2: int, y2: int, ms: int = 320, wait: float = 1.0) -> None:
    sh(f"input swipe {x1} {y1} {x2} {y2} {ms}")
    time.sleep(wait)


def back() -> None:
    sh("input keyevent 4")
    time.sleep(1.0)


# 底部导航栏 5 个 tab 的横向位置比例（按 1080 宽实测：首页/知识点/错题/统计/设置）
TAB_X = (0.09, 0.29, 0.50, 0.71, 0.91)
TAB_Y = 0.952


def goto_tab(xml: str, name: str, W: int, H: int) -> bool:
    """用底部导航栏坐标切页。

    不能用文本匹配：首页搜索框的提示文字里也含「知识点」，
    按文本点会跳到搜索页而不是知识点页。
    """
    idx = {"首页": 0, "知识点": 1, "错题": 2, "统计": 3, "设置": 4}.get(name)
    if idx is None:
        return False
    tap(int(W * TAB_X[idx]), int(H * TAB_Y), wait=2.0)
    return True


def check(name: str, ok: bool, detail: str = "") -> bool:
    log(f"  {'PASS' if ok else 'FAIL'}  {name}{(' — ' + detail) if detail else ''}")
    return ok


def ink_ratio(png: pathlib.Path, y0: int, y1: int, x0: int = 0, x1: int = 1080) -> float:
    """统计指定区域里「非背景色」的像素占比，用来判断图解是不是真的画出来了。"""
    sys.path.insert(0, str(ROOT / "tools"))
    from measure_shots import png_pixels  # noqa: PLC0415

    w, h, rows = png_pixels(png)
    if not rows:
        return 0.0
    bg = rows[0][:3]
    total = 0
    ink = 0
    for y in range(max(0, y0), min(h, y1)):
        row = rows[y]
        for x in range(max(0, x0), min(w, x1)):
            total += 1
            px = row[x * 3: x * 3 + 3]
            if px != bg:
                ink += 1
    return ink / total if total else 0.0


# 趋势图柱体渐变的两端色 + 答对段薄荷绿
BAR_COLORS = ((139, 92, 246), (34, 211, 238), (52, 211, 153), (5, 150, 105))


def bar_ratio(png: pathlib.Path, y0: int, y1: int, x0: int = 0, x1: int = 1080, tol: int = 46) -> float:
    """统计区域里「柱体颜色」的像素占比。

    图表是 Canvas 自绘的，语义树里看不到任何柱子，
    只能靠像素来确认柱子真的画出来了，而不是一片空白。
    """
    sys.path.insert(0, str(ROOT / "tools"))
    from measure_shots import png_pixels  # noqa: PLC0415

    w, h, rows = png_pixels(png)
    if not rows:
        return 0.0
    total = 0
    hit = 0
    for y in range(max(0, y0), min(h, y1)):
        row = rows[y]
        for x in range(max(0, x0), min(w, x1)):
            total += 1
            r, g, b = row[x * 3], row[x * 3 + 1], row[x * 3 + 2]
            if any(abs(r - c[0]) < tol and abs(g - c[1]) < tol and abs(b - c[2]) < tol for c in BAR_COLORS):
                hit += 1
    return hit / total if total else 0.0


def first_truefalse_index() -> int:
    """从题库里算出第一道判断题在顺序练习中的序号（1 起）。

    这样不用靠「一直翻页碰运气」，还能顺带测答题卡的分批翻页。
    """
    p = ROOT / "question-bank" / "system-integration" / "v2.json"
    try:
        bank = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return 0
    for i, q in enumerate(bank.get("questions", []), 1):
        if q.get("type") == "truefalse":
            return i
    return 0


def wait_for_text(needle: str, tries: int = 12, delay: float = 1.2) -> tuple[str, bool]:
    """等某个文字出现（页面加载可能要几秒）。"""
    xml = ""
    for _ in range(tries):
        xml = dump()
        if any(needle in t for t in texts(xml)):
            return xml, True
        time.sleep(delay)
    return xml, False


def scroll_to_text(xml: str, needle: str, times: int = 6) -> tuple[str, bool]:
    """反复上滑，直到语义树里出现目标文字。"""
    for _ in range(times):
        if any(needle in t for t in texts(xml)):
            return xml, True
        swipe(540, 1900, 540, 900, 260, 1.0)
        xml = dump()
    return xml, any(needle in t for t in texts(xml))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--keep", action="store_true", help="结束后保留模拟器")
    args = ap.parse_args()

    global _serial
    _serial = pick_emulator()
    if not _serial:
        print("找不到模拟器（emulator-XXXX）")
        return 2
    log(f"目标设备：{_serial}")

    size = sh("wm size")
    log(f"屏幕：{size.strip()}")
    m = re.search(r"(\d+)x(\d+)", size)
    W, H = (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)

    sh(f"am force-stop {PKG}")
    sh(f"am start -n {ACT}")
    # 冷启动要等首页内容真的渲染出来（刚装完 APK 时可能要好几秒，不能死等固定时长）
    xml, home_ready = wait_for_text("今日", tries=30, delay=1.0)
    shot("01-首页")

    passed = 0
    failed = 0

    def record(ok: bool) -> None:
        nonlocal passed, failed
        if ok:
            passed += 1
        else:
            failed += 1

    # ---------------- 首页
    ts = texts(xml)
    log(f"[首页] 文本节点 {len(ts)} 个")
    record(check("首页加载完成", home_ready))
    record(check("首页出现「今日」", any("今日" in t for t in ts)))
    record(check("首页出现科目切换", any("系统集成" in t or "多媒体" in t for t in ts)))
    record(check("首页出现练习入口", any("顺序练习" in t for t in ts)))

    # ---------------- 知识点页
    goto_tab(xml, "知识点", W, H)
    xml = dump()
    shot("02-知识点-章节横滑")
    ts = texts(xml)
    record(check("知识点页出现章节卡片", any("章" in t for t in ts)))
    record(check("知识点页出现图解库入口", any("图解库" in t for t in ts)))
    before = texts(xml)
    # 章节列表现在是纵向的：上下滑动翻页
    swipe(540, 1800, 540, 900, 260, 1.5)
    xml = dump()
    after = texts(xml)
    record(check("章节列表可以上下滑动", before != after, f"滑动前后文本 {len(before)}→{len(after)}"))

    # 进章节详情：按章节名点（不要用「章」去匹配，会命中「章节（36 章）」这个标题）
    chapter_names = [t for t in texts(xml) if t.endswith("管理") and len(t) > 4]
    card = find(xml, chapter_names[0]) if chapter_names else find(xml, "管理")
    if card:
        tap(card[0], card[1], wait=2.5)
        xml, ok = wait_for_text("本章概览", tries=8)
        record(check("章节详情打开了", ok))
        shot("04-章节详情")
        ts = texts(xml)
        record(check("章节详情出现「练习本章」", any("练习本章" in t for t in ts)))
        b4 = texts(xml)
        swipe(540, 1800, 540, 900, 260, 1.5)
        xml = dump()
        a4 = texts(xml)
        record(check("知识点清单可上下滑动", b4 != a4))
        shot("05-章节详情-知识点横滑")
        # 「知识点清单」在页面下方，uiautomator 只报可见节点，要滚下去才看得到
        xml, found = scroll_to_text(xml, "知识点清单")
        record(check("章节详情出现「知识点清单」", found))
        if found:
            shot("05b-章节详情-知识点清单")
        back()

    # ---------------- 图解库
    xml = dump()
    if tap_text(xml, "图解库"):
        time.sleep(3)
        xml = dump()
        shot("06-图解库")
        ts = texts(xml)
        record(check("图解库有卡片", len(ts) > 3, f"文本 {len(ts)} 个"))
        # 点开一张图解（真 WebView）：必须真的画出图形，不能只有标题
        cards = [t for t in texts(xml) if t and t not in ("图解库", "← 返回", "全部") and "（" not in t and len(t) > 3]
        if cards and tap_text(xml, cards[0]):
            time.sleep(5)
            p = shot("07-图解详情-WebView")
            xml = dump()
            ts = texts(xml)
            record(check("图解详情页有标题", len(ts) > 0, f"文本 {ts[:2]}"))
            ratio = ink_ratio(p, 420, 1250)
            record(check("图解真的画出来了（不是空白）", ratio > 0.02, f"图形区非背景像素占比 {ratio:.1%}"))
            back()
            time.sleep(1)

    # ---------------- 练习页（判断题 + 跳转栏 + 答题卡）
    goto_tab(xml, "首页", W, H)
    xml = dump()
    if tap_text(xml, "顺序练习"):
        pass
    xml, ok = wait_for_text("提交答案", tries=20)
    record(check("刷题页加载出来", ok))
    shot("08-刷题页")
    ts = texts(xml)
    record(check("刷题页有题干与选项", any("提交答案" in t for t in ts)))
    record(check("刷题页出现跳转栏（两行全显示）", any("答题卡" in t for t in ts) and any("尾页" in t for t in ts),
               f"{[t for t in ts if '答题卡' in t or '尾页' in t or '首页' in t]}"))
    record(check("刷题页出现题号", any("题" in t for t in ts)))

    # 答题卡：先开一次，顺便验证分批
    xml = dump()
    if tap_text(xml, "答题卡"):
        time.sleep(1.8)
        shot("10-答题卡")
        xml = dump()
        ts = texts(xml)
        record(check("答题卡弹出且分批", any("共" in t and "题" in t for t in ts), f"{ts[:2]}"))
        # 跳到那道判断题：翻到它所在的批，再点题号
        tf = first_truefalse_index()
        if tf:
            batch = (tf - 1) // 30
            for _ in range(batch):
                if not tap_text(xml, "下一批"):
                    break
                time.sleep(0.6)
                xml = dump()
            xml = dump()
            if tap_text(xml, str(tf)):
                time.sleep(2.0)
                xml = dump()
                ts = texts(xml)
                record(check("判断题渲染出 √/× 按钮", any("√" in t for t in ts), f"{[t for t in ts if '√' in t or '×' in t]}"))
                shot("09-判断题")
                tap_text(xml, "√")
                time.sleep(1.2)
                shot("09b-判断题已选")
            else:
                record(check("判断题渲染出 √/× 按钮", False, f"答题卡里没找到第 {tf} 题"))
        back()
    else:
        record(check("答题卡弹出且分批", False, "找不到答题卡按钮"))
        record(check("判断题渲染出 √/× 按钮", False, "没进到答题卡"))

    # ---------------- 选题页（按章节 / 按知识点）
    goto_tab(xml, "首页", W, H)
    xml = dump()
    # 要精确匹配按钮文案：页面上还有一行标题「按章节 / 按知识点练习」，
    # 按包含匹配会点到那行标题上，什么都不会发生
    if tap_text(xml, "按章节（36）"):
        time.sleep(2.5)
        xml = dump()
        shot("14-选题页-章节")
        ts = texts(xml)
        record(check("选题页打开了", any("选择练习范围" in t for t in ts), f"{ts[:3]}"))
        record(check("选题页有章节列表", any("练本章" in t for t in ts)))
        record(check("选题页有知识点 Tab", any("按知识点" in t for t in ts)))
        if tap_text(xml, "按知识点（1198）"):
            time.sleep(2)
            xml = dump()
            shot("15-选题页-知识点")
            record(check("知识点列表带搜索框", any("搜索知识点" in t for t in texts(xml))))
            record(check("知识点数量正确", any("1198" in t for t in texts(xml)), f"{[t for t in texts(xml) if '1198' in t]}"))
        back()
        time.sleep(1)

    # ---------------- 模拟卷：交卷前不给解析
    goto_tab(xml, "首页", W, H)
    xml = dump()
    if tap_text(xml, "模拟卷"):
        time.sleep(5)
        xml = dump()
        record(check("模拟卷进入成功", any("模拟卷" in t for t in texts(xml))))
        # 选一个答案并提交，看不到解析
        opts = [t for t in texts(xml) if len(t) > 2 and t not in ("← 返回", "★ 收藏")]
        for o in opts[:6]:
            if tap_text(xml, o):
                break
        time.sleep(0.8)
        xml = dump()
        if tap_text(xml, "提交本题"):
            time.sleep(2.5)
            xml = dump()
            ts = texts(xml)
            record(check("模拟卷提交后不显示答案解析", not any("正确答案" in t for t in ts), f"{[t for t in ts if '正确答案' in t or '解析' in t]}"))
            record(check("模拟卷提示交卷", any("交卷" in t for t in ts), f"{[t for t in ts if '交卷' in t]}"))
            shot("16-模拟卷-提交后")
        back()
        time.sleep(1)

    # ---------------- 错题本 / 统计 / 设置
    # 注意：别在 tab 页按返回键——那会直接退出 App，后面几步就全找不到页面了
    for name, key in (("11-错题本", "错题"), ("13-设置", "设置")):
        goto_tab(xml, key, W, H)
        xml = dump()
        shot(name)
        ts = texts(xml)
        record(check(f"{key}页可打开", len(ts) > 2, f"文本 {len(ts)} 个"))

    # ---------------- 统计页：趋势图表
    goto_tab(xml, "统计", W, H)
    xml = dump()
    p = shot("12-统计")
    ts = texts(xml)
    record(check("统计页可打开", len(ts) > 2, f"文本 {len(ts)} 个"))
    record(check("统计页图例齐全（做题量/答对数/趋势）",
                 any("做题量" in t for t in ts) and any("答对数" in t for t in ts) and any("趋势" in t for t in ts),
                 f"{[t for t in ts if t in ('做题量', '答对数', '趋势')]}"))
    record(check("统计页有图表说明文案", any("点柱子看当天明细" in t for t in ts)))
    # 图表是 Canvas 画的，语义树里看不到柱子，只能靠像素确认真的画出来了
    legend = find(xml, "做题量")
    if legend:
        ratio = bar_ratio(p, legend[1] - 620, legend[1] - 40)
        record(check("趋势图画出了彩色柱子", ratio > 0.002, f"图表区柱体色像素占比 {ratio:.2%}"))
    else:
        record(check("趋势图画出了彩色柱子", False, "找不到图例"))

    log("")
    log(f"通过 {passed} 项，失败 {failed} 项")
    (OUT / "walkthrough.log").write_text("\n".join(_log), encoding="utf-8")
    log(f"截图目录：{OUT}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
