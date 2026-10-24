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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.softkao.trainer.core.model.Mastery
import com.softkao.trainer.core.model.PracticeMode
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.core.model.WrongEntry
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WrongViewModel : ViewModel() {

    data class Item(
        val entry: WrongEntry?,
        val question: Question?,
    )

    data class State(
        val loading: Boolean = true,
        val items: List<Item> = emptyList(),
        val dueCount: Int = 0,
        val filter: Mastery? = null,
        val examFilter: ExamId? = null,
        val bookmarkFilter: Boolean = false,
        val bookmarkCount: Int = 0,
        val bookmarkItems: List<Item> = emptyList(),
        val byKnowledge: Map<String, Int> = emptyMap(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** 用户是否已经手动选过科目；用来避免初始化时被异步读取覆盖 */
    private var userPicked = false

    init {
        refresh()
        viewModelScope.launch {
            // 全局科目：错题本只显示当前科目，两门考试彻底隔离
            val saved = ServiceLocator.settingsRepository.currentExamId.first()
            if (!userPicked) {
                _state.value = _state.value.copy(examFilter = ExamId.fromId(saved))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val practice = ServiceLocator.practiceRepository
                val repo = ServiceLocator.questionRepository
                val entries = practice.wrongEntries()
                val items = entries.map { Item(it, repo.byId(it.questionId)) }
                val kp = entries.groupBy { it.examId }
                    .flatMap { (examId, list) ->
                        list.groupBy { repo.byId(it.questionId)?.knowledgePoint ?: "未知" }
                            .map { (kpName, group) -> "$examId · $kpName" to group.size }
                    }
                    .toMap()
                val bookmarks = practice.bookmarkedIds()
                val bookmarkItems = bookmarks.map { Item(null, repo.byId(it)) }
                Quad(items, kp, practice.dueWrongEntries().size, bookmarkItems)
            }
            val (items, kp, due, bookmarkItems) = loaded
            _state.value = _state.value.copy(
                loading = false,
                items = items,
                byKnowledge = kp,
                dueCount = due,
                bookmarkCount = bookmarkItems.size,
                bookmarkItems = bookmarkItems,
            )
        }
    }

    private data class Quad(
        val a: List<Item>,
        val b: Map<String, Int>,
        val c: Int,
        val d: List<Item>,
    )

    fun setBookmarkFilter(enabled: Boolean) {
        _state.value = _state.value.copy(bookmarkFilter = enabled, filter = null)
    }

    fun toggleBookmarkFilter() = setBookmarkFilter(!_state.value.bookmarkFilter)

    fun setFilter(mastery: Mastery?) {
        _state.value = _state.value.copy(filter = mastery)
    }

    fun setExamFilter(exam: ExamId?) {
        if (exam == null) return
        userPicked = true
        _state.value = _state.value.copy(examFilter = exam)
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(exam.id)
        }
    }

    fun visibleItems(): List<Item> {
        val s = _state.value
        if (s.bookmarkFilter) {
            return s.bookmarkItems.filter { s.examFilter == null || it.question?.examId == s.examFilter.id }
        }
        return s.items.filter { item ->
            (s.filter == null || item.entry?.mastery == s.filter) &&
                (s.examFilter == null || item.entry?.examId == s.examFilter.id)
        }
    }
}

@Composable
fun WrongScreen(
    onStartPractice: (examId: String, mode: PracticeMode, ids: String) -> Unit,
    viewModel: WrongViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.visibleItems()

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

        item {
            SectionCard {
                val visibleDue = items.filter { (it.entry?.nextReviewAt ?: 0L) <= System.currentTimeMillis() }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("错题", "${items.size}", modifier = Modifier.weight(1f))
                    StatTile("待复习", "${visibleDue.size}", modifier = Modifier.weight(1f))
                    StatTile(
                        "已掌握",
                        "${items.count { it.entry?.mastery == Mastery.MASTERED }}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                val dueIds = visibleDue.mapNotNull { it.entry?.questionId }.joinToString(",")
                val dueExam = visibleDue.firstOrNull()?.entry?.examId
                Button(
                    onClick = {
                        onStartPractice(
                            dueExam ?: ExamId.SYSTEM_INTEGRATION.id,
                            PracticeMode.WRONG,
                            dueIds,
                        )
                    },
                    enabled = dueIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("开始复习到期错题（${dueIds.split(",").count { it.isNotBlank() }}）") }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.bookmarkFilter,
                        onClick = { viewModel.toggleBookmarkFilter() },
                        label = { Text("★ 收藏 (${state.bookmarkCount})") },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == null && !state.bookmarkFilter,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("全部") },
                    )
                }
                items(Mastery.entries.filter { it != Mastery.NEW }) { mastery ->
                    FilterChip(
                        selected = state.filter == mastery,
                        onClick = { viewModel.setFilter(if (state.filter == mastery) null else mastery) },
                        label = { Text(mastery.label) },
                    )
                }
            }
        }

        item {
            Text(
                "错题本科目（本页独立选择，不影响其它页面）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExamId.entries) { exam ->
                    FilterChip(
                        selected = state.examFilter == exam,
                        onClick = { viewModel.setExamFilter(exam) },
                        label = { Text(exam.shortName) },
                    )
                }
            }
        }

        if (state.byKnowledge.isNotEmpty()) {
            item {
                SectionCard(title = "错题分布（按知识点）") {
                    state.byKnowledge.entries.sortedByDescending { it.value }.take(6).forEach { (kp, count) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(kp, style = MaterialTheme.typography.bodyMedium)
                            Text("$count 题", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        items(items, key = { it.entry?.questionId ?: it.question?.id ?: "" }) { item ->
            val q = item.question
            val entry = item.entry
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (entry != null) {
                            TagChip(entry.mastery.label)
                            TagChip("错 ${entry.wrongCount} 次", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                        } else {
                            TagChip("★ 收藏", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        q?.let {
                            TagChip(it.knowledgePoint, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        q?.stem?.take(90)?.let { if (q.stem.length > 90) "$it…" else it } ?: "（题目已不在题库中）",
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

private fun formatDate(time: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(time))
