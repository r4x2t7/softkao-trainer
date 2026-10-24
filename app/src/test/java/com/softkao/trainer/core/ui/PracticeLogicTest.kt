package com.softkao.trainer.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 刷题页纯逻辑的边界测试。
 *
 * 这些是最容易出「点了没反应 / 跳到不存在的题」的地方，
 * 靠手点很难覆盖所有边界，所以在 JVM 上直接钉死。
 */
class PracticeLogicTest {

    // ------------------------------------------------------------ 跳转

    @Test
    fun `正跳不会超过末题`() {
        assertEquals(9, PracticeLogic.jump(index = 0, delta = 100, total = 10))
        assertEquals(9, PracticeLogic.jump(index = 8, delta = 1, total = 10))
        assertEquals(9, PracticeLogic.jump(index = 9, delta = 1, total = 10))
    }

    @Test
    fun `负跳不会小于首题`() {
        assertEquals(0, PracticeLogic.jump(index = 0, delta = -100, total = 10))
        assertEquals(0, PracticeLogic.jump(index = 3, delta = -10, total = 10))
        assertEquals(0, PracticeLogic.jump(index = 0, delta = -1, total = 10))
    }

    @Test
    fun `跳转结果始终落在有效区间内`() {
        val total = 1335
        for (index in listOf(0, 1, 99, 100, 500, 1334)) {
            for (delta in listOf(-100, -10, -1, 1, 10, 100)) {
                val r = PracticeLogic.jump(index, delta, total)
                assertTrue("越界: index=$index delta=$delta -> $r", r in 0 until total)
            }
        }
    }

    @Test
    fun `题库为空时跳转不产生非法下标`() {
        assertEquals(0, PracticeLogic.jump(index = 0, delta = 1, total = 0))
        assertEquals(0, PracticeLogic.jump(index = 5, delta = -5, total = 0))
    }

    @Test
    fun `首页与尾页`() {
        assertEquals(0, PracticeLogic.firstIndex(100))
        assertEquals(99, PracticeLogic.lastIndex(100))
        assertEquals(0, PracticeLogic.lastIndex(0))
        assertEquals(0, PracticeLogic.lastIndex(1))
    }

    // ------------------------------------------------------------ 答题卡分批

    @Test
    fun `答题卡按 30 题一批分页`() {
        assertEquals(1, PracticeLogic.sheetPageCount(1))
        assertEquals(1, PracticeLogic.sheetPageCount(30))
        assertEquals(2, PracticeLogic.sheetPageCount(31))
        assertEquals(45, PracticeLogic.sheetPageCount(1335))
        assertEquals(36, PracticeLogic.sheetPageCount(1055))
    }

    @Test
    fun `题库为空时也有一批`() {
        assertEquals(1, PracticeLogic.sheetPageCount(0))
    }

    @Test
    fun `每批的题号区间不重不漏`() {
        val total = 1335
        val covered = (0 until PracticeLogic.sheetPageCount(total)).flatMap {
            PracticeLogic.sheetRange(it, total).toList()
        }
        assertEquals("覆盖题数不对", total, covered.size)
        // 不重不漏：首尾对上，且严格递增
        assertEquals(0, covered.first())
        assertEquals(total - 1, covered.last())
        assertEquals(total, covered.toSet().size)
        assertTrue(covered.zipWithNext().all { (a, b) -> b == a + 1 })
    }

    @Test
    fun `最后一批只到剩余题数`() {
        // 1335 题共 45 批，最后一批是第 44 批：1320..1334，共 15 题
        val last = PracticeLogic.sheetRange(44, 1335)
        assertEquals(15, last.count())
        assertEquals(1320, last.first)
        assertEquals(1334, last.last)
    }

    @Test
    fun `批号越界会被夹回合法范围`() {
        val first = PracticeLogic.sheetRange(-5, 1335)
        assertEquals(0, first.first)
        assertEquals(29, first.last)
        val overflow = PracticeLogic.sheetRange(999, 1335)
        assertEquals(1320, overflow.first)
        assertEquals(1334, overflow.last)
    }

    @Test
    fun `打开答题卡时停在当前题所在批`() {
        assertEquals(0, PracticeLogic.initialSheetPage(current = 0, total = 1335))
        assertEquals(0, PracticeLogic.initialSheetPage(current = 29, total = 1335))
        assertEquals(1, PracticeLogic.initialSheetPage(current = 30, total = 1335))
        assertEquals(44, PracticeLogic.initialSheetPage(current = 1334, total = 1335))
    }

    // ------------------------------------------------------------ 模拟卷计时

    @Test
    fun `倒计时文案`() {
        assertEquals("⏱ 模拟卷剩余 90:00", PracticeLogic.mockTimeText(90 * 60))
        assertEquals("⏱ 模拟卷剩余 01:05", PracticeLogic.mockTimeText(65))
        assertEquals("⏱ 模拟卷剩余 00:09", PracticeLogic.mockTimeText(9))
    }

    @Test
    fun `超时后文案改成提示交卷`() {
        assertEquals("⏱ 已超时，请尽快交卷", PracticeLogic.mockTimeText(0))
        assertEquals("⏱ 已超时，请尽快交卷", PracticeLogic.mockTimeText(-5))
    }

    @Test
    fun `最后十分钟算紧急`() {
        assertFalse(PracticeLogic.isMockUrgent(90 * 60))
        assertFalse(PracticeLogic.isMockUrgent(10 * 60))
        assertTrue(PracticeLogic.isMockUrgent(10 * 60 - 1))
        assertTrue(PracticeLogic.isMockUrgent(0))
    }

    @Test
    fun `模拟卷时长是九十分钟`() {
        assertEquals(5400L, PracticeLogic.MOCK_DURATION_SECONDS)
    }

    // ------------------------------------------------------------ 判断题与案例题

    @Test
    fun `判断题渲染为 √ 和 × 两个按钮`() {
        assertEquals(2, PracticeLogic.TRUE_FALSE_CHOICES.size)
        assertEquals("√", PracticeLogic.TRUE_FALSE_CHOICES[0].first)
        assertEquals("×", PracticeLogic.TRUE_FALSE_CHOICES[1].first)
        assertEquals("正确", PracticeLogic.TRUE_FALSE_CHOICES[0].second)
        assertEquals("错误", PracticeLogic.TRUE_FALSE_CHOICES[1].second)
    }

    @Test
    fun `案例题自评记住了算掌握`() {
        assertTrue(PracticeLogic.caseSelfAssessment(remembered = true))
        assertFalse(PracticeLogic.caseSelfAssessment(remembered = false))
    }
}
