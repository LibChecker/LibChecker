package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.absinthe.libchecker.ui.app.AppActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.UiUtils.setDarkMode
import com.absinthe.libchecker.utils.UiUtils.setSystemBarTransparent
import com.blankj.utilcode.util.BarUtils
import java.lang.ref.WeakReference

@SuppressLint("Registered")
abstract class BaseActivity : AppActivity() {

    protected var root: View? = null
    protected var isPaddingToolbar = false
    private lateinit var reference: WeakReference<BaseActivity>

    protected abstract fun setViewBinding(): View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewBindingImpl(setViewBinding())

        setDarkMode(this)
        setSystemBarTransparent(this)

        if (isPaddingToolbar) {
            root?.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
        }

        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }

    private fun setViewBindingImpl(root: View) {
        this.root = root
        setContentView(root)
    }
}