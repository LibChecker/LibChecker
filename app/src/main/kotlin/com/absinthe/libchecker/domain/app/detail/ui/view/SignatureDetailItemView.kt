package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageButton
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.domain.app.detail.model.SignatureDetailItem
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.view.app.TextColumnRowView
import rikka.core.util.ClipboardUtils

class SignatureDetailItemView(context: Context) : TextColumnRowView(context) {

  private val type = addTextLine {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }
  private val content = addTextLine {
    (layoutParams as LinearLayout.LayoutParams).topMargin = 4.dp
  }
  private val copyToClipboard = AppCompatImageButton(context).apply {
    layoutParams = LinearLayout.LayoutParams(24.dp, 24.dp)
    scaleX = 0.8f
    scaleY = 0.8f
    setImageResource(R.drawable.ic_twotone_content_copy_24)
    contentDescription = context.getString(android.R.string.copy)
    setBackgroundDrawable(context.getDrawableByAttr(android.R.attr.selectableItemBackgroundBorderless))
    setOnClickListener {
      ClipboardUtils.put(context, "${type.text}:${content.text}")
      VersionCompat.showCopiedOnClipboardToast(context)
    }
  }

  init {
    setPadding(8.dp, 2.dp, 8.dp, 2.dp)
    minimumHeight = 40.dp
    setTrailingView(
      copyToClipboard,
      gravity = Gravity.TOP,
      touchTargetSize = 48.dp
    )
  }

  fun bind(item: SignatureDetailItem) {
    type.text = item.type
    content.text = item.content
    contentDescription = item.contentDescription
  }
}
