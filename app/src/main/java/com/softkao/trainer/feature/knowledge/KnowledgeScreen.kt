package com.softkao.trainer.feature.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ChapterMaterial
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private suspend fun <T> withContextIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }

private data class ChapterRow(
    val chapter: String,
    val total: Int,
    val answered: Int,
    val correct: Int,
    val hasMaterial: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    onBack: () -> Unit,
    onOpenChapter: (chapter: String) -> Unit,
    onPracticeChapter: (examId: String, chapter: String) -> Unit,
) {
    val examIdState = produceState(ExamId.SYSTEM_INTEGRATION.id) {
        value = ServiceLocator.settingsRepository.currentExamId.first()
    }
    val examId = examIdState.value
    val scope = rememberCoroutineScope()

    val rows by produceState(emptyList<ChapterRow>(), examId) {        value = withContextIo {
            val stats = ServiceLocator.statsRepository.chapterStats(examId)
            val materials = ServiceLocator.materialRepository.load(examId)
            val materialChapters = materials.map { it.chapter }.toSet()
            stats.map { (chapter, t) ->
                ChapterRow(
                    chapter = chapter,
                    total = t.first,
                    answered = t.second,
                    correct = t.third,
                    hasMaterial = chapter in materialChapters,
                )
            }.sortedByDescending { it.total }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "知识点",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SectionCard {
                    Text(
                        "按章节组织的精讲资料：每章包含考点清单、易错清单、公式与关键数字、记忆口诀和主观题答题模板。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExamId.entries.forEach { exam ->
                            FilterChip(
                                selected = exam.id == examId,
                                onClick = { scope.launch { ServiceLocator.settingsRepository.setCurrentExamId(exam.id) } },
                                label = { Text(exam.shortName, maxLines = 1, softWrap = false) },
                            )
                        }
                    }
                }
            }

            items(rows, key = { it.chapter }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable { onOpenChapter(row.chapter) },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                row.chapter,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (row.hasMaterial) {
                                Spacer(Modifier.widthIn(min = 8.dp))
                                TagChip("有精讲")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (row.total == 0) 0f else row.answered.toFloat() / row.total },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "题量 ${row.total} · 已练 ${row.answered}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                if (row.answered == 0) "未开始" else "正确率 ${row.correct * 100 / row.answered}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChapterDetailScreen(
    chapter: String,
    onBack: () -> Unit,
    onPracticeChapter: (examId: String, chapter: String) -> Unit,
) {
    val examId by produceState(ExamId.SYSTEM_INTEGRATION.id) {
        value = ServiceLocator.settingsRepository.currentExamId.first()
    }
    val material by produceState<ChapterMaterial?>(null, chapter, examId) {
        value = withContextIo { ServiceLocator.materialRepository.find(examId, chapter) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        chapter,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(
                    onClick = { onPracticeChapter(examId, chapter) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("练习本章（${chapter}）") }
            }

            val m = material
            if (m == null) {
                item {
                    SectionCard(title = "本章精讲") {
                        Text(
                            "本章精讲资料尚未生成。题库与资料会随仓库更新同步下发，也可以先去练习本章题目。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@LazyColumn
            }

            if (m.summary.isNotBlank()) {
                item { SectionCard(title = "本章概览") { Text(m.summary, style = MaterialTheme.typography.bodyMedium) } }
            }
            if (m.examFocus.isNotBlank()) {
                item { SectionCard(title = "命题趋势") { Text(m.examFocus, style = MaterialTheme.typography.bodyMedium) } }
            }
            if (m.keyPoints.isNotEmpty()) {
                item {
                    SectionCard(title = "速记卡 · 考点清单") {
                        m.keyPoints.forEachIndexed { i, point ->
                            if (i > 0) Spacer(Modifier.height(8.dp))
                            Row {
                                Text(
                                    "${i + 1}. ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(point, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (m.pitfalls.isNotEmpty()) {
                item {
                    SectionCard(title = "易错清单") {
                        m.pitfalls.forEach { pitfall ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Box(
                                    Modifier
                                        .padding(top = 6.dp)
                                        .widthIn(min = 10.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.error),
                                )
                                Spacer(Modifier.widthIn(min = 8.dp))
                                Text(pitfall, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            if (m.formulas.isNotEmpty()) {
                item {
                    SectionCard(title = "公式与关键数字") {
                        m.formulas.forEachIndexed { i, f ->
                            if (i > 0) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(f.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            if (f.formula.isNotBlank()) {
                                Text(f.formula, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (f.note.isNotBlank()) {
                                Text(f.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (m.memoryTip.isNotBlank()) {
                item {
                    SectionCard(title = "记忆口诀") {
                        Text(m.memoryTip, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (m.answerTemplate.isNotBlank()) {
                item {
                    SectionCard(title = "主观题答题模板") {
                        Text(m.answerTemplate, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
