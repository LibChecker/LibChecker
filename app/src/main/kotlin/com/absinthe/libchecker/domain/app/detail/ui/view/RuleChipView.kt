package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.rulesbundle.Rule

internal class RuleChipView(context: Context) : AppCompatTextView(context) {

  private val backgroundDrawable = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE
    cornerRadius = 16.dp.toFloat()
    setColor(context.getColorByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
    setStroke(
      1.dp,
      context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    )
  }

  init {
    isClickable = false
    isFocusable = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    includeFontPadding = false
    gravity = Gravity.CENTER_VERTICAL
    maxLines = 1
    ellipsize = TextUtils.TruncateAt.MIDDLE
    compoundDrawablePadding = 6.dp
    setPaddingRelative(10.dp, 0, 12.dp, 0)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSecondaryContainer))
    background = backgroundDrawable
  }

  fun bind(rule: Rule, colorfulIcon: Boolean) {
    text = rule.label
    setCompoundDrawablesRelativeWithIntrinsicBounds(
      RuleChipIconCache.newDrawable(context, rule, colorfulIcon),
      null,
      null,
      null
    )
  }

  fun clear() {
    text = null
    setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
  }
}
