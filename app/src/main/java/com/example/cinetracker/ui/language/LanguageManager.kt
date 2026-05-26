package com.example.cinetracker.ui.language

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "cine_track_preferences"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_LAST_SYNCED_CONTENT_LANGUAGE = "last_synced_content_language"

    fun currentLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromTag(prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SLOVAK.tag))
    }

    fun wrapContext(context: Context): Context {
        val language = currentLanguage(context)
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    fun updateLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (currentLanguage(context) == language) return

        prefs.edit {
            putString(KEY_APP_LANGUAGE, language.tag)
        }

        context.findActivity()?.recreate()
    }

    fun currentTmdbLanguage(context: Context): String = when (currentLanguage(context)) {
        AppLanguage.ENGLISH -> "en-US"
        AppLanguage.SLOVAK -> "sk-SK"
    }

    fun shouldRefreshLocalizedContent(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SYNCED_CONTENT_LANGUAGE, null) != currentTmdbLanguage(context)
    }

    fun markLocalizedContentSynced(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LAST_SYNCED_CONTENT_LANGUAGE, currentTmdbLanguage(context))
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
