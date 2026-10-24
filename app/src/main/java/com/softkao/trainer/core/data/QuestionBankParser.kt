package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.BankVersion
import com.softkao.trainer.core.model.Bookmark
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.Option
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
import com.softkao.trainer.core.model.SubQuestion
import org.json.JSONArray
import org.json.JSONObject

/**
 * 题库 JSON 解析与校验。契约见 docs/QUESTION-BANK-SCHEMA.md。
 * 校验失败的单题只跳过，不中断整批导入。
 */
object QuestionBankParser {

    data class ParsedBank(
        val examId: String,
        val examName: String,
        val version: Int,
        val questions: List<Question>,
        val errors: List<String>,
    )

    fun parse(raw: String): ParsedBank {
        val errors = mutableListOf<String>()
        val root = JSONObject(raw)
        val examId = root.optString("examId")
        val examName = root.optString("examName", examId)
        val version = root.optInt("version", 1)
        val arr = root.optJSONArray("questions") ?: JSONArray()

        val questions = mutableListOf<Question>()
        val seenIds = mutableSetOf<String>()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: run {
                errors += "第 ${i + 1} 题不是合法 JSON 对象，已跳过"
                continue
            }
            try {
                val q = parseQuestion(obj, errors)
                if (q == null) continue
                if (!seenIds.add(q.id)) {
                    errors += "题目 id 重复：${q.id}，已跳过"
                    continue
                }
                questions += q
            } catch (e: Exception) {
                errors += "第 ${i + 1} 题解析失败：${e.message}"
            }
        }
        return ParsedBank(examId, examName, version, questions, errors)
    }

    private fun parseQuestion(obj: JSONObject, errors: MutableList<String>): Question? {
        val id = obj.optString("id").trim()
        if (id.isEmpty()) {
            errors += "缺少 id，已跳过"
            return null
        }
        val examId = obj.optString("examId").trim()
        if (examId.isEmpty()) {
            errors += "题目 $id 缺少 examId，已跳过"
            return null
        }
        val type = QuestionType.fromWire(obj.optString("type"))
        if (type == null) {
            errors += "题目 $id 的 type 非法：${obj.optString("type")}，已跳过"
            return null
        }
        val stem = obj.optString("stem").trim()
        if (stem.isEmpty()) {
            errors += "题目 $id 缺少题干，已跳过"
            return null
        }
        val chapter = obj.optString("chapter").trim()
        val kp = obj.optString("knowledgePoint").trim()
        if (chapter.isEmpty() || kp.isEmpty()) {
            errors += "题目 $id 缺少 chapter 或 knowledgePoint，已跳过"
            return null
        }
        val explanation = obj.optString("explanation").trim()
        if (explanation.length < 20) {
            errors += "题目 $id 的解析过短（${explanation.length} 字），已跳过"
            return null
        }

        val options = mutableListOf<Option>()
        val optArr = obj.optJSONArray("options")
        if (optArr != null) {
            for (j in 0 until optArr.length()) {
                val o = optArr.optJSONObject(j) ?: continue
                options += Option(o.optString("key"), o.optString("text"))
            }
        }

        val answer = mutableListOf<String>()
        val ansArr = obj.optJSONArray("answer")
        if (ansArr != null) {
            for (j in 0 until ansArr.length()) answer += ansArr.optString(j)
        }
        if (answer.isEmpty()) {
            errors += "题目 $id 缺少答案，已跳过"
            return null
        }

        // 题型与答案一致性
        when (type) {
            QuestionType.SINGLE -> if (answer.size != 1) {
                errors += "题目 $id 单选题答案数量为 ${answer.size}，已跳过"
                return null
            }
            QuestionType.MULTI -> if (answer.size < 2) {
                errors += "题目 $id 多选题答案少于 2 个，已跳过"
                return null
            }
            QuestionType.TRUE_FALSE -> if (answer.any { it != "√" && it != "×" }) {
                errors += "题目 $id 判断题答案必须是 √ 或 ×，已跳过"
                return null
            }
            QuestionType.CASE -> Unit
        }

        // 答案 key 必须存在于选项中（判断题除外）
        if (type != QuestionType.TRUE_FALSE && type != QuestionType.CASE) {
            if (options.isEmpty()) {
                errors += "题目 $id 缺少选项，已跳过"
                return null
            }
            val keys = options.map { it.key }.toSet()
            val missing = answer.filter { it !in keys }
            if (missing.isNotEmpty()) {
                errors += "题目 $id 的答案 ${missing.joinToString(",")} 不在选项中，已跳过"
                return null
            }
        }

        val difficulty = obj.optInt("difficulty", 3).coerceIn(1, 5)

        return Question(
            id = id,
            examId = examId,
            subject = obj.optString("subject", ""),
            type = type,
            difficulty = difficulty,
            chapter = chapter,
            knowledgePoint = kp,
            stem = stem,
            options = options,
            answer = answer,
            explanation = explanation,
            aiExplanation = obj.optString("aiExplanation").takeIf { it.isNotBlank() },
            source = obj.optString("source", ""),
            tags = parseStringArray(obj.optJSONArray("tags")),
            caseStudy = obj.optString("caseStudy").takeIf { it.isNotBlank() },
            caseId = obj.optString("caseId").takeIf { it.isNotBlank() },
            subQuestions = parseSubQuestions(obj.optJSONArray("subQuestions")),
        )
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }

    private fun parseSubQuestions(arr: JSONArray?): List<SubQuestion> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            SubQuestion(
                prompt = o.optString("q"),
                referenceAnswer = o.optString("answer").takeIf { it.isNotBlank() },
            )
        }
    }

    /** 供序列化使用：把 Question 还原为题库 JSON 的单题结构 */
    fun toJson(q: Question): JSONObject {
        val o = JSONObject()
        o.put("id", q.id)
        o.put("examId", q.examId)
        o.put("subject", q.subject)
        o.put("type", q.type.wire)
        o.put("difficulty", q.difficulty)
        o.put("chapter", q.chapter)
        o.put("knowledgePoint", q.knowledgePoint)
        o.put("stem", q.stem)
        o.put("options", JSONArray().apply {
            q.options.forEach { opt ->
                put(JSONObject().put("key", opt.key).put("text", opt.text))
            }
        })
        o.put("answer", JSONArray().apply { q.answer.forEach { put(it) } })
        o.put("explanation", q.explanation)
        if (q.aiExplanation != null) o.put("aiExplanation", q.aiExplanation)
        o.put("source", q.source)
        o.put("tags", JSONArray().apply { q.tags.forEach { put(it) } })
        if (q.caseStudy != null) o.put("caseStudy", q.caseStudy)
        if (q.caseId != null) o.put("caseId", q.caseId)
        if (q.subQuestions.isNotEmpty()) {
            o.put("subQuestions", JSONArray().apply {
                q.subQuestions.forEach { sq ->
                    put(JSONObject().put("q", sq.prompt).put("answer", sq.referenceAnswer ?: ""))
                }
            })
        }
        return o
    }
}

/** 索引文件 */
object BankIndexParser {
    data class IndexEntry(
        val examId: String,
        val version: Int,
        val file: String,
        val questionCount: Int,
        val sha256: String,
        /** 逐知识点精讲文件（相对题库根目录），老索引可能没有 */
        val kpNotesFile: String = "",
        /** 逐知识点精讲文件的 sha256 */
        val kpNotesSha256: String = "",
        /** 逐知识点精讲条数 */
        val kpNotesCount: Int = 0,
    )

    fun parse(raw: String): List<IndexEntry> {
        val root = JSONObject(raw)
        val arr = root.optJSONArray("banks") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val notes = o.optJSONObject("kpNotes")
            IndexEntry(
                examId = o.optString("examId"),
                version = o.optInt("version", 1),
                file = o.optString("file"),
                questionCount = o.optInt("questionCount", 0),
                sha256 = o.optString("sha256"),
                kpNotesFile = notes?.optString("file") ?: "",
                kpNotesSha256 = notes?.optString("sha256") ?: "",
                kpNotesCount = notes?.optInt("count", 0) ?: 0,
            )
        }.filter { it.examId.isNotEmpty() && it.file.isNotEmpty() }
    }
}
