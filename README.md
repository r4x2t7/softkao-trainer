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

## 功能

- **练习模式**：顺序练习、章节练习、专项（知识点）练习、每日一练、模拟卷（90 分钟倒计时条）、错题重做
- **四种题型**：单选、多选、判断（渲染 √/× 按钮）、案例分析（案例题按「先作答 → 对照参考答案 → 自评记住了/没记住」处理）
- **知识点模块**：底部「📚 知识点」页 → **章节横向滑动卡片**（题量/已练/正确率/有无精讲）→ 章节详情（概览、命题趋势、抽象图解、**结构图解整页卡片**、**知识点横向滑动卡片**、速记卡、易错清单、公式与关键数字、记忆口诀、主观题答题模板），一键练习本章 / 练该考点
- **图解库**：59 张结构型考点图解，按章节筛选，卡片横向滑动，深浅色自动适配
- **搜索**：按题干 / 解析 / 知识点 / 章节全文检索，支持「搜题目 / 搜知识点」切换，带防抖与科目过滤（题库到千级后的刚需）
- **错题分析**：答错自动入库；按章节/知识点/掌握度筛选；**按知识点聚合的条形热力图**直接指出最该回去刷的考点；连对 3 次且正确率 ≥ 80% 判定「已掌握」
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
