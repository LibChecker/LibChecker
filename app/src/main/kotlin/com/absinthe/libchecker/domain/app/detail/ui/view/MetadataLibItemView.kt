package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.LibStringMetadataItemDisplay
import com.absinthe.libchecker.utils.extensions.displayWidth
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setOrHighlightText
import com.absinthe.libchecker.view.app.ResourcePreviewButton
import com.absinthe.libchecker.view.app.TextColumnRowView
import com.google.android.material.chip.Chip

class MetadataLibItemView(context: Context) : TextColumnRowView(context) {

  private val libName = addTextLine(R.style.TextView_SansSerifMedium) {
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleSmall))
  }
  private val libSize = addTextLine(R.style.TextView_SansSerif) {
    setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmall))
    setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
  }
  private val resourcePreview = ResourcePreviewButton(context)
  private val ruleChip = Chip(context).apply {
    isClickable = false
    isFocusable = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48.dp)
    maxWidth = (context.displayWidth * 0.45f).toInt()
    ellipsize = TextUtils.TruncateAt.MIDDLE
  }
  private val trailing = LinearLayout(context).apply {
    gravity = Gravity.CENTER_VERTICAL
    addView(resourcePreview)
    addView(ruleChip)
  }

  init {
    isClickable = true
    isFocusable = true
    clipToPadding = false
    val horizontalPadding = context.getDimensionPixelSize(R.dimen.normal_padding)
    setPadding(horizontalPadding, 4.dp, horizontalPadding, 4.dp)
    minimumHeight = 40.dp
    setTrailingView(
      trailing,
      spacing = horizontalPadding + 8.dp,
      touchTargetSize = 48.dp
    )
  }

  fun bind(
    display: LibStringMetadataItemDisplay,
    highlightText: String,
    colorfulRuleIcon: Boolean,
    onResourceClick: ((LibStringMetadataItemDisplay) -> Unit)?
  ) {
    libName.setLibStringItemName(display.name, highlightText)
    libSize.setOrHighlightText(display.visibleValue, highlightText)
    contentDescription = display.contentDescription
    ruleChip.isVisible = display.rule != null
    display.rule?.let { rule ->
      ruleChip.text = rule.label
      ruleChip.chipIcon = RuleChipIconCache.newDrawable(context, rule, colorfulRuleIcon)
    }
    trailing.isVisible = display.rule != null || display.resource != null
    trailing.setOnClickListener {
      if (display.rule != null) performClick() else resourcePreview.performClick()
    }
    resourcePreview.bind(
      preview = display.preview,
      onClick = if (display.resource != null && onResourceClick != null) {
        { onResourceClick(display) }
      } else {
        null
      }
    )
  }
}
