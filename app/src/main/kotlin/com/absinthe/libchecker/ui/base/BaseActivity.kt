package com.absinthe.libchecker.ui.base

import android.content.Context
import android.content.res.Resources
import android.os.BadParcelableException
import android.os.Bundle
import android.text.method.TextKeyListener
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
    super.onCreate(savedInstanceState.discardIfContainsUnreadableParcelable(javaClass.classLoader))
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

  override fun onDestroy() {
    super.onDestroy()
    releaseTextKeyListeners()
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

  // TextKeyListener keeps process-wide singletons; release settings callbacks
  // that may otherwise retain a destroyed Activity context.
  private fun releaseTextKeyListeners() {
    runCatching {
      TextKeyListener.Capitalize.values().forEach { capitalize ->
        TextKeyListener.getInstance(false, capitalize).release()
        TextKeyListener.getInstance(true, capitalize).release()
      }
    }.onFailure {
      Timber.w(it)
    }
  }
}

internal fun Bundle?.discardIfContainsUnreadableParcelable(classLoader: ClassLoader?): Bundle? {
  if (this == null) {
    return null
  }
  return try {
    validateParcelableContents(classLoader)
    this
  } catch (exception: BadParcelableException) {
    Timber.w(exception, "Discarding activity state that cannot be restored")
    null
  }
}

@Suppress("DEPRECATION")
private fun Bundle.validateParcelableContents(classLoader: ClassLoader?) {
  if (classLoader != null) {
    this.classLoader = classLoader
  }
  keySet().forEach { key ->
    when (val value = get(key)) {
      is Bundle -> value.validateParcelableContents(classLoader)
      is Array<*> -> value.filterIsInstance<Bundle>().forEach { it.validateParcelableContents(classLoader) }
      is Iterable<*> -> value.filterIsInstance<Bundle>().forEach { it.validateParcelableContents(classLoader) }
    }
  }
}
