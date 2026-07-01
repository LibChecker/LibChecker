package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import com.absinthe.libchecker.R
import com.absinthe.libchecker.utils.extensions.applyCondensedSingleLine
import com.absinthe.libchecker.utils.extensions.applySingleLineEndEllipsize
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.card.MaterialCardView

class AppItemView(
  context: Context,
  private val style: Style = Style.create(context)
) : MaterialCardView(context) {

  val container = AppItemContainerView(context, style).apply {
    setPadding(style.cardPadding, style.cardPadding, style.cardPadding, style.cardPadding)
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
      setTextAppearance(style.labelLargeTextAppearance)
      setTextColor(style.onSurfaceVariantColor)
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
    setItemContentDescription(text)
  }

  fun setItemContentDescription(vararg parts: CharSequence?) {
    contentDescription = parts
      .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
      .joinToString()
  }

  class AppItemContainerView(
    context: Context,
    private val style: Style = Style.create(context)
  ) : AViewGroup(context) {

    private val iconBadgeGap = 4.dp
    private val abiBadgeGap = 1.dp
    private val abiBadgeWidthRatio = 0.75f

    val icon = AppCompatImageView(context).apply {
      layoutParams = LayoutParams(style.iconSize, style.iconSize)
      setImageDrawable(style.newIconPlaceholder(context))
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      addView(this)
    }

    val appName = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.marginStart = 8.dp
      }
      setTextAppearance(style.titleMediumTextAppearance)
      setTextColor(style.onSurfaceColor)
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.END
      addView(this)
    }

    val packageName = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(style.bodyMediumTextAppearance)
      setTextColor(style.onSurfaceVariantColor)
      applySingleLineEndEllipsize()
      addView(this)
    }

    val versionInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(style.bodySmallTextAppearance)
      setTextColor(style.onSurfaceVariantColor)
      applyCondensedSingleLine()
      addView(this)
    }

    val abiInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(0, 0, 0, 2.dp)
      setTextAppearance(style.labelSmallTextAppearance)
      setTextColor(style.onSurfaceVariantColor)
      applyCondensedSingleLine()
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
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
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
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      for (index in 0 until childCount) {
        getChildAt(index).autoMeasure()
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
      val abiBadge = abiBadge
      val multiArchBadge = multiArchBadge
      if (abiBadge == null && multiArchBadge == null) {
        return
      }
      val badgeWidth = if (abiBadge == null || multiArchBadge == null) {
        (icon.measuredWidth * abiBadgeWidthRatio).toInt().coerceAtLeast(1)
      } else {
        ((icon.measuredWidth - abiBadgeGap) / 2).coerceAtLeast(1)
      }
      abiBadge?.measureToBadgeSize(badgeWidth)
      multiArchBadge?.measureToBadgeSize(badgeWidth)
    }

    private fun AppCompatImageView.measureToBadgeSize(width: Int) {
      val badgeHeight = width.toIntrinsicRatioHeight(drawable)
      measure(
        width.toExactlyMeasureSpec(),
        badgeHeight.toExactlyMeasureSpec()
      )
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

  class Style private constructor(
    val cardPadding: Int,
    val iconSize: Int,
    val titleMediumTextAppearance: Int,
    val bodyMediumTextAppearance: Int,
    val bodySmallTextAppearance: Int,
    val labelSmallTextAppearance: Int,
    val labelLargeTextAppearance: Int,
    val onSurfaceColor: Int,
    val onSurfaceVariantColor: Int,
    private val iconPlaceholderState: Drawable.ConstantState?
  ) {

    fun newIconPlaceholder(context: Context): Drawable? {
      return iconPlaceholderState?.newDrawable(context.resources)
        ?: R.drawable.ic_icon_blueprint.getDrawable(context)
    }

    companion object {
      fun create(context: Context): Style {
        return Style(
          cardPadding = context.getDimensionPixelSize(R.dimen.main_card_padding),
          iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size),
          titleMediumTextAppearance = context.getResourceIdByAttr(
            com.google.android.material.R.attr.textAppearanceTitleMedium
          ),
          bodyMediumTextAppearance = context.getResourceIdByAttr(
            com.google.android.material.R.attr.textAppearanceBodyMedium
          ),
          bodySmallTextAppearance = context.getResourceIdByAttr(
            com.google.android.material.R.attr.textAppearanceBodySmall
          ),
          labelSmallTextAppearance = context.getResourceIdByAttr(
            com.google.android.material.R.attr.textAppearanceLabelSmall
          ),
          labelLargeTextAppearance = context.getResourceIdByAttr(
            com.google.android.material.R.attr.textAppearanceLabelLarge
          ),
          onSurfaceColor = context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface),
          onSurfaceVariantColor = context.getColorByAttr(
            com.google.android.material.R.attr.colorOnSurfaceVariant
          ),
          iconPlaceholderState = R.drawable.ic_icon_blueprint.getDrawable(context)?.constantState
        )
      }
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
