package com.softkao.trainer.core.data

/**
 * 学习中心的数据源：按最新考试大纲（2024 年启用，配第 3 版教程）组织的学习路径。
 *
 * 内容组成：
 * - [LearnChapter.sections]：大纲要求条目（掌握/熟悉/了解三档）；
 * - [LearnChapter.summary]：本章大白话讲解（回答「这章到底在讲啥」）；
 * - [LearnChapter.hooks]：直观记忆钩子（类比、口诀、数字锚点）；
 * - [LearnChapter.bankChapters]：对应题库章节（练习与章节精讲的入口）。
 *
 * 讲解为自编的通俗化表述，不替代官方教材。
 */
object LearnRepository {

    data class LearnSection(val title: String, val level: String)

    data class LearnChapter(
        val id: String,
        val examId: String,
        val order: Int,
        val title: String,
        val summary: String,
        val sections: List<LearnSection>,
        val howTested: String,
        val hooks: List<String>,
        val bankChapters: List<String>,
    )

    private val siChapters = listOf(
        LearnChapter(
            id = "si-l01", examId = VisualRepository.SI, order = 1,
            title = "信息化发展",
            summary = "这一章回答「为什么要信息化」：信息、信息系统的基本概念，信息化体系的六个要素，" +
                "以及数字经济发展、两化融合、新型基础设施这些国家层面的方向。它是全书的地基，" +
                "后面的技术与管理都建立在这些概念上。",
            sections = listOf(
                LearnSection("信息与信息系统的概念、功能", "掌握"),
                LearnSection("信息化体系六要素", "熟悉"),
                LearnSection("两化融合与数字经济", "熟悉"),
                LearnSection("新型基础设施的构成", "了解"),
                LearnSection("信息系统生命周期", "掌握"),
            ),
            howTested = "综合知识稳定出 2~4 分：概念辨析（信息≠数据）、六要素归类、生命周期阶段顺序。题干不绕，考的是「背得准不准」。",
            hooks = listOf(
                "六要素口诀「信息技术是手段，信息资源是核心，信息网络是通道，信息人才是根本，法律法规保秩序，应用是目的」",
                "生命周期顺序：立项→开发（规划分析设计实施验收）→运维→消亡，「先立项、再建设、后运维」",
            ),
            bankChapters = listOf("信息化常识", "新兴技术"),
        ),
        LearnChapter(
            id = "si-l02", examId = VisualRepository.SI, order = 2,
            title = "信息技术发展",
            summary = "这一章是「技术工具箱」：网络（OSI/TCP-IP、网络设备）、存储（RAID、存储层次）、" +
                "数据库与数据仓库、中间件等基础设施技术。不要求会配置，但要求概念清、层次明、参数准。",
            sections = listOf(
                LearnSection("OSI 七层与 TCP/IP 四层模型及对应关系", "掌握"),
                LearnSection("网络设备与网络协议的作用", "熟悉"),
                LearnSection("存储层次结构与 RAID 级别", "熟悉"),
                LearnSection("数据库与数据仓库、事务与完整性约束", "熟悉"),
                LearnSection("中间件的概念与作用", "了解"),
            ),
            howTested = "每年 4~6 分的稳定粮仓：模型对应、RAID 容错能力、存储层次排序、数仓与数据库的区别都是循环考。",
            hooks = listOf(
                "OSI 记「物数网传会表应」，TCP/IP 记「网传应」三层合并：上三层并入应用、下两层并入接口",
                "RAID 口诀「0 条带无冗余、1 镜像对半、5 单校验坏一、6 双校验坏二」",
                "存储金字塔「越靠 CPU 越快越贵越小」",
            ),
            bankChapters = listOf("信息化常识"),
        ),
        LearnChapter(
            id = "si-l03", examId = VisualRepository.SI, order = 3,
            title = "信息技术服务",
            summary = "这一章讲「IT 怎么作为一种服务来管」：IT 服务的组成、ITSS 服务标准体系四要素" +
                "（人员、过程、技术、资源）、服务生命周期与服务质量管理。系统集成商的运维业务就建立在它上面。",
            sections = listOf(
                LearnSection("IT 服务的概念与组成", "熟悉"),
                LearnSection("ITSS 四要素（人员/过程/技术/资源）", "掌握"),
                LearnSection("IT 服务生命周期与服务质量评价", "了解"),
            ),
            howTested = "1~2 分的小考点，但出现频率不低：四要素归属、生命周期阶段顺序。背关键词即可。",
            hooks = listOf(
                "ITSS 四要素「人过技资」——人员是主体、过程是规范、技术是手段、资源是保障",
            ),
            bankChapters = listOf("信息化常识"),
        ),
        LearnChapter(
            id = "si-l04", examId = VisualRepository.SI, order = 4,
            title = "信息系统架构",
            summary = "这一章讲「系统长什么样子」：总体架构（战略系统、业务系统、应用系统、信息基础设施等组成）、" +
                "常见的应用架构类型（单机、C/S、B/S、多层、分布式）以及选型依据。是连接技术与管理的桥梁章。",
            sections = listOf(
                LearnSection("信息系统总体架构的组成", "熟悉"),
                LearnSection("C/S 与 B/S 的特点与取舍", "掌握"),
                LearnSection("多层体系结构与中间层的作用", "熟悉"),
                LearnSection("架构选型的依据", "了解"),
            ),
            howTested = "2~3 分：给场景选架构（交互频繁选 C/S、广域部署选 B/S）、识别某层职责。理解「层」的分工就稳。",
            hooks = listOf(
                "C/S「专用的快」、B/S「通用的广」——交互密用 C/S，全网部署用 B/S",
                "多层架构的核心动机：把业务逻辑从客户端抽出来集中管理，改一处全生效",
            ),
            bankChapters = listOf("信息化常识"),
        ),
        LearnChapter(
            id = "si-l05", examId = VisualRepository.SI, order = 5,
            title = "软件工程",
            summary = "这一章讲「软件是怎么被工程化生产出来的」：开发模型（瀑布、迭代、敏捷）、需求工程、" +
                "面向对象的基本概念、软件测试与维护。与题库的「需求管理」强相关。",
            sections = listOf(
                LearnSection("生命周期模型的选择（瀑布/迭代/增量/敏捷）", "掌握"),
                LearnSection("需求工程：获取、分析、确认、跟踪", "掌握"),
                LearnSection("面向对象的基本概念（封装/继承/多态）", "熟悉"),
                LearnSection("软件测试的分类与 V 模型", "了解"),
            ),
            howTested = "2~4 分：模型选型场景题、需求可控性、面向对象三特征辨析。需求部分与案例分析联动。",
            hooks = listOf(
                "需求明确用瀑布、需求模糊用迭代或敏捷——选型题一句话定",
                "封装藏细节、继承管复用、多态管「同名不同形」",
            ),
            bankChapters = listOf("需求管理", "项目管理概论"),
        ),
        LearnChapter(
            id = "si-l06", examId = VisualRepository.SI, order = 6,
            title = "数据工程",
            summary = "这一章讲「数据从哪来、怎么管、怎么用」：数据采集与清洗、数据标准化与治理、" +
                "数据仓库与数据分析。数字政府与大数据项目的基础章。",
            sections = listOf(
                LearnSection("数据工程的基本过程", "了解"),
                LearnSection("数据治理与数据标准化", "熟悉"),
                LearnSection("数据仓库与数据分析", "熟悉"),
            ),
            howTested = "1~2 分，常与大数据、数字经济结合出概念题。记住「治理管质量与规范、仓库管分析」。",
            hooks = listOf(
                "数据治理四问：谁负责、什么标准、质量怎么评、安全问题谁管",
            ),
            bankChapters = listOf("信息化常识"),
        ),
        LearnChapter(
            id = "si-l07", examId = VisualRepository.SI, order = 7,
            title = "软硬件系统集成",
            summary = "这是「本行」章：系统集成的概念与分类、信息孤岛的成因与消除、集成项目的实施内容。" +
                "考试结合项目管理场景出题，也常出现在案例分析背景里。",
            sections = listOf(
                LearnSection("系统集成的概念与分类", "熟悉"),
                LearnSection("信息孤岛的成因与消除手段", "熟悉"),
                LearnSection("系统集成项目的实施要点", "了解"),
            ),
            howTested = "1~2 分概念题 + 案例背景常客。抓住「接口、标准、数据打通」三个关键词。",
            hooks = listOf(
                "信息孤岛 = 各建各的、标准不一、数据不通；消除靠统一规划、统一标准、接口集成",
            ),
            bankChapters = listOf("信息化常识"),
        ),
        LearnChapter(
            id = "si-l08", examId = VisualRepository.SI, order = 8,
            title = "信息安全工程",
            summary = "这一章把「安全」从概念变成工程：安全的基本属性（CIA）、常见攻击与防护、" +
                "加密与签名体系、等级保护制度与安全工程建设流程。高频且案例分析常客。",
            sections = listOf(
                LearnSection("信息安全五要素（机密/完整/可用/可控/可审查）", "掌握"),
                LearnSection("对称与非对称加密、哈希与数字签名", "掌握"),
                LearnSection("常见攻击与防护手段", "掌握"),
                LearnSection("网络安全等级保护制度", "熟悉"),
                LearnSection("安全工程建设流程", "了解"),
            ),
            howTested = "综合知识 4~6 分：加密体系辨析、攻击手段归类、等保数字；案例常考安全整改与应急处置。",
            hooks = listOf(
                "CIA 判别口诀「看不看得到（机密）、改没改过（完整）、用不用得上（可用）」",
                "签名用「自己的私钥」、加密用「对方的公钥」——方向别反",
                "等保五级、二级以上要备案、三级以上每年测评",
            ),
            bankChapters = listOf("信息安全"),
        ),
        LearnChapter(
            id = "si-l09", examId = VisualRepository.SI, order = 9,
            title = "项目管理概论",
            summary = "项目管理的「世界观」：什么是项目（临时性、独特性）、项目集与项目组合、" +
                "五大过程组与十大知识领域、生命周期类型与裁剪。后续所有过程组章节的总纲。",
            sections = listOf(
                LearnSection("项目的定义与特征（临时性/独特性/渐进明细）", "掌握"),
                LearnSection("五大过程组与十大知识领域", "掌握"),
                LearnSection("预测型/迭代型/增量型/敏捷型生命周期与选型", "掌握"),
                LearnSection("项目集与项目组合的概念", "熟悉"),
                LearnSection("项目裁剪与组织环境", "了解"),
            ),
            howTested = "3~5 分：特征辨析、过程组顺序、生命周期选型是三大循环考点；也是案例分析的理论基础。",
            hooks = listOf(
                "过程组顺序「启规执监收」，监控贯穿始终",
                "临时性≠时间短，是指「有起点有终点」；渐进明细=信息越走越清",
                "选型口诀「需求明确预测型、需求模糊迭代型、逐块交付增量型」",
            ),
            bankChapters = listOf("项目管理概论"),
        ),
        LearnChapter(
            id = "si-l10", examId = VisualRepository.SI, order = 10,
            title = "启动过程组",
            summary = "项目怎么「名正言顺地开始」：商业论证与立项、项目章程的编制与发布、识别干系人。" +
                "章程是项目经理的授权书，这一章的考点都围绕「授权」展开。",
            sections = listOf(
                LearnSection("项目章程的内容与作用", "掌握"),
                LearnSection("项目经理的任命与授权来源", "掌握"),
                LearnSection("商业论证与立项管理", "熟悉"),
                LearnSection("识别干系人的方法与输出", "掌握"),
            ),
            howTested = "综合知识 2~3 分 + 案例高频找茬（无章程开工、口头任命）。章程内容与授权来源是核心。",
            hooks = listOf(
                "章程由「项目以外的机构」签发——发起人给项目经理授权，不是项目经理自己写给自己",
                "启动顺序：商业论证→立项→章程任命→识别干系人→启动会",
            ),
            bankChapters = listOf("启动过程组", "项目整体管理"),
        ),
        LearnChapter(
            id = "si-l11", examId = VisualRepository.SI, order = 11,
            title = "规划过程组",
            summary = "全书分量最重的一章：规划如何覆盖范围、进度、成本、质量、资源、沟通、风险、采购" +
                "八大领域——WBS 怎么分解、进度怎么排、预算怎么定、风险怎么识别与应对。" +
                "考试的计算题大半出自这里。",
            sections = listOf(
                LearnSection("WBS 分解原则与范围基准", "掌握"),
                LearnSection("进度网络图、关键路径、总时差与三点估算", "掌握"),
                LearnSection("成本估算、成本基准与储备", "掌握"),
                LearnSection("风险识别与应对策略", "掌握"),
                LearnSection("沟通管理计划与采购规划", "熟悉"),
                LearnSection("质量管理计划与质量成本", "熟悉"),
            ),
            howTested = "计算题主战场：关键路径、挣值、三点估算每年必考其一；概念题覆盖所有知识域。案例至少一大题。",
            hooks = listOf(
                "三点估算「（乐+4×可+悲）÷6」，±1σ=68.26%、±2σ=95.44%、±3σ=99.73%",
                "关键路径=最长路径=总工期，其上总时差为零",
                "WBS 分解「100% 原则 + 8/80 工时 + 面向可交付成果」",
                "应急储备进基准、管理储备走审批",
            ),
            bankChapters = listOf(
                "项目范围管理", "项目进度管理概述", "活动定义与里程碑", "活动排序与网络图绘制",
                "活动历时估算", "进度网络图与关键路径法", "总时差与自由时差", "项目成本管理",
                "项目质量管理", "项目风险管理", "项目沟通管理", "项目采购管理",
            ),
        ),
        LearnChapter(
            id = "si-l12", examId = VisualRepository.SI, order = 12,
            title = "执行过程组",
            summary = "计划落地怎么「带队伍干活」：指导与管理项目工作、管理团队与冲突处理、沟通信息分发、" +
                "实施采购、质量保证。人的问题集中在这一章。",
            sections = listOf(
                LearnSection("指导与管理项目工作", "熟悉"),
                LearnSection("团队建设、激励与冲突处理", "掌握"),
                LearnSection("管理质量（质量保证）的活动", "熟悉"),
                LearnSection("信息分发与沟通实施", "熟悉"),
                LearnSection("实施采购的过程", "了解"),
            ),
            howTested = "2~4 分 + 案例高频：团队士气低落怎么办、冲突五种处理方式、质量保证 vs 质量控制。",
            hooks = listOf(
                "冲突处理首选「合作/解决问题」，强迫只能救急",
                "团队发展五阶段「形成、震荡、规范、发挥、解散」——公开争执是震荡期的正常现象",
            ),
            bankChapters = listOf("项目整体管理", "团队发展与冲突管理", "项目沟通管理", "项目采购管理", "项目质量管理"),
        ),
        LearnChapter(
            id = "si-l13", examId = VisualRepository.SI, order = 13,
            title = "监控过程组",
            summary = "怎么知道项目「跑没跑偏」以及跑偏了怎么办：监控项目工作、整体变更控制、" +
                "控制范围/进度/成本/质量、监督风险。挣值分析与变更流程是两大核心。",
            sections = listOf(
                LearnSection("整体变更控制的流程与 CCB", "掌握"),
                LearnSection("挣值分析：CV/SV/CPI/SPI/EAC/ETC", "掌握"),
                LearnSection("控制范围与范围蔓延治理", "掌握"),
                LearnSection("控制进度与控制成本的偏差处理", "熟悉"),
                LearnSection("配置审计与配置管理", "熟悉"),
            ),
            howTested = "计算与流程题的双高地：挣值计算、变更七步、范围蔓延辨析每年轮考；案例找茬最爱。",
            hooks = listOf(
                "变更七步「提出→记录→评估→CCB 审批→实施→验证→更新基准并通知」，先纸面后动手",
                "挣值判读：CPI/SPI 小于 1 都不利；典型偏差 EAC=BAC÷CPI",
                "镀金=团队主动送、蔓延=范围失控涨，两者都要走变更",
            ),
            bankChapters = listOf(
                "进度控制与进度成本", "配置审计与变更控制", "项目整体管理", "项目范围管理", "项目风险管理",
            ),
        ),
        LearnChapter(
            id = "si-l14", examId = VisualRepository.SI, order = 14,
            title = "收尾过程组",
            summary = "项目怎么「善始善终」：合同收尾与行政收尾、最终验收与移交、经验教训沉淀、" +
                "团队释放与运维交接。分值不多但全是「送分型」记忆题。",
            sections = listOf(
                LearnSection("合同收尾与行政收尾的内容与区别", "掌握"),
                LearnSection("项目档案与文档归档", "熟悉"),
                LearnSection("经验教训与组织过程资产更新", "掌握"),
                LearnSection("向运维的移交（建转运）", "熟悉"),
            ),
            howTested = "1~2 分选择 + 案例收尾简答（收尾该做什么、少了什么）。背框架就能拿分。",
            hooks = listOf(
                "收尾四件事「验收结算、归档复盘、资源释放、建转运交接」",
                "合同收尾按「份」做、行政收尾按「项目/阶段」做",
            ),
            bankChapters = listOf("收尾过程组", "项目整体管理"),
        ),
        LearnChapter(
            id = "si-l15", examId = VisualRepository.SI, order = 15,
            title = "组织保障",
            summary = "项目背后的「支持系统」：人力资源与团队管理、沟通与干系人、配置管理、" +
                "信息管理。组织层面的机制不到位，项目层面就会出问题。",
            sections = listOf(
                LearnSection("配置管理的三库与基线", "掌握"),
                LearnSection("干系人识别与参与管理", "掌握"),
                LearnSection("人力资源计划与团队管理", "熟悉"),
                LearnSection("信息（文档）管理与配置状态记录", "了解"),
            ),
            howTested = "3~5 分：配置库归属、基线修改流程、干系人方格与参与度矩阵循环考；案例找茬常客。",
            hooks = listOf(
                "三库「开发库随便改、受控库走流程、产品库做备份」",
                "干系人方格「高权高利重点管理、高权低利令其满意、低权高利随时告知、低权低利监督」",
            ),
            bankChapters = listOf(
                "项目人力资源管理", "项目干系人管理", "项目沟通管理", "项目配置管理",
            ),
        ),
        LearnChapter(
            id = "si-l16", examId = VisualRepository.SI, order = 16,
            title = "监理基础知识",
            summary = "信息系统工程监理的角色与工作：监理的四控三管一协调、监理单位与人员的执业要求。" +
                "分值小，但概念题稳定出现。",
            sections = listOf(
                LearnSection("监理的工作内容（四控三管一协调）", "熟悉"),
                LearnSection("监理单位与监理工程师的执业限制", "了解"),
            ),
            howTested = "0~2 分：四控三管一协调的归类、监理与建设单位/承建单位的关系。",
            hooks = listOf(
                "四控「质量进度投资变更」、三管「合同信息（文档）安全」、一协调「沟通协调」",
            ),
            bankChapters = listOf("法律法规与标准规范"),
        ),
        LearnChapter(
            id = "si-l17", examId = VisualRepository.SI, order = 17,
            title = "法律法规和标准规范",
            summary = "与系统集成最相关的法条与标准：招投标法（时限、保证金、评标）、政府采购法" +
                "（质疑投诉）、著作权与专利（保护期限）、标准化法（五类标准）、网络安全法（等保）。数字多、坑多。",
            sections = listOf(
                LearnSection("招标投标法：程序时限、保证金、评标委员会", "掌握"),
                LearnSection("政府采购法：适用主体、质疑与投诉", "熟悉"),
                LearnSection("著作权法与专利法：保护期限与归属", "掌握"),
                LearnSection("标准化法：标准分类与效力", "熟悉"),
                LearnSection("网络安全法与等级保护", "熟悉"),
            ),
            howTested = "综合知识 4~6 分，全是数字与期限考点；记错一个数字就丢分，最适合用卡片反复刷。",
            hooks = listOf(
                "招标「发出到截止最短 20 日、保证金 ≤2%、评标 5 人以上单数、专家 ≥2/3」",
                "质疑「7 个工作日」、投诉「答复期满后 15 个工作日」",
                "专利：发明 20 年、实用新型 10 年、外观设计 15 年；著作权：自然人终生+50、法人创作完成 50 年",
            ),
            bankChapters = listOf("法律法规与标准规范"),
        ),
        LearnChapter(
            id = "si-l18", examId = VisualRepository.SI, order = 18,
            title = "职业道德规范",
            summary = "项目经理的职业操守：诚实守信、廉洁自律、公正客观、保守秘密。分值很少，" +
                "但属于「不能丢」的送分题。",
            sections = listOf(
                LearnSection("项目管理从业人员的基本职业道德", "了解"),
            ),
            howTested = "0~1 分。答案永远选「最正直的那个」。",
            hooks = listOf(
                "记住一句：情面不能大于规范，利益不能大于公正",
            ),
            bankChapters = listOf("职业道德"),
        ),
    )

