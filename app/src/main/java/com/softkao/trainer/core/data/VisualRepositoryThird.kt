package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 抽象图解（第三批）：进度压缩、风险矩阵、合同类型、组织结构、WBS 分解、
 * 质量工具、MPEG 体系、矢量与位图、Huffman 编码、信噪比与动态范围。
 */
object VisualRepositoryThird {

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
        // ---------------------------------------------------------------- 系统集成

        VisualLesson(
            id = "si-schedule-compress",
            title = "进度压缩：赶工与快速跟进",
            examId = VisualRepository.SI,
            chapter = "项目进度管理",
            keywords = listOf("进度压缩", "赶工", "快速跟进", "并行", "关键路径", "工期", "缩短"),
            takeaway = "赶工＝加钱加人换工期，风险最小；快速跟进＝并行抢工期，风险最高。只压关键路径上的活动。",
            frames = listOf(
                VisualFrame(
                    "① 关键路径上三段活动串行，总工期 12 周",
                    nodes = listOf(
                        box("a", "活动A 4周", 0.05f, 0.38f, VisualTone.ACTIVE, w = 0.24f),
                        box("b", "活动B 5周", 0.38f, 0.38f, VisualTone.PATH, w = 0.24f),
                        box("c", "活动C 3周", 0.71f, 0.38f, VisualTone.ACTIVE, w = 0.24f),
                        text("t", "工期 12 周", 0.35f, 0.66f, "", 0.3f, 0.1f, VisualTone.PATH),
                    ),
                    edges = listOf(edge("a", "b", active = true), edge("b", "c", active = true)),
                ),
                VisualFrame(
                    "② 赶工 Crashing：投入更多资源把活动压短，成本上升",
                    nodes = listOf(
                        box("a", "活动A 3周", 0.05f, 0.38f, VisualTone.ACTIVE, w = 0.24f, caption="加班/加设备"),
                        box("b", "活动B 4周", 0.38f, 0.38f, VisualTone.ACTIVE, w = 0.24f, caption="增加人手"),
                        box("c", "活动C 3周", 0.71f, 0.38f, VisualTone.ACTIVE, w = 0.24f),
                        text("t", "工期 ↓ 成本 ↑ 风险 小", 0.3f, 0.68f, "", 0.4f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("a", "b", active = true), edge("b", "c", active = true)),
                ),
                VisualFrame(
                    "③ 快速跟进 Fast Tracking：把原本串行的活动改成并行",
                    nodes = listOf(
                        box("a", "活动A", 0.08f, 0.3f, VisualTone.ACTIVE, w = 0.24f),
                        box("b", "活动B", 0.08f, 0.56f, VisualTone.ACTIVE, w = 0.24f),
                        box("c", "活动C", 0.55f, 0.42f, VisualTone.BAD, w = 0.24f, caption="并行有返工风险"),
                        text("t", "工期 ↓ 成本 不变 风险 大", 0.28f, 0.05f, "", 0.44f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("a", "c", active = true), edge("b", "c", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "si-risk-matrix",
            title = "风险矩阵与风险登记册",
            examId = VisualRepository.SI,
            chapter = "项目风险管理",
            keywords = listOf("风险矩阵", "概率", "影响", "风险登记册", "风险等级", "风险评级", "定性风险"),
            takeaway = "定性风险分析＝概率×影响定等级：高概率高影响是红线，先管；登记册要持续更新。",
            frames = listOf(
                VisualFrame(
                    "① 矩阵两轴：发生概率（纵）× 影响程度（横）",
                    nodes = listOf(
                        box("hi", "高", 0.08f, 0.22f, VisualTone.BAD, w = 0.16f, h = 0.16f, caption="概率高"),
                        box("mi", "中", 0.08f, 0.44f, VisualTone.ACTIVE, w = 0.16f, h = 0.16f),
                        box("lo", "低", 0.08f, 0.66f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("i3", "影响大", 0.38f, 0.22f, VisualTone.NEUTRAL, w = 0.2f, h = 0.16f),
                        box("i2", "影响中", 0.38f, 0.44f, VisualTone.NEUTRAL, w = 0.2f, h = 0.16f),
                        box("i1", "影响小", 0.38f, 0.66f, VisualTone.NEUTRAL, w = 0.2f, h = 0.16f),
                        text("t", "每个风险落到一个格子", 0.66f, 0.4f, "", 0.32f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 风险点落位：越靠右上越危险",
                    nodes = listOf(
                        box("cell1", "", 0.3f, 0.2f, VisualTone.BAD, w = 0.24f, h = 0.2f),
                        box("cell2", "", 0.3f, 0.46f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f),
                        box("cell3", "", 0.3f, 0.72f, VisualTone.MUTED, w = 0.24f, h = 0.2f),
                        circle("r1", "风险A", 0.34f, 0.22f, VisualTone.NEUTRAL, w = 0.16f, h = 0.16f),
                        circle("r2", "风险B", 0.36f, 0.5f, VisualTone.NEUTRAL, w = 0.16f, h = 0.16f),
                        circle("r3", "风险C", 0.38f, 0.76f, VisualTone.NEUTRAL, w = 0.16f, h = 0.16f),
                        text("t", "右上＝红区，优先制定应对", 0.6f, 0.35f, "", 0.36f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 风险登记册：编号、描述、责任人、概率、影响、应对措施、状态",
                    nodes = listOf(
                        box("id", "编号", 0.04f, 0.4f, VisualTone.ACTIVE, w = 0.14f),
                        box("desc", "描述", 0.2f, 0.4f, VisualTone.ACTIVE, w = 0.14f),
                        box("own", "责任人", 0.36f, 0.4f, VisualTone.ACTIVE, w = 0.14f),
                        box("pg", "概率×影响", 0.52f, 0.4f, VisualTone.PATH, w = 0.18f),
                        box("plan", "应对措施", 0.72f, 0.4f, VisualTone.GOOD, w = 0.18f),
                        text("t", "登记册是风险的唯一台账，持续维护", 0.15f, 0.68f, "", 0.6f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-contract-type",
            title = "四种合同类型与风险分担",
            examId = VisualRepository.SI,
            chapter = "项目合同管理",
            keywords = listOf("合同类型", "总价合同", "成本补偿", "工料", "成本加固定费用", "固定总价", "合同风险"),
            takeaway = "卖方风险：总价＜成本补偿。成本越高越要选总价；范围越清楚越适合固定总价。",
            frames = listOf(
                VisualFrame(
                    "① 四类合同",
                    nodes = listOf(
                        box("fp", "固定总价", 0.04f, 0.28f, VisualTone.ACTIVE, w = 0.21f, h = 0.16f, caption="范围清楚"),
                        box("cp", "成本补偿", 0.27f, 0.28f, VisualTone.ACTIVE, w = 0.21f, h = 0.16f, caption="范围不清"),
                        box("tm", "工料", 0.5f, 0.28f, VisualTone.ACTIVE, w = 0.21f, h = 0.16f, caption="成本+激励"),
                        box("cpff", "成本+固定费", 0.73f, 0.28f, VisualTone.ACTIVE, w = 0.21f, h = 0.16f, caption="加管理费"),
                        text("t", "合同类型＝风险分担方式", 0.26f, 0.6f, "", 0.44f, 0.1f, VisualTone.PATH),
                    ),
                ),
                VisualFrame(
                    "② 风险落在谁身上",
                    nodes = listOf(
                        box("s", "卖方", 0.06f, 0.36f, VisualTone.ACTIVE, w = 0.2f),
                        box("b", "买方", 0.72f, 0.36f, VisualTone.ACTIVE, w = 0.2f),
                        box("fp", "固定总价", 0.3f, 0.36f, VisualTone.BAD, w = 0.2f, h = 0.16f, caption="风险压卖方"),
                        text("t", "总价：卖方自己扛超支", 0.24f, 0.66f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("fp", "s", active = true), edge("fp", "b", active = true)),
                ),
                VisualFrame(
                    "③ 卖方风险由小到大",
                    nodes = listOf(
                        bar("r1", "总价", 0.06f, 0.42f, 0.2f, VisualTone.GOOD, caption="卖方风险小"),
                        bar("r2", "工料", 0.28f, 0.42f, 0.45f, VisualTone.ACTIVE, caption="中等"),
                        bar("r3", "成本补偿", 0.5f, 0.42f, 0.85f, VisualTone.BAD, caption="卖方风险大"),
                        text("t", "风险越大，卖方要求的合同价格越高", 0.14f, 0.06f, "", 0.6f, 0.1f, VisualTone.BAD),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-org-type",
            title = "组织结构类型：职能型、矩阵型、项目型",
            examId = VisualRepository.SI,
            chapter = "项目组织与团队",
            keywords = listOf("组织结构", "职能型", "矩阵型", "项目型", "组织图", "项目经理", "弱矩阵", "强矩阵"),
            takeaway = "职能型项目经理没实权，矩阵型有部分权力，项目型全权；越接近项目型越适合强矩阵。",
            frames = listOf(
                VisualFrame(
                    "① 职能型：项目经理只协调，无指挥权",
                    nodes = listOf(
                        box("pm", "项目经理", 0.4f, 0.12f, VisualTone.MUTED, w = 0.2f, h = 0.13f, caption="无实权"),
                        box("d1", "开发部", 0.06f, 0.45f, VisualTone.ACTIVE, w = 0.24f),
                        box("d2", "测试部", 0.38f, 0.45f, VisualTone.ACTIVE, w = 0.24f),
                        box("d3", "运维部", 0.7f, 0.45f, VisualTone.ACTIVE, w = 0.24f),
                        text("t", "人员归部门，项目结束就遣散", 0.2f, 0.72f, "", 0.56f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("d1", "pm", dashed = true), edge("d2", "pm", dashed = true), edge("d3", "pm", dashed = true)),
                ),
                VisualFrame(
                    "② 矩阵型：项目经理与部门经理共享权力",
                    nodes = listOf(
                        box("pm", "项目经理", 0.08f, 0.36f, VisualTone.ACTIVE, w = 0.2f),
                        box("fm", "部门经理", 0.72f, 0.36f, VisualTone.ACTIVE, w = 0.2f),
                        circle("s1", "成员", 0.4f, 0.3f, VisualTone.NEUTRAL, w = 0.18f, h = 0.18f),
                        text("t", "矩阵分强矩阵与弱矩阵：项目经理权力不同", 0.12f, 0.66f, "", 0.76f, 0.1f, VisualTone.PATH),
                    ),
                    edges = listOf(edge("pm", "s1", active = true), edge("fm", "s1", active = true)),
                ),
                VisualFrame(
                    "③ 项目型：项目经理全权，团队专职",
                    nodes = listOf(
                        box("pm", "项目经理", 0.38f, 0.14f, VisualTone.PATH, w = 0.24f, h = 0.14f, caption="全权"),
                        box("t1", "成员", 0.16f, 0.46f, VisualTone.GOOD, w = 0.2f),
                        box("t2", "成员", 0.4f, 0.46f, VisualTone.GOOD, w = 0.2f),
                        box("t3", "成员", 0.64f, 0.46f, VisualTone.GOOD, w = 0.2f),
                        text("t", "项目结束团队解散或转入新产品开发", 0.16f, 0.72f, "", 0.68f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("pm", "t1", active = true), edge("pm", "t2", active = true), edge("pm", "t3", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "si-wbs",
            title = "WBS 分解与工作包",
            examId = VisualRepository.SI,
            chapter = "项目范围管理",
            keywords = listOf("WBS", "工作分解结构", "工作包", "范围基准", "可交付成果", "滚动规划", "WBS词典"),
            takeaway = "WBS 只拆「可交付成果」，最底层是工作包（可估算、可指派、可验收），不拆活动。",
            frames = listOf(
                VisualFrame(
                    "① 从项目往下拆可交付成果",
                    nodes = listOf(
                        box("p", "项目", 0.38f, 0.1f, VisualTone.PATH, w = 0.24f, h = 0.13f),
                        box("d1", "产品管理", 0.08f, 0.42f, VisualTone.ACTIVE, w = 0.22f),
                        box("d2", "技术管理", 0.39f, 0.42f, VisualTone.ACTIVE, w = 0.22f),
                        box("d3", "运维支持", 0.7f, 0.42f, VisualTone.ACTIVE, w = 0.22f),
                        text("t", "只拆成果，不拆活动", 0.32f, 0.7f, "", 0.36f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("p", "d1", active = true), edge("p", "d2", active = true), edge("p", "d3", active = true)),
                ),
                VisualFrame(
                    "② 一直拆到「工作包」为止",
                    nodes = listOf(
                        box("d2", "技术管理", 0.36f, 0.1f, VisualTone.ACTIVE, w = 0.24f, h = 0.13f),
                        box("w1", "工作包\n需求评审", 0.1f, 0.44f, VisualTone.GOOD, w = 0.22f, h = 0.18f),
                        box("w2", "工作包\n接口设计", 0.38f, 0.44f, VisualTone.GOOD, w = 0.22f, h = 0.18f),
                        box("w3", "工作包\n系统测试", 0.66f, 0.44f, VisualTone.GOOD, w = 0.22f, h = 0.18f),
                        text("t", "工作包＝可估算、可指派、可验收的最小单元", 0.12f, 0.72f, "", 0.76f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("d2", "w1", active = true), edge("d2", "w2", active = true), edge("d2", "w3", active = true)),
                ),
                VisualFrame(
                    "③ 配套件：WBS 词典 + 编号规则（1.2.3）+ 范围基准",
                    nodes = listOf(
                        box("n1", "1.2.3", 0.08f, 0.38f, VisualTone.ACTIVE, w = 0.18f, h = 0.16f, caption="层级编号"),
                        box("n2", "WBS词典", 0.34f, 0.38f, VisualTone.ACTIVE, w = 0.22f, h = 0.16f, caption="内容+责任+工期"),
                        box("n3", "范围基准", 0.64f, 0.38f, VisualTone.GOOD, w = 0.22f, h = 0.16f, caption="WBS+范围说明书"),
                        text("t", "批准后成为范围基准，变更要走变更控制", 0.16f, 0.68f, "", 0.68f, 0.1f, VisualTone.PATH),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-quality-tools",
            title = "质量工具速查：因果图、控制图、散点图",
            examId = VisualRepository.SI,
            chapter = "项目质量管理",
            keywords = listOf("因果图", "鱼骨图", "控制图", "散点图", "直方图", "核对表", "质量工具", "4M1E"),
            takeaway = "因果图找原因、控制图看过程是否受控、散点图看两个变量相关性；因果图从 4M1E 入手。",
            frames = listOf(
                VisualFrame(
                    "① 因果图（鱼骨图）：从人机料法环查原因",
                    nodes = listOf(
                        box("prob", "质量问题", 0.72f, 0.4f, VisualTone.BAD, w = 0.22f, h = 0.16f),
                        box("spine", "", 0.06f, 0.4f, VisualTone.NEUTRAL, w = 0.5f, h = 0.05f),
                        box("m", "人", 0.16f, 0.16f, VisualTone.ACTIVE, w = 0.13f),
                        box("mm", "机", 0.34f, 0.16f, VisualTone.ACTIVE, w = 0.13f),
                        box("mt", "料", 0.52f, 0.16f, VisualTone.ACTIVE, w = 0.13f),
                        box("ms", "法", 0.16f, 0.62f, VisualTone.ACTIVE, w = 0.13f),
                        box("me", "环", 0.34f, 0.62f, VisualTone.ACTIVE, w = 0.13f),
                        text("t", "4M1E：人、机、料、法、环", 0.5f, 0.72f, "", 0.44f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("spine", "prob", active = true)),
                ),
                VisualFrame(
                    "② 控制图：上下控制限内的点子就是受控",
                    nodes = listOf(
                        bar("ucl", "上控制限", 0.08f, 0.3f, 0.85f, VisualTone.BAD, w = 0.12f, h = 0.5f),
                        bar("cl", "均值线", 0.32f, 0.3f, 0.5f, VisualTone.ACTIVE, w = 0.12f, h = 0.5f),
                        bar("lcl", "下控制限", 0.56f, 0.3f, 0.15f, VisualTone.GOOD, w = 0.12f, h = 0.5f),
                        circle("p1", "", 0.78f, 0.5f, VisualTone.NEUTRAL, w = 0.08f, h = 0.08f),
                        text("t", "点越界＝过程失控，需查原因", 0.2f, 0.86f, "", 0.5f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 散点图：两点之间画线，看相关性",
                    nodes = listOf(
                        circle("d1", "", 0.12f, 0.6f, VisualTone.ACTIVE, w = 0.09f, h = 0.09f),
                        circle("d2", "", 0.28f, 0.5f, VisualTone.ACTIVE, w = 0.09f, h = 0.09f),
                        circle("d3", "", 0.44f, 0.42f, VisualTone.ACTIVE, w = 0.09f, h = 0.09f),
                        circle("d4", "", 0.6f, 0.28f, VisualTone.ACTIVE, w = 0.09f, h = 0.09f),
                        circle("d5", "", 0.76f, 0.16f, VisualTone.ACTIVE, w = 0.09f, h = 0.09f),
                        text("t", "正相关：随一个变另一个也变", 0.22f, 0.82f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("d1", "d5", active = true)),
                ),
            ),
        ),

        // ---------------------------------------------------------------- 多媒体

        VisualLesson(
            id = "md-mpeg",
            title = "MPEG 编码体系与存储估算",
            examId = VisualRepository.MD,
            chapter = "数字视频与编码",
            keywords = listOf("MPEG", "编码", "码率", "帧率", "分辨率", "压缩比", "MP4", "流媒体"),
            takeaway = "存储量 ≈ 码率 × 时间；MPEG-2 适合 DVD/数字电视，MPEG-4 适合网络与移动。",
            frames = listOf(
                VisualFrame(
                    "① 一段视频的三个参数",
                    nodes = listOf(
                        box("res", "分辨率", 0.05f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f, caption="720×576"),
                        box("fps", "帧率", 0.31f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f, caption="25 帧/秒"),
                        box("br", "码率", 0.57f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.16f, caption="比特/秒"),
                        box("size", "存储量", 0.83f, 0.4f, VisualTone.PATH, w = 0.14f, h = 0.16f),
                        text("t", "码率×时间＝体积", 0.28f, 0.68f, "", 0.4f, 0.1f, VisualTone.PATH),
                    ),
                    edges = listOf(edge("res", "size", active = true), edge("fps", "size", active = true), edge("br", "size", active = true)),
                ),
                VisualFrame(
                    "② MPEG 家族各自的主场",
                    nodes = listOf(
                        box("m1", "MPEG-1", 0.05f, 0.32f, VisualTone.ACTIVE, w = 0.2f, h = 0.15f, caption="VCD/MP3"),
                        box("m2", "MPEG-2", 0.3f, 0.32f, VisualTone.ACTIVE, w = 0.2f, h = 0.15f, caption="DVD/数字电视"),
                        box("m4", "MPEG-4", 0.55f, 0.32f, VisualTone.GOOD, w = 0.2f, h = 0.15f, caption="网络/移动"),
                        box("m7", "MPEG-7", 0.8f, 0.32f, VisualTone.MUTED, w = 0.16f, h = 0.15f, caption="内容描述"),
                        text("t", "MPEG-7 是多媒体内容描述标准，不是压缩标准", 0.12f, 0.62f, "", 0.76f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 估体积：4 Mbps × 10 分钟 ≈ 300 MB",
                    nodes = listOf(
                        bar("q1", "1 Mbps", 0.08f, 0.42f, 0.25f, VisualTone.ACTIVE, caption="1分钟≈7.5MB"),
                        bar("q2", "4 Mbps", 0.34f, 0.42f, 0.8f, VisualTone.PATH, caption="10分钟≈300MB"),
                        bar("q3", "8 Mbps", 0.6f, 0.42f, 1.0f, VisualTone.BAD, caption="画质越好越大"),
                        text("t", "码率翻倍，体积翻倍", 0.24f, 0.82f, "", 0.4f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-vector-bitmap",
            title = "位图与矢量图",
            examId = VisualRepository.MD,
            chapter = "数字图像与图形",
            keywords = listOf("位图", "矢量图", "像素", "分辨率", "放大", "图形", "图像", "GIF", "JPG"),
            takeaway = "位图＝像素矩阵，放大会糊；矢量＝路径描述，放大不失真。照片用位图，标志和字体用矢量。",
            frames = listOf(
                VisualFrame(
                    "① 位图：一格一像素",
                    nodes = listOf(
                        box("g1", "", 0.06f, 0.3f, VisualTone.ACTIVE, w = 0.1f, h = 0.1f),
                        box("g2", "", 0.17f, 0.3f, VisualTone.ACTIVE, w = 0.1f, h = 0.1f),
                        box("g3", "", 0.28f, 0.3f, VisualTone.BAD, w = 0.1f, h = 0.1f),
                        box("g4", "", 0.06f, 0.41f, VisualTone.MUTED, w = 0.1f, h = 0.1f),
                        box("g5", "", 0.17f, 0.41f, VisualTone.GOOD, w = 0.1f, h = 0.1f),
                        box("g6", "", 0.28f, 0.41f, VisualTone.ACTIVE, w = 0.1f, h = 0.1f),
                        box("g7", "", 0.06f, 0.52f, VisualTone.MUTED, w = 0.1f, h = 0.1f),
                        box("g8", "", 0.17f, 0.52f, VisualTone.ACTIVE, w = 0.1f, h = 0.1f),
                        box("g9", "", 0.28f, 0.52f, VisualTone.ACTIVE, w = 0.1f, h = 0.1f),
                        text("t", "每个格子存一个颜色值", 0.46f, 0.4f, "", 0.34f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 矢量：存的是路径参数",
                    nodes = listOf(
                        box("path", "起点→控制点→终点", 0.1f, 0.3f, VisualTone.PATH, w = 0.34f, h = 0.16f),
                        circle("p1", "起点", 0.56f, 0.24f, VisualTone.GOOD, w = 0.14f, h = 0.14f),
                        circle("p2", "控制点", 0.56f, 0.5f, VisualTone.BAD, w = 0.14f, h = 0.14f),
                        text("t", "参数＋公式，任意缩放都清晰", 0.16f, 0.68f, "", 0.62f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("path", "p1", active = true), edge("path", "p2")),
                ),
                VisualFrame(
                    "③ 放大之后：位图糊，矢量锐",
                    nodes = listOf(
                        box("b1", "位图放大", 0.08f, 0.28f, VisualTone.ACTIVE, w = 0.26f, h = 0.16f),
                        box("b2", "", 0.36f, 0.28f, VisualTone.MUTED, w = 0.08f, h = 0.08f),
                        box("b3", "", 0.46f, 0.28f, VisualTone.MUTED, w = 0.08f, h = 0.08f),
                        box("b4", "", 0.56f, 0.28f, VisualTone.MUTED, w = 0.08f, h = 0.08f),
                        box("v1", "矢量放大", 0.08f, 0.58f, VisualTone.GOOD, w = 0.26f, h = 0.16f),
                        circle("v2", "", 0.36f, 0.56f, VisualTone.PATH, w = 0.14f, h = 0.14f),
                        circle("v3", "", 0.52f, 0.56f, VisualTone.PATH, w = 0.14f, h = 0.14f),
                        text("t", "照片／照片类素材用位图，标志／字体／插画用矢量", 0.1f, 0.82f, "", 0.8f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-huffman",
            title = "Huffman 编码：高频短码",
            examId = VisualRepository.MD,
            chapter = "数据压缩与编码",
            keywords = listOf("Huffman", "哈夫曼", "无损压缩", "编码", "前缀码", "压缩率", "冗余"),
            takeaway = "Huffman 构造最优前缀码：出现越频繁的符号编码越短；平均码长必小于定长编码。",
            frames = listOf(
                VisualFrame(
                    "① 先统计出现频率",
                    nodes = listOf(
                        bar("f1", "a", 0.08f, 0.42f, 0.9f, VisualTone.PATH, caption="40%"),
                        bar("f2", "b", 0.3f, 0.42f, 0.6f, VisualTone.ACTIVE, caption="25%"),
                        bar("f3", "c", 0.52f, 0.42f, 0.35f, VisualTone.MUTED, caption="20%"),
                        bar("f4", "d", 0.74f, 0.42f, 0.15f, VisualTone.NEUTRAL, caption="15%"),
                        text("t", "频率表决定码长", 0.28f, 0.8f, "", 0.4f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 每次合并两个最小的节点",
                    nodes = listOf(
                        circle("d", "d", 0.06f, 0.3f, VisualTone.NEUTRAL, w = 0.13f, h = 0.13f, caption="15%"),
                        circle("c", "c", 0.06f, 0.55f, VisualTone.MUTED, w = 0.13f, h = 0.13f, caption="20%"),
                        box("cd", "35", 0.28f, 0.42f, VisualTone.ACTIVE, w = 0.13f, h = 0.13f),
                        circle("b", "b", 0.52f, 0.3f, VisualTone.ACTIVE, w = 0.13f, h = 0.13f, caption="25%"),
                        circle("a", "a", 0.52f, 0.55f, VisualTone.PATH, w = 0.13f, h = 0.13f, caption="40%"),
                        box("root", "100", 0.76f, 0.42f, VisualTone.PATH, w = 0.14f, h = 0.13f),
                        text("t", "合并直到只剩一个根节点", 0.24f, 0.82f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("d", "cd", active = true), edge("c", "cd", active = true), edge("cd", "root", active = true), edge("b", "root", active = true), edge("a", "root", active = true)),
                ),
                VisualFrame(
                    "③ 编码结果：高频短码",
                    nodes = listOf(
                        box("ca", "a → 0", 0.08f, 0.3f, VisualTone.PATH, w = 0.22f, h = 0.15f, caption="最高频最短"),
                        box("cb", "b → 10", 0.34f, 0.3f, VisualTone.ACTIVE, w = 0.22f, h = 0.15f),
                        box("cc", "c → 110", 0.6f, 0.3f, VisualTone.MUTED, w = 0.22f, h = 0.15f),
                        box("cdd", "d → 111", 0.86f, 0.3f, VisualTone.NEUTRAL, w = 0.12f, h = 0.15f),
                        text("t", "前缀码：没有一个编码是另一个的前缀", 0.16f, 0.62f, "", 0.68f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-snr",
            title = "量化位数、动态范围与信噪比",
            examId = VisualRepository.MD,
            chapter = "音频参数与计算",
            keywords = listOf("量化", "量化位数", "动态范围", "信噪比", "采样率", "位深", "失真", "dB"),
            takeaway = "n 位量化动态范围 ≈ 6.02n + 1.76 dB；8 位约 48 dB，16 位约 98 dB，24 位约 146 dB。",
            frames = listOf(
                VisualFrame(
                    "① 量化就是把幅度切成有限台阶",
                    nodes = listOf(
                        box("l1", "8位 256 级", 0.06f, 0.34f, VisualTone.MUTED, w = 0.22f, h = 0.16f, caption="台阶粗"),
                        box("l2", "16位 65536 级", 0.34f, 0.34f, VisualTone.ACTIVE, w = 0.22f, h = 0.16f),
                        box("l3", "24位 1677 万级", 0.62f, 0.34f, VisualTone.GOOD, w = 0.22f, h = 0.16f, caption="台阶细"),
                        text("t", "台阶越细，量化噪声越小", 0.24f, 0.62f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                ),
                VisualFrame(
                    "② 动态范围公式：6.02n + 1.76 dB",
                    nodes = listOf(
                        bar("d8", "8bit", 0.1f, 0.42f, 0.33f, VisualTone.MUTED, caption="≈48 dB"),
                        bar("d16", "16bit", 0.36f, 0.42f, 0.67f, VisualTone.ACTIVE, caption="≈98 dB"),
                        bar("d24", "24bit", 0.62f, 0.42f, 1.0f, VisualTone.GOOD, caption="≈146 dB"),
                        text("t", "每加 1 位，动态范围约 +6 dB", 0.24f, 0.82f, "", 0.5f, 0.1f, VisualTone.PATH),
                    ),
                ),
                VisualFrame(
                    "③ 采样率与奈奎斯特：≥ 2× 最高频率",
                    nodes = listOf(
                        box("f", "声音最高频率", 0.06f, 0.4f, VisualTone.NEUTRAL, w = 0.24f, h = 0.16f),
                        box("s", "采样率 ≥ 2f", 0.42f, 0.4f, VisualTone.PATH, w = 0.22f, h = 0.16f),
                        box("ok", "可还原", 0.74f, 0.4f, VisualTone.GOOD, w = 0.18f, h = 0.16f),
                        text("t", "CD 44.1 kHz ≈ 2× 20 kHz 人耳上限", 0.14f, 0.68f, "", 0.7f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("f", "s", active = true), edge("s", "ok", active = true)),
                ),
            ),
        ),
    )
}
