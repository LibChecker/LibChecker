package com.absinthe.libchecker.base

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.extensions.inflateBinding
import rikka.material.app.MaterialActivity

@SuppressLint("Registered, MissingSuperCall")
abstract class BaseActivity<VB : ViewBinding> : MaterialActivity() {

  protected lateinit var binding: VB

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = inflateBinding(layoutInflater)
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
    if (GlobalValues.md3Theme) {
      theme.applyStyle(R.style.Base_AppTheme_Material3, true)
    }
  }
}
