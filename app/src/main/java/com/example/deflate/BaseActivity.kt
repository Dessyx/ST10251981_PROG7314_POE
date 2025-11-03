package com.example.deflate

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localeUpdatedContext = LocaleHelper.setLocale(newBase, LocaleHelper.getLocale(newBase))
        super.attachBaseContext(localeUpdatedContext)
    }
}

