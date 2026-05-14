package com.absinthe.libchecker.ui.base

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.LocaleUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.applySystemBarsMargin
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import java.util.Locale
import timber.log.Timber

abstract class BaseActivity<VB : ViewBinding> :
  AppCompatActivity(),
  IBinding<VB> {

  override lateinit var binding: VB
  private var appliedLocale: Locale? = null

  override fun attachBaseContext(newBase: Context) {
    val locale = GlobalValues.locale
    appliedLocale = locale
    super.attachBaseContext(LocaleUtils.wrapContext(newBase, locale))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    onApplyUserThemeResource(theme, false)
    if (shouldApplyTranslucentSystemBars()) {
      onApplyTranslucentSystemBars()
    }
    super.onCreate(savedInstanceState)
    binding = (inflateBinding(layoutInflater) as VB).also {
      setContentView(it.root)
    }
    if (shouldApplyTranslucentSystemBars()) {
      onApplyContentWindowInsets()
    }
  }

  override fun onResume() {
    super.onResume()
    if (OsUtils.atLeastT()) {
      if (appliedLocale != GlobalValues.locale) {
        recreate()
      }
    }
  }

  override fun invalidateMenu() {
    // It will somehow cause a crash when calling super.invalidateMenu() in some cases
    // java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.
    runCatching {
      super.invalidateMenu()
    }.onFailure {
      Timber.e(it)
    }
  }

  open fun shouldApplyTranslucentSystemBars(): Boolean {
    return true
  }

  open fun computeUserThemeKey(): String {
    return GlobalValues.darkMode
  }

  open fun onApplyTranslucentSystemBars() {
    enableEdgeToEdge()
    if (OsUtils.atLeastQ()) {
      window.isNavigationBarContrastEnforced = false
    }
  }

  open fun onApplyContentWindowInsets() {
    binding.root.applySystemBarsPadding(left = true, right = true)
    binding.root.findViewById<View>(R.id.appbar)?.applySystemBarsPadding(top = true)
    binding.root.findViewById<View>(R.id.progress_horizontal)?.applySystemBarsMargin(top = true)
  }

  open fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
    theme.applyStyle(R.style.ThemeOverlay, true)
  }

  protected fun isBindingInitialized(): Boolean {
    return ::binding.isInitialized
  }
}
