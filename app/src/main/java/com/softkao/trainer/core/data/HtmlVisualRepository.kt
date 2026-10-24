package com.softkao.trainer.core.data

import android.content.Context
import com.softkao.trainer.core.model.HtmlVisual
import org.json.JSONObject

/**
 * 静态图解（HTML 卡片）仓库。
 *
 * 素材放在 `assets/visuals/`，由 `assets/visuals/index.json` 给出清单：
 * 每条含科目、章节、标题、一句话说明、标签，以及亮色/暗色两个文件名。
 * 全部离线，不需要网络。
 */
class HtmlVisualRepository(private val context: Context) {

    private var cache: List<HtmlVisual>? = null

    fun all(): List<HtmlVisual> = cache ?: load().also { cache = it }

    fun forExam(examId: String): List<HtmlVisual> = all().filter { it.examId == examId }

    /** 章节名可能与题库章节不完全一致，这里做「包含即算」的宽松匹配 */
    fun forChapter(examId: String, chapter: String): List<HtmlVisual> {
        if (chapter.isBlank()) return emptyList()
        return forExam(examId).filter { v ->
            v.chapter == chapter ||
                chapter.contains(v.chapter, ignoreCase = true) ||
                v.chapter.contains(chapter, ignoreCase = true)
        }
    }

    fun byId(id: String): HtmlVisual? = all().firstOrNull { it.id == id }

    private fun load(): List<HtmlVisual> = runCatching {
        val raw = context.assets.open("visuals/index.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(raw).optJSONArray("visuals") ?: return emptyList()
        val out = mutableListOf<HtmlVisual>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("id")
            val file = o.optString("file")
            if (id.isBlank() || file.isBlank()) continue
            out += HtmlVisual(
                id = id,
                examId = o.optString("examId"),
                chapter = o.optString("chapter"),
                title = o.optString("title"),
                caption = o.optString("caption"),
                tags = (0 until (o.optJSONArray("tags")?.length() ?: 0)).map {
                    o.optJSONArray("tags")!!.optString(it)
                }.filter { it.isNotBlank() },
                file = file,
                darkFile = o.optString("darkFile"),
            )
        }
        out
    }.getOrDefault(emptyList())
}
