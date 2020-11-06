package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import com.absinthe.libchecker.extensions.paddingTopCompat
import com.absinthe.libchecker.extensions.setSystemPadding
import com.absinthe.libchecker.ui.app.AppActivity
import com.absinthe.libraries.utils.utils.UiUtils.setSystemBarStyle
import com.blankj.utilcode.util.BarUtils

@SuppressLint("Registered")
abstract class BaseActivity : AppActivity() {

    protected var root: ViewGroup? = null
    protected var isPaddingToolbar = false

    protected abstract fun setViewBinding(): ViewGroup

    protected fun setRootPadding() {
        root?.setSystemPadding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setViewBindingImpl(setViewBinding())
        window.decorView.post { setSystemBarStyle(window) }

        if (isPaddingToolbar) {
            root?.paddingTopCompat = BarUtils.getStatusBarHeight()
        }
    }

    private fun setViewBindingImpl(root: ViewGroup) {
        this.root = root
        setContentView(root)
    }
}