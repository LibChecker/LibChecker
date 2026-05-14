package com.absinthe.libchecker.features.applist.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class AppItemView(context: Context) : MaterialCardView(context) {

  val container = AppItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  private val floatView by lazy {
    AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER
        it.topMargin = 24.dp
        it.bottomMargin = 24.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelLarge))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
    }
  }

  init {
    strokeColor = Color.TRANSPARENT
    addView(container)
  }

  fun addFloatView(text: String) {
    if (container.parent != null) {
      removeView(container)
    }
    if (floatView.parent == null) {
      addView(floatView)
    }
    floatView.text = text
  }

  class AppItemContainerView(context: Context) : AViewGroup(context) {

    private val iconBadgeGap = 4.dp
    private val abiBadgeGap = 1.dp
    private val abiBadgeWidthRatio = 0.75f

    val icon = AppCompatImageView(context).apply {
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = LayoutParams(iconSize, iconSize)
      setImageResource(R.drawable.ic_icon_blueprint)
      addView(this)
    }

    val appName = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val packageName = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodyMedium))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val versionInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val abiInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(0, 0, 0, 2.dp)
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    private var abiBadge: AppCompatImageView? = null
    private var multiArchBadge: AppCompatImageView? = null
    private var badge: AppCompatImageView? = null
    private var useDetachedAbiBadgeLayout = false

    fun setAppName(text: String) {
      appName.text = text
      appName.setItemBackground()
    }

    fun setPackageName(text: String) {
      packageName.text = text
      packageName.setItemBackground()
    }

    fun setVersionInfo(text: String) {
      versionInfo.text = text
      versionInfo.setItemBackground()
    }

    fun setAbiInfo(text: String) {
      abiInfo.text = text
      abiInfo.setItemBackground()
    }

    fun setDetachedAbiBadgeLayoutEnabled(enabled: Boolean) {
      useDetachedAbiBadgeLayout = enabled
    }

    fun setAbiBadges(abiDrawable: Drawable?, multiArchDrawable: Drawable?) {
      abiBadge = setBadgeDrawable(abiBadge, abiDrawable)
      multiArchBadge = setBadgeDrawable(multiArchBadge, multiArchDrawable)
    }

    fun setBadge(res: Int) {
      setBadge(res.getDrawable(context))
    }

    fun setBadge(drawable: Drawable?) {
      if (drawable != null) {
        if (badge == null) {
          badge = AppCompatImageView(context).apply {
            layoutParams = LayoutParams(24.dp, 24.dp)
            addView(this)
          }
        }
        badge!!.setImageDrawable(drawable)
      } else {
        if (badge != null) {
          removeView(badge)
          badge = null
        }
      }
    }

    private fun setBadgeDrawable(
      view: AppCompatImageView?,
      drawable: Drawable?
    ): AppCompatImageView? {
      if (drawable == null) {
        if (view != null) {
          removeView(view)
        }
        return null
      }
      return (view ?: AppCompatImageView(context).also { addView(it) }).apply {
        layoutParams = LayoutParams(drawable.intrinsicWidth, drawable.intrinsicHeight)
        setImageDrawable(drawable)
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      measureAbiBadges()
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - appName.marginStart

      if (appName.measuredWidth > textWidth) {
        appName.measure(
          textWidth.toExactlyMeasureSpec(),
          appName.defaultHeightMeasureSpec(this)
        )
      }
      if (packageName.measuredWidth > textWidth) {
        packageName.measure(
          textWidth.toExactlyMeasureSpec(),
          packageName.defaultHeightMeasureSpec(this)
        )
      }
      if (versionInfo.measuredWidth > textWidth) {
        versionInfo.measure(
          textWidth.toExactlyMeasureSpec(),
          versionInfo.defaultHeightMeasureSpec(this)
        )
      }
      if (abiInfo.measuredWidth > textWidth) {
        abiInfo.measure(
          textWidth.toExactlyMeasureSpec(),
          abiInfo.defaultHeightMeasureSpec(this)
        )
      }
      val iconColumnHeight = icon.measuredHeight +
        if (abiBadge != null || multiArchBadge != null) {
          iconBadgeGap + maxOf(
            abiBadge?.measuredHeight ?: 0,
            multiArchBadge?.measuredHeight ?: 0
          )
        } else {
          0
        }
      val textColumnHeight =
        appName.measuredHeightWithVisibility +
          packageName.measuredHeightWithVisibility +
          versionInfo.measuredHeightWithVisibility +
          abiInfo.measuredHeightWithVisibility
      setMeasuredDimension(
        measuredWidth,
        paddingTop + if (useDetachedAbiBadgeLayout) {
          maxOf(textColumnHeight, iconColumnHeight)
        } else {
          textColumnHeight
        } + paddingBottom
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(
        paddingStart,
        if (useDetachedAbiBadgeLayout) paddingTop else icon.toVerticalCenter(this)
      )
      val offsetStart = paddingStart + icon.measuredWidth + appName.marginStart
      appName.layout(offsetStart, paddingTop)
      packageName.layout(offsetStart, appName.bottom)
      versionInfo.layout(offsetStart, packageName.bottom)
      abiInfo.layout(offsetStart, versionInfo.bottom)
      if (hasDetachedAbiBadges()) {
        layoutAbiBadges()
      }
      badge?.layout(paddingTop, paddingEnd, fromRight = true)
    }

    private fun hasDetachedAbiBadges(): Boolean {
      return abiBadge != null || multiArchBadge != null
    }

    private fun layoutAbiBadges() {
      val abiBadge = abiBadge
      val multiArchBadge = multiArchBadge
      if (abiBadge == null && multiArchBadge == null) {
        return
      }
      val rowWidth = (abiBadge?.measuredWidth ?: 0) +
        (multiArchBadge?.measuredWidth ?: 0) +
        if (abiBadge != null && multiArchBadge != null) abiBadgeGap else 0
      val rowHeight = maxOf(
        abiBadge?.measuredHeight ?: 0,
        multiArchBadge?.measuredHeight ?: 0
      )
      var childStart = paddingStart + (icon.measuredWidth - rowWidth) / 2
      val childTop = icon.bottom + iconBadgeGap

      abiBadge?.let {
        it.layout(childStart, childTop + (rowHeight - it.measuredHeight) / 2)
        childStart += it.measuredWidth + if (multiArchBadge != null) abiBadgeGap else 0
      }
      multiArchBadge?.layout(
        childStart,
        childTop + (rowHeight - multiArchBadge.measuredHeight) / 2
      )
    }

    private fun measureAbiBadges() {
      val badges = listOfNotNull(abiBadge, multiArchBadge)
      if (badges.isEmpty()) {
        return
      }
      val badgeWidth = if (badges.size == 1) {
        (icon.measuredWidth * abiBadgeWidthRatio).toInt().coerceAtLeast(1)
      } else {
        ((icon.measuredWidth - abiBadgeGap) / 2).coerceAtLeast(1)
      }
      badges.forEach {
        val drawable = it.drawable
        val badgeHeight = badgeWidth.toIntrinsicRatioHeight(drawable)
        it.measure(
          badgeWidth.toExactlyMeasureSpec(),
          badgeHeight.toExactlyMeasureSpec()
        )
      }
    }

    private fun Int.toIntrinsicRatioHeight(drawable: Drawable?): Int {
      val intrinsicWidth = drawable?.intrinsicWidth ?: 0
      val intrinsicHeight = drawable?.intrinsicHeight ?: 0
      if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
        return this
      }
      return (this * intrinsicHeight.toFloat() / intrinsicWidth).toInt().coerceAtLeast(1)
    }
  }
}

private fun TextView.setItemBackground() {
  if (text.trim().isEmpty()) {
    setBackgroundResource(R.drawable.bg_app_item_text_inset)
    alpha = 0.65f
  } else {
    background = null
    alpha = 1f
  }
}
