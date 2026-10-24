package com.softkao.trainer.core.ui

/**
 * 刷题页的纯逻辑：跳转、分页、计时、判断题选项、案例题自评。
 *
 * 这些逻辑原先散在 Composable 里，只能靠手点才能发现边界问题
 * （比如在第 3 题点「−100」会跳到哪一题、答题卡翻页越界怎么办）。
 * 抽到这里之后可以在 JVM 上直接跑单元测试。
 */
object PracticeLogic {

    /** 答题卡每批显示多少个题号 */
    const val ANSWER_SHEET_PER_PAGE = 30

    /** 模拟卷时长（秒）：考试要求选择题 90 分钟 */
    const val MOCK_DURATION_SECONDS = 90L * 60

    /** 判断题在题库里不带选项，界面上要渲染成两个可点按钮 */
    val TRUE_FALSE_CHOICES: List<Pair<String, String>> = listOf("√" to "正确", "×" to "错误")

    /**
     * 相对跳转，结果永远落在 [0, total-1] 内。
     * total 为 0 时返回 0，避免出现 -1 这种非法下标。
     */
    fun jump(index: Int, delta: Int, total: Int): Int {
        if (total <= 0) return 0
        return (index + delta).coerceIn(0, total - 1)
    }

    /** 首页 */
    fun firstIndex(total: Int): Int = if (total <= 0) 0 else 0

    /** 尾页 */
    fun lastIndex(total: Int): Int = (total - 1).coerceAtLeast(0)

    /** 答题卡一共几批 */
    fun sheetPageCount(total: Int): Int {
        if (total <= 0) return 1
        return (total + ANSWER_SHEET_PER_PAGE - 1) / ANSWER_SHEET_PER_PAGE
    }

    /** 某一批覆盖的题号区间（闭区间），越界自动收敛 */
    fun sheetRange(page: Int, total: Int): IntRange {
        val safePage = page.coerceIn(0, sheetPageCount(total) - 1)
        val start = safePage * ANSWER_SHEET_PER_PAGE
        val end = (start + ANSWER_SHEET_PER_PAGE).coerceAtMost(total)
        return start until end
    }

    /** 打开答题卡时应该停在第几批：跟当前题号走 */
    fun initialSheetPage(current: Int, total: Int): Int {
        val page = current / ANSWER_SHEET_PER_PAGE
        return page.coerceIn(0, sheetPageCount(total) - 1)
    }

    /** 模拟卷倒计时文案 */
    fun mockTimeText(remaining: Long): String {
        if (remaining <= 0) return "⏱ 已超时，请尽快交卷"
        val m = remaining / 60
        val s = remaining % 60
        return "⏱ 模拟卷剩余 %02d:%02d".format(m, s)
    }

    /** 最后 10 分钟算紧急，界面变红 */
    fun isMockUrgent(remaining: Long): Boolean = remaining < 10 * 60

    /** 案例题自评：记住了算掌握，没记住进错题本 */
    fun caseSelfAssessment(remembered: Boolean): Boolean = remembered
}
