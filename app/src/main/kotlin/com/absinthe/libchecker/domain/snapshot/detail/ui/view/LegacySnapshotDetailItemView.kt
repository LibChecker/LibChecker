package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailItemDisplayData
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.view.drawable.G2PillDrawable
import com.google.android.material.R as MaterialR
import com.google.android.material.chip.Chip

/** One card layout for all diff types, using the same display data as the modern rows. */
class LegacySnapshotDetailItemView(context: Context) : LinearLayout(context) {
  private val card = LinearLayout(context).apply {
    orientation = HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(16.dp, 12.dp, 16.dp, 12.dp)
  }
  private val statusIcon = AppCompatImageView(context).apply {
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    imageTintList = ColorStateList.valueOf(Color.DKGRAY)
  }
  private val content = LinearLayout(context).apply { orientation = VERTICAL }
  private val title = text(14f, R.style.TextView_SansSerifMedium)
  private val extra = text(12f, R.style.TextView_SansSerifCondensed)
  private val chip = Chip(context).apply {
    setTextColor(Color.BLACK)
    chipStrokeColor = ColorStateList.valueOf(0x20000000)
    chipStrokeWidth = 1.dp.toFloat()
    setEnsureMinTouchTargetSize(true)
    maxLines = 1
  }

  init {
    orientation = VERTICAL
    setPadding(16.dp, 4.dp, 16.dp, 4.dp)
    isFocusable = true
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    foreground = legacySnapshotRipple()
    addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    card.addView(statusIcon, LayoutParams(16.dp, 16.dp))
    card.addView(content, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 8.dp })
    content.addView(title)
    content.addView(extra)
    content.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
  }

  fun render(data: SnapshotDetailItemDisplayData) {
    contentDescription = data.description
    title.text = data.title.withSnapshotTechnicalPathBreakOpportunities()
    extra.text = data.extra
    extra.isVisible = data.extra.isNotBlank()
    statusIcon.setImageResource(data.status.iconRes)
    val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val color = ColorUtils.blendARGB(
      context.getColorByAttr(MaterialR.attr.colorSurface),
      ContextCompat.getColor(context, legacySnapshotStatusColor(data.item.diffType)),
      if (dark) 0.75f else 0.95f
    )
    card.background = G2PillDrawable(fillColor = color, cornerRadius = LEGACY_CARD_CORNER_RADIUS)
    chip.isVisible = data.ruleChip != null
    data.ruleChip?.let { rule ->
      chip.text = rule.label
      chip.chipBackgroundColor = ColorStateList.valueOf(color)
      chip.chipIconTint = null
      chip.chipIcon = ContextCompat.getDrawable(context, rule.iconRes)?.mutate()?.apply {
        clearColorFilter()
        DrawableCompat.setTintList(this, null)
        when {
          rule.isSimpleColorIcon -> DrawableCompat.setTint(this, Color.BLACK)
          !rule.useColorfulIcon -> colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
      }
    }
  }

  fun setChipOnClickListener(listener: OnClickListener?) {
    chip.setOnClickListener(listener)
  }

  private fun text(size: Float, style: Int) = AppCompatTextView(ContextThemeWrapper(context, style)).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    textSize = size
    setTextColor(Color.BLACK)
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
}

internal fun legacySnapshotStatusColor(diffType: Int): Int = when (diffType) {
  ADDED -> R.color.material_green_300
  REMOVED -> R.color.material_red_300
  CHANGED -> R.color.material_yellow_300
  MOVED -> R.color.material_blue_300
  else -> error("Unknown snapshot diff type: $diffType")
}

internal fun View.legacySnapshotRipple(): RippleDrawable = RippleDrawable(
  ColorStateList.valueOf(context.getColorByAttr(androidx.appcompat.R.attr.colorControlHighlight)),
  null,
  InsetDrawable(
    G2PillDrawable(fillColor = Color.WHITE, cornerRadius = LEGACY_CARD_CORNER_RADIUS),
    paddingLeft,
    paddingTop,
    paddingRight,
    paddingBottom
  )
)

private val LEGACY_CARD_CORNER_RADIUS = 16.dp.toFloat()
