package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import com.absinthe.libchecker.extensions.paddingTopCompat
import com.absinthe.libchecker.extensions.setSystemPadding
import com.absinthe.libchecker.ui.app.AppActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.UiUtils.setSystemBarStyle
import com.blankj.utilcode.util.BarUtils
import java.lang.ref.WeakReference

@SuppressLint("Registered")
abstract class BaseActivity : AppActivity() {

    protected var root: ViewGroup? = null
    protected var isPaddingToolbar = false
    private lateinit var reference: WeakReference<BaseActivity>

    protected abstract fun setViewBinding(): ViewGroup

    protected fun setRootPadding() {
        root?.setSystemPadding()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)

        setViewBindingImpl(setViewBinding())
        setSystemBarStyle(this)

        if (isPaddingToolbar) {
            root?.paddingTopCompat = BarUtils.getStatusBarHeight()
        }
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }

    private fun setViewBindingImpl(root: ViewGroup) {
        this.root = root
        setContentView(root)
    }
}