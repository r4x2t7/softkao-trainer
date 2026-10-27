package com.softkao.trainer.feature.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.softkao.trainer.core.ui.PracticeLogic
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val showReference: Boolean = false,
        /** 模拟卷：交卷后才公布答案与解析 */
        val mockRevealed: Boolean = false,
        /** 模拟卷：每题作答记录，下标与题目一致 */
        val mockAnswers: List<List<String>> = emptyList(),
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
        PracticeMode.CASE -> "案例分析"
    }

    private fun buildQuestionSet(): List<Question> {
        val repo = ServiceLocator.questionRepository
        val practice = ServiceLocator.practiceRepository
        if (presetIds.isNotEmpty()) return repo.byIds(presetIds)

        // 顺序/章节/专项/错题 一律加载全部题目，由界面分页展示，不再截断
        return when (mode) {
            PracticeMode.CHAPTER -> repo.list(examId = examId, chapter = chapter)
            PracticeMode.KNOWLEDGE -> repo.list(examId = examId, knowledgePoint = knowledgePoint)
            PracticeMode.WRONG -> {
                val ids = practice.wrongEntries(examId).map { it.questionId }
                repo.byIds(ids)
            }
            PracticeMode.DAILY -> dailySet()
            PracticeMode.MOCK -> mockSet()
            PracticeMode.SEQUENTIAL -> repo.list(examId = examId)
            // 案例分析有独立模块（CaseListScreen），不会从练习路由进入
            PracticeMode.CASE -> emptyList()
        }
    }

    /** 每日一练：同一天进入永远是同一组题 */
    private fun dailySet(count: Int = 20): List<Question> {
        val all = ServiceLocator.questionRepository.list(examId = examId)
        if (all.isEmpty()) return emptyList()
        val daySeed = java.time.LocalDate.now().toEpochDay().toInt()
        val rnd = java.util.Random(daySeed * 31L + examId.hashCode())
        return all.shuffled(rnd).take(count)
    }

    /** 模拟卷：按章节轮流抽题，覆盖面比纯随机更均衡 */
    private fun mockSet(total: Int = 40): List<Question> {
        val all = ServiceLocator.questionRepository.list(examId = examId)
        if (all.isEmpty()) return emptyList()
        val byChapter = all.groupBy { it.chapter }
        val picked = mutableListOf<Question>()
        var round = 0
        while (picked.size < total) {
            var added = false
            for (chapter in byChapter.keys.sorted()) {
                val list = byChapter[chapter].orEmpty()
                if (round < list.size) {
                    picked += list[round]
                    added = true
                    if (picked.size >= total) break
                }
            }
            if (!added) break
            round++
        }
        val rnd = java.util.Random(java.time.LocalDate.now().toEpochDay())
        return picked.shuffled(rnd)
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
            if (mode == PracticeMode.MOCK) {
                // 模拟卷按真实考试来：只记录答案，不当场给解析，全部答完交卷后再公布
                val idx = s.index
                val answers = s.mockAnswers.toMutableList()
                while (answers.size <= idx) answers.add(emptyList())
                answers[idx] = s.selected.toList()
                val flags = s.answeredFlags.toMutableList()
                while (flags.size <= idx) flags.add(null)
                flags[idx] = correct
                _state.value = s.copy(
                    mockAnswers = answers,
                    answeredFlags = flags,
                    selected = emptySet(),
                    submitted = false,
                    correct = false,
                    aiText = null,
                )
                // 自动进入下一题
                val next = (idx + 1).coerceAtMost(s.questions.size - 1)
                if (next != idx) {
                    startedAt = System.currentTimeMillis()
                    _state.value = _state.value.copy(index = next)
                }
            } else {
                _state.value = s.copy(
                    submitted = true,
                    correct = correct,
                    answeredFlags = s.answeredFlags.toMutableList().also { it[s.index] = correct },
                )
            }
        }
    }

    /** 模拟卷交卷：公布答案与解析，回到第 1 题逐题回看 */
    fun finishMock() {
        val s = _state.value
        if (mode != PracticeMode.MOCK) return
        val right = s.answeredFlags.count { it == true }
        val done = s.answeredFlags.count { it != null }
        _state.value = s.copy(
            mockRevealed = true,
            index = 0,
            selected = emptySet(),
            submitted = false,
            title = "模拟卷 · 已交卷（答对 $right / 已答 $done）",
        )
    }

    /** 模拟卷里还没作答的题数 */
    fun mockUnansweredCount(): Int =
        _state.value.answeredFlags.count { it == null }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.questions.size) {
            _state.value = s.copy(finished = true)
            return
        }
        startedAt = System.currentTimeMillis()
        val next = s.index + 1
        _state.value = s.copy(index = next, selected = emptySet(), submitted = false, aiText = null, aiError = null, showReference = false)
        syncMockCorrect(next)
        refreshBookmark()
    }

    fun prev() {
        val s = _state.value
        if (s.index == 0) return
        startedAt = System.currentTimeMillis()
        val prev = s.index - 1
        _state.value = s.copy(index = prev, selected = emptySet(), submitted = false, aiText = null, aiError = null, showReference = false)
        syncMockCorrect(prev)
        refreshBookmark()
    }

    fun jumpTo(target: Int) {
        val s = _state.value
        if (target !in s.questions.indices) return
        startedAt = System.currentTimeMillis()
        _state.value = s.copy(index = target, selected = emptySet(), submitted = false, aiText = null, aiError = null, showReference = false)
        syncMockCorrect(target)
        refreshBookmark()
    }

    /** 模拟卷回看阶段：对错要跟着当前题走 */
    private fun syncMockCorrect(index: Int) {
        val s = _state.value
        if (!s.mockRevealed) return
        _state.value = s.copy(correct = s.answeredFlags.getOrNull(index) == true)
    }

    /** 相对跳转，负数往回 */
    fun jumpBy(delta: Int) = jumpTo(_state.value.index + delta)

    fun jumpFirst() = jumpTo(0)

    fun jumpLast() = jumpTo((_state.value.questions.size - 1).coerceAtLeast(0))

    fun toggleBookmark() {
        val q = currentQuestion ?: return
        val now = ServiceLocator.practiceRepository.toggleBookmark(q.id, q.examId)
        _state.value = _state.value.copy(bookmarked = now)
    }

    /** 案例题：展示/隐藏参考答案 */
    fun toggleReference() {
        _state.value = _state.value.copy(showReference = !_state.value.showReference)
    }

    /** 案例题自评：记住了 / 没记住 */
    fun submitCase(remembered: Boolean) {
        val s = _state.value
        if (s.submitted) return
        val q = currentQuestion ?: return
        val duration = System.currentTimeMillis() - startedAt
        viewModelScope.launch {
            val correct = withContext(Dispatchers.IO) {
                ServiceLocator.practiceRepository.submitCase(q, remembered, s.mode, duration)
            }
            _state.value = _state.value.copy(
                submitted = true,
                correct = correct,
                showReference = true,
                answeredFlags = _state.value.answeredFlags.toMutableList().also { it[_state.value.index] = correct },
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        Text(
                            state.title.ifBlank { "练习" },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.questions.isNotEmpty()) {
                            Text(
                                "第 ${state.index + 1} / ${state.questions.size} 题",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
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
                },
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider()
                if (state.mode == PracticeMode.MOCK) {
                    MockTimerBar(questions = state.questions.size)
                }
                PracticeJumpBar(
                    index = state.index,
                    total = state.questions.size,
                    onJump = { vm.jumpTo(it) },
                    onFirst = { vm.jumpFirst() },
                    onLast = { vm.jumpLast() },
                    onSheet = { showSheet = true },
                )
            }
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TagChip(q.type.label)
                TagChip("难度 ${q.difficulty}", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                TagChip(q.chapter, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                TagChip(q.knowledgePoint, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 模拟卷：像真实考试一样，作答阶段不给答案与解析
            if (state.mode == PracticeMode.MOCK) {
                SectionCard(title = if (state.mockRevealed) "已交卷 · 逐题回看" else "模拟卷进行中") {
                    Text(
                        if (state.mockRevealed) {
                            "下面每题都会显示你当时的作答、正确答案与解析。"
                        } else {
                            "模拟卷按考试规则来：提交只记录答案，不当场给答案与解析。" +
                                "还有 ${vm.mockUnansweredCount()} 题没答，答完后点「交卷查看结果」再逐题回看。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

            if (q.type == QuestionType.CASE) {
                // 案例分析题：先自己作答，再对照参考答案自评
                if (state.showReference) {
                    SectionCard(title = "参考答案") {
                        Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                        q.subQuestions.forEach { sq ->
                            sq.referenceAnswer?.let {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (q.source.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("来源：${q.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    OutlinedButton(onClick = { vm.toggleReference() }, modifier = Modifier.fillMaxWidth()) {
                        Text("先自己写一遍，再点此对照参考答案")
                    }
                }
            } else {
                if (q.type == QuestionType.TRUE_FALSE) {
                    // 判断题在题库中不带选项，这里渲染 √ / × 两个可选项
                    TrueFalseChoiceRow(
                        selected = state.selected,
                        answer = q.answer,
                        submitted = state.submitted,
                        enabled = !state.submitted,
                        onSelect = { vm.select(it) },
                    )
                } else {
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
                }
            }

            if (state.submitted || state.mockRevealed) {
                val mockMode = state.mockRevealed && state.mode == PracticeMode.MOCK
                val correctColor = if (state.correct) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                if (mockMode) {
                    // 模拟卷回看：显示这道题当时选了什么、对不对
                    val my = state.mockAnswers.getOrNull(state.index).orEmpty()
                    SectionCard(title = "本题作答情况") {
                        Text(
                            "你的答案：${if (my.isEmpty()) "（未作答）" else my.joinToString("")}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.correct) "结果：答对" else "结果：答错",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = correctColor,
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = correctColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            when {
                                q.type == QuestionType.CASE && state.correct -> "已记录：记住了"
                                q.type == QuestionType.CASE -> "已记录：没记住（已进错题本）"
                                state.correct -> "回答正确"
                                else -> "回答错误"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = correctColor,
                        )
                        if (q.type != QuestionType.CASE) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "正确答案：${q.answer.joinToString("")}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                if (q.type != QuestionType.CASE) {
                    SectionCard(title = "解析") {
                        Text(q.explanation, style = MaterialTheme.typography.bodyMedium)
                        if (q.source.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("来源：${q.source}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(10.dp))
                        when {
                            // 题库自带 AI 深度解析 → 直接展示，离线可用
                            !q.aiExplanation.isNullOrBlank() -> {
                                Text("AI 深度解析", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(q.aiExplanation!!, style = MaterialTheme.typography.bodyMedium)
                            }
                            state.aiText != null -> Text(state.aiText!!, style = MaterialTheme.typography.bodyMedium)
                            state.aiError != null -> Text(state.aiError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            else -> OutlinedButton(onClick = { vm.requestAi() }, enabled = !state.aiLoading) {
                                Text(if (state.aiLoading) "AI 解析生成中…" else "AI 深度解析（需在题库页配置接口）")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.prev() }, enabled = state.index > 0, modifier = Modifier.weight(1f)) { Text("上一题") }
                when {
                    // 模拟卷：作答阶段只记录不公布，交卷后才回看
                    state.mode == PracticeMode.MOCK && !state.mockRevealed -> {
                        if (state.index + 1 >= state.questions.size) {
                            Button(onClick = { vm.finishMock() }, modifier = Modifier.weight(1f)) {
                                Text("交卷查看结果")
                            }
                        } else {
                            Button(
                                onClick = { vm.submit() },
                                enabled = state.selected.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                            ) { Text("提交本题") }
                        }
                    }
                    state.mode == PracticeMode.MOCK && state.mockRevealed -> {
                        Button(onClick = { vm.next() }, modifier = Modifier.weight(1f)) {
                            Text(if (state.index + 1 >= state.questions.size) "回到第 1 题" else "下一题")
                        }
                    }
                    q.type == QuestionType.CASE && !state.submitted -> {
                        Button(onClick = { vm.submitCase(true) }, modifier = Modifier.weight(1f)) { Text("记住了") }
                        OutlinedButton(onClick = { vm.submitCase(false) }, modifier = Modifier.weight(1f)) { Text("没记住") }
                    }
                    !state.submitted -> {
                        Button(onClick = { vm.submit() }, enabled = state.selected.isNotEmpty(), modifier = Modifier.weight(1f)) {
                            Text("提交答案")
                        }
                    }
                    else -> {
                        Button(onClick = { vm.next() }, modifier = Modifier.weight(1f)) {
                            Text(if (state.index + 1 >= state.questions.size) "完成" else "下一题")
                        }
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
private fun MockTimerBar(questions: Int) {
    // 模拟卷按考试节奏倒计时：选择题 90 分钟，案例题另计
    var remaining by remember { mutableLongStateOf(PracticeLogic.MOCK_DURATION_SECONDS) }
    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    val timeText = PracticeLogic.mockTimeText(remaining)
    val urgent = PracticeLogic.isMockUrgent(remaining)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            timeText,
            style = MaterialTheme.typography.labelMedium,
            color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "共 $questions 题 · 超时未答按未掌握计入错题",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
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
    val pages = PracticeLogic.sheetPageCount(total)
    var page by remember { mutableStateOf(PracticeLogic.initialSheetPage(current, total)) }
    val range = PracticeLogic.sheetRange(page, total)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("答题卡（共 $total 题）") },
        text = {
            Column {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(210.dp),
                    contentPadding = PaddingValues(2.dp),
                ) {
                    items(range.toList()) { i ->
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
                                .clickable {
                                    onJump(i)
                                    page = (i / PracticeLogic.ANSWER_SHEET_PER_PAGE).coerceAtMost(pages - 1)
                                    // 点了题号要顺手把答题卡关掉，否则弹窗会一直盖在题目上
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
                        }
                    }
                }
                if (pages > 1) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { page = (page - 1).coerceAtLeast(0) }) { Text("‹ 上一批") }
                        Text("第 ${page + 1} / $pages 批", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        TextButton(onClick = { page = (page + 1).coerceAtMost(pages - 1) }) { Text("下一批 ›") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
