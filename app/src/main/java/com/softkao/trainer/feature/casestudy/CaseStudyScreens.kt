package com.softkao.trainer.feature.casestudy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.ui.rememberCurrentExamId
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.StatTile
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * 案例分析（大题）模块。
 *
 * 大题和选择题的练习方式完全不同：先读案例背景，自己动手写答案，
 * 再逐小问对照参考答案与 AI 深度解析，最后自评。
 * 自评结果走 [com.softkao.trainer.core.data.repo.PracticeRepository.submitCase]，
 * 「没答上」会进错题本、参与间隔复习，与选择题共用同一套掌握度体系。
 */
class CaseStudyViewModel : ViewModel() {

    data class Group(
        val caseId: String,
        val examId: String,
        val title: String,
        val chapter: String,
        val knowledgePoints: List<String>,
        val questions: List<Question>,
        val attempted: Int,
    )

    data class ListState(
        val loading: Boolean = true,
        val examId: String = ExamId.SYSTEM_INTEGRATION.id,
        val groups: List<Group> = emptyList(),
        val query: String = "",
    )

    data class DetailState(
        val loading: Boolean = true,
        val group: Group? = null,
        val revealed: Set<String> = emptySet(),
        /** questionId → 最近一次自评（true=答上了） */
        val graded: Map<String, Boolean> = emptyMap(),
        /** 在错题本里的掌握度，没有记录则为 null */
        val wrongMastery: Map<String, Mastery> = emptyMap(),
    )

    private val _list = MutableStateFlow(ListState())
    val list = _list.asStateFlow()

    private val _detail = MutableStateFlow(DetailState())
    val detail = _detail.asStateFlow()

    init {
        // 全站唯一科目：任何页面切换科目，这里立即跟着刷新
        viewModelScope.launch {
            ServiceLocator.settingsRepository.currentExamId.collect { id -> refresh(id) }
        }
    }

    fun setQuery(q: String) {
        _list.value = _list.value.copy(query = q)
    }

    fun refresh(examId: String) {
        viewModelScope.launch {
            val groups = withContext(Dispatchers.IO) { loadGroups(examId) }
            _list.value = _list.value.copy(loading = false, examId = examId, groups = groups)
        }
    }

    fun loadDetail(examId: String, caseId: String) {
        viewModelScope.launch {
            val group = withContext(Dispatchers.IO) { loadGroups(examId).firstOrNull { it.caseId == caseId } }
            val mastery = withContext(Dispatchers.IO) {
                if (group == null) emptyMap() else wrongMastery(group)
            }
            _detail.value = DetailState(loading = false, group = group, wrongMastery = mastery)
        }
    }

    fun reveal(questionId: String) {
        val s = _detail.value
        _detail.value = s.copy(revealed = s.revealed + questionId)
    }

