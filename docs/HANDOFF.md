# 交接文档 · SoftKao Trainer（软考刷题 App）

> 交接时间：2026-10-25 00:55 ｜ 仓库：https://github.com/r4x2t7/softkao-trainer
> 接手前先读本文件，再读 `docs/REQUIREMENTS-REVIEW.md`（需求评审）与 `docs/QUESTION-PLAN.md`（题库生产计划）。

---

## 1. 一句话现状

一个离线优先的 Android 软考刷题 App：两科题库 **2390 题**（全部带 AI 深度解析）+ **59 章知识点精讲** + 逐知识点精讲（系统集成已写 590 条，多媒体待写）+ **9 个抽象图解动画**，题库托管在 GitHub 可增量更新，仓库 public、CI 全绿、已发版到 v2.1.0。

## 2. 环境（已装好，别重装）

| 工具 | 位置 | 说明 |
|---|---|---|
| JDK 17 | `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` | 构建必需，设 `JAVA_HOME` |
| Git | `C:\Program Files\Git\cmd\git.exe` | 2.56.0 |
| GitHub CLI | `C:\Program Files\GitHub CLI\gh.exe` | 2.103.2，已登录账号 **r4x2t7**（有 repo 权限） |
| Android SDK | `C:\Users\jw\.local\share\android-sdk` | platforms 34/35/36、build-tools 36.0.0 |
| Gradle | 用仓库自带 wrapper（9.3.1 已在缓存） | 首次构建用 `gradle.bat` 生成过 wrapper |

**构建命令**（在 `E:\ai coding\pojie\softkao-trainer` 下）：

```powershell
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew assembleDebug     # 产物：app/build/outputs/apk/debug/app-debug.apk
```

> ⚠️ **用 `gradlew.bat` 可能会卡住**（wrapper 会尝试重新下载 gradle-9.3.1-all 发行包，本机缓存的哈希目录对不上）。
> 稳妥做法：直接用缓存里的发行版
> ```powershell
> & 'C:\Users\jw\.gradle\wrapper\dists\gradle-9.3.1-all\5wooodlkfwsm42ictrailuf0e\gradle-9.3.1\bin\gradle.bat' -p $proj assembleDebug --console=plain --offline
> ```

## 3. 技术栈与关键约定

- Kotlin + Jetpack Compose + Material 3（compose-bom **2025.09.00**），单模块 `:app`
- **AGP 9.1.0 自带 Kotlin 支持**（内置 Kotlin 2.2.10）：
  - ❌ 不要应用 `org.jetbrains.kotlin.android` 插件（会报错）
  - ✅ 只应用 `org.jetbrains.kotlin.plugin.compose`，版本必须与内置 Kotlin 一致（`2.2.10`）
- 持久化用**平台 SQLite**（`AppDatabase`，手写 DAO），刻意不用 Room/KSP，构建更稳
- 网络用平台 `HttpURLConnection`，JSON 用 `org.json`，无第三方网络/序列化依赖
- 依赖注入：`ServiceLocator`（手写单例，ServiceLocator.kt）
- 数据流：ViewModel + `StateFlow` + `collectAsStateWithLifecycle`

## 4. 目录地图

```
app/src/main/java/com/softkao/trainer/
├── MainActivity.kt              # 底部 5 栏导航 + NavHost（路由常量在 Routes 里）
├── SoftKaoApp.kt                # Application + ServiceLocator
├── core/
│   ├── model/                   # Models.kt / Materials.kt / Visuals.kt
│   └── data/
│       ├── SettingsRepository.kt     # 全局科目 currentExamId（已统一，ExamScope 已删除）
│       ├── MaterialRepository.kt     # 章节精讲 + 知识点精讲读取
│       ├── VisualRepository.kt       # 9 个抽象图解（帧动画数据）
│       ├── QuestionBankParser.kt    # 题库解析 + 校验（与 validate_bank.py 规则一致）
│       ├── QuestionBankImporter.kt   # 导入 + sha256（LF 归一化）
│       ├── db/AppDatabase.kt        # 表结构与全部 SQL
│       └── remote/                  # BankUpdater（题库/资料更新通道）、AiExplainer
└── feature/
    ├── home/ practice/ wrong/ stats/ bank/ knowledge/ search/
tools/                           # 题库与资料的流水线脚本（见下）
question-bank/<examId>/           # v1.json / v2.json / materials.json / kp_notes.json / index.json
docs/                             # 需求评审、题库规范、生产计划、调研报告
```

## 5. 题库与资料现状

| 内容 | 状态 |
|---|---|
| 系统集成 v2.json | 1335 题 / 36 章 / 1199 知识点 ✅ |
| 多媒体 v2.json | 1055 题 / 23 章 / 1005 知识点 ✅ |
| AI 深度解析 | 2390 题 **100% 覆盖**（160~280 字，结论→依据→易错点）✅ |
| 章节精讲资料 | 59 章全覆盖（概览/趋势/速记卡/易错/公式/口诀/模板）✅ |
| 逐知识点精讲 | 系统集成 **590 条**（约一半）；多媒体 **0 条** ❌ 待补 |
| 抽象图解动画 | 9 个（关键路径、挣值、变更七步、沟通渠道、质量成本、I/P/B 帧、JPEG 五步、采样量化、色彩模型）✅ |
| 去重 | 2407 → 2390（删 17 道真重复；单选版/多选版不误删）✅ |
| 纠错 | AI 复核标出 19 处疑点，已逐条修订并同步订正 AI 解析文本 ✅ |

