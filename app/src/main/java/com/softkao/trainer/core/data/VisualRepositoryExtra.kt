package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 抽象图解（第二批）：覆盖储备与储备、动画补间、冗余类型、生命周期模型、
 * 配置库与基线、干系人矩阵、帕累托、备份容灾。
 */
object VisualRepositoryExtra {

    private fun box(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.2f, h: Float = 0.13f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BOX, 0f, tone, caption)

    private fun circle(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.16f, h: Float = 0.16f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.CIRCLE, 0f, tone, caption)

    private fun bar(id: String, label: String, x: Float, y: Float, value: Float, tone: VisualTone = VisualTone.ACTIVE, w: Float = 0.1f, h: Float = 0.42f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BAR, value, tone, caption)

    private fun text(id: String, label: String, x: Float, y: Float, caption: String = "", w: Float = 0.3f, h: Float = 0.1f, tone: VisualTone = VisualTone.NEUTRAL) =
        VisualNode(id, label, x, y, w, h, VisualKind.TEXT, 0f, tone, caption)

    private fun edge(from: String, to: String, active: Boolean = false, dashed: Boolean = false, label: String = "") =
        VisualEdge(from, to, active, dashed, label)

    val lessons: List<VisualLesson> = listOf(
        VisualLesson(
            id = "si-reserve",
            title = "应急储备与管理储备的边界",
            examId = VisualRepository.SI,
            chapter = "风险应对与储备",
            keywords = listOf("应急储备", "管理储备", "储备", "成本基准", "风险预算"),
            takeaway = "应急储备进成本基准，用于已识别风险；管理储备不进成本基准，用于未知未知，由管理层掌握。",
            frames = listOf(
                VisualFrame(
                    "① 成本基准 = 成本估算 + 应急储备",
                    nodes = listOf(
                        box("est", "成本估算", 0.06f, 0.45f, VisualTone.ACTIVE, w = 0.24f, caption="不含储备"),
                        box("er", "应急储备", 0.36f, 0.45f, VisualTone.GOOD, w = 0.24f, caption="已识别风险"),
                        box("cb", "成本基准", 0.7f, 0.45f, VisualTone.ACTIVE, w = 0.24f, caption="可执行的项目预算"),
                    ),
                    edges = listOf(edge("est", "cb", active = true), edge("er", "cb", active = true)),
                ),
                VisualFrame(
                    "② 项目总预算 = 成本基准 + 管理储备",
                    nodes = listOf(
                        box("cb", "成本基准", 0.14f, 0.45f, VisualTone.ACTIVE, w = 0.24f, caption="估算+应急"),
                        box("mr", "管理储备", 0.46f, 0.45f, VisualTone.BAD, w = 0.24f, caption="未知未知"),
                        box("tot", "项目总预算", 0.78f, 0.45f, VisualTone.NEUTRAL, w = 0.2f),
                        text("t", "管理储备动用需管理层批准", 0.3f, 0.75f, "", 0.5f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("cb", "tot", active = true), edge("mr", "tot")),
                ),
                VisualFrame(
                    "③ 应急储备用于「已识别」风险，动用一般不必提交整体变更请求",
                    nodes = listOf(
                        circle("r1", "已识别风险", 0.22f, 0.4f, VisualTone.ACTIVE, w = 0.22f, h = 0.22f),
                        circle("r2", "未识别风险", 0.68f, 0.4f, VisualTone.BAD, w = 0.22f, h = 0.22f),
                        box("er", "应急储备", 0.28f, 0.75f, VisualTone.GOOD, w = 0.2f),
                        box("mr", "管理储备", 0.72f, 0.75f, VisualTone.BAD, w = 0.2f),
                        text("t", "储备不得用于扩大范围", 0.3f, 0.1f, "", 0.4f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("r1", "er", active = true), edge("r2", "mr")),
                ),
            ),
        ),

        VisualLesson(
            id = "si-config",
            title = "配置库三库与基线",
            examId = VisualRepository.SI,
            chapter = "项目配置管理",
            keywords = listOf("配置库", "基线", "动态库", "受控库", "主库", "配置审计", "版本控制"),
            takeaway = "开发库自由改、受控库需审批、主库只放发布件；基线一旦确立，改动必须走变更控制。",
            frames = listOf(
                VisualFrame(
                    "① 三个配置库：动态库（开发）→ 受控库（测试/集成）→ 主库（发布）",
                    nodes = listOf(
                        box("d", "动态库", 0.06f, 0.45f, VisualTone.MUTED, w = 0.22f, caption="开发人员随意改"),
                        box("c", "受控库", 0.39f, 0.45f, VisualTone.ACTIVE, w = 0.22f, caption="经审核受控"),
                        box("m", "主库", 0.72f, 0.45f, VisualTone.GOOD, w = 0.22f, caption="正式发布件"),
                    ),
                    edges = listOf(edge("d", "c", active = true), edge("c", "m", active = true)),
                ),
                VisualFrame(
                    "② 基线：功能基线、分配基线、产品基线",
                    nodes = listOf(
                        box("b1", "功能基线", 0.08f, 0.3f, VisualTone.ACTIVE, w = 0.24f, caption="需求确认"),
                        box("b2", "分配基线", 0.38f, 0.3f, VisualTone.ACTIVE, w = 0.24f, caption="任务分配"),
                        box("b3", "产品基线", 0.68f, 0.3f, VisualTone.ACTIVE, w = 0.24f, caption="交付发布"),
                        text("t", "基线只能通过整体变更控制修改", 0.22f, 0.68f, "", 0.56f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 配置审计：FCA 看「做对没」，PCA 看「做没做对」",
                    nodes = listOf(
                        box("fca", "功能审计 FCA", 0.1f, 0.35f, VisualTone.ACTIVE, w = 0.3f, caption="需求是否被实现"),
                        box("pca", "物理审计 PCA", 0.56f, 0.35f, VisualTone.ACTIVE, w = 0.3f, caption="交付物是否与文档一致"),
                        text("t", "审计在配置项变更后与里程碑处进行", 0.2f, 0.68f, "", 0.6f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-stakeholder",
            title = "干系人权力/利益矩阵",
            examId = VisualRepository.SI,
            chapter = "项目干系人管理",
            keywords = listOf("干系人", "权力", "利益", "紧密管理", "令其满意", "随时告知", "监督"),
            takeaway = "高权力高利益=紧密管理，高权力低利益=令其满意，低权力高利益=随时告知，低权力低利益=监督。",
            frames = listOf(
                VisualFrame(
                    "① 两根轴：权力（高→低）、利益（高→低）",
                    nodes = listOf(
                        text("y", "利益 高", 0.02f, 0.12f, "", 0.18f, 0.1f, VisualTone.MUTED),
                        text("x", "权力 高", 0.02f, 0.55f, "", 0.18f, 0.1f, VisualTone.MUTED),
                        box("q1", "紧密管理", 0.22f, 0.14f, VisualTone.BAD, w = 0.3f, h = 0.22f, caption="高权力 高利益"),
                        box("q2", "令其满意", 0.58f, 0.14f, VisualTone.ACTIVE, w = 0.3f, h = 0.22f, caption="高权力 低利益"),
                        box("q3", "随时告知", 0.22f, 0.55f, VisualTone.GOOD, w = 0.3f, h = 0.22f, caption="低权力 高利益"),
                        box("q4", "监督", 0.58f, 0.55f, VisualTone.MUTED, w = 0.3f, h = 0.22f, caption="低权力 低利益"),
                    ),
                ),
                VisualFrame(
                    "② 策略随象限走：全力经营重点干系人，长期争取改进",
                    nodes = listOf(
                        box("q1", "紧密管理", 0.22f, 0.14f, VisualTone.BAD, w = 0.3f, h = 0.22f),
                        box("q2", "令其满意", 0.58f, 0.14f, VisualTone.ACTIVE, w = 0.3f, h = 0.22f),
                        box("q3", "随时告知", 0.22f, 0.55f, VisualTone.GOOD, w = 0.3f, h = 0.22f),
                        box("q4", "监督", 0.58f, 0.55f, VisualTone.MUTED, w = 0.3f, h = 0.22f),
                        text("t", "干系人登记册要持续更新，分类会随项目推进变化", 0.1f, 0.88f, "", 0.8f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-pareto",
            title = "帕累托图：80/20 抓主要缺陷",
            examId = VisualRepository.SI,
            chapter = "质量工具与质量成本",
            keywords = listOf("帕累托", "80/20", "排列图", "累计曲线", "质量工具"),
            takeaway = "按发生频次排序画柱状图，叠加累计百分比曲线；前几个少数原因往往占大部分问题。",
            frames = listOf(
                VisualFrame(
                    "① 按缺陷发生频次从高到低排列",
                    nodes = listOf(
                        bar("b1", "文档", 0.1f, 0.3f, 1.0f, VisualTone.BAD, caption="40%"),
                        bar("b2", "界面", 0.28f, 0.3f, 0.62f, VisualTone.ACTIVE, caption="25%"),
                        bar("b3", "性能", 0.46f, 0.3f, 0.4f, VisualTone.ACTIVE, caption="16%"),
                        bar("b4", "其它", 0.64f, 0.3f, 0.22f, VisualTone.MUTED, caption="9%"),
                        bar("b5", "偶发", 0.8f, 0.3f, 0.14f, VisualTone.MUTED, caption="6%"),
                        text("t", "柱：频次  曲线：累计%", 0.2f, 0.78f, "", 0.6f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 累计曲线过 80% 的位置，就是优先解决的那几个原因",
                    nodes = listOf(
                        bar("b1", "文档", 0.1f, 0.3f, 1.0f, VisualTone.BAD, caption="40%"),
                        bar("b2", "界面", 0.28f, 0.3f, 0.62f, VisualTone.ACTIVE, caption="累计 65%"),
                        bar("b3", "性能", 0.46f, 0.3f, 0.4f, VisualTone.MUTED, caption="累计 81%"),
                        bar("b4", "其它", 0.64f, 0.3f, 0.22f, VisualTone.MUTED, caption="累计 90%"),
                        bar("b5", "偶发", 0.8f, 0.3f, 0.14f, VisualTone.MUTED, caption="累计 96%"),
                        text("t", "先治「文档」这一类，收益最大", 0.25f, 0.78f, "", 0.5f, 0.1f, VisualTone.GOOD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-lifecycle",
            title = "四种生命周期模型怎么选",
            examId = VisualRepository.SI,
            chapter = "项目管理概论",
            keywords = listOf("瀑布模型", "原型模型", "迭代模型", "螺旋模型", "生命周期", "需求明确"),
            takeaway = "需求明确用瀑布，需求不确定用原型/迭代，风险高用螺旋（每轮都做风险分析）。",
            frames = listOf(
                VisualFrame(
                    "① 瀑布：阶段串行，前一阶段评审通过才进下一阶段",
                    nodes = listOf(
                        box("w1", "需求", 0.06f, 0.45f, VisualTone.ACTIVE, w = 0.18f),
                        box("w2", "设计", 0.27f, 0.45f, VisualTone.ACTIVE, w = 0.18f),
                        box("w3", "开发", 0.48f, 0.45f, VisualTone.ACTIVE, w = 0.18f),
                        box("w4", "测试", 0.69f, 0.45f, VisualTone.ACTIVE, w = 0.18f),
                        box("w5", "上线", 0.88f, 0.45f, VisualTone.GOOD, w = 0.12f),
                    ),
                    edges = listOf(edge("w1", "w2", active = true), edge("w2", "w3", active = true), edge("w3", "w4", active = true), edge("w4", "w5", active = true)),
                ),
                VisualFrame(
                    "② 原型：先做可运行原型给用户试用，再迭代收敛需求",
                    nodes = listOf(
                        circle("p1", "原型", 0.16f, 0.45f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f, caption="快速试错"),
                        circle("p2", "反馈", 0.45f, 0.45f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f),
                        circle("p3", "迭代", 0.74f, 0.45f, VisualTone.GOOD, w = 0.2f, h = 0.2f, caption="需求不明时用"),
                        text("t", "需求不确定 → 原型 / 迭代", 0.22f, 0.78f, "", 0.56f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("p1", "p2", active = true), edge("p2", "p3", active = true)),
                ),
                VisualFrame(
                    "③ 螺旋：每轮迭代都做一次风险分析，优先化解高风险",
                    nodes = listOf(
                        circle("s1", "目标", 0.1f, 0.45f, VisualTone.MUTED, w = 0.18f, h = 0.18f),
                        circle("s2", "风险", 0.36f, 0.45f, VisualTone.BAD, w = 0.18f, h = 0.18f, caption="每轮必做"),
                        circle("s3", "验证", 0.62f, 0.45f, VisualTone.ACTIVE, w = 0.18f, h = 0.18f),
                        circle("s4", "下一轮", 0.86f, 0.45f, VisualTone.ACTIVE, w = 0.18f, h = 0.18f),
                        text("t", "风险高、需求不确定 → 螺旋", 0.24f, 0.78f, "", 0.52f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("s1", "s2", active = true), edge("s2", "s3", active = true), edge("s3", "s4", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "md-tween",
            title = "三种补间动画的区别",
            examId = VisualRepository.MD,
            chapter = "动画技术",
            keywords = listOf("补间", "关键帧", "形状补间", "动作补间", "颜色补间", "逐帧动画"),
            takeaway = "动作补间改位移旋转缩放，形状补间改形状，颜色补间改颜色与透明度。",
            frames = listOf(
                VisualFrame(
                    "① 动作补间：位置变了，形状没变",
                    nodes = listOf(
                        circle("a1", "起", 0.14f, 0.45f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        circle("a2", "终", 0.72f, 0.45f, VisualTone.ACTIVE, w = 0.16f, h = 0.16f),
                        text("t", "运动类补间：位移 / 旋转 / 缩放", 0.22f, 0.78f, "", 0.56f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("a1", "a2", active = true)),
                ),
                VisualFrame(
                    "② 形状补间：轮廓变了，位置没变",
                    nodes = listOf(
                        box("b1", "■", 0.16f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.2f),
                        box("b2", "●", 0.64f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f),
                        text("t", "形状补间：轮廓从方变圆", 0.26f, 0.78f, "", 0.48f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("b1", "b2", active = true)),
                ),
                VisualFrame(
                    "③ 颜色补间：颜色与透明度渐变",
                    nodes = listOf(
                        box("c1", "红", 0.16f, 0.4f, VisualTone.BAD, w = 0.2f, h = 0.2f),
                        box("c2", "蓝", 0.64f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f),
                        text("t", "颜色补间 / Alpha 补间", 0.26f, 0.78f, "", 0.48f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("c1", "c2", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "md-redundancy",
            title = "四种冗余分别怎么压",
            examId = VisualRepository.MD,
            chapter = "冗余与压缩比",
            keywords = listOf("冗余", "空间冗余", "时间冗余", "视觉冗余", "统计冗余", "压缩比"),
            takeaway = "空间冗余靠变换压缩，时间冗余靠帧间预测，视觉冗余靠舍弃高频，统计冗余靠熵编码。",
            frames = listOf(
                VisualFrame(
                    "① 空间冗余：相邻像素相似 → 用 DCT 把能量集中到低频",
                    nodes = listOf(
                        box("s1", "像素块", 0.1f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.22f, caption="8×8"),
                        box("s2", "频率域", 0.62f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.22f, caption="能量集中左上"),
                        text("t", "对应 JPEG 的变换编码", 0.26f, 0.78f, "", 0.48f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(
                        edge("s1", "s2", active = true),
                    ),
                ),
                VisualFrame(
                    "② 时间冗余：相邻帧相似 → 用帧间预测（P/B 帧）",
                    nodes = listOf(
                        box("t1", "帧1", 0.1f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.22f),
                        box("t2", "帧2", 0.4f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.22f),
                        box("t3", "帧3", 0.7f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.22f),
                        text("t", "对应视频的 P/B 帧预测", 0.26f, 0.78f, "", 0.48f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(
                        edge("t1", "t2", active = true), edge("t2", "t3", active = true),
                    ),
                ),
                VisualFrame(
                    "③ 视觉冗余：人眼对高频不敏感 → 量化时丢弃",
                    nodes = listOf(
                        box("v1", "高频细节", 0.1f, 0.4f, VisualTone.MUTED, w = 0.24f, h = 0.22f, caption="人眼不敏感"),
                        box("v2", "丢弃", 0.62f, 0.4f, VisualTone.BAD, w = 0.2f, h = 0.22f),
                        text("t", "有损压缩的根本原因", 0.3f, 0.78f, "", 0.4f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(
                        edge("v1", "v2", active = true),
                    ),
                ),
                VisualFrame(
                    "④ 统计冗余：符号出现概率不均 → 熵编码（哈夫曼）",
                    nodes = listOf(
                        box("e1", "常见符号", 0.1f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.22f, caption="短码"),
                        box("e2", "罕见符号", 0.62f, 0.4f, VisualTone.MUTED, w = 0.24f, h = 0.22f, caption="长码"),
                        text("t", "对应熵编码，接近熵极限", 0.28f, 0.78f, "", 0.44f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(
                        edge("e1", "e2", active = true),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-audio-format",
            title = "音频格式：体积与音质的取舍",
            examId = VisualRepository.MD,
            chapter = "音频格式与编辑",
            keywords = listOf("WAV", "MP3", "AAC", "FLAC", "MIDI", "音频格式", "无损"),
            takeaway = "WAV 未压缩最保险但最占空间；FLAC 无损压缩；MP3 有损最通用；AAC 效率更高；MIDI 存的是乐谱不是波形。",
            frames = listOf(
                VisualFrame(
                    "① 同样 10 分钟音频，各格式体积差多少",
                    nodes = listOf(
                        bar("w", "WAV", 0.06f, 0.32f, 1.0f, VisualTone.MUTED, caption="100 MB 未压缩"),
                        bar("f", "FLAC", 0.28f, 0.32f, 0.6f, VisualTone.GOOD, caption="≈60 MB 无损"),
                        bar("a", "AAC", 0.5f, 0.32f, 0.2f, VisualTone.ACTIVE, caption="≈20 MB"),
                        bar("m", "MP3", 0.72f, 0.32f, 0.17f, VisualTone.ACTIVE, caption="≈17 MB"),
                        text("t", "柱高 = 相对体积（以 WAV 为 100%）", 0.16f, 0.78f, "", 0.68f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 音质：WAV = FLAC > AAC > MP3",
                    nodes = listOf(
                        bar("w", "WAV", 0.06f, 0.32f, 1.0f, VisualTone.GOOD, caption="原始"),
                        bar("f", "FLAC", 0.28f, 0.32f, 1.0f, VisualTone.GOOD, caption="无损还原"),
                        bar("a", "AAC", 0.5f, 0.32f, 0.78f, VisualTone.ACTIVE, caption="高码率仍好"),
                        bar("m", "MP3", 0.72f, 0.32f, 0.66f, VisualTone.MUTED, caption="高频损失"),
                        text("t", "MIDI 存乐谱，不含波形，不能直接播声音", 0.18f, 0.78f, "", 0.64f, 0.1f, VisualTone.BAD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-backup",
            title = "备份方式与容灾指标",
            examId = VisualRepository.MD,
            chapter = "信息安全",
            keywords = listOf("备份", "全备份", "增量备份", "差异备份", "RTO", "RPO", "容灾"),
            takeaway = "全备份恢复最全但最慢；增量只备变化、恢复要叠全；差异备自上次全备之后的全部变化。RTO 决定能停多久，RPO 决定能丢多少。",
            frames = listOf(
                VisualFrame(
                    "① 全量 / 增量 / 差异：备份了什么",
                    nodes = listOf(
                        box("f", "全备份", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.22f, caption="全部数据"),
                        box("i", "增量备份", 0.38f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.22f, caption="仅变化量"),
                        box("d", "差异备份", 0.7f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.22f, caption="上次全备后全部变化"),
                    ),
                ),
                VisualFrame(
                    "② 恢复速度：全备份最快，增量最慢（要叠全链）",
                    nodes = listOf(
                        bar("f", "全", 0.12f, 0.32f, 0.9f, VisualTone.GOOD, caption="恢复快"),
                        bar("d", "差异", 0.42f, 0.32f, 0.55f, VisualTone.ACTIVE, caption="居中"),
                        bar("i", "增量", 0.72f, 0.32f, 0.25f, VisualTone.MUTED, caption="要叠全链"),
                    ),
                ),
                VisualFrame(
                    "③ RTO 与 RPO：能停多久、能丢多少",
                    nodes = listOf(
                        box("rto", "RTO", 0.12f, 0.28f, VisualTone.BAD, w = 0.26f, h = 0.22f, caption="恢复时间目标"),
                        box("rpo", "RPO", 0.58f, 0.28f, VisualTone.BAD, w = 0.26f, h = 0.22f, caption="可容忍数据丢失"),
                        text("t", "RPO 越小 → 备份越频繁，成本越高", 0.16f, 0.7f, "", 0.68f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),
    )
}
