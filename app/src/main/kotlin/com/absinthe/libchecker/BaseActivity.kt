package com.absinthe.libchecker

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.libchecker.utils.ActivityStackManager
import com.absinthe.libchecker.utils.UiUtils.setDarkMode
import com.absinthe.libchecker.utils.UiUtils.setSystemBarTransparent
import com.blankj.utilcode.util.BarUtils
import java.lang.ref.WeakReference

@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity() {

    protected var root: View? = null
    private lateinit var reference: WeakReference<BaseActivity>

    protected abstract fun setViewBinding()
    protected abstract fun setRoot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setViewBinding()

        setDarkMode(this)
        setSystemBarTransparent(this)
        setRoot()
        root?.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }
}