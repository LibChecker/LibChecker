package com.absinthe.libchecker.domain.snapshot.album.ui.view

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.album.model.AlbumItemDisplayData
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.view.app.TextColumnRowView
import com.google.android.material.card.MaterialCardView

class AlbumItemView(context: Context) : MaterialCardView(context) {

  private val row = AlbumRow(context).apply {
    val horizontal = context.getDimensionPixelSize(R.dimen.album_card_inset_horizontal)
    val vertical = context.getDimensionPixelSize(R.dimen.album_card_inset_vertical)
    setPadding(horizontal, vertical, horizontal, vertical)
  }

  init {
    setSmoothRoundCorner(12.dp)
    setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
    addView(row)
  }

  fun render(data: AlbumItemDisplayData) {
    row.icon.setImageResource(data.iconRes)
    row.icon.backgroundTintList = data.iconBackgroundColorRes.toColorStateList(context)
    row.title.text = data.title
    row.subtitle.text = data.subtitle
    contentDescription = data.contentDescription
  }

  private class AlbumRow(context: Context) : TextColumnRowView(context) {
    val icon = AppCompatImageView(context).apply {
      val size = context.getDimensionPixelSize(R.dimen.album_card_icon_size)
      layoutParams = ViewGroup.LayoutParams(size, size)
      setBackgroundResource(R.drawable.bg_circle_secondary_container)
      scaleType = ImageView.ScaleType.CENTER_INSIDE
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    val title = addTextLine {
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleLarge))
    }
    val subtitle = addTextLine {
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    }

    init {
      setLeadingView(icon, context.getDimensionPixelSize(R.dimen.album_card_inset_horizontal))
      centerTextColumn()
    }
  }
}
