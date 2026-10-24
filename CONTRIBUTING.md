# 贡献指南

感谢参与！本项目最缺的就是**好题和好解析**，贡献题目是最有价值的方式。

## 一、贡献题目

1. 认领或新建题目文件：`question-bank/<examId>/vN.json`
   - `system-integration` → 系统集成项目管理工程师
   - `multimedia-designer` → 多媒体应用设计师
2. 字段规范见 [`docs/QUESTION-BANK-SCHEMA.md`](docs/QUESTION-BANK-SCHEMA.md)，**必填字段一个都不能少**
3. 本地校验：

```bash
python tools/validate_bank.py question-bank/system-integration/v1.json
```

4. 提 PR，说明新增题目的考点依据。

### 硬性要求

- ❌ **不得抄录商业机构真题原文**。可以依据公开大纲与教材考点**自编**或**改编**。
- ✅ 每题 `source` 必须写明「改编自…（知识点）」或「自编（依据…）」。
- ✅ `explanation` ≥ 20 字，讲清「为什么」，不能只写「故选 B」。
- ✅ `id` 全局唯一，建议 `<examId>-<四位序号>`；**已发布的 id 不要改**（改了会导致用户端重复入库）。
- ✅ 多选题正确答案 ≥ 2 个；判断题答案只能是 `√` 或 `×`。
- ✅ `answer` 里的选项 key 必须真实存在于 `options`。

### 分布建议

- 两个科目都要覆盖，避免只堆某一章。
- 难度比例大致：简单 30% / 中等 50% / 偏难 20%。
- 每个知识点至少 2 题，便于错题分析形成有效画像。
- 案例分析题用 `caseId` + `caseStudy` + `subQuestions` 组织，小问的参考答案写进 `subQuestions[].answer`。

## 二、改解析

解析写得不好是最大的问题。如果某题解析没讲清，请直接改——这是最有价值的 PR。

## 三、提代码

```bash
git clone https://github.com/<你的用户名>/softkao-trainer.git
cd softkao-trainer
# ... 修改 ...
./gradlew assembleDebug        # 确保能编译
git commit -am "feat: 补充 XX 章节题目"
git push
```

代码风格：Kotlin 官方风格（4 空格缩进），Compose 组件保持无状态，状态提升到 ViewModel。

## 四、代码贡献前自查

- [ ] `./gradlew assembleDebug` 通过
- [ ] 没有引入新的第三方依赖（除非确有必要并说明理由）
- [ ] 题库 JSON 通过校验脚本
- [ ] 没有把任何个人密钥、token 写进代码或题库

## 五、行为准则

友善、就事论事。不接受复制他人受版权保护内容的行为。
