package com.absinthe.libchecker.domain.about.ui.view

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import coil.request.CachePolicy
import coil.transform.CircleCropTransformation
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.about.model.DeveloperInfo
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.app.TextColumnRowView
import com.google.android.material.card.MaterialCardView

class DeveloperItemView(context: Context) : MaterialCardView(context) {

  private val row = DeveloperRow(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  init {
    setSmoothRoundCorner(16.dp)
    strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
    addView(row)
  }

  fun bind(item: DeveloperInfo, onClick: () -> Unit) {
    val avatarCacheKey = "developer_avatar:${item.avatarUrl}"
    row.icon.load(item.avatarUrl) {
      memoryCacheKey(avatarCacheKey)
      diskCacheKey(avatarCacheKey)
      placeholderMemoryCacheKey(avatarCacheKey)
      memoryCachePolicy(CachePolicy.ENABLED)
      diskCachePolicy(CachePolicy.ENABLED)
      networkCachePolicy(CachePolicy.ENABLED)
      transformations(CircleCropTransformation())
    }
    row.name.text = item.name
    row.description.text = item.desc
    contentDescription = listOf(item.name, item.desc).joinToString()
    setOnClickListener { onClick() }
  }

  private class DeveloperRow(context: Context) : TextColumnRowView(context) {
    val icon = AppCompatImageView(context).apply {
      id = android.R.id.icon
      val size = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = ViewGroup.LayoutParams(size, size)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val name = addTextLine {
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium))
    }
    val description = addTextLine {
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall))
    }

    init {
      setLeadingView(icon)
    }
  }
}
