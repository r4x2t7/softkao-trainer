package com.softkao.trainer.core.data.db

import com.softkao.trainer.core.model.Question

/** 单题历史正确率 */
fun AppDatabase.questionAccuracy(questionId: String): Pair<Int, Int> =
    readableDatabase.rawQuery(
        "SELECT COUNT(*), SUM(CASE WHEN correct=1 THEN 1 ELSE 0 END) FROM attempts WHERE question_id=?",
        arrayOf(questionId)
    ).use { c ->
        if (c.moveToFirst()) {
            val total = c.getInt(0)
            val ok = if (c.isNull(1)) 0 else c.getInt(1)
            total to ok
        } else 0 to 0
    }

/** 题目列表按给定顺序返回（保持调用方排序） */
fun AppDatabase.questionsByIds(ids: List<String>): List<Question> {
    if (ids.isEmpty()) return emptyList()
    val map = questions(ids = ids).associateBy { it.id }
    return ids.mapNotNull { map[it] }
}
