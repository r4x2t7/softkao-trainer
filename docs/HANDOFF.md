# 交接文档 · SoftKao Trainer（软考刷题 App）

> 交接时间：2026-10-28（v2.11.0）｜ 仓库：https://github.com/r4x2t7/softkao-trainer
> 接手前先读本文件，再读 `docs/REQUIREMENTS-REVIEW.md`（需求评审）与 `docs/QUESTION-PLAN.md`（题库生产计划）。

---

## 1. 一句话现状

一个离线优先的 Android 软考刷题 App：两科题库 **2565 题**（100% 带 AI 深度解析，含 **152 组 / 314 道案例分析大题**，独立大题模块练习）+ **59 章章节精讲** + **2242 条逐知识点精讲（100% 覆盖）** + **93 张可视化图解**（34 个帧动画 + 59 张离线 HTML 卡片，2026-10-28 完成一轮可读性升级），题库与资料托管在 GitHub 可增量更新，仓库 public、CI 全绿，已发版到 v2.11.0。

## 2. 环境（已装好，别重装）

| 工具 | 位置 | 说明 |
|---|---|---|
| JDK 17 | `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` | 构建必需，设 `JAVA_HOME` |
| Git | `C:\Program Files\Git\cmd\git.exe` | 2.56.0（**不在默认 PATH**，脚本里要写全路径） |
| GitHub CLI | `C:\Program Files\GitHub CLI\gh.exe` | 2.103.2，已登录账号 **r4x2t7**（有 repo 权限） |
| Android SDK | `C:\Users\jw\.local\share\android-sdk` | platforms 34/35/36、build-tools 36.0.0 |
| Gradle | 缓存里的 9.3.1 发行版 | 见下方构建命令 |

**构建命令**（在 `E:\ai coding\softkao-trainer` 下）：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& 'C:\Users\jw\.gradle\wrapper\dists\gradle-9.3.1-all\5wooodlkfwsm42ictrailuf0e\gradle-9.3.1\bin\gradle.bat' -p 'E:\ai coding\softkao-trainer' assembleDebug --console=plain --offline
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
│       ├── VisualRepositoryFourth.kt # 图解第四批（决策树/敏感性/成本类型/CDN/DRM/开发流程）
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

## 5. 测试与自检（不依赖设备）

```bash
./gradlew testDebugUnitTest                # 102 个测试：解析校验 + 纯逻辑 + Compose 界面测试 + 统计数据库行为 + 错题本筛选 + 图解/案例页逐帧出图
python tools/ui_contract_check.py          # UI 契约：文字不截断 / 路由齐全 / 科目作用域统一 / 图解不越界
python tools/kp_notes_pipeline.py coverage # 知识点精讲覆盖率
python tools/kp_notes_lint.py              # 精讲长度体检
python tools/check_ai_consistency.py       # 答案与解析首句一致
python tools/check_visual_bounds.py        # 图解 SVG 内容是否越出 viewBox
python tools/render_visuals.py --size=640x900   # 把 59 张图解渲染成 PNG 做视觉检查
python tools/probe_visual.py <id>          # 给图解注入尺寸探针，量真实布局
```

CI 有两个 job：`validate`（题库 + UI 契约）与 `unit-test`（Gradle 单测 + Compose 界面测试）。

**测试环境的坑**
- Robolectric 会自动创建 `SoftKaoApp` → 它后台线程写 SQLite，Robolectric 的影子不支持跨线程 → 用 `TestApp`（`app/src/test/java/com/softkao/trainer/TestApp.kt`）
- Android 的 `org.json` 是抛异常的桩类，JVM 单测必须额外引 `org.json:json`
- 无头 Chrome 在 Windows 上有最小窗口宽度（~504px），`--screenshot` 给出的是**裁过的图**，别当成真裁切；用 `probe_visual.py` 量 `window.innerWidth` 才准
- 截图前要禁用入场动画（`animation:none`），否则拍到 opacity:0 的中间态，会误判成「元素没渲染」

## 6. 模拟器 UI 走查（真实运行时）

