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

data class ConfusionItem(val a: String, val b: String, val diff: String, val tip: String)
data class ConfusionCategory(val category: String, val items: List<ConfusionItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfusionScreen(onBack: () -> Unit) {
    val examId = rememberCurrentExamId()
    val context = LocalContext.current
    val categories = remember(examId) {
        try {
            val text = context.assets.open("confusions/confusions.json").bufferedReader().readText()
            val root = JSONObject(text)
            val arr = root.getJSONArray(examId)
            (0 until arr.length()).map { i ->
                val cat = arr.getJSONObject(i)
                val itemsArr = cat.getJSONArray("items")
                val items = (0 until itemsArr.length()).map { j ->
                    val it = itemsArr.getJSONObject(j)
                    ConfusionItem(it.optString("a"), it.optString("b"), it.optString("diff"), it.optString("tip"))
                }
                ConfusionCategory(cat.optString("category"), items)
            }
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("易混概念对照") },
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
                    "易混概念是选择题的失分重灾区——每组给一句话区别与考法提示，考前过一遍。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(categories, key = { it.category }) { cat ->
                SectionCard(title = cat.category) {
                    cat.items.forEach { item ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                "${item.a}  vs  ${item.b}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    item.diff,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            if (item.tip.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text("⚠ ${item.tip}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (item != cat.items.last()) { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }
        }
    }
}