    /** 自评后记入作答记录与错题本，并刷新列表里的「已自评」进度 */
    fun grade(question: Question, remembered: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ServiceLocator.practiceRepository.submitCase(
                    question = question,
                    remembered = remembered,
                    mode = PracticeMode.CASE,
                    durationMs = 0,
                )
            }
            val s = _detail.value
            val group = s.group ?: return@launch
            _detail.value = s.copy(
                graded = s.graded + (question.id to remembered),
                wrongMastery = withContext(Dispatchers.IO) { wrongMastery(group) },
            )
            _list.value = _list.value.copy(groups = loadGroups(_list.value.examId))
        }
    }

    private fun loadGroups(examId: String): List<Group> {
        val cases = ServiceLocator.questionRepository.list(examId = examId, subject = "案例分析")
        return cases
            .groupBy { it.caseId ?: it.id }
            .map { (caseId, members) ->
                val sorted = members.sortedBy { subNo(it) }
                Group(
                    caseId = caseId,
                    examId = examId,
                    title = groupTitle(sorted.first()),
                    chapter = sorted.first().chapter,
                    knowledgePoints = sorted.mapNotNull { it.knowledgePoint.takeIf(String::isNotBlank) }.distinct(),
                    questions = sorted,
                    attempted = ServiceLocator.practiceRepository.attempted(sorted.map { it.id }).size,
                )
            }
            .sortedWith(compareBy({ it.chapter }, { it.caseId }))
    }

    private fun wrongMastery(group: Group): Map<String, Mastery> =
        ServiceLocator.practiceRepository.wrongEntries(group.examId)
            .filter { it.questionId in group.questions.map { q -> q.id } }
            .associate { it.questionId to it.mastery }

    companion object {
        private val SUB_NO = Regex("小问（(\\d+)）")

        fun subNo(q: Question): Int = SUB_NO.find(q.stem)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE

        /** 案例没有标题字段：用知识点当标题，缺了再退回章节名 */
        fun groupTitle(first: Question): String =
            first.knowledgePoint.takeIf { it.isNotBlank() } ?: first.chapter
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseListScreen(
    onBack: () -> Unit,
    onOpenCase: (examId: String, caseId: String) -> Unit,
    viewModel: CaseStudyViewModel = viewModel(),
) {
    // 科目是全局的，列表跟随当前科目
    val examId = rememberCurrentExamId()
    androidx.compose.runtime.LaunchedEffect(examId) { viewModel.refresh(examId) }

    val state by viewModel.list.collectAsStateWithLifecycle()
    val filtered = remember(state.groups, state.query) {
        if (state.query.isBlank()) state.groups
        else state.groups.filter { g ->
            g.title.contains(state.query, ignoreCase = true) ||
                g.chapter.contains(state.query, ignoreCase = true) ||
                g.knowledgePoints.any { it.contains(state.query, ignoreCase = true) } ||
                g.questions.any { it.stem.contains(state.query, ignoreCase = true) }
        }
    }
    val totalQuestions = state.groups.sumOf { it.questions.size }
    val totalAttempted = state.groups.sumOf { it.attempted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("案例分析 · 大题") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile("案例组", "${state.groups.size} 组", modifier = Modifier.weight(1f))
                        StatTile("小问", "$totalQuestions 问", modifier = Modifier.weight(1f))
                        StatTile("已自评", "$totalAttempted 问", modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "先自己写答案，再逐小问对照解析自评；「没答上」自动进错题本参与复习。当前科目：${
                            ExamId.fromId(state.examId)?.shortName ?: state.examId
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("搜索章节 / 知识点 / 小问内容") },
                )
            }
            items(filtered, key = { it.examId + it.caseId }) { group ->
                Card(
                    onClick = { onOpenCase(group.examId, group.caseId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            group.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TagChip(group.chapter)
                            if (group.attempted == group.questions.size) {
                                TagChip(
                                    "已练完",
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            } else if (group.attempted > 0) {
                                TagChip("自评 ${group.attempted}/${group.questions.size}")
                            } else {
                                TagChip(
                                    "${group.questions.size} 问 · 未练",
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty() && !state.loading) {
                item {
                    SectionCard {
                        Text(
                            if (state.groups.isEmpty()) "当前科目的题库还没有案例分析题，试试在「设置 · 题库」里更新题库"
                            else "没有匹配「${state.query}」的案例，换个关键词试试",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    examId: String,
    caseId: String,
    onBack: () -> Unit,
    viewModel: CaseStudyViewModel = viewModel(),
) {
    androidx.compose.runtime.LaunchedEffect(examId, caseId) { viewModel.loadDetail(examId, caseId) }

    val state by viewModel.detail.collectAsStateWithLifecycle()
    val group = state.group

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.title ?: "案例分析") },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) { Text("‹ 返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (group == null) {
            Text(
                if (state.loading) "加载中…" else "没有找到这个案例",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
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
                SectionCard(title = "案例背景") {
                    Text(group.questions.first().caseStudy.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TagChip(group.chapter)
                        group.knowledgePoints.take(2).forEach { TagChip(it) }
                    }
                }
            }
            items(group.questions, key = { it.id }) { q ->
                SubQuestionCard(
                    index = CaseStudyViewModel.subNo(q),
                    question = q,
                    revealed = q.id in state.revealed,
                    graded = state.graded[q.id],
                    wrongMastery = state.wrongMastery[q.id],
                    onReveal = { viewModel.reveal(q.id) },
                    onGrade = { remembered -> viewModel.grade(q, remembered) },
                )
            }
            item {
                Text(
                    "来源：${group.questions.first().source}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubQuestionCard(
    index: Int,
    question: Question,
    revealed: Boolean,
    graded: Boolean?,
    wrongMastery: Mastery?,
    onReveal: () -> Unit,
    onGrade: (Boolean) -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip("小问 $index")
            if (wrongMastery != null) {
                TagChip(
                    "错题本：${wrongMastery.label}",
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(question.stem, style = MaterialTheme.typography.bodyLarge)

        if (!revealed) {
            Spacer(Modifier.height(10.dp))
            // 大题的规矩：先自己写，再看答案 —— 和模拟卷「交卷才看解析」同一个原则
            FilledTonalButton(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                Text("自己作答后，点此对照参考答案")
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text("参考答案", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
            if (question.aiExplanation.isNullOrBlank().not()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            "AI 深度解析",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(question.aiExplanation.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Text("对照答案自评：", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onGrade(false) },
                    modifier = Modifier.weight(1f),
                ) { Text("没答上", maxLines = 1, softWrap = false) }
                Button(
                    onClick = { onGrade(true) },
                    modifier = Modifier.weight(1f),
                ) { Text("答上了", maxLines = 1, softWrap = false) }
            }
            if (graded != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "已记录。「没答上」会进错题本按间隔复习，连对 3 次自动转为「已掌握」。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