模拟器**可以跑**：VT-x 在 BIOS 里是开着的（`systeminfo` 报 "A hypervisor has been detected"），
Windows 的「虚拟机平台」/WHPX 也开着（`vmcompute` 服务在 Running），只是 SDK 里没装模拟器和系统镜像。

```powershell
$sdk='C:\Users\jw\.local\share\android-sdk'
$env:ANDROID_SDK_ROOT=$sdk; $env:ANDROID_HOME=$sdk
$env:ANDROID_AVD_HOME="$env:USERPROFILE\.android\avd"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --install "emulator" "system-images;android-34;google_apis;x86_64"
& "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n sk34 -k "system-images;android-34;google_apis;x86_64" -d pixel_6 --force
Start-Process "$sdk\emulator\emulator.exe" -ArgumentList @(
  '-avd','sk34','-no-window','-no-audio','-no-boot-anim','-no-snapshot',
  '-gpu','swiftshader_indirect','-accel','on','-port','5554','-memory','2048') -WindowStyle Hidden
```

开机约 15 秒（有硬件加速），然后：

```bash
adb -s emulator-5554 install -r -t app/build/outputs/apk/debug/app-debug.apk
python tools/ui_walkthrough.py      # 19 项断言 + 截图落到 build/ui-shots/
```

`tools/ui_walkthrough.py` **只认 `emulator-` 前缀**，不会碰插着的实体机。

**写走查脚本的坑**
- `uiautomator dump` 只报**屏幕上可见**的节点，页面下方内容要滚动后才看得到
- 不要用文本匹配做导航：首页搜索框提示里也含「知识点」，会跳错页；底部导航栏按坐标点
- 判断题按「√ / ×」匹配，别用「正确」——题干里也有「正确的有哪些」
- `bottomBar` 里的装饰层**不能用 `fillMaxHeight()`**：bottomBar 的高度约束上限是整屏，
  会把底栏撑成全屏高、把题目内容整个挤没（这个坑我踩过一次）

## 7. 题库与资料现状

| 内容 | 状态 |
|---|---|
| 系统集成 v2.json | 1439 题 / 36 章 / 1226 知识点 ✅ |
| 多媒体 v2.json | 1126 题 / 23 章 / 1016 知识点 ✅ |
| AI 深度解析 | 2565 题 **100% 覆盖**（结论→依据→易错点）✅ |
| 案例分析大题 | **152 组 / 314 道小问**（SI 93 组 + MD 59 组），独立模块练习，全部带采分点解析 + AI 深度解析 ✅ |
| 章节精讲资料 | 59 章全覆盖（概览/趋势/速记卡/易错/公式/口诀/模板）✅ |
| 逐知识点精讲 | **2242 条 / 100% 覆盖**（系统集成 1226 + 多媒体 1016）✅ |
| 抽象图解（帧动画） | **34 个** ✅ |
| 结构图解（离线 HTML 卡片） | **59 张**（`assets/visuals/`，2026-10-28 完成可读性模板升级并全部重新生成）✅ |
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

## 8. 用户已提出并已修复的问题

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

### v2.11.0 又做的（选择题题库扩充 +100）

基于上午题（综合知识 75 单选）真题调研（CSDN 50 高频考点、希赛/信管网考点清单、GitHub 真题库 recollection PDF）新增 **100 道选择题**：SI 60 道（信息化/新技术/信息安全/进度成本计算/法规/管理过程）、MD 40 道（色彩/图像音频容量计算/视频制式/压缩编码/流媒体/版权/数据库），全部带解析与 AI 深度解析，知识点复用为主（新增 kp 仅 2 个并补精讲）。`merge_banks` 题干级去重拦下 5 道与旧题撞题的新题，已改写为不同考查角度后全部入库。注意：`publish.py` 不含 `merge_banks.py`，新增分片后必须先跑 `merge_banks.py` 再跑 `publish.py`。

### v2.10.0 又做的（大题模块 + 题库扩充 + 图解升级）

