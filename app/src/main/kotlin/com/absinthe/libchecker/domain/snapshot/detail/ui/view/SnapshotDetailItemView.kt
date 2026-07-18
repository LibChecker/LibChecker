package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemLayoutPlan
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemViewRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipIconStyle
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailRuleChipRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailThemeColors
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.planSnapshotDetailItemLayout
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.resolveSnapshotDetailItemColors
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.R as MaterialR

class SnapshotDetailItemView(context: Context) : AViewGroup(context) {

  private val contentHorizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)

  private val statusRail = View(context).apply {
    layoutParams = ViewGroup.LayoutParams(STATUS_RAIL_WIDTH, ViewGroup.LayoutParams.MATCH_PARENT)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val statusIcon = AppCompatImageView(context).apply {
    layoutParams = LayoutParams(STATUS_ICON_SIZE, STATUS_ICON_SIZE)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val statusLabel = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val title = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val previousPackagePath = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensedMedium)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    isVisible = false
    addView(this)
  }

  private val movedArrow = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifMedium)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    text = MOVED_ARROW
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    isVisible = false
    addView(this)
  }

  private val extra = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val divider = View(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DIVIDER_HEIGHT)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private var chip: AppCompatTextView? = null
  private var chipClickListener: OnClickListener? = null
  private var statusIconOpticalInset = 0
  private var layoutPlan = SnapshotDetailItemLayoutPlan(
    titleWidth = 0,
    chipOnTitleLine = false
  )

  init {
    minimumHeight = MINIMUM_HEIGHT
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    isFocusable = true
    foreground = context.getDrawable(
      context.getResourceIdByAttr(android.R.attr.selectableItemBackground)
    )
  }

  fun render(state: SnapshotDetailItemViewRenderState) {
    contentDescription = state.contentDescription
    title.text = state.title.withSnapshotTechnicalPathBreakOpportunities()
    previousPackagePath.apply {
      text = state.movedPath?.previousPackagePath
        ?.withSnapshotTechnicalPathBreakOpportunities()
        ?: ""
      isVisible = state.movedPath != null
    }
    movedArrow.isVisible = state.movedPath != null
    extra.apply {
      text = state.extra
      isVisible = state.extra.isNotBlank()
    }
    statusIconOpticalInset = resolveStatusIconOpticalInset(state.iconRes)
    statusIcon.setImageResource(state.iconRes)
    statusLabel.setText(state.statusLabelRes)

    val colors = resolveSnapshotDetailItemColors(
      theme = SnapshotDetailThemeColors(
        surface = context.getColorByAttr(MaterialR.attr.colorSurface),
        onSurface = context.getColorByAttr(MaterialR.attr.colorOnSurface),
        onSurfaceVariant = context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant),
        outlineVariant = context.getColorByAttr(MaterialR.attr.colorOutlineVariant),
        chipSurface = context.getColorByAttr(MaterialR.attr.colorSurfaceContainerLow)
      ),
      statusColor = state.statusColorRes.getColor(context)
    )
    background = GradientDrawable(
      GradientDrawable.Orientation.LEFT_RIGHT,
      intArrayOf(
        colors.gradientStart,
        colors.gradientMiddle,
        colors.surface,
        colors.surface,
        colors.surface
      )
    )
    statusRail.setBackgroundColor(colors.status)
    ImageViewCompat.setImageTintList(statusIcon, ColorStateList.valueOf(colors.status))
    statusLabel.setTextColor(colors.status)
    title.setTextColor(colors.title)
    previousPackagePath.setTextColor(colors.supportingText)
    movedArrow.setTextColor(colors.status)
    extra.setTextColor(colors.supportingText)
    divider.setBackgroundColor(colors.divider)
    setChip(state.ruleChip, colors)
  }

  fun setChipOnClickListener(listener: OnClickListener?) {
    chipClickListener = listener
    chip?.setOnClickListener(listener)
  }

  private fun setChip(
    state: SnapshotDetailRuleChipRenderState?,
    colors: com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailItemResolvedColors
  ) {
    if (state == null) {
      chip?.let(::removeView)
      chip = null
      touchDelegate = null
      return
    }

    val target = chip ?: AppCompatTextView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        RULE_CHIP_HEIGHT
      )
      gravity = Gravity.CENTER_VERTICAL
      setPaddingRelative(RULE_CHIP_HORIZONTAL_PADDING, 0, RULE_CHIP_HORIZONTAL_PADDING, 0)
      compoundDrawablePadding = 4.dp
      setTextAppearance(
        context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelSmall)
      )
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
      isFocusable = true
      addView(this)
      chip = this
    }
    target.apply {
      text = state.label
      background = buildRuleChipInteractionBackground(colors.chipSurface)
      setTextColor(colors.chipText)
      val icon = requireNotNull(state.iconRes.getDrawable(context)).mutate().apply {
        setBounds(0, 0, RULE_CHIP_ICON_SIZE, RULE_CHIP_ICON_SIZE)
        clearColorFilter()
        DrawableCompat.setTintList(this, null)
      }
      when (state.iconStyle) {
        SnapshotDetailRuleChipIconStyle.Desaturated -> {
          icon.colorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setSaturation(0f) }
          )
        }

        SnapshotDetailRuleChipIconStyle.ThemeTint -> {
          DrawableCompat.setTint(icon, colors.chipText)
        }

        SnapshotDetailRuleChipIconStyle.Original -> Unit
      }
      setCompoundDrawablesRelative(icon, null, null, null)
      setOnClickListener(chipClickListener)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.filter { it !== divider }.forEach { it.autoMeasure() }
    divider.measure(measuredWidth.toExactlyMeasureSpec(), DIVIDER_HEIGHT.toExactlyMeasureSpec())

    val contentWidth = (
      measuredWidth - STATUS_RAIL_WIDTH - contentHorizontalPadding * 2
      ).coerceAtLeast(0)
    val maximumStatusLabelWidth = (
      contentWidth - statusIcon.measuredWidth - STATUS_LABEL_GAP
      ).coerceAtLeast(0)
    if (statusLabel.measuredWidth > maximumStatusLabelWidth) {
      statusLabel.measure(
        maximumStatusLabelWidth.toExactlyMeasureSpec(),
        statusLabel.defaultHeightMeasureSpec(this)
      )
    }
    layoutPlan = planSnapshotDetailItemLayout(
      contentWidth = contentWidth,
      naturalTitleWidth = title.measuredWidth,
      chipWidth = chip?.measuredWidth ?: 0,
      chipGap = TITLE_CHIP_GAP
    )
    if (title.measuredWidth > layoutPlan.titleWidth) {
      title.measure(
        layoutPlan.titleWidth.toExactlyMeasureSpec(),
        title.defaultHeightMeasureSpec(this)
      )
    }
    if (previousPackagePath.isVisible && previousPackagePath.measuredWidth > layoutPlan.titleWidth) {
      previousPackagePath.measure(
        layoutPlan.titleWidth.toExactlyMeasureSpec(),
        previousPackagePath.defaultHeightMeasureSpec(this)
      )
    }
    if (extra.isVisible && extra.measuredWidth > layoutPlan.titleWidth) {
      extra.measure(
        layoutPlan.titleWidth.toExactlyMeasureSpec(),
        extra.defaultHeightMeasureSpec(this)
      )
    }

    val statusHeight = maxOf(statusIcon.measuredHeight, statusLabel.measuredHeight)
    val statusBottom = CONTENT_TOP_PADDING + statusHeight
    var titleTop = statusBottom + STATUS_CONTENT_GAP
    if (previousPackagePath.isVisible) {
      titleTop += previousPackagePath.measuredHeight + movedArrow.measuredHeight
    }
    val titleLineHeight = maxOf(
      title.measuredHeight,
      chip?.takeIf { layoutPlan.chipOnTitleLine }?.measuredHeight ?: 0
    )
    var contentBottom = titleTop + titleLineHeight
    if (extra.isVisible) {
      contentBottom += extra.measuredHeight
    }
    chip?.takeUnless { layoutPlan.chipOnTitleLine }?.let {
      contentBottom += RULE_CHIP_VERTICAL_GAP + it.measuredHeight
    }
    val desiredHeight = maxOf(contentBottom, statusBottom) + CONTENT_BOTTOM_PADDING
    setMeasuredDimension(measuredWidth, desiredHeight.coerceAtLeast(MINIMUM_HEIGHT))
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    statusRail.layout(0, 0, STATUS_RAIL_WIDTH, measuredHeight)
    val contentStart = STATUS_RAIL_WIDTH + contentHorizontalPadding
    val statusStart = contentStart - statusIconOpticalInset
    val statusHeight = maxOf(statusIcon.measuredHeight, statusLabel.measuredHeight)
    statusIcon.layout(
      statusStart,
      CONTENT_TOP_PADDING + (statusHeight - statusIcon.measuredHeight) / 2
    )
    statusLabel.layout(
      statusStart + statusIcon.measuredWidth + STATUS_LABEL_GAP,
      CONTENT_TOP_PADDING + (statusHeight - statusLabel.measuredHeight) / 2
    )

    val titleX = contentStart
    var titleY = CONTENT_TOP_PADDING + statusHeight + STATUS_CONTENT_GAP
    if (previousPackagePath.isVisible) {
      previousPackagePath.layout(titleX, titleY)
      movedArrow.layout(
        statusStart + (statusIcon.measuredWidth - movedArrow.measuredWidth) / 2,
        previousPackagePath.bottom
      )
      titleY = movedArrow.bottom
    }
    val inlineChip = chip?.takeIf { layoutPlan.chipOnTitleLine }
    val titleLineHeight = maxOf(
      title.measuredHeight,
      inlineChip?.measuredHeight ?: 0
    )
    title.layout(
      titleX,
      titleY + (titleLineHeight - title.measuredHeight) / 2
    )
    inlineChip?.layout(
      title.right + TITLE_CHIP_GAP,
      titleY + (titleLineHeight - inlineChip.measuredHeight) / 2
    )
    var nextY = titleY + titleLineHeight
    if (extra.isVisible) {
      extra.layout(titleX, nextY)
      nextY = extra.bottom
    }
    chip?.takeUnless { layoutPlan.chipOnTitleLine }?.layout(
      titleX,
      nextY + RULE_CHIP_VERTICAL_GAP
    )
    updateChipTouchDelegate()
    divider.layout(
      contentStart,
      measuredHeight - DIVIDER_HEIGHT,
      measuredWidth,
      measuredHeight
    )
  }

  private fun updateChipTouchDelegate() {
    val target = chip ?: run {
      touchDelegate = null
      return
    }
    val bounds = Rect().also(target::getHitRect)
    val verticalExpansion = ((RULE_CHIP_TOUCH_HEIGHT - target.measuredHeight) / 2).coerceAtLeast(0)
    bounds.inset(-RULE_CHIP_HORIZONTAL_TOUCH_EXPANSION, -verticalExpansion)
    touchDelegate = TouchDelegate(bounds, target)
  }

  private fun resolveStatusIconOpticalInset(iconRes: Int): Int {
    return when (iconRes) {
      R.drawable.ic_add,
      R.drawable.ic_remove -> 3.dp

      R.drawable.ic_changed -> 4.dp

      R.drawable.ic_move -> 2.dp

      else -> 0
    }
  }

  private fun buildRuleChipInteractionBackground(backgroundColor: Int): RippleDrawable {
    return RippleDrawable(
      ColorStateList.valueOf(context.getColorByAttr(android.R.attr.colorControlHighlight)),
      ruleChipPillDrawable(backgroundColor),
      ruleChipPillDrawable(Color.WHITE)
    )
  }

  private fun ruleChipPillDrawable(color: Int): GradientDrawable {
    return GradientDrawable().apply {
      shape = GradientDrawable.RECTANGLE
      cornerRadius = RULE_CHIP_RADIUS.toFloat()
      setColor(color)
    }
  }

  private companion object {
    val STATUS_RAIL_WIDTH = 3.dp
    val STATUS_ICON_SIZE = 16.dp
    val STATUS_LABEL_GAP = 4.dp
    const val MOVED_ARROW = "↓"
    val TITLE_CHIP_GAP = 6.dp
    val CONTENT_TOP_PADDING = 4.dp
    val CONTENT_BOTTOM_PADDING = 8.dp
    val STATUS_CONTENT_GAP = 0.dp
    val RULE_CHIP_VERTICAL_GAP = 2.dp
    val RULE_CHIP_HEIGHT = 24.dp
    val RULE_CHIP_HORIZONTAL_PADDING = 8.dp
    val RULE_CHIP_ICON_SIZE = 16.dp
    val RULE_CHIP_TOUCH_HEIGHT = 48.dp
    val RULE_CHIP_HORIZONTAL_TOUCH_EXPANSION = 4.dp
    val RULE_CHIP_RADIUS = 6.dp
    val MINIMUM_HEIGHT = 56.dp
    val DIVIDER_HEIGHT = 1.dp
  }
}
