package com.absinthe.libchecker.ui.app

import android.annotation.SuppressLint
import android.content.res.Resources
import com.absinthe.libchecker.R
import rikka.material.app.MaterialActivity

@SuppressLint("Registered")
open class AppActivity : MaterialActivity() {

    override fun shouldApplyTranslucentSystemBars(): Boolean {
        return true
    }

    override fun computeUserThemeKey(): String {
        return ""
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        theme.applyStyle(R.style.ThemeOverlay, true)
    }
}