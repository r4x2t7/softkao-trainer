package com.softkao.trainer.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.data.LearnRepository
import com.softkao.trainer.core.data.LearnRepository.LearnChapter
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.ui.rememberCurrentExamId
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.StatTile
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 学习中心：按最新考试大纲（2024 启用版 / 第 3 版教程）组织的学习路径。
 * 每章 = 大纲要求 + 大白话讲解 + 直观记忆钩子 + 关联练习与精讲入口；
 * 掌握度直接取自练习统计（做过多少题、正确率多少），不做额外的打卡存储。
 */
class LearningViewModel : ViewModel() {

    data class ChapterMastery(
        val questionCount: Int = 0,
        val answered: Int = 0,
        val accuracy: Float = 0f,
    )

    data class State(
        val loading: Boolean = true,
        val examId: String = ExamId.SYSTEM_INTEGRATION.id,
        val chapters: List<LearnChapter> = emptyList(),
        val mastery: Map<String, ChapterMastery> = emptyMap(),
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ServiceLocator.settingsRepository.currentExamId.collect { id ->
                refresh(id)
            }
        }
    }

    fun refresh(examId: String) {
        viewModelScope.launch {
            val chapters = LearnRepository.forExam(examId)
            val mastery = withContext(Dispatchers.IO) { computeMastery(examId, chapters) }
            _state.value = State(loading = false, examId = examId, chapters = chapters, mastery = mastery)
        }
    }

    private fun computeMastery(
        examId: String,
        chapters: List<LearnChapter>,
    ): Map<String, ChapterMastery> {
        val stats = ServiceLocator.statsRepository.chapterStats(examId)
        // chapterStats: 章节 → (题量, 作答数, 正确数)
        return chapters.associate { ch ->
            var total = 0
            var answered = 0
            var correct = 0
            ch.bankChapters.forEach { bc ->
                stats[bc]?.let { (cnt, ans, ok) ->
                    total += cnt
                    answered += ans
                    correct += ok
                }
            }
            ch.id to ChapterMastery(
                questionCount = total,
                answered = answered,
                accuracy = if (answered == 0) 0f else correct.toFloat() / answered,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningScreen(
    onBack: () -> Unit,
    onOpenChapter: (examId: String, chapterId: String) -> Unit,
    viewModel: LearningViewModel = viewModel(),
) {
    val examId = rememberCurrentExamId()
    androidx.compose.runtime.LaunchedEffect(examId) { viewModel.refresh(examId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学习中心") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("‹ 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Text(
                        "按最新考试大纲（2024 年启用 · 第 3 版教程）组织的学习路径：" +
                            "每章给你大纲要求、大白话讲解、直观记忆钩子，学完直接跳本章练习。" +
                            "掌握度来自你的练习记录。当前科目：${
                                ExamId.fromId(state.examId)?.shortName ?: state.examId
                            }",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            "章节",
                            "${state.chapters.size} 章",
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            "已练过",
                            "${state.mastery.values.count { it.answered > 0 }} 章",
                            modifier = Modifier.weight(1f),
                        )
                        StatTile(
                            "题库总题量",
                            "${state.mastery.values.sumOf { it.questionCount }} 题",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            items(state.chapters, key = { it.id }) { ch ->
                val m = state.mastery[ch.id] ?: LearningViewModel.ChapterMastery()
                Card(
                    onClick = { onOpenChapter(state.examId, ch.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TagChip("第 ${ch.order} 章")
                            Text(
                                ch.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            ch.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val byLevel = ch.sections.groupBy { it.level }
                            TagChip("掌握 ${byLevel["掌握"]?.size ?: 0}")
                            TagChip(
                                "熟悉 ${byLevel["熟悉"]?.size ?: 0}",
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TagChip(
                                "了解 ${byLevel["了解"]?.size ?: 0}",
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (m.answered > 0) "已练 ${m.answered} 题 · 正确率 ${(m.accuracy * 100).toInt()}%"
                            else "本章 ${m.questionCount} 题还没练",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterLearnScreen(
    examId: String,
    chapterId: String,
    onBack: () -> Unit,
    onPractice: (examId: String, chapter: String) -> Unit,
    onOpenChapterDetail: (examId: String, chapter: String) -> Unit,
    viewModel: LearningViewModel = viewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(examId) { viewModel.refresh(examId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ch = state.chapters.firstOrNull { it.id == chapterId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ch?.let { "第 ${it.order} 章 · ${it.title}" } ?: "学习") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("‹ 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (ch == null) {
            Text("加载中…", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(title = "本章在讲什么") {
                    Text(ch.summary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionCard(title = "大纲要求") {
                    ch.sections.forEach { s ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            LevelChip(s.level)
                            Text(s.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                SectionCard(title = "怎么考") {
                    Text(ch.howTested, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionCard(title = "直观记忆") {
                    ch.hooks.forEach { hook ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Text(hook, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
            item {
                SectionCard(title = "学完就练") {
                    Text(
                        "本章对应题库章节：${ch.bankChapters.joinToString("、")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    ch.bankChapters.take(3).forEach { bc ->
                        Button(
                            onClick = { onPractice(examId, bc) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                        ) { Text("练习「$bc」", maxLines = 1, softWrap = false) }
                    }
                    if (ch.bankChapters.size > 3) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "其余 ${ch.bankChapters.size - 3} 个关联章节可在「知识点」页按章节进入",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { onOpenChapterDetail(examId, ch.bankChapters.first()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("本章精讲 · 图解 · 知识点") }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(level: String) {
    val (bg, fg) = when (level) {
        "掌握" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "熟悉" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    TagChip(level, color = bg, contentColor = fg)
}
