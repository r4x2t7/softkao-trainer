package com.softkao.trainer.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 真实题库的回归测试。
 *
 * 用 App 自己的解析器去读仓库里实际发布的题库文件：
 * 只要发布出去的题库有一条不满足 App 的校验规则（比如答案不在选项中），
 * 用户更新时那一条就会被静默丢弃——这里先把它拦住。
 */
class ShippedBankTest {

    private fun bankFile(exam: String): File {
        val candidates = listOf(
            File("../question-bank/$exam/v2.json"),
            File("question-bank/$exam/v2.json"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("找不到题库文件：${candidates.joinToString { it.path }}")
    }

    @Test
    fun `系统集成题库能被 App 解析器完整解析且零错误`() {
        val file = bankFile("system-integration")
        val parsed = QuestionBankParser.parse(file.readText(Charsets.UTF_8))
        assertTrue(
            "解析报错：${parsed.errors.take(5)}",
            parsed.errors.isEmpty(),
        )
        assertEquals(1379, parsed.questions.size)
    }

    @Test
    fun `多媒体题库能被 App 解析器完整解析且零错误`() {
        val file = bankFile("multimedia-designer")
        val parsed = QuestionBankParser.parse(file.readText(Charsets.UTF_8))
        assertTrue(
            "解析报错：${parsed.errors.take(5)}",
            parsed.errors.isEmpty(),
        )
        assertEquals(1086, parsed.questions.size)
    }

    @Test
    fun `题库里的题目 id 唯一`() {
        for (exam in listOf("system-integration", "multimedia-designer")) {
            val parsed = QuestionBankParser.parse(bankFile(exam).readText(Charsets.UTF_8))
            val ids = parsed.questions.map { it.id }
            assertEquals("id 重复：$exam", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `每道题都有来源标注`() {
        for (exam in listOf("system-integration", "multimedia-designer")) {
            val parsed = QuestionBankParser.parse(bankFile(exam).readText(Charsets.UTF_8))
            val missing = parsed.questions.filter { it.source.isBlank() }
            assertTrue("$exam 有 ${missing.size} 道题缺 source", missing.isEmpty())
        }
    }

    @Test
    fun `每道题都有 AI 深度解析`() {
        for (exam in listOf("system-integration", "multimedia-designer")) {
            val parsed = QuestionBankParser.parse(bankFile(exam).readText(Charsets.UTF_8))
            val missing = parsed.questions.filter { it.aiExplanation.isNullOrBlank() }
            assertTrue("$exam 有 ${missing.size} 道题缺 aiExplanation", missing.isEmpty())
        }
    }

    @Test
    fun `索引里的 sha256 与题库文件实际哈希一致`() {
        val indexFile = listOf(
            File("../question-bank/index.json"),
            File("question-bank/index.json"),
        ).firstOrNull { it.exists() } ?: error("找不到 index.json")
        val index = org.json.JSONObject(indexFile.readText(Charsets.UTF_8))
        val banks = index.getJSONArray("banks")
        for (i in 0 until banks.length()) {
            val b = banks.getJSONObject(i)
            val bankFile = listOf(
                File("../question-bank/${b.getString("file")}"),
                File("question-bank/${b.getString("file")}"),
            ).firstOrNull { it.exists() } ?: error("找不到 ${b.getString("file")}")
            assertEquals(
                "sha256 不匹配：${b.getString("examId")}",
                b.getString("sha256"),
                Hashing.sha256(bankFile.readText(Charsets.UTF_8)),
            )
        }
    }
}
