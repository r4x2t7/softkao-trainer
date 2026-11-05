package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 第七批知识动画：SI 三点估算 PERT / 前推后推法 / 沟通方式三分类 / 招投标时间线，
 * MD 图像缩放插值 / 音频编辑流程。
 */
object VisualRepositorySeventh {

    private fun box(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.2f, h: Float = 0.13f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BOX, 0f, tone, caption)

    private fun circle(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.16f, h: Float = 0.16f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.CIRCLE, 0f, tone, caption)

    private fun text(id: String, label: String, x: Float, y: Float, caption: String = "", w: Float = 0.3f, h: Float = 0.1f, tone: VisualTone = VisualTone.NEUTRAL) =
        VisualNode(id, label, x, y, w, h, VisualKind.TEXT, 0f, tone, caption)

    private fun edge(from: String, to: String, active: Boolean = false, dashed: Boolean = false, label: String = "") =
        VisualEdge(from, to, active, dashed, label)

    val lessons: List<VisualLesson> = listOf(
        // ============ SI 1：三点估算 PERT ============
        VisualLesson(
            id = "si-pert-beta",
            title = "三点估算：PERT 贝塔分布 vs 三角分布",
            examId = VisualRepository.SI,
            chapter = "项目进度管理",
            keywords = listOf("三点估算", "PERT", "贝塔", "期望值", "标准差", "乐观", "悲观", "最可能"),
            takeaway = "PERT 贝塔分布期望 = (O+4M+P)/6，σ=(P−O)/6；三角分布 = (O+M+P)/3。最可能值 M 权重是 4 倍——「加权」是两者的分水岭。",
            frames = listOf(
                VisualFrame(
                    "① 收集三个估计值：乐观 O、最可能 M、悲观 P",
                    nodes = listOf(
                        box("o", "乐观 O\n一切顺利", 0.04f, 0.4f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        box("m", "最可能 M\n常规情况", 0.37f, 0.4f, VisualTone.ACTIVE, w = 0.26f, h = 0.2f),
                        box("p", "悲观 P\n处处受阻", 0.7f, 0.4f, VisualTone.BAD, w = 0.26f, h = 0.2f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② PERT 贝塔加权：期望 = (O + 4M + P) ÷ 6 —— M 占 4 票",
                    nodes = listOf(
                        text("f1", "E = (O + 4M + P) / 6", 0.2f, 0.36f, w = 0.6f, tone = VisualTone.PATH),
                        text("why", "M 乘 4：最可能值最可信，加权压低极端值影响", 0.1f, 0.6f, w = 0.8f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 三角分布：期望 = (O + M + P) ÷ 3 —— 三值等权",
                    nodes = listOf(
                        text("f2", "E = (O + M + P) / 3", 0.2f, 0.36f, w = 0.6f, tone = VisualTone.GOOD),
                        text("cmp", "对比：三角分布尊重极端值，PERT 抑制极端值", 0.08f, 0.6f, w = 0.8f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 标准差与概率：σ = (P−O)/6，±1σ 覆盖约 68%",
                    nodes = listOf(
                        text("sd", "σ = (P − O) / 6", 0.24f, 0.3f, w = 0.5f, tone = VisualTone.PATH),
                        text("p1", "±1σ ≈ 68%　±2σ ≈ 95%　±3σ ≈ 99%", 0.1f, 0.54f, w = 0.8f),
                        text("eg", "例：O=8, M=12, P=22 → E=(8+48+22)/6=13，σ=(22-8)/6≈2.33", 0.04f, 0.78f, w = 0.9f, tone = VisualTone.ACTIVE),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 2：前推后推法 ============
        VisualLesson(
            id = "si-forward-backward",
            title = "前推后推法：ES/EF 与 LS/LF 求总时差",
            examId = VisualRepository.SI,
            chapter = "进度网络图与关键路径法",
            keywords = listOf("最早开始", "最晚开始", "总时差", "关键路径", "前推", "后推", "ES", "LS"),
            takeaway = "顺推取最大得 ES/EF，逆推取最小得 LS/LF；总时差 = LS − ES = LF − EF；总时差为零的活动串成关键路径。",
            frames = listOf(
                VisualFrame(
                    "① 顺推（从左到右）：ES = 紧前 EF 的最大值，EF = ES + 工期",
                    nodes = listOf(
                        box("a", "A\nES=0 EF=3", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f),
                        box("b", "B\nES=3 EF=7", 0.4f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f),
                        text("rule1", "顺推取最大：多条紧前汇入时取最大 EF", 0.24f, 0.7f, w = 0.6f),
                    ),
                    edges = listOf(edge("a", "b", active = true)),
                ),
                VisualFrame(
                    "② 逆推（从右到左）：LF = 紧后 LS 的最小值，LS = LF − 工期",
                    nodes = listOf(
                        box("b2", "B\nLS=3 LF=7", 0.4f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("c", "C\nLS=7 LF=10", 0.74f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        text("rule2", "逆推取最小：多条紧后分叉时取最小 LS", 0.24f, 0.7f, w = 0.6f),
                    ),
                    edges = listOf(edge("b2", "c", active = true)),
                ),
                VisualFrame(
                    "③ 总时差 = LS − ES（或 LF − EF），两种算法结果相同",
                    nodes = listOf(
                        text("tf1", "总时差 = LS − ES = LF − EF", 0.16f, 0.36f, w = 0.6f, tone = VisualTone.PATH),
                        text("eg2", "某活动 ES=3 EF=7 LS=5 LF=9 → TF = 2 天", 0.1f, 0.6f, w = 0.7f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 关键路径 = 总时差为零的活动链：一条不能延误的线",
                    nodes = listOf(
                        box("cp1", "A TF=0", 0.06f, 0.4f, VisualTone.BAD, w = 0.2f, h = 0.18f),
                        box("cp2", "B TF=0", 0.32f, 0.4f, VisualTone.BAD, w = 0.2f, h = 0.18f),
                        box("cp3", "C TF=0", 0.58f, 0.4f, VisualTone.BAD, w = 0.2f, h = 0.18f),
                        text("cp4", "关键活动延误一天 = 工期延误一天", 0.14f, 0.7f, w = 0.6f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(edge("cp1", "cp2", active = true), edge("cp2", "cp3", active = true)),
                ),
            ),
        ),

        // ============ SI 3：沟通方式三分类 ============
        VisualLesson(
            id = "si-comm-three-modes",
            title = "沟通方式三分类：交互式 / 推式 / 拉式",
            examId = VisualRepository.SI,
            chapter = "项目沟通管理",
            keywords = listOf("沟通方式", "交互式", "推式", "拉式", "沟通技术", "沟通分类"),
            takeaway = "交互式一来一回（会议/访谈）效率最高；推式我发你收（邮件/报告）不能确保送达；拉式你来取（知识库/门户）适合大受众。敏感事项用正式书面。",
            frames = listOf(
                VisualFrame(
                    "① 交互式：一来一回、即时反馈——需求澄清与共识的利器",
                    nodes = listOf(
                        circle("a", "A ⇄ B", 0.3f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.24f),
                        text("e1", "会议 / 访谈 / 视频会 / 即时对话", 0.5f, 0.42f, w = 0.48f),
                        text("w1", "效率最高，但占双方时间", 0.24f, 0.7f, w = 0.5f, tone = VisualTone.GOOD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 推式：我发你收——邮件、报告、信函（不确认送达）",
                    nodes = listOf(
                        box("sender", "发送方", 0.08f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f),
                        box("recv", "接收方", 0.66f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.16f),
                        text("w2", "发完不等于收到，更不等于看懂", 0.2f, 0.66f, w = 0.6f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(edge("sender", "recv", active = true, label = "单向推送")),
                ),
                VisualFrame(
                    "③ 拉式：信息放公共位，受众自己来取——知识库/内网门户",
                    nodes = listOf(
                        box("lib", "知识库\n内网门户", 0.3f, 0.4f, VisualTone.GOOD, w = 0.3f, h = 0.2f),
                        text("crowd", "受众按需取用 ↑", 0.3f, 0.14f, w = 0.4f),
                        text("w3", "信息量大、受众多、可自查", 0.22f, 0.68f, w = 0.56f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 选择逻辑：要共识→交互式；要广而告→推式；要长期自查→拉式",
                    nodes = listOf(
                        text("s1", "需求澄清 / 冲突处理 → 交互式", 0.06f, 0.14f, w = 0.6f),
                        text("s2", "进度周报 / 通知公告 → 推式", 0.06f, 0.4f, w = 0.6f),
                        text("s3", "经验沉淀 / 制度文档 → 拉式", 0.06f, 0.66f, w = 0.6f, tone = VisualTone.PATH),
                        text("s4", "重要敏感事项 → 正式书面（记入档案）", 0.06f, 0.9f, w = 0.66f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 4：招投标时间线 ============
        VisualLesson(
            id = "si-bidding-deadlines",
            title = "招投标时间线：5 日 / 20 日 / 30 日三个刻度",
            examId = VisualRepository.SI,
            chapter = "项目采购管理",
            keywords = listOf("招标", "投标", "时间", "期限", "发售期", "投标准备期", "中标通知书", "签订合同"),
            takeaway = "三个数字各管一段：招标文件发售期 ≥5 日；投标准备期 ≥20 日（自发出至截止）；中标通知书发出后 30 日内订立合同。",
            frames = listOf(
                VisualFrame(
                    "① 第一刻度：招标文件发售期不少于 5 日",
                    nodes = listOf(
                        box("t1", "开始发出\n—\n停止出售", 0.1f, 0.36f, VisualTone.ACTIVE, w = 0.32f, h = 0.24f),
                        text("d1", "≥ 5 日", 0.5f, 0.42f, w = 0.2f, tone = VisualTone.PATH),
                        text("w1", "保证潜在投标人有渠道拿到文件", 0.24f, 0.7f, w = 0.6f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 第二刻度：自招标文件发出至投标截止 ≥20 日（投标准备期）",
                    nodes = listOf(
                        box("t2", "文件发出\n—\n投标截止", 0.1f, 0.36f, VisualTone.ACTIVE, w = 0.34f, h = 0.24f),
                        text("d2", "≥ 20 日", 0.52f, 0.42f, w = 0.24f, tone = VisualTone.PATH),
                        text("w2", "给投标人编标书的时间——压缩此期限属程序违规", 0.14f, 0.7f, w = 0.72f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 第三刻度：中标通知书发出之日起 30 日内订立书面合同",
                    nodes = listOf(
                        box("t3", "中标通知书\n发出", 0.1f, 0.36f, VisualTone.GOOD, w = 0.3f, h = 0.22f),
                        text("d3", "→ 30 日内签约", 0.46f, 0.42f, w = 0.34f, tone = VisualTone.PATH),
                        text("w3", "合同不得背离招标文件与投标文件的实质性内容", 0.12f, 0.7f, w = 0.72f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 全景对照：5/20/30 各有归属，出题最爱互换来钓鱼",
                    nodes = listOf(
                        text("s1", "5 日 → 管文件发售", 0.06f, 0.14f, w = 0.5f),
                        text("s2", "20 日 → 管投标准备", 0.06f, 0.4f, w = 0.5f, tone = VisualTone.ACTIVE),
                        text("s3", "30 日 → 管中标签约", 0.06f, 0.66f, w = 0.5f),
                        text("s4", "投标保证金 ≤估算价 2%；履约保证金 ≤合同价 10%", 0.04f, 0.9f, w = 0.86f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 1：图像缩放插值 ============
        VisualLesson(
            id = "md-interpolation-scaling",
            title = "图像缩放与插值：锯齿从哪来、怎么救",
            examId = VisualRepository.MD,
            chapter = "数字图形与图像基础",
            keywords = listOf("插值", "缩放", "锯齿", "最近邻", "双线性", "重采样", "图像缩放"),
            takeaway = "放大位图必须「无中生有」造新像素——最近邻直接复制（快但有锯齿），双线性取邻域加权（平滑），高倍放大锯齿不可根治。矢量图靠重绘永远锐利。",
            frames = listOf(
                VisualFrame(
                    "① 放大位图的本质：目标网格比原图密，新像素必须插值生成",
                    nodes = listOf(
                        box("src", "原图\n2×2 像素", 0.08f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f),
                        box("dst", "放大后\n4×4 像素", 0.5f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.2f),
                        text("q", "新多出来的 12 个像素值从哪来？", 0.14f, 0.7f, w = 0.66f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(edge("src", "dst")),
                ),
                VisualFrame(
                    "② 最近邻插值：直接抄最近的像素——快，但边缘成锯齿/马赛克",
                    nodes = listOf(
                        box("nn", "最近邻\n复制邻近像素", 0.1f, 0.4f, VisualTone.BAD, w = 0.3f, h = 0.2f, caption = "速度最快，质量最差"),
                        text("r1", "斜线放大 → 台阶状锯齿", 0.5f, 0.42f, w = 0.44f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 双线性插值：取周围 4 像素加权平均——过渡平滑",
                    nodes = listOf(
                        box("bl", "双线性\n4 邻域加权", 0.1f, 0.4f, VisualTone.GOOD, w = 0.3f, h = 0.2f, caption = "常用默认方案"),
                        text("r2", "边缘柔和、几乎无锯齿\n更高档：双三次/ Lanczos", 0.5f, 0.36f, w = 0.46f, tone = VisualTone.GOOD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 根治路径：要任意缩放不糊 → 用矢量图重绘，别放大位图",
                    nodes = listOf(
                        text("s1", "位图放大：不可避免的信息重建（选好插值算法）", 0.04f, 0.18f, w = 0.8f),
                        text("s2", "矢量放大：按目标尺寸重新绘制，永远锐利", 0.04f, 0.44f, w = 0.8f, tone = VisualTone.GOOD),
                        text("s3", "实务：Logo 用矢量源文件；照片只做小幅放大", 0.04f, 0.7f, w = 0.8f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 2：音频编辑流程 ============
        VisualLesson(
            id = "md-audio-edit-flow",
            title = "音频后期流程：剪辑 → 混音 → 母带",
            examId = VisualRepository.MD,
            chapter = "音频格式与编辑",
            keywords = listOf("音频编辑", "剪辑", "混音", "母带", "电平", "均衡", "降噪", "后期"),
            takeaway = "三段式后期：先剪辑修素材（去噪/对轨），再混音调平衡（电平/均衡/效果），最后母带做整体响度与格式输出——顺序不能乱，母带前别动响度。",
            frames = listOf(
                VisualFrame(
                    "① 剪辑：整理素材——去噪、剪切、对轨、修瑕疵",
                    nodes = listOf(
                        box("cut", "剪辑\n去噪/对轨/修剪", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.3f, h = 0.22f, caption = "素材层：先让每条音轨干净"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 混音：多轨平衡——电平、声像、均衡、效果器",
                    nodes = listOf(
                        box("cut", "干净素材", 0.04f, 0.4f, VisualTone.MUTED, w = 0.2f, h = 0.16f),
                        box("mix", "混音\n电平/均衡/声像", 0.34f, 0.38f, VisualTone.GOOD, w = 0.32f, h = 0.22f, caption = "各轨协调：人声清晰、配乐不抢"),
                    ),
                    edges = listOf(edge("cut", "mix", active = true)),
                ),
                VisualFrame(
                    "③ 母带：对整体做响度统一与格式输出——最后一道工序",
                    nodes = listOf(
                        box("mix", "混音成品", 0.04f, 0.4f, VisualTone.MUTED, w = 0.22f, h = 0.16f),
                        box("mb", "母带处理\n响度/限制器/导出", 0.36f, 0.38f, VisualTone.PATH, w = 0.34f, h = 0.22f, caption = "整体视角：不再改单轨"),
                    ),
                    edges = listOf(edge("mix", "mb", active = true)),
                ),
                VisualFrame(
                    "④ 顺序铁律：剪辑未干净别混音，混音未定别做母带",
                    nodes = listOf(
                        text("s1", "素材层 → 平衡层 → 整体层，单向流水线", 0.06f, 0.2f, w = 0.7f, tone = VisualTone.PATH),
                        text("s2", "常见错误：混音阶段调响度→母带无从下手", 0.06f, 0.48f, w = 0.7f, tone = VisualTone.BAD),
                        text("s3", "导出规格按用途定：CD 44.1kHz / 视频 48kHz", 0.06f, 0.76f, w = 0.7f),
                    ),
                    edges = listOf(),
                ),
            ),
        ),
    )
}
