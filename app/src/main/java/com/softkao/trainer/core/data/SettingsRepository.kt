package com.softkao.trainer.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.softkao.trainer.core.model.ExamId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "softkao_settings")

/** 本地设置：题库更新源、AI 接口、每日目标、全局科目 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val bankSourceUrl = stringPreferencesKey("bank_source_url")
        val aiBaseUrl = stringPreferencesKey("ai_base_url")
        val aiApiKey = stringPreferencesKey("ai_api_key")
        val aiModel = stringPreferencesKey("ai_model")
        val dailyGoal = intPreferencesKey("daily_goal")
        val seeded = stringPreferencesKey("seeded_version")

        /** 全局唯一的当前科目 */
        val currentExamId = stringPreferencesKey("current_exam_id")
    }

    /**
     * 当前科目，全站唯一。
     *
     * 一个人通常只考一门科目，所以这里只保留一个值：首页、知识点、错题本、
     * 统计、搜索、设置全部共用同一份，任何一处切换都会同步到其它页面。
     * 两门考试的数据本身完全隔离（题库、错题、掌握度各算各的）。
     */
    val currentExamId: Flow<String> = context.settingsStore.data.map {
        it[Keys.currentExamId] ?: ExamId.SYSTEM_INTEGRATION.id
    }

    /** 切换全局科目 */
    suspend fun setCurrentExamId(id: String) =
        context.settingsStore.edit { it[Keys.currentExamId] = id }

    val bankSourceUrl: Flow<String> = context.settingsStore.data.map { it[Keys.bankSourceUrl] ?: "" }
    val aiBaseUrl: Flow<String> = context.settingsStore.data.map { it[Keys.aiBaseUrl] ?: "" }
    val aiApiKey: Flow<String> = context.settingsStore.data.map { it[Keys.aiApiKey] ?: "" }
    val aiModel: Flow<String> = context.settingsStore.data.map { it[Keys.aiModel] ?: "" }
    val dailyGoal: Flow<Int> = context.settingsStore.data.map { it[Keys.dailyGoal] ?: 20 }

    suspend fun setBankSourceUrl(value: String) = context.settingsStore.edit { it[Keys.bankSourceUrl] = value.trim() }
    suspend fun setAiBaseUrl(value: String) = context.settingsStore.edit { it[Keys.aiBaseUrl] = value.trim() }
    suspend fun setAiApiKey(value: String) = context.settingsStore.edit { it[Keys.aiApiKey] = value.trim() }
    suspend fun setAiModel(value: String) = context.settingsStore.edit { it[Keys.aiModel] = value.trim() }
    suspend fun setDailyGoal(value: Int) = context.settingsStore.edit { it[Keys.dailyGoal] = value.coerceIn(5, 200) }
    suspend fun markSeeded(version: String) = context.settingsStore.edit { it[Keys.seeded] = version }
    suspend fun seededVersion(): String {
        var result = ""
        context.settingsStore.edit { result = it[Keys.seeded] ?: "" }
        return result
    }
}
