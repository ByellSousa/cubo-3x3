package com.gabs.cubo3x3.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gabs.cubo3x3.domain.reminder.ReminderDay
import com.gabs.cubo3x3.domain.reminder.ReminderSettings
import com.gabs.cubo3x3.ui.theme.ThemeMode
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlan
import com.gabs.cubo3x3.domain.timer.CfopTrainingPlanCodec
import com.gabs.cubo3x3.domain.training.DailyGoalSettings
import com.gabs.cubo3x3.domain.training.DailyGoalSettingsCodec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferences(context: Context, storeOverride: DataStore<Preferences>? = null) {
    private val dataStore = storeOverride ?: context.applicationContext.appDataStore
    private val safePreferences = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        } else {
            throw error
        }
    }

    val themeMode: Flow<ThemeMode> = safePreferences
        .map { preferences ->
            ThemeMode.fromStorage(preferences[ThemeModeKey])
        }

    val reminderSettings: Flow<ReminderSettings> = safePreferences.map { preferences ->
        val days = preferences[ReminderDaysKey]
            ?.mapNotNull { stored -> ReminderDay.entries.firstOrNull { it.name == stored } }
            ?.toSet()
            ?: ReminderDay.entries.toSet()
        val enabled = preferences[ReminderEnabledKey] ?: false
        ReminderSettings(
            enabled = enabled && days.isNotEmpty(),
            hour = (preferences[ReminderHourKey] ?: 19).coerceIn(0, 23),
            minute = (preferences[ReminderMinuteKey] ?: 0).coerceIn(0, 59),
            days = days,
        )
    }

    val timerInspectionEnabled: Flow<Boolean> = safePreferences.map { preferences ->
        preferences[TimerInspectionEnabledKey] ?: false
    }

    val timerInspectionSoundsEnabled: Flow<Boolean> = safePreferences.map { preferences ->
        preferences[TimerInspectionSoundsEnabledKey] ?: false
    }

    val selectedTimerSessionId: Flow<Long> = safePreferences.map { preferences ->
        preferences[SelectedTimerSessionIdKey] ?: 1L
    }

    val cfopTrainingPlan: Flow<CfopTrainingPlan> = safePreferences.map { preferences ->
        preferences[CfopTrainingPlanKey]?.let {
            runCatching { CfopTrainingPlanCodec.decode(it) }.getOrNull()
        } ?: CfopTrainingPlan()
    }

    val dailyGoals: Flow<DailyGoalSettings> = safePreferences.map { preferences ->
        preferences[DailyGoalsKey]?.let {
            runCatching { DailyGoalSettingsCodec.decode(it) }.getOrNull()
        } ?: DailyGoalSettings()
    }

    suspend fun setDailyGoals(settings: DailyGoalSettings) {
        val encoded = DailyGoalSettingsCodec.encode(settings)
        dataStore.edit { preferences -> preferences[DailyGoalsKey] = encoded }
    }

    suspend fun setCfopTrainingPlan(plan: CfopTrainingPlan) {
        val encoded = CfopTrainingPlanCodec.encode(plan)
        dataStore.edit { preferences -> preferences[CfopTrainingPlanKey] = encoded }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[ThemeModeKey] = mode.storageValue
        }
    }

    suspend fun setReminderSettings(settings: ReminderSettings) {
        dataStore.edit { preferences ->
            preferences[ReminderEnabledKey] = settings.enabled
            preferences[ReminderHourKey] = settings.hour
            preferences[ReminderMinuteKey] = settings.minute
            preferences[ReminderDaysKey] = settings.days.map(ReminderDay::name).toSet()
        }
    }

    suspend fun setTimerInspectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TimerInspectionEnabledKey] = enabled
            if (!enabled) preferences[TimerInspectionSoundsEnabledKey] = false
        }
    }

    suspend fun setTimerInspectionSoundsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[TimerInspectionSoundsEnabledKey] = enabled
        }
    }

    suspend fun setSelectedTimerSessionId(id: Long) {
        require(id > 0) { "Session id must be positive" }
        dataStore.edit { preferences -> preferences[SelectedTimerSessionIdKey] = id }
    }

    private companion object {
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val ReminderEnabledKey = booleanPreferencesKey("reminder_enabled")
        val ReminderHourKey = intPreferencesKey("reminder_hour")
        val ReminderMinuteKey = intPreferencesKey("reminder_minute")
        val ReminderDaysKey = stringSetPreferencesKey("reminder_days")
        val TimerInspectionEnabledKey = booleanPreferencesKey("timer_inspection_enabled")
        val TimerInspectionSoundsEnabledKey =
            booleanPreferencesKey("timer_inspection_sounds_enabled")
        val SelectedTimerSessionIdKey = longPreferencesKey("selected_timer_session_id")
        val CfopTrainingPlanKey = stringPreferencesKey("cfop_training_plan")
        val DailyGoalsKey = stringPreferencesKey("daily_goals")
    }
}
