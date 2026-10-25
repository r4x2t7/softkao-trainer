# 软考刷题 · SoftKao Trainer

> 离线优先的 Android 软考刷题 App。题库用 GitHub 托管，随时更新；答错自动进错题本，按知识点分析掌握度。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 为什么做这个

2026-10-24 考完软考中级两场考试后复盘，发现痛点很具体：

- 题库散在机构 App、论坛帖、真题回忆版里，口径不一、解析常常只有一句"故选 B"；
- 机构题库要会员、要网络、刷不了离线；
- 错题只进一个列表，没人帮你安排复习。

于是决定自己做一个：**题库是 JSON，放在 GitHub 上，谁都能提 PR 加题；App 本地优先，装上就能刷；错题自动分析，按知识点给你画像。**

## 当前支持的考试

| 考试 | 科目 | 题库文件 | 题量 | 章节 / 知识点 |
|---|---|---|---|---|
| 系统集成项目管理工程师（中级） | 基础知识 / 案例分析 | [`question-bank/system-integration/v2.json`](question-bank/system-integration/v2.json) | **1335 题** | 36 章 / 1198 知识点 |
| 多媒体应用设计师（中级） | 基础知识 / 应用技术 | [`question-bank/multimedia-designer/v2.json`](question-bank/multimedia-designer/v2.json) | **1055 题** | 23 章 / 997 知识点 |
| 合计 | | | **2390 题** | 59 章 / 2195 知识点 |

题型分布：单选 1416 / 多选 432 / 判断 320 / 案例 239；平均解析 133 字，每题均给出依据或推导，并标注「改编自…（考点）」或「自编（依据…）」来源。

**每题都带 AI 深度解析**：结论先行 → 依据与推导 → 易错点，160~280 字；App 内置，离线也能看。
**章节精讲资料**：按章组织的学习资料（概览 / 命题趋势 / 速记卡 / 易错清单 / 公式与关键数字 / 记忆口诀 / 主观题答题模板），见 `question-bank/materials/<examId>.json`。
**逐知识点精讲**：2195 个知识点**逐条**一句话讲清（定义 + 关键数字/公式 + 易错点），见 `question-bank/<examId>/kp_notes.json`，**100% 覆盖**。
**抽象图解（帧动画）**：28 个可逐帧播放的示意动画（关键路径、挣值、变更七步、沟通渠道、质量成本、I/P/B 帧、JPEG 五步、采样量化、色彩模型、储备边界、配置库与基线、干系人矩阵、帕累托、生命周期模型、三种补间、四种冗余、音频格式、备份容灾、进度压缩、风险矩阵、合同类型、组织结构、WBS 分解、质量工具、MPEG 体系、矢量与位图、Huffman 编码、信噪比与动态范围）。
**结构图解（整页卡片）**：59 张离线 HTML 图解（OSI 分层、法律层级、信息安全地图、生命周期曲线、质量工具对比、流程图、公式卡…），深浅两套配色，点开整屏查看。

## 法条与数字是怎么核的

题库里的法条引用和法定数字，**逐条对着权威法条全文核过**，不是凭记忆写的。

```bash
python tools/grab_laws.py            # 抓法条正文到本地
python tools/law_cite_audit.py       # 把题库里的「第X条」与法条原文对照
python tools/which_article.py <法条文件> <关键词>   # 某段话属于第几条
python tools/fetch_grep.py <url> <正则>            # 抓网页，只回显命中的行
python tools/fix_law_citations.py    # 应用勘误（已接入 publish.py）
```

审计结果见 [`docs/research/LAW-AUDIT.md`](docs/research/LAW-AUDIT.md)。已经查出并修掉的典型问题：

