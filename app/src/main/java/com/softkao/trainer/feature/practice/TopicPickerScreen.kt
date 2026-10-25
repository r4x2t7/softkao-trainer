package com.softkao.trainer.feature.practice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.PracticeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 选题页：按章节或按知识点挑一个开始练。
 *
 * 之前这两个入口是首页里的横向 chip 条，知识点有 1198 个——横着滑根本找不到，
 * 这在交互上是不成立的。所以改成独立页面：纵向列表 + 搜索。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPickerScreen(
    examId: String,
    initialTab: Int,
    onBack: () -> Unit,
    onPick: (examId: String, mode: PracticeMode, chapter: String, knowledgePoint: String) -> Unit,
) {
    var tab by remember { mutableStateOf(initialTab) }
    var keyword by remember { mutableStateOf("") }

    val chapters by produceState(emptyList<Pair<String, Int>>(), examId) {
        value = withContext(Dispatchers.IO) {
            ServiceLocator.statsRepository.chapterStats(examId)
                .map { (chapter, t) -> chapter to t.first }
                .sortedByDescending { it.second }
        }
    }
    val points by produceState(emptyList<Pair<String, Int>>(), examId) {
        value = withContext(Dispatchers.IO) {
            ServiceLocator.questionRepository.knowledgePointCounts(examId)
                .map { (kp, count) -> kp to count }
                .sortedByDescending { it.second }
        }
    }

    val filteredPoints = remember(points, keyword) {
        if (keyword.isBlank()) points
        else points.filter { it.first.contains(keyword, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("选择练习范围", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "当前科目：${ExamId.fromId(examId)?.fullName ?: examId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text("按章节（${chapters.size}）") },
                )
                FilterChip(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text("按知识点（${points.size}）") },
                )
            }
            Spacer(Modifier.height(8.dp))

            if (tab == 1) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("搜索知识点") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (keyword.isBlank()) "共 ${points.size} 个知识点，列表可上下滑动" else "匹配 ${filteredPoints.size} 个"
                    ,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            } else {
                Text(
                    "共 ${chapters.size} 章，点一下直接练本章",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
            }

            if (tab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(chapters, key = { it.first }) { (chapter, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(examId, PracticeMode.CHAPTER, chapter, "") }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(chapter, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "$count 题",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("练本章 ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filteredPoints, key = { it.first }) { (kp, count) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(examId, PracticeMode.KNOWLEDGE, "", kp) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(kp, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "$count 题",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("练 ›", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider()
                    }
                    if (filteredPoints.isEmpty()) {
                        item {
                            Text(
                                "没有匹配的知识点，换个关键词试试",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
