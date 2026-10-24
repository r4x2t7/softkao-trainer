package com.softkao.trainer.core.data.repo

import com.softkao.trainer.core.data.db.AppDatabase
import com.softkao.trainer.core.data.db.questionsByIds
import com.softkao.trainer.core.data.db.questionAccuracy
import com.softkao.trainer.core.model.BankVersion
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.WrongEntry

/** 题目读取 */
class QuestionRepository(private val db: AppDatabase) {

    fun count(examId: String? = null): Int = db.questionCount(examId)

    fun byId(id: String): Question? = db.questionById(id)

    fun list(
        examId: String? = null,
        chapter: String? = null,
        knowledgePoint: String? = null,
        subject: String? = null,
        ids: List<String>? = null,
        limit: Int? = null,
        offset: Int = 0,
    ): List<Question> = db.questions(examId, chapter, knowledgePoint, subject, ids, limit, offset)

    fun chapters(examId: String): List<String> = db.chapters(examId)

    fun chapterCounts(examId: String): Map<String, Int> = db.chapterCounts(examId)

    fun knowledgePoints(examId: String, chapter: String? = null): List<String> = db.knowledgePoints(examId, chapter)

    /** 按给定顺序返回题目（顺序保持调用方给的顺序） */
    fun byIds(ids: List<String>): List<Question> = db.questionsByIds(ids)

    /** 关键词搜索题目 */
    fun search(keyword: String, examId: String? = null, limit: Int = 60): List<Question> =
        db.search(keyword, examId, limit)

    /** 每个知识点的题量（用于专项练习入口） */
    fun knowledgePointCounts(examId: String): Map<String, Int> = db.knowledgePointCounts(examId)
}

/**
 * 作答记录 + 错题本 + 掌握度。
 * 掌握度规则：连对 3 次且该题历史正确率 ≥ 80% 记为「已掌握」，其余按连对次数区分「模糊 / 未掌握」。
 */
class PracticeRepository(private val db: AppDatabase) {

    fun submit(
        question: Question,
        selected: List<String>,
        mode: PracticeMode,
        durationMs: Long,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        val correct = selected.toSet() == question.answer.toSet()
        record(question, correct, selected, mode, durationMs, now)
        return correct
    }

    /**
     * 案例分析题由用户自评（记住了/没记住），答案不参与自动判分，
     * 但同样计入作答记录、错题本与掌握度。
     */
    fun submitCase(
        question: Question,
        remembered: Boolean,
        mode: PracticeMode,
        durationMs: Long,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        record(question, remembered, listOf(if (remembered) "√" else "×"), mode, durationMs, now)
        return remembered
    }

    private fun record(
        question: Question,
        correct: Boolean,
        selected: List<String>,
        mode: PracticeMode,
        durationMs: Long,
        now: Long,
    ) {
        db.insertAttempt(question.id, question.examId, mode, selected, correct, durationMs, now)

        val existing = db.wrongEntry(question.id)
        if (!correct) {
            val wrongCount = (existing?.wrongCount ?: 0) + 1
            db.upsertWrongEntry(
                WrongEntry(
                    questionId = question.id,
                    examId = question.examId,
                    wrongCount = wrongCount,
                    streak = 0,
                    mastery = Mastery.NOT_MASTERED,
                    lastWrongAt = now,
                    nextReviewAt = now + REVIEW_STEPS[0] * DAY_MS,
                    note = existing?.note,
                )
            )
        } else if (existing != null) {
            val streak = existing.streak + 1
            val (total, ok) = db.questionAccuracy(question.id)
            val accuracy = if (total == 0) 0f else ok.toFloat() / total
            val mastery = when {
                streak >= 3 && accuracy >= 0.8f -> Mastery.MASTERED
                streak >= 1 -> Mastery.SHADY
                else -> Mastery.NOT_MASTERED
            }
            val stepIndex = when (streak) {
                0 -> 0
                1 -> 1
                2 -> 2
                else -> 3
            }
            db.upsertWrongEntry(
                existing.copy(
                    streak = streak,
                    mastery = mastery,
                    lastWrongAt = existing.lastWrongAt,
                    nextReviewAt = now + REVIEW_STEPS[stepIndex] * DAY_MS,
                )
            )
        }
    }

    fun toggleBookmark(questionId: String, examId: String, now: Long = System.currentTimeMillis()): Boolean =
        db.toggleBookmark(questionId, examId, now)

    fun isBookmarked(questionId: String): Boolean = db.isBookmarked(questionId)

    fun bookmarkedIds(examId: String? = null): List<String> = db.bookmarkedIds(examId)

    fun wrongEntries(examId: String? = null): List<WrongEntry> = db.wrongEntries(examId)

    /** 到期需要复习的错题 */
    fun dueWrongEntries(examId: String? = null, now: Long = System.currentTimeMillis()): List<WrongEntry> =
        db.wrongEntries(examId).filter { it.nextReviewAt <= now }.sortedBy { it.nextReviewAt }

    fun setWrongNote(questionId: String, note: String?) = db.updateWrongNote(questionId, note)

    fun removeWrongEntry(questionId: String) = db.deleteWrongEntry(questionId)

    companion object {
        const val DAY_MS = 24 * 3600 * 1000L
        val REVIEW_STEPS = longArrayOf(1, 3, 7, 15)
    }
}

/** 统计 */
class StatsRepository(private val db: AppDatabase) {

    fun totalAttempts(examId: String? = null): Int = db.attemptCount(examId)

    fun correctAttempts(examId: String? = null): Int = db.correctCount(examId)

    fun accuracy(examId: String? = null): Float {
        val total = totalAttempts(examId)
        return if (total == 0) 0f else correctAttempts(examId).toFloat() / total
    }

    fun streakDays(): Int = db.streakDays()

    fun attemptsToday(examId: String? = null): Int {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return db.attemptsSince(cal.timeInMillis, examId)
    }

    fun dailyTrend(days: Int = 7): List<Pair<String, Pair<Int, Int>>> = db.dailyTrend(days)

    fun knowledgeStats(examId: String): Map<String, Pair<Int, Int>> = db.knowledgeStats(examId)

    fun bankVersions(): List<BankVersion> = db.allBankVersions()

    /** 每章的题量 / 作答数 / 正确数 */
    fun chapterStats(examId: String): Map<String, Triple<Int, Int, Int>> = db.chapterStats(examId)

    /** 章节 → 知识点 → 题量 */
    fun knowledgePointsByChapter(examId: String): Map<String, Map<String, Int>> =
        db.knowledgePointsByChapter(examId)
}
