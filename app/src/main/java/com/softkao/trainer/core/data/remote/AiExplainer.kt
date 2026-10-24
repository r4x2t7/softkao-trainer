package com.softkao.trainer.core.data.remote

import com.softkao.trainer.core.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI 深度解析：调用 OpenAI 兼容的 /chat/completions 接口。
 * 未配置接口时返回 null，App 回落到题库自带的静态解析。
 */
class AiExplainer {

    sealed class Result {
        data class Success(val text: String) : Result()
        data class Failure(val message: String) : Result()
        object NotConfigured : Result()
    }

    suspend fun explain(
        baseUrl: String,
        apiKey: String,
        model: String,
        question: Question,
        userAnswer: List<String> = emptyList(),
    ): Result = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) return@withContext Result.NotConfigured
        try {
            val prompt = buildPrompt(question, userAnswer)
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", 0.3)
                put("stream", false)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "你是一名软考辅导老师，讲解要结论先行、给出依据与推导，指出易错点，控制在 200 字以内。")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val url = URL(baseUrl.trimEnd('/') + "/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val text = if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    error("HTTP $code ${err.take(200)}")
                }
                val content = JSONObject(text)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                if (content.isEmpty()) Result.Failure("接口返回内容为空") else Result.Success(content)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.Failure(e.message ?: "请求失败")
        }
    }

    private fun buildPrompt(q: Question, userAnswer: List<String>): String {
        val sb = StringBuilder()
        sb.appendLine("【题目】${q.stem}")
        if (q.options.isNotEmpty()) {
            sb.appendLine("【选项】")
            q.options.forEach { sb.appendLine("${it.key}. ${it.text}") }
        }
        sb.appendLine("【正确答案】${q.answer.joinToString("")}")
        if (userAnswer.isNotEmpty()) sb.appendLine("【用户作答】${userAnswer.joinToString("")}")
        sb.appendLine("【参考答案】${q.explanation}")
        sb.appendLine("请输出：1) 结论；2) 依据与推导；3) 易错点。200 字以内，不要重复题干。")
        return sb.toString()
    }
}
