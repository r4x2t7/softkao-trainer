package com.softkao.trainer.feature.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 趋势图数据点：[day] = MM-dd，[total] = 当日做题量，[correct] = 当日答对数 */
data class TrendPoint(
    val day: String,
    val total: Int,
    val correct: Int,
)

// ---- 图表配色（明暗两套主题下都能看清）----
private val BarTop = Color(0xFF8B5CF6)      // 紫
private val BarBottom = Color(0xFF22D3EE)   // 青
private val CorrectTop = Color(0xFF34D399)  // 薄荷绿
private val CorrectBottom = Color(0xFF059669)
private val LineColor = Color(0xFFFB923C)   // 琥珀
private val LineGlow = Color(0xFFFCD34D)

private const val CHART_HEIGHT_DP = 210f
private const val GUTTER_DP = 30f        // 左侧 Y 轴留白
private const val HEADROOM_DP = 22f      // 顶部留给数值标签
private const val AXIS_BAND_DP = 22f     // 底部 X 轴文字带
private const val RIGHT_PAD_DP = 6f

/** 让坐标轴刻度落在整数上：4 / 8 / 12 / 16 / 20 / 24 / 32 ... */
private val NICE_MAXS = intArrayOf(
    4, 8, 12, 16, 20, 24, 32, 40, 48, 60, 80, 100, 120, 160, 200, 240, 320, 400, 500, 600, 800, 1000,
)

internal fun niceMax(v: Int): Int {
    if (v <= 4) return 4
    NICE_MAXS.firstOrNull { it >= v }?.let { return it }
    return ((v + 399) / 400) * 400
}

