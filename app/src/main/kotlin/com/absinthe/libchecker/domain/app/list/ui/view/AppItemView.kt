package com.absinthe.libchecker.domain.app.list.ui.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isGone
import androidx.core.view.marginStart
import coil.dispose
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.list.model.AppListItemDisplay
import com.absinthe.libchecker.domain.app.list.model.AppListItemIconDisplay
import com.absinthe.libchecker.domain.app.list.model.AppListItemIdentityText
import com.absinthe.libchecker.domain.app.list.model.AppListItemMetadataDisplay
import com.absinthe.libchecker.domain.app.list.model.buildAppListItemDescription
import com.absinthe.libchecker.utils.extensions.addStrikeThroughSpan
import com.absinthe.libchecker.utils.extensions.applyCondensedSingleLine
import com.absinthe.libchecker.utils.extensions.applySingleLineEndEllipsize
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.isRtl
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.span.CenterAlignImageSpan
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
    setItemContentDescription(buildAppListItemDescription(*parts))
  }

  fun setItemContentDescription(description: CharSequence?) {
    contentDescription = description
  }

  fun setItemDisplay(display: AppListItemDisplay, highlightText: String) {
    container.setIconDisplay(display.icon)
    setItemIdentityDisplay(display, highlightText)
    container.setMetadataDisplay(display.metadata)
    container.setChips(display.chips)
  }

  fun setItemIdentityDisplay(display: AppListItemDisplay, highlightText: String) {
    container.setIdentityText(display.identity, highlightText)
    setItemContentDescription(
      display.identity.contentDescription,
      display.chips.joinToString().takeIf(String::isNotEmpty)
    )
  }

  class AppItemContainerView(
    context: Context,
    private val style: Style = Style.create(context)
  ) : AViewGroup(context) {

    private val iconBadgeGap = 4.dp
    private val abiBadgeGap = 1.dp
    private val abiBadgeWidthRatio = 0.75f
    private val labelTopGap = 4.dp

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

    private val labelGroup = PillLabelGroup(context, style).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      visibility = GONE
      this@AppItemContainerView.addView(this)
    }

    private var abiBadge: AppCompatImageView? = null
    private var multiArchBadge: AppCompatImageView? = null
    private var badge: AppCompatImageView? = null
    private var useDetachedAbiBadgeLayout = false

    fun setIconDisplay(display: AppListItemIconDisplay) {
      if (!display.usePackageIcon) {
        icon.dispose()
        icon.setImageDrawable(style.newIconPlaceholder(context))
        return
      }
      icon.load(display.packageInfo) {
        placeholder(style.newIconPlaceholder(context))
        error(style.newIconPlaceholder(context))
      }
    }

    fun setIdentityText(identityText: AppListItemIdentityText, highlightText: String) {
      appName.setOrHighlightText(identityText.label, highlightText)
      appName.setItemBackground()
      packageName.setOrHighlightText(identityText.packageName, highlightText)
      packageName.setItemBackground()

      if (identityText.showMissingPackageStrikeThrough) {
        appName.addStrikeThroughSpan()
        packageName.addStrikeThroughSpan()
      }
    }

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

    fun setMetadataDisplay(display: AppListItemMetadataDisplay) {
      setVersionInfo(display.versionInfo)
      setAbiDisplay(display)
      setPackageBadge(display.packageBadge)
    }

    fun setChips(labels: List<String>) {
      labelGroup.setLabels(labels)
      labelGroup.isGone = labels.isEmpty()
    }

    private fun setAbiDisplay(display: AppListItemMetadataDisplay) {
      setDetachedAbiBadgeLayoutEnabled(display.useDetachedAbiBadges)

      if (display.useDetachedAbiBadges) {
        if (display.largeAbiBadgeRes != 0) {
          val abiBadge = display.largeAbiBadgeRes.getDrawable(context)?.mutate()?.apply {
            setTint(context.getAbiBadgeTint(display.isAbiBadge64Bit, display.tintAbiLabels))
          }
          val multiArchBadge = if (display.showMultiArchBadge) {
            R.drawable.ic_abi_label_multi_arch.getDrawable(context)?.mutate()?.apply {
              setTint(context.getMultiArchBadgeTint(display.tintAbiLabels))
            }
          } else {
            null
          }
          setAbiBadges(abiBadge, multiArchBadge)
        } else {
          setAbiBadges(null, null)
        }
        abiInfo.text = display.abiInfo
      } else {
        setAbiBadges(null, null)
        abiInfo.text = context.buildInlineAbiInfo(display)
      }
    }

    private fun setPackageBadge(packageBadge: AppListItemMetadataDisplay.PackageBadge?) {
      when (packageBadge) {
        AppListItemMetadataDisplay.PackageBadge.Harmony -> setBadge(R.drawable.ic_harmony_badge)
        AppListItemMetadataDisplay.PackageBadge.Frozen -> setBadge(R.drawable.ic_disabled_package)
        null -> setBadge(null)
      }
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
        getChildAt(index).takeUnless { it === labelGroup }?.autoMeasure()
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
      if (!labelGroup.isGone) {
        labelGroup.measure(
          textWidth.toExactlyMeasureSpec(),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
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
          abiInfo.measuredHeightWithVisibility +
          if (labelGroup.isGone) 0 else labelTopGap + labelGroup.measuredHeight
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
      if (!labelGroup.isGone) {
        labelGroup.layout(offsetStart, abiInfo.bottom + labelTopGap)
      }
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

  private class PillLabelGroup(
    context: Context,
    private val style: Style
  ) : ViewGroup(context) {
    private val horizontalGap = 4.dp
    private val verticalGap = 4.dp
    private val labelHeight = 28.dp
    private val horizontalPadding = 10.dp
    private val strokeWidth = 1.dp
    private val strokeColor = context.getColorByAttr(
      com.google.android.material.R.attr.colorOutline
    )

    fun setLabels(labels: List<String>) {
      removeAllViews()
      labels.forEach { label ->
        addView(
          AppCompatTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, labelHeight)
            text = label
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            setTextAppearance(style.labelSmallTextAppearance)
            setTextColor(style.onSurfaceVariantColor)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            background = GradientDrawable().apply {
              shape = GradientDrawable.RECTANGLE
              setColor(Color.TRANSPARENT)
              setStroke(strokeWidth, strokeColor)
              cornerRadius = labelHeight / 2f
            }
          }
        )
      }
      requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
      var rowWidth = 0
      var rowHeight = 0
      var contentHeight = 0
      for (index in 0 until childCount) {
        val child = getChildAt(index)
        measureChild(child, widthMeasureSpec, heightMeasureSpec)
        val nextWidth = if (rowWidth == 0) {
          child.measuredWidth
        } else {
          rowWidth + horizontalGap + child.measuredWidth
        }
        if (rowWidth > 0 && nextWidth > availableWidth) {
          contentHeight += rowHeight + verticalGap
          rowWidth = child.measuredWidth
          rowHeight = child.measuredHeight
        } else {
          rowWidth = nextWidth
          rowHeight = maxOf(rowHeight, child.measuredHeight)
        }
      }
      if (childCount > 0) {
        contentHeight += rowHeight
      }
      setMeasuredDimension(
        resolveSize(availableWidth, widthMeasureSpec),
        resolveSize(contentHeight, heightMeasureSpec)
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      val availableWidth = r - l
      var childStart = 0
      var childTop = 0
      var rowHeight = 0
      for (index in 0 until childCount) {
        val child = getChildAt(index)
        val nextEnd = childStart + child.measuredWidth
        if (childStart > 0 && nextEnd > availableWidth) {
          childStart = 0
          childTop += rowHeight + verticalGap
          rowHeight = 0
        }
        val childLeft = if (isRtl()) {
          availableWidth - childStart - child.measuredWidth
        } else {
          childStart
        }
        child.layout(
          childLeft,
          childTop,
          childLeft + child.measuredWidth,
          childTop + child.measuredHeight
        )
        childStart += child.measuredWidth + horizontalGap
        rowHeight = maxOf(rowHeight, child.measuredHeight)
      }
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
        ?: R.mipmap.ic_app_icon_placeholder.getDrawable(context)
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
          iconPlaceholderState = R.mipmap.ic_app_icon_placeholder.getDrawable(context)?.constantState
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

private fun TextView.setOrHighlightText(text: CharSequence, highlightText: String) {
  if (highlightText.isNotBlank()) {
    tintHighlightText(highlightText, text)
  } else {
    this.text = text
  }
}

private fun Context.getAbiBadgeTint(isAbi64Bit: Boolean, tintAbiLabels: Boolean): Int {
  if (!tintAbiLabels) {
    return getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
  }
  return getColorByAttr(
    if (isAbi64Bit) {
      androidx.appcompat.R.attr.colorPrimary
    } else {
      com.google.android.material.R.attr.colorTertiary
    }
  )
}

private fun Context.buildInlineAbiInfo(display: AppListItemMetadataDisplay): CharSequence {
  if (display.abiBadgeRes == 0) {
    return display.abiInfo
  }

  var paddingString = "  ${display.abiInfo}"
  if (display.showMultiArchBadge) {
    paddingString = "  $paddingString"
  }
  val spanString = SpannableString(paddingString)

  display.abiBadgeRes.getDrawable(this)?.mutate()?.let {
    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
    it.setTint(getAbiBadgeTint(display.isAbiBadge64Bit, display.tintAbiLabels))
    spanString.setSpan(CenterAlignImageSpan(it), 0, 1, ImageSpan.ALIGN_BOTTOM)
  }
  if (display.showMultiArchBadge) {
    R.drawable.ic_multi_arch.getDrawable(this)?.mutate()?.let {
      it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
      it.setTint(getMultiArchBadgeTint(display.tintAbiLabels))
      spanString.setSpan(CenterAlignImageSpan(it), 2, 3, ImageSpan.ALIGN_BOTTOM)
    }
  }

  return spanString
}

private fun Context.getMultiArchBadgeTint(tintAbiLabels: Boolean): Int {
  return getColorByAttr(
    if (tintAbiLabels) {
      com.google.android.material.R.attr.colorSecondary
    } else {
      com.google.android.material.R.attr.colorOnSurfaceVariant
    }
  )
}
