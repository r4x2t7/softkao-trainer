package com.softkao.trainer.core.data

import android.content.Context
import com.softkao.trainer.core.model.ChapterMaterial
import com.softkao.trainer.core.model.Formula
import org.json.JSONArray
import org.json.JSONObject

/**
 * 知识点精讲资料。优先读内置 assets/materials/<examId>.json，
 * 题库更新时可从远端一并拉取（materialUrl 字段），覆盖本地。
 */
class MaterialRepository(private val context: Context) {

    private val cache = mutableMapOf<String, List<ChapterMaterial>>()

    /** 优先读本地更新过的资料文件，其次读内置 assets */
    fun load(examId: String): List<ChapterMaterial> = cache.getOrPut(examId) {
        val downloaded = readLocalFile(examId)
        if (downloaded.isNotEmpty()) downloaded else readAssets(examId)
    }

    /** 题库更新通道下载下来的资料会写到这里 */
    fun saveLocal(examId: String, raw: String): Boolean = runCatching {
        val dir = java.io.File(context.filesDir, "materials")
        dir.mkdirs()
        java.io.File(dir, "$examId.json").writeText(raw, Charsets.UTF_8)
        cache.remove(examId)
        true
    }.getOrDefault(false)

    private fun readLocalFile(examId: String): List<ChapterMaterial> = runCatching {
        val f = java.io.File(context.filesDir, "materials/$examId.json")
        if (!f.exists()) return@runCatching emptyList()
        parse(f.readText(Charsets.UTF_8))
    }.getOrDefault(emptyList())

    fun find(examId: String, chapter: String): ChapterMaterial? =
        load(examId).firstOrNull { it.chapter == chapter }

    private fun readAssets(examId: String): List<ChapterMaterial> = runCatching {
        val raw = context.assets.open("materials/$examId.json").bufferedReader().use { it.readText() }
        parse(raw)
    }.getOrDefault(emptyList())

    fun parse(raw: String): List<ChapterMaterial> {
        val root = JSONObject(raw)
        val arr = root.optJSONArray("chapters") ?: return emptyList()
        val out = mutableListOf<ChapterMaterial>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val chapter = o.optString("chapter")
            if (chapter.isBlank()) continue
            out += ChapterMaterial(
                chapter = chapter,
                summary = o.optString("summary"),
                examFocus = o.optString("examFocus"),
                keyPoints = stringList(o.optJSONArray("keyPoints")),
                pitfalls = stringList(o.optJSONArray("pitfalls")),
                formulas = formulaList(o.optJSONArray("formulas")),
                memoryTip = o.optString("memoryTip"),
                answerTemplate = o.optString("answerTemplate"),
            )
        }
        return out
    }

    private fun stringList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
    }

    private fun formulaList(arr: JSONArray?): List<Formula> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Formula>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            val formula = o.optString("formula")
            if (name.isBlank() && formula.isBlank()) continue
            out += Formula(name, formula, o.optString("note"))
        }
        return out
    }
}
