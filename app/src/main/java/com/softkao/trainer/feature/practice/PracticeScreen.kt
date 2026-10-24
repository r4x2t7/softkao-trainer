package com.softkao.trainer.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.data.remote.AiExplainer
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.QuestionType
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeViewModel(
    private val examId: String,
    private val mode: PracticeMode,
    private val chapter: String,
    private val knowledgePoint: String,
    private val presetIds: List<String>,
    private val title: String,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val title: String = "",
        val mode: PracticeMode = PracticeMode.SEQUENTIAL,
        val questions: List<Question> = emptyList(),
        val index: Int = 0,
        val selected: Set<String> = emptySet(),
        val submitted: Boolean = false,
        val correct: Boolean = false,
        val bookmarked: Boolean = false,
        val aiText: String? = null,
        val aiLoading: Boolean = false,
        val aiError: String? = null,
        val answeredFlags: List<Boolean?> = emptyList(),
        val finished: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var startedAt = System.currentTimeMillis()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val questions = withContext(Dispatchers.IO) { buildQuestionSet() }
            _state.value = _state.value.copy(
                loading = false,
                title = title.ifBlank { defaultTitle() },
                mode = mode,
                questions = questions,
                answeredFlags = List(questions.size) { null },
                index = 0,
                selected = emptySet(),
                submitted = false,
            )
            refreshBookmark()
        }
    }

    private fun defaultTitle(): String = when (mode) {
        PracticeMode.SEQUENTIAL -> "顺序练习"
        PracticeMode.CHAPTER -> "章节练习 · $chapter"
        PracticeMode.KNOWLEDGE -> "专项练习 · $knowledgePoint"
        PracticeMode.DAILY -> "每日一练"
        PracticeMode.WRONG -> "错题重做"
        PracticeMode.MOCK -> "模拟卷"
    }

    private fun buildQuestionSet(): List<Question> {
        val repo = ServiceLocator.questionRepository
        val practice = ServiceLocator.practiceRepository
        if (presetIds.isNotEmpty()) return repo.byIds(presetIds)

        return when (mode) {
            PracticeMode.CHAPTER -> repo.list(examId = examId, chapter = chapter, limit = 100)
            PracticeMode.KNOWLEDGE -> repo.list(examId = examId, knowledgePoint = knowledgePoint, limit = 100)
            PracticeMode.WRONG -> {
                val ids = practice.wrongEntries(examId).map { it.questionId }
                repo.byIds(ids)
            }
            PracticeMode.DAILY -> repo.list(examId = examId).shuffled().take(20)
            PracticeMode.MOCK -> repo.list(examId = examId).shuffled().take(40)
            PracticeMode.SEQUENTIAL -> repo.list(examId = examId, limit = 100)
        }
    }

    private fun refreshBookmark() {
        val q = currentQuestion ?: return
        _state.value = _state.value.copy(bookmarked = ServiceLocator.practiceRepository.isBookmarked(q.id))
    }

    private val currentQuestion: Question? get() = _state.value.questions.getOrNull(_state.value.index)

    fun select(key: String) {
        val s = _state.value
        if (s.submitted) return
        val q = currentQuestion ?: return
        val next = when (q.type) {
            // 案例题每题只对应一个小问，按单选处理，避免误判
            QuestionType.SINGLE, QuestionType.TRUE_FALSE, QuestionType.CASE -> setOf(key)
            QuestionType.MULTI -> if (key in s.selected) s.selected - key else s.selected + key
        }
        _state.value = s.copy(selected = next)
    }

    fun submit() {
        val s = _state.value
        if (s.submitted || s.selected.isEmpty()) return
        val q = currentQuestion ?: return
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            val correct = withContext(Dispatchers.IO) {
                ServiceLocator.practiceRepository.submit(q, s.selected.toList(), s.mode, duration)
            }
            _state.value = _state.value.copy(
                submitted = true,
                correct = correct,
                answeredFlags = _state.value.answeredFlags.toMutableList().also { it[_state.value.index] = correct },
            )
        }
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.questions.size) {
            _state.value = s.copy(finished = true)
            return
        }
        startedAt = System.currentTimeMillis()
        _state.value = s.copy(index = s.index + 1, selected = emptySet(), submitted = false, aiText = null, aiError = null)
        refreshBookmark()
    }

    fun prev() {
        val s = _state.value
        if (s.index == 0) return
        startedAt = System.currentTimeMillis()
        _state.value = s.copy(index = s.index - 1, selected = emptySet(), submitted = false, aiText = null, aiError = null)
        refreshBookmark()
    }

    fun jumpTo(target: Int) {
        val s = _state.value
        if (target !in s.questions.indices) return
        startedAt = System.currentTimeMillis()
        _state.value = s.copy(index = target, selected = emptySet(), submitted = false, aiText = null, aiError = null)
        refreshBookmark()
    }

    fun toggleBookmark() {
        val q = currentQuestion ?: return
        val now = ServiceLocator.practiceRepository.toggleBookmark(q.id, q.examId)
        _state.value = _state.value.copy(bookmarked = now)
    }

    fun requestAi() {
        val q = currentQuestion ?: return
        if (_state.value.aiLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(aiLoading = true, aiError = null, aiText = null)
            val settings = ServiceLocator.settingsRepository
            val baseUrl = settings.aiBaseUrl.first()
            val apiKey = settings.aiApiKey.first()
            val model = settings.aiModel.first()
            val userAnswer = _state.value.selected.toList()
            when (val result = ServiceLocator.aiExplainer.explain(baseUrl, apiKey, model, q, userAnswer)) {
                is AiExplainer.Result.Success ->
                    _state.value = _state.value.copy(aiLoading = false, aiText = result.text)
                is AiExplainer.Result.Failure ->
                    _state.value = _state.value.copy(aiLoading = false, aiError = result.message)
                AiExplainer.Result.NotConfigured ->
                    _state.value = _state.value.copy(aiLoading = false, aiError = "未配置 AI 接口，请到「题库」页填写")
            }
        }
    }

    companion object {
        fun factory(
            examId: String,
            mode: PracticeMode,
            chapter: String,
            knowledgePoint: String,
            presetIds: List<String>,
            title: String,
        ) = viewModelFactory {
            initializer { PracticeViewModel(examId, mode, chapter, knowledgePoint, presetIds, title) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    examId: String,
    mode: String,
    chapter: String,
    knowledgePoint: String,
    title: String,
    presetIds: String,
    onBack: () -> Unit,
) {
    val ids = remember(presetIds) { presetIds.split(",").filter { it.isNotBlank() } }
    val practiceMode = remember(mode) { PracticeMode.fromWire(mode) }
    val vm: PracticeViewModel = viewModel(
        factory = PracticeViewModel.factory(
            examId = examId,
            mode = practiceMode,
            chapter = chapter,
            knowledgePoint = knowledgePoint,
            presetIds = ids,
            title = title,
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.title.ifBlank { "练习" }, style = MaterialTheme.typography.titleMedium)
                        if (state.questions.isNotEmpty()) {
                            Text(
                                "第 ${state.index + 1} / ${state.questions.size} 题",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 返回") }
                },
                actions = {
                    TextButton(onClick = { vm.toggleBookmark() }) {
                        Text(if (state.bookmarked) "★ 已收藏" else "☆ 收藏")
                    }
                    TextButton(onClick = { showSheet = true }) { Text("答题卡") }
                },
            )
        },
    ) { padding ->
        val q = state.questions.getOrNull(state.index)
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("加载中…") }
            return@Scaffold
        }
        if (q == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("这一组没有题目", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "换个练习方式，或去题库页检查题库是否已导入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("返回") }
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TagChip(q.type.label)
                TagChip("难度 ${q.difficulty}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                TagChip(q.chapter, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            }

            if (!q.caseStudy.isNullOrBlank()) {
                SectionCard(title = "案例背景") {
                    Text(q.caseStudy, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(q.stem, style = MaterialTheme.typography.bodyLarge)

            if (q.subQuestions.isNotEmpty()) {
                SectionCard(title = "小问") {
                    q.subQuestions.forEachIndexed { i, sq ->
                        Text("(${(i + 1)}) ${sq.prompt}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }

            q.options.forEach { option ->
                OptionRow(
                    key = option.key,
                    text = option.text,
                    selected = option.key in state.selected,
                    submitted = state.submitted,
                    isCorrect = option.key in q.answer,
                    multi = q.type == QuestionType.MULTI,
                    enabled = !state.submitted,
                    onClick = { vm.select(option.key) },
                )
            }

            if (state.submitted) {
                val correctColor = if (state.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                Card(
                    colors = CardDefaults.cardColors(containerColor = correctColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            if (state.correct) "回答正确" else "回答错误",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = correctColor,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "正确答案：${q.answer.joinToString("")}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                SectionCard(title = "解析") {
                    Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                    if (q.source.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("来源：${q.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    when {
                        state.aiText != null -> Text(state.aiText!!, style = MaterialTheme.typography.bodyMedium)
                        state.aiError != null -> Text(state.aiError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        else -> OutlinedButton(onClick = { vm.requestAi() }, enabled = !state.aiLoading) {
                            Text(if (state.aiLoading) "AI 解析生成中…" else "AI 深度解析")
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.prev() }, enabled = state.index > 0, modifier = Modifier.weight(1f)) { Text("上一题") }
                if (!state.submitted) {
                    Button(onClick = { vm.submit() }, enabled = state.selected.isNotEmpty(), modifier = Modifier.weight(1f)) {
                        Text("提交答案")
                    }
                } else {
                    Button(onClick = { vm.next() }, modifier = Modifier.weight(1f)) {
                        Text(if (state.index + 1 >= state.questions.size) "完成" else "下一题")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSheet) {
        AnswerSheetDialog(
            total = state.questions.size,
            flags = state.answeredFlags,
            current = state.index,
            onJump = { vm.jumpTo(it) },
            onDismiss = { showSheet = false },
        )
    }
}

@Composable
private fun OptionRow(
    key: String,
    text: String,
    selected: Boolean,
    submitted: Boolean,
    isCorrect: Boolean,
    multi: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = when {
        submitted && isCorrect -> MaterialTheme.colorScheme.tertiary
        submitted && selected -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(if (multi) 6.dp else 50.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.5.dp, borderColor, RoundedCornerShape(if (multi) 6.dp else 50.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text(key, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AnswerSheetDialog(
    total: Int,
    flags: List<Boolean?>,
    current: Int,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("答题卡") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = Modifier.height(220.dp),
                contentPadding = PaddingValues(2.dp),
            ) {
                items(total) { i ->
                    val flag = flags.getOrNull(i)
                    val bg = when {
                        i == current -> MaterialTheme.colorScheme.primary
                        flag == true -> MaterialTheme.colorScheme.tertiary
                        flag == false -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val fg = if (flag != null || i == current) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        Modifier
                            .padding(3.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .clickable { onJump(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = fg)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
