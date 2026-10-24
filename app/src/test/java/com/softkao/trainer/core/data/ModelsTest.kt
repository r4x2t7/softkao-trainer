package com.softkao.trainer.core.data

import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 领域模型的 wire 格式解析测试：App 与题库之间靠这些字符串对齐。 */
class ModelsTest {

    @Test
    fun `题型 wire 双向一致`() {
        QuestionType.entries.forEach { t ->
            assertEquals(t, QuestionType.fromWire(t.wire))
        }
        assertNull(QuestionType.fromWire("填空"))
        assertNull(QuestionType.fromWire(""))
    }

    @Test
    fun `题型 wire 是小写英文且互不重复`() {
        val wires = QuestionType.entries.map { it.wire }
        assertEquals(wires.size, wires.toSet().size)
        wires.forEach { assertTrue(it == it.lowercase()) }
        assertEquals(listOf("single", "multi", "truefalse", "case"), wires)
    }

    @Test
    fun `科目 id 与全名都能解析`() {
        ExamId.entries.forEach { e ->
            assertEquals(e, ExamId.fromId(e.id))
            assertEquals(e, ExamId.fromName(e.fullName))
        }
        assertNull(ExamId.fromId("不存在的科目"))
        assertNull(ExamId.fromName("不存在的科目"))
    }

    @Test
    fun `科目 id 是题库目录名`() {
        assertEquals("system-integration", ExamId.SYSTEM_INTEGRATION.id)
        assertEquals("multimedia-designer", ExamId.MULTIMEDIA_DESIGNER.id)
    }

    @Test
    fun `掌握度解析失败时回落到未做过`() {
        assertEquals(Mastery.MASTERED, Mastery.fromWire("MASTERED"))
        assertEquals(Mastery.NEW, Mastery.fromWire(null))
        assertEquals(Mastery.NEW, Mastery.fromWire("乱填"))
    }

    @Test
    fun `练习模式解析失败时回落到顺序练习`() {
        assertEquals(PracticeMode.MOCK, PracticeMode.fromWire("MOCK"))
        assertEquals(PracticeMode.SEQUENTIAL, PracticeMode.fromWire("不存在"))
        assertEquals(PracticeMode.SEQUENTIAL, PracticeMode.fromWire(""))
    }

    @Test
    fun `练习模式覆盖全部入口`() {
        val wires = PracticeMode.entries.map { it.name }
        listOf("SEQUENTIAL", "CHAPTER", "KNOWLEDGE", "DAILY", "WRONG", "MOCK").forEach {
            assertTrue("缺少练习模式 $it", wires.contains(it))
        }
    }
}