| 事项 | 处理 |
|---|---|
| 案例分析没有独立入口，用户以为「只有选择题」 | 新增**案例分析模块**（`feature/casestudy/CaseStudyScreens.kt`）：列表（案例组/小问/已自评统计 + 搜索）→ 详情（案例背景 → 逐小问「先自己作答再对答案」→ 参考答案 + **AI 深度解析** → 自评「没答上/答上了」）。「没答上」走 `submitCase` 进错题本参与间隔复习。首页 exam 卡片带数量入口；新增 `PracticeMode.CASE` |
| 大题题量不足 | 基于两份真题调研简报（`docs/research/CASE-SI-BRIEF.md` / `CASE-MD-BRIEF.md`，来源见文末）新增 **34 组 / 75 道小问**（SI 20 组：挣值+完工预测、三点估算+关键路径、赶工成本斜率、网络图找茬、资源平滑、变更/配置/质量找茬、索赔计算、评标找茬、EMV 决策树、干系人、沟通渠道、章程启动、收尾、WBS、等保、团队冲突、云方案、需求跟踪；MD 14 组：图像/音频/视频容量计算、JPEG 管线、GOP、冗余、流媒体部署与协议、防盗链+DRM、降噪流程、开发流程找茬、交互设计、色彩管理、版权合规、矢量位图）。数值全部验算自洽 |
| 新知识点没有精讲 | 补 45 条 kp_notes（走 kp_parts 分片），覆盖率保持 100% |
| HTML 图解「看不明白」 | `tools/build_visuals.py` 模板级升级：全局字号放大（标题 17px/正文 13.5px/注释 11.5px）、muted 对比度加深、stage 竖向居中（矮图不再贴顶留大片空白）、`fit_viewbox` 增加 class 字号兜底测量（之前 class 字号一律按 12px 低估会漏扩 viewBox）、flow/compare/layers/formula 等原型尺寸加码（公式 21px、色块加高、注释不再截断）。59 张全部重新生成并逐张渲染验收 |
| 坑：重跑 merge_banks 会冲掉 AI 解析 | `merge_banks.py` 从 parts 重建 v2.json 时**不含 AI 解析**（AI 解析走 `ai_pipeline.py merge` 从 ai_parts 注入），必须跑完整 `publish.py` 流水线。这次还暴露了 ai_parts 里 si-1198 的旧解析（答案C/80万旧规）——已在源头修正 |

### v2.8.0 又修的（错题本上边的按钮点了没反应）

用户原话：「错题本上边的所有按钮都点了没效果，有反馈」

之前错题本上面的 chip（★ 收藏 / 全部 / 未掌握 / 模糊 / 已掌握）点下去**几乎看不到任何变化**，
空状态还只是一片白，连「为什么空」都不说。查下来是两个真 bug 加一个设计问题：

| 问题 | 根因 | 处理 |
|---|---|---|
| 先点「收藏」再点「未掌握」，掌握度筛选被吞掉 | **筛选状态不对称**：`setFilter()` 只改 `filter` 不清 `bookmarkFilter`，而 `setBookmarkFilter()` 会清 `filter`。收藏分支优先级更高 → 点了「未掌握」chip 它亮着，列表纹丝不动 | 筛选状态收敛成 `WrongFilterState(mode, mastery)`，**任何时刻只有一个筛选生效**：选掌握度自动回到错题视图，切收藏自动清掉掌握度 |
| chip 切了但列表没跟着切 | **两处读了不同快照**：摘要文字来自 `collectAsState` 的 `state`（快照读），列表却来自 `viewModel.visibleItems()` 里直接读 `_state.value`（非快照读），两者可能不在同一帧 | 列表改成 `remember(state) { selectWrongItems(...) }`，**列表和摘要永远来自同一份状态** |
| 点了没反馈 | chip 不带数量、筛选后为空页面一模一样、按钮禁用也不说原因 | 见下 |

现在的错题本顶部：

- **chip 上直接带数量**：`全部错题 12` / `未掌握 5` / `模糊 3` / `已掌握 4` / `★ 收藏 2`——点之前就知道会筛出什么
- **用 FlowRow 不用横向滑动**：之前「已掌握」被挤出屏幕外，用户根本点不到
- **当前视图摘要**：`当前：未掌握 · 系统集成 · 5 题`，有筛选时是主色并出现「**清除筛选**」
- **结果为空时说清楚为什么**：区分「还没有收藏的题」「没有『未掌握』的错题」「还没有错题」，
  每种都给下一步（看全部错题 / 去练××题）