    private data class MdBlock(
        val id: String,
        val title: String,
        val summary: String,
        val sections: List<LearnSection>,
        val howTested: String,
        val hooks: List<String>,
        val bankChapters: List<String>,
    )

    private val mdBlocks = listOf(
        MdBlock(
            "md-l01", "多媒体基础理论",
            "多媒体的「世界观」：什么是多媒体、五大媒体分类、技术特征（集成/交互/实时）、" +
                "系统组成与超媒体组织方式。概念题的主产地。",
            listOf(
                LearnSection("媒体的五种分类", "掌握"),
                LearnSection("多媒体技术特征（集成/交互/实时）", "掌握"),
                LearnSection("超文本与超媒体", "熟悉"),
            ),
            howTested = "2~4 分概念题：媒体归类、特征辨析。判别口诀见记忆钩子。",
            hooks = listOf(
                "五媒体判别「看扮演什么角色」：原始形态=感觉、编码后=表示、设备=显示、存放=存储、传输线=传输",
                "交互性=用户能控制；电视是线性、多媒体可跳转",
            ),
            bankChapters = listOf("多媒体技术基础", "数字图形与图像基础", "交互设计"),
        ),
        MdBlock(
            "md-l02", "色彩与图像",
            "颜色怎么描述、图像怎么存储：RGB/CMYK/HSV 模型、色域、分辨率与位深、" +
                "未压缩容量公式、索引色与真彩色、DPI/PPI。计算题第一高频区。",
            listOf(
                LearnSection("加色与减色模型及适用场景", "掌握"),
                LearnSection("未压缩图像容量计算", "掌握"),
                LearnSection("分辨率、位深与容量的倍数关系", "掌握"),
                LearnSection("索引色/真彩色、DPI 与 PPI", "熟悉"),
                LearnSection("色域与跨媒体色彩管理", "熟悉"),
            ),
            howTested = "容量计算必考其一；模型与色域概念题常伴。公式「宽×高×位深÷8」必须秒写。",
            hooks = listOf(
                "屏幕加色 RGB（无光为黑）、印刷减色 CMYK（无墨为白）",
                "宽高各翻倍=容量 4 倍；位深翻倍再翻倍",
                "16 位色 800×600 ≈ 937.5KB——记住这道母题",
            ),
            bankChapters = listOf("色彩与色彩管理", "数字图像与格式", "分辨率与图像容量"),
        ),
        MdBlock(
            "md-l03", "音频技术",
            "声音怎么变成数字：采样、量化、编码三步；采样定理与量化位数的两个数字规律；" +
                "容量计算与格式选型（无损 FLAC vs 有损 MP3/AAC）。",
            listOf(
                LearnSection("采样定理与混叠", "掌握"),
                LearnSection("量化位数与动态范围（每位约 6dB）", "掌握"),
                LearnSection("未压缩音频容量计算", "掌握"),
                LearnSection("CD 标准参数与常见格式", "掌握"),
                LearnSection("降噪与音频后期流程", "了解"),
            ),
            howTested = "音频容量计算几乎年年考；采样定理、CD 参数、格式选型循环出现。",
            hooks = listOf(
                "容量公式「采样率×位数×声道×时间÷8」；CD=44.1kHz/16bit/双声道≈10MB/分钟",
                "fs≥2×fmax：语音 8kHz、CD 44.1kHz、影视 48kHz",
                "每位 6dB：16 位≈96dB",
            ),
            bankChapters = listOf("数字音频", "音频参数与计算", "音频格式与编辑"),
        ),
        MdBlock(
            "md-l04", "视频与动画",
            "连续媒体的时间维度：制式与帧率、隔行与逐行、未压缩码率计算、" +
                "I/P/B 帧与 GOP、动画原理与制作流程。",
            listOf(
                LearnSection("PAL/NTSC 制式参数与扫描方式", "熟悉"),
                LearnSection("未压缩单帧容量与码率计算", "掌握"),
                LearnSection("I/P/B 帧依赖与 GOP", "掌握"),
                LearnSection("视觉暂留与帧率", "熟悉"),
            ),
            howTested = "视频计算（单帧→码率→存储）与帧结构概念双高频；动画部分送分。",
            hooks = listOf(
                "PAL 625 行/25fps、NTSC 525 行/30fps",
                "I 帧独立、P 前向、B 双向；GOP 短=访问快但码率高",
                "YUV 4:2:0 每像素平均 1.5 字节",
            ),
            bankChapters = listOf("数字视频", "视频参数与计算", "视频格式与制作流程", "动画技术", "帧与编码"),
        ),
        MdBlock(
            "md-l05", "压缩与流媒体",
            "多媒体的「省带宽工程」：冗余类型与压缩方法对应、JPEG 管线、压缩比计算、" +
                "流媒体协议（RTMP/HLS）、CDN、防盗链与 DRM、多媒体数据库检索。",
            listOf(
                LearnSection("冗余类型与编码方法对应", "掌握"),
                LearnSection("JPEG 压缩管线与有损环节", "掌握"),
                LearnSection("压缩比计算", "掌握"),
                LearnSection("流媒体协议选择与 CDN", "熟悉"),
                LearnSection("DRM 与版权保护技术", "熟悉"),
                LearnSection("多媒体数据库与检索", "了解"),
            ),
            howTested = "压缩概念与 JPEG 流程高频；流媒体部署类案例分析的主产区。",
            hooks = listOf(
                "冗余四类「时间-帧间、空间-变换、统计-熵编码、视听-感知丢弃」",
                "JPEG 管线「分块→DCT→量化（有损在此）→DC 差分→RLE→熵编码」",
                "直播出口带宽=并发数×码率；CDN 就近分发",
            ),
            bankChapters = listOf(
                "压缩编码技术", "冗余与压缩比", "JPEG与DCT", "流媒体技术", "多媒体数据库", "传输与安全",
            ),
        ),
        MdBlock(
            "md-l06", "项目实施与版权",
            "把多媒体作品做成「项目」：开发流程七阶段、生命周期模型选型、交互设计原则、" +
                "素材版权合规与职业道德。",
            listOf(
                LearnSection("多媒体应用系统开发流程", "掌握"),
                LearnSection("原型法与迭代/螺旋模型", "熟悉"),
                LearnSection("交互设计原则", "熟悉"),
                LearnSection("素材版权与合理使用", "掌握"),
            ),
            howTested = "案例必考区：流程找茬、素材先行的坑；综合知识 2~3 分版权与交互概念。",
            hooks = listOf(
                "流程「需求→设计→素材→集成→测试→发布→运维」，素材必须等规格",
                "注明来源≠可以商用；字体看商用授权、音乐看传播权",
            ),
            bankChapters = listOf("多媒体项目实施", "标准与版权", "职业道德"),
        ),
    )

    private val mdChapters: List<LearnChapter> = mdBlocks.map {
        LearnChapter(
            id = it.id,
            examId = VisualRepository.MD,
            order = mdBlocks.indexOf(it) + 1,
            title = it.title,
            summary = it.summary,
            sections = it.sections,
            howTested = it.howTested,
            hooks = it.hooks,
            bankChapters = it.bankChapters,
        )
    }

    fun forExam(examId: String): List<LearnChapter> = when (examId) {
        VisualRepository.SI -> siChapters
        else -> mdChapters
    }

    fun chapter(examId: String, id: String): LearnChapter? =
        forExam(examId).firstOrNull { it.id == id }
}
