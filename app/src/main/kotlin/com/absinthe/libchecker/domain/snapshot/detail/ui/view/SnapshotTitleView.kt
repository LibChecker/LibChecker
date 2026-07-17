package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
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
    maxLines = Int.MAX_VALUE
    addView(this)
  }

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

  private val summaryView = AppCompatTextView(
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
      text = buildBoldNumericText(data.apis)
      isVisible = data.apis.isNotBlank()
      if (isVisible) {
        contentDescription = context.getString(
          R.string.snapshot_detail_metric_description,
          context.getString(R.string.snapshot_detail_sdk_label),
          data.apis
        )
        setLongClickCopiedToClipboard(data.apis)
      }
    }
    apisLabelView.isVisible = apisView.isVisible
    renderSummary(data.summary)
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
    summaryView.isVisible = visible
    if (summary != null) {
      summaryView.apply {
        text = SpannableStringBuilder().also { builder ->
          val label = context.getString(R.string.snapshot_detail_changes_label)
          builder.append(label)
          builder.setSpan(
            TextAppearanceSpan(
              context,
              context.getResourceIdByAttr(MaterialR.attr.textAppearanceLabelMedium)
            ),
            0,
            label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
          )
          builder.setSpan(
            ForegroundColorSpan(context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant)),
            0,
            label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
          )
          builder.append(' ')
          val totalStart = builder.length
          builder.append(summary.totalCountText)
          builder.setSpan(
            StyleSpan(Typeface.BOLD),
            totalStart,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
          )
          val counts = buildSnapshotDetailCountText(context, summary.counts)
          if (counts.isNotEmpty()) {
            builder.appendSnapshotDetailCountGap()
            builder.append(counts)
          }
        }
        contentDescription = summary.contentDescription
      }
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach { it.autoMeasure() }
    val contentWidth = measuredWidth - paddingStart - paddingEnd
    val identityTextWidth = contentWidth - iconView.measuredWidth - IDENTITY_GAP
    measureToWidth(appNameView, identityTextWidth)
    measureToWidth(packageNameView, identityTextWidth)
    measureToWidth(versionInfoView, identityTextWidth)

    val identityHeight = appNameView.measuredHeight +
      packageNameView.measuredHeight +
      versionInfoView.measuredHeight
    var contentBottom = paddingTop + maxOf(iconView.measuredHeight, identityHeight)

    val metricsContentWidth = (contentWidth - METRICS_HORIZONTAL_PADDING * 2).coerceAtLeast(0)
    val labelWidth = maxOf(
      packageSizeLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0,
      apisLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0
    )
    val valueWidth = (metricsContentWidth - labelWidth - METRIC_GAP).coerceAtLeast(0)
    var metricsContentHeight = 0
    if (packageSizeView.isVisible) {
      packageSizeLineBreaker.apply(valueWidth)
      packageSizeView.measure(
        valueWidth.toExactlyMeasureSpec(),
        packageSizeView.defaultHeightMeasureSpec(this)
      )
      metricsContentHeight += planSnapshotMetricRowLayout(
        labelHeight = packageSizeLabelView.measuredHeight,
        labelBaseline = packageSizeLabelView.baseline,
        valueHeight = packageSizeView.measuredHeight,
        valueBaseline = packageSizeView.baseline
      ).height
    }
    if (apisView.isVisible) {
      if (packageSizeView.isVisible) {
        metricsContentHeight += METRIC_ROW_GAP
      }
      apisView.measure(valueWidth.toExactlyMeasureSpec(), apisView.defaultHeightMeasureSpec(this))
      metricsContentHeight += planSnapshotMetricRowLayout(
        labelHeight = apisLabelView.measuredHeight,
        labelBaseline = apisLabelView.baseline,
        valueHeight = apisView.measuredHeight,
        valueBaseline = apisView.baseline
      ).height
    }
    if (summaryView.isVisible) {
      if (packageSizeView.isVisible || apisView.isVisible) {
        metricsContentHeight += METRIC_ROW_GAP
      }
      summaryView.measure(
        metricsContentWidth.toExactlyMeasureSpec(),
        summaryView.defaultHeightMeasureSpec(this)
      )
      metricsContentHeight += summaryView.measuredHeight
    }
    if (hasVisibleMetrics()) {
      contentBottom += METRICS_SECTION_GAP +
        metricsContentHeight
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

    if (hasVisibleMetrics()) {
      nextY += METRICS_SECTION_GAP
    }
    val labelWidth = maxOf(
      packageSizeLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0,
      apisLabelView.takeIf { it.isVisible }?.measuredWidth ?: 0
    )
    val metricsContentStart = paddingStart + METRICS_HORIZONTAL_PADDING
    val valueX = metricsContentStart + labelWidth + METRIC_GAP
    if (packageSizeView.isVisible) {
      val row = planSnapshotMetricRowLayout(
        labelHeight = packageSizeLabelView.measuredHeight,
        labelBaseline = packageSizeLabelView.baseline,
        valueHeight = packageSizeView.measuredHeight,
        valueBaseline = packageSizeView.baseline
      )
      packageSizeLabelView.layout(metricsContentStart, nextY + row.labelTopOffset)
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
      apisLabelView.layout(metricsContentStart, nextY + row.labelTopOffset)
      apisView.layout(valueX, nextY + row.valueTopOffset)
      nextY += row.height
    }
    if (summaryView.isVisible) {
      if (packageSizeView.isVisible || apisView.isVisible) {
        nextY += METRIC_ROW_GAP
      }
      summaryView.layout(metricsContentStart, nextY)
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

  private fun hasVisibleMetrics(): Boolean {
    return packageSizeView.isVisible || apisView.isVisible || summaryView.isVisible
  }

  private companion object {
    val IDENTITY_GAP = 16.dp
    val METRICS_SECTION_GAP = 16.dp
    val METRICS_HORIZONTAL_PADDING = 12.dp
    val METRIC_GAP = 16.dp
    val METRIC_ROW_GAP = 8.dp
  }
}

private fun buildBoldNumericText(text: CharSequence): CharSequence {
  return SpannableStringBuilder(text).apply {
    SDK_NUMBER_REGEX.findAll(text).forEach { match ->
      setSpan(
        StyleSpan(Typeface.BOLD),
        match.range.first,
        match.range.last + 1,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
    }
  }
}

private val SDK_NUMBER_REGEX = Regex("\\d+")

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