- **主按钮禁用也说原因**：没有到期错题时写明「最近一次复习安排在 10-28」或
  「当前筛选下没有到期的错题（该科目共 3 道到期）」
- 顶部三个数字（错题 / 待复习 / 已掌握）改成**按科目范围**统计，不跟着筛选跳

顺带把首屏等 DataStore 的问题也修了：ViewModel 之前只在设置流变化时才刷新，
首屏会空着；现在 `init` 里先 `refresh()` 一次。

> **一个绕不开的约束**：真机和模拟器都在用，不能碰 adb。
> 所以这轮改用 **Robolectric NATIVE 图形模式在 JVM 上真渲染并截 PNG**（`WrongScreenShotTest`），
> 把错题本画出来看排版。抓图方式是 `decorView.draw(Canvas(bitmap))`——
> Robolectric 没有真正的窗口，`captureToImage()` 会一直等 redraw 直到超时。

> **教训**：界面状态要从**同一份快照**派生。
> 一处读 `state`、另一处读 `_state.value`，在重组时序不对的那一帧就会对不上，
> 表现出来就是「按钮变了、列表没变」——用户只会说「点了没反应」。

### v2.7.0 又修的（统计页图表重做）

用户原话：「这个统计图也太垃圾了，怎么一上一下的？你能不能用点好点的东西做？做点花里胡哨的炫酷的」

旧图表是用 `Row` + `Box(height=...)` 拼的柱状图，用户截图里点名了四个毛病：

| 问题 | 处理 |
|---|---|
| X 轴日期折行成「10-2」「0」 | 日期改用 `TextMeasurer` **直接画在 Canvas 上**，根本没有换行这个概念；文字宽度自己算，永远单行 |
| 0 值被画成台座状短条，很丑 | **0 值不画柱**，只在基线上留一个空心圆点 + 一个淡「0」 |
| 最后一根柱子被卡片底边切掉 | 整个图表改成 `Canvas` 绝对定位绘制，柱高按坐标算，不可能被裁 |
| 无渐变、无基线、无网格线、样式单调 | 见下 |

新图表 `app/src/main/java/com/softkao/trainer/feature/stats/TrendChart.kt`（全新文件）：

- **柱体**：紫 `#8B5CF6` → 青 `#22D3EE` 竖向渐变，顶部带光晕；柱内嵌一段薄荷绿 = 当天答对题数
- **趋势线**：穿过柱顶的琥珀色发光平滑曲线（`PathMeasure` 按进度截取，自带「画出来」动画），带节点
- **网格**：4 条虚线 + 一条实线基线；Y 轴刻度取整（`niceMax` 永远是 4 的倍数，四等分落在整数上，不会出现 7.5 这种刻度）
- **入场动画**：柱子错开 0.5/n 依次生长；最后一根有流光扫过
- **点柱子看明细**：点击弹出气泡（日期 / 做题量 / 答对数 / 正确率），带尖角指向柱子
- **图例**：做题量 / 答对数 / 趋势 三色说明
- 顶部四个统计块也改成 `GradientStatTile`（渐变底 + 色条 + 彩色数值），跟图表统一风格

> **顺带修掉一个真 bug**：统计页「连续 N 天」**永远是 0**。
> `AppDatabase.streakDays()` 里 SQL 取的是 `date(...)` → `'2026-10-25'`，
> 代码却拿 `SimpleDateFormat("yyyyMMdd")` → `'20261025'` 去比对，两边永远对不上。
> 改成日期键统一在 Kotlin 侧算（`dayKey()`），不依赖 SQL 日期函数。
> 新增 `StatsDbTest` 7 个用例锁住这个行为。

走查脚本同步加固：

- 首页原来死等 4 秒就截图，刚装完 APK 时会拍到一片空白 → 改成**等「今日」真的出现**再断言
- 新增 `bar_ratio()`：图表是 Canvas 画的，语义树里看不到柱子，只能靠像素确认柱体色真的画出来了

