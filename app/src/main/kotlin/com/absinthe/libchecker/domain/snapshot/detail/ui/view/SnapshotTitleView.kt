package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotDetailSummaryRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotTitlePackageSizeRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.SnapshotTitleRenderState
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.ui.view.SnapshotPackageSizeLineBreaker
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView
import com.google.android.material.R as MaterialR

class SnapshotTitleView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  private val iconView = AppCompatImageView(context).apply {
    val iconSize = context.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    layoutParams = LayoutParams(iconSize, iconSize)
    setImageResource(R.drawable.ic_icon_blueprint)
    addView(this)
  }

  val appNameView = AlwaysMarqueeTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceTitleMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    setTypeface(typeface, Typeface.BOLD)
    addView(this)
  }

  val packageNameView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant))
    maxLines = 2
    addView(this)
  }

  val versionInfoView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodySmall))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant))
    maxLines = 2
    addView(this)
  }

  private val detailDivider = dividerView()

  private val packageSizeLabelView = metricLabel(R.string.snapshot_detail_size_label)

  val packageSizeView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    maxLines = Int.MAX_VALUE
    addView(this)
  }
  private val packageSizeLineBreaker = SnapshotPackageSizeLineBreaker(packageSizeView)

  private val apisLabelView = metricLabel(R.string.snapshot_detail_sdk_label)

  val apisView = AppCompatTextView(
    ContextThemeWrapper(context, R.style.TextView_SansSerifCondensed)
  ).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    maxLines = Int.MAX_VALUE
    addView(this)
  }

  private val summaryDivider = dividerView()

  private val summaryTotalView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceTitleSmall))
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    setTypeface(typeface, Typeface.BOLD)
    addView(this)
  }

  private val summaryCountsView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelLarge))
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    addView(this)
  }

  private var summaryCountsOnSecondLine = false

  fun render(data: SnapshotTitleRenderState) {
    appNameView.apply {
      text = data.appName
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    iconView.contentDescription = data.iconContentDescription
    packageNameView.apply {
      text = data.packageName
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    versionInfoView.apply {
      text = data.versionInfo
      if (data.copyPrimaryText) {
        setLongClickCopiedToClipboard(text)
      }
    }
    setPackageSizeText(data.packageSize)
    apisView.apply {
      text = data.apis
      isVisible = data.apis.isNotBlank()
      if (isVisible) {
        contentDescription = context.getString(
          R.string.snapshot_detail_metric_description,
          context.getString(R.string.snapshot_detail_sdk_label),
          text
        )
        setLongClickCopiedToClipboard(text)
      }
    }
    apisLabelView.isVisible = apisView.isVisible
    renderSummary(data.summary)
    detailDivider.isVisible = packageSizeView.isVisible || apisView.isVisible
  }

  fun setIconImage(bitmap: Bitmap?) {
    if (bitmap == null) {
      setFallbackIcon()
    } else {
      iconView.load(bitmap)
    }
  }

  fun setIconSource(iconSource: SnapshotPackageIconSource?) {
    when (iconSource) {
      is SnapshotPackageIconSource.InstalledPackage -> iconView.load(iconSource.packageInfo)

      SnapshotPackageIconSource.Fallback,
      null -> setFallbackIcon()
    }
  }

  fun setFallbackIcon() {
    iconView.setImageResource(R.drawable.ic_icon_blueprint)
  }

  fun setIconClickListener(listener: OnClickListener?) {
    iconView.setOnClickListener(listener)
  }

  private fun setPackageSizeText(data: SnapshotTitlePackageSizeRenderState?) {
    if (data == null) {
      packageSizeView.isVisible = false
      packageSizeLabelView.isVisible = false
      packageSizeLineBreaker.clear()
      return
    }
    packageSizeLabelView.isVisible = true
    packageSizeView.apply {
      isVisible = true
      packageSizeLineBreaker.setText(data.text, data.breakStart)
      contentDescription = context.getString(
        R.string.snapshot_detail_metric_description,
        context.getString(R.string.snapshot_detail_size_label),
        data.text
      )
      setLongClickCopiedToClipboard(text)
    }
  }

  private fun renderSummary(summary: SnapshotDetailSummaryRenderState?) {
    val visible = summary != null
    summaryDivider.isVisible = visible
    summaryTotalView.isVisible = visible
    summaryCountsView.isVisible = visible
    if (summary != null) {
      summaryTotalView.apply {
        text = summary.totalCountText
        contentDescription = summary.contentDescription
      }
      summaryCountsView.text = buildSnapshotDetailCountText(context, summary.counts)
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.filter { it !== detailDivider && it !== summaryDivider }.forEach { it.autoMeasure() }
    val contentWidth = measuredWidth - paddingStart - paddingEnd
    val identityTextWidth = contentWidth - iconView.measuredWidth - IDENTITY_GAP
    measureToWidth(appNameView, identityTextWidth)
    measureToWidth(packageNameView, identityTextWidth)
    measureToWidth(versionInfoView, identityTextWidth)

    val identityHeight = appNameView.measuredHeight +
      packageNameView.measuredHeight +
      versionInfoView.measuredHeight
    var contentBottom = paddingTop + maxOf(iconView.measuredHeight, identityHeight)

    if (detailDivider.isVisible) {
      detailDivider.measure(contentWidth.toExactlyMeasureSpec(), 1.dp.toExactlyMeasureSpec())
      contentBottom += SECTION_GAP
      contentBottom += detailDivider.measuredHeight + SECTION_GAP
    }

    val labelWidth = maxOf(
      packageSizeLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0,
      apisLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0
    )
    val valueWidth = (contentWidth - labelWidth - METRIC_GAP).coerceAtLeast(0)
    if (packageSizeView.isVisible) {
      packageSizeLineBreaker.apply(valueWidth)
      packageSizeView.measure(
        valueWidth.toExactlyMeasureSpec(),
        packageSizeView.defaultHeightMeasureSpec(this)
      )
      contentBottom += planSnapshotMetricRowLayout(
        labelHeight = packageSizeLabelView.measuredHeight,
        labelBaseline = packageSizeLabelView.baseline,
        valueHeight = packageSizeView.measuredHeight,
        valueBaseline = packageSizeView.baseline
      ).height
    }
    if (apisView.isVisible) {
      if (packageSizeView.isVisible) {
        contentBottom += METRIC_ROW_GAP
      }
      apisView.measure(valueWidth.toExactlyMeasureSpec(), apisView.defaultHeightMeasureSpec(this))
      contentBottom += planSnapshotMetricRowLayout(
        labelHeight = apisLabelView.measuredHeight,
        labelBaseline = apisLabelView.baseline,
        valueHeight = apisView.measuredHeight,
        valueBaseline = apisView.baseline
      ).height
    }

    if (summaryDivider.isVisible) {
      summaryDivider.measure(contentWidth.toExactlyMeasureSpec(), 1.dp.toExactlyMeasureSpec())
      contentBottom += SECTION_GAP + summaryDivider.measuredHeight + SECTION_GAP
      summaryCountsOnSecondLine = summaryTotalView.measuredWidth + SUMMARY_GAP +
        summaryCountsView.measuredWidth > contentWidth
      if (summaryCountsView.measuredWidth > contentWidth) {
        summaryCountsView.measure(
          contentWidth.toExactlyMeasureSpec(),
          summaryCountsView.defaultHeightMeasureSpec(this)
        )
      }
      contentBottom += if (summaryCountsOnSecondLine) {
        summaryTotalView.measuredHeight + SUMMARY_SECOND_LINE_GAP + summaryCountsView.measuredHeight
      } else {
        maxOf(summaryTotalView.measuredHeight, summaryCountsView.measuredHeight)
      }
    } else {
      summaryCountsOnSecondLine = false
    }
    setMeasuredDimension(measuredWidth, contentBottom + paddingBottom)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val identityTextX = paddingStart + iconView.measuredWidth + IDENTITY_GAP
    iconView.layout(paddingStart, paddingTop)
    appNameView.layout(identityTextX, paddingTop)
    packageNameView.layout(identityTextX, appNameView.bottom)
    versionInfoView.layout(identityTextX, packageNameView.bottom)
    var nextY = paddingTop + maxOf(
      iconView.measuredHeight,
      appNameView.measuredHeight + packageNameView.measuredHeight + versionInfoView.measuredHeight
    )

    if (detailDivider.isVisible) {
      nextY += SECTION_GAP
      detailDivider.layout(paddingStart, nextY)
      nextY = detailDivider.bottom + SECTION_GAP
    }
    val labelWidth = maxOf(
      packageSizeLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0,
      apisLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0
    )
    val valueX = paddingStart + labelWidth + METRIC_GAP
    if (packageSizeView.isVisible) {
      val row = planSnapshotMetricRowLayout(
        labelHeight = packageSizeLabelView.measuredHeight,
        labelBaseline = packageSizeLabelView.baseline,
        valueHeight = packageSizeView.measuredHeight,
        valueBaseline = packageSizeView.baseline
      )
      packageSizeLabelView.layout(paddingStart, nextY + row.labelTopOffset)
      packageSizeView.layout(valueX, nextY + row.valueTopOffset)
      nextY += row.height
    }
    if (apisView.isVisible) {
      if (packageSizeView.isVisible) {
        nextY += METRIC_ROW_GAP
      }
      val row = planSnapshotMetricRowLayout(
        labelHeight = apisLabelView.measuredHeight,
        labelBaseline = apisLabelView.baseline,
        valueHeight = apisView.measuredHeight,
        valueBaseline = apisView.baseline
      )
      apisLabelView.layout(paddingStart, nextY + row.labelTopOffset)
      apisView.layout(valueX, nextY + row.valueTopOffset)
      nextY += row.height
    }

    if (summaryDivider.isVisible) {
      nextY += SECTION_GAP
      summaryDivider.layout(paddingStart, nextY)
      nextY = summaryDivider.bottom + SECTION_GAP
      summaryTotalView.layout(paddingStart, nextY)
      if (summaryCountsOnSecondLine) {
        summaryCountsView.layout(
          paddingStart,
          summaryTotalView.bottom + SUMMARY_SECOND_LINE_GAP
        )
      } else {
        summaryCountsView.layout(
          measuredWidth - paddingEnd - summaryCountsView.measuredWidth,
          nextY + (summaryTotalView.measuredHeight - summaryCountsView.measuredHeight) / 2
        )
      }
    }
  }

  private fun dividerView(): View {
    return View(context).apply {
      layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp)
      setBackgroundColor(context.getColorByAttr(MaterialR.attr.colorOutlineVariant))
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      addView(this)
    }
  }

  private fun metricLabel(textRes: Int): AppCompatTextView {
    return AppCompatTextView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelMedium))
      setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant))
      setText(textRes)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      addView(this)
    }
  }

  private fun measureToWidth(view: AppCompatTextView, width: Int) {
    if (view.measuredWidth > width) {
      view.measure(width.toExactlyMeasureSpec(), view.defaultHeightMeasureSpec(this))
    }
  }

  private companion object {
    val IDENTITY_GAP = 16.dp
    val SECTION_GAP = 12.dp
    val METRIC_GAP = 16.dp
    val METRIC_ROW_GAP = 8.dp
    val SUMMARY_GAP = 16.dp
    val SUMMARY_SECOND_LINE_GAP = 4.dp
  }
}

internal data class SnapshotMetricRowLayout(
  val labelTopOffset: Int,
  val valueTopOffset: Int,
  val height: Int
)

internal fun planSnapshotMetricRowLayout(
  labelHeight: Int,
  labelBaseline: Int,
  valueHeight: Int,
  valueBaseline: Int
): SnapshotMetricRowLayout {
  val sharedBaseline = maxOf(labelBaseline, valueBaseline)
  val labelTopOffset = sharedBaseline - labelBaseline
  val valueTopOffset = sharedBaseline - valueBaseline
  return SnapshotMetricRowLayout(
    labelTopOffset = labelTopOffset,
    valueTopOffset = valueTopOffset,
    height = maxOf(
      labelTopOffset + labelHeight,
      valueTopOffset + valueHeight
    )
  )
}
