# 交接文档 · SoftKao Trainer（软考刷题 App）

> 交接时间：2026-10-25 ｜ 仓库：https://github.com/r4x2t7/softkao-trainer
> 接手前先读本文件，再读 `docs/REQUIREMENTS-REVIEW.md`（需求评审）与 `docs/QUESTION-PLAN.md`（题库生产计划）。

---

## 1. 一句话现状

一个离线优先的 Android 软考刷题 App：两科题库 **2390 题**（全部带 AI 深度解析）+ **59 章章节精讲** + **2195 条逐知识点精讲（100% 覆盖）** + **87 张可视化图解**（28 个帧动画 + 59 张离线 HTML 卡片），题库与资料托管在 GitHub 可增量更新，仓库 public、CI 全绿，已发版到 v2.3.0。

## 2. 环境（已装好，别重装）

| 工具 | 位置 | 说明 |
|---|---|---|
| JDK 17 | `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` | 构建必需，设 `JAVA_HOME` |
| Git | `C:\Program Files\Git\cmd\git.exe` | 2.56.0（**不在默认 PATH**，脚本里要写全路径） |
| GitHub CLI | `C:\Program Files\GitHub CLI\gh.exe` | 2.103.2，已登录账号 **r4x2t7**（有 repo 权限） |
| Android SDK | `C:\Users\jw\.local\share\android-sdk` | platforms 34/35/36、build-tools 36.0.0 |
| Gradle | 缓存里的 9.3.1 发行版 | 见下方构建命令 |

**构建命令**（在 `E:\ai coding\pojie\softkao-trainer` 下）：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& 'C:\Users\jw\.gradle\wrapper\dists\gradle-9.3.1-all\5wooodlkfwsm42ictrailuf0e\gradle-9.3.1\bin\gradle.bat' -p 'E:\ai coding\pojie\softkao-trainer' assembleDebug --console=plain --offline
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ **别用 `gradlew.bat`**，wrapper 会尝试重新下载 gradle-9.3.1-all 发行包，本机缓存哈希目录对不上，会卡住。
> 用上面那条缓存发行版的命令，加 `--offline`。

## 3. 技术栈与关键约定

- Kotlin + Jetpack Compose + Material 3（compose-bom **2025.09.00**），单模块 `:app`
- **AGP 9.1.0 自带 Kotlin 支持**（内置 Kotlin 2.2.10）：
  - ❌ 不要应用 `org.jetbrains.kotlin.android` 插件（会报错）
  - ✅ 只应用 `org.jetbrains.kotlin.plugin.compose`，版本必须与内置 Kotlin 一致（`2.2.10`）
- 持久化用**平台 SQLite**（`AppDatabase`，手写 DAO），刻意不用 Room/KSP，构建更稳
- 网络用平台 `HttpURLConnection`，JSON 用 `org.json`，无第三方网络/序列化依赖
- 依赖注入：`ServiceLocator`（手写单例，SoftKaoApp.kt）
- 数据流：ViewModel + `StateFlow` + `collectAsStateWithLifecycle`
- **脚本里的路径一律用 `pathlib.Path(__file__).resolve().parent.parent`**，不要硬编码盘符——CI 跑在 ubuntu 上

## 4. 目录地图

