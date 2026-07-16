package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.Spanned
import android.text.SpannedString
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.view.marginEnd
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringNativeItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.buildLibStringItemDescription
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.view.drawable.CapsuleDrawable
import com.absinthe.libchecker.view.span.CenterAlignImageSpan

class NativeLibItemView(context: Context) : RuleChipItemView(context) {

  private val libName =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginEnd = context.getDimensionPixelSize(R.dimen.normal_padding)
      }
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      addView(this)
    }

  private val libSize =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(0, 1.dp, 0, 1.dp)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      setLineSpacing(2.dp.toFloat(), 1.2f)
      addView(this)
    }

  private val nativeLabelSpanCache = mutableMapOf<String, SpannedString>()

  fun bind(
    display: LibStringNativeItemDisplay,
    highlightText: String,
    colorfulRuleIcon: Boolean
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    libSize.text = buildNativeSizeText(display)
    bindRuleChip(display.rule, colorfulRuleIcon)
    contentDescription = display.contentDescription
  }

  fun bindText(
    name: CharSequence,
    secondaryText: CharSequence
  ) {
    libName.text = name
    libSize.text = secondaryText
    bindRuleChip(null, false)
    contentDescription = buildLibStringItemDescription(name, secondaryText)
  }

  private fun buildNativeSizeText(display: LibStringNativeItemDisplay): CharSequence {
    return buildSpannedString {
      append(display.sizeText)
      display.labels.forEach { label ->
        append(createNativeLabelSpan(label))
      }
    }
  }

  private fun createNativeLabelSpan(text: String): SpannedString = nativeLabelSpanCache.getOrPut(text) {
    buildSpannedString {
      append(" $text ")
      val capsuleDrawable = CapsuleDrawable(
        context = context,
        text = text,
        textSize = 10.dp.toFloat(),
        textColor = context.getColorByAttr(com.google.android.material.R.attr.colorOnSecondaryFixed),
        backgroundColor = context.getColorByAttr(com.google.android.material.R.attr.colorSecondaryFixed),
        borderColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant),
        borderWidth = 1f,
        cornerRadius = 5.dp.toFloat()
      )
      capsuleDrawable.setBounds(0, 0, capsuleDrawable.intrinsicWidth, capsuleDrawable.intrinsicHeight)
      setSpan(
        CenterAlignImageSpan(capsuleDrawable),
        1,
        1 + text.length,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    val chipWidth = ruleChip?.let {
      it.autoMeasure()
      it.measuredWidth + libName.marginEnd
    } ?: 0
    val textWidth =
      (measuredWidth - paddingStart - paddingEnd - libName.marginEnd - chipWidth).coerceAtLeast(0)

    libName.measure(textWidth.toAtMostMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    libSize.measure(textWidth.toAtMostMeasureSpec(), libSize.defaultHeightMeasureSpec(this))

    setMeasuredDimension(
      measuredWidth,
      (libName.measuredHeight + libSize.measuredHeight + paddingTop + paddingBottom).coerceAtLeast(
        40.dp
      )
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    libName.layout(paddingStart, paddingTop)
    libSize.layout(paddingStart, libName.bottom)
    ruleChip?.let { it.layout(paddingEnd, it.toVerticalCenter(this), fromRight = true) }
  }
}
