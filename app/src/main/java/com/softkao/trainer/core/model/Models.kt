package com.softkao.trainer.core.model

/** 题型 */
enum class QuestionType(val wire: String, val label: String) {
    SINGLE("single", "单选"),
    MULTI("multi", "多选"),
    TRUE_FALSE("truefalse", "判断"),
    CASE("case", "案例分析");

    companion object {
        fun fromWire(value: String): QuestionType? = entries.firstOrNull { it.wire == value }
    }
}

/** 考试科目 */
enum class ExamId(val id: String, val fullName: String, val shortName: String) {
    SYSTEM_INTEGRATION("system-integration", "系统集成项目管理工程师", "系统集成"),
    MULTIMEDIA_DESIGNER("multimedia-designer", "多媒体应用设计师", "多媒体");

    companion object {
        fun fromId(id: String): ExamId? = entries.firstOrNull { it.id == id }
        fun fromName(name: String): ExamId? = entries.firstOrNull { it.fullName == name }
    }
}

/** 掌握度 */
enum class Mastery(val label: String) {
    NEW("未做过"),
    NOT_MASTERED("未掌握"),
    SHADY("模糊"),
    MASTERED("已掌握");

    companion object {
        fun fromWire(value: String?): Mastery = entries.firstOrNull { it.name == value } ?: NEW
    }
}

/** 练习模式 */
enum class PracticeMode(val label: String) {
    SEQUENTIAL("顺序练习"),
    CHAPTER("章节练习"),
    KNOWLEDGE("专项练习"),
    DAILY("每日一练"),
    WRONG("错题重做"),
    MOCK("模拟卷"),
    CASE("案例分析");

    companion object {
        fun fromWire(value: String): PracticeMode = entries.firstOrNull { it.name == value } ?: SEQUENTIAL
    }
}

data class Option(val key: String, val text: String)

data class SubQuestion(val prompt: String, val referenceAnswer: String?)

data class Question(
    val id: String,
    val examId: String,
    val subject: String,
    val type: QuestionType,
    val difficulty: Int,
    val chapter: String,
    val knowledgePoint: String,
    val stem: String,
    val options: List<Option>,
    val answer: List<String>,
    val explanation: String,
    val aiExplanation: String?,
    val source: String,
    val tags: List<String>,
    val caseStudy: String?,
    val caseId: String?,
    val subQuestions: List<SubQuestion>,
)

data class Attempt(
    val id: Long = 0,
    val questionId: String,
    val examId: String,
    val mode: PracticeMode,
    val selected: List<String>,
    val correct: Boolean,
    val durationMs: Long,
    val answeredAt: Long,
)

data class WrongEntry(
    val questionId: String,
    val examId: String,
    val wrongCount: Int,
    val streak: Int,
    val mastery: Mastery,
    val lastWrongAt: Long,
    val nextReviewAt: Long,
    val note: String?,
)

data class Bookmark(
    val questionId: String,
    val examId: String,
    val createdAt: Long,
)

/** 一次导入的结果 */
data class ImportReport(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val errors: List<String> = emptyList(),
) {
    val total: Int get() = added + updated
}

/** 题库版本信息 */
data class BankVersion(
    val examId: String,
    val version: Int,
    val updatedAt: Long,
    val questionCount: Int,
)
