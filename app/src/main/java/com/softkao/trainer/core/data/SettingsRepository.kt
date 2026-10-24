package com.softkao.trainer.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "softkao_settings")

/** 本地设置：题库更新源、AI 接口、每日目标 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val bankSourceUrl = stringPreferencesKey("bank_source_url")
        val aiBaseUrl = stringPreferencesKey("ai_base_url")
        val aiApiKey = stringPreferencesKey("ai_api_key")
        val aiModel = stringPreferencesKey("ai_model")
        val dailyGoal = intPreferencesKey("daily_goal")
        val seeded = stringPreferencesKey("seeded_version")
    }

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
