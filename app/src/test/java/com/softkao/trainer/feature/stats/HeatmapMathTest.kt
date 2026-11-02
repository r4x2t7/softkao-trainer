package com.softkao.trainer.feature.stats

import com.softkao.trainer.feature.stats.StatsViewModel.HeatCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HeatmapMathTest {

    private fun millis(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month - 1, day, 12, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    @Test
    fun `周一开头对齐且首列补空`() {
        // 2026-11-02 是周一；从 11-02 起连续 1 天 → 首列无空
        val raw = listOf("11-02" to (3 to 2))
        val weeks = StatsViewModel.buildHeatmapWeeks(raw, millis(2026, 11, 2))
        assertEquals(1, weeks.size)
        assertEquals(7, weeks[0].size)
        assertEquals(HeatCell(3, 2), weeks[0][0])
        assertEquals(6, weeks[0].count { it == null })
    }

    @Test
    fun `周中日期在首列前补空位`() {
        // 2026-11-04 是周三（周一=0，周三=2）→ 前面补 2 个空
        val raw = listOf("11-04" to (1 to 1))
        val weeks = StatsViewModel.buildHeatmapWeeks(raw, millis(2026, 11, 4))
        assertEquals(1, weeks.size)
        assertEquals(null, weeks[0][0])
        assertEquals(null, weeks[0][1])
        assertEquals(HeatCell(1, 1), weeks[0][2])
    }

    @Test
    fun `跨周边界按 7 切分`() {
        // 2026-11-02(周一) 起 8 天：周一~周日一行，第二个周一进下一行
        val raw = (1..8).map { d -> String.format("11-%02d", d) to (d to d) }
        val weeks = StatsViewModel.buildHeatmapWeeks(raw, millis(2026, 11, 9))
        assertEquals(2, weeks.size)
        assertEquals(7, weeks[0].size)
        assertEquals(7, weeks[1].size)
        assertEquals(HeatCell(8, 8), weeks[1][0])
        assertEquals(6, weeks[1].count { it == null })
        assertTrue(weeks[0].none { it == null })
    }

    @Test
    fun `空数据返回空网格`() {
        assertEquals(0, StatsViewModel.buildHeatmapWeeks(emptyList(), millis(2026, 11, 2)).size)
    }
}
