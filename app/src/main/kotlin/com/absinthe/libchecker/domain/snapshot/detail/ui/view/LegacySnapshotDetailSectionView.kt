package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.view.drawable.G2PillDrawable
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.R as MaterialR

class LegacySnapshotDetailSectionView(context: Context) : LinearLayout(context) {
  private val arrow = AppCompatImageView(context).apply {
    setImageResource(R.drawable.ic_snapshot_expand_centered)
    imageTintList = ColorStateList.valueOf(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val label = AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
    setTextAppearance(context.getResourceIdByAttr(android.R.attr.textAppearanceListItemSmall))
    setTypeface(typeface, Typeface.BOLD)
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }
  private val labels = FlexboxLayout(context).apply {
    flexWrap = FlexWrap.WRAP
    alignItems = AlignItems.CENTER
    addView(
      label,
      FlexboxLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        marginEnd = 8.dp
      }
    )
  }
  private var expanded: Boolean? = null

  init {
    setPadding(16.dp, 4.dp, 16.dp, 4.dp)
    isFocusable = true
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    foreground = legacySnapshotRipple()
    val card = LinearLayout(context).apply {
      gravity = Gravity.CENTER_VERTICAL
      minimumHeight = 48.dp
      setPadding(16.dp, 10.dp, 16.dp, 10.dp)
    }
    addView(card, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    card.addView(arrow, LayoutParams(12.dp, 12.dp))
    card.addView(labels, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 10.dp })
  }

  fun render(section: SnapshotDetailSection, isExpanded: Boolean) {
    contentDescription = if (isExpanded) section.expandedDescription else section.collapsedDescription
    label.text = section.title
    while (labels.childCount > section.statusCounts.size + 1) labels.removeViewAt(labels.childCount - 1)
    section.statusCounts.forEachIndexed { index, count ->
      val badge = (labels.getChildAt(index + 1) as? AppCompatTextView) ?: AppCompatTextView(context).apply {
        textSize = 12f
        setTextColor(ContextCompat.getColor(context, R.color.material_grey_800))
        setPadding(8.dp, 2.dp, 8.dp, 2.dp)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        labels.addView(
          this,
          FlexboxLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(4.dp, 2.dp, 4.dp, 2.dp)
          }
        )
      }
      badge.text = count.countText
      val color = when (count.diffType) {
        ADDED -> R.color.material_green_200
        REMOVED -> R.color.material_red_200
        CHANGED -> R.color.material_yellow_200
        MOVED -> R.color.material_blue_200
        else -> error("Unknown snapshot diff type: ${count.diffType}")
      }
      badge.background = G2PillDrawable(fillColor = ContextCompat.getColor(context, color))
    }
    val rotation = if (isExpanded) 90f else 0f
    arrow.animate().cancel()
    if (expanded != null && expanded != isExpanded) {
      arrow.animate().rotation(rotation).setDuration(180).start()
    } else {
      arrow.rotation = rotation
    }
    expanded = isExpanded
  }
}
