package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 第九批知识动画：SI 五大过程组 / 干系人参与度阶梯 / CIA 三要素，
 * MD 图像格式家族 / 三维动画流程 / 印刷输出分辨率。
 */
object VisualRepositoryNinth {

    private fun box(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.2f, h: Float = 0.13f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BOX, 0f, tone, caption)

    private fun circle(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.16f, h: Float = 0.16f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.CIRCLE, 0f, tone, caption)

    private fun text(id: String, label: String, x: Float, y: Float, caption: String = "", w: Float = 0.3f, h: Float = 0.1f, tone: VisualTone = VisualTone.NEUTRAL) =
        VisualNode(id, label, x, y, w, h, VisualKind.TEXT, 0f, tone, caption)

    private fun edge(from: String, to: String, active: Boolean = false, dashed: Boolean = false, label: String = "") =
        VisualEdge(from, to, active, dashed, label)

    val lessons: List<VisualLesson> = listOf(
        // ============ SI 1：五大过程组 ============
        VisualLesson(
            id = "si-process-groups",
            title = "五大过程组全景：启动 → 规划 → 执行 → 监控 → 收尾",
            examId = VisualRepository.SI,
            chapter = "项目管理概论",
            keywords = listOf("过程组", "启动", "规划", "执行", "监控", "收尾", "项目章程", "项目管理计划"),
            takeaway = "启动出章程与识别干系人；规划出计划与三大基准；执行干工作出交付物；监控全程伴随（不是阶段）；收尾出最终产品移交与组织过程资产更新。监控贯穿始终是最高频考点。",
            frames = listOf(
                VisualFrame(
                    "① 启动过程组：正式授权项目——产出项目章程 + 识别干系人",
                    nodes = listOf(
                        box("init", "启动\n项目章程\n识别干系人", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.3f, h = 0.24f, caption = "章程授权 PM 调动资源"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 规划过程组：产出项目管理计划与三大基准（范围/进度/成本）",
                    nodes = listOf(
                        box("init", "启动", 0.04f, 0.4f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("plan", "规划\n子计划+三大基准", 0.34f, 0.38f, VisualTone.GOOD, w = 0.36f, h = 0.24f, caption = "基准是绩效测量依据"),
                    ),
                    edges = listOf(edge("init", "plan", active = true)),
                ),
                VisualFrame(
                    "③ 执行 + 监控：干活出交付物，监控全程伴随比对基准",
                    nodes = listOf(
                        box("plan", "计划", 0.04f, 0.4f, VisualTone.MUTED, w = 0.14f, h = 0.16f),
                        box("exec", "执行\n产出可交付成果", 0.3f, 0.38f, VisualTone.ACTIVE, w = 0.3f, h = 0.24f),
                        circle("mon", "监控\n贯穿全程", 0.72f, 0.4f, VisualTone.BAD, w = 0.22f, h = 0.22f, caption = "不是第五个阶段，而是平行伴随"),
                    ),
                    edges = listOf(edge("plan", "exec", active = true), edge("exec", "mon", dashed = true)),
                ),
                VisualFrame(
                    "④ 收尾过程组：验收移交、经验教训归档、释放资源",
                    nodes = listOf(
                        box("close", "收尾\n验收移交\n组织过程资产更新", 0.3f, 0.38f, VisualTone.PATH, w = 0.4f, h = 0.24f, caption = "每个阶段末与项目末都要收尾"),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 2：干系人参与度阶梯 ============
        VisualLesson(
            id = "si-engagement-ladder",
            title = "干系人参与度阶梯：不了解 → 抵制 → 中立 → 支持 → 领导",
            examId = VisualRepository.SI,
            chapter = "项目干系人管理",
            keywords = listOf("参与度", "参与度评估矩阵", "不了解", "抵制", "中立", "支持", "领导", "干系人"),
            takeaway = "五级参与度：不了解（Unaware）→抵制（Resistant）→中立（Neutral）→支持（Supportive）→领导（Leading）。管理目标是把「当前」拉到「期望」——差距就是沟通计划的工作量。",
            frames = listOf(
                VisualFrame(
                    "① 五级阶梯全景：从不知道到主动带路",
                    nodes = listOf(
                        box("l1", "不了解\n不知道项目", 0.02f, 0.62f, VisualTone.MUTED, w = 0.18f, h = 0.2f),
                        box("l2", "抵制\n知道但反对", 0.22f, 0.62f, VisualTone.BAD, w = 0.18f, h = 0.2f),
                        box("l3", "中立\n不反对不支持", 0.42f, 0.62f, VisualTone.NEUTRAL, w = 0.18f, h = 0.2f),
                        box("l4", "支持\n愿意配合", 0.62f, 0.62f, VisualTone.GOOD, w = 0.18f, h = 0.2f),
                        box("l5", "领导\n主动推动", 0.82f, 0.62f, VisualTone.PATH, w = 0.18f, h = 0.2f),
                    ),
                    edges = listOf(edge("l1", "l2"), edge("l2", "l3"), edge("l3", "l4"), edge("l4", "l5")),
                ),
                VisualFrame(
                    "② 评估矩阵：同一干系人标出「当前 C」与「期望 D」两个位置",
                    nodes = listOf(
                        box("row", "财务总监\n当前=中立 C\n期望=支持 D", 0.3f, 0.4f, VisualTone.ACTIVE, w = 0.4f, h = 0.24f),
                        text("gap", "C 与 D 的差距 → 沟通与参与计划的工作量", 0.14f, 0.72f, w = 0.7f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 提升手段：了解靠告知，抵制靠化解诉求，支持靠参与收益",
                    nodes = listOf(
                        text("s1", "不了解 → 定向沟通让其知情", 0.06f, 0.18f, w = 0.6f),
                        text("s2", "抵制 → 分析顾虑、安排利益（如转岗保障）", 0.06f, 0.44f, w = 0.66f),
                        text("s3", "支持 → 邀请参与决策，让其成为方案的一部分", 0.06f, 0.7f, w = 0.7f, tone = VisualTone.GOOD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 「领导」级是加分项：让关键干系人主动为项目代言",
                    nodes = listOf(
                        box("lead", "领导级\n主动影响他人支持项目", 0.3f, 0.4f, VisualTone.PATH, w = 0.4f, h = 0.22f, caption = "发起人/高层支持者天然在此级"),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 3：CIA 三要素 ============
        VisualLesson(
            id = "si-cia-triad",
            title = "信息安全 CIA 三要素与对应措施",
            examId = VisualRepository.SI,
            chapter = "信息安全",
            keywords = listOf("CIA", "保密性", "完整性", "可用性", "信息安全", "加密", "数字签名"),
            takeaway = "保密性=不泄露（加密/权限最小化）；完整性=不被篡改（数字签名/哈希/校验）；可用性=要用时能用（冗余/备份/抗 DDoS）。措施与要素对应是最爱考的连线题。",
            frames = listOf(
                VisualFrame(
                    "① 保密性 Confidentiality：信息不泄露给未授权者",
                    nodes = listOf(
                        box("c", "保密性\n不泄露", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.28f, h = 0.22f),
                        text("m1", "措施：加密 / 访问控制 / 最小权限 / 分类分级", 0.4f, 0.42f, w = 0.56f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 完整性 Integrity：信息不被未授权篡改，保持准确完整",
                    nodes = listOf(
                        box("i", "完整性\n不被篡改", 0.06f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.22f),
                        text("m2", "措施：数字签名 / 哈希校验 / 版本控制 / 审计日志", 0.4f, 0.42f, w = 0.56f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 可用性 Availability：授权者需要时能正常访问",
                    nodes = listOf(
                        box("a", "可用性\n要用时能用", 0.06f, 0.4f, VisualTone.BAD, w = 0.28f, h = 0.22f),
                        text("m3", "措施：冗余集群 / 备份容灾 / 抗 DDoS / 限流降级", 0.4f, 0.42f, w = 0.56f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 对应演练：入侵检测守的是哪一项？——完整性侧的预警",
                    nodes = listOf(
                        text("q1", "防泄露 → 保密性；防篡改 → 完整性；防瘫痪 → 可用性", 0.04f, 0.24f, w = 0.9f, tone = VisualTone.ACTIVE),
                        text("q2", "易错：CA 认证/身份认证主要守「真实性」，别硬归到三要素", 0.04f, 0.56f, w = 0.9f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 1：图像格式家族 ============
        VisualLesson(
            id = "md-image-formats",
            title = "图像格式家族：JPEG / PNG / GIF / WebP 怎么选",
            examId = VisualRepository.MD,
            chapter = "数字图像与格式",
            keywords = listOf("JPEG", "PNG", "GIF", "WebP", "图像格式", "透明通道", "选型"),
            takeaway = "JPEG 照片（有损无透明）；PNG 无损+Alpha 透明（截图/Logo）；GIF 8 位索引+帧动画（简单动画/小图标）；WebP 两者兼得（新一代网页格式）。",
            frames = listOf(
                VisualFrame(
                    "① JPEG：有损压缩的照片之王——24 位真彩但无透明",
                    nodes = listOf(
                        box("jpg", "JPEG\n有损 / 24 位 / 无透明", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.36f, h = 0.24f, caption = "照片首选；文字线稿勿用"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② PNG：无损 + Alpha 透明通道——截图与 UI 素材标配",
                    nodes = listOf(
                        box("png", "PNG\n无损 / Alpha 透明", 0.06f, 0.38f, VisualTone.GOOD, w = 0.34f, h = 0.24f, caption = "需要透明与锐利细节时用"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ GIF：8 位索引色 + 多帧动画——简单动画的老将",
                    nodes = listOf(
                        box("gif", "GIF\n8 位 256 色 / 帧动画", 0.06f, 0.38f, VisualTone.PATH, w = 0.36f, h = 0.24f, caption = "色数受限，照片会失真"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ WebP 与选型决策树：小体积+有损无损+透明全都要 → WebP",
                    nodes = listOf(
                        text("q1", "照片 → JPEG；要透明/锐利 → PNG；小动画 → GIF", 0.04f, 0.16f, w = 0.86f),
                        text("q2", "网页全都要（小体积+无损/有损+透明）→ WebP", 0.04f, 0.46f, w = 0.86f, tone = VisualTone.PATH),
                        text("q3", "归档/医学/再编辑 → PNG/TIFF 无损", 0.04f, 0.76f, w = 0.86f, tone = VisualTone.GOOD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 2：三维动画流程 ============
        VisualLesson(
            id = "md-3d-pipeline",
            title = "三维动画流水线：建模 → 材质 → 灯光 → 渲染",
            examId = VisualRepository.MD,
            chapter = "动画技术",
            keywords = listOf("三维动画", "建模", "材质", "灯光", "渲染", "骨骼", "3D"),
            takeaway = "三维流水线：建模（塑形）→材质贴图（皮肤）→绑骨（驱动）→灯光（氛围）→渲染（出图），任一环节返工都会向后传导——所以先定稿再下游。",
            frames = listOf(
                VisualFrame(
                    "① 建模：用多边形/NURBS 塑造物体与角色形体",
                    nodes = listOf(
                        box("model", "建模\n多边形/NURBS", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.32f, h = 0.24f, caption = "低模用于实时，高模用于影视"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 材质与贴图：给模型穿上「皮肤」——颜色/粗糙度/法线",
                    nodes = listOf(
                        box("model", "模型", 0.04f, 0.4f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("mat", "材质贴图\n颜色/粗糙度/法线", 0.36f, 0.38f, VisualTone.GOOD, w = 0.34f, h = 0.24f),
                    ),
                    edges = listOf(edge("model", "mat", active = true)),
                ),
                VisualFrame(
                    "③ 绑骨与灯光：骨架驱动动作，灯光营造氛围",
                    nodes = listOf(
                        box("rig", "绑骨\n骨骼驱动动作", 0.06f, 0.38f, VisualTone.PATH, w = 0.28f, h = 0.24f),
                        box("light", "灯光\n主光/辅光/轮廓光", 0.44f, 0.38f, VisualTone.NEUTRAL, w = 0.32f, h = 0.24f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 渲染输出：逐帧计算最终画面——质量与耗时的总闸门",
                    nodes = listOf(
                        box("render", "渲染\n逐帧计算成像", 0.3f, 0.38f, VisualTone.BAD, w = 0.4f, h = 0.24f, caption = "影视级一帧可渲染数小时"),
                        text("rule", "上游改动 → 渲染全部重来：先定稿再渲染", 0.1f, 0.72f, w = 0.8f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 3：印刷输出分辨率 ============
        VisualLesson(
            id = "md-print-resolution",
            title = "印刷输出分辨率：300dpi 规则与像素换算",
            examId = VisualRepository.MD,
            chapter = "分辨率与图像容量",
            keywords = listOf("dpi", "印刷", "分辨率", "像素", "300dpi", "LPI", "输出尺寸"),
            takeaway = "印刷常用 300dpi：所需像素 = 输出尺寸(英寸) × 300。屏幕只看像素总数，印刷还看「每英寸多少点」——同一张图印得越大 dpi 越低越糊。",
            frames = listOf(
                VisualFrame(
                    "① dpi 是密度：像素总数 ÷ 输出英寸数 = 每英寸点数",
                    nodes = listOf(
                        box("px", "像素总数\n（图像固有）", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.3f, h = 0.24f),
                        box("inch", "输出尺寸\n（英寸）", 0.46f, 0.38f, VisualTone.NEUTRAL, w = 0.26f, h = 0.24f),
                        text("dpi", "dpi = 像素 ÷ 英寸", 0.3f, 0.7f, w = 0.4f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 300dpi 规则：印刷品质的通行标准（报刊可放宽到 200+）",
                    nodes = listOf(
                        text("r1", "A4（8.27×11.69 英寸）@300dpi ≈ 2480×3508 像素", 0.04f, 0.24f, w = 0.9f, tone = VisualTone.ACTIVE),
                        text("r2", "不够 300dpi → 印出来发虚、出锯齿", 0.04f, 0.52f, w = 0.8f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 反向计算：要印 6×4 英寸照片 @300dpi → 需 1800×1200 像素",
                    nodes = listOf(
                        box("calc", "6×300=1800\n4×300=1200", 0.3f, 0.38f, VisualTone.GOOD, w = 0.32f, h = 0.26f),
                        text("inv", "像素不够 → 别硬拉插值，重拍或降尺寸", 0.14f, 0.72f, w = 0.7f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 屏幕与印刷双轨：屏幕只认像素总数，印刷认「像素/英寸」",
                    nodes = listOf(
                        text("s1", "屏幕：1920×1080 就是 1920×1080（与英寸无关）", 0.04f, 0.2f, w = 0.88f),
                        text("s2", "印刷：同一张图，印 A4 与印海报 dpi 相差数倍", 0.04f, 0.46f, w = 0.88f, tone = VisualTone.PATH),
                        text("s3", "双用途交付：屏幕版 + 300dpi 印刷版（CMYK）各一份", 0.04f, 0.72f, w = 0.88f),
                    ),
                    edges = listOf(),
                ),
            ),
        ),
    )
}
