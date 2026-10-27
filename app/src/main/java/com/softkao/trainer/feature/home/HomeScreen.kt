package com.softkao.trainer.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.softkao.trainer.feature.common.TagChip
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
        val caseGroups: Int = 0,
        val caseQuestions: Int = 0,
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
        /** 全站唯一科目，首页跟随它显示 */
        val examId: String = ExamId.SYSTEM_INTEGRATION.id,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        // 订阅全局科目：在任意页面切换科目，首页立刻跟着换
        viewModelScope.launch {
            ServiceLocator.settingsRepository.currentExamId.collect { id ->
                _state.value = _state.value.copy(examId = id)
                refresh()
            }
        }
    }

    /**
     * 切换科目：写入全局设置，全站所有页面同步切换。
     */
    fun setExamId(id: String) {
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(id)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val goal = withContext(Dispatchers.IO) {
                ServiceLocator.settingsRepository.dailyGoal.first()
            }
            val examId = withContext(Dispatchers.IO) {
                ServiceLocator.settingsRepository.currentExamId.first()
            }
            val snapshot = withContext(Dispatchers.IO) {
                val q = ServiceLocator.questionRepository
                val s = ServiceLocator.statsRepository
                val p = ServiceLocator.practiceRepository
                val exams = ExamId.entries.map { exam ->
                    val cases = q.list(exam.id, subject = "案例分析")
                    ExamStat(
                        exam = exam,
                        questionCount = q.count(exam.id),
                        answered = s.totalAttempts(exam.id),
                        correct = s.correctAttempts(exam.id),
                        accuracy = s.accuracy(exam.id),
                        chapters = q.chapters(exam.id),
                        knowledgePoints = q.knowledgePointCounts(exam.id).toList(),
                        caseGroups = cases.mapNotNull { it.caseId }.distinct().size,
                        caseQuestions = cases.size,
                    )
                }
                val extra = Quad(
                    s.attemptsToday(examId),
                    s.streakDays(),
                    s.accuracy(examId),
                    p.dueWrongEntries().size,
                )
                exams to extra
            }
            val (examStats, extra) = snapshot
            _state.value = State(
                loading = false,
                exams = examStats.sortedBy {
                    if (it.exam.id == examId) 0 else 1
                },
                todayDone = extra.a,
                goal = goal,
                streak = extra.b,
                totalDone = examStats.sumOf { it.answered },
                overallAccuracy = extra.c,
                wrongDue = extra.d,
                examId = examId,
            )
        }
    }

    private data class Quad(val a: Int, val b: Int, val c: Float, val d: Int)
}

@Composable
fun HomeScreen(
    onStartPractice: (examId: String, mode: PracticeMode, chapter: String, kp: String, title: String) -> Unit,
    onOpenPicker: (examId: String, tab: Int) -> Unit,
    onOpenWrong: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenBank: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCases: (examId: String) -> Unit = {},
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
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "当前科目",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ExamId.entries.forEach { exam ->
                        FilterChip(
                            selected = state.examId == exam.id,
                            onClick = { viewModel.setExamId(exam.id) },
                            label = { Text(exam.shortName, maxLines = 1, softWrap = false) },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "全站唯一科目：这里切换后，知识点、错题本、统计、搜索会同步切换；两科的题库与记录互不干扰",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("今日", "${state.todayDone}/${state.goal}", modifier = Modifier.weight(1f))
                    StatTile("连续打卡", "${state.streak} 天", modifier = Modifier.weight(1f))
                    StatTile("正确率", "${(state.overallAccuracy * 100).toInt()}%", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "「今日」「正确率」按本页科目统计，连续打卡为两科合计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                LabeledProgress(
                    label = "今日目标",
                    value = if (state.goal == 0) 0f else state.todayDone.toFloat() / state.goal,
                    trailing = "${state.todayDone} / ${state.goal} 题",
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenWrong,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (state.wrongDue > 0) "错题本（待复习 ${state.wrongDue}）" else "错题本") }
                    OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) { Text("统计") }
                    OutlinedButton(onClick = onOpenBank, modifier = Modifier.weight(1f)) { Text("题库") }
                }
            }
        }

        // 两门考试完全隔离：首页只展示当前科目
        items(state.exams.filter { it.exam.id == state.examId }, key = { it.exam.id }) { stat ->
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("顺序练习", maxLines = 1, softWrap = false, fontSize = 13.sp) }
                    FilledTonalButton(
                        onClick = { onStartPractice(stat.exam.id, PracticeMode.DAILY, "", "", "每日一练") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("每日一练", maxLines = 1, softWrap = false, fontSize = 13.sp) }
                    FilledTonalButton(
                        onClick = { onStartPractice(stat.exam.id, PracticeMode.MOCK, "", "", "模拟卷") },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("模拟卷", maxLines = 1, softWrap = false, fontSize = 13.sp) }
                }
                if (stat.chapters.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("按章节 / 按知识点练习", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    // 知识点有上千个，横着滑根本找不到，所以改成跳到纵向列表的选题页
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenPicker(stat.exam.id, 0) },
                            modifier = Modifier.weight(1f),
                        ) { Text("按章节（${stat.chapters.size}）") }
                        OutlinedButton(
                            onClick = { onOpenPicker(stat.exam.id, 1) },
                            modifier = Modifier.weight(1f),
                        ) { Text("按知识点（${stat.knowledgePoints.size}）") }
                    }
                }
                if (stat.caseQuestions > 0) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onOpenCases(stat.exam.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "📝 案例分析 · ${stat.caseGroups} 组大题（${stat.caseQuestions} 问）",
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                if (stat.knowledgePoints.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "共 ${stat.knowledgePoints.size} 个知识点，可在选题页里按名称搜索",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
