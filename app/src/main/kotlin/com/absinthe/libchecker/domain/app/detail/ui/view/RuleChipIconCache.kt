package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.rulesbundle.Rule

internal object RuleChipIconCache {
  private val iconStates = mutableMapOf<Int, Drawable.ConstantState?>()
  private val grayscaleColorFilter by lazy {
    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
  }

  fun preload(context: Context, rules: Sequence<Rule>) {
    rules
      .map { it.iconRes }
      .distinct()
      .forEach { getOrLoadConstantState(context, it) }
  }

  fun newDrawable(context: Context, rule: Rule, colorfulIcon: Boolean): Drawable? {
    val drawable = getOrLoadConstantState(context, rule.iconRes)?.newDrawable(context.resources, context.theme)
      ?: rule.iconRes.getDrawable(context)

    return drawable?.mutate()?.apply {
      colorFilter = if (!colorfulIcon && !rule.isSimpleColorIcon) {
        grayscaleColorFilter
      } else {
        null
      }
    }
  }

  private fun getOrLoadConstantState(context: Context, iconRes: Int): Drawable.ConstantState? {
    synchronized(iconStates) {
      if (iconStates.containsKey(iconRes)) {
        return iconStates[iconRes]
      }
    }

    val constantState = iconRes.getDrawable(context)?.constantState
    synchronized(iconStates) {
      return iconStates.getOrPut(iconRes) { constantState }
    }
  }
}
