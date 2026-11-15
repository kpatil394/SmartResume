
package com.example.smartresume

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "smartresume_preferences"
    private const val KEY_DARK_MODE = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        preferences(context)
            .edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
}
