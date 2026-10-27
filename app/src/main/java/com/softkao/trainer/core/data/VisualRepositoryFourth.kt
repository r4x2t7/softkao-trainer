package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 抽象图解（第四批）：决策树与 EMV、敏感性分析、固定/变动成本、
 * CDN 边缘分发、DRM 数字版权管理、多媒体应用系统开发流程。
 */
object VisualRepositoryFourth {

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
            id = "si-decision-tree",
            title = "决策树与期望货币值 EMV",
            examId = VisualRepository.SI,
            chapter = "风险分析与评价",
            keywords = listOf("决策树", "EMV", "期望货币价值", "期望值", "机会节点", "风险量化"),
            takeaway = "EMV = Σ（概率 × 影响金额）；每个方案先算期望再比较，选期望值最大的分支。期望值是长期平均，不代表单次结果。",
            frames = listOf(
                VisualFrame(
                    "① 画树：方块是决策点，圆是机会点，右侧列出各结果",
                    nodes = listOf(
                        box("d", "决策", 0.03f, 0.4f, VisualTone.ACTIVE, w = 0.16f, h = 0.2f),
                        circle("ca", "方案A", 0.34f, 0.08f, w = 0.18f, h = 0.18f),
                        circle("cb", "方案B", 0.34f, 0.62f, w = 0.18f, h = 0.18f),
                        text("ra1", "销路好", 0.66f, 0.04f, w = 0.2f),
                        text("ra2", "销路差", 0.66f, 0.26f, w = 0.2f),
                        text("rb1", "销路好", 0.66f, 0.56f, w = 0.2f),
                        text("rb2", "销路差", 0.66f, 0.78f, w = 0.2f),
                    ),
                    edges = listOf(
                        edge("d", "ca"), edge("d", "cb"),
                        edge("ca", "ra1"), edge("ca", "ra2"),
                        edge("cb", "rb1"), edge("cb", "rb2"),
                    ),
                ),
                VisualFrame(
                    "② 标上概率与损益：销路好 0.7，销路差 0.3",
                    nodes = listOf(
                        box("d", "决策", 0.03f, 0.4f, VisualTone.ACTIVE, w = 0.16f, h = 0.2f),
                        circle("ca", "方案A", 0.34f, 0.08f, w = 0.18f, h = 0.18f),
                        circle("cb", "方案B", 0.34f, 0.62f, w = 0.18f, h = 0.18f),
                        text("ra1", "销路好", 0.66f, 0.04f, "0.7 → 赚 100 万", 0.2f),
                        text("ra2", "销路差", 0.66f, 0.26f, "0.3 → 亏 20 万", 0.2f),
                        text("rb1", "销路好", 0.66f, 0.56f, "0.7 → 赚 50 万", 0.2f),
                        text("rb2", "销路差", 0.66f, 0.78f, "0.3 → 赚 10 万", 0.2f),
                    ),
                    edges = listOf(
                        edge("d", "ca"), edge("d", "cb"),
                        edge("ca", "ra1"), edge("ca", "ra2"),
                        edge("cb", "rb1"), edge("cb", "rb2"),
                    ),
                ),
                VisualFrame(
                    "③ 每个机会点求期望：EMV(A)=100×0.7−20×0.3=64；EMV(B)=50×0.7+10×0.3=38",
                    nodes = listOf(
                        box("d", "决策", 0.03f, 0.4f, VisualTone.ACTIVE, w = 0.16f, h = 0.2f),
                        circle("ca", "方案A", 0.34f, 0.08f, VisualTone.ACTIVE, w = 0.18f, h = 0.18f, caption = "EMV = 64 万"),
                        circle("cb", "方案B", 0.34f, 0.62f, VisualTone.ACTIVE, w = 0.18f, h = 0.18f, caption = "EMV = 38 万"),
                        text("ra1", "销路好", 0.66f, 0.04f, "0.7 → 赚 100 万", 0.2f),
                        text("ra2", "销路差", 0.66f, 0.26f, "0.3 → 亏 20 万", 0.2f),
                        text("rb1", "销路好", 0.66f, 0.56f, "0.7 → 赚 50 万", 0.2f),
                        text("rb2", "销路差", 0.66f, 0.78f, "0.3 → 赚 10 万", 0.2f),
                    ),
                    edges = listOf(
                        edge("d", "ca"), edge("d", "cb"),
                        edge("ca", "ra1"), edge("ca", "ra2"),
                        edge("cb", "rb1"), edge("cb", "rb2"),
                    ),
                ),
                VisualFrame(
                    "④ 比较：64 > 38，选方案A；把期望值小的分支剪掉",
                    nodes = listOf(
                        box("d", "决策", 0.03f, 0.4f, VisualTone.GOOD, w = 0.16f, h = 0.2f),
                        circle("ca", "方案A", 0.34f, 0.08f, VisualTone.GOOD, w = 0.18f, h = 0.18f, caption = "EMV = 64 万"),
                        circle("cb", "方案B", 0.34f, 0.62f, VisualTone.MUTED, w = 0.18f, h = 0.18f, caption = "EMV = 38 万"),
                        text("ra1", "销路好", 0.66f, 0.04f, "0.7 → 赚 100 万", 0.2f, 0.1f, VisualTone.GOOD),
                        text("ra2", "销路差", 0.66f, 0.26f, "0.3 → 亏 20 万", 0.2f, 0.1f, VisualTone.BAD),
                        text("rb1", "销路好", 0.66f, 0.56f, "0.7 → 赚 50 万", 0.2f, 0.1f, VisualTone.MUTED),
                        text("rb2", "销路差", 0.66f, 0.78f, "0.3 → 赚 10 万", 0.2f, 0.1f, VisualTone.MUTED),
                        text("pick", "64 > 38，选 A", 0.02f, 0.88f, "", 0.3f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(
                        edge("d", "ca", active = true), edge("d", "cb", dashed = true),
                        edge("ca", "ra1", active = true), edge("ca", "ra2", active = true),
                        edge("cb", "rb1", dashed = true), edge("cb", "rb2", dashed = true),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-sensitivity",
            title = "敏感性分析：谁最值得盯",
            examId = VisualRepository.SI,
            chapter = "风险分析与评价",
            keywords = listOf("敏感性分析", "不确定因素", "单因素", "敏感度", "风险量化"),
            takeaway = "每次只动一个不确定因素，看结果跟着变多少；变得最狠的就是要重点管控的因素。输出是敏感度排序，不是具体损失金额。",
            frames = listOf(
                VisualFrame(
                    "① 基准情景：所有因素都不动，利润 100 万",
                    nodes = listOf(
                        box("f1", "人力成本", 0.05f, 0.1f, VisualTone.NEUTRAL, w = 0.26f, caption = "不动"),
                        box("f2", "工期", 0.05f, 0.42f, VisualTone.NEUTRAL, w = 0.26f, caption = "不动"),
                        box("f3", "需求", 0.05f, 0.74f, VisualTone.NEUTRAL, w = 0.26f, caption = "不动"),
                        text("base", "利润 100 万", 0.5f, 0.42f, "基准", 0.4f, 0.1f, VisualTone.GOOD),
                    ),
                ),
                VisualFrame(
                    "② 只让人力成本 +10%：利润掉到 70 万，动了 30 万",
                    nodes = listOf(
                        box("f1", "人力成本", 0.05f, 0.1f, VisualTone.ACTIVE, w = 0.26f, caption = "只动它：+10%"),
                        box("f2", "工期", 0.05f, 0.42f, VisualTone.MUTED, w = 0.26f, caption = "不动"),
                        box("f3", "需求", 0.05f, 0.74f, VisualTone.MUTED, w = 0.26f, caption = "不动"),
                        text("base", "利润 70 万", 0.5f, 0.42f, "−30 万", 0.4f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "③ 换成只让工期 +10%：利润 90 万，只动了 10 万",
                    nodes = listOf(
                        box("f1", "人力成本", 0.05f, 0.1f, VisualTone.MUTED, w = 0.26f, caption = "不动"),
                        box("f2", "工期", 0.05f, 0.42f, VisualTone.ACTIVE, w = 0.26f, caption = "只动它：+10%"),
                        box("f3", "需求", 0.05f, 0.74f, VisualTone.MUTED, w = 0.26f, caption = "不动"),
                        text("base", "利润 90 万", 0.5f, 0.42f, "−10 万", 0.4f, 0.1f, VisualTone.BAD),
                    ),
                ),
                VisualFrame(
                    "④ 排序：人力成本最敏感 → 集中资源管控它",
                    nodes = listOf(
                        text("t", "同样 +10%，对利润的冲击", 0.2f, 0.04f, "", 0.6f, 0.1f, VisualTone.ACTIVE),
                        bar("b1", "人力成本", 0.1f, 0.4f, 0.85f, VisualTone.BAD, w = 0.16f, caption = "−30 万"),
                        bar("b2", "工期", 0.4f, 0.4f, 0.35f, VisualTone.ACTIVE, w = 0.16f, caption = "−10 万"),
                        bar("b3", "需求", 0.7f, 0.4f, 0.15f, VisualTone.MUTED, w = 0.16f, caption = "−3 万"),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "si-cost-type",
            title = "固定成本与变动成本",
            examId = VisualRepository.SI,
            chapter = "项目成本管理",
            keywords = listOf("固定成本", "变动成本", "间接成本", "直接成本", "成本动因", "最佳工期"),
            takeaway = "固定成本的动因是时间（租金/折旧/管理费），工期越短总额越少；变动成本跟着工作量走（人工/材料/加班）。判据是「总额是否随工作量变」，不是「单价是否固定」。",
            frames = listOf(
                VisualFrame(
                    "① 固定成本：按期发生，动因是时间，工期越短总额越少",
                    nodes = listOf(
                        box("f", "固定成本", 0.05f, 0.12f, VisualTone.GOOD, w = 0.26f, caption = "动因 = 时间"),
                        box("e1", "场地租金", 0.45f, 0.08f, w = 0.24f),
                        box("e2", "设备折旧", 0.45f, 0.36f, w = 0.24f),
                        box("e3", "管理费", 0.45f, 0.64f, w = 0.24f),
                        text("t", "工期越短，总额越少", 0.25f, 0.86f, "", 0.5f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("f", "e1"), edge("f", "e2"), edge("f", "e3")),
                ),
                VisualFrame(
                    "② 变动成本：跟着工作量走，动因是活动量",
                    nodes = listOf(
                        box("v", "变动成本", 0.05f, 0.12f, VisualTone.ACTIVE, w = 0.26f, caption = "动因 = 活动量"),
                        box("e1", "人工工时", 0.45f, 0.08f, w = 0.24f),
                        box("e2", "材料费", 0.45f, 0.36f, w = 0.24f),
                        box("e3", "加班补贴", 0.45f, 0.64f, w = 0.24f),
                        text("t", "工作量越大，总额越大", 0.25f, 0.86f, "", 0.5f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("v", "e1"), edge("v", "e2"), edge("v", "e3")),
                ),
                VisualFrame(
                    "③ 压缩工期 = 拿变动换固定：赶工费涨、租金折旧省",
                    nodes = listOf(
                        box("f", "固定成本 ↓", 0.06f, 0.3f, VisualTone.GOOD, w = 0.28f),
                        box("v", "变动成本 ↑", 0.06f, 0.64f, VisualTone.BAD, w = 0.28f),
                        text("sum", "总成本 = 固定 + 变动", 0.42f, 0.16f, "", 0.5f, 0.1f, VisualTone.ACTIVE),
                        text("note", "赶工：加班/加人 → 变动成本涨", 0.42f, 0.42f, "", 0.52f, 0.1f, VisualTone.MUTED),
                        text("best", "总成本曲线最低点 = 最佳工期", 0.42f, 0.7f, "", 0.52f, 0.1f, VisualTone.GOOD),
                    ),
                ),
                VisualFrame(
                    "④ 同一件事，两种工期下的成本对比",
                    nodes = listOf(
                        text("t", "压缩工期：固定省一点，变动涨一截", 0.15f, 0.04f, "", 0.7f, 0.1f, VisualTone.ACTIVE),
                        bar("f1", "固定", 0.1f, 0.4f, 0.65f, VisualTone.GOOD, w = 0.12f, caption = "长工期"),
                        bar("v1", "变动", 0.3f, 0.4f, 0.45f, VisualTone.ACTIVE, w = 0.12f, caption = "长工期"),
                        bar("f2", "固定", 0.6f, 0.4f, 0.35f, VisualTone.GOOD, w = 0.12f, caption = "短工期"),
                        bar("v2", "变动", 0.8f, 0.4f, 0.8f, VisualTone.BAD, w = 0.12f, caption = "短工期"),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-cdn",
            title = "CDN 边缘分发与回源",
            examId = VisualRepository.MD,
            chapter = "流媒体技术",
            keywords = listOf("CDN", "边缘节点", "回源", "缓存命中", "RTMP", "防盗链", "就近分发"),
            takeaway = "CDN 把内容缓存到离用户近的边缘节点：命中就在边缘取，未命中才回源（只有第一次）；源站压力小、用户延迟低。RTMP 常用于把直播流推给源站/CDN。",
            frames = listOf(
                VisualFrame(
                    "① 推流：主播用 RTMP 把直播流持续推到源站",
                    nodes = listOf(
                        box("a", "主播", 0.04f, 0.42f, VisualTone.NEUTRAL, w = 0.18f),
                        box("src", "源站", 0.4f, 0.42f, VisualTone.ACTIVE, w = 0.2f),
                        text("p", "RTMP 推流", 0.18f, 0.62f, "基于 TCP 持续传输", 0.3f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("a", "src", active = true)),
                ),
                VisualFrame(
                    "② 分发：源站把内容复制到各地的边缘节点",
                    nodes = listOf(
                        box("e1", "边缘·华东", 0.08f, 0.08f, VisualTone.GOOD, w = 0.24f),
                        box("e2", "边缘·华南", 0.08f, 0.42f, VisualTone.GOOD, w = 0.24f),
                        box("e3", "边缘·西南", 0.08f, 0.76f, VisualTone.GOOD, w = 0.24f),
                        box("src", "源站", 0.55f, 0.42f, VisualTone.ACTIVE, w = 0.2f),
                        text("t", "内容离用户越近，体验越好", 0.35f, 0.9f, "", 0.5f, 0.1f, VisualTone.ACTIVE),
                    ),
                    edges = listOf(edge("src", "e1", active = true), edge("src", "e2", active = true), edge("src", "e3", active = true)),
                ),
                VisualFrame(
                    "③ 观众就近拉流：边缘缓存命中，不用麻烦源站",
                    nodes = listOf(
                        circle("u", "观众", 0.06f, 0.4f, w = 0.17f, h = 0.17f),
                        box("e", "边缘节点", 0.38f, 0.4f, VisualTone.GOOD, w = 0.24f, caption = "缓存命中"),
                        box("src", "源站", 0.76f, 0.4f, VisualTone.MUTED, w = 0.2f, caption = "无压力"),
                        text("t", "就近取内容，延迟低", 0.28f, 0.78f, "", 0.45f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("u", "e", active = true), edge("e", "src", dashed = true)),
                ),
                VisualFrame(
                    "④ 未命中才回源；顺带用 Referer/Token 防盗链",
                    nodes = listOf(
                        circle("u", "观众", 0.06f, 0.4f, w = 0.17f, h = 0.17f),
                        box("e", "边缘节点", 0.38f, 0.4f, VisualTone.ACTIVE, w = 0.24f, caption = "未命中"),
                        box("src", "源站", 0.76f, 0.4f, VisualTone.ACTIVE, w = 0.2f, caption = "只回一次"),
                        text("t1", "回源取一次，之后就近服务", 0.22f, 0.72f, "", 0.5f, 0.1f, VisualTone.GOOD),
                        text("t2", "防盗链：Referer / Token 校验", 0.22f, 0.88f, "", 0.5f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("u", "e", active = true), edge("e", "src", active = true)),
                ),
            ),
        ),

        VisualLesson(
            id = "md-drm",
            title = "DRM 数字版权管理",
            examId = VisualRepository.MD,
            chapter = "标准与版权",
            keywords = listOf("DRM", "数字版权管理", "许可证", "设备绑定", "数字水印", "版权保护", "防盗链"),
            takeaway = "DRM = 加密打包 + 许可证签发 + 设备绑定 + 水印溯源。它提高侵权成本，但挡不住合法解密后的录屏盗版——必须与法律手段配合。",
            frames = listOf(
                VisualFrame(
                    "① 加密打包：媒体本体对称加密，没钥匙就是乱码",
                    nodes = listOf(
                        box("c", "片源", 0.05f, 0.42f, w = 0.2f),
                        box("p", "加密打包", 0.4f, 0.42f, VisualTone.ACTIVE, w = 0.24f, caption = "对称加密"),
                        box("e", "密文分发", 0.75f, 0.42f, VisualTone.MUTED, w = 0.22f),
                        text("t", "拿到文件、没有钥匙也播不了", 0.25f, 0.78f, "", 0.5f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("c", "p"), edge("p", "e", active = true)),
                ),
                VisualFrame(
                    "② 播放要许可证：按「身份+设备+有效期」校验后签发",
                    nodes = listOf(
                        circle("u", "用户", 0.06f, 0.42f, w = 0.16f, h = 0.16f),
                        box("s", "授权服务器", 0.38f, 0.42f, VisualTone.ACTIVE, w = 0.28f, caption = "身份 · 设备 · 有效期"),
                        text("t", "播放器在线申请播放许可证", 0.2f, 0.76f, "", 0.45f, 0.1f, VisualTone.MUTED),
                    ),
                    edges = listOf(edge("u", "s", active = true)),
                ),
                VisualFrame(
                    "③ 解密播放：密钥按设备下发，换设备要重新授权",
                    nodes = listOf(
                        circle("u", "用户", 0.06f, 0.42f, w = 0.16f, h = 0.16f),
                        box("s", "授权服务器", 0.36f, 0.42f, VisualTone.ACTIVE, w = 0.26f),
                        box("play", "解密播放", 0.74f, 0.42f, VisualTone.GOOD, w = 0.24f, caption = "设备绑定 · 输出限制"),
                        text("t", "许可证 + 密钥走加密通道下发", 0.2f, 0.76f, "", 0.5f, 0.1f, VisualTone.GOOD),
                    ),
                    edges = listOf(edge("u", "s"), edge("s", "play", active = true)),
                ),
                VisualFrame(
                    "④ 溯源与局限：水印能追责，录屏挡不住",
                    nodes = listOf(
                        text("w", "数字水印：泄露可溯源", 0.08f, 0.1f, "", 0.42f, 0.1f, VisualTone.GOOD),
                        text("lim", "合法解密后挡不住录屏", 0.52f, 0.1f, "", 0.42f, 0.1f, VisualTone.BAD),
                        text("c1", "DRM 提高侵权成本", 0.14f, 0.42f, "", 0.36f, 0.1f, VisualTone.ACTIVE),
                        text("c2", "杜绝盗版要靠法律配合", 0.54f, 0.42f, "", 0.4f, 0.1f, VisualTone.ACTIVE),
                        text("all", "手段：加密 / 许可证 / 设备绑定 / 水印 / 防盗链", 0.1f, 0.74f, "", 0.8f, 0.1f, VisualTone.MUTED),
                    ),
                ),
            ),
        ),

        VisualLesson(
            id = "md-mmedia-flow",
            title = "多媒体应用系统开发流程",
            examId = VisualRepository.MD,
            chapter = "多媒体项目实施",
            keywords = listOf("多媒体系统开发流程", "系统设计", "素材制作", "螺旋模型", "原型法", "迭代开发"),
            takeaway = "顺序：需求分析与规划 → 系统设计 → 素材采集与制作 → 系统集成 → 测试 → 发布与交付 → 运行维护。素材制作必须等设计规格明确，否则因格式/分辨率不明而返工。",
            frames = listOf(
                VisualFrame(
                    "① 需求分析与规划：定目标与范围",
                    nodes = listOf(
                        box("s1", "需求分析", 0.02f, 0.12f, VisualTone.ACTIVE, w = 0.22f, caption = "定目标与范围"),
                    ),
                ),
                VisualFrame(
                    "② 系统设计：输出体系结构 / 功能 / 界面规格",
                    nodes = listOf(
                        box("s1", "需求分析", 0.02f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "定目标与范围"),
                        box("s2", "系统设计", 0.27f, 0.12f, VisualTone.ACTIVE, w = 0.22f, caption = "界面与功能规格"),
                    ),
                    edges = listOf(edge("s1", "s2")),
                ),
                VisualFrame(
                    "③ 素材采集与制作：必须等设计规格明确再动手",
                    nodes = listOf(
                        box("s1", "需求分析", 0.02f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "定目标与范围"),
                        box("s2", "系统设计", 0.27f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "界面与功能规格"),
                        box("s3", "素材制作", 0.52f, 0.12f, VisualTone.ACTIVE, w = 0.22f, caption = "按规格采集制作"),
                        text("warn", "规格不清就做素材 → 必返工", 0.06f, 0.4f, "", 0.6f, 0.1f, VisualTone.BAD),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3")),
                ),
                VisualFrame(
                    "④ 集成 → 测试 → 发布交付 → 运行维护，七步走完",
                    nodes = listOf(
                        box("s1", "需求分析", 0.02f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "定目标与范围"),
                        box("s2", "系统设计", 0.27f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "界面与功能规格"),
                        box("s3", "素材制作", 0.52f, 0.12f, VisualTone.GOOD, w = 0.22f, caption = "按规格采集制作"),
                        box("s4", "系统集成", 0.77f, 0.12f, VisualTone.ACTIVE, w = 0.22f, caption = "组装成系统"),
                        box("s5", "测试", 0.27f, 0.55f, VisualTone.ACTIVE, w = 0.2f, caption = "找缺陷"),
                        box("s6", "发布交付", 0.52f, 0.55f, VisualTone.ACTIVE, w = 0.22f, caption = "交付用户"),
                        box("s7", "运行维护", 0.77f, 0.55f, VisualTone.ACTIVE, w = 0.22f, caption = "长期运营"),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3"), edge("s3", "s4"), edge("s4", "s5"), edge("s5", "s6"), edge("s6", "s7")),
                ),
                VisualFrame(
                    "⑤ 需求不明或高风险 → 螺旋模型 / 原型法",
                    nodes = listOf(
                        box("s1", "需求分析", 0.02f, 0.12f, VisualTone.GOOD, w = 0.22f),
                        box("s2", "系统设计", 0.27f, 0.12f, VisualTone.GOOD, w = 0.22f),
                        box("s3", "素材制作", 0.52f, 0.12f, VisualTone.GOOD, w = 0.22f),
                        box("s4", "系统集成", 0.77f, 0.12f, VisualTone.GOOD, w = 0.22f),
                        box("s5", "测试", 0.02f, 0.4f, VisualTone.GOOD, w = 0.2f),
                        box("s6", "发布交付", 0.27f, 0.4f, VisualTone.GOOD, w = 0.22f),
                        box("s7", "运行维护", 0.52f, 0.4f, VisualTone.GOOD, w = 0.22f),
                        box("sp", "螺旋模型", 0.05f, 0.72f, VisualTone.ACTIVE, w = 0.22f, caption = "风险驱动迭代"),
                        box("pt1", "抛弃式原型", 0.33f, 0.72f, VisualTone.NEUTRAL, w = 0.26f, caption = "验证后丢弃"),
                        box("pt2", "演化式原型", 0.66f, 0.72f, VisualTone.NEUTRAL, w = 0.26f, caption = "逐步演化成成品"),
                    ),
                    edges = listOf(edge("s1", "s2"), edge("s2", "s3"), edge("s3", "s4"), edge("s4", "s5", dashed = true), edge("s5", "s6"), edge("s6", "s7")),
                ),
            ),
        ),
    )
}
