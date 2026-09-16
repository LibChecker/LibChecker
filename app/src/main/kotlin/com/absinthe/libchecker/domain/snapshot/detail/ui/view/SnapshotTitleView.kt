package com.absinthe.libchecker.domain.snapshot.detail.ui.view

import android.content.Context
import android.graphics.Bitmap
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

  var useLegacyLayout: Boolean = false
    set(value) {
      if (field == value) return
      field = value
      listOf(versionInfoView, packageSizeView, apisView).forEach {
        it.setTextAppearance(
          context.getResourceIdByAttr(
            if (value) MaterialR.attr.textAppearanceBodySmall else MaterialR.attr.textAppearanceBodyMedium
          )
        )
        it.applyCondensedTypeface()
        if (value) it.letterSpacing = 0f
        it.setTextColor(
          context.getColorByAttr(
            if (value || it === versionInfoView) MaterialR.attr.colorOnSurfaceVariant else MaterialR.attr.colorOnSurface
          )
        )
      }
      versionLabelView.isVisible = !value && versionInfoView.isVisible
      packageSizeLabelView.isVisible = !value && packageSizeView.isVisible
      apisLabelView.isVisible = !value && apisView.isVisible
      requestLayout()
    }

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
    versionLabelView.isVisible = !useLegacyLayout && versionInfoView.isVisible
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
    apisLabelView.isVisible = !useLegacyLayout && apisView.isVisible
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
    packageSizeLabelView.isVisible = !useLegacyLayout
    packageSizeView.apply {
      isVisible = true
      packageSizeLineBreaker.setText(data.text, data.breakStart)
      contentDescription = context.getString(
        R.string.snapshot_detail_metric_description,
        context.getString(R.string.snapshot_detail_size_label),
        data.text
      )
      setLongClickCopiedToClipboard(data.text)
    }
  }

  private fun legacyValues() = listOf(versionInfoView, packageSizeView, apisView).filter { it.isVisible }

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
    if (useLegacyLayout) {
      val values = legacyValues()
      values.forEach { value ->
        if (value === versionInfoView) versionInfoLineBreaker.apply(identityTextWidth)
        if (value === packageSizeView) packageSizeLineBreaker.apply(identityTextWidth)
        value.measure(identityTextWidth.toExactlyMeasureSpec(), value.defaultHeightMeasureSpec(this))
      }
      val textHeight = appNameView.measuredHeight + packageNameView.measuredHeight + values.sumOf { it.measuredHeight }
      setMeasuredDimension(measuredWidth, paddingTop + maxOf(iconView.measuredHeight, textHeight) + paddingBottom + 16.dp)
      return
    }
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
    if (useLegacyLayout) {
      val values = listOf(appNameView, packageNameView) + legacyValues()
      val textHeight = values.sumOf { it.measuredHeight }
      val contentHeight = maxOf(iconView.measuredHeight, textHeight)
      iconView.layout(paddingStart, paddingTop + (contentHeight - iconView.measuredHeight) / 2)
      var y = paddingTop + (contentHeight - textHeight) / 2
      values.forEach {
        it.layout(identityTextX, y)
        y += it.measuredHeight
      }
      return
    }
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
