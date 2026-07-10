package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringMetadataItemDisplay
import com.absinthe.libchecker.domain.app.detail.resource.AppResourcePreview
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawableByAttr
import com.absinthe.libchecker.utils.extensions.visibleWidth
import com.absinthe.libchecker.view.AViewGroup

class MetadataLibItemView(context: Context) : AViewGroup(context) {

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    val verticalPadding = 4.dp
    setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
  }

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
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      addView(this)
    }

  private val linkToIcon = AppCompatImageButton(context).apply {
    layoutParams = LayoutParams(24.dp, 24.dp).also {
      it.marginStart = 8.dp
    }
    scaleType = ImageView.ScaleType.CENTER
    setImageResource(R.drawable.ic_outline_change_circle_24)
    contentDescription = context.getString(R.string.lib_detail_app_props_tip)
    setBackgroundDrawable(context.getDrawableByAttr(android.R.attr.selectableItemBackgroundBorderless))
    isVisible = false
    addView(this)
  }

  fun bind(
    display: LibStringMetadataItemDisplay,
    highlightText: String,
    onResourceClick: ((LibStringMetadataItemDisplay) -> Unit)?
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    libSize.setOrHighlightText(display.visibleValue, highlightText)
    contentDescription = display.contentDescription

    linkToIcon.apply {
      val canOpenResource = display.resource != null && onResourceClick != null
      isVisible = canOpenResource
      setOnClickListener(
        if (canOpenResource) {
          View.OnClickListener { onResourceClick(display) }
        } else {
          null
        }
      )
      when (val preview = display.preview) {
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
  }

  private val AppCompatImageButton.previewWidth: Int
    get() = measuredWidth.takeIf { it > 0 } ?: layoutParams.width

  private val AppCompatImageButton.previewHeight: Int
    get() = measuredHeight.takeIf { it > 0 } ?: layoutParams.height

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach {
      it.autoMeasure()
    }
    val libNameWidth =
      measuredWidth - paddingStart - paddingEnd - libName.marginEnd - linkToIcon.visibleWidth() - linkToIcon.marginStart
    if (libName.measuredWidth > libNameWidth) {
      libName.measure(libNameWidth.toExactlyMeasureSpec(), libName.defaultHeightMeasureSpec(this))
    }
    if (libSize.measuredWidth > libNameWidth) {
      libSize.measure(libNameWidth.toExactlyMeasureSpec(), libSize.defaultHeightMeasureSpec(this))
    }
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
    linkToIcon.layout(paddingEnd, linkToIcon.toVerticalCenter(this), true)
  }
}
