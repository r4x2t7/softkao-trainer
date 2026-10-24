package com.softkao.trainer.core.model

/**
 * 抽象图解（可视化知识点）的数据模型。
 *
 * 思路借鉴 algorithm-visualizer（48k★）的核心做法：
 * 把一个知识点的讲解拆成一串「帧」，每帧只描述图形状态，
 * 由播放器在帧之间做动画过渡，而不是手写一整段动画代码。
 */

enum class VisualKind { BOX, CIRCLE, BAR, TEXT, LABEL }

enum class VisualTone { NEUTRAL, ACTIVE, GOOD, BAD, MUTED, PATH }

data class VisualNode(
    val id: String,
    val label: String = "",
    val x: Float,
    val y: Float,
    val w: Float = 0.2f,
    val h: Float = 0.14f,
    val kind: VisualKind = VisualKind.BOX,
    val value: Float = 0f,
    val tone: VisualTone = VisualTone.NEUTRAL,
    val caption: String = "",
)

data class VisualEdge(
    val from: String,
    val to: String,
    val active: Boolean = false,
    val dashed: Boolean = false,
    val label: String = "",
)

data class VisualFrame(
    val caption: String,
    val nodes: List<VisualNode> = emptyList(),
    val edges: List<VisualEdge> = emptyList(),
)

data class VisualLesson(
    val id: String,
    val title: String,
    val examId: String,
    val chapter: String,
    val keywords: List<String>,
    val takeaway: String,
    val frames: List<VisualFrame>,
) {
    /** 该图解能对应到哪些考点（按关键字包含匹配） */
    fun matches(point: String): Boolean {
        if (point.isBlank()) return false
        return keywords.any { point.contains(it, ignoreCase = true) || it.contains(point, ignoreCase = true) }
    }
}