- **条号引错**：专利权期限在第四十二条（第二十二条讲的是新颖性/创造性/实用性）；投标保证金在《招标投标法实施条例》第二十六条；履约保证金在《招标投标法》第四十六条、实施条例第五十八条
- **引用已废止的旧规**：投标保证金「最高不超过 80 万元」出自 2003 年的旧办法，2013 年九部委修订时已删除，现行只有「不超过估算价 2%」——有 6 道题还在用旧规，其中一道连答案都是错的
- **数字错**：政府采购质疑期限是「知道或者应当知道权益受损之日起 **7 个工作日**」，不是 10 个工作日
- **用了旧法分类**：旧《标准化法》四分法，现行是五类（国标/行标/地标/团标/**企标**）
- **著作权保护期**：2020 年修正后，法人作品与视听作品是「发表权自创作完成后 50 年、财产权自首次发表后 50 年」，视听作品不是「终生加 50 年」

勘误以「外科手术式」字符串替换落地在 `tools/fix_law_citations.py`，已接入发布流水线，**每次出包都会重新校验并应用**。CI 里还有两道门禁：答案与 AI 解析首句必须一致、每个知识点都必须有精讲。

## 测试与自检

不依赖设备也能跑的测试，全部放在 CI 里：

```bash
# JVM 单元测试 + Compose 界面测试（Robolectric 渲染真实 Compose，用语义树断言）
./gradlew testDebugUnitTest

# UI 契约检查：文字不许被省略号截断、路由齐全、科目作用域统一、图解不越界
python tools/ui_contract_check.py

# 逐知识点精讲覆盖率 / 长度
python tools/kp_notes_pipeline.py coverage
python tools/kp_notes_lint.py

# 答案与 AI 解析首句是否自洽
python tools/check_ai_consistency.py

# 图解素材体检：把 59 张 HTML 渲染成 PNG 做视觉检查 + 坐标越界检查
python tools/render_visuals.py --size=640x900
python tools/check_visual_bounds.py
```

**为什么能在没有设备的情况下测界面**：Robolectric 能把 Compose 真正渲染出来，
语义树里能读到「这段文字确实显示了」。抽出来的可测部件见
`KnowledgeCards.kt`（章节卡 / 知识点卡 / 图解卡）与 `PracticeWidgets.kt`（判断题选项行 / 底部跳转栏）。

**踩过的坑**
- Robolectric 会自动创建 `SoftKaoApp`，它在后台线程往 SQLite 灌种子数据，
  而 Robolectric 的 SQLite 影子不支持跨线程 → 测试要用 `TestApp`（不初始化 ServiceLocator）
- Android 的 `org.json` 在 JVM 上是只会抛异常的桩类，单元测试要额外引 `org.json:json`
- 无头 Chrome 在 Windows 上有最小窗口宽度（约 504px），`--screenshot` 截到的是被裁过的图，
  看起来像「右边被切了」——其实是截图假象，量一下 `window.innerWidth` 才知道

## 模拟器 UI 走查（不需要真机）

除了 JVM 单元测试，还能用 Android 模拟器做真实运行时走查：真 Android、真 WebView、
真 SQLite/DataStore、真触摸事件。

```bash
# 一次性准备（需要下载约 1.2GB）
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"
avdmanager create avd -n sk34 -k "system-images;android-34;google_apis;x86_64" -d pixel_6

# 起模拟器（无窗口，WHPX 硬件加速）
emulator -avd sk34 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -port 5554

# 装 APK 并走查
adb -s emulator-5554 install -r -t app/build/outputs/apk/debug/app-debug.apk
python tools/ui_walkthrough.py     # 走 19 项断言 + 截图到 build/ui-shots/
```

`tools/ui_walkthrough.py` 只认 `emulator-` 前缀的设备，**不会碰插着的实体机**。

**写这类脚本的坑**
- `uiautomator dump` **只报屏幕上可见的节点**——页面下方的内容要滚动后才看得到
- 别用文本匹配导航：首页搜索框的提示文字里也含「知识点」，按文本点会跳错页；底部导航栏要按坐标点
- 判断题要按「√ / ×」匹配，别用「正确」——题干里也可能有「正确的有哪些」

## 功能

- **练习模式**：顺序练习、章节练习、专项（知识点）练习、每日一练、模拟卷（90 分钟倒计时、**交卷后才公布答案与解析**）、错题重做
- **选题页**：首页「按章节（36）/ 按知识点（1198）」→ 独立页面，**纵向列表 + 搜索**（上千个知识点横着滑是找不到的）
- **四种题型**：单选、多选、判断（渲染 √/× 按钮）、案例分析（案例题按「先作答 → 对照参考答案 → 自评记住了/没记住」处理）
- **知识点模块**：底部「📚 知识点」页 → **章节纵向列表**（题量 / 知识点数 / 已练 / 正确率）→ 章节详情（概览、抽象图解、结构图解、**知识点清单纵向列表**、速记卡、易错清单、公式与关键数字、记忆口诀、主观题答题模板），一键练习本章 / 练该考点
- **图解库**：59 张结构型考点图解，按章节筛选，**纵向列表**，点开整屏查看；另有 28 个可逐帧播放的抽象图解（带考点标签 + 要点前置）
- **搜索**：按题干 / 解析 / 知识点 / 章节全文检索，支持「搜题目 / 搜知识点」切换，带防抖与科目过滤（题库到千级后的刚需）
- **错题分析**：答错自动入库；按章节/知识点/掌握度筛选；**按知识点聚合的条形热力图**直接指出最该回去刷的考点；空状态给引导而不是摆三个 0
- **间隔复习**：错题按 1 / 3 / 7 / 15 天阶梯安排下次复习时间
- **收藏夹**：题目收藏后可在错题本内单独筛选复习
- **全局唯一科目**：任意页面切换科目，全站（首页 / 知识点 / 错题本 / 统计 / 搜索 / 设置）**立即同步**；两科题库、错题、掌握度完全隔离
- **统计**：总题量、正确率、连续打卡、近 7 天趋势、各科目概览、知识点掌握度
- **题库更新**：填 GitHub 仓库地址即可拉取新题、章节精讲与逐知识点精讲，**增量合并，不影响你的答题记录、错题和收藏**
- **AI 深度解析**：题库内置，离线也能看；也可自行配置任意 OpenAI 兼容接口实时生成
- **完全离线**：无账号、无广告、无埋点，断网可用

## 题库生产流水线

题库到千级靠的是流水线，不是手工：

```
分片生产（16 个分片并行）→ 合并去重 → 校验 → 重建索引与内置种子 → CI 门禁 → 发版
```

- 分片任务书与章节名规范：[`docs/QUESTION-PLAN.md`](docs/QUESTION-PLAN.md)
- 合并脚本：`tools/merge_banks.py`（按 id + 归一化题干双重去重，输出章节/题型分布与质量门禁结果）
- 校验脚本：`tools/validate_bank.py`（字段、答案一致性、解析长度、id 唯一性）
- 索引与种子重建：`tools/build_index.py`（生成带 sha256 的 `index.json`，同步 App 内置题库）
- CI：`.github/workflows/validate-bank.yml` 检查「分片与合并结果一致」「索引与内置种子一致」「题库规模达标」
- 质量门禁：两科各 ≥800 题、每章 ≥8 题、单章占比 ≤15%、总量 ≥2000 题、题干零重复

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / UI | Kotlin + Jetpack Compose + Material 3 |
| 架构 | 单 Activity + Navigation Compose + MVVM（StateFlow） |
| 持久化 | 平台 SQLite（`SQLiteOpenHelper`）+ DataStore |
| 网络 | 平台 `HttpURLConnection`（仅用于题库拉取与 AI 解析） |
| 依赖注入 | 手写 `ServiceLocator`（规模上来再换 Hilt） |
| 最低版本 | Android 8.0（minSdk 26），compileSdk 36 |

> 刻意不引入 Room/KSP 等注解处理器：构建更稳、依赖更少，数据访问收敛在 Repository 层，后续换 Room 只需替换实现。

## 快速开始

### 用 Android Studio 打开

1. Android Studio → Open → 选择本目录
2. 等待 Gradle 同步（需要 JDK 17）
3. Run ▶ 到真机或模拟器

### 命令行构建

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 题库与更新通道

题库格式见 [`docs/QUESTION-BANK-SCHEMA.md`](docs/QUESTION-BANK-SCHEMA.md)。要点：

- 一个考试一个 JSON 文件，字段固定为 `id / examId / subject / type / difficulty / chapter / knowledgePoint / stem / options / answer / explanation / source / tags …`
- `id` 一经发布不可更改；重复导入视为更新，**不会覆盖你的作答与错题数据**
- `explanation` 必须 ≥ 20 字，必须讲清"为什么"

### 让 App 拉取更新

「题库」页填入题库仓库的 raw 地址：

```
https://raw.githubusercontent.com/<你的用户名>/softkao-trainer/main/question-bank
```

App 会先拉 `index.json` 比对版本，再按需下载新版本，校验 SHA-256 后逐题导入；不合法的题会被跳过并提示，不影响其余题目。

### 贡献题目

见 [`CONTRIBUTING.md`](CONTRIBUTING.md)。简单说：照着 schema 加题 → 本地校验 → 提 PR。

## AI 解析配置

「题库」页填写：

- Base URL：如 `https://api.openai.com/v1` 或任意兼容服务
- API Key
- 模型名：如 `gpt-4o-mini`、`qwen-plus`

请求体走标准 `POST {base}/chat/completions`，提示词模板见 [`docs/research/题库来源调研报告.md`](docs/research/题库来源调研报告.md)。

## 目录结构

```
app/src/main/java/com/softkao/trainer/
├── core/model/        领域模型（题目、作答、错题、掌握度、资料、图解）
├── core/ui/           全局科目等跨页面共享的 Compose 状态
├── core/data/
│   ├── db/            SQLite 表结构与 DAO
│   ├── repo/          题目 / 练习 / 统计 仓储
│   └── remote/        题库更新通道、AI 解析
├── feature/
│   ├── home/          首页与练习入口
│   ├── practice/      刷题、答题卡、底部跳转栏、解析
│   ├── knowledge/     章节 / 知识点（横向滑动）+ 抽象图解播放器
│   ├── search/        搜题目 / 搜知识点
│   ├── wrong/         错题本与错题分析（知识点热力图）
│   ├── stats/         统计与掌握度
│   ├── bank/          题库更新与设置
│   └── common/        通用组件
└── ui/theme/          主题
```

```
question-bank/
├── index.json                       # 版本清单（含题库与精讲的 sha256）
├── <examId>/v2.json                 # 题库
├── <examId>/kp_notes.json           # 逐知识点一句话精讲
└── materials/<examId>.json         # 章节精讲
```

## 文档

- [需求评审纪要](docs/REQUIREMENTS-REVIEW.md) —— 范围、技术选型、验收标准
- [题库规范与更新通道](docs/QUESTION-BANK-SCHEMA.md)
- [题库来源调研报告](docs/research/题库来源调研报告.md) —— 来源盘点、AI 方案、竞品对标

## 版权声明

本仓库**不收录任何商业机构真题原文**。所有题目均为依据公开考试大纲与教材知识点自编或改编，`source` 字段标注来源。请贡献者同样遵守这条红线。

## 许可

[MIT](LICENSE)。题库内容同样按 MIT 协议开放。
