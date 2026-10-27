package com.softkao.trainer.feature.knowledge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softkao.trainer.core.model.VisualFrame
import com.softkao.trainer.core.model.VisualKind
import com.softkao.trainer.core.model.VisualLesson
import com.softkao.trainer.core.model.VisualNode
import com.softkao.trainer.core.model.VisualTone
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.delay

/**
 * 抽象图解播放器。
 *
 * 每个知识点是一串「帧」，播放器逐帧播放并做淡入过渡，
 * 类似 algorithm-visualizer 用帧序列驱动可视化的做法。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConceptAnimation(
    lesson: VisualLesson,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
) {
    var frameIndex by remember(lesson.id) { mutableIntStateOf(0) }
    var playing by remember(lesson.id) { mutableStateOf(autoPlay) }

    LaunchedEffect(lesson.id, playing) {
        while (playing) {
            delay(2600)
            frameIndex = (frameIndex + 1) % lesson.frames.size
        }
    }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "frameAlpha",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                lesson.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (lesson.keywords.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                // 明确告诉用户这张图讲的是哪些考点，否则看完动画也不知道学到了什么
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    lesson.keywords.take(6).forEach { TagChip(it) }
                }
            }
            Spacer(Modifier.height(8.dp))
            // 要点放在图前面：先知道要记什么，再看动画
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "要点：${lesson.takeaway}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.height(10.dp))

            val frame = lesson.frames[frameIndex.coerceIn(0, lesson.frames.lastIndex)]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                LessonCanvas(frame = frame, alpha = alpha)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                frame.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        frameIndex = if (frameIndex == 0) lesson.frames.lastIndex else frameIndex - 1
                    }) { Text("‹") }
                    TextButton(onClick = { playing = !playing }) { Text(if (playing) "暂停" else "播放") }
                    TextButton(onClick = { frameIndex = (frameIndex + 1) % lesson.frames.size }) { Text("›") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    lesson.frames.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(if (i == frameIndex) 8.dp else 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (i == frameIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "${frameIndex + 1} / ${lesson.frames.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCanvas(frame: VisualFrame, alpha: Float) {
    val measurer = rememberTextMeasurer()
    val colors = LessonColors()

    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val w = size.width
        val h = size.height

        // 先画边
        frame.edges.forEach { e ->
            val from = frame.nodes.firstOrNull { it.id == e.from } ?: return@forEach
            val to = frame.nodes.firstOrNull { it.id == e.to } ?: return@forEach
            val p1 = Offset(from.x * w + from.w * w / 2, from.y * h + from.h * h / 2)
            val p2 = Offset(to.x * w + to.w * w / 2, to.y * h + to.h * h / 2)
            val color = if (e.active) colors.active else colors.muted
            drawLine(
                color = color.copy(alpha = alpha),
                start = p1,
                end = p2,
                strokeWidth = if (e.active) 3.dp.toPx() else 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (e.dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
            )
            // 箭头
            val angle = kotlin.math.atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
            val size = 8.dp.toPx()
            drawLine(
                color = color.copy(alpha = alpha),
                start = p2,
                end = Offset(
                    p2.x - size * kotlin.math.cos(angle).toFloat(),
                    p2.y - size * kotlin.math.sin(angle).toFloat(),
                ),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 再画节点
        frame.nodes.forEach { node ->
            val x = node.x * w
            val y = node.y * h
            val nw = node.w * w
            val nh = node.h * h
            val fill = colors.forTone(node.tone).copy(alpha = alpha)
            when (node.kind) {
                VisualKind.BOX -> {
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(x, y),
                        size = Size(nw, nh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    )
                    if (node.label.isNotBlank()) {
                        drawCenteredText(measurer, node.label, Offset(x + nw / 2, y + nh / 2), colors.onFill)
                    }
                    if (node.caption.isNotBlank()) {
                        drawCenteredText(
                            measurer, node.caption, Offset(x + nw / 2, y + nh + 12.dp.toPx()),
                            colors.muted, 10.sp,
                        )
                    }
                }
                VisualKind.CIRCLE -> {
                    drawCircle(color = fill, radius = minOf(nw, nh) / 2, center = Offset(x + nw / 2, y + nh / 2))
                    if (node.label.isNotBlank()) {
                        drawCenteredText(measurer, node.label, Offset(x + nw / 2, y + nh / 2), colors.onFill)
                    }
                    if (node.caption.isNotBlank()) {
                        drawCenteredText(
                            measurer, node.caption, Offset(x + nw / 2, y + nh + 12.dp.toPx()),
                            colors.muted, 10.sp,
                        )
                    }
                }
                VisualKind.BAR -> {
                    val maxH = h * 0.62f
                    val barH = (node.value.coerceIn(0f, 1f)) * maxH
                    drawRoundRect(
                        color = fill,
                        topLeft = Offset(x, h - barH),
                        size = Size(nw, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                    // 画布外（h+12dp）会被父级 clip 裁掉：caption 必须画在柱顶上方
                    if (node.label.isNotBlank()) {
                        drawCenteredText(measurer, node.label, Offset(x + nw / 2, h - barH - 14.dp.toPx()), colors.onSurface)
                    }
                    if (node.caption.isNotBlank()) {
                        drawCenteredText(measurer, node.caption, Offset(x + nw / 2, h - barH - 30.dp.toPx()), colors.muted, 10.sp)
                    }
                }
                VisualKind.TEXT, VisualKind.LABEL -> {
                    if (node.label.isNotBlank()) {
                        drawCenteredText(measurer, node.label, Offset(x + nw / 2, y + nh / 2), colors.onSurface, 12.sp)
                    }
                    if (node.caption.isNotBlank()) {
                        drawCenteredText(measurer, node.caption, Offset(x + nw / 2, y + nh + 14.dp.toPx()), colors.muted, 10.sp)
                    }
                }
            }
        }
    }
}

private data class LessonColors(
    val neutral: Color = Color(0xFFDDE3EA),
    val active: Color = Color(0xFF2E74B5),
    val good: Color = Color(0xFF3F7D3F),
    val bad: Color = Color(0xFFB3261E),
    val muted: Color = Color(0xFFB9C2CC),
    val path: Color = Color(0xFF1F4E79),
    val onFill: Color = Color.White,
    val onSurface: Color = Color(0xFF1A1C1E),
)

private fun LessonColors.forTone(tone: VisualTone): Color = when (tone) {
    VisualTone.NEUTRAL -> neutral
    VisualTone.ACTIVE -> active
    VisualTone.GOOD -> good
    VisualTone.BAD -> bad
    VisualTone.MUTED -> muted
    VisualTone.PATH -> path
}

private fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    center: Offset,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
) {
    val layout = measurer.measure(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Medium),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
    )
}
