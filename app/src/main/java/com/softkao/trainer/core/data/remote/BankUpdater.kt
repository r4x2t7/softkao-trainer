package com.softkao.trainer.core.data.remote

import com.softkao.trainer.core.data.BankIndexParser
import com.softkao.trainer.core.data.Hashing
import com.softkao.trainer.core.data.MaterialRepository
import com.softkao.trainer.core.data.QuestionBankImporter
import com.softkao.trainer.core.data.QuestionBankParser
import com.softkao.trainer.core.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 题库更新通道：拉取远端 index.json → 对比本地版本 → 下载新版本 → 校验 → 增量导入。
 * 全程不依赖任何第三方 SDK，只用平台 HttpURLConnection。
 */
class BankUpdater(
    private val db: AppDatabase,
    private val importer: QuestionBankImporter,
    private val materials: MaterialRepository? = null,
) {

    data class UpdateResult(
        val ok: Boolean,
        val message: String,
        val reports: List<Pair<String, com.softkao.trainer.core.model.ImportReport>> = emptyList(),
    )

    suspend fun checkAndUpdate(baseUrl: String): UpdateResult = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return@withContext UpdateResult(false, "请先填写题库地址")
        try {
            val indexRaw = get("$base/index.json")
            val entries = BankIndexParser.parse(indexRaw)
            if (entries.isEmpty()) return@withContext UpdateResult(false, "索引为空或格式不正确")

            val reports = mutableListOf<Pair<String, com.softkao.trainer.core.model.ImportReport>>()
            var updatedCount = 0
            var materialCount = 0
            for (entry in entries) {
                val local = db.bankVersion(entry.examId)
                if (local != null && local.version >= entry.version) {
                    // 版本没变也同步一次资料（资料可能单独更新）
                    if (fetchMaterials(base, entry.examId)) materialCount++
                    continue
                }
                val raw = get("$base/${entry.file}")
                if (entry.sha256.isNotBlank() && Hashing.sha256(raw) != entry.sha256.lowercase()) {
                    reports += entry.examId to com.softkao.trainer.core.model.ImportReport(
                        skipped = 1, errors = listOf("${entry.examId}: sha256 校验失败，已拒绝导入")
                    )
                    continue
                }
                val parsed = QuestionBankParser.parse(raw)
                val report = importer.import(parsed, entry.version)
                reports += entry.examId to report
                updatedCount += report.total
                if (fetchMaterials(base, entry.examId)) materialCount++
            }
            val message = buildString {
                if (updatedCount == 0) append("题库已是最新版本") else append("题库更新完成，共新增/更新 $updatedCount 道题")
                if (materialCount > 0) append("，同步 $materialCount 份知识点资料")
            }
            UpdateResult(true, message, reports)
        } catch (e: Exception) {
            UpdateResult(false, "更新失败：${e.message}")
        }
    }

    /** 从本地文件内容导入（不经过网络） */
    fun importFromText(raw: String): UpdateResult {
        return try {
            val parsed = QuestionBankParser.parse(raw)
            val report = importer.import(parsed)
            UpdateResult(true, "已导入 ${report.total} 道题（新增 ${report.added}，更新 ${report.updated}）",
                listOf(parsed.examId to report))
        } catch (e: Exception) {
            UpdateResult(false, "导入失败：${e.message}")
        }
    }

    /** 顺带拉取知识点精讲资料（materials/<examId>.json），失败不影响题库更新 */
    private fun fetchMaterials(base: String, examId: String): Boolean = runCatching {
        val raw = get("$base/materials/$examId.json")
        materials?.saveLocal(examId, raw) ?: false
    }.getOrDefault(false)

    private fun get(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SoftKaoTrainer/0.1 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
