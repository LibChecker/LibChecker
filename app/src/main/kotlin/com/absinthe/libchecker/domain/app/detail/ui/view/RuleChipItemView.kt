package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.rulesbundle.Rule
import com.google.android.material.chip.Chip

abstract class RuleChipItemView(context: Context) : AViewGroup(context) {

  protected var ruleChip: Chip? = null
    private set

  private var chipRule: Rule? = null
  private var chipColorfulIcon: Boolean? = null

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
  }

  protected fun bindRuleChip(rule: Rule?, colorfulIcon: Boolean) {
    if (chipRule == rule && chipColorfulIcon == colorfulIcon) {
      return
    }
    chipRule = rule
    chipColorfulIcon = colorfulIcon
    ruleChip = rule?.let {
      getOrCreateRuleChip().apply {
        text = it.label
        chipIcon = RuleChipIconCache.newDrawable(context, it, colorfulIcon)
      }
    } ?: run {
      ruleChip?.let(::removeView)
      null
    }
  }

  private fun getOrCreateRuleChip() = ruleChip ?: Chip(context).apply {
    isClickable = false
    isFocusable = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
    maxWidth = (context.displayWidth * 0.45f).toInt()
    ellipsize = TextUtils.TruncateAt.MIDDLE
    addView(this)
  }
}
