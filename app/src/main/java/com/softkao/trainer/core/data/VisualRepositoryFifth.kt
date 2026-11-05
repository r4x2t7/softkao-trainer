package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 第五批知识动画：SI 配置库流转 / 变更控制 / 挣值动态 / 质量成本 / 干系人四象限，
 * MD 色彩三属性 / GOP 帧结构 / 采样量化 / 有损无损。
 */
object VisualRepositoryFifth {

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
        // ============ SI 1：配置库三库流转 ============
        VisualLesson(
            id = "si-config-lib-flow",
            title = "配置库三库流转：开发库 → 受控库 → 产品库",
            examId = VisualRepository.SI,
            chapter = "项目配置管理",
            keywords = listOf("配置库", "开发库", "受控库", "产品库", "检出", "检入", "基线"),
            takeaway = "开发库是动态区（可自由改）、受控库是受控区（改须审批+检出检入）、产品库是静态区（已发布基线存档只读）。受控库「直接改」是案例题必抓错误。",
            frames = listOf(
                VisualFrame(
                    "① 三个库各自的定位：动态区 / 受控区 / 静态区",
                    nodes = listOf(
                        box("dev", "开发库\n动态区", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f),
                        box("ctl", "受控库\n受控区", 0.4f, 0.4f, VisualTone.NEUTRAL, w = 0.24f, h = 0.2f),
                        box("prod", "产品库\n静态区", 0.74f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        text("d1", "自由修改", 0.06f, 0.68f, w = 0.24f),
                        text("d2", "变更控制", 0.4f, 0.68f, w = 0.24f),
                        text("d3", "只读存档", 0.74f, 0.68f, w = 0.24f),
                    ),
                    edges = listOf(edge("dev", "ctl", label = "提交"), edge("ctl", "prod", label = "发布")),
                ),
                VisualFrame(
                    "② 开发人员在开发库检出工作副本，随便改不犯规",
                    nodes = listOf(
                        box("dev", "开发库\n检出中", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f, caption = "工作副本可自由修改"),
                        box("ctl", "受控库\n基线 V1.2", 0.4f, 0.4f, w = 0.24f, h = 0.2f),
                        box("prod", "产品库\nV1.0 存档", 0.74f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                    ),
                    edges = listOf(edge("dev", "ctl", dashed = true, label = "检出 check-out")),
                ),
                VisualFrame(
                    "③ 改完想进受控库？必须走变更审批 + 检入，版本被记录",
                    nodes = listOf(
                        box("dev", "开发库\n修改完成", 0.06f, 0.4f, w = 0.24f, h = 0.2f),
                        box("ctl", "受控库\nV1.2→V1.3", 0.4f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.2f, caption = "变更申请→审批→检入"),
                        box("prod", "产品库\nV1.0 存档", 0.74f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        text("ccb", "CCB 审批", 0.4f, 0.12f, w = 0.24f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(edge("ccb", "ctl", active = true, label = "批准"), edge("dev", "ctl", active = true, label = "检入 check-in")),
                ),
                VisualFrame(
                    "④ 交付发布：受控库基线整体转入产品库存档",
                    nodes = listOf(
                        box("dev", "开发库", 0.06f, 0.4f, VisualTone.MUTED, w = 0.24f, h = 0.2f),
                        box("ctl", "受控库\n基线 V1.3", 0.4f, 0.4f, w = 0.24f, h = 0.2f),
                        box("prod", "产品库\nV1.3 存档", 0.74f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f, caption = "完全配置管理之下"),
                    ),
                    edges = listOf(edge("ctl", "prod", active = true, label = "发布")),
                ),
            ),
        ),

        // ============ SI 2：变更控制流程 ============
        VisualLesson(
            id = "si-change-flow-ccb",
            title = "整体变更控制：从变更申请到通知实施",
            examId = VisualRepository.SI,
            chapter = "项目整体管理",
            keywords = listOf("变更控制", "变更流程", "CCB", "变更申请", "整体变更控制"),
            takeaway = "变更六步：提出申请→影响评估→CCB 审批→更新基准→实施并验证→通知干系人。任何绕过 CCB 的「先改再说」都是案例找茬点。",
            frames = listOf(
                VisualFrame(
                    "① 谁都能提变更：提交书面变更申请（记录在案）",
                    nodes = listOf(
                        box("req", "变更申请", 0.06f, 0.42f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f, caption = "书面提出，写明内容与理由"),
                        text("who", "客户/团队/PM 均可提出", 0.3f, 0.16f, w = 0.5f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 先评估影响：范围/进度/成本/质量/风险都要过一遍",
                    nodes = listOf(
                        box("req", "变更申请", 0.06f, 0.42f, w = 0.24f, h = 0.16f),
                        box("eval", "影响评估", 0.38f, 0.42f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f, caption = "三基准+质量+风险"),
                        text("note", "不评估就批 = 流程违规", 0.36f, 0.16f, w = 0.5f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(edge("req", "eval")),
                ),
                VisualFrame(
                    "③ CCB 决策：批准 / 驳回 / 挂起，决策要记录",
                    nodes = listOf(
                        box("eval", "影响评估", 0.06f, 0.42f, w = 0.24f, h = 0.16f),
                        circle("ccb", "CCB", 0.42f, 0.42f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f, caption = "变更控制委员会"),
                        box("ok", "批准", 0.72f, 0.16f, VisualTone.GOOD, w = 0.2f, h = 0.12f),
                        box("no", "驳回", 0.72f, 0.46f, VisualTone.BAD, w = 0.2f, h = 0.12f),
                        box("hold", "挂起", 0.72f, 0.74f, VisualTone.MUTED, w = 0.2f, h = 0.12f),
                    ),
                    edges = listOf(edge("eval", "ccb"), edge("ccb", "ok", active = true), edge("ccb", "no"), edge("ccb", "hold")),
                ),
                VisualFrame(
                    "④ 批准后：先更新基准与计划，再实施变更",
                    nodes = listOf(
                        circle("ccb", "CCB 批准", 0.06f, 0.42f, VisualTone.GOOD, w = 0.22f, h = 0.22f),
                        box("base", "更新基准\n与计划", 0.36f, 0.42f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f, caption = "基准只有批准后才能改"),
                        box("impl", "实施变更", 0.7f, 0.42f, w = 0.22f, h = 0.16f),
                    ),
                    edges = listOf(edge("ccb", "base"), edge("base", "impl")),
                ),
                VisualFrame(
                    "⑤ 实施后验证效果，并通知所有相关干系人",
                    nodes = listOf(
                        box("impl", "实施完成", 0.06f, 0.42f, w = 0.24f, h = 0.16f),
                        box("ver", "验证效果", 0.38f, 0.42f, VisualTone.GOOD, w = 0.22f, h = 0.16f),
                        box("noti", "通知干系人", 0.72f, 0.42f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f),
                    ),
                    edges = listOf(edge("impl", "ver"), edge("ver", "noti")),
                ),
            ),
        ),

        // ============ SI 3：挣值动态 ============
        VisualLesson(
            id = "si-evm-dynamic",
            title = "挣值三线动态：PV / EV / AC 随时间展开",
            examId = VisualRepository.SI,
            chapter = "项目成本管理",
            keywords = listOf("挣值", "PV", "EV", "AC", "CV", "SV", "CPI", "SPI", "绩效"),
            takeaway = "检查点上看三条线的相对位置：EV 在 PV 下方=进度落后（SV<0），EV 在 AC 下方=成本超支（CV<0）。四个指标分子都是 EV。",
            frames = listOf(
                VisualFrame(
                    "① 计划价值 PV：按进度基准，「计划到今天应完成多少预算工作」",
                    nodes = listOf(
                        bar("pv", "PV\n64万", 0.3f, 0.3f, 0.8f, VisualTone.NEUTRAL, w = 0.14f, h = 0.5f),
                        text("t1", "计划应完成的工作价值", 0.5f, 0.1f, w = 0.5f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 挣值 EV：实际完成的工作按预算折算（不管花了多少钱）",
                    nodes = listOf(
                        bar("pv", "PV\n64万", 0.18f, 0.3f, 0.8f, VisualTone.MUTED, w = 0.14f, h = 0.5f),
                        bar("ev", "EV\n56万", 0.44f, 0.3f, 0.7f, VisualTone.ACTIVE, w = 0.14f, h = 0.5f),
                        text("t2", "SV = EV − PV = −8万 < 0：进度落后", 0.3f, 0.1f, w = 0.6f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 实际成本 AC：实际花掉的钱（EV 与 AC 都看「实际」，口径不同）",
                    nodes = listOf(
                        bar("pv", "PV", 0.12f, 0.3f, 0.8f, VisualTone.MUTED, w = 0.12f, h = 0.5f),
                        bar("ev", "EV", 0.34f, 0.3f, 0.7f, VisualTone.ACTIVE, w = 0.12f, h = 0.5f),
                        bar("ac", "AC\n60万", 0.56f, 0.3f, 0.75f, VisualTone.BAD, w = 0.12f, h = 0.5f),
                        text("t3", "CV = EV − AC = −4万 < 0：成本超支", 0.28f, 0.1f, w = 0.6f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 指数化：SPI=EV/PV=0.875，CPI=EV/AC≈0.93，都小于 1",
                    nodes = listOf(
                        text("s1", "SPI 0.875", 0.12f, 0.42f, w = 0.22f, tone = VisualTone.BAD),
                        text("s2", "CPI 0.93", 0.4f, 0.42f, w = 0.22f, tone = VisualTone.BAD),
                        text("s3", "EAC = BAC/CPI\n= 80/0.93 ≈ 85.71万", 0.66f, 0.38f, w = 0.32f, tone = VisualTone.PATH),
                        text("t4", "双落后 → 赶工/快速跟进 + 分析根因纠偏", 0.2f, 0.14f, w = 0.7f),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 4：质量成本构成 ============
        VisualLesson(
            id = "si-quality-cost",
            title = "质量成本构成：一致性投入与非一致性损失",
            examId = VisualRepository.SI,
            chapter = "项目质量管理",
            keywords = listOf("质量成本", "预防成本", "评估成本", "失败成本", "一致性成本", "质量"),
            takeaway = "预防+评估=一致性成本（主动投入）；内部+外部失败=非一致性成本（被动损失）。加大预防投入会显著压低失败成本。",
            frames = listOf(
                VisualFrame(
                    "① 一致性成本：为「把事情一次做对」而主动投入",
                    nodes = listOf(
                        box("prev", "预防成本\n培训/手册", 0.08f, 0.4f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        box("appr", "评估成本\n评审/测试", 0.4f, 0.4f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        text("sum1", "预防 + 评估 = 一致性成本", 0.24f, 0.14f, w = 0.56f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 非一致性成本：缺陷已经发生后付出的代价",
                    nodes = listOf(
                        box("inf", "内部失败\n返工/重测\n交付前发现", 0.08f, 0.4f, VisualTone.BAD, w = 0.26f, h = 0.24f),
                        box("ext", "外部失败\n售后/赔偿\n交付后发现", 0.4f, 0.4f, VisualTone.BAD, w = 0.26f, h = 0.24f),
                        text("sum2", "内部 + 外部失败 = 非一致性成本", 0.24f, 0.12f, w = 0.56f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 跷跷板规律：预防投入↑ → 失败成本↓↓，总质量成本反而降",
                    nodes = listOf(
                        bar("p2", "预防投入", 0.14f, 0.3f, 0.75f, VisualTone.GOOD, w = 0.14f, h = 0.5f),
                        bar("f2", "失败成本", 0.42f, 0.3f, 0.2f, VisualTone.BAD, w = 0.14f, h = 0.5f),
                        text("law", "1 元预防 ≈ 省下数元失败损失", 0.62f, 0.4f, w = 0.36f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 案例计算套路：先归类（六笔费用），再按类求和",
                    nodes = listOf(
                        box("q1", "培训 2.0\n手册 1.5", 0.06f, 0.42f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("q2", "评审 2.5\n测试 3.0", 0.36f, 0.42f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("q3", "返工 2.0\n售后 1.5", 0.66f, 0.42f, VisualTone.BAD, w = 0.24f, h = 0.2f),
                        text("ans", "预防3.5 + 评估5.5 + 失败3.5 = 12.5万", 0.14f, 0.12f, w = 0.7f, tone = VisualTone.ACTIVE),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 5：权力/利益方格 ============
        VisualLesson(
            id = "si-stakeholder-quad",
            title = "权力/利益方格：干系人落位与应对策略",
            examId = VisualRepository.SI,
            chapter = "项目干系人管理",
            keywords = listOf("权力", "利益", "方格", "干系人", "重点管理", "令其满意", "随时告知"),
            takeaway = "高权高利重点管理、高权低利令其满意、低权高利随时告知、低权低利监督。干系人可能移动，策略要动态调整。",
            frames = listOf(
                VisualFrame(
                    "① 画格子：横轴利益（受项目影响程度），纵轴权力（影响项目的能力）",
                    nodes = listOf(
                        text("axis", "↑权力　→利益", 0.04f, 0.08f, w = 0.3f),
                        box("q1", "① 重点管理", 0.56f, 0.12f, VisualTone.ACTIVE, w = 0.3f, h = 0.14f),
                        box("q2", "② 令其满意", 0.2f, 0.12f, VisualTone.GOOD, w = 0.3f, h = 0.14f),
                        box("q3", "③ 随时告知", 0.56f, 0.62f, VisualTone.PATH, w = 0.3f, h = 0.14f),
                        box("q4", "④ 监督", 0.2f, 0.62f, VisualTone.MUTED, w = 0.3f, h = 0.14f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 高权高利（①重点管理）：发起人、关键客户——密切沟通、及时汇报",
                    nodes = listOf(
                        circle("sponsor", "发起人", 0.6f, 0.16f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f),
                        text("act1", "例会汇报 + 里程碑确认", 0.52f, 0.42f, w = 0.44f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 高权低利（②令其满意）：财务/法务等职能部门——不打扰但别得罪",
                    nodes = listOf(
                        circle("fin", "财务总监", 0.24f, 0.16f, VisualTone.GOOD, w = 0.2f, h = 0.2f),
                        text("act2", "关键节点通报即可", 0.16f, 0.42f, w = 0.4f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 低权高利（③随时告知）：终端用户、支持团队——保持知情防传言",
                    nodes = listOf(
                        circle("user", "最终用户", 0.6f, 0.66f, VisualTone.PATH, w = 0.2f, h = 0.2f),
                        text("act3", "定期进度通报 / 培训预告", 0.52f, 0.9f, w = 0.44f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "⑤ 落位是动态的：权力与利益变化要重新评估并调整策略",
                    nodes = listOf(
                        circle("u2", "用户代表\n升为关键角色", 0.58f, 0.3f, VisualTone.ACTIVE, w = 0.26f, h = 0.22f, caption = "参与度提升 → 移入①区"),
                        text("warn", "干系人登记册要随项目滚动更新", 0.2f, 0.7f, w = 0.5f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 1：色彩三属性 ============
        VisualLesson(
            id = "md-color-three-props",
            title = "色彩三属性：加白 / 加黑 / 加灰的影响",
            examId = VisualRepository.MD,
            chapter = "色彩与色彩管理",
            keywords = listOf("色相", "饱和度", "明度", "色彩三属性", "加白", "加黑"),
            takeaway = "加白：饱和度↓明度↑；加黑：饱和度↓明度↓；加灰：只降饱和度。色相三者都动不了——色相只由波长类别决定。",
            frames = listOf(
                VisualFrame(
                    "① 三属性各自管什么：色相=相貌，饱和度=纯度，明度=明暗",
                    nodes = listOf(
                        box("h", "色相\n红/橙/黄…", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.26f, h = 0.2f),
                        box("s", "饱和度\n鲜艳↔灰暗", 0.38f, 0.4f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        box("b", "明度\n明亮↔深暗", 0.7f, 0.4f, VisualTone.PATH, w = 0.26f, h = 0.2f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 加白：红色变粉红——饱和度降低、明度升高、色相不变",
                    nodes = listOf(
                        box("r0", "纯红", 0.08f, 0.42f, VisualTone.BAD, w = 0.24f, h = 0.16f),
                        box("r1", "粉红\nS↓ B↑", 0.42f, 0.42f, VisualTone.BAD, w = 0.24f, h = 0.16f, caption = "还是红色系（色相不变）"),
                        text("w", "+ 白", 0.74f, 0.42f, w = 0.2f),
                    ),
                    edges = listOf(edge("r0", "r1", active = true)),
                ),
                VisualFrame(
                    "③ 加黑：红色变暗红——饱和度降低、明度降低、色相不变",
                    nodes = listOf(
                        box("r0", "纯红", 0.08f, 0.42f, VisualTone.BAD, w = 0.24f, h = 0.16f),
                        box("r2", "暗红\nS↓ B↓", 0.42f, 0.42f, VisualTone.BAD, w = 0.24f, h = 0.16f),
                        text("k", "+ 黑", 0.74f, 0.42f, w = 0.2f),
                    ),
                    edges = listOf(edge("r0", "r2", active = true)),
                ),
                VisualFrame(
                    "④ 判断题套路：加白/加黑都「降饱和度」——只有明度升降方向不同",
                    nodes = listOf(
                        text("a1", "加白：S↓ 明度↑", 0.08f, 0.3f, w = 0.4f, tone = VisualTone.GOOD),
                        text("a2", "加黑：S↓ 明度↓", 0.52f, 0.3f, w = 0.4f, tone = VisualTone.BAD),
                        text("a3", "共同点：饱和度都下降；色相都不变", 0.12f, 0.56f, w = 0.72f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 2：GOP 帧结构 ============
        VisualLesson(
            id = "md-gop-frames",
            title = "GOP 帧结构：I / P / B 帧的参考关系",
            examId = VisualRepository.MD,
            chapter = "视频参数与计算",
            keywords = listOf("GOP", "I帧", "P帧", "B帧", "关键帧", "双向预测", "视频编码"),
            takeaway = "I 帧全量编码是随机访问点；P 帧参考前一帧；B 帧双向参考压缩率最高但延迟大。GOP 越短拖动越快、体积越大。",
            frames = listOf(
                VisualFrame(
                    "① GOP = 一组帧：开头必须是 I 帧（全量，解码起点）",
                    nodes = listOf(
                        box("i1", "I", 0.06f, 0.42f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f, caption = "帧内编码 全量"),
                        box("e1", "…", 0.24f, 0.42f, VisualTone.MUTED, w = 0.1f, h = 0.16f),
                        text("cap1", "I 帧不依赖任何其他帧", 0.4f, 0.42f, w = 0.5f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② P 帧：参考前面已解码的帧，只存差异（前向预测）",
                    nodes = listOf(
                        box("i2", "I", 0.08f, 0.42f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f),
                        box("p1", "P", 0.34f, 0.42f, VisualTone.GOOD, w = 0.14f, h = 0.16f, caption = "体积约为 I 的 1/3~1/2"),
                        text("ref1", "P ← 参考 I", 0.56f, 0.42f, w = 0.34f),
                    ),
                    edges = listOf(edge("i2", "p1", active = true, label = "预测")),
                ),
                VisualFrame(
                    "③ B 帧：前后都参考（双向预测），压缩率最高、延迟最大",
                    nodes = listOf(
                        box("i3", "I", 0.06f, 0.42f, VisualTone.ACTIVE, w = 0.14f, h = 0.16f),
                        box("b1", "B", 0.32f, 0.42f, VisualTone.PATH, w = 0.14f, h = 0.16f),
                        box("p2", "P", 0.58f, 0.42f, VisualTone.GOOD, w = 0.14f, h = 0.16f),
                        text("ref2", "B 同时参考 I 和 P", 0.32f, 0.16f, w = 0.5f),
                    ),
                    edges = listOf(edge("i3", "b1", active = true, label = "前向"), edge("p2", "b1", active = true, label = "后向")),
                ),
                VisualFrame(
                    "④ GOP 长短的取舍：短 GOP=拖动快/体积大，长 GOP=压缩率高/容错差",
                    nodes = listOf(
                        box("g1", "短 GOP\n拖动快", 0.1f, 0.36f, VisualTone.GOOD, w = 0.26f, h = 0.2f),
                        box("g2", "长 GOP\n更省流量", 0.56f, 0.36f, VisualTone.ACTIVE, w = 0.26f, h = 0.2f),
                        text("rule", "直播点播都要「秒开」→ 密集 I 帧", 0.16f, 0.68f, w = 0.7f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 3：采样与量化 ============
        VisualLesson(
            id = "md-sampling-quant",
            title = "音频数字化：采样 → 量化 → 编码",
            examId = VisualRepository.MD,
            chapter = "数字音频",
            keywords = listOf("采样", "量化", "采样频率", "量化位数", "奈奎斯特", "数字化"),
            takeaway = "采样率管「纵向切多密」（频率范围，≥2×最高频率），量化位数管「横向切多细」（动态范围）。CD=44.1kHz/16bit/双声道。",
            frames = listOf(
                VisualFrame(
                    "① 连续声波：时间与幅度都连续的模拟信号",
                    nodes = listOf(
                        box("wave", "模拟波形\n连续", 0.34f, 0.4f, VisualTone.ACTIVE, w = 0.32f, h = 0.2f),
                        text("mic", "麦克风拾音 →", 0.02f, 0.42f, w = 0.3f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 采样：按固定时间间隔取值（纵向切）——44.1kHz = 每秒 44100 次",
                    nodes = listOf(
                        box("wave", "波形", 0.06f, 0.4f, VisualTone.MUTED, w = 0.18f, h = 0.2f),
                        box("sam", "采样点\n每 1/44100 秒一个", 0.34f, 0.4f, VisualTone.ACTIVE, w = 0.3f, h = 0.2f),
                        text("ny", "奈奎斯特：采样率 ≥ 2×最高频率", 0.28f, 0.14f, w = 0.6f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(edge("wave", "sam")),
                ),
                VisualFrame(
                    "③ 量化：把幅度近似到有限档位（横向切）——16bit = 65536 级",
                    nodes = listOf(
                        box("sam", "采样值", 0.06f, 0.4f, VisualTone.MUTED, w = 0.18f, h = 0.2f),
                        box("qnt", "量化\n16bit=65536 级", 0.34f, 0.4f, VisualTone.GOOD, w = 0.3f, h = 0.2f),
                        text("err", "档位少 → 量化噪声大", 0.7f, 0.4f, w = 0.3f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(edge("sam", "qnt")),
                ),
                VisualFrame(
                    "④ 编码输出：二进制串 → 未压缩 PCM；容量 = 采样率×位深÷8×声道×时长",
                    nodes = listOf(
                        box("qnt", "量化值", 0.06f, 0.4f, VisualTone.MUTED, w = 0.18f, h = 0.2f),
                        box("bin", "01 串\nPCM 数据", 0.34f, 0.4f, VisualTone.ACTIVE, w = 0.28f, h = 0.2f),
                        text("cd", "CD 一分钟 ≈ 10MB", 0.68f, 0.4f, w = 0.3f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(edge("qnt", "bin")),
                ),
            ),
        ),

        // ============ MD 4：有损 vs 无损 ============
        VisualLesson(
            id = "md-lossy-vs-lossless",
            title = "有损压缩 vs 无损压缩：能不能还原的取舍",
            examId = VisualRepository.MD,
            chapter = "冗余与压缩比",
            keywords = listOf("有损", "无损", "压缩", "JPEG", "PNG", "冗余"),
            takeaway = "有损丢信息不可逆（JPEG，照片适用）；无损完全可逆（PNG/ZIP，文字线稿适用）。判定标准：内容能否容忍失真。",
            frames = listOf(
                VisualFrame(
                    "① 出发点：多媒体数据有冗余——空间/时间/统计/视觉四类",
                    nodes = listOf(
                        box("sp", "空间冗余\n帧内相似", 0.06f, 0.34f, w = 0.26f, h = 0.16f),
                        box("tm", "时间冗余\n帧间相似", 0.38f, 0.34f, w = 0.26f, h = 0.16f),
                        box("st", "统计冗余\n概率不均", 0.7f, 0.34f, w = 0.26f, h = 0.16f),
                        text("vis", "视觉冗余：人眼不敏感的信息——有损压缩的下手处", 0.12f, 0.62f, w = 0.8f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 无损压缩：只动统计冗余，解码可 100% 还原（PNG / ZIP / FLAC）",
                    nodes = listOf(
                        box("src", "原始数据", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f),
                        box("zip", "无损压缩\n去统计冗余", 0.38f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.16f),
                        box("bak", "还原=原件", 0.72f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.16f),
                    ),
                    edges = listOf(edge("src", "zip", label = "编码"), edge("zip", "bak", active = true, label = "解码")),
                ),
                VisualFrame(
                    "③ 有损压缩：敢丢视觉冗余，压缩率高但不可逆（JPEG / MP3 / H.264）",
                    nodes = listOf(
                        box("src", "原始数据", 0.06f, 0.4f, VisualTone.ACTIVE, w = 0.24f, h = 0.16f),
                        box("loss", "有损压缩\n丢视觉冗余", 0.38f, 0.4f, VisualTone.BAD, w = 0.28f, h = 0.16f),
                        box("sim", "近似还原\n细节永久丢失", 0.72f, 0.4f, VisualTone.BAD, w = 0.26f, h = 0.16f),
                    ),
                    edges = listOf(edge("src", "loss", label = "编码"), edge("loss", "sim", active = true, label = "解码")),
                ),
                VisualFrame(
                    "④ 选型判定：能不能容忍失真？文字/线稿/医疗→无损；照片/音乐/视频→有损",
                    nodes = listOf(
                        box("yes", "无损：PNG 文字\nGIF 图标 FLAC 音乐", 0.08f, 0.4f, VisualTone.GOOD, w = 0.32f, h = 0.24f),
                        box("no", "有损：JPEG 照片\nMP3 音频 H.264 视频", 0.52f, 0.4f, VisualTone.BAD, w = 0.34f, h = 0.24f),
                        text("rule", "考试判断：涉及「文字/法律/医疗/再编辑」一律选无损", 0.1f, 0.72f, w = 0.8f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),
    )
}
