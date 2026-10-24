package com.softkao.trainer.feature.home

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.PracticeMode
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

class HomeViewModel : ViewModel() {

    data class ExamStat(
        val exam: ExamId,
        val questionCount: Int,
        val answered: Int,
        val correct: Int,
        val accuracy: Float,
        val chapters: List<String>,
        val knowledgePoints: List<Pair<String, Int>> = emptyList(),
    )

    data class State(
        val loading: Boolean = true,
        val exams: List<ExamStat> = emptyList(),
        val todayDone: Int = 0,
        val goal: Int = 20,
        val streak: Int = 0,
        val totalDone: Int = 0,
        val overallAccuracy: Float = 0f,
        val wrongDue: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val goal = withContext(Dispatchers.IO) {
                ServiceLocator.settingsRepository.dailyGoal.first()
            }
            val snapshot = withContext(Dispatchers.IO) {
                val q = ServiceLocator.questionRepository
                val s = ServiceLocator.statsRepository
                val p = ServiceLocator.practiceRepository
                val exams = ExamId.entries.map { exam ->
                    ExamStat(
                        exam = exam,
                        questionCount = q.count(exam.id),
                        answered = s.totalAttempts(exam.id),
                        correct = s.correctAttempts(exam.id),
                        accuracy = s.accuracy(exam.id),
                        chapters = q.chapters(exam.id),
                        knowledgePoints = q.knowledgePointCounts(exam.id).toList(),
                    )
                }
                val extra = Quad(
                    s.attemptsToday(),
                    s.streakDays(),
                    s.accuracy(),
                    p.dueWrongEntries().size,
                )
                exams to extra
            }
            val (examStats, extra) = snapshot
            _state.value = State(
                loading = false,
                exams = examStats,
                todayDone = extra.a,
                goal = goal,
                streak = extra.b,
                totalDone = examStats.sumOf { it.answered },
                overallAccuracy = extra.c,
                wrongDue = extra.d,
            )
        }
    }

    private data class Quad(val a: Int, val b: Int, val c: Float, val d: Int)
}

@Composable
fun HomeScreen(
    onStartPractice: (examId: String, mode: PracticeMode, chapter: String, kp: String, title: String) -> Unit,
    onOpenWrong: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBank: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("软考刷题", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "离线可用 · 题库随 GitHub 更新 · 错题自动分析",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth()) {
                Text("🔍 搜索题目（题干 / 解析 / 知识点）")
            }
        }

        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("今日", "${state.todayDone}/${state.goal}", modifier = Modifier.weight(1f))
                    StatTile("连续打卡", "${state.streak} 天", modifier = Modifier.weight(1f))
                    StatTile("正确率", "${(state.overallAccuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                LabeledProgress(
                    label = "今日目标",
                    value = if (state.goal == 0) 0f else state.todayDone.toFloat() / state.goal,
                    trailing = "${state.todayDone} / ${state.goal} 题",
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenWrong, modifier = Modifier.weight(1f)) { Text("错题本 (${state.wrongDue})") }
                    OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) { Text("统计") }
                    OutlinedButton(onClick = onOpenBank, modifier = Modifier.weight(1f)) { Text("题库") }
                }
            }
        }

        items(state.exams, key = { it.exam.id }) { stat ->
            SectionCard(title = stat.exam.fullName) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("题库", "${stat.questionCount} 题", modifier = Modifier.weight(1f))
                    StatTile("已做", "${stat.answered}", modifier = Modifier.weight(1f))
                    StatTile("正确率", "${(stat.accuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onStartPractice(stat.exam.id, PracticeMode.SEQUENTIAL, "", "", "顺序练习") },
                        modifier = Modifier.weight(1f),
                    ) { Text("顺序练习") }
                    FilledTonalButton(
                        onClick = { onStartPractice(stat.exam.id, PracticeMode.DAILY, "", "", "每日一练") },
                        modifier = Modifier.weight(1f),
                    ) { Text("每日一练") }
                    FilledTonalButton(
                        onClick = { onStartPractice(stat.exam.id, PracticeMode.MOCK, "", "", "模拟卷") },
                        modifier = Modifier.weight(1f),
                    ) { Text("模拟卷") }
                }
                if (stat.chapters.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("按章节练习", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stat.chapters) { chapter ->
                            AssistChip(
                                onClick = { onStartPractice(stat.exam.id, PracticeMode.CHAPTER, chapter, "", "章节 · $chapter") },
                                label = { Text(chapter) },
                            )
                        }
                    }
                }
                if (stat.knowledgePoints.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "按知识点专项练习（共 ${stat.knowledgePoints.size} 个知识点）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(stat.knowledgePoints) { (kp, count) ->
                            AssistChip(
                                onClick = { onStartPractice(stat.exam.id, PracticeMode.KNOWLEDGE, "", kp, "专项 · $kp") },
                                label = { Text("$kp ($count)") },
                            )
                        }
                    }
                }
            }
        }
    }
}
