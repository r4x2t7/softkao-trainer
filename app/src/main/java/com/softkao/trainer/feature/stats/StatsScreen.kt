package com.softkao.trainer.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.feature.common.LabeledProgress
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.StatTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val knowledge: List<Triple<String, Int, Int>> = emptyList(),
        val examOverview: List<Pair<ExamId, Pair<Int, Int>>> = emptyList(),
        /** 用户是否已经手动选过科目；用来避免初始化时被异步读取覆盖 */
        val touched: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            // 统计页的科目是本页自己的作用域，跟其它页面互不影响
            val saved = ServiceLocator.settingsRepository.currentExamId.first()
            ExamId.fromId(saved)?.let { exam ->
                if (!_state.value.touched) {
                    _state.value = _state.value.copy(exam = exam)
                    refresh()
                }
            }
        }
    }

    fun selectExam(exam: ExamId) {
        _state.value = _state.value.copy(exam = exam, touched = true)
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(exam.id)
        }
        refresh()
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
                "统计科目（本页独立选择，不影响其它页面）",
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
                    StatTile("总题数", "${state.total}", modifier = Modifier.weight(1f))
                    StatTile("正确率", "${(state.accuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
                    StatTile("连续", "${state.streak} 天", modifier = Modifier.weight(1f))
                    StatTile("今日", "${state.today}", modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            SectionCard(title = "近 7 天做题量（两科合计）") {
                val max = state.trend.maxOfOrNull { it.second.first }?.coerceAtLeast(1) ?: 1
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    state.trend.forEach { (day, counts) ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "${counts.first}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((counts.first * 72 / max).dp.coerceAtLeast(6.dp))
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (counts.first > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
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