```
app/src/main/java/com/softkao/trainer/
├── MainActivity.kt              # 底部 5 栏导航 + NavHost（路由常量在 Routes 里）
├── SoftKaoApp.kt                # Application + ServiceLocator
├── core/
│   ├── model/                   # Models.kt / Materials.kt / Visuals.kt（含 HtmlVisual）
│   ├── ui/ExamScope.kt          # rememberCurrentExamId()：全局科目的响应式订阅
│   └── data/
│       ├── SettingsRepository.kt     # 全局科目 currentExamId（已统一，ExamScope 旧枚举已删除）
│       ├── MaterialRepository.kt     # 章节精讲 + 知识点精讲（本地更新优先于 assets）
│       ├── HtmlVisualRepository.kt   # 59 张静态 HTML 图解清单（assets/visuals/index.json）
│       ├── VisualRepository.kt       # 图解总入口（第一批）
│       ├── VisualRepositoryExtra.kt  # 图解第二批
│       ├── VisualRepositoryThird.kt  # 图解第三批
│       ├── QuestionBankParser.kt    # 题库解析 + 索引解析（含 kpNotes 字段）
│       ├── QuestionBankImporter.kt   # 导入 + sha256（LF 归一化）
│       ├── db/AppDatabase.kt        # 表结构与全部 SQL
│       └── remote/                  # BankUpdater（题库/章节精讲/知识点精讲更新通道）、AiExplainer
└── feature/
    ├── home/ practice/ wrong/ stats/ bank/ knowledge/ search/
    └── knowledge/ConceptAnimation.kt # 帧动画播放器；VisualGalleryScreen.kt 图解库 + WebView 详情
tools/                           # 题库与资料的流水线脚本（见下）
question-bank/<examId>/           # v2.json / kp_notes.json
question-bank/materials/          # 章节精讲（更新通道按这个路径拉）
question-bank/index.json          # 版本清单（含题库与精讲的 sha256）
docs/                             # 需求评审、题库规范、生产计划、调研报告
```

## 5. 题库与资料现状

| 内容 | 状态 |
|---|---|
| 系统集成 v2.json | 1335 题 / 36 章 / 1198 知识点 ✅ |
| 多媒体 v2.json | 1055 题 / 23 章 / 997 知识点 ✅ |
| AI 深度解析 | 2390 题 **100% 覆盖**（160~280 字，结论→依据→易错点）✅ |
| 章节精讲资料 | 59 章全覆盖（概览/趋势/速记卡/易错/公式/口诀/模板）✅ |
| 逐知识点精讲 | **2195 条 / 100% 覆盖**（系统集成 1198 + 多媒体 997）✅ |
| 抽象图解（帧动画） | **28 个** ✅ |
| 结构图解（离线 HTML 卡片） | **59 张**（`assets/visuals/`，59 张全部能匹配到题库章节）✅ |
| 法条引用与法定数字 | 已对权威法条全文逐条核对，修订 15 道题 + 7 条精讲；报告见 `docs/research/LAW-AUDIT.md` ✅ |
| 去重 | 2407 → 2390（删 17 道真重复；单选版/多选版不误删）✅ |
| 纠错 | AI 复核标出 19 处疑点，已逐条修订并同步订正 AI 解析文本 ✅ |

### 流水线脚本（都在 `tools/`）

```bash
python tools/validate_bank.py --all        # 校验题库（字段/答案/解析长度/id 唯一）
python tools/merge_banks.py                # 合并分片 → v2.json（含去重与质量门禁）
python tools/build_index.py                # 重建 index.json（含 sha256 + kpNotes 索引）+ 同步内置种子
python tools/material_pipeline.py split|merge|status   # 章节精讲资料
python tools/kp_notes_pipeline.py split|merge|status|missing|coverage  # 逐知识点精讲
python tools/kp_notes_lint.py [examId]     # 列出长度越界的精讲（人工改写用）
python tools/check_visuals.py             # 图解素材完整性（文件存在 + 章节能匹配）
python tools/law_cite_audit.py            # 法条引用与本地法条原文对照
python tools/which_article.py <法条文件> <关键词>   # 某段话属于第几条
python tools/fetch_grep.py <url|文件> <正则>        # 抓网页，只回显命中的行
python tools/grab_laws.py                 # 抓法条正文到本地（不入库）
python tools/fix_law_citations.py         # 应用法条勘误（publish.py 第 1 步）
python tools/check_ai_consistency.py      # 答案与 AI 解析首句一致性
python tools/publish.py                    # 一键：合并解析+资料+索引+校验
python tools/verify_remote.py              # 校验 GitHub 上的内容完整性
python tools/check_update_channel.py       # 端到端验证更新通道（题库+章节精讲+知识点精讲）
```

