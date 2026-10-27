package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 知识点抽象图解库。
 *
 * 每个知识点配一串「帧」，播放器逐帧播放，用最少的形状讲清一件事：
 * 关键路径怎么走、挣值四个指标怎么算、I/P/B 帧怎么依赖、
 * JPEG 编码五步各自在干什么、采样量化把波形变成什么样子……
 *
 * 坐标全部用 0~1 归一化，渲染层只管按比例画。
 */
object VisualRepository {

    const val SI = "system-integration"
    const val MD = "multimedia-designer"

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

    private val lessons: List<VisualLesson> = listOf(
        // ---------------------------------------------------------------- 系统集成
        VisualLesson(
            id = "si-cpm",
            title = "关键路径与总时差",
            examId = SI,
            chapter = "进度网络图与关键路径法",
            keywords = listOf("关键路径", "总时差", "自由时差", "工期延长", "进度网络图"),
            takeaway = "关键路径上的活动总时差为 0，它一延误，项目完工就等量往后推；非关键活动还有总时差可以吸收延误。",
            frames = listOf(
                VisualFrame(
                    "① 画出网络图：A→B→C→D 为一条路径，A→E→D 为另一条",
                    nodes = listOf(
                        circle("A", "A", 0.08f, 0.5f), circle("B", "B", 0.32f, 0.22f), circle("C", "C", 0.58f, 0.22f),
                        circle("D", "D", 0.85f, 0.5f), circle("E", "E", 0.32f, 0.78f),
                    ),
                    edges = listOf(
                        edge("A", "B"), edge("B", "C"), edge("C", "D"), edge("A", "E"), edge("E", "D"),
                    ),
                ),
                VisualFrame(
                    "② 两条路径比较时长：上路径 2+3+4=9 天，下路径 3+3=6 天",
                    nodes = listOf(
                        circle("A", "A", 0.08f, 0.5f), circle("B", "B", 0.32f, 0.22f, VisualTone.ACTIVE), circle("C", "C", 0.58f, 0.22f, VisualTone.ACTIVE),
                        circle("D", "D", 0.85f, 0.5f), circle("E", "E", 0.32f, 0.78f, VisualTone.MUTED),
                    ),
                    edges = listOf(
                        edge("A", "B", active = true), edge("B", "C", active = true), edge("C", "D", active = true),
                        edge("A", "E"), edge("E", "D"),
                    ),
                ),
                VisualFrame(
                    "③ 关键路径 = A-B-C-D，总时差 TF=0；E 有 3 天总时差可吸收延误",
                    nodes = listOf(
                        circle("A", "A", 0.08f, 0.5f, VisualTone.ACTIVE), circle("B", "B", 0.32f, 0.22f, VisualTone.ACTIVE),
                        circle("C", "C", 0.58f, 0.22f, VisualTone.ACTIVE), circle("D", "D", 0.85f, 0.5f, VisualTone.ACTIVE),
                        circle("E", "E", 0.32f, 0.78f, VisualTone.GOOD),
                        text("t1", "TF(B)=0", 0.72f, 0.12f, "关键活动", 0.22f, 0.1f, VisualTone.ACTIVE),
                        text("t2", "TF(E)=3", 0.1f, 0.86f, "有余量", 0.22f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(
                        edge("A", "B", active = true), edge("B", "C", active = true), edge("C", "D", active = true),
                        edge("A", "E"), edge("E", "D"),
                    ),
                ),
                VisualFrame(
                    "④ 关键活动 B 延误 5 天：无人可吸收，工期由 9 天延长到 14 天",
                    nodes = listOf(
                        circle("A", "A", 0.08f, 0.5f, VisualTone.ACTIVE), circle("B", "B", 0.32f, 0.22f, VisualTone.BAD),
                        circle("C", "C", 0.58f, 0.22f, VisualTone.BAD), circle("D", "D", 0.85f, 0.5f, VisualTone.BAD),
                        circle("E", "E", 0.32f, 0.78f, VisualTone.MUTED),
                        text("t3", "工期 9 → 14 天", 0.55f, 0.88f, "延误全部落在关键路径上", 0.36f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(
                        edge("A", "B", active = true), edge("B", "C", active = true), edge("C", "D", active = true),
                        edge("A", "E", dashed = true), edge("E", "D", dashed = true),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-evm",
            title = "挣值四指标怎么读",
            examId = SI,
            chapter = "项目成本管理",
            keywords = listOf("挣值", "CV", "SV", "CPI", "SPI", "EAC", "成本偏差", "进度偏差"),
            takeaway = "CV=EV−AC 看成本，SV=EV−PV 看进度；CPI=EV/AC、SPI=EV/PV，小于 1 都不利。",
            frames = listOf(
                VisualFrame(
                    "① 三条基准柱：PV 计划值、EV 挣值、AC 实际花费",
                    nodes = listOf(
                        bar("pv", "PV", 0.18f, 0.44f, 1.0f, VisualTone.MUTED, caption="计划做 100"),
                        bar("ev", "EV", 0.46f, 0.44f, 0.9f, VisualTone.ACTIVE, caption="实际做了 90"),
                        bar("ac", "AC", 0.74f, 0.44f, 1.1f, VisualTone.BAD, caption="实际花了 110"),
                    ),
                ),
                VisualFrame(
                    "② CV = EV − AC = 90 − 110 = −20 → 成本超支",
                    nodes = listOf(
                        bar("pv", "PV", 0.18f, 0.44f, 1.0f, VisualTone.MUTED, caption="计划 100"),
                        bar("ev", "EV", 0.46f, 0.44f, 0.9f, VisualTone.ACTIVE, caption="挣值 90"),
                        bar("ac", "AC", 0.74f, 0.44f, 1.1f, VisualTone.BAD, caption="花费 110"),
                        text("cv", "CV = −20", 0.2f, 0.92f, "成本超支 20", 0.3f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ SV = EV − PV = 90 − 100 = −10 → 进度落后",
                    nodes = listOf(
                        bar("pv", "PV", 0.18f, 0.44f, 1.0f, VisualTone.MUTED, caption="计划 100"),
                        bar("ev", "EV", 0.46f, 0.44f, 0.9f, VisualTone.ACTIVE, caption="挣值 90"),
                        bar("ac", "AC", 0.74f, 0.44f, 1.1f, VisualTone.BAD, caption="花费 110"),
                        text("sv", "SV = −10", 0.55f, 0.92f, "进度落后 10", 0.3f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "④ CPI = 90/110 = 0.82，SPI = 90/100 = 0.90，两者都 < 1",
                    nodes = listOf(
                        bar("pv", "PV", 0.14f, 0.44f, 1.0f, VisualTone.MUTED, caption="计划 100"),
                        bar("ev", "EV", 0.38f, 0.44f, 0.9f, VisualTone.ACTIVE, caption="挣值 90"),
                        bar("ac", "AC", 0.62f, 0.44f, 1.1f, VisualTone.BAD, caption="花费 110"),
                        text("cpi", "CPI 0.82", 0.1f, 0.92f, "每花 1 元只做出 0.82 元", 0.36f, 0.1f, VisualTone.BAD),
                        text("spi", "SPI 0.90", 0.54f, 0.92f, "进度只完成 90%", 0.32f, 0.1f, VisualTone.BAD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-change",
            title = "变更七步流程",
            examId = SI,
            chapter = "项目整体管理",
            keywords = listOf("变更", "CCB", "整体变更控制", "变更流程", "变更控制委员会"),
            takeaway = "变更必须走完「提出—记录—评估—审批—实施—验证—归档」，口头变更一律不算。",
            frames = listOf(
                VisualFrame(
                    "① 提出：干系人提交书面变更申请",
                    nodes = listOf(
                        box("s1", "① 提出申请", 0.05f, 0.45f, VisualTone.ACTIVE, w = 0.22f),
                        text("t", "口头变更无效", 0.42f, 0.5f, "", 0.3f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "② 记录：登记到变更请求日志",
                    nodes = listOf(
                        box("s1", "① 提出申请", 0.05f, 0.45f, VisualTone.GOOD, w = 0.22f),
                        box("s2", "② 登记日志", 0.3f, 0.45f, VisualTone.ACTIVE, w = 0.22f),
                    ),
                    edges = listOf(edge("s1", "s2")),
                ),
                VisualFrame(
                    "③ 评估：分析范围/进度/成本/质量/风险影响",
                    nodes = listOf(
                        box("s1", "① 提出申请", 0.05f, 0.45f, VisualTone.GOOD, w = 0.22f),
                        box("s2", "② 登记日志", 0.3f, 0.45f, VisualTone.GOOD, w = 0.22f),
                        box("s3", "③ 影响评估", 0.55f, 0.45f, VisualTone.ACTIVE, w = 0.22f),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3")),
                ),
                VisualFrame(
                    "④ 审批：重大变更报 CCB / 建设单位",
                    nodes = listOf(
                        box("s1", "① 提出", 0.04f, 0.45f, VisualTone.GOOD, w = 0.17f),
                        box("s2", "② 登记", 0.23f, 0.45f, VisualTone.GOOD, w = 0.17f),
                        box("s3", "③ 评估", 0.42f, 0.45f, VisualTone.GOOD, w = 0.17f),
                        box("s4", "④ CCB 审批", 0.61f, 0.45f, VisualTone.ACTIVE, w = 0.22f),
                        text("t", "未批准不得实施", 0.6f, 0.72f, "", 0.3f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3"), edge("s3", "s4")),
                ),
                VisualFrame(
                    "⑤ 实施 → ⑥ 验证 → ⑦ 更新基准与记录并通知相关方",
                    nodes = listOf(
                        box("s1", "① 提出", 0.03f, 0.45f, VisualTone.GOOD, w = 0.16f),
                        box("s2", "② 登记", 0.21f, 0.45f, VisualTone.GOOD, w = 0.16f),
                        box("s3", "③ 评估", 0.39f, 0.45f, VisualTone.GOOD, w = 0.16f),
                        box("s4", "④ 审批", 0.57f, 0.45f, VisualTone.GOOD, w = 0.16f),
                        box("s5", "⑤ 实施", 0.75f, 0.45f, VisualTone.ACTIVE, w = 0.16f),
                        text("t", "⑥ 验证 → ⑦ 更新基准 · 归档 · 通知", 0.3f, 0.74f, "", 0.45f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3"), edge("s3", "s4"), edge("s4", "s5")),
                ),
            ),
        ),

        VisualLesson(
            id = "si-channels",
            title = "沟通渠道随人数平方级增长",
            examId = SI,
            chapter = "项目沟通管理",
            keywords = listOf("沟通渠道", "n(n-1)/2", "沟通数量"),
            takeaway = "渠道数 n(n−1)/2：6 人 15 条，9 人 36 条，20 人 190 条——人越多，沟通成本越爆炸。",
            frames = listOf(
                VisualFrame(
                    "① 3 人团队：3×2÷2 = 3 条渠道",
                    nodes = listOf(
                        circle("a", "A", 0.2f, 0.25f), circle("b", "B", 0.5f, 0.6f), circle("c", "C", 0.8f, 0.25f),
                        text("n", "3 人 → 3 条", 0.32f, 0.85f, "", 0.36f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("a", "b"), edge("b", "c"), edge("a", "c")),
                ),
                VisualFrame(
                    "② 6 人团队：6×5÷2 = 15 条",
                    nodes = listOf(
                        circle("a", "A", 0.15f, 0.3f), circle("b", "B", 0.45f, 0.18f), circle("c", "C", 0.78f, 0.28f),
                        circle("d", "D", 0.2f, 0.66f), circle("e", "E", 0.55f, 0.7f), circle("f", "F", 0.85f, 0.62f),
                        text("n", "6 人 → 15 条", 0.32f, 0.88f, "", 0.36f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(
                        edge("a", "b"), edge("a", "c"), edge("a", "d"), edge("a", "e"), edge("a", "f"),
                        edge("b", "c"), edge("b", "d"), edge("b", "e"), edge("b", "f"),
                        edge("c", "d"), edge("c", "e"), edge("c", "f"),
                        edge("d", "e"), edge("d", "f"), edge("e", "f"),
                    ),
                ),
                VisualFrame(
                    "③ 20 人：20×19÷2 = 190 条，沟通量翻十几倍",
                    nodes = listOf(
                        bar("x", "", 0.12f, 0.35f, 0.25f, VisualTone.MUTED, caption="3 人 3 条"),
                        bar("y", "", 0.4f, 0.35f, 0.5f, VisualTone.ACTIVE, caption="6 人 15 条"),
                        bar("z", "", 0.68f, 0.35f, 1.0f, VisualTone.BAD, caption="20 人 190 条"),
                        text("n", "人数翻几倍，沟通渠道按平方涨", 0.15f, 0.82f, "", 0.6f, 0.1f, VisualTone.BAD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-quality-cost",
            title = "质量成本：钱该花在哪",
            examId = SI,
            chapter = "质量工具与质量成本",
            keywords = listOf("质量成本", "预防成本", "评估成本", "内部失败", "外部失败", "一致性成本"),
            takeaway = "预防成本花 1 块，能省下数块外部失败成本；测试太晚 = 缺陷成本爆炸。",
            frames = listOf(
                VisualFrame(
                    "① 预防成本：需求评审、培训、统一规范 —— 花在缺陷发生之前",
                    nodes = listOf(
                        box("p", "预防成本", 0.08f, 0.25f, VisualTone.GOOD, w = 0.25f, caption="需求评审 / 培训"),
                        box("a", "评估成本", 0.08f, 0.72f, VisualTone.ACTIVE, w = 0.25f, caption="测试 / 评审"),
                        text("t", "一致性成本 ↑", 0.45f, 0.3f, "", 0.3f, 0.1f, VisualTone.GOOD),
                    ),
                ),
                VisualFrame(
                    "② 失败成本：内部（返工）vs 外部（保修、索赔、声誉）",
                    nodes = listOf(
                        box("i", "内部失败", 0.35f, 0.25f, VisualTone.ACTIVE, w = 0.25f, caption="缺陷已发现，尚未流出"),
                        box("e", "外部失败", 0.35f, 0.72f, VisualTone.BAD, w = 0.25f, caption="客户已发现，最贵"),
                        text("t", "非一致性成本 ↑", 0.7f, 0.5f, "", 0.3f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 越早发现问题越便宜：预防 > 评估 > 内部失败 > 外部失败",
                    nodes = listOf(
                        bar("b1", "", 0.06f, 0.4f, 0.2f, VisualTone.GOOD, caption="预防"),
                        bar("b2", "", 0.28f, 0.4f, 0.4f, VisualTone.ACTIVE, caption="评估"),
                        bar("b3", "", 0.5f, 0.4f, 0.7f, VisualTone.BAD, caption="内部失败"),
                        bar("b4", "", 0.72f, 0.4f, 1.0f, VisualTone.BAD, caption="外部失败"),
                    ),
                ),
            ),
        ),

        // ---------------------------------------------------------------- 多媒体
        VisualLesson(
            id = "md-frames",
            title = "I / P / B 帧的依赖关系",
            examId = MD,
            chapter = "帧与编码",
            keywords = listOf("I帧", "P帧", "B帧", "关键帧", "GOP", "帧内编码", "前向预测", "双向预测"),
            takeaway = "I 帧独立可解码；P 帧依赖前一帧；B 帧依赖前后两帧，压缩率最高但不能随机访问。",
            frames = listOf(
                VisualFrame(
                    "① I 帧：帧内编码，自己就能还原",
                    nodes = listOf(
                        box("i0", "I", 0.06f, 0.45f, VisualTone.GOOD, w = 0.14f, caption="关键帧"),
                        box("p0", "P", 0.3f, 0.45f, VisualTone.NEUTRAL, w = 0.14f),
                        box("b0", "B", 0.5f, 0.45f, VisualTone.NEUTRAL, w = 0.14f),
                        box("i1", "I", 0.74f, 0.45f, VisualTone.GOOD, w = 0.14f, caption="下一 GOP"),
                        text("t", "解码起点", 0.05f, 0.72f, "", 0.24f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("i0", "p0"), edge("p0", "b0"), edge("b0", "i1")),
                ),
                VisualFrame(
                    "② P 帧：前向预测，依赖它前面那一帧",
                    nodes = listOf(
                        box("i0", "I", 0.06f, 0.45f, VisualTone.GOOD, w = 0.14f),
                        box("p0", "P", 0.3f, 0.45f, VisualTone.ACTIVE, w = 0.14f, caption="依赖前面"),
                        box("b0", "B", 0.5f, 0.45f, VisualTone.NEUTRAL, w = 0.14f),
                        box("i1", "I", 0.74f, 0.45f, VisualTone.GOOD, w = 0.14f),
                        text("t", "箭头方向 = 依赖方向", 0.25f, 0.72f, "", 0.4f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("i0", "p0", active = true), edge("p0", "b0"), edge("b0", "i1")),
                ),
                VisualFrame(
                    "③ B 帧：双向预测，同时依赖前后两帧",
                    nodes = listOf(
                        box("i0", "I", 0.06f, 0.45f, VisualTone.GOOD, w = 0.14f),
                        box("p0", "P", 0.3f, 0.45f, VisualTone.ACTIVE, w = 0.14f),
                        box("b0", "B", 0.5f, 0.45f, VisualTone.ACTIVE, w = 0.14f, caption="依赖前后"),
                        box("i1", "I", 0.74f, 0.45f, VisualTone.GOOD, w = 0.14f),
                    ),
                    edges = listOf(
                        edge("i0", "p0", active = true),
                        edge("p0", "b0", active = true),
                        edge("b0", "i1", active = true),
                    ),
                ),
                VisualFrame(
                    "④ 压缩率：I 最低、B 最高；码率 I > P > B",
                    nodes = listOf(
                        bar("bi", "I", 0.12f, 0.42f, 1.0f, VisualTone.MUTED, caption="压缩最弱"),
                        bar("bp", "P", 0.42f, 0.42f, 0.6f, VisualTone.ACTIVE, caption="中等"),
                        bar("bb", "B", 0.72f, 0.42f, 0.3f, VisualTone.GOOD, caption="压缩最强"),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-jpeg",
            title = "JPEG 编码五步在做什么",
            examId = MD,
            chapter = "JPEG与DCT",
            keywords = listOf("JPEG", "DCT", "量化", "行程编码", "熵编码", "FDCT", "8×8"),
            takeaway = "FDCT 变频率域 → 量化丢高频 → DPCM 压 DC → 行程编码压 AC → 熵编码再压一遍。",
            frames = listOf(
                VisualFrame(
                    "① FDCT：把 8×8 像素块从空间域变到频率域，能量集中到左上角",
                    nodes = listOf(
                        box("blk", "8×8 块", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, caption="空间域"),
                        text("dct", "低频 ▓▓▓░", 0.4f, 0.45f, "频率域：能量集中在左上", 0.34f, 0.1f, VisualTone.ACTIVE),
                        text("t", "DCT 是 DFT 在实偶函数下的特例", 0.3f, 0.74f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("blk", "dct", active = true)),
                ),
                VisualFrame(
                    "② 量化：把系数除以量化步长并取整，高频被抹平（不可逆）",
                    nodes = listOf(
                        box("blk", "8×8 块", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.22f),
                        box("q", "量化", 0.34f, 0.4f, VisualTone.ACTIVE, w = 0.2f, caption="高频→0"),
                        box("out", "系数矩阵", 0.6f, 0.4f, VisualTone.MUTED, w = 0.24f),
                        text("t", "有损就发生在这一步", 0.3f, 0.74f, "", 0.4f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("blk", "q", active = true), edge("q", "out")),
                ),
                VisualFrame(
                    "③ DPCM：对 DC 系数做差分预测（相邻块相似）",
                    nodes = listOf(
                        box("q", "量化后", 0.05f, 0.4f, VisualTone.MUTED, w = 0.2f),
                        box("dc", "DC 差分", 0.31f, 0.4f, VisualTone.ACTIVE, w = 0.22f, caption="只留与邻块的差"),
                        box("ac", "AC 系数", 0.6f, 0.4f, VisualTone.MUTED, w = 0.2f, caption="留待下一步"),
                    ),
                    edges = listOf(edge("q", "dc", active = true), edge("q", "ac")),
                ),
                VisualFrame(
                    "④ 行程编码：把 AC 中的零串成「零游程」",
                    nodes = listOf(
                        box("dc", "DC 差分", 0.05f, 0.4f, VisualTone.MUTED, w = 0.2f),
                        box("rle", "RLE 行程", 0.32f, 0.4f, VisualTone.ACTIVE, w = 0.22f, caption="0×12, 5, 0×3 …"),
                        box("ac", "AC 系数", 0.62f, 0.4f, VisualTone.MUTED, w = 0.2f),
                    ),
                    edges = listOf(edge("dc", "rle", active = true), edge("ac", "rle")),
                ),
                VisualFrame(
                    "⑤ 熵编码（Huffman）：对最终符号再压一次，到此形成码流",
                    nodes = listOf(
                        box("rle", "RLE 行程", 0.05f, 0.4f, VisualTone.MUTED, w = 0.22f),
                        box("huff", "Huffman 熵编码", 0.34f, 0.4f, VisualTone.ACTIVE, w = 0.26f, caption="高频符号短码"),
                        box("bit", "JPEG 码流", 0.68f, 0.4f, VisualTone.GOOD, w = 0.22f),
                        text("t", "解码按相反顺序还原", 0.3f, 0.74f, "", 0.4f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("rle", "huff", active = true), edge("huff", "bit", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "md-sample",
            title = "采样与量化：波形怎么变成数字",
            examId = MD,
            chapter = "数字音频",
            keywords = listOf("采样", "量化", "采样定理", "混叠", "位深", "采样频率"),
            takeaway = "采样点不够会出现混叠；位深决定量化台阶数，16 bit = 65536 级。",
            frames = listOf(
                VisualFrame(
                    "① 连续信号：一条平滑的波",
                    nodes = listOf(
                        text("wave", "∿∿∿∿∿∿∿∿∿∿", 0.1f, 0.45f, "模拟信号", 0.8f, 0.14f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 采样：按固定间隔取值（每秒取 44100 次）",
                    nodes = listOf(
                        text("wave", "∿∿∿∿∿∿∿∿∿∿", 0.1f, 0.3f, "采样点", 0.8f, 0.14f, VisualTone.ACTIVE),
                        text("t", "44.1 kHz", 0.1f, 0.6f, "采样频率 ≥ 信号最高频率 × 2", 0.8f, 0.1f, VisualTone.ACTIVE),
                    ),
                ),
                VisualFrame(
                    "③ 量化：每个采样值取整到最近的台阶（16 bit → 65536 级）",
                    nodes = listOf(
                        text("wave", "∿∿∿∿∿∿∿∿∿∿", 0.1f, 0.28f, "阶梯化", 0.8f, 0.14f, VisualTone.ACTIVE),
                        text("q", "▁▄▆█▆▄▂▁▃▅", 0.1f, 0.58f, "量化后的数字", 0.8f, 0.14f, VisualTone.GOOD),
                        text("t", "位深越大，台阶越密，量化误差越小", 0.1f, 0.82f, "", 0.8f, 0.1f, VisualTone.GOOD),
                    ),
                ),
                VisualFrame(
                    "④ 采样频率低于 2×最高频率 → 混叠失真，音频不可还原",
                    nodes = listOf(
                        text("wave", "∿∿∿∿∿∿∿∿∿∿", 0.1f, 0.3f, "高频信号", 0.8f, 0.14f, VisualTone.MUTED),
                        text("alias", "∿﹏∿﹏∿﹏∿﹏∿﹏", 0.1f, 0.6f, "被采样成低频「假音」", 0.8f, 0.14f, VisualTone.BAD),
                        text("t", "奈奎斯特定理：fs ≥ 2 × fmax", 0.1f, 0.84f, "", 0.8f, 0.1f, VisualTone.BAD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-color",
            title = "加色与减色：屏幕与印刷",
            examId = MD,
            chapter = "色彩与色彩管理",
            keywords = listOf("RGB", "CMYK", "色相", "明度", "饱和度", "色彩模型", "加色", "减色"),
            takeaway = "RGB 加色（光越多越亮，用于屏幕）；CMYK 减色（油墨越多越暗，用于印刷）。",
            frames = listOf(
                VisualFrame(
                    "① RGB 加色：三原色圆叠在一起，重叠越多越亮",
                    nodes = listOf(
                        circle("r", "R", 0.3f, 0.35f, VisualTone.BAD, w = 0.3f, h = 0.34f),
                        circle("g", "G", 0.42f, 0.35f, VisualTone.GOOD, w = 0.3f, h = 0.34f),
                        circle("b", "B", 0.36f, 0.52f, VisualTone.ACTIVE, w = 0.3f, h = 0.34f),
                        text("t", "屏幕发光用 RGB", 0.62f, 0.5f, "", 0.32f, 0.1f, VisualTone.ACTIVE),
                    ),
                ),
                VisualFrame(
                    "② CMYK 减色：印刷油墨叠加，越叠越暗",
                    nodes = listOf(
                        circle("c", "C", 0.3f, 0.35f, VisualTone.ACTIVE, w = 0.28f, h = 0.32f),
                        circle("m", "M", 0.42f, 0.35f, VisualTone.BAD, w = 0.28f, h = 0.32f),
                        circle("y", "Y", 0.36f, 0.52f, VisualTone.GOOD, w = 0.28f, h = 0.32f),
                        circle("k", "K", 0.36f, 0.43f, VisualTone.MUTED, w = 0.22f, h = 0.24f, caption="K 专管黑"),
                        text("t", "印刷用 CMYK", 0.62f, 0.5f, "", 0.32f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "③ 三属性：色相（是什么色）、明度（多亮）、饱和度（多艳）",
                    nodes = listOf(
                        bar("h", "色相", 0.08f, 0.42f, 1.0f, VisualTone.ACTIVE, caption="红黄蓝"),
                        bar("s", "明度", 0.38f, 0.42f, 0.6f, VisualTone.MUTED, caption="黑白"),
                        bar("v", "饱和度", 0.68f, 0.42f, 0.8f, VisualTone.GOOD, caption="艳与灰"),
                    ),
                ),
            ),
        ),
    )

    /** 全部图解（四批合并） */
    private val allLessons: List<VisualLesson>
        get() = lessons + VisualRepositoryExtra.lessons + VisualRepositoryThird.lessons +
            VisualRepositoryFourth.lessons

    fun forExam(examId: String): List<VisualLesson> = allLessons.filter { it.examId == examId }

    fun forChapter(examId: String, chapter: String): List<VisualLesson> =
        allLessons.filter { it.examId == examId && it.chapter == chapter }

    fun all(): List<VisualLesson> = allLessons
}
