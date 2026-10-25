package com.softkao.trainer.feature.wrong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.StatTile

/**
 * 错题本的筛选区。
 *
 * 三条硬要求：
 * 1. **chip 上直接带数量**——点之前就知道会筛出什么，不会「点了没反应」
 * 2. **一次只有一个视图生效**（错题 / 收藏 / 某个掌握度），chip 选中态永远说实话
 * 3. **用 FlowRow 不用横向滑动**——之前「已掌握」被挤出屏幕外，用户根本点不到
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongFilterBar(
    filter: WrongFilterState,
    examId: ExamId?,
    masteryCounts: Map<Mastery, Int>,
    bookmarkCount: Int,
    totalWrong: Int,
    visibleCount: Int,
    onSelectMode: (WrongListMode) -> Unit,
    onSelectMastery: (Mastery?) -> Unit,
    onExamChange: (ExamId) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter.mode == WrongListMode.WRONG && filter.mastery == null,
                onClick = { onSelectMastery(null) },
                label = { Text("全部错题 $totalWrong") },
            )
            Mastery.entries.filter { it != Mastery.NEW }.forEach { m ->
                FilterChip(
                    selected = filter.mode == WrongListMode.WRONG && filter.mastery == m,
                    onClick = { onSelectMastery(if (filter.mastery == m) null else m) },
                    label = { Text("${m.label} ${masteryCounts[m] ?: 0}") },
                )
            }
            FilterChip(
                selected = filter.mode == WrongListMode.BOOKMARK,
                onClick = {
                    onSelectMode(if (filter.mode == WrongListMode.BOOKMARK) WrongListMode.WRONG else WrongListMode.BOOKMARK)
                },
                label = { Text("★ 收藏 $bookmarkCount") },
            )
        }

        Spacer(Modifier.height(10.dp))

        // 当前视图摘要：让「点完发生了什么」一眼可见
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "当前：${filter.label} · ${examId?.shortName ?: "全部科目"} · $visibleCount 题",
                style = MaterialTheme.typography.labelMedium,
                color = if (filter.isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (filter.isFiltered) {
                TextButton(onClick = onClear) { Text("清除筛选") }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "科目（全局唯一，切换后首页、知识点、统计、搜索一起换）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExamId.entries.forEach { exam ->
                FilterChip(
                    selected = examId == exam,
                    onClick = { onExamChange(exam) },
                    label = { Text(exam.shortName) },
                )
            }
        }
    }
}

/**
 * 空状态。
 *
 * 之前空结果就是一页白，连「为什么空」都不说；现在按原因分情况给话和下一步。
 */
@Composable
fun WrongEmptyState(
    filter: WrongFilterState,
    totalWrong: Int,
    examName: String?,
    onClear: () -> Unit,
    onPractice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        val (title, reason) = when {
            filter.mode == WrongListMode.BOOKMARK -> "还没有收藏的题" to
                "在练习页点「收藏」，把想反复看的题存进来，就会在这一栏里。"
            filter.mastery != null -> "没有「${filter.mastery.label}」的错题" to
                "当前科目下 $totalWrong 道错题里没有这一档。换一档看看，或者再练几道题。"
            totalWrong > 0 -> "这个科目下还没有错题" to
                "答错的题会自动收进来，并按 1 / 3 / 7 / 15 天安排复习。"
            else -> "还没有错题" to
                "答错的题会自动收进来，并按 1 / 3 / 7 / 15 天安排复习。先去练几道题，答错后这里就会出现复习计划。"
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filter.isFiltered) {
                TextButton(onClick = onClear) { Text("看全部错题") }
            }
            TextButton(onClick = onPractice) { Text("去练${examName ?: ""}题") }
        }
    }
}

/** 顶部三个数据块（按科目范围统计，不随筛选变化，避免数字跟着筛选跳） */
@Composable
fun WrongOverviewRow(
    total: Int,
    due: Int,
    mastered: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile("错题", "$total", modifier = Modifier.weight(1f))
        StatTile("待复习", "$due", modifier = Modifier.weight(1f))
        StatTile("已掌握", "$mastered", modifier = Modifier.weight(1f))
    }
}
