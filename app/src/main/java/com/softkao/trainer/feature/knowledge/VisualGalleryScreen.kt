package com.softkao.trainer.feature.knowledge

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.model.HtmlVisual
import com.softkao.trainer.core.ui.rememberCurrentExamId
import com.softkao.trainer.feature.common.SectionCard
import com.softkao.trainer.feature.common.TagChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图解库：59 张静态图解卡片（HTML，离线渲染）。
 *
 * 和「抽象图解」（帧动画）互补：帧动画讲过程，这里讲结构。
 * 卡片横向滑动，点开是整屏的图解。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualGalleryScreen(
    onBack: () -> Unit,
    onOpenVisual: (HtmlVisual) -> Unit,
) {
    val examId = rememberCurrentExamId()
    val scope = rememberCoroutineScope()
    var chapter by remember { mutableStateOf("") }

    val all by produceState<List<HtmlVisual>>(emptyList(), examId) {
        value = withContext(Dispatchers.IO) {
            ServiceLocator.htmlVisualRepository.forExam(examId)
        }
    }
    val chapters = remember(all) { all.map { it.chapter }.distinct().filter { it.isNotBlank() } }
    val shown = remember(all, chapter) {
        if (chapter.isBlank()) all else all.filter { it.chapter == chapter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("图解库", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExamId.entries.forEach { exam ->
                                FilterChip(
                                    selected = exam.id == examId,
                                    onClick = { scope.launch { ServiceLocator.settingsRepository.setCurrentExamId(exam.id) } },
                                    label = { Text(exam.shortName, maxLines = 1, softWrap = false) },
                                )
                            }
                        }
                    }
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
                SectionCard {
                    Text(
                        "图解把结构型考点画成一张图：协议分层、法律层级、信息安全地图、生命周期曲线、质量工具对比等。上下滑动浏览，点开看整张图。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "共 ${all.size} 张 · 当前显示 ${shown.size} 张",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = chapter.isBlank(),
                            onClick = { chapter = "" },
                            label = { Text("全部（${all.size}）") },
                        )
                    }
                    items(chapters) { c ->
                        FilterChip(
                            selected = chapter == c,
                            onClick = { chapter = c },
                            label = { Text("$c（${all.count { it.chapter == c }}）") },
                        )
                    }
                }
            }

            // 图解改成纵向列表：横着滑几十张也找不到想要的那张
            items(shown, key = { it.id }) { v ->
                VisualCard(v) { onOpenVisual(v) }
            }

            if (shown.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            "这个科目还没有图解素材。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualCard(v: HtmlVisual, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                v.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                v.chapter,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                v.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (v.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    v.tags.forEach { TagChip(it) }
                }
            }
        }
    }
}

/**
 * 图解详情：整屏 WebView 渲染离线 HTML。
 * 深色模式自动切到 .dark.html 版本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualDetailScreen(
    visual: HtmlVisual,
    onBack: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val file = remember(visual.id, dark) {
        if (dark && visual.darkFile.isNotBlank()) visual.darkFile else visual.file
    }
    val bg = MaterialTheme.colorScheme.background
    val bgArgb = bg.toArgb()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(visual.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            visual.chapter,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            )
        },
        containerColor = bg,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bg),
        ) {
            val holder = remember { arrayOfNulls<WebView>(1) }
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(bgArgb)
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = true
                        loadUrl("file:///android_asset/visuals/$file")
                        holder[0] = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            DisposableEffect(Unit) {
                onDispose { holder[0]?.destroy() }
            }
        }
    }
}
