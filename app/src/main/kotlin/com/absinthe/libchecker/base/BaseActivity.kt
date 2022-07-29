package com.absinthe.libchecker.base

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.inflateBinding
import rikka.material.app.MaterialActivity

abstract class BaseActivity<VB : ViewBinding> : MaterialActivity() {

  protected lateinit var binding: VB

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!this::binding.isInitialized) {
      binding = inflateBinding(layoutInflater)
    }
    setContentView(binding.root)
  }

  override fun shouldApplyTranslucentSystemBars(): Boolean {
    return true
  }

  override fun computeUserThemeKey(): String {
    return GlobalValues.darkMode + GlobalValues.rengeTheme
  }

  override fun onApplyTranslucentSystemBars() {
    super.onApplyTranslucentSystemBars()
    window.statusBarColor = Color.TRANSPARENT
    window.decorView.post {
      window.navigationBarColor = Color.TRANSPARENT
      if (OsUtils.atLeastQ()) {
        window.isNavigationBarContrastEnforced = false
      }
    }
  }

  override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
    theme.applyStyle(R.style.ThemeOverlay, true)

    if (GlobalValues.rengeTheme) {
      theme.applyStyle(R.style.ThemeOverlay_Renge, true)
    }
  }
}
