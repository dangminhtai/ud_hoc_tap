package com.duong.udhoctap.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ud_hoc_tap_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_DEFAULT_KB = "default_kb"
        private const val KEY_DEFAULT_ENABLE_RAG = "default_enable_rag"
        private const val KEY_DEFAULT_ENABLE_WEB_SEARCH = "default_enable_web_search"
        private const val KEY_LANGUAGE = "app_language"

        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8001"
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 20)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var dailyGoal: Int
        get() = prefs.getInt(KEY_DAILY_GOAL, 20)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value).apply()

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value).apply()

    var defaultKbName: String
        get() = prefs.getString(KEY_DEFAULT_KB, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEFAULT_KB, value).apply()

    var defaultEnableRag: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_ENABLE_RAG, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_ENABLE_RAG, value).apply()

    var defaultEnableWebSearch: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_ENABLE_WEB_SEARCH, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_ENABLE_WEB_SEARCH, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "vi") ?: "vi"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
}
