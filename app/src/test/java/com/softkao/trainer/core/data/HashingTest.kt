package com.softkao.trainer.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * sha256 测试。
 *
 * 关键点是 **LF 归一化**：Windows 上写文件是 CRLF、Git 提交是 LF，
 * 不归一化的话 App 端算出的哈希和仓库 index.json 里的对不上，
 * 更新通道会全部拒绝导入。build_index.py 用的也是同一套规则。
 */
class HashingTest {

    @Test
    fun `CRLF 与 LF 内容哈希相同`() {
        val lf = "{\"a\":1}\n{\"b\":2}\n"
        val crlf = lf.replace("\n", "\r\n")
        assertEquals(Hashing.sha256(lf), Hashing.sha256(crlf))
    }

    @Test
    fun `内容不同哈希不同`() {
        assertNotEquals(Hashing.sha256("a"), Hashing.sha256("b"))
        assertNotEquals(Hashing.sha256("a\n"), Hashing.sha256("a"))
    }

    @Test
    fun `哈希是 64 位小写十六进制`() {
        val h = Hashing.sha256("软考刷题")
        assertEquals(64, h.length)
        assertTrue("含非十六进制字符: $h", h.all { it in "0123456789abcdef" })
    }

    @Test
    fun `已知内容的哈希是稳定值`() {
        // 空串的 SHA-256 是公开常量，钉死它可以防住实现被改坏
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Hashing.sha256(""),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hashing.sha256("abc"),
        )
    }
}