> **教训**：Canvas 自绘的东西，`uiautomator dump` 里一个节点都没有。
> 想断言「真的画出来了」，只能靠像素——这也是为什么图解库要加像素级断言。

### v2.6.0 又修的（按用户反馈重做交互）

用户原话：「首页的按章节和按知识点横着滑动有超过 1000 个选项，符合正常交互逻辑吗」「答题卡底栏还需要滑动，就不能做稍微高一点全部显示了吗」「模拟卷应该答完题才能看到答案和解析」「错题本后边有个（0）」「知识点章节又是横着滑动」「打开图解库也是横着滑动，点看之后看不到图解」「抽象图解基本都体验不出来知识点」「错题啥的都是 0 点不了开始复习，底下显示有几个错题了」「设置里关于描述一下作者加 AI」

| 问题 | 处理 |
|---|---|
| 1198 个知识点横着滑 | 首页改成「按章节（36）/ 按知识点（1198）」按钮 → 独立**选题页**：纵向列表 + 知识点搜索框 |
| 章节/知识点清单/图解库全是横滑 | **全部改纵向列表** |
| 跳转栏 10 个入口挤一行，「答题卡」「尾页」在屏幕外 | **分两行**，一屏全显示 |
| 模拟卷当场就给答案解析 | 提交只记录答案不给解析；全部答完点「交卷查看结果」再逐题回看 |
| 图解点开**整片空白**（真 bug） | HTML 里 `.stage` 靠 `height:100%` 撑高，WebView 里 flex 分不到确定高度就塌成 0 → 改 `min-height` + SVG `height:auto`；走查加**像素级断言**（图形区非背景占比 93.2%） |
| 抽象图解看不出知识点 | 加考点标签；**要点前置到图上面**；每帧说明加大 |
| 错题本 (0) + 空状态三个 0 + 按钮点不了 | 首页按钮只在有待复习时显示数量；空状态改成一句话引导 |
| 「本页独立选择科目」过期文案 | 改成「与全站科目同步」 |
| 设置页「关于」没写作者 | 补作者（霁伪）+ AI 协助说明 |
| 练习页标题显示成「章节+·+项目成本管理」 | `URLEncoder` 把空格编成 `+`，Navigation 用 `Uri.decode` 不还原 → 全改 `Uri.encode` |

> **这一轮的核心教训**：横滑只适合「少量、内容丰富」的卡片（每张都值得看一眼），
> 不适合「上千个同质条目」（章节名、知识点名、图解标题）。后者必须纵向列表 + 搜索。

### v2.5.0 又修的（模拟器走查测出来的）

| 问题 | 处理 |
|---|---|
| **在答题卡里点题号后，弹窗不关**，一直盖在题目上 | 点题号时同时 `onDismiss()` |
| **「答题卡」入口默认在屏幕外**（跳转栏横向滚动，它排最后） | 提到跳转栏第二位，一进页面就能看到；后面的 ±100/±10 等仍可右滑 |
| 章节卡片信息太单薄、页面下方大片留白 | 卡片补「N 个知识点」和「练习本章 ›」入口行，信息更完整、卡片更饱满 |
| 底部导航栏按文本匹配会跳错页（首页搜索框提示里也含「知识点」） | 走查脚本改按底部导航坐标点击；App 本身无此问题 |

> 顺带记一个我自己踩的坑：给跳转栏加「右缘渐隐」装饰时用了 `fillMaxHeight()`，
> 而 `bottomBar` 的高度约束上限是整屏 —— 底栏被撑成全屏高，**题干和选项全被挤没了**。
> 模拟器截图一眼就看出来了，单测和静态检查都发现不了。

### v2.4.1 又修的（测试驱动）

