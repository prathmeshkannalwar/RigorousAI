package com.example.Rigorous_X

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var typewriterSpeed: Long
        get() = prefs.getLong(KEY_SPEED, 5L) // Default speed is 5ms
        set(value) = prefs.edit().putLong(KEY_SPEED, value).apply()

    var theme: Int
        get() = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            prefs.edit().putInt(KEY_THEME, value).apply()
            AppCompatDelegate.setDefaultNightMode(value)
        }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_SPEED = "typewriter_speed"
        private const val KEY_THEME = "app_theme"
    }
}