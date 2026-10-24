package com.softkao.trainer.feature.search

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.Question
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onOpenQuestion: (questionId: String, examId: String) -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var examFilter by remember { mutableStateOf<ExamId?>(null) }
    var results by remember { mutableStateOf<List<Question>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(keyword, examFilter) {
        if (keyword.isBlank()) {
            results = emptyList()
            return@LaunchedEffect
        }
        searching = true
        delay(300) // 防抖
        results = withContext(Dispatchers.IO) {
            ServiceLocator.questionRepository.search(keyword, examFilter?.id)
        }
        searching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索题目", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("输入关键词（题干 / 解析 / 知识点）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = examFilter == null,
                        onClick = { examFilter = null },
                        label = { Text("全部") },
                    )
                }
                items(ExamId.entries) { exam ->
                    FilterChip(
                        selected = examFilter == exam,
                        onClick = { examFilter = if (examFilter == exam) null else exam },
                        label = { Text(exam.shortName) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    keyword.isBlank() -> "支持搜题干、解析、知识点、章节名"
                    searching -> "搜索中…"
                    else -> "找到 ${results.size} 条结果"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { q ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                TagChip(q.type.label)
                                TagChip(
                                    q.chapter,
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                q.stem.take(110).let { if (q.stem.length > 110) "$it…" else it },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${q.knowledgePoint} · ${q.source.ifBlank { "自编" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { onOpenQuestion(q.id, q.examId) }) { Text("打开这题 →") }
                        }
                    }
                }
            }
        }
    }
}
