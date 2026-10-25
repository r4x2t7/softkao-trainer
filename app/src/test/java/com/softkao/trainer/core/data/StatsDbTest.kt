package com.softkao.trainer.core.data.db

import androidx.test.core.app.ApplicationProvider
import com.softkao.trainer.core.model.PracticeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * 统计相关的数据库行为测试。
 *
 * 重点盯「连续打卡天数」：之前 SQL 取的日期格式（2026-10-25）和代码里
 * 比对的格式（20261025）对不上，导致连续天数永远显示 0。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = com.softkao.trainer.TestApp::class)
class StatsDbTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = AppDatabase(ApplicationProvider.getApplicationContext())
    }

    /** back=0 表示今天，返回当天零点的时间戳 */
    private fun dayStart(back: Int): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -back)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** 在 back 天前的那天记 count 道题（其中一半答对） */
    private fun addAttempts(back: Int, count: Int) {
        repeat(count) { i ->
            db.insertAttempt(
                questionId = "q-$back-$i",
                examId = "system-integration",
                mode = PracticeMode.SEQUENTIAL,
                selected = emptyList(),
                correct = i % 2 == 0,
                durationMs = 1000L,
                at = dayStart(back) + 10 * 3600_000L,
            )
        }
    }

    @Test
    fun `连续三天有记录时连续天数是 3`() {
        addAttempts(0, 1)
        addAttempts(1, 1)
        addAttempts(2, 1)
        assertEquals(3, db.streakDays())
    }

    @Test
    fun `同一天多道题只算一天`() {
        addAttempts(0, 5)
        addAttempts(1, 3)
        assertEquals(2, db.streakDays())
    }

    @Test
    fun `今天还没做题不打断昨天起的连续`() {
        addAttempts(1, 1)
        addAttempts(2, 1)
        assertEquals(2, db.streakDays())
    }

    @Test
    fun `中间断一天只统计最近连续的一段`() {
        addAttempts(0, 1)
        addAttempts(1, 1)
        addAttempts(3, 1) // 2 天前没做，断档
        assertEquals(2, db.streakDays())
    }

    @Test
    fun `没有任何记录时连续天数为 0`() {
        assertEquals(0, db.streakDays())
    }

    @Test
    fun `七天趋势按天分组且数量正确`() {
        addAttempts(0, 2) // 今天 2 题，其中 1 题答对
        addAttempts(2, 3)
        val trend = db.dailyTrend(7)
        assertEquals(7, trend.size)
        val today = trend.last()
        assertEquals(2, today.second.first)
        assertEquals(1, today.second.second)
        // 没有记录的日期也要占位，画图时才知道那天是 0
        assertTrue(trend.any { it.second.first == 0 })
        assertEquals(3, trend.first { it.second.first == 3 }.second.first)
    }

    @Test
    fun `今日题数只统计今天`() {
        addAttempts(0, 4)
        addAttempts(1, 4)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(4, db.attemptsSince(cal.timeInMillis, "system-integration"))
    }
}
