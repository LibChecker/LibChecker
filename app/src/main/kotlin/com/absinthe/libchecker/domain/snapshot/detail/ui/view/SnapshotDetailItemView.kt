package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
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
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.AViewGroup
import com.google.android.material.R as MaterialR
import com.google.android.material.chip.Chip

class SnapshotDetailItemView(context: Context) : AViewGroup(context) {

  private val horizontalContentPadding = context.getDimensionPixelSize(R.dimen.normal_padding)

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
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private val title = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_MonoSpace)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
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

  private var chip: Chip? = null
  private var chipClickListener: OnClickListener? = null
  private var layoutPlan = SnapshotDetailItemLayoutPlan(
    titleStartsOnNewLine = false,
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
    title.text = state.title
    extra.apply {
      text = state.extra
      isVisible = state.extra.isNotBlank()
    }
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
    setBackgroundColor(colors.surface)
    statusRail.setBackgroundColor(colors.status)
    ImageViewCompat.setImageTintList(statusIcon, ColorStateList.valueOf(colors.status))
    statusLabel.setTextColor(colors.status)
    title.setTextColor(colors.title)
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

    val target = chip ?: Chip(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setEnsureMinTouchTargetSize(false)
      minimumHeight = RULE_CHIP_HEIGHT
      chipMinHeight = RULE_CHIP_HEIGHT.toFloat()
      shapeAppearanceModel = shapeAppearanceModel.toBuilder()
        .setAllCornerSizes(RULE_CHIP_RADIUS.toFloat())
        .build()
      chipStrokeWidth = 1.dp.toFloat()
      chipStartPadding = 6.dp.toFloat()
      chipEndPadding = 6.dp.toFloat()
      iconStartPadding = 0f
      iconEndPadding = 2.dp.toFloat()
      textStartPadding = 2.dp.toFloat()
      textEndPadding = 0f
      setTextAppearance(
        context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelSmall)
      )
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
      addView(this)
      chip = this
    }
    target.apply {
      text = state.label
      setChipIconResource(state.iconRes)
      chipBackgroundColor = ColorStateList.valueOf(colors.chipSurface)
      chipStrokeColor = ColorStateList.valueOf(colors.chipOutline)
      setTextColor(colors.chipText)
      chipIconTint = null
      chipIcon?.clearColorFilter()
      when (state.iconStyle) {
        SnapshotDetailRuleChipIconStyle.Desaturated -> {
          chipIcon?.mutate()?.let { icon ->
            icon.colorFilter = ColorMatrixColorFilter(
              ColorMatrix().apply { setSaturation(0f) }
            )
            chipIcon = icon
          }
        }

        SnapshotDetailRuleChipIconStyle.ThemeTint -> {
          chipIconTint = ColorStateList.valueOf(colors.chipText)
        }

        SnapshotDetailRuleChipIconStyle.Original -> Unit
      }
      setOnClickListener(chipClickListener)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.filter { it !== divider }.forEach { it.autoMeasure() }
    val dividerWidth = (measuredWidth - horizontalContentPadding * 2).coerceAtLeast(0)
    divider.measure(dividerWidth.toExactlyMeasureSpec(), DIVIDER_HEIGHT.toExactlyMeasureSpec())

    val contentWidth = (
      measuredWidth - horizontalContentPadding * 2 - STATUS_RAIL_WIDTH - CONTENT_START_GAP
      ).coerceAtLeast(0)
    val statusClusterWidth = statusIcon.measuredWidth + STATUS_LABEL_GAP + statusLabel.measuredWidth
    layoutPlan = planSnapshotDetailItemLayout(
      contentWidth = contentWidth,
      statusClusterWidth = statusClusterWidth,
      naturalTitleWidth = title.measuredWidth,
      chipWidth = chip?.measuredWidth ?: 0,
      titleGap = STATUS_TITLE_GAP,
      chipGap = TITLE_CHIP_GAP,
      minimumTitleWidth = MINIMUM_INLINE_TITLE_WIDTH
    )
    if (title.measuredWidth > layoutPlan.titleWidth) {
      title.measure(
        layoutPlan.titleWidth.toExactlyMeasureSpec(),
        title.defaultHeightMeasureSpec(this)
      )
    }
    if (extra.isVisible && extra.measuredWidth > layoutPlan.titleWidth) {
      extra.measure(
        layoutPlan.titleWidth.toExactlyMeasureSpec(),
        extra.defaultHeightMeasureSpec(this)
      )
    }

    val statusHeight = maxOf(statusIcon.measuredHeight, statusLabel.measuredHeight)
    val titleTop = CONTENT_TOP_PADDING + if (layoutPlan.titleStartsOnNewLine) {
      statusHeight + STACKED_TITLE_GAP
    } else {
      0
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
    val statusBottom = CONTENT_TOP_PADDING + statusHeight
    val desiredHeight = maxOf(contentBottom, statusBottom) + CONTENT_BOTTOM_PADDING
    setMeasuredDimension(measuredWidth, desiredHeight.coerceAtLeast(MINIMUM_HEIGHT))
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    statusRail.layout(
      horizontalContentPadding,
      0,
      horizontalContentPadding + STATUS_RAIL_WIDTH,
      measuredHeight
    )
    val contentStart = horizontalContentPadding + STATUS_RAIL_WIDTH + CONTENT_START_GAP
    val statusClusterWidth = statusIcon.measuredWidth + STATUS_LABEL_GAP + statusLabel.measuredWidth
    val statusRowHeight = maxOf(
      statusIcon.measuredHeight,
      statusLabel.measuredHeight,
      if (layoutPlan.titleStartsOnNewLine) 0 else title.measuredHeight
    )
    statusIcon.layout(
      contentStart,
      CONTENT_TOP_PADDING + (statusRowHeight - statusIcon.measuredHeight) / 2
    )
    statusLabel.layout(
      contentStart + statusIcon.measuredWidth + STATUS_LABEL_GAP,
      CONTENT_TOP_PADDING + (statusRowHeight - statusLabel.measuredHeight) / 2
    )

    val titleX = if (layoutPlan.titleStartsOnNewLine) {
      contentStart
    } else {
      contentStart + statusClusterWidth + STATUS_TITLE_GAP
    }
    val titleY = CONTENT_TOP_PADDING + if (layoutPlan.titleStartsOnNewLine) {
      maxOf(statusIcon.measuredHeight, statusLabel.measuredHeight) + STACKED_TITLE_GAP
    } else {
      0
    }
    title.layout(titleX, titleY)
    val inlineChip = chip?.takeIf { layoutPlan.chipOnTitleLine }
    inlineChip?.layout(
      title.right + TITLE_CHIP_GAP,
      titleY + (maxOf(title.measuredHeight, inlineChip.measuredHeight) - inlineChip.measuredHeight) / 2
    )
    var nextY = titleY + maxOf(title.measuredHeight, inlineChip?.measuredHeight ?: 0)
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
      horizontalContentPadding,
      measuredHeight - DIVIDER_HEIGHT,
      horizontalContentPadding + divider.measuredWidth,
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

  private companion object {
    val STATUS_RAIL_WIDTH = 3.dp
    val STATUS_ICON_SIZE = 20.dp
    val STATUS_LABEL_GAP = 6.dp
    val STATUS_TITLE_GAP = 12.dp
    val TITLE_CHIP_GAP = 6.dp
    val CONTENT_START_GAP = 12.dp
    val CONTENT_TOP_PADDING = 8.dp
    val CONTENT_BOTTOM_PADDING = 8.dp
    val STACKED_TITLE_GAP = 4.dp
    val RULE_CHIP_VERTICAL_GAP = 2.dp
    val RULE_CHIP_HEIGHT = 24.dp
    val RULE_CHIP_TOUCH_HEIGHT = 48.dp
    val RULE_CHIP_HORIZONTAL_TOUCH_EXPANSION = 4.dp
    val RULE_CHIP_RADIUS = 4.dp
    val MINIMUM_INLINE_TITLE_WIDTH = 112.dp
    val MINIMUM_HEIGHT = 56.dp
    val DIVIDER_HEIGHT = 1.dp
  }
}
