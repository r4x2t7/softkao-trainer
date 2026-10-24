package com.softkao.trainer.core.model

/** 公式 / 关键数字 */
data class Formula(
    val name: String,
    val formula: String,
    val note: String,
)

/** 一章的精讲资料 */
data class ChapterMaterial(
    val chapter: String,
    val summary: String = "",
    val examFocus: String = "",
    val keyPoints: List<String> = emptyList(),
    val pitfalls: List<String> = emptyList(),
    val formulas: List<Formula> = emptyList(),
    val memoryTip: String = "",
    val answerTemplate: String = "",
)
