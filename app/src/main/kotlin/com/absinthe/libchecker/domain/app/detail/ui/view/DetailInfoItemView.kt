package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.buildDetailItemDescription
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup

class DetailInfoItemView(context: Context) : AViewGroup(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  private val tip = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 8.dp
    }
    alpha = 0.65f
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
  }

  private val text = AppCompatTextView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    ).also {
      it.marginStart = 8.dp
      it.topMargin = 0
    }
  }

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    addView(icon)
    addView(tip)
    addView(text)
  }

  fun bind(item: DetailInfoItemDisplay) {
    icon.setImageResource(item.iconRes)
    tip.setText(item.tipRes)
    text.setTextAppearance(
      context.getResourceIdByAttr(
        when (item.textStyle) {
          DetailInfoTextStyle.TITLE -> com.google.android.material.R.attr.textAppearanceTitleSmall
          DetailInfoTextStyle.BODY -> com.google.android.material.R.attr.textAppearanceBodyMedium
        }
      )
    )
    bindText(item)
    contentDescription = buildDetailItemDescription(tip.text, text.text)
  }

  private fun bindText(item: DetailInfoItemDisplay) {
    val linkUrl = item.linkUrl
    if (linkUrl == null) {
      text.isClickable = false
      text.movementMethod = null
      text.text = item.text
      return
    }

    val linkedText = SpannableString(" ${item.text} ").apply {
      setSpan(URLSpan(linkUrl), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    text.isClickable = true
    text.movementMethod = LinkMovementMethod.getInstance()
    text.text = linkedText
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val textWidth = measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - tip.marginStart
    if (tip.measuredWidth > textWidth) {
      tip.measure(textWidth.toExactlyMeasureSpec(), tip.defaultHeightMeasureSpec(this))
    }
    if (text.measuredWidth > textWidth) {
      text.measure(textWidth.toExactlyMeasureSpec(), text.defaultHeightMeasureSpec(this))
    }
    setMeasuredDimension(
      measuredWidth,
      (tip.measuredHeight + text.marginTop + text.measuredHeight).coerceAtLeast(icon.measuredHeight) + paddingTop + paddingBottom
    )
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    icon.layout(paddingStart, icon.toVerticalCenter(this))
    tip.layout(paddingStart + icon.measuredWidth + tip.marginStart, paddingTop)
    text.layout(paddingStart + icon.measuredWidth + tip.marginStart, tip.bottom + text.marginTop)
  }
}