### 流水线脚本（都在 `tools/`）

```bash
python tools/validate_bank.py --all        # 校验题库（字段/答案/解析长度/id 唯一）
python tools/merge_banks.py                # 合并分片 → v2.json（含去重与质量门禁）
python tools/build_index.py                # 重建 index.json（含 sha256）+ 同步内置种子
python tools/material_pipeline.py split|merge|status   # 章节精讲资料
python tools/kp_notes_pipeline.py split|merge|status   # 逐知识点精讲
python tools/publish.py                    # 一键：合并解析+资料+索引+校验
python tools/verify_remote.py              # 校验 GitHub 上的内容完整性
python tools/check_update_channel.py       # 端到端验证更新通道
```

**踩过的坑（别再犯）**
- 合并脚本**门禁不过时必须拒绝写出 v2.json**（已改好），否则半成品会被提交、CI 必红
- sha256 必须对 **LF 归一化**后的内容计算：Windows 下 Python 写文件是 CRLF，git 提交是 LF，两边不一致会让 CI 误报
- `python` 在 Windows 控制台打印 ✓/✗ 会 `UnicodeEncodeError`，脚本里统一 `sys.stdout.reconfigure(encoding="utf-8")`
- `.ps1` 脚本里的中文路径字面量会被 PowerShell 按 ANSI 误读 → 脚本只用 ASCII，路径走参数传
- GitHub raw 有传播延迟，校验失败先等几秒再判

## 6. 用户已提出并已修复的问题（v2.1.0 之后又修的，在 v2.2.0 里）

| 用户原话 | 处理 |
|---|---|
| 界面好多文字自己换行了 | 标签类文字统一单行+省略号；标签行改 FlowRow；chip 限宽；底部导航标签 10sp 单行 |
| 题库显示的数字省略了 | StatTile 改 titleMedium + 8dp 内边距 |
| 顺序练习/每日一练换行了 | 按钮 contentPadding 缩小 + 13sp 单行 |
| 错题本按钮换行 | 导航标签改「错题」+ 10sp |
| 按章节/按知识点要横着滑动 | 本来就是 LazyRow，保持；知识点 1200+ 条也是懒加载 |
| 科目选择彻底分开、独立掉 | **删除 ExamScope**，改成全局唯一 `currentExamId`，任何一处切换全站生效；两科数据完全隔离；错题本/搜索去掉「全部科目」 |
| 知识点里有时无法切换科目 | 科目切换固定在知识点页顶栏，滚动也看得见 |
| 题目没有选项导致无法提交 | **281 道判断题没有选项**（题库按 schema 判断题不带 options）→ 刷题页为判断题渲染 √/× 两个可选按钮 |
| 系统集成顺序练习第六题 | 就是 si-0006，判断题，同上已修 |
| 顺序练习只有 200 题、题库 1300+ | 去掉 limit，**全量加载**（1335 题），靠界面分页/跳转 |
| 答题卡小框里塞 200 个按钮 | 答题卡改分批：每批 30 个 + 上一批/下一批 |
| 刷题界面底部要跟页数一样的跳转 | 新增底部跳转栏：⌂首页 / −100 / −10 / 上一页 / 第 x/y 题 / 下一页 / +10 / +100 / 尾页 / 答题卡（横向滚动不换行） |

## 7. 待办（建议按此顺序接手）

1. **补齐多媒体的逐知识点精讲**（`question-bank/multimedia-designer/kp_parts/` 已有 4 个 `.in.json` 待写，每片 ~250 个知识点）
   - 4 个子任务并行，输出 `<chunk>.out.json`（`{"notes":[{"point","note"}]}`，每条 40~70 字，point 必须与输入完全一致）
   - 完成后 `python tools/kp_notes_pipeline.py merge`
2. **补齐系统集成剩余 ~600 个知识点的精讲**（对比 `kp_notes.json` 的 590 条与题库 1199 个知识点，缺的补写）
3. **多写几个抽象图解**（`VisualRepository.kt` 里加 `VisualLesson` 即可，帧动画模型已就绪），可优先做：风险储备与应急响应、配置库与基线、动画补间（形状/动作/颜色）、音频格式选择、冗余与压缩比、生命周期模型选择
4. 让**知识点页也能搜知识点**（现在只能搜题目）
5. **实机验证**：手机装 v2.1.0 后的反馈（尤其是底部跳转栏、答题卡分批、知识点页图解）
6. 可选：错题本加「按知识点聚合的掌握度热力图」；模拟卷加倒计时；AI 解析改为可选调用（现在优先用题库内置解析）

## 8. 发布会话照抄（做过的动作）

```powershell
# 1) 合并 + 重建索引 + 校验
python tools/publish.py
# 2) 构建
$env:JAVA_HOME='C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot'
.\gradlew assembleDebug
# 3) 提交推送
git add -A; git commit -m "..."; git push origin main
git tag -a v2.2.0 -m "..."; git push origin v2.2.0
# 4) 发 Release（带 APK）
gh release create v2.2.0 .\SoftKaoTrainer-v2.2.0-debug.apk --title "..." --notes-file .release-notes.md
```

**注意**：`versionCode`/`versionName` 在 `app/build.gradle.kts`，发版时记得同步改（v2.1.0 = code 3 / name 2.1.0）。

## 9. 常用验证

```bash
python tools/verify_remote.py       # 远端题库/资料完整性与覆盖率
python tools/check_update_channel.py# sha256 MATCH + 字段完整
gh run list --repo r4x2t7/softkao-trainer --limit 3   # CI 是否全绿
```
