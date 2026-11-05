package com.softkao.trainer.feature.learn

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.core.ui.rememberCurrentExamId
import com.softkao.trainer.feature.common.SectionCard
import org.json.JSONObject

data class GlossaryItem(val term: String, val zh: String, val note: String)
data class GlossaryCategory(val category: String, val items: List<GlossaryItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(onBack: () -> Unit) {
    val examId = rememberCurrentExamId()
    val context = LocalContext.current
    val categories = remember(examId) {
        try {
            val text = context.assets.open("glossary/glossary.json").bufferedReader().readText()
            val root = JSONObject(text)
            val arr = root.getJSONArray(examId)
            (0 until arr.length()).map { i ->
                val cat = arr.getJSONObject(i)
                val itemsArr = cat.getJSONArray("items")
                val items = (0 until itemsArr.length()).map { j ->
                    val it = itemsArr.getJSONObject(j)
                    GlossaryItem(it.optString("term"), it.optString("zh"), it.optString("note"))
                }
                GlossaryCategory(cat.optString("category"), items)
            }
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("英语术语速查") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("‹ 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "综合知识卷固定考英语题，术语重复率高——背熟对照表即可拿到大部分分数。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(categories, key = { it.category }) { cat ->
                SectionCard(title = cat.category) {
                    cat.items.forEach { item ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                "${item.term} · ${item.zh}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item != cat.items.last()) { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }
        }
    }
}
