package com.softkao.trainer.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 索引解析测试。
 *
 * 索引是 App 判断「要不要更新题库」的唯一依据，还额外承载逐知识点精讲的
 * 路径与 sha256；老版本索引没有 kpNotes 字段，必须能正常解析。
 */
class BankIndexParserTest {

    @Test
    fun `解析含知识点精讲索引的新版本索引`() {
        val raw = """
            {"updatedAt":"2026-10-24T20:00:00Z","banks":[
              {"examId":"system-integration","examName":"系统集成项目管理工程师","version":2,
               "file":"system-integration/v2.json","questionCount":1335,
               "sha256":"abc123","updatedAt":"2026-10-24T20:00:00Z",
               "kpNotes":{"file":"system-integration/kp_notes.json","count":1200,"sha256":"def456"}}
            ]}
        """.trimIndent()
        val entries = BankIndexParser.parse(raw)
        assertEquals(1, entries.size)
        val e = entries.first()
        assertEquals("system-integration", e.examId)
        assertEquals(2, e.version)
        assertEquals("system-integration/v2.json", e.file)
        assertEquals(1335, e.questionCount)
        assertEquals("abc123", e.sha256)
        assertEquals("system-integration/kp_notes.json", e.kpNotesFile)
        assertEquals(1200, e.kpNotesCount)
        assertEquals("def456", e.kpNotesSha256)
    }

    @Test
    fun `老索引没有 kpNotes 字段也能解析`() {
        val raw = """
            {"banks":[{"examId":"multimedia-designer","version":1,
             "file":"multimedia-designer/v1.json","questionCount":96,"sha256":"abc"}]}
        """.trimIndent()
        val entries = BankIndexParser.parse(raw)
        assertEquals(1, entries.size)
        val e = entries.first()
        assertEquals("", e.kpNotesFile)
        assertEquals(0, e.kpNotesCount)
        assertEquals("", e.kpNotesSha256)
    }

    @Test
    fun `缺少 file 的条目被丢弃`() {
        val raw = """{"banks":[{"examId":"x","version":1},{"examId":"y","file":"y.json"}]}"""
        val entries = BankIndexParser.parse(raw)
        assertEquals(1, entries.size)
        assertEquals("y", entries.first().examId)
    }

    @Test
    fun `banks 为空时返回空列表`() {
        assertTrue(BankIndexParser.parse("""{"banks":[]}""").isEmpty())
        assertTrue(BankIndexParser.parse("""{}""").isEmpty())
    }

    @Test
    fun `多个题库的条目都保留且顺序不变`() {
        val raw = """
            {"banks":[
              {"examId":"system-integration","file":"si.json","version":2},
              {"examId":"multimedia-designer","file":"md.json","version":2}
            ]}
        """.trimIndent()
        val entries = BankIndexParser.parse(raw)
        assertEquals(2, entries.size)
        assertEquals(listOf("system-integration", "multimedia-designer"), entries.map { it.examId })
    }
}
