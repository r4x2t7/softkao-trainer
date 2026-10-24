package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 题库解析与校验规则测试。
 *
 * 这些规则是 App 更新通道的安全底线：坏数据必须被跳过而不是拖垮整批导入，
 * 所以每条校验分支都要有测试兜住。
 */
class QuestionBankParserTest {

    private fun question(
        id: String = "q-1",
        type: String = "single",
        answer: String = "[\"A\"]",
        options: String = "[{\"key\":\"A\",\"text\":\"甲\"},{\"key\":\"B\",\"text\":\"乙\"}]",
        explanation: String = "这是一个长度足够的解析，用来通过二十字下限的校验规则检查。",
        chapter: String = "第一章",
        kp: String = "知识点一",
        stem: String = "题干是什么？",
    ): String = """
        {
          "id": "$id",
          "examId": "system-integration",
          "subject": "基础知识",
          "type": "$type",
          "difficulty": 3,
          "chapter": "$chapter",
          "knowledgePoint": "$kp",
          "stem": "$stem",
          "options": $options,
          "answer": $answer,
          "explanation": "$explanation",
          "source": "自编"
        }
    """.trimIndent()

    private fun bank(vararg questions: String): String =
        """{"examId":"system-integration","examName":"系统集成项目管理工程师","version":2,"questions":[${questions.joinToString(",")}]}"""

    // ------------------------------------------------------------ 正常路径

    @Test
    fun `解析合法单选题`() {
        val parsed = QuestionBankParser.parse(bank(question()))
        assertEquals(1, parsed.questions.size)
        assertTrue(parsed.errors.isEmpty())
        val q = parsed.questions.first()
        assertEquals("q-1", q.id)
        assertEquals(QuestionType.SINGLE, q.type)
        assertEquals(listOf("A"), q.answer)
        assertEquals(2, q.options.size)
        assertEquals("甲", q.options.first().text)
        assertEquals("system-integration", q.examId)
    }

    @Test
    fun `判断题允许没有选项`() {
        val raw = bank(
            question(
                id = "tf-1",
                type = "truefalse",
                answer = "[\"√\"]",
                options = "[]",
            )
        )
        val parsed = QuestionBankParser.parse(raw)
        assertEquals(1, parsed.questions.size)
        assertEquals(QuestionType.TRUE_FALSE, parsed.questions.first().type)
        assertTrue(parsed.questions.first().options.isEmpty())
    }

    @Test
    fun `多选题允许两个以上答案`() {
        val raw = bank(
            question(
                id = "m-1",
                type = "multi",
                answer = "[\"A\",\"B\"]",
            )
        )
        val parsed = QuestionBankParser.parse(raw)
        assertEquals(1, parsed.questions.size)
        assertEquals(listOf("A", "B"), parsed.questions.first().answer)
    }

    @Test
    fun `案例题带小问与参考答案`() {
        val raw = """
            {"examId":"system-integration","version":2,"questions":[{
              "id":"case-1","examId":"system-integration","type":"case","difficulty":3,
              "chapter":"第一章","knowledgePoint":"知识点一","stem":"小问（1）：请分析。",
              "options":[],"answer":["A"],
              "explanation":"这是一个长度足够的解析，用来通过二十字下限的校验规则检查。",
              "source":"自编","caseStudy":"案例背景","caseId":"c1",
              "subQuestions":[{"q":"小问（1）","answer":"参考答案一"},{"q":"小问（2）"}]
            }]}
        """.trimIndent()
        val parsed = QuestionBankParser.parse(raw)
        assertEquals(1, parsed.questions.size)
        val q = parsed.questions.first()
        assertEquals(QuestionType.CASE, q.type)
        assertEquals("案例背景", q.caseStudy)
        assertEquals("c1", q.caseId)
        assertEquals(2, q.subQuestions.size)
        assertEquals("参考答案一", q.subQuestions.first().referenceAnswer)
        // 没有参考答案的小问应为 null，而不是空串
        assertEquals(null, q.subQuestions[1].referenceAnswer)
    }

    // ------------------------------------------------------------ 坏数据必须被跳过

    @Test
    fun `缺少 id 被跳过`() {
        val raw = """{"examId":"x","version":2,"questions":[{"examId":"x","type":"single","chapter":"c","knowledgePoint":"k","stem":"s","answer":["A"],"explanation":"这是一个长度足够的解析，用来通过二十字下限的校验规则检查。"}]}"""
        val parsed = QuestionBankParser.parse(raw)
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("缺少 id") })
    }

    @Test
    fun `解析短于二十字被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(explanation = "太短了")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("解析过短") })
    }

    @Test
    fun `单选题答案数量不为 1 被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(answer = "[\"A\",\"B\"]")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("单选题答案数量") })
    }

    @Test
    fun `多选题答案少于两个被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(type = "multi", answer = "[\"A\"]")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("多选题答案少于") })
    }

    @Test
    fun `判断题答案不是 √ 或 × 被跳过`() {
        val parsed = QuestionBankParser.parse(
            bank(question(id = "tf-2", type = "truefalse", answer = "[\"A\"]", options = "[]"))
        )
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("判断题答案必须是") })
    }

    @Test
    fun `答案不在选项中被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(answer = "[\"C\"]")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("不在选项中") })
    }

    @Test
    fun `选择题缺少选项被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(options = "[]")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("缺少选项") })
    }

    @Test
    fun `缺少答案被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(answer = "[]")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("缺少答案") })
    }

    @Test
    fun `重复 id 只保留第一道并报错`() {
        val parsed = QuestionBankParser.parse(bank(question(id = "dup"), question(id = "dup")))
        assertEquals(1, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("id 重复") })
    }

    @Test
    fun `非法 type 被跳过`() {
        val parsed = QuestionBankParser.parse(bank(question(type = "填空")))
        assertEquals(0, parsed.questions.size)
        assertTrue(parsed.errors.any { it.contains("type 非法") })
    }

    @Test
    fun `坏数据不会中断整批导入`() {
        val parsed = QuestionBankParser.parse(
            bank(
                question(id = "good-1"),
                question(id = "bad-1", explanation = "太短"),
                question(id = "good-2"),
            )
        )
        assertEquals(2, parsed.questions.size)
        assertEquals(listOf("good-1", "good-2"), parsed.questions.map { it.id })
        assertEquals(1, parsed.errors.size)
    }

    @Test
    fun `difficulty 越界时被夹到 1 到 5`() {
        val raw = bank(question().replace("\"difficulty\": 3", "\"difficulty\": 99"))
        val parsed = QuestionBankParser.parse(raw)
        assertEquals(5, parsed.questions.first().difficulty)
    }
}
