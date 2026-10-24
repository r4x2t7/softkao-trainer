package com.softkao.trainer.feature.wrong

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WrongViewModel : ViewModel() {

    data class Item(
        val entry: WrongEntry,
        val question: Question?,
    )

    data class State(
        val loading: Boolean = true,
        val items: List<Item> = emptyList(),
        val dueCount: Int = 0,
        val filter: Mastery? = null,
        val examFilter: ExamId? = null,
        val byKnowledge: Map<String, Int> = emptyMap(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
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
                Triple(items, kp, practice.dueWrongEntries().size)
            }
            val (items, kp, due) = loaded
            _state.value = _state.value.copy(
                loading = false,
                items = items,
                byKnowledge = kp,
                dueCount = due,
            )
        }
    }

    fun setFilter(mastery: Mastery?) {
        _state.value = _state.value.copy(filter = mastery)
    }

    fun setExamFilter(exam: ExamId?) {
        _state.value = _state.value.copy(examFilter = exam)
    }

    fun visibleItems(): List<Item> {
        val s = _state.value
        return s.items.filter { item ->
            (s.filter == null || item.entry.mastery == s.filter) &&
                (s.examFilter == null || item.entry.examId == s.examFilter.id)
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("错题", "${state.items.size}", modifier = Modifier.weight(1f))
                    StatTile("待复习", "${state.dueCount}", modifier = Modifier.weight(1f))
                    StatTile("已掌握", "${state.items.count { it.entry.mastery == Mastery.MASTERED }}", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                val dueIds = state.items.filter { it.entry.nextReviewAt <= System.currentTimeMillis() }
                    .joinToString(",") { it.entry.questionId }
                Button(
                    onClick = {
                        onStartPractice(
                            state.items.firstOrNull()?.entry?.examId ?: ExamId.SYSTEM_INTEGRATION.id,
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
                        selected = state.filter == null,
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.examFilter == null,
                        onClick = { viewModel.setExamFilter(null) },
                        label = { Text("全部科目") },
                    )
                }
                items(ExamId.entries) { exam ->
                    FilterChip(
                        selected = state.examFilter == exam,
                        onClick = { viewModel.setExamFilter(if (state.examFilter == exam) null else exam) },
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

        items(items, key = { it.entry.questionId }) { item ->
            val q = item.question
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TagChip(item.entry.mastery.label)
                        TagChip("错 ${item.entry.wrongCount} 次", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
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
                            "下次复习 ${formatDate(item.entry.nextReviewAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                onStartPractice(item.entry.examId, PracticeMode.WRONG, item.entry.questionId)
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
