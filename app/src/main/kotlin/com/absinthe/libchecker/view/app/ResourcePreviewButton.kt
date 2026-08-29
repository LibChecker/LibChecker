package com.absinthe.libchecker.view.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr

class ResourcePreviewButton(context: Context) : AppCompatImageButton(context) {

  init {
    layoutParams = ViewGroup.LayoutParams(24.dp, 24.dp)
    scaleType = ImageView.ScaleType.CENTER
    setImageResource(R.drawable.ic_outline_change_circle_24)
    contentDescription = context.getString(R.string.lib_detail_app_props_tip)
    setBackgroundDrawable(context.getDrawableByAttr(android.R.attr.selectableItemBackgroundBorderless))
    isVisible = false
  }

  fun bind(preview: AppResourcePreview, onClick: (() -> Unit)?) {
    isVisible = onClick != null
    setOnClickListener(onClick?.let { action -> View.OnClickListener { action() } })
    when (preview) {
      is AppResourcePreview.DrawableValue -> setImageBitmap(
        preview.drawable.toBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
      )

      is AppResourcePreview.ColorValue -> setImageBitmap(
        ShapeDrawable(OvalShape()).apply {
          paint.color = preview.color
        }.toBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
      )

      AppResourcePreview.Original,
      is AppResourcePreview.Text -> setImageResource(R.drawable.ic_outline_change_circle_24)
    }
  }

  private val previewWidth: Int
    get() = measuredWidth.takeIf { it > 0 } ?: layoutParams.width

  private val previewHeight: Int
    get() = measuredHeight.takeIf { it > 0 } ?: layoutParams.height
}
