package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitlePackageSizeData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.utils.extensions.applyCondensedTypeface
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.app.AlwaysMarqueeTextView
import com.absinthe.libchecker.view.app.AppIdentityHeaderRenderState
import com.absinthe.libchecker.view.app.AppIdentityHeaderRenderer
import com.google.android.material.R as MaterialR

class SnapshotTitleView(
  context: Context,
  attributeSet: AttributeSet? = null
) : AViewGroup(context, attributeSet) {

  private val iconView = AppCompatImageView(context).apply {
    val iconSize = 40.dp
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
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    applyCondensedTypeface()
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurfaceVariant))
    maxLines = Int.MAX_VALUE
    addView(this)
  }
  private val versionInfoLineBreaker = SnapshotDetailLineBreaker(versionInfoView)
  private val versionLabelView = metricLabel(R.string.signature_version)
  private val identityHeaderRenderer = AppIdentityHeaderRenderer(
    iconView = iconView,
    appNameView = appNameView,
    packageNameView = packageNameView,
    versionInfoView = versionInfoView,
    setVersionInfo = versionInfoLineBreaker::setText
  )

  private val packageSizeLabelView = metricLabel(R.string.snapshot_detail_size_label)

  val packageSizeView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    applyCondensedTypeface()
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    maxLines = Int.MAX_VALUE
    addView(this)
  }
  private val packageSizeLineBreaker = SnapshotDetailLineBreaker(packageSizeView)

  private val apisLabelView = metricLabel(R.string.snapshot_detail_sdk_label)

  val apisView = AppCompatTextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
    setTextAppearance(context.getResourceIdByAttr(MaterialR.attr.textAppearanceBodyMedium))
    applyCondensedTypeface()
    setTextColor(context.getColorByAttr(MaterialR.attr.colorOnSurface))
    maxLines = Int.MAX_VALUE
    addView(this)
  }

  fun render(data: SnapshotTitleDisplayData, copyPrimaryText: Boolean = true) {
    identityHeaderRenderer.render(
      AppIdentityHeaderRenderState(
        appName = data.appName,
        iconContentDescription = data.appName,
        packageName = data.packageName,
        versionInfo = data.versionInfo,
        copyPrimaryText = copyPrimaryText
      )
    )
    versionInfoView.isVisible = data.versionInfo.isNotBlank()
    versionLabelView.isVisible = versionInfoView.isVisible
    setPackageSizeText(data.packageSize)
    apisView.apply {
      text = data.apis
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

  private fun setPackageSizeText(data: SnapshotTitlePackageSizeData?) {
    if (data == null) {
      packageSizeView.isVisible = false
      packageSizeLabelView.isVisible = false
      packageSizeLineBreaker.clear()
      return
    }
    packageSizeLabelView.isVisible = true
    packageSizeView.apply {
      isVisible = true
      val displayText = SpannableStringBuilder(data.text)
      Regex("\\([^()]* Bytes\\)").findAll(data.text).forEach { match ->
        displayText.setSpan(RelativeSizeSpan(0.85f), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
      packageSizeLineBreaker.setText(displayText, data.breakStart)
      contentDescription = context.getString(
        R.string.snapshot_detail_metric_description,
        context.getString(R.string.snapshot_detail_size_label),
        data.text
      )
      setLongClickCopiedToClipboard(data.text)
    }
  }

  private fun metricRows() = listOf(
    versionLabelView to versionInfoView,
    apisLabelView to apisView,
    packageSizeLabelView to packageSizeView
  ).filter { it.second.isVisible }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    children.forEach { it.autoMeasure() }
    val contentWidth = measuredWidth - paddingStart - paddingEnd
    val identityTextWidth = (contentWidth - iconView.measuredWidth - IDENTITY_GAP).coerceAtLeast(0)
    measureToWidth(appNameView, identityTextWidth)
    measureToWidth(packageNameView, identityTextWidth)
    val identityHeight = maxOf(iconView.measuredHeight, appNameView.measuredHeight + packageNameView.measuredHeight)
    val rows = metricRows()
    val labelWidth = rows.maxOfOrNull { it.first.measuredWidth } ?: 0
    val valueWidth = (contentWidth - labelWidth - METRIC_GAP).coerceAtLeast(0)
    var height = paddingTop + identityHeight
    rows.forEachIndexed { index, (label, value) ->
      if (value === versionInfoView) versionInfoLineBreaker.apply(valueWidth)
      if (value === packageSizeView) packageSizeLineBreaker.apply(valueWidth)
      value.measure(valueWidth.toExactlyMeasureSpec(), value.defaultHeightMeasureSpec(this))
      height += if (index == 0) METRICS_SECTION_GAP else METRIC_ROW_GAP
      height += planSnapshotMetricRowLayout(label.measuredHeight, label.baseline, value.measuredHeight, value.baseline).height
    }
    setMeasuredDimension(measuredWidth, height + paddingBottom)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val identityTextX = paddingStart + iconView.measuredWidth + IDENTITY_GAP
    val textHeight = appNameView.measuredHeight + packageNameView.measuredHeight
    val identityHeight = maxOf(iconView.measuredHeight, textHeight)
    iconView.layout(paddingStart, paddingTop + (identityHeight - iconView.measuredHeight) / 2)
    appNameView.layout(identityTextX, paddingTop + (identityHeight - textHeight) / 2)
    packageNameView.layout(identityTextX, appNameView.bottom)
    var nextY = paddingTop + identityHeight
    val rows = metricRows()
    val labelWidth = rows.maxOfOrNull { it.first.measuredWidth } ?: 0
    val valueX = paddingStart + labelWidth + METRIC_GAP
    rows.forEachIndexed { index, (label, value) ->
      nextY += if (index == 0) METRICS_SECTION_GAP else METRIC_ROW_GAP
      val row = planSnapshotMetricRowLayout(label.measuredHeight, label.baseline, value.measuredHeight, value.baseline)
      label.layout(paddingStart, nextY + row.labelTopOffset)
      value.layout(valueX, nextY + row.valueTopOffset)
      nextY += row.height
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
    val IDENTITY_GAP = 12.dp
    val METRICS_SECTION_GAP = 12.dp
    val METRIC_GAP = 12.dp
    val METRIC_ROW_GAP = 4.dp
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
