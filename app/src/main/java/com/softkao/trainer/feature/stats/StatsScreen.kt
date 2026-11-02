package com.softkao.trainer.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.feature.common.GradientStatTile
import com.softkao.trainer.feature.common.LabeledProgress
import com.softkao.trainer.feature.common.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

class StatsViewModel : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val exam: ExamId = ExamId.SYSTEM_INTEGRATION,
        val total: Int = 0,
        val correct: Int = 0,
        val accuracy: Float = 0f,
        val streak: Int = 0,
        val today: Int = 0,
        val trend: List<Pair<String, Pair<Int, Int>>> = emptyList(),
        val heatmap: List<List<HeatCell?>> = emptyList(),
        val knowledge: List<Triple<String, Int, Int>> = emptyList(),
        val examOverview: List<Pair<ExamId, Pair<Int, Int>>> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    data class HeatCell(val attempts: Int, val correct: Int)

    companion object {
        /**
         * 把 dailyTrend 的有序日序列（旧→新，最后一天=今天）排成「周列 × 周一~周日」的热力图网格。
         * 纯函数，便于 JVM 单测。
         */
        fun buildHeatmapWeeks(
            raw: List<Pair<String, Pair<Int, Int>>>,
            lastDayMillis: Long,
        ): List<List<HeatCell?>> {
            if (raw.isEmpty()) return emptyList()
            val firstCal = java.util.Calendar.getInstance().apply {
                timeInMillis = lastDayMillis - (raw.size - 1) * 86_400_000L
            }
            // 周一开头对齐：周一=0 … 周日=6（Calendar 的 SUNDAY=1）
            val leading = (firstCal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            val cells = mutableListOf<HeatCell?>()
            repeat(leading) { cells.add(null) }
            raw.forEach { entry -> cells.add(HeatCell(entry.second.first, entry.second.second)) }
            while (cells.size % 7 != 0) cells.add(null)
            return cells.chunked(7)
        }
    }

    val state: StateFlow<State> = _state.asStateFlow()

    init {
        // 订阅全局科目：统计跟着当前科目走，切换后立即重算
        viewModelScope.launch {
            ServiceLocator.settingsRepository.currentExamId.collect { id ->
                val exam = ExamId.fromId(id) ?: return@collect
                if (_state.value.exam != exam) {
                    _state.value = _state.value.copy(exam = exam)
                }
                refresh()
            }
        }
    }

    fun selectExam(exam: ExamId) {
        _state.value = _state.value.copy(exam = exam)
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(exam.id)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val exam = _state.value.exam
            val data = withContext(Dispatchers.IO) {
                val s = ServiceLocator.statsRepository
                Triple(
                    Triple(s.totalAttempts(exam.id), s.correctAttempts(exam.id), s.accuracy(exam.id)),
                    Triple(s.streakDays(), s.attemptsToday(exam.id), s.dailyTrend(7)),
                    ExamId.entries.map { it to (s.totalAttempts(it.id) to s.correctAttempts(it.id)) },
                )
            }
            val kp = withContext(Dispatchers.IO) {
                ServiceLocator.statsRepository.knowledgeStats(exam.id)
                    .map { (name, pair) -> Triple(name, pair.first, pair.second) }
                    .sortedByDescending { it.second }
            }
            val heat = withContext(Dispatchers.IO) {
                val raw = ServiceLocator.statsRepository.dailyTrend(119) // 近 17 周，旧 → 新，两科合计
                val today = System.currentTimeMillis()
                buildHeatmapWeeks(raw, today)
            }
            val (core, trend, overview) = data
            _state.value = _state.value.copy(
                loading = false,
                total = core.first,
                correct = core.second,
                accuracy = core.third,
                streak = trend.first,
                today = trend.second,
                trend = trend.third,
                knowledge = kp,
                examOverview = overview,
                heatmap = heat,
            )
        }
    }
}

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("学习统计", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "按知识点看掌握度，比只看总分更有用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "当前科目（全站生效，与首页、知识点、错题本同步）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExamId.entries) { exam ->
                    FilterChip(
                        selected = state.exam == exam,
                        onClick = { viewModel.selectExam(exam) },
                        label = { Text(exam.shortName) },
                    )
                }
            }
        }

        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GradientStatTile(
                        label = "总题数",
                        value = "${state.total}",
                        accent = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f),
                    )
                    GradientStatTile(
                        label = "正确率",
                        value = "${(state.accuracy * 100).toInt()}%",
                        accent = Color(0xFF22D3EE),
                        modifier = Modifier.weight(1f),
                    )
                    GradientStatTile(
                        label = "连续",
                        value = "${state.streak} 天",
                        accent = Color(0xFF34D399),
                        modifier = Modifier.weight(1f),
                    )
                    GradientStatTile(
                        label = "今日",
                        value = "${state.today}",
                        accent = Color(0xFFFB923C),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            SectionCard(title = "近 7 天做题量（两科合计）") {
                TrendChart(
                    points = state.trend.map { (day, counts) ->
                        TrendPoint(day = day, total = counts.first, correct = counts.second)
                    },
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "点柱子看当天明细；绿色小段是当天答对的题数",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard(title = "学习日历 · 近 17 周（两科合计）") {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    state.heatmap.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { cell ->
                                val color = when {
                                    cell == null || cell.attempts == 0 ->
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    cell.attempts <= 2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                    cell.attempts <= 5 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.60f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    Modifier
                                        .size(16.dp)
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                        .background(color),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "少",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf(0.55f, 0.30f, 0.60f, 1f).forEach { a ->
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
                                .background(
                                    if (a == 0.55f) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = a)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = a)
                                ),
                        )
                    }
                    Text(
                        "多（0 / 1-2 / 3-5 / 6+ 题）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard(title = "各科目概览") {
                state.examOverview.forEach { (exam, counts) ->
                    val acc = if (counts.first == 0) 0f else counts.second.toFloat() / counts.first
                    Spacer(Modifier.height(6.dp))
                    LabeledProgress(
                        label = "${exam.fullName}（${counts.first} 题）",
                        value = acc,
                        trailing = "${(acc * 100).toInt()}%",
                    )
                }
            }
        }

        item {
            SectionCard(title = "知识点掌握度") {
                if (state.knowledge.isEmpty()) {
                    Text(
                        "还没有数据，先去做几道题",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.knowledge.take(12).forEach { (name, total, correct) ->
                        val acc = if (total == 0) 0f else correct.toFloat() / total
                        Spacer(Modifier.height(8.dp))
                        LabeledProgress(
                            label = name,
                            value = acc,
                            trailing = "$correct/$total",
                        )
                    }
                }
            }
        }
    }
}
