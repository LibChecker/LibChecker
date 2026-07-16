package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.text.LineBreaker
import android.text.Layout
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.children
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringStaticItemDisplay
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.extensions.getColorByAttr

class StaticLibItemView(context: Context) : RuleChipItemView(context) {

  private val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  private val libDetail =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      if (OsUtils.atLeastQ()) {
        breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
      } else if (OsUtils.atLeastO()) {
        // noinspection WrongConstant
        breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
      }
      addView(this)
    }

  fun bind(
    display: LibStringStaticItemDisplay,
    highlightText: String,
    colorfulRuleIcon: Boolean
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    libDetail.text = display.detail?.let(::renderDetail) ?: ""
    bindRuleChip(display.rule, colorfulRuleIcon)
    contentDescription = display.contentDescription
  }

  private fun renderDetail(detail: LibStringStaticItemDisplay.Detail): CharSequence {
    return buildSpannedString {
      bold { append("[Path] ") }
      append(detail.path).appendLine()
      bold { append("[Version Code] ") }
      append(detail.versionCode.toString()).appendLine()
      bold { append("[Cert] ") }
      append(detail.certificateDigest)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val textWidth = measuredWidth - paddingStart - paddingEnd
    if (libName.measuredWidth > textWidth) {
      libName.measure(textWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    }
    if (libDetail.measuredWidth > textWidth) {
      libDetail.measure(textWidth.toExactlyMeasureSpec(), libDetail.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (
        libName.measuredHeight +
          libDetail.measuredHeight +
          (ruleChip?.measuredHeight ?: 0) +
          paddingTop +
          paddingBottom
        ).coerceAtLeast(40.dp)
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libDetail.layout(paddingStart, libName.bottom)
    ruleChip?.layout(paddingStart, libDetail.bottom)
  }
}
