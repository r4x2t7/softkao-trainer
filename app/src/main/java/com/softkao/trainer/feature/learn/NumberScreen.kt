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

data class NumberItem(val name: String, val value: String, val note: String)
data class NumberCategory(val category: String, val items: List<NumberItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberScreen(onBack: () -> Unit) {
    val examId = rememberCurrentExamId()
    val context = LocalContext.current
    val categories = remember(examId) {
        try {
            val text = context.assets.open("numbers/numbers.json").bufferedReader().readText()
            val root = JSONObject(text)
            val arr = root.getJSONArray(examId)
            (0 until arr.length()).map { i ->
                val cat = arr.getJSONObject(i)
                val itemsArr = cat.getJSONArray("items")
                val items = (0 until itemsArr.length()).map { j ->
                    val it = itemsArr.getJSONObject(j)
                    NumberItem(it.optString("name"), it.optString("value"), it.optString("note"))
                }
                NumberCategory(cat.optString("category"), items)
            }
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("高频数字速查") },
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
                    "数字题每年必考。背熟这些法定期限、比例与换算，配合⚠陷阱提示防混淆。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(categories, key = { it.category }) { cat ->
                SectionCard(title = cat.category) {
                    cat.items.forEach { item ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(
                                "${item.name}：${item.value}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (item.note.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text("⚠ ${item.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (item != cat.items.last()) { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }
        }
    }
}
