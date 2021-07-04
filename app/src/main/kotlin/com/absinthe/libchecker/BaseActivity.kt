package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import com.absinthe.libchecker.extensions.setSystemPadding
import com.absinthe.libraries.utils.utils.UiUtils.setSystemBarStyle
import rikka.material.app.MaterialActivity

@SuppressLint("Registered, MissingSuperCall")
abstract class BaseActivity : MaterialActivity() {

    protected var root: ViewGroup? = null

    protected abstract fun setViewBinding(): ViewGroup

    protected fun setRootPadding() {
        root?.setSystemPadding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewBindingImpl(setViewBinding())
        window.decorView.post { setSystemBarStyle(window) }
    }

    override fun shouldApplyTranslucentSystemBars(): Boolean {
        return true
    }

    override fun computeUserThemeKey(): String {
        return ""
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        theme.applyStyle(R.style.ThemeOverlay, true)
    }

    private fun setViewBindingImpl(root: ViewGroup) {
        this.root = root
        setContentView(root)
    }
}
