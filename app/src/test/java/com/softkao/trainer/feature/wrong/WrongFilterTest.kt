package com.softkao.trainer.feature.wrong

import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
import com.softkao.trainer.core.model.WrongEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 错题本筛选逻辑测试。
 *
 * **背景**：错题本上面的 chip 之前「点了没效果」——
 * 「收藏」和「掌握度」两个筛选能同时开着，而收藏分支优先级更高，
 * 结果点了「未掌握」chip 它亮着，列表却纹丝不动。
 * 根因是筛选状态不对称（选掌握度不清收藏，切收藏才清掌握度）。
 */
class WrongFilterTest {

    private fun question(id: String, exam: ExamId) = Question(
        id = id,
        examId = exam.id,
        subject = "",
        type = QuestionType.SINGLE,
        difficulty = 1,
        chapter = "第一章",
        knowledgePoint = "挣值管理",
        stem = "题干 $id",
        options = emptyList(),
        answer = emptyList(),
        explanation = "",
        aiExplanation = null,
        source = "",
        tags = emptyList(),
        caseStudy = null,
        caseId = null,
        subQuestions = emptyList(),
    )

    private fun wrongItem(id: String, exam: ExamId, mastery: Mastery) = WrongItem(
        entry = WrongEntry(
            questionId = id,
            examId = exam.id,
            wrongCount = 1,
            streak = 0,
            mastery = mastery,
            lastWrongAt = 0L,
            nextReviewAt = 0L,
            note = null,
        ),
        question = question(id, exam),
    )

    private fun bookmarkItem(id: String, exam: ExamId) = WrongItem(
        entry = null,
        question = question(id, exam),
    )

    private val si = ExamId.SYSTEM_INTEGRATION
    private val md = ExamId.MULTIMEDIA_DESIGNER

    private val wrongItems = listOf(
        wrongItem("w1", si, Mastery.NOT_MASTERED),
        wrongItem("w2", si, Mastery.NOT_MASTERED),
        wrongItem("w3", si, Mastery.SHADY),
        wrongItem("w4", si, Mastery.MASTERED),
        wrongItem("m1", md, Mastery.NOT_MASTERED),
    )

    private val bookmarkItems = listOf(
        bookmarkItem("b1", si),
        bookmarkItem("b2", md),
    )

    // ---------------------------------------------------------------- 状态互斥

    @Test
    fun `选掌握度会自动回到错题视图`() {
        val f = WrongFilterState().withMode(WrongListMode.BOOKMARK).withMastery(Mastery.NOT_MASTERED)
        assertEquals(WrongListMode.WRONG, f.mode)
        assertEquals(Mastery.NOT_MASTERED, f.mastery)
    }

    @Test
    fun `切到收藏会清掉掌握度筛选`() {
        val f = WrongFilterState().withMastery(Mastery.SHADY).withMode(WrongListMode.BOOKMARK)
        assertEquals(WrongListMode.BOOKMARK, f.mode)
        assertNull(f.mastery)
    }

    @Test
    fun `先点收藏再点掌握度时掌握度筛选不会被吞掉`() {
        // 这条就是「点了没效果」那个 bug 的回归测试
        val f = WrongFilterState()
            .withMode(WrongListMode.BOOKMARK)
            .withMastery(Mastery.NOT_MASTERED)
        val shown = selectWrongItems(wrongItems, bookmarkItems, f, examId = si.id)
        assertEquals(listOf("w1", "w2"), shown.map { it.questionId })
    }

    @Test
    fun `清除筛选回到全部错题`() {
        val f = WrongFilterState().withMode(WrongListMode.BOOKMARK).cleared()
        assertEquals(WrongListMode.WRONG, f.mode)
        assertNull(f.mastery)
        assertFalse(f.isFiltered)
    }

    @Test
    fun `有筛选时才显示清除入口`() {
        assertFalse(WrongFilterState().isFiltered)
        assertTrue(WrongFilterState().withMastery(Mastery.SHADY).isFiltered)
        assertTrue(WrongFilterState().withMode(WrongListMode.BOOKMARK).isFiltered)
    }

    @Test
    fun `当前视图有明确的人话描述`() {
        assertEquals("全部错题", WrongFilterState().label)
        assertEquals("模糊", WrongFilterState().withMastery(Mastery.SHADY).label)
        assertEquals("★ 收藏", WrongFilterState().withMode(WrongListMode.BOOKMARK).label)
    }

    // ---------------------------------------------------------------- 筛选结果

    @Test
    fun `错题视图下掌握度筛选生效`() {
        val f = WrongFilterState().withMastery(Mastery.NOT_MASTERED)
        val shown = selectWrongItems(wrongItems, bookmarkItems, f, examId = si.id)
        assertEquals(listOf("w1", "w2"), shown.map { it.questionId })
    }

    @Test
    fun `收藏视图列出收藏的题`() {
        val f = WrongFilterState().withMode(WrongListMode.BOOKMARK)
        val shown = selectWrongItems(wrongItems, bookmarkItems, f, examId = si.id)
        assertEquals(listOf("b1"), shown.map { it.questionId })
    }

    @Test
    fun `科目过滤生效`() {
        val f = WrongFilterState()
        val shown = selectWrongItems(wrongItems, bookmarkItems, f, examId = md.id)
        assertEquals(listOf("m1"), shown.map { it.questionId })
    }

    @Test
    fun `科目为空时不按科目过滤`() {
        val f = WrongFilterState()
        val shown = selectWrongItems(wrongItems, bookmarkItems, f, examId = null)
        assertEquals(5, shown.size)
    }

    @Test
    fun `各掌握度数量统计正确`() {
        val counts = countByMastery(wrongItems.filter { it.examId == si.id })
        assertEquals(2, counts[Mastery.NOT_MASTERED])
        assertEquals(1, counts[Mastery.SHADY])
        assertEquals(1, counts[Mastery.MASTERED])
    }
}