| 问题 | 处理 |
|---|---|
| OSI 分层图标题与副标题叠字 | `r_layers` 原来把副标题放在色条内、比标题只低 6px，必然叠字；改为放到色条下方并按 32 字折行 |
| CPM 网络图「活动 E」与「完成」两个节点重叠 | 7 列节点用固定 66px 宽在 360px 画布里根本排不下；改为节点宽度按列数自适应，并把边标签移到边的法线方向 |
| 干系人矩阵格子文字溢出、和邻格叠在一起 | 折行宽度改为按格子实际宽度算（原来固定 11 字），并限制在格子高度内；轴标题左移避让 |
| 118 张图解有内容画到 viewBox 之外 | 生成器加一步「按内容实际范围重算 viewBox」（只扩不缩），越界数 118 → 0 |
| HomeScreen 还有 3 处省略号截断（章节 chip / 知识点 chip / 小标题） | 改成最多两行自然换行 |

### v2.3.0 又修的

| 问题 | 处理 |
|---|---|
| 切科目后别的页面还停在旧科目 | 各页原来用 `currentExamId.first()` 只取一次快照 → 统一改成订阅 Flow（`rememberCurrentExamId()` / ViewModel 里 `collect`），**任何一处切换全站立刻同步**；顺手删掉「本页独立选择」这类过期文案 |
| 长文字被省略号截断 | 去掉内容型文本上的 `maxLines=1` 与 `TextOverflow.Ellipsis`，改成自然换行完整显示 |
| 知识点精讲更新不了 | 逐知识点精讲原来只读内置 assets → **接入更新通道**：`index.json` 增加 `kpNotes` 字段（file/count/sha256），App 拉取后校验 sha256 再落盘；章节精讲与知识点精讲独立于题库版本同步 |
| 知识点精讲只有一半 | 补齐到 **2195 条 100% 覆盖**；CI 增加覆盖率门禁（`kp_notes_pipeline.py coverage`，缺一条就红） |
| 图解不够 | 从 9 个补到 **28 个** |

## 9. 待办（建议按此顺序接手）

1. **实机验证**：装 v2.10.0 后重点看——案例分析模块的自评与错题联动、HTML 图解新版观感是否够清楚（尤其深色模式）、首页大题入口数量
2. **图解内容级精修**：模板升级已解决字号/截断/留白；部分卡片仍有「图不对题」空间，最典型是 `si-osi-layers`（标题说 OSI 七层 ↔ TCP/IP 四层映射，图里只画了四层，七层的映射关系靠注释文字）——值得按 archetype 逐类做第二轮数据精修
3. **可选增强**：错题本按知识点做「掌握度热力图」；模拟卷支持自定义题量与时长；案例分析支持「整组连做」模式（一次练完整案例组的全部小问）
4. **可选**：给 App 做签名配置（目前用 debug 签名，发 Release 需要正式 keystore）
5. **注意**：`trae-temp_20261014_01/` 是用户要求复制进来的 Trae 项目快照，实际为空目录（0 个文件），确认无用后可删除

## 10. 发布会话照抄（做过的动作）

```powershell
# 1) 合并 + 重建索引 + 校验
python tools/kp_notes_pipeline.py merge
python tools/build_index.py
python tools/validate_bank.py --all
python tools/kp_notes_pipeline.py coverage
# 2) 构建
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
& 'C:\Users\jw\.gradle\wrapper\dists\gradle-9.3.1-all\5wooodlkfwsm42ictrailuf0e\gradle-9.3.1\bin\gradle.bat' -p 'E:\ai coding\softkao-trainer' assembleDebug --console=plain --offline
# 3) 提交推送
git add -A; git commit -m "..."; git push origin main
git tag -a v2.3.0 -m "..."; git push origin v2.3.0
# 4) 发 Release（带 APK）
gh release create v2.3.0 .\SoftKaoTrainer-v2.3.0-debug.apk --title "..." --notes-file .release-notes.md
```

**注意**：`versionCode`/`versionName` 在 `app/build.gradle.kts`，发版时记得同步改（v2.3.0 = code 4 / name 2.3.0）。

## 11. 常用验证

```bash
python tools/verify_remote.py       # 远端题库/资料完整性与覆盖率
python tools/check_update_channel.py# sha256 MATCH + 字段完整（含精讲通道）
python tools/kp_notes_pipeline.py coverage  # 知识点精讲覆盖率
gh run list --repo r4x2t7/softkao-trainer --limit 3   # CI 是否全绿
```
