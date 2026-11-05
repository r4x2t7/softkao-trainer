package com.softkao.trainer.feature.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softkao.trainer.core.model.ExamId
import com.softkao.trainer.core.ui.rememberCurrentExamId
import com.softkao.trainer.feature.common.SectionCard
import org.json.JSONObject

data class FormulaItem(val name: String, val formula: String, val example: String, val trap: String)
data class FormulaCategory(val category: String, val items: List<FormulaItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormulaScreen(onBack: () -> Unit) {
    val examId = rememberCurrentExamId()
    val context = LocalContext.current
    val categories = remember(examId) {
        try {
            val text = context.assets.open("formulas/formulas.json").bufferedReader().readText()
            val root = JSONObject(text)
            val arr = root.getJSONArray(examId)
            (0 until arr.length()).map { i ->
                val cat = arr.getJSONObject(i)
                val itemsArr = cat.getJSONArray("items")
                val items = (0 until itemsArr.length()).map { j ->
                    val it = itemsArr.getJSONObject(j)
                    FormulaItem(it.optString("name"), it.optString("formula"), it.optString("example"), it.optString("trap"))
                }
                FormulaCategory(cat.optString("category"), items)
            }
        } catch (e: Exception) { emptyList() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公式速查") },
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
                    "考试必背公式速查。当前科目：${ExamId.fromId(examId)?.shortName ?: examId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(categories, key = { it.category }) { cat ->
                SectionCard(title = cat.category) {
                    cat.items.forEach { item ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    item.formula,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            if (item.example.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("例：${item.example}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.trap.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text("⚠ ${item.trap}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        if (item != cat.items.last()) { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                    }
                }
            }
        }
    }
}
