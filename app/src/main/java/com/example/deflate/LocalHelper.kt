package com.example.deflate

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "app_language"
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
        fun getLocale(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LANGUAGE, "en") ?: "en"

        }
    }


