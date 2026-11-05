package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.VisualEdge
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone

/**
 * 第八批知识动画：SI 冲突处理五方式 / 风险识别方法 / 索赔流程，
 * MD 流媒体协议对比 / 音视频同步 / 著作权权利体系。
 */
object VisualRepositoryEighth {

    private fun box(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.2f, h: Float = 0.13f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.BOX, 0f, tone, caption)

    private fun circle(id: String, label: String, x: Float, y: Float, tone: VisualTone = VisualTone.NEUTRAL, w: Float = 0.16f, h: Float = 0.16f, caption: String = "") =
        VisualNode(id, label, x, y, w, h, VisualKind.CIRCLE, 0f, tone, caption)

    private fun text(id: String, label: String, x: Float, y: Float, caption: String = "", w: Float = 0.3f, h: Float = 0.1f, tone: VisualTone = VisualTone.NEUTRAL) =
        VisualNode(id, label, x, y, w, h, VisualKind.TEXT, 0f, tone, caption)

    private fun edge(from: String, to: String, active: Boolean = false, dashed: Boolean = false, label: String = "") =
        VisualEdge(from, to, active, dashed, label)

    val lessons: List<VisualLesson> = listOf(
        // ============ SI 1：冲突处理五方式 ============
        VisualLesson(
            id = "si-conflict-five",
            title = "冲突处理五方式：从撤退到合作",
            examId = VisualRepository.SI,
            chapter = "项目组织与团队",
            keywords = listOf("冲突", "撤退", "缓和", "妥协", "强迫", "合作", "解决问题", "冲突处理"),
            takeaway = "合作/解决问题是唯一能彻底解决并增进关系的方式（技术分歧首选）；强迫应急用；撤退只降温不解决；缓和牺牲己方；妥协双方各让一步。",
            frames = listOf(
                VisualFrame(
                    "① 撤退/回避：暂时离开冲突——只降温，不解决",
                    nodes = listOf(
                        box("wd", "撤退/回避\n暂缓、避而不谈", 0.06f, 0.4f, VisualTone.MUTED, w = 0.32f, h = 0.22f, caption = "冲突会留着，火种未灭"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 缓和/包容：强调一致、牺牲己方观点——求同存异",
                    nodes = listOf(
                        box("hh", "缓和/包容\n突出共识淡化分歧", 0.06f, 0.4f, VisualTone.NEUTRAL, w = 0.34f, h = 0.22f, caption = "己方立场让位，关系优先"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 妥协/调解：双方各让一步——没有全赢者",
                    nodes = listOf(
                        box("tj", "妥协/调解\n各退半步换推进", 0.06f, 0.4f, VisualTone.NEUTRAL, w = 0.34f, h = 0.22f, caption = "临时方案，可能都不满意"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 强迫/命令：以权压人——快但伤关系，应急可用",
                    nodes = listOf(
                        box("qp", "强迫/命令\n职级拍板", 0.06f, 0.4f, VisualTone.BAD, w = 0.34f, h = 0.22f, caption = "紧急/原则问题才用，日常是反模式"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "⑤ 合作/解决问题：直面分歧摆数据——唯一的双赢解",
                    nodes = listOf(
                        box("hz", "合作/解决问题\n同堂对数据求最优", 0.06f, 0.4f, VisualTone.GOOD, w = 0.38f, h = 0.24f, caption = "考试默认答案：技术/方案分歧"),
                        text("note", "把冲突当问题来解，而非压下去", 0.5f, 0.44f, w = 0.46f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 2：风险识别方法 ============
        VisualLesson(
            id = "si-risk-identify-methods",
            title = "风险识别方法：七种工具各自怎么用",
            examId = VisualRepository.SI,
            chapter = "项目风险管理",
            keywords = listOf("风险识别", "头脑风暴", "德尔菲", "检查表", "SWOT", "因果图", "访谈", "文档审阅"),
            takeaway = "头脑风暴开放共创、德尔菲匿名多轮防权威压制、检查表靠历史沉淀、SWOT 四象限定位、因果图溯根因、访谈挖专家经验、文档审阅查遗漏。",
            frames = listOf(
                VisualFrame(
                    "① 头脑风暴 vs 德尔菲：一个当面放开说，一个匿名背靠背",
                    nodes = listOf(
                        box("bs", "头脑风暴\n当面共创\n禁止批评", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.3f, h = 0.24f),
                        box("dp", "德尔菲\n多轮匿名\n汇总再征询", 0.5f, 0.38f, VisualTone.GOOD, w = 0.3f, h = 0.24f, caption = "防权威压制与从众"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 检查表 vs 文档审阅：一个靠历史清单，一个靠现有文档",
                    nodes = listOf(
                        box("ck", "检查表\n历史风险清单核对", 0.06f, 0.38f, VisualTone.GOOD, w = 0.3f, h = 0.24f, caption = "快，但覆盖不到新风险"),
                        box("doc", "文档审阅\n章程/计划/假设分析", 0.5f, 0.38f, VisualTone.NEUTRAL, w = 0.34f, h = 0.24f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ SWOT vs 因果图：一个定方位，一个溯根因",
                    nodes = listOf(
                        box("sw", "SWOT\n优势/劣势/机会/威胁", 0.06f, 0.38f, VisualTone.PATH, w = 0.34f, h = 0.24f),
                        box("fish", "因果图（鱼骨图）\n沿脊骨追根因", 0.52f, 0.38f, VisualTone.BAD, w = 0.34f, h = 0.24f, caption = "人机料法环分类"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 组合拳：实际项目常多法并用，识别是迭代过程",
                    nodes = listOf(
                        text("r1", "识别不是一次性：新信息出现要重新识别", 0.06f, 0.18f, w = 0.7f, tone = VisualTone.PATH),
                        text("r2", "产出统一进风险登记册（后续分析应对的基础）", 0.06f, 0.46f, w = 0.72f),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ SI 3：索赔流程 ============
        VisualLesson(
            id = "si-claim-process",
            title = "索赔处理流程：28 天时限与证据链",
            examId = VisualRepository.SI,
            chapter = "项目合同管理",
            keywords = listOf("索赔", "索赔意向", "28天", "索赔报告", "工程师审核", "合同争议"),
            takeaway = "索赔事件发生后 28 天内提出索赔意向通知（逾期失权），随后提交索赔报告与证据；监理/工程师审核协商，不成再按合同争议机制解决。证据链决定成败。",
            frames = listOf(
                VisualFrame(
                    "① 事件发生 → 28 天内发出索赔意向通知（逾期视为放弃）",
                    nodes = listOf(
                        box("evt", "索赔事件\n发生", 0.06f, 0.4f, VisualTone.BAD, w = 0.24f, h = 0.2f),
                        box("intent", "索赔意向通知\n28 天内", 0.44f, 0.4f, VisualTone.ACTIVE, w = 0.34f, h = 0.2f, caption = "先报意向，续报详情"),
                    ),
                    edges = listOf(edge("evt", "intent", active = true, label = "时限")),
                ),
                VisualFrame(
                    "② 提交正式索赔报告：事实+依据+损失计算+证据",
                    nodes = listOf(
                        box("intent", "意向已报", 0.06f, 0.4f, VisualTone.MUTED, w = 0.22f, h = 0.18f),
                        box("report", "索赔报告\n事实/合同依据/损失/证据", 0.4f, 0.4f, VisualTone.GOOD, w = 0.42f, h = 0.22f, caption = "证据链是核心得分点"),
                    ),
                    edges = listOf(edge("intent", "report", active = true)),
                ),
                VisualFrame(
                    "③ 监理/工程师审核：核实证据，双方协商确定补偿",
                    nodes = listOf(
                        box("report", "索赔报告", 0.06f, 0.4f, VisualTone.MUTED, w = 0.24f, h = 0.18f),
                        circle("eng", "工程师\n审核", 0.44f, 0.4f, VisualTone.ACTIVE, w = 0.2f, h = 0.2f),
                        box("settle", "协商确定\n工期/费用补偿", 0.72f, 0.4f, VisualTone.GOOD, w = 0.28f, h = 0.2f),
                    ),
                    edges = listOf(edge("report", "eng"), edge("eng", "settle", active = true)),
                ),
                VisualFrame(
                    "④ 协商不成 → 按合同争议条款：调解/仲裁/诉讼",
                    nodes = listOf(
                        box("dis", "争议解决\n调解→仲裁/诉讼", 0.3f, 0.4f, VisualTone.BAD, w = 0.4f, h = 0.22f, caption = "按合同约定渠道，注意仲裁与诉讼二选一"),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 1：流媒体协议对比 ============
        VisualLesson(
            id = "md-streaming-protocols",
            title = "流媒体协议对比：HLS / DASH / CMAF 与 RTC",
            examId = VisualRepository.MD,
            chapter = "流媒体技术",
            keywords = listOf("HLS", "DASH", "CMAF", "流媒体协议", "RTMP", "WebRTC", "协议选型"),
            takeaway = "HLS 苹果系事实标准（TS+m3u8）、DASH 国际标准（开放灵活）、CMAF 统一两者分片格式省存储；RTMP 推流上行、WebRTC 毫秒级实时。选型=生态×延迟×开放度。",
            frames = listOf(
                VisualFrame(
                    "① HLS：Apple 提出的事实标准——TS 分片 + m3u8 清单",
                    nodes = listOf(
                        box("hls", "HLS\nTS 分片 / m3u8", 0.06f, 0.38f, VisualTone.ACTIVE, w = 0.32f, h = 0.24f, caption = "苹果生态必选；延迟传统 15~30 秒"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② DASH：MPEG 国际标准——不绑定编解码器与容器，最开放",
                    nodes = listOf(
                        box("dash", "MPEG-DASH\n国际标准\n开放灵活", 0.06f, 0.38f, VisualTone.GOOD, w = 0.34f, h = 0.24f, caption = "ISO/IEC 23009"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ CMAF：统一 HLS 与 DASH 的分片格式——一份内容两端服务",
                    nodes = listOf(
                        box("hls2", "HLS", 0.06f, 0.4f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("dash2", "DASH", 0.06f, 0.66f, VisualTone.MUTED, w = 0.16f, h = 0.16f),
                        box("cmaf", "CMAF\n统一分片", 0.4f, 0.4f, VisualTone.PATH, w = 0.28f, h = 0.22f, caption = "存储减半、CDN 复用"),
                    ),
                    edges = listOf(edge("hls2", "cmaf", active = true), edge("dash2", "cmaf", active = true)),
                ),
                VisualFrame(
                    "④ 延迟谱系：RTMP 推流 → HLS/DASH 秒级 → LL-HLS 2~5 秒 → WebRTC 毫秒级",
                    nodes = listOf(
                        text("l1", "WebRTC：连麦/会议（<1 秒）", 0.06f, 0.14f, w = 0.6f, tone = VisualTone.GOOD),
                        text("l2", "LL-HLS：直播互动（2~5 秒）", 0.06f, 0.4f, w = 0.6f),
                        text("l3", "HLS/DASH：点播与普通直播（秒级）", 0.06f, 0.66f, w = 0.66f, tone = VisualTone.PATH),
                    ),
                    edges = listOf(),
                ),
            ),
        ),

        // ============ MD 2：音视频同步 ============
        VisualLesson(
            id = "md-av-sync",
            title = "音视频同步：时间戳与唇音对齐",
            examId = VisualRepository.MD,
            chapter = "视频参数与计算",
            keywords = listOf("音视频同步", "时间戳", "PTS", "唇音同步", "时钟", "同步"),
            takeaway = "音视频分开编码传输后靠时间戳（PTS）重对齐；统一参考时钟是基准；唇音失序超过 ±80ms 人眼可感——同步是播放体验的隐形底线。",
            frames = listOf(
                VisualFrame(
                    "① 问题来源：音视频分开编码/传输，到达时间各不相同",
                    nodes = listOf(
                        box("aud", "音频流\n时间戳序列", 0.06f, 0.16f, VisualTone.GOOD, w = 0.3f, h = 0.2f),
                        box("vid", "视频流\n时间戳序列", 0.06f, 0.6f, VisualTone.ACTIVE, w = 0.3f, h = 0.2f),
                        text("net", "网络传输后错位 → 播放需重对齐", 0.42f, 0.4f, w = 0.5f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 解决机制：编码时打时间戳，播放时按统一时钟对齐",
                    nodes = listOf(
                        box("pts", "PTS 时间戳\n打在每帧上", 0.06f, 0.4f, VisualTone.PATH, w = 0.3f, h = 0.2f),
                        box("clk", "统一参考时钟", 0.46f, 0.4f, VisualTone.ACTIVE, w = 0.34f, h = 0.2f, caption = "音频时钟通常作主时钟"),
                    ),
                    edges = listOf(edge("pts", "clk", active = true)),
                ),
                VisualFrame(
                    "③ 感知阈值：唇音失序 ±80ms 内难察觉，超过明显（人耳更敏感声音提前）",
                    nodes = listOf(
                        text("t1", "音频比画面早/晚 > 80ms → 观众明显察觉", 0.04f, 0.34f, w = 0.72f, tone = VisualTone.BAD),
                        text("t2", "直播连麦还要加「网络抖动缓冲」动态对齐", 0.04f, 0.6f, w = 0.7f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 多机位场景：时间码（Timecode）统一各机位时钟",
                    nodes = listOf(
                        box("cam1", "机位A\nTimecode", 0.06f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("cam2", "机位B\nTimecode", 0.36f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("sync", "后期按时间码对齐切切", 0.68f, 0.4f, VisualTone.PATH, w = 0.3f, h = 0.2f),
                    ),
                    edges = listOf(edge("cam1", "sync"), edge("cam2", "sync", active = true)),
                ),
            ),
        ),

        // ============ MD 3：著作权权利体系 ============
        VisualLesson(
            id = "md-copyright-rights",
            title = "著作权权利体系：人身权与财产权",
            examId = VisualRepository.MD,
            chapter = "标准与版权",
            keywords = listOf("著作权", "人身权", "财产权", "署名权", "信息网络传播权", "保护期", "转让"),
            takeaway = "人身权（发表/署名/修改/保护作品完整）与作者人身不可分、不可转让；财产权（复制/发行/信息网络传播等）可授权可转让、有保护期。上传网络供随时获取=信息网络传播权。",
            frames = listOf(
                VisualFrame(
                    "① 人身权四项：发表权、署名权、修改权、保护作品完整权",
                    nodes = listOf(
                        box("pub", "发表权", 0.04f, 0.4f, VisualTone.ACTIVE, w = 0.18f, h = 0.16f),
                        box("sig", "署名权", 0.26f, 0.4f, VisualTone.GOOD, w = 0.18f, h = 0.16f, caption = "永久保护"),
                        box("mod", "修改权", 0.48f, 0.4f, VisualTone.NEUTRAL, w = 0.18f, h = 0.16f),
                        box("int", "保护作品完整权", 0.7f, 0.4f, VisualTone.NEUTRAL, w = 0.28f, h = 0.16f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "② 人身权特性：与作者人身不可分离，不可转让、不可剥夺",
                    nodes = listOf(
                        box("no1", "不可转让", 0.14f, 0.4f, VisualTone.BAD, w = 0.24f, h = 0.18f),
                        box("no2", "署名权永久", 0.5f, 0.4f, VisualTone.BAD, w = 0.26f, h = 0.18f, caption = "死后续存，无期限"),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "③ 财产权清单：复制、发行、出租、展览权 + 信息网络传播权等",
                    nodes = listOf(
                        box("cp", "复制权\n发行权", 0.06f, 0.4f, VisualTone.GOOD, w = 0.24f, h = 0.2f),
                        box("net", "信息网络传播权\n上传网络供随时获取", 0.42f, 0.4f, VisualTone.ACTIVE, w = 0.36f, h = 0.22f, caption = "互联网时代核心权利"),
                        box("more", "改编/翻译/\n放映权…", 0.74f, 0.4f, VisualTone.NEUTRAL, w = 0.24f, h = 0.2f),
                    ),
                    edges = listOf(),
                ),
                VisualFrame(
                    "④ 财产权特性：可授权可转让，有保护期（公民：终生+50 年）",
                    nodes = listOf(
                        text("p1", "公民作品：终生 + 去世后 50 年（12 月 31 日止）", 0.04f, 0.24f, w = 0.8f, tone = VisualTone.PATH),
                        text("p2", "法人作品/视听作品/软件：首次发表后 50 年", 0.04f, 0.52f, w = 0.8f),
                        text("p3", "商业使用要取得财产权许可；人身权永远尊重", 0.04f, 0.8f, w = 0.8f, tone = VisualTone.BAD),
                    ),
                    edges = listOf(),
                ),
            ),
        ),
    )
}