/**
 * 近 N 天做题量图表。
 *
 * - 柱体：紫→青竖向渐变，底部嵌套薄荷绿「答对数」段
 * - 趋势线：穿过柱顶的发光平滑曲线，带节点
 * - 0 值：不画柱，只在基线上留一个空心圆点（避免出现难看的"台座"）
 * - X 轴日期用 TextMeasurer 直接画在画布上，永远不会折行
 * - 点击任意一天弹出详情气泡
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "暂无做题记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val measurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val inverseSurface = MaterialTheme.colorScheme.inverseSurface
    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface

    val gridColor = onSurfaceVariant.copy(alpha = 0.22f)
    val baselineColor = onSurfaceVariant.copy(alpha = 0.45f)
    val axisTextStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = onSurfaceVariant,
    )
    val valueTextStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = onSurface,
    )
    val todayAxisStyle = axisTextStyle.copy(fontWeight = FontWeight.Bold, color = primary)
    val tipTitleStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = inverseOnSurface,
    )
    val tipBodyStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        color = inverseOnSurface.copy(alpha = 0.85f),
    )

    val grow = remember { Animatable(0f) }
    var selected by remember(points) { mutableIntStateOf(-1) }

    val infinite = rememberInfiniteTransition(label = "trendSweep")
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    LaunchedEffect(points) {
        grow.snapTo(0f)
        grow.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT_DP.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val left = GUTTER_DP.dp.toPx()
                        val slot = (size.width - left) / points.size
                        val idx = floor((offset.x - left) / slot).toInt()
                            .coerceIn(0, points.size - 1)
                        selected = if (selected == idx) -1 else idx
                    }
                },
        ) {
            val n = points.size
            val axisLeft = GUTTER_DP.dp.toPx()
            val plotRight = size.width - RIGHT_PAD_DP.dp.toPx()
            val plotTop = HEADROOM_DP.dp.toPx()
            val plotBottom = size.height - AXIS_BAND_DP.dp.toPx()
            val plotWidth = max(1f, plotRight - axisLeft)
            val plotHeight = max(1f, plotBottom - plotTop)
            val slot = plotWidth / n
            val barWidth = min(slot * 0.52f, 26.dp.toPx())
            val maxValue = niceMax(points.maxOf { it.total })

            fun centerX(i: Int): Float = axisLeft + slot * (i + 0.5f)

            // 每根柱的入场进度（错开一点，更有节奏感）
            val delayStep = if (n > 1) 0.5f / n else 0f
            val progress = FloatArray(n) { i ->
                val local = ((grow.value - i * delayStep) / 0.5f).coerceIn(0f, 1f)
                FastOutSlowInEasing.transform(local)
            }

            // ---------- 网格线 + Y 轴刻度 ----------
            for (i in 0..4) {
                val frac = i / 4f
                val y = plotBottom - frac * plotHeight
                val value = (maxValue * frac).toInt()
                if (i == 0) {
                    // 基线：实线、更实
                    drawLine(
                        color = baselineColor,
                        start = Offset(axisLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                } else {
                    drawLine(
                        color = gridColor,
                        start = Offset(axisLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                        ),
                    )
                }
                val layout = measurer.measure(text = value.toString(), style = axisTextStyle)
                drawText(
                    measurer,
                    value.toString(),
                    Offset(axisLeft - 6.dp.toPx() - layout.size.width, y - layout.size.height / 2f),
                    axisTextStyle,
                )
            }

            // ---------- 选中列的高亮背景 ----------
            if (selected in points.indices) {
                val cx = centerX(selected)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.16f), primary.copy(alpha = 0.02f)),
                        startY = plotTop - 8.dp.toPx(),
                        endY = plotBottom,
                    ),
                    topLeft = Offset(cx - slot * 0.46f, plotTop - 8.dp.toPx()),
                    size = Size(slot * 0.92f, plotHeight + 8.dp.toPx()),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                )
            }

            // ---------- 柱体 ----------
            val barTops = FloatArray(n)
            points.forEachIndexed { i, point ->
                val cx = centerX(i)
                val full = plotHeight * (point.total.toFloat() / maxValue)
                val h = full * progress[i]
                barTops[i] = plotBottom - h
                if (point.total <= 0) {
                    // 0 值：只在基线上点一个空心圆，不再画难看的短台座
                    val r = 3.dp.toPx()
                    drawCircle(
                        color = onSurfaceVariant.copy(alpha = 0.35f),
                        radius = r,
                        center = Offset(cx, plotBottom),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    val layout = measurer.measure(text = "0", style = axisTextStyle)
                    drawText(
                        measurer,
                        "0",
                        Offset(cx - layout.size.width / 2f, plotBottom - 6.dp.toPx() - layout.size.height),
                        axisTextStyle.copy(color = onSurfaceVariant.copy(alpha = 0.55f)),
                    )
                    return@forEachIndexed
                }

                val top = barTops[i]
                val radius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                val left = cx - barWidth / 2f

                // 顶部光晕
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BarTop.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(cx, top),
                        radius = barWidth * 1.25f,
                    ),
                    radius = barWidth * 1.25f,
                    center = Offset(cx, top),
                )

                // 总量柱：紫→青竖向渐变
                val barPath = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = left,
                            top = top,
                            right = cx + barWidth / 2f,
                            bottom = plotBottom,
                            topLeftCornerRadius = radius,
                            topRightCornerRadius = radius,
                            bottomLeftCornerRadius = radius,
                            bottomRightCornerRadius = radius,
                        ),
                    )
                }
                drawPath(
                    path = barPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BarTop, BarBottom),
                        startY = top,
                        endY = plotBottom,
                    ),
                )

                // 嵌套的「答对数」段
                if (point.correct > 0) {
                    val innerW = barWidth * 0.46f
                    val innerH = (h * (point.correct.toFloat() / point.total)).coerceAtLeast(3.dp.toPx())
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(CorrectTop, CorrectBottom),
                            startY = plotBottom - innerH,
                            endY = plotBottom,
                        ),
                        topLeft = Offset(cx - innerW / 2f, plotBottom - innerH),
                        size = Size(innerW, innerH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }

                // 高亮柱的流光扫过
                val highlight = selected == i || (selected < 0 && i == points.indexOfLast { it.total > 0 })
                if (highlight && h > 4.dp.toPx()) {
                    clipPath(barPath) {
                        val bandW = 14.dp.toPx()
                        val travel = barWidth + bandW * 2f
                        val sx = left - bandW + travel * sweep
                        rotate(degrees = 16f, pivot = Offset(sx, top + h / 2f)) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.38f),
                                        Color.Transparent,
                                    ),
                                    start = Offset(sx - bandW / 2f, 0f),
                                    end = Offset(sx + bandW / 2f, 0f),
                                ),
                                topLeft = Offset(sx - bandW / 2f, top),
                                size = Size(bandW, h),
                            )
                        }
                    }
                }

                // 柱顶数值
                val layout = measurer.measure(text = point.total.toString(), style = valueTextStyle)
                drawText(
                    measurer,
                    point.total.toString(),
                    Offset(cx - layout.size.width / 2f, top - 5.dp.toPx() - layout.size.height),
                    valueTextStyle,
                )
            }

            // ---------- 趋势线（只连非 0 的天） ----------
            val nodes = ArrayList<Pair<Offset, Int>>(n)
            points.forEachIndexed { i, point ->
                if (point.total > 0) nodes += Offset(centerX(i), barTops[i]) to i
            }
            if (nodes.size >= 2) {
                val path = Path()
                path.moveTo(nodes[0].first.x, nodes[0].first.y)
                for (k in 1 until nodes.size) {
                    val prev = nodes[k - 1].first
                    val cur = nodes[k].first
                    val midX = (prev.x + cur.x) / 2f
                    path.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
                val measure = PathMeasure()
                measure.setPath(path, false)
                if (measure.length > 0f) {
                    val drawn = Path()
                    measure.getSegment(0f, measure.length * grow.value, drawn, true)
                    // 外发光
                    drawPath(
                        path = drawn,
                        color = LineGlow.copy(alpha = 0.22f),
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // 主线
                    drawPath(
                        path = drawn,
                        brush = Brush.horizontalGradient(
                            colors = listOf(LineColor, LineGlow, LineColor),
                            startX = nodes.first().first.x - 30.dp.toPx(),
                            endX = nodes.last().first.x + 30.dp.toPx(),
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // 节点
                    nodes.forEach { (pos, idx) ->
                        if (progress[idx] > 0.9f) {
                            drawCircle(color = LineColor.copy(alpha = 0.28f), radius = 7.dp.toPx(), center = pos)
                            drawCircle(color = Color.White, radius = 4.2.dp.toPx(), center = pos)
                            drawCircle(color = LineColor, radius = 3.2.dp.toPx(), center = pos)
                        }
                    }
                }
            }

            // ---------- X 轴日期（画在画布上，永不折行） ----------
            points.forEachIndexed { i, point ->
                val cx = centerX(i)
                val style = if (i == n - 1) todayAxisStyle else axisTextStyle
                val layout = measurer.measure(text = point.day, style = style)
                drawText(
                    measurer,
                    point.day,
                    Offset(cx - layout.size.width / 2f, plotBottom + (AXIS_BAND_DP.dp.toPx() - layout.size.height) / 2f),
                    style,
                )
            }

            // ---------- 选中气泡 ----------
            if (selected in points.indices) {
                val point = points[selected]
                val cx = centerX(selected)
                val top = barTops[selected]
                val acc = if (point.total == 0) 0 else point.correct * 100 / point.total
                val lines = listOf(
                    point.day to tipTitleStyle,
                    "共 ${point.total} 题" to tipBodyStyle,
                    "答对 ${point.correct} · ${acc}%" to tipBodyStyle,
                )
                val layouts = lines.map { measurer.measure(text = it.first, style = it.second) }
                val padH = 10.dp.toPx()
                val padV = 8.dp.toPx()
                val gap = 2.dp.toPx()
                val boxW = layouts.maxOf { it.size.width } + padH * 2f
                val boxH = layouts.sumOf { it.size.height } + gap * (lines.size - 1) + padV * 2f
                var bx = cx - boxW / 2f
                bx = bx.coerceIn(2.dp.toPx(), max(2.dp.toPx(), size.width - boxW - 2.dp.toPx()))
                var by = top - boxH - 12.dp.toPx()
                val below = by < 0.dp.toPx()
                if (below) by = top + 10.dp.toPx()

                drawRoundRect(
                    color = inverseSurface.copy(alpha = 0.96f),
                    topLeft = Offset(bx, by),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                )
                drawRoundRect(
                    color = LineColor.copy(alpha = 0.65f),
                    topLeft = Offset(bx, by),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx()),
                )
                var ty = by + padV
                lines.forEachIndexed { li, (txt, style) ->
                    drawText(measurer, txt, Offset(bx + padH, ty), style)
                    ty += layouts[li].size.height + gap
                }
                // 小尖角
                val tipX = (cx - bx).coerceIn(10.dp.toPx(), boxW - 10.dp.toPx())
                val tip = Path().apply {
                    if (below) {
                        moveTo(bx + tipX - 5.dp.toPx(), by)
                        lineTo(bx + tipX + 5.dp.toPx(), by)
                        lineTo(bx + tipX, by - 6.dp.toPx())
                    } else {
                        moveTo(bx + tipX - 5.dp.toPx(), by + boxH)
                        lineTo(bx + tipX + 5.dp.toPx(), by + boxH)
                        lineTo(bx + tipX, by + boxH + 6.dp.toPx())
                    }
                    close()
                }
                drawPath(tip, color = inverseSurface.copy(alpha = 0.96f))
            }
        }

        Spacer(Modifier.height(10.dp))
        ChartLegend()
    }
}

/** 图例：总量 / 答对 / 趋势线 */
@Composable
private fun ChartLegend() {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendBar()
        Text("做题量", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        LegendBar(colors = listOf(CorrectTop, CorrectBottom))
        Text("答对数", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
        LegendLine()
        Text("趋势", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
    }
}

@Composable
private fun LegendBar(colors: List<Color> = listOf(BarTop, BarBottom)) {
    Box(
        Modifier
            .size(width = 14.dp, height = 10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.verticalGradient(colors)),
    )
}

@Composable
private fun LegendLine() {
    Box(
        Modifier
            .width(16.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Brush.horizontalGradient(listOf(LineColor, LineGlow))),
    )
}

/** 画布辅助：把 [DrawScope] 里的文本测量样式统一 */
internal fun TextStyle.bold(color: Color): TextStyle = copy(fontWeight = FontWeight.Bold, color = color)
