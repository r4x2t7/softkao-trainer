package com.softkao.trainer.feature.bank

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softkao.trainer.ServiceLocator
import com.softkao.trainer.core.model.BankVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BankViewModel : ViewModel() {

    data class State(
        val sourceUrl: String = "",
        val aiBaseUrl: String = "",
        val aiApiKey: String = "",
        val aiModel: String = "",
        val dailyGoal: String = "20",
        val versions: List<BankVersion> = emptyList(),
        val message: String? = null,
        val busy: Boolean = false,
        val aiMessage: String? = null,
        val currentExamId: String = com.softkao.trainer.core.model.ExamId.SYSTEM_INTEGRATION.id,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = ServiceLocator.settingsRepository
            _state.value = _state.value.copy(
                sourceUrl = settings.bankSourceUrl.first(),
                aiBaseUrl = settings.aiBaseUrl.first(),
                aiApiKey = settings.aiApiKey.first(),
                aiModel = settings.aiModel.first(),
                dailyGoal = settings.dailyGoal.first().toString(),
                currentExamId = settings.currentExamId.first(),
            )
            refreshVersions()
        }
    }

    /** 切换当前科目（一个人通常只考一门） */
    fun setCurrentExam(id: String) {
        viewModelScope.launch {
            ServiceLocator.settingsRepository.setCurrentExamId(id)
            _state.value = _state.value.copy(currentExamId = id, message = "当前科目已切换")
        }
    }

    fun refreshVersions() {
        viewModelScope.launch {
            val versions = withContext(Dispatchers.IO) { ServiceLocator.statsRepository.bankVersions() }
            _state.value = _state.value.copy(versions = versions)
        }
    }

    fun onSourceUrlChange(value: String) {
        _state.value = _state.value.copy(sourceUrl = value)
    }

    fun onAiBaseUrlChange(value: String) {
        _state.value = _state.value.copy(aiBaseUrl = value)
    }

    fun onAiApiKeyChange(value: String) {
        _state.value = _state.value.copy(aiApiKey = value)
    }

    fun onAiModelChange(value: String) {
        _state.value = _state.value.copy(aiModel = value)
    }

    fun onDailyGoalChange(value: String) {
        _state.value = _state.value.copy(dailyGoal = value.filter { it.isDigit() }.take(3))
    }

    fun saveSettings() {
        viewModelScope.launch {
            val s = ServiceLocator.settingsRepository
            s.setBankSourceUrl(_state.value.sourceUrl)
            s.setAiBaseUrl(_state.value.aiBaseUrl)
            s.setAiApiKey(_state.value.aiApiKey)
            s.setAiModel(_state.value.aiModel)
            _state.value.dailyGoal.toIntOrNull()?.let { s.setDailyGoal(it) }
            _state.value = _state.value.copy(message = "设置已保存")
        }
    }

    fun checkUpdate() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = "正在检查更新…")
            ServiceLocator.settingsRepository.setBankSourceUrl(_state.value.sourceUrl)
            val result = ServiceLocator.bankUpdater.checkAndUpdate(_state.value.sourceUrl)
            _state.value = _state.value.copy(busy = false, message = result.message)
            refreshVersions()
        }
    }

    fun importLocalText(text: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { ServiceLocator.bankUpdater.importFromText(text) }
            _state.value = _state.value.copy(message = result.message)
            refreshVersions()
        }
    }
}

@Composable
fun BankScreen(viewModel: BankViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "2.0.0"
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.let { viewModel.importLocalText(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("题库与设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "题库托管在 GitHub，填入仓库地址即可增量更新",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "学习科目") {
                Text(
                    "一个人通常只考一门科目，选定后首页、错题本、统计都会默认跟随它",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.softkao.trainer.core.model.ExamId.entries.forEach { exam ->
                        FilterChip(
                            selected = state.currentExamId == exam.id,
                            onClick = { viewModel.setCurrentExam(exam.id) },
                            label = {
                                Text(
                                    exam.shortName,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "题库更新源") {
                OutlinedTextField(
                    value = state.sourceUrl,
                    onValueChange = viewModel::onSourceUrlChange,
                    label = { Text("仓库地址（raw 或 https）") },
                    placeholder = { Text("https://raw.githubusercontent.com/<user>/softkao-trainer/main/question-bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::checkUpdate, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Text(if (state.busy) "更新中…" else "检查更新")
                    }
                    OutlinedButton(
                        onClick = { filePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("导入本地文件") }
                }
                if (state.message != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "本地题库版本") {
                if (state.versions.isEmpty()) {
                    Text(
                        "尚未导入题库，首次启动会自动灌入内置种子题库",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.versions.forEach { v ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(v.examId, style = MaterialTheme.typography.bodyMedium)
                            Text("v${v.version} · ${v.questionCount} 题", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "AI 深度解析（可选）") {
                Text(
                    "填写任意 OpenAI 兼容接口，未配置时使用题库自带解析",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.aiBaseUrl,
                    onValueChange = viewModel::onAiBaseUrlChange,
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.aiApiKey,
                    onValueChange = viewModel::onAiApiKeyChange,
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.aiModel,
                    onValueChange = viewModel::onAiModelChange,
                    label = { Text("模型名") },
                    placeholder = { Text("gpt-4o-mini / qwen-plus") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "学习设置") {
                OutlinedTextField(
                    value = state.dailyGoal,
                    onValueChange = viewModel::onDailyGoalChange,
                    label = { Text("每日目标题量") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = viewModel::saveSettings, modifier = Modifier.fillMaxWidth()) { Text("保存设置") }
            }
        }

        item {
            com.softkao.trainer.feature.common.SectionCard(title = "关于") {
                Text(
                    "软考刷题 · SoftKao Trainer v$appVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "题库版本：${state.versions.joinToString("，") { "${it.examId} v${it.version}（${it.questionCount} 题）" }.ifEmpty { "尚未导入" }}\n" +
                        "内置题库：${state.versions.sumOf { it.questionCount }} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "开源项目，题库与解析以 JSON 形式托管在 GitHub，欢迎提 PR 补充题目。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
