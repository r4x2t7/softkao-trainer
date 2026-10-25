package com.softkao.trainer.feature.wrong

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.feature.common.LabeledProgress
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WrongViewModel : ViewModel() {

    data class State(
        val loading: Boolean = true,
        /** 当前列表视图：错题 / 收藏（二选一） */
        /** 筛选状态（视图模式 + 掌握度，二者互斥） */
        val filter: WrongFilterState = WrongFilterState(),
        val exam: ExamId? = ExamId.SYSTEM_INTEGRATION,
        val wrongItems: List<WrongItem> = emptyList(),
        val bookmarkItems: List<WrongItem> = emptyList(),
        /** 当前科目下各掌握度的错题数（chip 上直接显示） */
        val masteryCounts: Map<Mastery, Int> = emptyMap(),
        val bookmarkCount: Int = 0,
        /** 当前科目下的错题总数（不随筛选变化） */
        val totalWrong: Int = 0,
        /** 当前科目下到期待复习的错题数（不随筛选变化） */
        val dueTotal: Int = 0,
        val masteredTotal: Int = 0,
        /** 当前科目下最近一次复习时间；0 表示没有安排 */
        val nextReviewAt: Long = 0L,
        val byKnowledge: Map<String, Int> = emptyMap(),
    )

    /** 一次性装载出来的东西 */
    private data class Loaded(
        val wrongItems: List<WrongItem>,
        val bookmarkItems: List<WrongItem>,
        val masteryCounts: Map<Mastery, Int>,
        val bookmarkCount: Int,
        val totalWrong: Int,
        val dueTotal: Int,
        val masteredTotal: Int,
        val nextReviewAt: Long,
        val byKnowledge: Map<String, Int>,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        // 先把数据拉出来，别干等设置流：首屏空着比慢一拍难受得多
        refresh()
        // 订阅全局科目：错题本只显示当前科目，切换科目时立即重载
        viewModelScope.launch {
            ServiceLocator.settingsRepository.currentExamId.collect { id ->
                val exam = ExamId.fromId(id)
                if (_state.value.exam != exam) {
                    _state.value = _state.value.copy(exam = exam)
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val exam = _state.value.exam
            val loaded = withContext(Dispatchers.IO) { load(exam) }
            _state.value = _state.value.copy(
                loading = false,
                wrongItems = loaded.wrongItems,
                bookmarkItems = loaded.bookmarkItems,
                masteryCounts = loaded.masteryCounts,
                bookmarkCount = loaded.bookmarkCount,
                totalWrong = loaded.totalWrong,
                dueTotal = loaded.dueTotal,
                masteredTotal = loaded.masteredTotal,
                nextReviewAt = loaded.nextReviewAt,
                byKnowledge = loaded.byKnowledge,
            )
        }
    }

    /** 只能在 IO 线程调用（refresh 已经用 withContext(Dispatchers.IO) 包住了） */
    private fun load(exam: ExamId?): Loaded {
        val practice = ServiceLocator.practiceRepository
        val repo = ServiceLocator.questionRepository
        val now = System.currentTimeMillis()

        val wrongItems = practice.wrongEntries().map { WrongItem(it, repo.byId(it.questionId)) }
        val scoped = wrongItems.filter { exam == null || it.examId == exam.id }

        // 收藏：题库里已经不存在的题目直接丢掉，避免列表 key 重复把界面搞崩
        val bookmarkItems = practice.bookmarkedIds()
            .distinct()
            .mapNotNull { id -> repo.byId(id)?.let { WrongItem(null, it) } }
        val bookmarkScoped = bookmarkItems.filter { exam == null || it.question?.examId == exam.id }

        return Loaded(
            wrongItems = wrongItems,
            bookmarkItems = bookmarkItems,
            masteryCounts = countByMastery(scoped),
            bookmarkCount = bookmarkScoped.size,
            totalWrong = scoped.size,
            dueTotal = scoped.count { entry -> (entry.entry?.nextReviewAt ?: Long.MAX_VALUE) <= now },
            masteredTotal = scoped.count { it.mastery == Mastery.MASTERED },
            nextReviewAt = scoped.mapNotNull { it.entry?.nextReviewAt }.filter { it > now }.minOrNull() ?: 0L,
            byKnowledge = scoped.groupBy { it.question?.knowledgePoint ?: "未知" }
                .mapValues { (_, v) -> v.size },
        )
    }

    // ---------------------------------------------------------------- 筛选

    /**
     * 切到收藏视图。收藏没有掌握度，所以顺手清掉掌握度筛选——
     * 这正是之前「点了没效果」的根源：两个筛选同时开着，收藏分支把掌握度吞掉了。
     */
    fun selectMode(mode: WrongListMode) {
        _state.value = _state.value.copy(filter = _state.value.filter.withMode(mode))
    }

    /** 选掌握度：自动回到错题视图，保证任何时刻只有一个筛选生效 */
    fun selectMastery(mastery: Mastery?) {
        _state.value = _state.value.copy(filter = _state.value.filter.withMastery(mastery))
    }

    fun clearFilter() {
        _state.value = _state.value.copy(filter = _state.value.filter.cleared())
    }

    fun selectExam(exam: ExamId) {
        if (_state.value.exam == exam) return
        _state.value = _state.value.copy(exam = exam)
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(exam.id)
        }
        refresh()
    }

    /** 当前应该显示的条目（界面用 remember(state) 自行派生，这里留给非 Compose 调用方） */
    fun visibleItems(): List<WrongItem> {
        val s = _state.value
        return selectWrongItems(
            wrongItems = s.wrongItems,
            bookmarkItems = s.bookmarkItems,
            filter = s.filter,
            examId = s.exam?.id,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongScreen(
    onStartPractice: (examId: String, mode: PracticeMode, ids: String) -> Unit,
    viewModel: WrongViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 列表必须和摘要来自同一份状态快照。
    // 之前这里直接读 ViewModel 的 _state.value（非快照读），
    // 会和 collectAsState 拿到的 state 不是同一帧 —— 就会出现
    // 「chip 已经切到收藏，列表却还是旧的错题」这种看起来点了没反应的情况。
    val items = remember(state) {
        selectWrongItems(
            wrongItems = state.wrongItems,
            bookmarkItems = state.bookmarkItems,
            filter = state.filter,
            examId = state.exam?.id,
        )
    }
    val now = System.currentTimeMillis()

    // 到期错题：跟着当前筛选走，所见即所得
    val dueEntries = items.mapNotNull { it.entry }.filter { it.nextReviewAt <= now }
    val dueIds = dueEntries.joinToString(",") { it.questionId }
    val dueExam = dueEntries.firstOrNull()?.examId ?: state.exam?.id ?: ExamId.SYSTEM_INTEGRATION.id

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("错题本", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "答错自动入库，按 1/3/7/15 天阶梯安排复习",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // -------- 顶部：概览 + 主行动按钮
        if (state.totalWrong > 0) {
            item {
                SectionCard {
                    WrongOverviewRow(
                        total = state.totalWrong,
                        due = state.dueTotal,
                        mastered = state.masteredTotal,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.filter.mode == WrongListMode.WRONG) {
                        Button(
                            onClick = {
                                onStartPractice(dueExam, PracticeMode.WRONG, dueIds)
                            },
                            enabled = dueIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("开始复习到期错题（${dueEntries.size}）")
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = reviewHint(state, dueIds.isNotEmpty()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "收藏视图里没有复习计划，切回「全部错题」可以开始复习。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // -------- 筛选区
        item {
            WrongFilterBar(
                filter = state.filter,
                examId = state.exam,
                masteryCounts = state.masteryCounts,
                bookmarkCount = state.bookmarkCount,
                totalWrong = state.totalWrong,
                visibleCount = items.size,
                onSelectMode = { viewModel.selectMode(it) },
                onSelectMastery = { viewModel.selectMastery(it) },
                onExamChange = { viewModel.selectExam(it) },
                onClear = { viewModel.clearFilter() },
            )
        }

        // -------- 结果为空：说清楚为什么，给下一步
        if (items.isEmpty()) {
            item {
                WrongEmptyState(
                    filter = state.filter,
                    totalWrong = state.totalWrong,
                    examName = state.exam?.shortName,
                    onClear = { viewModel.clearFilter() },
                    onPractice = {
                        onStartPractice(
                            state.exam?.id ?: ExamId.SYSTEM_INTEGRATION.id,
                            PracticeMode.SEQUENTIAL,
                            "",
                        )
                    },
                )
            }
        }

        // -------- 知识点分布（只在错题视图下有意义）
        if (state.filter.mode == WrongListMode.WRONG && state.byKnowledge.isNotEmpty() && items.isNotEmpty()) {
            item {
                SectionCard(title = "错题分布（按知识点）") {
                    val max = state.byKnowledge.values.maxOrNull() ?: 1
                    state.byKnowledge.entries.sortedByDescending { it.value }.take(8).forEach { (kp, count) ->
                        Spacer(Modifier.height(6.dp))
                        LabeledProgress(
                            label = kp,
                            value = count.toFloat() / max,
                            trailing = "$count 题",
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "条形越长＝这个考点错得越多，优先回去刷它",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // -------- 列表
        items(items, key = { it.questionId }) { item ->
            val q = item.question
            val entry = item.entry
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (entry != null) {
                            TagChip(entry.mastery.label)
                            TagChip(
                                "错 ${entry.wrongCount} 次",
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer,
                            )
                        } else {
                            TagChip(
                                "★ 收藏",
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        q?.let {
                            TagChip(
                                it.knowledgePoint,
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        q?.stem ?: "（题目已不在题库中）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (entry != null) "下次复习 ${formatDate(entry.nextReviewAt)}" else "已收藏题目",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                val targetId = entry?.questionId ?: q?.id
                                val targetExam = entry?.examId ?: q?.examId
                                if (targetId != null && targetExam != null) {
                                    onStartPractice(targetExam, PracticeMode.WRONG, targetId)
                                }
                            },
                        ) { Text("重做") }
                    }
                }
            }
        }
    }
}

/** 主按钮下方的说明：把「为什么不能点」「点下去会做什么」都讲清楚 */
private fun reviewHint(state: WrongViewModel.State, hasDue: Boolean): String = when {
    hasDue -> "复习范围＝当前筛选下到期的错题，点「重做」可以单独重做某一道"
    state.dueTotal > 0 -> "当前筛选下没有到期的错题（该科目共 ${state.dueTotal} 道到期）"
    state.nextReviewAt > 0 -> "最近一次复习安排在 ${formatDate(state.nextReviewAt)}"
    else -> "这个科目下暂时没有需要复习的错题"
}

private fun formatDate(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(time))
