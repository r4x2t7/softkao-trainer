package com.softkao.trainer.feature.wrong

import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.WrongEntry

/**
 * 错题本的两种列表视图。
 *
 * **必须二选一、互斥**：之前「收藏」和「掌握度」能同时生效，
 * 而收藏分支优先级更高，结果就是点了「未掌握」chip 它亮着但列表纹丝不动——
 * 典型的「点了没效果」。现在收敛成一个枚举，任何时刻只有一个视图生效。
 */
enum class WrongListMode(val label: String) {
    WRONG("错题"),
    BOOKMARK("★ 收藏"),
}

/**
 * 错题本的筛选状态。
 *
 * 规则很简单：**同一时刻只能有一个筛选生效**。
 * - 选掌握度 → 回到错题视图（收藏被关掉）
 * - 切收藏 → 掌握度被清掉
 *
 * 做成纯数据类是为了能直接单测，不用起数据库。
 */
data class WrongFilterState(
    val mode: WrongListMode = WrongListMode.WRONG,
    val mastery: Mastery? = null,
) {
    /** 选掌握度：自动回到错题视图，保证任何时候只有一个筛选生效 */
    fun withMastery(mastery: Mastery?): WrongFilterState =
        copy(mode = WrongListMode.WRONG, mastery = mastery)

    /** 切视图：收藏没有掌握度，切过去顺手清掉掌握度筛选 */
    fun withMode(mode: WrongListMode): WrongFilterState =
        copy(mode = mode, mastery = null)

    /** 清除全部筛选，回到「全部错题」 */
    fun cleared(): WrongFilterState = WrongFilterState()

    /** 当前是否有筛选生效（决定要不要显示「清除筛选」） */
    val isFiltered: Boolean get() = mode == WrongListMode.BOOKMARK || mastery != null

    /** 当前视图的人话描述 */
    val label: String
        get() = when {
            mode == WrongListMode.BOOKMARK -> "★ 收藏"
            mastery != null -> mastery.label
            else -> "全部错题"
        }
}

/** 错题本里的一条：错题条目（[entry] 非空）或收藏题目（[entry] 为空） */
data class WrongItem(
    val entry: WrongEntry?,
    val question: Question?,
) {
    val examId: String? get() = entry?.examId ?: question?.examId
    val questionId: String get() = entry?.questionId ?: question?.id.orEmpty()
    val mastery: Mastery? get() = entry?.mastery
}

/**
 * 按「视图模式 + 掌握度 + 科目」筛出要显示的条目。
 *
 * 纯函数，不碰 Compose 也不碰数据库，方便直接单测。
 */
fun selectWrongItems(
    wrongItems: List<WrongItem>,
    bookmarkItems: List<WrongItem>,
    filter: WrongFilterState,
    examId: String?,
): List<WrongItem> {
    val pool = if (filter.mode == WrongListMode.BOOKMARK) bookmarkItems else wrongItems
    return pool.filter { item ->
        val examOk = examId == null || item.examId == examId
        // 掌握度只对错题有意义：收藏的题没有掌握度
        val masteryOk = filter.mode == WrongListMode.BOOKMARK ||
            filter.mastery == null ||
            item.mastery == filter.mastery
        examOk && masteryOk
    }
}

/** 各掌握度在某个科目下的错题数量（用于在 chip 上直接显示数量） */
fun countByMastery(items: List<WrongItem>): Map<Mastery, Int> =
    items.mapNotNull { it.mastery }.groupingBy { it }.eachCount()
