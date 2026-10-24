package com.softkao.trainer.core.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.softkao.trainer.core.model.BankVersion
import com.softkao.trainer.core.model.Bookmark
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.Option
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
import com.softkao.trainer.core.model.SubQuestion
import com.softkao.trainer.core.model.WrongEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地离线数据库。刻意使用平台自带 SQLite，避免引入注解处理器带来的构建复杂度；
 * 数据访问统一收敛在 Repository 层，后续要换 Room 只需替换本类实现。
 */
class AppDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE questions(
              id TEXT PRIMARY KEY,
              exam_id TEXT NOT NULL,
              subject TEXT,
              type TEXT NOT NULL,
              difficulty INTEGER,
              chapter TEXT,
              knowledge_point TEXT,
              stem TEXT,
              options_json TEXT,
              answer_json TEXT,
              explanation TEXT,
              ai_explanation TEXT,
              source TEXT,
              tags_json TEXT,
              case_study TEXT,
              case_id TEXT,
              sub_questions_json TEXT,
              updated_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_questions_exam ON questions(exam_id)")
        db.execSQL("CREATE INDEX idx_questions_chapter ON questions(exam_id, chapter)")
        db.execSQL("CREATE INDEX idx_questions_kp ON questions(exam_id, knowledge_point)")
        db.execSQL(
            """
            CREATE TABLE attempts(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              question_id TEXT NOT NULL,
              exam_id TEXT,
              mode TEXT,
              selected_json TEXT,
              correct INTEGER,
              duration_ms INTEGER,
              answered_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_attempts_question ON attempts(question_id)")
        db.execSQL("CREATE INDEX idx_attempts_time ON attempts(answered_at)")
        db.execSQL(
            """
            CREATE TABLE wrong_entries(
              question_id TEXT PRIMARY KEY,
              exam_id TEXT,
              wrong_count INTEGER,
              streak INTEGER,
              mastery TEXT,
              last_wrong_at INTEGER,
              next_review_at INTEGER,
              note TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE bookmarks(
              question_id TEXT PRIMARY KEY,
              exam_id TEXT,
              created_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE banks(
              exam_id TEXT PRIMARY KEY,
              version INTEGER,
              updated_at INTEGER,
              question_count INTEGER
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 题库内容可随时重建，学习数据不可丢：这里只做增量迁移，不做 destructive 降级
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE questions ADD COLUMN updated_at INTEGER")
        }
    }

    // ---------------------------------------------------------------- questions

    /** 写入或更新题目；返回 true 表示新增 */
    fun upsertQuestion(q: Question, now: Long): Boolean {
        val db = writableDatabase
        val exists = db.rawQuery("SELECT 1 FROM questions WHERE id=?", arrayOf(q.id)).use { it.moveToFirst() }
        val values = ContentValues().apply {
            put("id", q.id)
            put("exam_id", q.examId)
            put("subject", q.subject)
            put("type", q.type.wire)
            put("difficulty", q.difficulty)
            put("chapter", q.chapter)
            put("knowledge_point", q.knowledgePoint)
            put("stem", q.stem)
            put("options_json", JSONArray().apply { q.options.forEach { put(JSONObject().put("key", it.key).put("text", it.text)) } }.toString())
            put("answer_json", JSONArray().apply { q.answer.forEach { put(it) } }.toString())
            put("explanation", q.explanation)
            put("ai_explanation", q.aiExplanation)
            put("source", q.source)
            put("tags_json", JSONArray().apply { q.tags.forEach { put(it) } }.toString())
            put("case_study", q.caseStudy)
            put("case_id", q.caseId)
            put("sub_questions_json", JSONArray().apply {
                q.subQuestions.forEach { sq ->
                    put(JSONObject().put("q", sq.prompt).put("answer", sq.referenceAnswer ?: ""))
                }
            }.toString())
            put("updated_at", now)
        }
        val id = db.insertWithOnConflict("questions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return id != -1L && !exists
    }

    fun questionCount(examId: String? = null): Int {
        val db = readableDatabase
        return if (examId == null) {
            db.rawQuery("SELECT COUNT(*) FROM questions", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } else {
            db.rawQuery("SELECT COUNT(*) FROM questions WHERE exam_id=?", arrayOf(examId)).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        }
    }

    fun questions(
        examId: String? = null,
        chapter: String? = null,
        knowledgePoint: String? = null,
        subject: String? = null,
        ids: List<String>? = null,
        limit: Int? = null,
        offset: Int = 0,
    ): List<Question> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (examId != null) { where += "exam_id=?"; args += examId }
        if (chapter != null) { where += "chapter=?"; args += chapter }
        if (knowledgePoint != null) { where += "knowledge_point=?"; args += knowledgePoint }
        if (subject != null) { where += "subject=?"; args += subject }
        if (ids != null) {
            if (ids.isEmpty()) return emptyList()
            where += "id IN (${ids.joinToString(",") { "?" }})"
            args += ids
        }
        val sql = buildString {
            append("SELECT * FROM questions")
            if (where.isNotEmpty()) append(" WHERE ").append(where.joinToString(" AND "))
            append(" ORDER BY id")
            if (limit != null) { append(" LIMIT ?"); args += limit.toString() }
            if (offset > 0) { append(" OFFSET ?"); args += offset.toString() }
        }
        return readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            val list = mutableListOf<Question>()
            while (c.moveToNext()) list += c.toQuestion()
            list
        }
    }

    fun questionById(id: String): Question? =
        readableDatabase.rawQuery("SELECT * FROM questions WHERE id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toQuestion() else null
        }

    fun chapters(examId: String): List<String> =
        readableDatabase.rawQuery(
            "SELECT DISTINCT chapter FROM questions WHERE exam_id=? ORDER BY chapter",
            arrayOf(examId)
        ).use { c ->
            val list = mutableListOf<String>()
            while (c.moveToNext()) list += c.getString(0)
            list
        }

    fun knowledgePoints(examId: String, chapter: String? = null): List<String> =
        if (chapter == null) {
            readableDatabase.rawQuery(
                "SELECT DISTINCT knowledge_point FROM questions WHERE exam_id=? ORDER BY knowledge_point",
                arrayOf(examId)
            ).use { c -> val l = mutableListOf<String>(); while (c.moveToNext()) l += c.getString(0); l }
        } else {
            readableDatabase.rawQuery(
                "SELECT DISTINCT knowledge_point FROM questions WHERE exam_id=? AND chapter=? ORDER BY knowledge_point",
                arrayOf(examId, chapter)
            ).use { c -> val l = mutableListOf<String>(); while (c.moveToNext()) l += c.getString(0); l }
        }

    fun chapterCounts(examId: String): Map<String, Int> =
        readableDatabase.rawQuery(
            "SELECT chapter, COUNT(*) FROM questions WHERE exam_id=? GROUP BY chapter",
            arrayOf(examId)
        ).use { c ->
            val m = linkedMapOf<String, Int>()
            while (c.moveToNext()) m[c.getString(0)] = c.getInt(1)
            m
        }

    /** 每章的题量 / 作答数 / 正确数（用于知识点模块的掌握度） */
    fun chapterStats(examId: String): Map<String, Triple<Int, Int, Int>> =
        readableDatabase.rawQuery(
            """
            SELECT q.chapter,
                   (SELECT COUNT(*) FROM questions q2 WHERE q2.exam_id=q.exam_id AND q2.chapter=q.chapter) AS total,
                   COUNT(a.id) AS answered,
                   SUM(CASE WHEN a.correct=1 THEN 1 ELSE 0 END) AS correct
            FROM questions q
            LEFT JOIN attempts a ON a.question_id = q.id
            WHERE q.exam_id = ?
            GROUP BY q.chapter
            """.trimIndent(),
            arrayOf(examId)
        ).use { c ->
            val m = linkedMapOf<String, Triple<Int, Int, Int>>()
            while (c.moveToNext()) {
                m[c.getString(0)] = Triple(c.getInt(1), c.getInt(2), c.getInt(3))
            }
            m
        }

    /** 章节 → 该章的知识点 → 题量 */
    fun knowledgePointsByChapter(examId: String): Map<String, Map<String, Int>> =
        readableDatabase.rawQuery(
            "SELECT chapter, knowledge_point, COUNT(*) FROM questions WHERE exam_id=? GROUP BY chapter, knowledge_point",
            arrayOf(examId)
        ).use { c ->
            val out = linkedMapOf<String, MutableMap<String, Int>>()
            while (c.moveToNext()) {
                val chapter = c.getString(0)
                val point = c.getString(1)
                val count = c.getInt(2)
                out.getOrPut(chapter) { linkedMapOf() }[point] = count
            }
            out
        }

    /** 每个知识点的题量 */
    fun knowledgePointCounts(examId: String): Map<String, Int> =        readableDatabase.rawQuery(
            "SELECT knowledge_point, COUNT(*) FROM questions WHERE exam_id=? GROUP BY knowledge_point ORDER BY COUNT(*) DESC",
            arrayOf(examId)
        ).use { c ->
            val m = linkedMapOf<String, Int>()
            while (c.moveToNext()) m[c.getString(0)] = c.getInt(1)
            m
        }

    /** 关键词搜索：题干 / 解析 / 知识点 / 章节 */
    fun search(keyword: String, examId: String? = null, limit: Int = 60): List<Question> {
        val kw = keyword.trim()
        if (kw.isEmpty()) return emptyList()
        val where = mutableListOf(
            "(stem LIKE ? OR explanation LIKE ? OR knowledge_point LIKE ? OR chapter LIKE ?)"
        )
        val args = mutableListOf<String>()
        repeat(4) { args += "%$kw%" }
        if (examId != null) {
            where += "exam_id=?"
            args += examId
        }
        val sql = "SELECT * FROM questions WHERE ${where.joinToString(" AND ")} ORDER BY exam_id, chapter, id LIMIT ?"
        args += limit.toString()
        return readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            val list = mutableListOf<Question>()
            while (c.moveToNext()) list += c.toQuestion()
            list
        }
    }

    // ---------------------------------------------------------------- attempts

    fun insertAttempt(questionId: String, examId: String, mode: PracticeMode, selected: List<String>, correct: Boolean, durationMs: Long, at: Long) {
        writableDatabase.insert("attempts", null, ContentValues().apply {
            put("question_id", questionId)
            put("exam_id", examId)
            put("mode", mode.name)
            put("selected_json", JSONArray().apply { selected.forEach { put(it) } }.toString())
            put("correct", if (correct) 1 else 0)
            put("duration_ms", durationMs)
            put("answered_at", at)
        })
    }

    fun attemptCount(examId: String? = null): Int {
        val db = readableDatabase
        return if (examId == null) {
            db.rawQuery("SELECT COUNT(*) FROM attempts", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } else {
            db.rawQuery("SELECT COUNT(*) FROM attempts WHERE exam_id=?", arrayOf(examId)).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        }
    }

    fun correctCount(examId: String? = null): Int {
        val db = readableDatabase
        return if (examId == null) {
            db.rawQuery("SELECT COUNT(*) FROM attempts WHERE correct=1", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } else {
            db.rawQuery("SELECT COUNT(*) FROM attempts WHERE correct=1 AND exam_id=?", arrayOf(examId)).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        }
    }

    /** 今日作答数 */
    fun attemptsSince(since: Long, examId: String? = null): Int {
        val db = readableDatabase
        return if (examId == null) {
            db.rawQuery("SELECT COUNT(*) FROM attempts WHERE answered_at>=?", arrayOf(since.toString())).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        } else {
            db.rawQuery("SELECT COUNT(*) FROM attempts WHERE answered_at>=? AND exam_id=?", arrayOf(since.toString(), examId)).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        }
    }

    /** 连续打卡天数（按本地日期去重） */
    fun streakDays(): Int {
        val db = readableDatabase
        val days = mutableSetOf<Long>()
        db.rawQuery("SELECT DISTINCT date(answered_at/1000, 'unixepoch', 'localtime') FROM attempts ORDER BY 1 DESC", null).use { c ->
            while (c.moveToNext()) days += c.getString(0).toLongOrNull() ?: 0L
        }
        if (days.isEmpty()) return 0
        val tz = java.util.TimeZone.getDefault()
        val today = java.util.Calendar.getInstance(tz).let {
            it.set(java.util.Calendar.HOUR_OF_DAY, 0); it.set(java.util.Calendar.MINUTE, 0)
            it.set(java.util.Calendar.SECOND, 0); it.set(java.util.Calendar.MILLISECOND, 0)
            it.timeInMillis
        }
        val dayMs = 24 * 3600 * 1000L
        var streak = 0
        var cursor = today
        while (true) {
            val local = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.CHINA).format(java.util.Date(cursor))
            if (days.contains(local.toLong())) {
                streak++
                cursor -= dayMs
            } else {
                // 今天还没做题不中断连续（允许今天未开始）
                if (streak == 0 && cursor == today) {
                    cursor -= dayMs
                    continue
                }
                break
            }
        }
        return streak
    }

    /** 最近 N 天的每日作答数与正确数 */
    fun dailyTrend(days: Int = 7): List<Pair<String, Pair<Int, Int>>> {
        val db = readableDatabase
        val result = LinkedHashMap<String, Pair<Int, Int>>()
        val cal = java.util.Calendar.getInstance()
        for (i in days - 1 downTo 0) {
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.DAY_OF_YEAR, -i)
            c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
            c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
            val start = c.timeInMillis
            val end = start + 24 * 3600 * 1000L
            val key = java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA).format(java.util.Date(start))
            val total = db.rawQuery("SELECT COUNT(*) FROM attempts WHERE answered_at>=? AND answered_at<?", arrayOf(start.toString(), end.toString()))
                .use { if (it.moveToFirst()) it.getInt(0) else 0 }
            val ok = db.rawQuery("SELECT COUNT(*) FROM attempts WHERE answered_at>=? AND answered_at<? AND correct=1", arrayOf(start.toString(), end.toString()))
                .use { if (it.moveToFirst()) it.getInt(0) else 0 }
            result[key] = total to ok
        }
        return result.toList()
    }

    /** 按知识点统计：作答数 / 正确数 */
    fun knowledgeStats(examId: String): Map<String, Pair<Int, Int>> {
        val db = readableDatabase
        val map = linkedMapOf<String, Pair<Int, Int>>()
        db.rawQuery(
            """
            SELECT q.knowledge_point, COUNT(a.id), SUM(CASE WHEN a.correct=1 THEN 1 ELSE 0 END)
            FROM attempts a JOIN questions q ON q.id = a.question_id
            WHERE a.exam_id = ?
            GROUP BY q.knowledge_point
            """.trimIndent(),
            arrayOf(examId)
        ).use { c ->
            while (c.moveToNext()) map[c.getString(0)] = c.getInt(1) to c.getInt(2)
        }
        return map
    }

    // ---------------------------------------------------------------- wrong entries

    fun wrongEntry(questionId: String): WrongEntry? =
        readableDatabase.rawQuery("SELECT * FROM wrong_entries WHERE question_id=?", arrayOf(questionId)).use { c ->
            if (c.moveToFirst()) c.toWrongEntry() else null
        }

    fun wrongEntries(examId: String? = null): List<WrongEntry> {
        val db = readableDatabase
        val sql = if (examId == null) "SELECT * FROM wrong_entries" else "SELECT * FROM wrong_entries WHERE exam_id=?"
        val args = if (examId == null) null else arrayOf(examId)
        return db.rawQuery(sql, args).use { c ->
            val l = mutableListOf<WrongEntry>()
            while (c.moveToNext()) l += c.toWrongEntry()
            l
        }
    }

    fun upsertWrongEntry(entry: WrongEntry) {
        writableDatabase.insertWithOnConflict("wrong_entries", null, ContentValues().apply {
            put("question_id", entry.questionId)
            put("exam_id", entry.examId)
            put("wrong_count", entry.wrongCount)
            put("streak", entry.streak)
            put("mastery", entry.mastery.name)
            put("last_wrong_at", entry.lastWrongAt)
            put("next_review_at", entry.nextReviewAt)
            put("note", entry.note)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteWrongEntry(questionId: String) {
        writableDatabase.delete("wrong_entries", "question_id=?", arrayOf(questionId))
    }

    fun updateWrongNote(questionId: String, note: String?) {
        writableDatabase.update("wrong_entries", ContentValues().apply { put("note", note) }, "question_id=?", arrayOf(questionId))
    }

    // ---------------------------------------------------------------- bookmarks

    fun isBookmarked(questionId: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM bookmarks WHERE question_id=?", arrayOf(questionId)).use { it.moveToFirst() }

    fun toggleBookmark(questionId: String, examId: String, now: Long): Boolean {
        val db = writableDatabase
        return if (isBookmarked(questionId)) {
            db.delete("bookmarks", "question_id=?", arrayOf(questionId))
            false
        } else {
            db.insert("bookmarks", null, ContentValues().apply {
                put("question_id", questionId); put("exam_id", examId); put("created_at", now)
            })
            true
        }
    }

    fun bookmarkedIds(examId: String? = null): List<String> {
        val db = readableDatabase
        val sql = if (examId == null) "SELECT question_id FROM bookmarks" else "SELECT question_id FROM bookmarks WHERE exam_id=?"
        val args = if (examId == null) null else arrayOf(examId)
        return db.rawQuery(sql, args).use { c ->
            val l = mutableListOf<String>()
            while (c.moveToNext()) l += c.getString(0)
            l
        }
    }

    // ---------------------------------------------------------------- banks

    fun bankVersion(examId: String): BankVersion? =
        readableDatabase.rawQuery("SELECT * FROM banks WHERE exam_id=?", arrayOf(examId)).use { c ->
            if (c.moveToFirst()) {
                BankVersion(c.getString(0), c.getInt(1), c.getLong(2), c.getInt(3))
            } else null
        }

    fun allBankVersions(): List<BankVersion> =
        readableDatabase.rawQuery("SELECT * FROM banks", null).use { c ->
            val l = mutableListOf<BankVersion>()
            while (c.moveToNext()) l += BankVersion(c.getString(0), c.getInt(1), c.getLong(2), c.getInt(3))
            l
        }

    fun setBankVersion(v: BankVersion) {
        writableDatabase.insertWithOnConflict("banks", null, ContentValues().apply {
            put("exam_id", v.examId); put("version", v.version); put("updated_at", v.updatedAt); put("question_count", v.questionCount)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    companion object {
        const val DB_NAME = "softkao.db"
        const val DB_VERSION = 2
    }
}

// ------------------------------------------------------------------ cursor mapping

private fun Cursor.str(name: String): String {
    val idx = getColumnIndex(name)
    return if (idx >= 0 && !isNull(idx)) getString(idx) else ""
}

private fun Cursor.strOrNull(name: String): String? {
    val idx = getColumnIndex(name)
    return if (idx >= 0 && !isNull(idx)) getString(idx) else null
}

private fun Cursor.int(name: String, def: Int = 0): Int {
    val idx = getColumnIndex(name)
    return if (idx >= 0 && !isNull(idx)) getInt(idx) else def
}

private fun Cursor.long(name: String, def: Long = 0L): Long {
    val idx = getColumnIndex(name)
    return if (idx >= 0 && !isNull(idx)) getLong(idx) else def
}

private fun Cursor.toQuestion(): Question {
    val options = mutableListOf<Option>()
    runCatching {
        val arr = JSONArray(str("options_json"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            options += Option(o.optString("key"), o.optString("text"))
        }
    }
    val answers = mutableListOf<String>()
    runCatching {
        val arr = JSONArray(str("answer_json"))
        for (i in 0 until arr.length()) answers += arr.getString(i)
    }
    val tags = mutableListOf<String>()
    runCatching {
        val arr = JSONArray(str("tags_json"))
        for (i in 0 until arr.length()) tags += arr.getString(i)
    }
    val subs = mutableListOf<SubQuestion>()
    runCatching {
        val arr = JSONArray(str("sub_questions_json"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            subs += SubQuestion(o.optString("q"), o.optString("answer").takeIf { it.isNotBlank() })
        }
    }
    return Question(
        id = str("id"),
        examId = str("exam_id"),
        subject = str("subject"),
        type = QuestionType.fromWire(str("type")) ?: QuestionType.SINGLE,
        difficulty = int("difficulty", 3),
        chapter = str("chapter"),
        knowledgePoint = str("knowledge_point"),
        stem = str("stem"),
        options = options,
        answer = answers,
        explanation = str("explanation"),
        aiExplanation = strOrNull("ai_explanation"),
        source = str("source"),
        tags = tags,
        caseStudy = strOrNull("case_study"),
        caseId = strOrNull("case_id"),
        subQuestions = subs,
    )
}

private fun Cursor.toWrongEntry(): WrongEntry = WrongEntry(
    questionId = str("question_id"),
    examId = str("exam_id"),
    wrongCount = int("wrong_count"),
    streak = int("streak"),
    mastery = Mastery.fromWire(str("mastery")),
    lastWrongAt = long("last_wrong_at"),
    nextReviewAt = long("next_review_at"),
    note = strOrNull("note"),
)
