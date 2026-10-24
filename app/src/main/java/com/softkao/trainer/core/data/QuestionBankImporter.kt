package com.softkao.trainer.core.data

import android.content.Context
import com.softkao.trainer.core.data.db.AppDatabase
import com.softkao.trainer.core.model.BankVersion
import com.softkao.trainer.core.model.ImportReport
import com.softkao.trainer.core.model.Question
import java.security.MessageDigest

/** 把解析好的题库写入本地库。用户的作答数据不受影响。 */
class QuestionBankImporter(private val db: AppDatabase) {

    fun import(parsed: QuestionBankParser.ParsedBank, version: Int? = null, now: Long = System.currentTimeMillis()): ImportReport {
        var added = 0
        var updated = 0
        for (q in parsed.questions) {
            val isNew = db.upsertQuestion(q, now)
            if (isNew) added++ else updated++
        }
        db.setBankVersion(
            BankVersion(
                examId = parsed.examId,
                version = version ?: parsed.version,
                updatedAt = now,
                questionCount = db.questionCount(parsed.examId),
            )
        )
        return ImportReport(added = added, updated = updated, skipped = parsed.errors.size, errors = parsed.errors)
    }
}

/** 首次安装时把 assets 里的种子题库灌进本地库 */
class SeedImporter(
    private val context: Context,
    private val db: AppDatabase,
    private val importer: QuestionBankImporter,
) {
    private val seedFiles = listOf(
        "seed/system-integration.json",
        "seed/multimedia-designer.json",
    )

    fun importAll(): List<Pair<String, ImportReport>> {
        val out = mutableListOf<Pair<String, ImportReport>>()
        for (path in seedFiles) {
            val raw = runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            val parsed = QuestionBankParser.parse(raw)
            val report = importer.import(parsed)
            out += parsed.examId to report
        }
        return out
    }

    fun localSeedExists(): Boolean = seedFiles.any {
        runCatching { context.assets.open(it).close(); true }.getOrDefault(false)
    }
}

object Hashing {
    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
