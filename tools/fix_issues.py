#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按 AI 复核结论逐条修订题库。

背景：AI 深度解析阶段对 19 道题标记了「【存疑】」（题干自相矛盾、答案与题干冲突、
解析计算错误、选项口径不唯一）。本脚本把这些题改到自洽且唯一答案。

用法：
    python tools/fix_issues.py --dry     # 只打印将要做的事
    python tools/fix_issues.py           # 实际写入
"""
from __future__ import annotations

import json
import pathlib
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = pathlib.Path(__file__).resolve().parent.parent
BANK = ROOT / "question-bank"


def patch(exam: str, qid: str, **fields) -> None:
    """fields: answer / explanation / stem / option_text={key: 新文本} / add_note"""
    path = BANK / exam / "v2.json"
    bank = json.loads(path.read_text(encoding="utf-8"))
    for q in bank["questions"]:
        if q["id"] != qid:
            continue
        if "answer" in fields:
            q["answer"] = fields["answer"]
        if "explanation" in fields:
            q["explanation"] = fields["explanation"]
        if "stem" in fields:
            q["stem"] = fields["stem"]
        if "option_text" in fields:
            for opt in q.get("options") or []:
                if opt["key"] in fields["option_text"]:
                    opt["text"] = fields["option_text"][opt["key"]]
        break
    else:
        print(f"  [miss] {qid} 未找到")
        return
    path.write_text(json.dumps(bank, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"  [fix] {qid}")


def apply_global_replacements(exam: str, pairs: list[tuple[str, str]]) -> None:
    """对整个题库做文本替换（含小问参考答案）"""
    path = BANK / exam / "v2.json"
    raw = path.read_text(encoding="utf-8")
    changed = 0
    for old, new in pairs:
        if old in raw:
            changed += raw.count(old)
            raw = raw.replace(old, new)
    path.write_text(raw, encoding="utf-8")
    print(f"  [bulk] {exam}: 替换 {changed} 处")


SI = "system-integration"
MD = "multimedia-designer"


def main() -> int:
    dry = "--dry" in sys.argv
    if dry:
        print("=== DRY RUN，不会写文件 ===")

    print("\n[系统集成] 修正答案/解析/选项")
    patch(
        SI, "si-0017",
        stem="关键路径上的活动总时差为零。若关键路径上的某活动拖延了 5 天，且网络图中不存在其他可以吸收该延期的并行路径，则下列判断正确的是？",
        explanation="关键路径活动的总时差为零，意味着它没有任何可吸收的机动时间，其一旦拖延就会等量推迟项目完成时间。前置条件已排除其他并行路径吸收该延期的可能，因此项目总工期延长 5 天，选 A。需要注意：一旦发生延期，关键路径本身可能发生变化，这也是必须重新计算进度网络图并更新进度基准的原因。",
    )
    patch(SI, "si-0044", answer=["B"])
    patch(
        SI, "si-0150",
        answer=["C"],
        explanation="依据教程第 6 章，项目管理知识体系划分为十大知识领域：项目整合管理、项目范围管理、项目进度管理、项目成本管理、项目质量管理、项目资源管理、项目沟通管理、项目风险管理、项目采购管理和项目干系人管理。财务与审计属于组织治理与内部控制的范畴，教材并未把它列为项目管理知识领域，因此选项 C 不属于十大知识领域；选项 A、B、D 分别是整合、资源与干系人领域的正式名称，与教材表述一致。",
    )
    patch(
        SI, "si-0260",
        option_text={"B": "用户需求描述用户使用系统必须完成的任务与业务流程"},
        explanation="业务需求是组织或客户高层提出的宏观目标与期望收益，回答为什么要做这个项目；用户需求描述用户使用系统要完成的任务与业务流程，回答用户要做什么，因此 B 正确。界面与操作方式属于功能性需求的细化，开发人员提出的通常是技术需求，业务需求也不可能脱离功能，因此 A、C、D 均不正确。",
    )
    patch(
        SI, "si-0472",
        explanation="本问考查三点估算公式的套用与风险解读。计算：te=(O+4M+P)/6=(10+4×18+34)/6=116/6≈19.3 天；σ=(P-O)/6=(34-10)/6=4 天。问题：项目经理把最可能值 18 天当成了必然结果。按近似正态分布，P(X≤18)=Φ((18-19.3)/4)≈Φ(-0.33)≈37%，也就是说约有三成概率能在 18 天内完成，约有六成以上的概率会超出承诺日期；若要给出约 95% 的把握，可承诺上限应取 te+2σ=19.3+8=27.3 天。该活动位于关键路径上，一旦延误将直接推迟项目完工并形成违约与信誉风险。",
    )
    patch(
        SI, "si-0958",
        option_text={"D": "风险储备经项目经理批准后即可用于扩大项目范围"},
        explanation="应急储备包含在成本基准内，专用于应对已识别风险，动用时一般不必提交整体变更请求，但仍需审批并记录；管理储备通常单列，用于未识别出来的风险，动用需管理层批准。储备的用途被严格限定为应对风险，挪作扩大范围属于违规，因此 D 的说法错误，正确表述应是风险储备不得用于扩大项目范围或充当日常超支的挡箭牌。储备动用后要更新基准并跟踪余额。",
    )
    patch(
        SI, "si-1152",
        stem="采用成本加固定百分比合同的项目，若实际成本明显超出估算成本，采购方面临的主要风险是？",
        option_text={
            "A": "承包商利润被显著摊薄甚至亏损，从而彻底丧失控制成本的动力",
            "B": "承包商获得额外利润，而采购方承担成本超支、预算失控的风险",
        },
        answer=["B"],
        explanation="成本加固定百分比合同（CPF）的费用按实际成本乘以一个在签约时固定的百分比计算，实际成本上升时费用会同比例上升，承包商并不会因为成本超支而减少收入。相反，采购方要按更高的实际成本付费，超出估算的部分全部由采购方承担，预算因此失控，这也是 CPF 被称为采购方风险最大、承包商缺乏控本动机的合同类型。因此选 B。",
    )
    patch(
        SI, "si-1153",
        stem="判断：在成本加固定百分比合同中，承包商的实际成本上升时，其按固定百分比获得的费用会随之上升，因而采购方承担了成本超支的全部风险。",
        explanation="该说法正确。CPF 按实际成本乘以签约时固定的百分比计算费用，成本上升时费用同比例上升，承包商不会因超支而减少收费；同时因为百分比固定，承包商也缺乏主动控制成本的动力，超支部分最终由采购方承担，采购方因此承担了成本超支的全部风险。",
    )
    patch(
        SI, "si-1198",
        option_text={"B": "合法，因为 100 万元未超过招标项目估算价 2%（即 120 万元）的比例上限"},
        explanation="投标保证金受双重约束：不得超过招标项目估算价的 2%，且最高不得超过 80 万元人民币。本案招标估算价 6000 万元，2% 为 120 万元，但受 80 万元绝对上限约束，因此投标保证金最多只能是 80 万元，招标文件约定 100 万元违反上限规定，属违法约定，故选 C。选项 A 把比例与绝对上限的关系说反，选项 B 只核对了 2% 的比例上限而忽略了 80 万元的绝对上限，选项 D 无法律依据。",
    )
    patch(
        SI, "si-1215",
        stem="政府采购中，供应商认为采购文件、采购过程和中标成交结果使自己的权益受到损害的，其提出质疑与投诉的期限说法正确的是？",
        explanation="《政府采购法》第四十五条规定，供应商认为采购文件、采购过程和中标、成交结果使自己的权益受到损害的，可以在知道采购结果之日起 10 个工作日内向采购人、采购代理机构提出质疑；第四十六条规定，采购人、采购代理机构收到质疑后应当作出答复，质疑答复不满意的，供应商可以在答复期满后 15 个工作日内向同级政府采购监督管理部门提起投诉。注意法律使用的是工作日而非自然日，选项 B、C 在期限种类与数字上均不符，选项 D 则完全错误。",
    )
    patch(
        SI, "si-1322",
        option_text={"D": "证书撤销列表 CRL 用于公布即将到期、需要提前续期的证书信息"},
        explanation="PKI 是提供身份认证、密钥分发与不可否认性服务的一套体系，典型组成包括认证机构 CA、注册机构 RA、证书库、密钥管理、证书撤销机制（CRL、OCSP）与安全审计等部分，A 正确。CRL 的作用是公布已被撤销的证书信息，与到期续期无关，D 错误。PKI 的核心价值恰恰是解决身份信任问题，B 错误；数字证书须由受信任的 CA 签发，用户自签证书不具信任效力，C 错误。",
    )

    print("\n[多媒体] 修正答案/解析/选项")
    patch(
        MD, "md-0311",
        option_text={"B": "3:1"},
        explanation="码率 = 采样频率 × 量化位数 × 声道数。5.1 环绕声含 5 个全频段声道加 1 个 LFE 低频声道共 6 个，立体声为 2 个，采样频率与位深相同时码率之比只由声道数决定，为 6:2 = 3:1。48 kHz、24 bit 下两者分别为 4608 kbps 与 2304 kbps，同样是 3 倍关系，故选 B。",
    )
    patch(
        MD, "md-0417",
        option_text={"B": "NTSC 约 25 fps，主要用于北美、日本等地区的电视广播"},
        explanation="PAL 为 25 fps、625 行，是我国及欧洲、澳洲等地区的模拟电视制式，A 正确；NTSC 为约 29.97 fps、525 行，用于北美与日本，B 把帧率写成了 25 fps，错误；电影的标准帧率是 24 fps，C 错；24 fps 既不是 NTSC 也不是 PAL 的帧率，D 错。故选 A。",
    )
    patch(
        MD, "md-0421",
        answer=["A"],
        explanation="未压缩码率 = 宽×高×位深×帧率÷8 = 720×576×24×25÷8 = 31 104 000 bps ≈ 31.10 Mbps，故选 A。常见错误是误用场频 50 代替帧率 25，得出 62.21 Mbps（B）；D 项 155.52 Mbps 则是 1080p 在 24 位、25 fps 下的对应值。",
    )
    patch(
        MD, "md-0527",
        explanation="容量计算：单讲 25 分钟 = 1500 秒，文件大小(KB) ≈ 2000 kbps × 1500 s ÷ 8 = 375 000 KB ≈ 366.21 MB，20 讲合计约 7 324 MB ≈ 7.15 GiB。建议：录屏采用软件编码时优先设定恒定码率以保证录制稳定，避免码率波动导致播放卡顿；平台侧按 1080p 2 Mbps 与 720p 1 Mbps 两版输出以适配不同网络；同时保留原始录屏工程与无损中间文件用于后续修改课件，并按课程单元分目录归档、定期异地备份。",
    )
    patch(
        MD, "md-0538",
        explanation="导出方案：归档母版用 ProRes 422 HQ 或 DNxHR HQX（1080p 码率约 220 Mbps），容器 MOV，保留原始时码、多轨音频与修复记录，并归档工程文件；网络版用 H.264（Main/High Profile）、1080p、码率 4~8 Mbps、音频 AAC 128 kbps、MP4 容器，另出 720p、2~4 Mbps 移动端版本；字幕保留独立轨或外附 SRT，并按需再渲染硬字幕版。质检要点：时长、帧数与时码与台账一致；无丢帧、闪断、残留噪点与明显色偏；画面比例与构图未被误裁、字幕未被裁切且无错别字；音频电平与口型同步正常；文件命名与版本号规范、校验值一致；保留原磁带作为最高保真的长期备份。",
    )
    patch(
        MD, "md-0604",
        option_text={"B": "需要逐位还原的程序安装包 → 字典编码（LZ77 加 Huffman 熵编码）"},
        explanation="RLE 适用于重复度高的数据，灰度条带中大量像素取值相同，用游程编码极其有效；程序安装包的符号分布不均匀，用 LZ77 字典编码配合 Huffman 熵编码可取得接近熵极限的无损压缩；人像照片是自然图像，JPEG 的变换加量化加熵编码混合结构最合适。而高频振荡的音频噪声相邻样本差值大、熵高，差分预测不但收益极小还会放大量化噪声，应改用变换编码或直接做熵编码，故 D 错。",
    )
    patch(
        MD, "md-0925",
        option_text={
            "B": "证书中包含持有者的私钥，便于第三方验证持有者身份",
            "C": "证书由使用者自行签名即可生效，无需颁发机构参与",
        },
        explanation="X.509 证书包含版本号、序列号、签名算法标识、颁发者、有效期、主体信息、主体公钥及扩展项，并由颁发者私钥签名，使任何持有 CA 公钥的第三方都能验证证书未被伪造，并确认它与持有者公钥的绑定关系，这一身份与公钥的绑定正是证书的核心意义，故选 A。证书中只包含公钥而不包含私钥，B 错；证书必须由受信任的 CA 签发才具有信任效力，C 错；浏览历史与证书内容无关，D 错。",
    )
    patch(
        MD, "md-0963",
        explanation="素材清单是项目配置管理的基础台账，A 记录每项素材的来源与授权状态，使缺失、替换与版权核查有据可查，避免交付前才发现授权瑕疵；B 便于追踪缺失素材并及时补充；C 制作过程中随素材一并登记采集与处理工时，可与工时台账合并用于成本与进度核算。D 错在结算还须以合同、验收单与发票等凭据为准，素材清单不能单独作为结算凭据。",
    )

    print("\n[批量] 修正小问参考答案里的计算错误")
    apply_global_replacements(MD, [("457.76", "366.21"), ("1.6 GB", "16.5 GB"), ("9 155 MB", "7 324 MB")])

    print("\n完成。" if not dry else "\n（dry run，未写文件）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
