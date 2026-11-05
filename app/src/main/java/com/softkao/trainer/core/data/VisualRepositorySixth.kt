package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 第六批知识动画：SI 风险应对镜像 / 政府采购五方式 / 质量三过程 / 等保五级，
 * MD 色彩空间转换链 / 三种动画技术 / 转码交付流水线。
 * 选型原则：不与前三批任何主题重复（此前挣值/配置库/方格等已双份）。
 */
object VisualRepositorySixth {

    private fun box(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.2f, h: Float = 0.13f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BOX, 0f, tone, caption)

    private fun circle(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.16f, h: Float = 0.16f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.CIRCLE, 0f, tone, caption)

    private fun text(id: String, label: String, x: Float, y: Float, caption: String = "", w: Float = 0.3f, h: Float = 0.1f, tone: VisualTone = VisualTone.NEUTRAL) =
        VisualNode(id, label, x, y, w, h, VisualKind.TEXT, 0f, tone, caption)

    private fun edge(from: String, to: String, active: Boolean = false, dashed: Boolean = false, label: String = "") =
        VisualEdge(from, to, active, dashed, label)

    val lessons: List<VisualLesson> = listOf(
        // ============ SI 1：风险应对策略镜像 ============
        VisualLesson(
            id = "si-risk-response-mirror",
            title = "风险应对镜像：威胁四策略 ↔ 机会四策略",
            examId = VisualRepository.SI,
            chapter = "项目风险管理",
            keywords = listOf("风险应对", "规避", "转移", "减轻", "开拓", "分享", "提高", "机会", "威胁"),
            takeaway = "威胁（规避/转移/减轻/接受）与机会（开拓/分享/提高/接受）一一镜像：规避↔开拓（彻底消除/确保实现）、转移↔分享（甩给别人/拉人共享）、减轻↔提高（降概率影响/升概率影响）。",
            frames = listOf(
                VisualFrame(
                    "① 威胁侧：负面风险的四种应对（接受在底线兜底）",
                    nodes = listOf(
                        box("avoid", "规避\n改计划消除威胁", 0.06f, 0.1f, VisualTone.BAD, w = 0.28f, h = 0.16f),
                        box("trans", "转移\n外包/保险甩给第三方", 0.06f, 0.32f, VisualTone.BAD, w = 0.28f, h = 0.16f),
                        box("mit", "减轻\n降概率或降影响", 0.06f, 0.54f, VisualTone.BAD, w = 0.28f, h = 0.16f),
                        box("acc", "接受\n不行动，留储备", 0.06f, 0.76f, VisualTone.MUTED, w = 0.28f, h = 0.16f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 机会侧：正面风险（机会）的镜像四策略",
                    nodes = listOf(
                        box("exploit", "开拓\n确保机会 100% 实现", 0.64f, 0.1f, VisualTone.GOOD, w = 0.3f, h = 0.16f),
                        box("share", "分享\n拉合伙人共享机会", 0.64f, 0.32f, VisualTone.GOOD, w = 0.3f, h = 0.16f),
                        box("enh", "提高\n升概率或升收益", 0.64f, 0.54f, VisualTone.GOOD, w = 0.3f, h = 0.16f),
                        box("acc2", "接受\n不主动追求", 0.64f, 0.76f, VisualTone.MUTED, w = 0.3f, h = 0.16f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 镜像对齐：规避↔开拓、转移↔分享、减轻↔提高",
                    nodes = listOf(
                        text("m1", "规避 ↔ 开拓", 0.3f, 0.08f, w = 0.4f, tone = VisualTone.ACTIVE),
                        text("m2", "转移 ↔ 分享", 0.3f, 0.32f, w = 0.4f, tone = VisualTone.ACTIVE),
                        text("m3", "减轻 ↔ 提高", 0.3f, 0.56f, w = 0.4f, tone = VisualTone.ACTIVE),
                        text("note", "「接受」两侧通用", 0.32f, 0.78f, w = 0.36f, tone = VisualTone.MUTED),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 判定演练：题干关键词 → 策略对号",
                    nodes = listOf(
                        text("k1", "「确保实现」→开拓", 0.06f, 0.14f, w = 0.4f),
                        text("k2", "「联合体合作」→分享", 0.52f, 0.14f, w = 0.42f),
                        text("k3", "「买保险/外包」→转移(威胁)", 0.06f, 0.38f, w = 0.44f),
                        text("k4", "「增加资源提高胜率」→提高", 0.5f, 0.38f, w = 0.46f),
                        text("k5", "「改用成熟方案消除不确定性」→规避", 0.06f, 0.62f, w = 0.6f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 2：政府采购五方式 ============
        VisualLesson(
            id = "si-procurement-five",
            title = "政府采购五种方式：适用条件与选择逻辑",
            examId = VisualRepository.SI,
            chapter = "项目采购管理",
            keywords = listOf("政府采购", "公开招标", "邀请招标", "竞争性谈判", "单一来源", "询价", "采购方式"),
            takeaway = "公开招标是默认首选；邀请招标需特殊批准；竞争性谈判用于技术复杂或紧急；询价只用于规格统一的现货；单一来源的唯一条件是来源唯一。",
            frames = listOf(
                VisualFrame(
                    "① 默认方式：公开招标——充分竞争，改用它必须报批",
                    nodes = listOf(
                        box("pub", "公开招标\n默认首选", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.28f, h = 0.2f, caption = "不可达标的替代方式均需报财政部门批准"),
                        text("law", "法定方式五种，适用条件是判断题眼", 0.36f, 0.42f, w = 0.6f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 邀请招标与竞争性谈判：都有特定适用情形",
                    nodes = listOf(
                        box("inv", "邀请招标\n特殊情形+批准", 0.06f, 0.38f, w = 0.28f, h = 0.2f, caption = "从符合资格的供应商中随机邀请 ≥3 家"),
                        box("neg", "竞争性谈判\n技术复杂/紧急", 0.44f, 0.38f, VisualTone.GOOD, w = 0.3f, h = 0.2f, caption = "与 ≥3 家谈判"),
                        text("note1", "不能采购人所好随意切换方式", 0.3f, 0.7f, w = 0.5f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 询价与单一来源：两个极端——最多竞争 vs 零竞争",
                    nodes = listOf(
                        box("rq", "询价\n规格统一现货\n≥3 家报价比价", 0.06f, 0.36f, w = 0.3f, h = 0.26f),
                        box("single", "单一来源\n唯一供应商\n公示+审批", 0.52f, 0.36f, VisualTone.BAD, w = 0.32f, h = 0.26f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 判定演练：题眼 → 方式",
                    nodes = listOf(
                        text("j1", "「只能从唯一供应商处」→单一来源", 0.04f, 0.12f, w = 0.56f, tone = VisualTone.PATH),
                        text("j2", "「技术复杂无法确定详细规格」→竞争性谈判", 0.04f, 0.36f, w = 0.72f),
                        text("j3", "「规格统一、现货供应」→询价", 0.04f, 0.6f, w = 0.5f),
                        text("j4", "「没有特殊情形」→公开招标（默认）", 0.04f, 0.84f, w = 0.6f),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 3：质量管理三过程 ============
        VisualLesson(
            id = "si-quality-three-processes",
            title = "质量管理三过程：规划 / 管理 / 控制各管什么",
            examId = VisualRepository.SI,
            chapter = "项目质量管理",
            keywords = listOf("规划质量管理", "管理质量", "控制质量", "质量保证", "质量控制", "质量标准", "质量管理"),
            takeaway = "规划定标准与指标（团队+客户参与）；管理质量执行质量活动、审计过程（质量部门）；控制质量检测交付物（测试/QC 团队）。「定标准」是规划的事，不是管理质量的事。",
            frames = listOf(
                VisualFrame(
                    "① 规划质量管理：定标准、定指标——后续所有质量工作的依据",
                    nodes = listOf(
                        box("plan", "规划质量管理\n输出：质量标准\n质量测量指标", 0.3f, 0.36f, VisualTone.ACTIVE, w = 0.4f, h = 0.26f, caption = "项目团队+客户共同参与"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 管理质量：执行质量活动、审计过程合规、编报告促改进",
                    nodes = listOf(
                        box("plan", "质量标准", 0.04f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.16f),
                        box("ma", "管理质量\n审计过程合规\n质量报告", 0.34f, 0.38f, VisualTone.GOOD, w = 0.32f, h = 0.24f, caption = "质量部门主导"),
                        text("qa", "管的是「过程」", 0.7f, 0.42f, w = 0.28f),
                    ),
                    edges = listOf(edge("plan", "ma", active = true, label = "依据")),
                ),
                VisualFrame(
                    "③ 控制质量：检测交付物是否合格、跟踪缺陷并验证修复",
                    nodes = listOf(
                        box("plan", "质量标准", 0.04f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.16f),
                        box("qc", "控制质量\n测试/检查\n缺陷跟踪", 0.34f, 0.38f, VisualTone.BAD, w = 0.32f, h = 0.24f, caption = "测试/QC 团队主导"),
                        text("qc2", "管的是「结果」", 0.7f, 0.42f, w = 0.28f),
                    ),
                    edges = listOf(edge("plan", "qc", active = true, label = "依据")),
                ),
                VisualFrame(
                    "④ 考法提醒：「确定质量标准」属于规划，不是管理质量的活动",
                    nodes = listOf(
                        text("t1", "规划：定标准 + 定指标", 0.08f, 0.16f, w = 0.44f, tone = VisualTone.ACTIVE),
                        text("t2", "管理质量：执行活动 + 审计 + 报告", 0.08f, 0.4f, w = 0.52f, tone = VisualTone.GOOD),
                        text("t3", "控制质量：检测 + 缺陷验证", 0.08f, 0.64f, w = 0.46f, tone = VisualTone.BAD),
                        text("trap", "「找不属于管理质量的工作」→ 选「确定标准」", 0.08f, 0.88f, w = 0.6f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 4：等保五级阶梯 ============
        VisualLesson(
            id = "si-mlps-five-levels",
            title = "网络安全等级保护：五级阶梯与定级要素",
            examId = VisualRepository.SI,
            chapter = "信息安全",
            keywords = listOf("等级保护", "等保", "安全保护等级", "五级", "定级", "GB 17859"),
            takeaway = "五级从下往上：自主→审计→标记→结构化→访问验证，控制力逐级增强；定级看「受侵害客体+侵害程度」，三级以上每年密评。",
            frames = listOf(
                VisualFrame(
                    "① 一二级：用户自主保护 → 系统审计保护（一般系统）",
                    nodes = listOf(
                        box("l1", "一级 自主保护\n自主访问控制", 0.06f, 0.66f, VisualTone.MUTED, w = 0.36f, h = 0.18f),
                        box("l2", "二级 审计保护\n增加安全审计", 0.48f, 0.66f, w = 0.36f, h = 0.18f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 三级：安全标记保护——强制访问控制登场，需每年密评",
                    nodes = listOf(
                        box("l3", "三级 标记保护\n强制访问控制+标记", 0.27f, 0.62f, VisualTone.ACTIVE, w = 0.44f, h = 0.2f, caption = "关键信息系统多在此级"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 四五级：结构化保护 → 访问验证保护（最高，专控）",
                    nodes = listOf(
                        box("l4", "四级 结构化保护\n形式化安全模型", 0.06f, 0.1f, VisualTone.GOOD, w = 0.36f, h = 0.2f),
                        box("l5", "五级 访问验证\n最高专控", 0.48f, 0.1f, VisualTone.GOOD, w = 0.36f, h = 0.2f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 定级与合规动作：客体+程度两要素定级 → 专家评审 → 公安备案",
                    nodes = listOf(
                        box("g1", "受侵害客体\n+侵害程度", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.3f, h = 0.22f),
                        box("g2", "专家评审", 0.42f, 0.4f, w = 0.22f, h = 0.16f),
                        box("g3", "公安备案审查", 0.7f, 0.4f, w = 0.28f, h = 0.16f),
                        text("kv", "三级及以上：每年商用密码应用安全性评估", 0.16f, 0.7f, w = 0.7f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(edge("g1", "g2"), edge("g2", "g3")),
                ),
            ),
        ),

        // ============ MD 1：色彩空间转换链 ============
        VisualLesson(
            id = "md-color-space-chain",
            title = "色彩空间转换链：RGB / YUV / Lab 各自的岗位",
            examId = VisualRepository.MD,
            chapter = "色彩与色彩管理",
            keywords = listOf("RGB", "YUV", "Lab", "色彩空间", "转换", "连接空间", "PCS"),
            takeaway = "RGB 给屏幕（加色）、YUV 给视频传输（亮度色度分离、兼容黑白）、Lab 给跨设备转换（设备无关连接空间 PCS）。设备相关↔设备无关的分工是色彩管理的骨架。",
            frames = listOf(
                VisualFrame(
                    "① RGB：屏幕的母语——加色模型，R/G/B 光相加成白",
                    nodes = listOf(
                        box("rgb", "RGB\n加色 / 设备相关", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.3f, h = 0.22f, caption = "显示器/相机传感器"),
                        text("u1", "用途：屏幕显示、采集", 0.42f, 0.42f, w = 0.5f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② YUV：视频的运输标准——Y 亮度独立，UV 色度可压缩",
                    nodes = listOf(
                        box("yuv", "YUV\n亮度+色度分离", 0.06f, 0.38f, VisualTone.GOOD, w = 0.3f, h = 0.22f, caption = "4:2:0 只存 1/4 色度"),
                        text("u2", "用途：视频编码/传输/兼容黑白电视", 0.42f, 0.42f, w = 0.6f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ Lab：设备无关的「中转站」——色彩管理的连接空间 PCS",
                    nodes = listOf(
                        box("lab", "Lab\n设备无关", 0.06f, 0.38f, VisualTone.PATH, w = 0.3f, h = 0.22f, caption = "L 明度 / a 红绿 / b 黄蓝"),
                        text("u3", "用途：跨设备转换的中转（源→PCS→目标）", 0.42f, 0.42f, w = 0.66f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 转换链全景：源设备 Profile → PCS(Lab) → 目标设备 Profile",
                    nodes = listOf(
                        box("s1", "显示器\nRGB Profile", 0.04f, 0.4f, VisualTone.ACTIVE, w = 0.26f, h = 0.2f),
                        circle("pcs", "PCS\nLab", 0.44f, 0.4f, VisualTone.PATH, w = 0.2f, h = 0.2f),
                        box("s2", "打印机\nCMYK Profile", 0.72f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.2f),
                        text("cmm", "CMM 引擎执行转换", 0.36f, 0.14f, w = 0.4f),
                    ),
                    edges = listOf(edge("s1", "pcs", active = true), edge("pcs", "s2", active = true)),
                ),
            ),
        ),

        // ============ MD 2：三种动画技术谱系 ============
        VisualLesson(
            id = "md-animation-three-tech",
            title = "动画技术谱系：逐帧 / 补间 / 骨骼",
            examId = VisualRepository.MD,
            chapter = "动画技术",
            keywords = listOf("逐帧动画", "补间动画", "骨骼动画", "关键帧", "插值", "动画技术"),
            takeaway = "逐帧全手绘（表现力最强、工作量最大）；补间由关键帧自动插值（规则运动省力）；骨骼用骨架驱动蒙皮（角色动画复用性最高）。选型=表现力与成本的平衡。",
            frames = listOf(
                VisualFrame(
                    "① 逐帧动画：每一帧都手工绘制——表现力天花板",
                    nodes = listOf(
                        box("f1", "帧1", 0.04f, 0.4f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f),
                        box("f2", "帧2", 0.22f, 0.4f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f),
                        box("f3", "帧3", 0.4f, 0.4f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f),
                        text("cost1", "全手工：1 秒 24 张画\n工作量最大、文件大", 0.6f, 0.36f, w = 0.4f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 补间动画：只画首尾关键帧，中间自动插值",
                    nodes = listOf(
                        box("k1", "关键帧A", 0.08f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f),
                        box("mid", "自动插值…", 0.38f, 0.4f, VisualTone.MUTED, w = 0.24f, h = 0.16f),
                        box("k2", "关键帧B", 0.7f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f),
                        text("cost2", "适合位移/缩放/旋转等规则运动", 0.18f, 0.66f, w = 0.6f, tone = VisualTone.GOOD),
                    ),
                    edges = listOf(edge("k1", "mid", active = true), edge("mid", "k2", active = true)),
                ),
                VisualFrame(
                    "③ 骨骼动画：骨架绑定蒙皮，动一根骨头带动全身",
                    nodes = listOf(
                        circle("bone", "骨架\n层级绑定", 0.16f, 0.4f, VisualTone.PATH, w = 0.22f, h = 0.22f),
                        box("skin", "蒙皮角色", 0.48f, 0.38f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        text("cost3", "角色复用性最高\n适合人物/生物动作", 0.32f, 0.68f, w = 0.5f),
                    ),
                    edges = listOf(edge("bone", "skin", active = true, label = "驱动")),
                ),
                VisualFrame(
                    "④ 选型判定：复杂表演→逐帧；规则运动→补间；角色复用→骨骼",
                    nodes = listOf(
                        text("s1", "水墨晕染/复杂表演 → 逐帧", 0.06f, 0.16f, w = 0.6f),
                        text("s2", "Logo 位移/透明度变化 → 补间", 0.06f, 0.42f, w = 0.6f),
                        text("s3", "游戏角色/虚拟主播 → 骨骼", 0.06f, 0.68f, w = 0.6f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 3：转码交付流水线 ============
        VisualLesson(
            id = "md-transcode-pipeline",
            title = "转码交付流水线：从母版到多档分发",
            examId = VisualRepository.MD,
            chapter = "视频格式与制作流程",
            keywords = listOf("转码", "母版", "分发", "多码率", "交付", "H.264", "H.265", "ABR"),
            takeaway = "先出高画质母版存档（ProRes/H.265 4K），再按目标设备与信道转出分发版（4K 投影/1080p 网络/移动端各不同）；流媒体还需多码率档位供 ABR 切换。",
            frames = listOf(
                VisualFrame(
                    "① 渲染母版：最高画质一版，用于存档与后续转码的源头",
                    nodes = listOf(
                        box("master", "母版\nProRes / H.265 4K\n高码率", 0.3f, 0.38f, VisualTone.ACTIVE, w = 0.4f, h = 0.26f, caption = "永远从母版转码，不从分发版二压"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 按信道转出分发版：投影 4K / 网络 1080p / 移动端各不同",
                    nodes = listOf(
                        box("master", "母版", 0.06f, 0.4f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("d1", "4K 投影版\n高码率", 0.34f, 0.14f, VisualTone.GOOD, w = 0.28f, h = 0.16f),
                        box("d2", "网络 1080p\n中码率", 0.34f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.16f),
                        box("d3", "移动端竖版\n低码率", 0.34f, 0.66f, VisualTone.GOOD, w = 0.28f, h = 0.16f),
                    ),
                    edges = listOf(edge("master", "d1", active = true), edge("master", "d2", active = true), edge("master", "d3", active = true)),
                ),
                VisualFrame(
                    "③ 流媒体再加一档：多码率梯度供 ABR 动态切换",
                    nodes = listOf(
                        box("lad", "码率梯度\n1080p/720p/480p", 0.2f, 0.38f, VisualTone.ACTIVE, w = 0.34f, h = 0.24f, caption = "每档独立转码+切片"),
                        box("abr", "ABR 播放器\n按带宽选档", 0.64f, 0.38f, VisualTone.PATH, w = 0.3f, h = 0.22f),
                    ),
                    edges = listOf(edge("lad", "abr", active = true)),
                ),
                VisualFrame(
                    "④ 交付三件套：母版 + 分发版清单 + 真实环境试播验收",
                    nodes = listOf(
                        box("c1", "母版存档", 0.04f, 0.4f, VisualTone.GOOD, w = 0.22f, h = 0.18f),
                        box("c2", "分发版+清单", 0.32f, 0.4f, VisualTone.GOOD, w = 0.26f, h = 0.18f),
                        box("c3", "试播+验收单", 0.66f, 0.4f, VisualTone.ACTIVE, w = 0.3f, h = 0.18f, caption = "4K 投影试播防「画面模糊」事故"),
                    ),
                    edges = listOf(edge("c1", "c2"), edge("c2", "c3", active = true)),
                ),
            ),
        ),
    )
}
