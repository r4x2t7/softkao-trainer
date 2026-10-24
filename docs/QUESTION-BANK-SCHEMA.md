# 题库数据规范与 GitHub 更新通道

> 版本 1.0 ｜ 2026-10-24 ｜ 本文件是 App 内置题库、外置题库仓库、以及自动更新功能共同遵守的唯一契约。

## 1. 为什么题库要独立于 App

App 是安装包，更新要过应用商店审核；题库是纯数据，改一道题不该让用户等审核。
因此：

- **App 内置种子题库**（`app/src/main/assets/seed/`）：保证首次安装离线可用。
- **题库仓库**（GitHub）：数据源，JSON 格式，任何人都能提 PR 加题、改错。
- **App 内更新通道**：设置页填仓库地址 → 拉取 → 校验 → 增量合并进本地库。

## 2. 题库文件结构

```
question-bank/
├── system-integration/
│   ├── v1.json
│   ├── v2.json
│   ├── materials.json           # 本地生成的章节精讲（已同步到 materials/ 目录）
│   └── kp_notes.json            # 逐知识点一句话精讲
├── multimedia-designer/
│   ├── v2.json
│   └── kp_notes.json
├── materials/                   # App 更新通道拉取的章节精讲
│   ├── system-integration.json
│   └── multimedia-designer.json
└── index.json          # 版本清单，App 先拉它再决定拉哪个版本
```

### 2.1 index.json

```json
{
  "updatedAt": "2026-10-24T20:00:00Z",
  "banks": [
    {
      "examId": "system-integration",
      "examName": "系统集成项目管理工程师",
      "version": 2,
      "file": "system-integration/v2.json",
      "questionCount": 1335,
      "sha256": "…",
      "updatedAt": "2026-10-24T20:00:00Z",
      "kpNotes": {
        "file": "system-integration/kp_notes.json",
        "count": 1199,
        "sha256": "…"
      }
    }
  ]
}
```

- `kpNotes` 是**可选**字段：App 拉到后会校验 `sha256`，不匹配就整份丢弃，不写坏数据。
- `sha256` 统一按 **LF 归一化**后的字节计算（Windows 写 CRLF、Git 存 LF，不归一化会误报不匹配）。

### 2.2 kp_notes.json（逐知识点精讲）

```json
{
  "examId": "system-integration",
  "examName": "系统集成项目管理工程师",
  "version": 1,
  "notes": {
    "挣值管理": "挣值三参数：PV 计划价值、EV 挣值、AC 实际成本；CV=EV−AC、SV=EV−PV。",
    "关键路径法": "关键路径是网络图中总时差为零的路径，决定项目最短工期。"
  }
}
```

- key 必须是题库里 `knowledgePoint` 的**原样字符串**，一字不差，否则 App 匹配不上。
- value 是一句话精讲，建议 40~70 字：给出定义 + 关键数字/公式 + 易错点。

### 2.3 题库文件

```json
{
  "examId": "system-integration",
  "examName": "系统集成项目管理工程师",
  "level": "中级",
  "version": 2,
  "updatedAt": "2026-10-24T20:00:00Z",
  "questions": [ Question… ]
}
```

## 3. Question 字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | string | ✅ | 全局唯一，建议 `<examId>-<4位序号>`，如 `si-0001`；**一旦发布不可改**，改题不改 id |
| `examId` | string | ✅ | `system-integration` / `multimedia-designer`，与文件名目录一致 |
| `subject` | string | ✅ | `基础知识`（对应上午选择题）/ `案例分析`（系统集成下午）/ `应用技术`（多媒体下午） |
| `type` | enum | ✅ | `single` 单选 ｜ `multi` 多选 ｜ `truefalse` 判断 |
| `difficulty` | int 1-5 | ✅ | 1 入门 … 5 难 |
| `chapter` | string | ✅ | 章节名，如「项目成本管理」「压缩编码技术」 |
| `knowledgePoint` | string | ✅ | 知识点标签，用于错题分析与掌握度统计，如「挣值管理」 |
| `stem` | string | ✅ | 题干 |
| `options` | array | single/multi 必填 | `[{"key":"A","text":"…"}]`；判断题可省略 |
| `answer` | array[string] | ✅ | 答案 key 数组；多选≥2；判断题固定 `["√"]` 或 `["×"]` |
| `explanation` | string | ✅ | 解析，必须说明**为什么**，不能只复述答案 |
| `aiExplanation` | string/null | ❌ | 可选的 AI 深度解析（多屏、含推导） |
| `source` | string | ✅ | 来源说明，如「改编自《系统集成项目管理工程师教程》第3版 第9章」 |
| `tags` | array[string] | ❌ | 如 `["计算","高频"]` |
| `caseStudy` | string/null | ❌ | 案例分析题的案例背景原文（多道小问共用同一背景时按 `caseId` 归组） |
| `caseId` | string/null | ❌ | 案例分组 id |
| `subQuestions` | array | ❌ | 案例小问：`[{"q":"…","answer":"…"}]` |

## 4. 校验规则（App 导入时强制执行）

1. `id` 全局唯一且非空；同一 `id` 重复导入视为**更新**（upsert），不新增。
2. `type=single` 时 `answer` 长度必须为 1；`type=multi` 时 ≥2；`type=truefalse` 时 ∈ {√, ×}。
3. `answer` 中的每个 key 必须存在于 `options`（判断题除外）。
4. `options` 的 key 必须连续（single/multi：A,B,C…）。
5. `explanation` 长度 ≥ 20 字（防止占位空解析）。
6. `difficulty` ∈ [1,5]；`chapter`、`knowledgePoint` 非空。
7. 导入失败的单题**跳过并记录**，不中断整批导入（坏数据不能拖垮整个更新）。
8. `sha256` 与实际文件不符时拒绝导入（防传输损坏/篡改）。
9. **用户的答题记录永不被题库更新覆盖**：只更新题目内容，历史作答、错题标记、掌握度保留。

## 5. 版权红线（开源项目必须守）

- ❌ 直接复制商业机构真题原文、官方样题原文、付费题库内容。
- ✅ 允许：依据公开考试大纲与教材知识点**自行编写**或**改编**题目；公开领域标准条款（标准号、条文大意）事实性引用。
- ✅ 每题 `source` 必须写明"改编自…（知识点）"或"自编（依据…）"。
- ✅ 采集网络资源只能用于**研究来源清单与考点分布**，不能把原文抄进题库。

## 6. App 侧更新流程

```
设置 → 题库更新 → 填仓库地址（GitHub raw 或自有域名）
   ↓
拉 index.json → 比较本地 version → 需要更新？
   ↓ 是
拉对应 vN.json → sha256 校验 → JSON 解析 → 逐题 schema 校验
   ↓
写入平台 SQLite（事务 + upsert），保留用户作答数据
   ↓
顺带拉 materials/<examId>.json 与 <examId>/kp_notes.json（章节精讲 + 逐知识点精讲）
   ↓
展示结果：新增 N 题 / 更新 M 题 / 跳过 K 题（不合法）+ 同步了 X 条知识点精讲
```

- 章节精讲与逐知识点精讲**独立于题库版本**：即使题库版本没变，每次检查更新也会同步一次资料，
  这样「只补精讲、不加题」的 PR 也能立刻到达用户。
- 资料下载失败**不影响**题库更新结果，两者分开统计。

- 更新全程**离线优先**：网络不可用时只提示"检查更新失败"，不影响任何本地功能。
- 仓库地址与 token 存 DataStore（本地），不上传任何数据到第三方。