**踩过的坑（别再犯）**
- 合并脚本**门禁不过时必须拒绝写出 v2.json**（已改好），否则半成品会被提交、CI 必红
- sha256 必须对 **LF 归一化**后的内容计算：Windows 下 Python 写文件是 CRLF，git 提交是 LF，两边不一致会让 CI 误报
- `python` 在 Windows 控制台打印 ✓/✗ 会 `UnicodeEncodeError`，脚本里统一 `sys.stdout.reconfigure(encoding="utf-8")`
- `.ps1` 脚本里的中文路径字面量会被 PowerShell 按 ANSI 误读 → 脚本只用 ASCII，路径走参数传
- 脚本里的 `ROOT` 不要写死 `E:\ai coding\...`，CI 在 ubuntu 上跑会找不到目录 → 统一用 `__file__` 推导
- GitHub raw 有传播延迟，校验失败先等几秒再判

## 6. 用户已提出并已修复的问题

| 用户原话 | 处理 |
|---|---|
| 界面好多文字自己换行了 | 标签类文字统一单行+省略号；标签行改 FlowRow；chip 限宽；底部导航标签 10sp 单行 |
| 题库显示的数字省略了 | StatTile 改 titleMedium + 8dp 内边距 |
| 顺序练习/每日一练换行了 | 按钮 contentPadding 缩小 + 13sp 单行 |
| 错题本按钮换行 | 导航标签改「错题」+ 10sp |
| 按章节/按知识点要横着滑动 | **章节、知识点都改成横向滑动卡片**（LazyRow）：章节页一屏两章，章节详情里知识点一卡一考点 |
| 科目选择彻底分开、独立掉 | **删除 ExamScope**，改成全局唯一 `currentExamId`；任何一处切换全站生效；两科数据完全隔离；错题本/搜索去掉「全部科目」 |
| 知识点里有时无法切换科目 | 科目切换固定在知识点页顶栏，滚动也看得见 |
| 题目没有选项导致无法提交 | **281 道判断题没有选项** → 刷题页为判断题渲染 √/× 两个可选按钮 |
| 顺序练习只有 200 题、题库 1300+ | 去掉 limit，**全量加载**（1335 题），靠界面分页/跳转 |
| 答题卡小框里塞 200 个按钮 | 答题卡改分批：每批 30 个 + 上一批/下一批 |
| 刷题界面底部要跟页数一样的跳转 | 新增底部跳转栏：⌂首页 / −100 / −10 / 上一页 / 第 x/y 题 / 下一页 / +10 / +100 / 尾页 / 答题卡 |

### v2.4.0 又修的（法条核对）

| 问题 | 处理 |
|---|---|
| 专利权期限引用成《专利法》第二十二条 | 改第四十二条（第二十二条是新颖性/创造性/实用性）。涉及 si-1225/1226/1227/1224 |
| 投标保证金「最高不超过 80 万元」已废止 | 该上限出自 2003 年旧办法，2013 年九部委修订时删除，现行只有「不超过估算价 2%」。修 si-0055/1197/1198/1199/1201/1202，其中 si-1198 连答案都是错的（已改为估算价 3000 万、答案由 C 改 B） |
| 履约保证金引用成第四十一条 | 改《招标投标法》第四十六条 + 实施条例第五十八条 |
| 政府采购质疑期限写成 10 个工作日 | 改 7 个工作日（《政府采购法》第五十二条、财政部令第94号第十条） |
| 政府采购法条号多处错引 | 质疑/答复/投诉 → 第五十二/五十三/五十五条；合同书面形式 → 第四十四条；按预算执行 → 第六条；差别待遇 → 第二十二条 |
| 著作权保护期用了旧口径 | 2020 年修正后，法人作品与视听作品是「发表权自创作完成后 50 年、财产权自首次发表后 50 年」；视听作品不是「终生加 50 年」。修 si-0056/1221/md-0057 |
| 标准化法用了旧法四分法 | 现行是五类（国标/行标/地标/团标/企标）；si-0059 原本四个选项无一正确，已重写 |
| 知识点精讲里 7 条专利/著作权数字错 | 实用新型 10 年、外观设计 15 年、法人作品创作完成后 50 年 |

