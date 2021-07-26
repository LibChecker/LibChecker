package com.absinthe.libchecker.base

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import rikka.material.app.MaterialActivity

@SuppressLint("Registered, MissingSuperCall")
abstract class BaseActivity : MaterialActivity() {

  protected var root: ViewGroup? = null

  protected abstract fun setViewBinding(): ViewGroup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setViewBindingImpl(setViewBinding())
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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

  private fun setViewBindingImpl(root: ViewGroup) {
    this.root = root
    setContentView(root)
  }
}
