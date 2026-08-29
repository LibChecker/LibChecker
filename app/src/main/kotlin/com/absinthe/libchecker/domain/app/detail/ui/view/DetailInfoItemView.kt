package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoItemDisplay
import com.absinthe.libchecker.domain.app.detail.model.DetailInfoTextStyle
import com.absinthe.libchecker.domain.app.detail.model.buildDetailItemDescription
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.app.TextColumnRowView

class DetailInfoItemView(context: Context) : TextColumnRowView(context) {

  private val icon = AppCompatImageView(context).apply {
    layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val tip = addTextLine {
    alpha = 0.65f
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
  }
  private val text = addTextLine()

  init {
    setPadding(8.dp, 8.dp, 8.dp, 8.dp)
    setBackgroundResource(R.drawable.bg_lib_detail_item)
    setLeadingView(icon)
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
    text.isClickable = true
    text.movementMethod = LinkMovementMethod.getInstance()
    text.text = SpannableString(" ${item.text} ").apply {
      setSpan(URLSpan(linkUrl), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
  }
}