**机制**：勘误集中在 `tools/fix_law_citations.py`，做外科手术式替换，同时改**分片 + 合并产物 + 内置种子**三处（只改合并产物会被下次 merge 冲掉），并接入 `publish.py` 第 1 步。CI 新增「答案与 AI 解析首句一致性」门禁。

### v2.3.0 又修的

| 问题 | 处理 |
|---|---|
| 切科目后别的页面还停在旧科目 | 各页原来用 `currentExamId.first()` 只取一次快照 → 统一改成订阅 Flow（`rememberCurrentExamId()` / ViewModel 里 `collect`），**任何一处切换全站立刻同步**；顺手删掉「本页独立选择」这类过期文案 |
| 长文字被省略号截断 | 去掉内容型文本上的 `maxLines=1` 与 `TextOverflow.Ellipsis`，改成自然换行完整显示 |
| 知识点精讲更新不了 | 逐知识点精讲原来只读内置 assets → **接入更新通道**：`index.json` 增加 `kpNotes` 字段（file/count/sha256），App 拉取后校验 sha256 再落盘；章节精讲与知识点精讲独立于题库版本同步 |
| 知识点精讲只有一半 | 补齐到 **2195 条 100% 覆盖**；CI 增加覆盖率门禁（`kp_notes_pipeline.py coverage`，缺一条就红） |
| 图解不够 | 从 9 个补到 **28 个** |

## 7. 待办（建议按此顺序接手）

1. **实机验证**：装 v2.3.0 后重点看——章节/知识点横滑是否顺手、全局科目切换是否即时、逐知识点精讲是否出现在章节详情里、模拟卷倒计时条
2. **继续加抽象图解**：还有余量的重点是「多媒体项目实施流程」「广告与数字版权管理」「网络存储与 CDN」「项目成本类型（固定/变动）」「决策树分析」「敏感性分析」
3. **HTML 图解也有增量空间**：`assets/visuals/` 目前 59 张，只覆盖结构型考点；补图时同时更新 `assets/visuals/index.json`，并跑 `python tools/check_visuals.py` 确认文件齐全、章节能匹配到题库
4. **可选增强**：错题本按知识点做「掌握度热力图」而不是只按错误数；模拟卷支持用户自定义题量与时长；题库支持按知识点的智能组卷
5. **可选**：给 App 做签名配置（目前用 debug 签名，发 Release 需要正式 keystore）

## 8. 发布会话照抄（做过的动作）

```powershell
# 1) 合并 + 重建索引 + 校验
python tools/kp_notes_pipeline.py merge
python tools/build_index.py
python tools/validate_bank.py --all
python tools/kp_notes_pipeline.py coverage
# 2) 构建
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
& 'C:\Users\jw\.gradle\wrapper\dists\gradle-9.3.1-all\5wooodlkfwsm42ictrailuf0e\gradle-9.3.1\bin\gradle.bat' -p 'E:\ai coding\pojie\softkao-trainer' assembleDebug --console=plain --offline
# 3) 提交推送
git add -A; git commit -m "..."; git push origin main
git tag -a v2.3.0 -m "..."; git push origin v2.3.0
# 4) 发 Release（带 APK）
gh release create v2.3.0 .\SoftKaoTrainer-v2.3.0-debug.apk --title "..." --notes-file .release-notes.md
```

**注意**：`versionCode`/`versionName` 在 `app/build.gradle.kts`，发版时记得同步改（v2.3.0 = code 4 / name 2.3.0）。

## 9. 常用验证

```bash
python tools/verify_remote.py       # 远端题库/资料完整性与覆盖率
python tools/check_update_channel.py# sha256 MATCH + 字段完整（含精讲通道）
python tools/kp_notes_pipeline.py coverage  # 知识点精讲覆盖率
gh run list --repo r4x2t7/softkao-trainer --limit 3   # CI 是否全绿
```
