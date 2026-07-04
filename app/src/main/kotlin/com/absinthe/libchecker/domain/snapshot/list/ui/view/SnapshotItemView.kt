package com.absinthe.libchecker.domain.snapshot.list.ui.view

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.scale
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.marginStart
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeDisplayData
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeText
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemStateIndicatorData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.ui.view.SnapshotPackageSizeLineBreaker
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.applyCondensedSingleLine
import com.absinthe.libchecker.utils.extensions.applySingleLineEndEllipsize
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr
import com.absinthe.libchecker.utils.extensions.setAlphaForAll
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.tintHighlightText
import com.absinthe.libchecker.utils.extensions.visibleHeight
import com.absinthe.libchecker.view.AViewGroup
import com.absinthe.libchecker.view.span.CenterAlignImageSpan
import com.google.android.material.card.MaterialCardView
import java.util.Locale
import kotlin.math.abs

class SnapshotItemView(context: Context) : MaterialCardView(context) {

  private val container = SnapshotItemContainerView(context).apply {
    val padding = context.getDimensionPixelSize(R.dimen.main_card_padding)
    setPadding(padding, padding, padding, padding)
  }

  init {
    layoutParams = ViewGroup.LayoutParams(
      LayoutParams.MATCH_PARENT,
      LayoutParams.WRAP_CONTENT
    )
    addView(container)
  }

  fun render(data: SnapshotItemDisplayData) {
    val isNewOrDeleted = data.isNewInstalled || data.isDeleted
    setCardPresentation(data.cardPresentation)
    container.apply {
      setIconSource(data.iconSource)
      setDeleted(data.isDeleted)
      setStateIndicator(
        data = data.stateIndicator,
        isNewOrDeleted = isNewOrDeleted
      )
      setAppNameDisplay(
        labelDiff = data.labelDiff,
        isTrackItem = data.isTrackItem,
        packageStateLabel = data.packageStateLabel,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = data.highlightDiffs,
        highlightText = data.highlightText
      )
      setPackageNameDisplay(data.packageName, data.highlightText)
      setVersionDisplay(
        versionNameDiff = data.versionNameDiff,
        versionCodeDiff = data.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = data.highlightDiffs
      )
      setPackageSizeDisplay(
        packageSizeDiff = data.packageSizeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = data.highlightDiffs
      )
      setApiDisplay(
        targetApiDiff = data.api.targetApiDiff,
        minSdkDiff = data.api.minSdkDiff,
        compileSdkDiff = data.api.compileSdkDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = data.highlightDiffs
      )
      setAbiDisplay(
        abiDisplayData = data.abi.abiDisplayData,
        showChangedAbi = data.abi.showChangedAbi,
        tintChangedAbiBadge = data.abi.tintChangedAbiBadge
      )
      setUpdateTimeDisplay(data.updateTimeDisplayData)
    }
    setItemContentDescription(data.stateIndicator)
  }

  private fun setCardPresentation(cardPresentation: SnapshotItemCardPresentation) {
    when (cardPresentation) {
      SnapshotItemCardPresentation.Normal -> {
        strokeColor = Color.TRANSPARENT
        radius = 0f
      }

      SnapshotItemCardPresentation.Rounded -> {
        setSmoothRoundCorner(16.dp)
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
      }
    }
  }

  private fun setItemContentDescription(stateIndicator: SnapshotItemStateIndicatorData) {
    contentDescription = buildItemDescription(
      container.appName.text,
      container.packageName.text,
      container.versionInfo.text,
      container.packageSizeInfo.text.takeIf { container.packageSizeInfo.isVisible },
      container.apisInfo.text,
      container.abiInfo.text,
      container.updateTime.text.takeIf { container.updateTime.isVisible },
      buildSnapshotStateDescription(
        stateIndicator.added,
        stateIndicator.removed,
        stateIndicator.changed,
        stateIndicator.moved
      )
    )
  }

  enum class PackageStateLabel {
    New,
    Deleted
  }

  class SnapshotItemContainerView(context: Context) : AViewGroup(context) {

    val icon = AppCompatImageView(context).apply {
      val iconSize = context.getDimensionPixelSize(R.dimen.app_icon_size)
      layoutParams = LayoutParams(iconSize, iconSize)
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
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceTitleMedium))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      addView(this)
    }

    val packageName = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceBodySmallEmphasized))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applySingleLineEndEllipsize()
      addView(this)
    }

    val versionInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applyCondensedSingleLine()
      addView(this)
    }

    val packageSizeInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applyCondensedSingleLine()
      setSingleLine(false)
      setHorizontallyScrolling(false)
      maxLines = Int.MAX_VALUE
      ellipsize = null
      addView(this)
    }
    private val packageSizeLineBreaker = SnapshotPackageSizeLineBreaker(packageSizeInfo)

    val apisInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applyCondensedSingleLine()
      addView(this)
    }

    val abiInfo = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      clipChildren = false
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applyCondensedSingleLine()
      addView(this)
    }

    val updateTime = AppCompatTextView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextAppearance(context.getResourceIdByAttr(com.google.android.material.R.attr.textAppearanceLabelSmall))
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      applyCondensedSingleLine()
      addView(this)
    }

    val stateIndicator = SnapshotStateIndicatorView(context).apply {
      layoutParams = LayoutParams(5.dp, ViewGroup.LayoutParams.MATCH_PARENT)
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      addView(this)
    }

    fun setIconSource(iconSource: SnapshotPackageIconSource?) {
      when (iconSource) {
        is SnapshotPackageIconSource.InstalledPackage -> icon.load(iconSource.packageInfo)

        SnapshotPackageIconSource.Fallback,
        null -> icon.load(R.drawable.ic_icon_blueprint)
      }
    }

    fun setDeleted(isDeleted: Boolean) {
      setAlphaForAll(if (isDeleted) 0.7f else 1.0f)
    }

    fun setStateIndicator(
      data: SnapshotItemStateIndicatorData,
      isNewOrDeleted: Boolean
    ) {
      if (data.animate) {
        stateIndicator.startDemoAnimation()
        return
      }

      stateIndicator.stopDemoAnimation()
      if (isNewOrDeleted) {
        stateIndicator.added = false
        stateIndicator.removed = false
        stateIndicator.changed = false
        stateIndicator.moved = false
      } else {
        stateIndicator.setSnapshotStateCounts(
          data.added,
          data.removed,
          data.changed,
          data.moved
        )
      }
    }

    fun setAppNameDisplay(
      labelDiff: SnapshotDiffItem.DiffNode<String>,
      isTrackItem: Boolean,
      packageStateLabel: PackageStateLabel?,
      isNewOrDeleted: Boolean,
      highlightDiffs: Boolean,
      highlightText: String
    ) {
      val highlightDiffColor = getHighlightDiffColor(highlightDiffs)
      val appNameLabel = buildSpannedString {
        if (isTrackItem) {
          inSpans(ImageSpan(context, R.drawable.ic_track)) {
            append(" ")
          }
        }
        append(
          LCAppUtils.getDiffString(
            diff = labelDiff,
            isNewOrDeleted = isNewOrDeleted,
            highlightDiffColor = highlightDiffColor
          )
        )
      }
      appName.setOrHighlightText(appNameLabel, highlightText)
      appendPackageStateLabel(packageStateLabel)
    }

    fun setPackageNameDisplay(text: CharSequence, highlightText: String) {
      packageName.setOrHighlightText(text, highlightText)
    }

    private fun appendPackageStateLabel(packageStateLabel: PackageStateLabel?) {
      val labelDrawable = when (packageStateLabel) {
        PackageStateLabel.New -> R.drawable.ic_label_new_package
        PackageStateLabel.Deleted -> R.drawable.ic_label_deleted_package
        null -> return
      }.getDrawable(context) ?: return
      val spanString = SpannableString("   ")
      spanString.setSpan(
        CenterAlignImageSpan(
          labelDrawable.also {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          }
        ),
        1,
        2,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      appName.text = SpannableStringBuilder(appName.text).append(spanString)
    }

    fun setVersionDisplay(
      versionNameDiff: SnapshotDiffItem.DiffNode<String>,
      versionCodeDiff: SnapshotDiffItem.DiffNode<Long>,
      isNewOrDeleted: Boolean,
      highlightDiffs: Boolean
    ) {
      versionInfo.text = LCAppUtils.getDiffString(
        diff1 = versionNameDiff,
        diff2 = versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = getHighlightDiffColor(highlightDiffs)
      )
    }

    fun setAbiDisplay(
      abiDisplayData: SnapshotAbiDisplayData,
      showChangedAbi: Boolean,
      tintChangedAbiBadge: Boolean
    ) {
      val builder = SpannableStringBuilder(
        buildAbiSpanString(
          item = abiDisplayData.old,
          tintBadge = false,
          tintAbiLabels = tintChangedAbiBadge
        )
      )
      if (showChangedAbi) {
        val changedAbiSpanString = abiDisplayData.new?.let {
          buildAbiSpanString(
            item = it,
            tintBadge = true,
            tintAbiLabels = tintChangedAbiBadge
          )
        } ?: SpannableString("")
        builder.append(" $ABI_CHANGE_ARROW ").append(changedAbiSpanString)
      }
      abiInfo.text = builder
    }

    fun setPackageSizeDisplay(
      packageSizeDiff: SnapshotDiffItem.DiffNode<Long>,
      isNewOrDeleted: Boolean,
      highlightDiffs: Boolean
    ) {
      if (packageSizeDiff.old <= 0L) {
        packageSizeInfo.isVisible = false
        clearPackageSizeText()
        return
      }

      packageSizeInfo.isVisible = true
      val sizeDiff = SnapshotDiffItem.DiffNode(
        packageSizeDiff.old.sizeToString(context, showBytes = false),
        packageSizeDiff.new?.sizeToString(context, showBytes = false)
      )
      val bytesDiff = SnapshotDiffItem.DiffNode(
        packageSizeDiff.old,
        packageSizeDiff.new
      )
      var packageSizeBreakStart = -1
      val diffText = buildSpannedString {
        append(
          LCAppUtils.getDiffString(
            diff1 = sizeDiff,
            diff2 = bytesDiff,
            diff2Suffix = " Bytes",
            isNewOrDeleted = isNewOrDeleted,
            highlightDiffColor = getHighlightDiffColor(highlightDiffs)
          )
        )

        if (packageSizeDiff.new != null) {
          val diffSize = packageSizeDiff.new - packageSizeDiff.old
          if (diffSize != 0L) {
            append(" ")
            packageSizeBreakStart = length
            append(buildPackageSizeChangeText(diffSize, packageSizeDiff.old))
          }
        }
      }

      setPackageSizeText(diffText, packageSizeBreakStart)
    }

    fun setApiDisplay(
      targetApiDiff: SnapshotDiffItem.DiffNode<Short>,
      minSdkDiff: SnapshotDiffItem.DiffNode<Short>,
      compileSdkDiff: SnapshotDiffItem.DiffNode<Short>,
      isNewOrDeleted: Boolean,
      highlightDiffs: Boolean
    ) {
      val targetDiff = buildApiDiffString(
        diff = targetApiDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = highlightDiffs
      )
      val minDiff = buildApiDiffString(
        diff = minSdkDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = highlightDiffs
      )
      val compileDiff = buildApiDiffString(
        diff = compileSdkDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffs = highlightDiffs
      )
      apisInfo.text = buildSpannedString {
        targetDiff?.let {
          scale(1f) {
            append("Target: ")
          }
          append(it)
          append("  ")
        }

        minDiff?.let {
          scale(1f) {
            append("Min: ")
          }
          append(it)
          append("  ")
        }

        compileDiff?.let {
          scale(1f) {
            append("Compile: ")
          }
          append(it)
        }
      }
    }

    fun setUpdateTimeDisplay(updateTimeDisplayData: SnapshotUpdateTimeDisplayData?) {
      updateTime.isVisible = updateTimeDisplayData != null
      if (updateTimeDisplayData == null) {
        updateTime.text = null
        return
      }

      updateTime.text = when (val text = updateTimeDisplayData.text) {
        SnapshotUpdateTimeText.Preinstalled -> context.getString(R.string.snapshot_preinstalled_app)

        is SnapshotUpdateTimeText.LastUpdated ->
          context.getString(R.string.format_last_updated).format(text.timeText)
      }
      if (updateTimeDisplayData.isApexPackage) {
        updateTime.append(", APEX")
      }
    }

    fun setPackageSizeText(text: CharSequence, breakStart: Int) {
      packageSizeLineBreaker.setText(text, breakStart)
    }

    fun clearPackageSizeText() {
      packageSizeLineBreaker.clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
      children.forEach {
        it.autoMeasure()
      }
      val textWidth =
        measuredWidth - paddingStart - paddingEnd - icon.measuredWidth - 5.dp - appName.marginStart
      if (appName.measuredWidth > textWidth) {
        appName.measure(textWidth.toExactlyMeasureSpec(), appName.defaultHeightMeasureSpec(this))
      }
      if (packageName.measuredWidth > textWidth) {
        packageName.measure(textWidth.toExactlyMeasureSpec(), packageName.defaultHeightMeasureSpec(this))
      }
      if (versionInfo.measuredWidth > textWidth) {
        versionInfo.measure(textWidth.toExactlyMeasureSpec(), versionInfo.defaultHeightMeasureSpec(this))
      }
      if (packageSizeInfo.isVisible) {
        packageSizeLineBreaker.apply(textWidth)
        packageSizeInfo.measure(textWidth.toExactlyMeasureSpec(), packageSizeInfo.defaultHeightMeasureSpec(this))
      }
      if (apisInfo.measuredWidth > textWidth) {
        apisInfo.measure(textWidth.toExactlyMeasureSpec(), apisInfo.defaultHeightMeasureSpec(this))
      }
      if (updateTime.measuredWidth > textWidth) {
        updateTime.measure(textWidth.toExactlyMeasureSpec(), updateTime.defaultHeightMeasureSpec(this))
      }
      if (abiInfo.measuredWidth > textWidth) {
        abiInfo.measure(textWidth.toExactlyMeasureSpec(), abiInfo.defaultHeightMeasureSpec(this))
      }

      setMeasuredDimension(
        measuredWidth,
        (
          paddingTop +
            appName.measuredHeight +
            packageName.measuredHeight +
            versionInfo.measuredHeight +
            packageSizeInfo.visibleHeight() +
            apisInfo.measuredHeight +
            updateTime.visibleHeight() +
            abiInfo.measuredHeight +
            paddingBottom
          )
      )
      stateIndicator.measure(
        stateIndicator.defaultWidthMeasureSpec(this),
        (measuredHeight - paddingTop - paddingBottom).toExactlyMeasureSpec()
      )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
      icon.layout(paddingStart, icon.toVerticalCenter(this))
      val appNameXOffset = paddingStart + icon.measuredWidth + appName.marginStart
      appName.layout(appNameXOffset, paddingTop)
      packageName.layout(appNameXOffset, appName.bottom)
      versionInfo.layout(appNameXOffset, packageName.bottom)
      packageSizeInfo.layout(appNameXOffset, versionInfo.bottom)
      apisInfo.layout(
        appNameXOffset,
        if (packageSizeInfo.isVisible) packageSizeInfo.bottom else versionInfo.bottom
      )
      updateTime.layout(appNameXOffset, apisInfo.bottom)
      abiInfo.layout(
        appNameXOffset,
        if (updateTime.isVisible) updateTime.bottom else apisInfo.bottom
      )
      stateIndicator.layout(
        paddingEnd,
        stateIndicator.toVerticalCenter(this),
        fromRight = true
      )
    }
  }
}

private const val ABI_CHANGE_ARROW = "→"

private val SnapshotItemDisplayData.packageStateLabel: SnapshotItemView.PackageStateLabel?
  get() = when {
    isNewInstalled -> SnapshotItemView.PackageStateLabel.New
    isDeleted -> SnapshotItemView.PackageStateLabel.Deleted
    else -> null
  }

private fun SnapshotItemView.buildSnapshotStateDescription(
  added: Int,
  removed: Int,
  changed: Int,
  moved: Int
): String {
  return listOf(
    added.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_added)} $it" },
    removed.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_removed)} $it" },
    changed.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_changed)} $it" },
    moved.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_moved)} $it" }
  ).filterNotNull().joinToString()
}

private fun buildItemDescription(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

private fun AppCompatTextView.setOrHighlightText(text: CharSequence, highlightText: String) {
  if (highlightText.isNotBlank()) {
    tintHighlightText(highlightText, text)
  } else {
    this.text = text
  }
}

private fun SnapshotItemView.SnapshotItemContainerView.buildApiDiffString(
  diff: SnapshotDiffItem.DiffNode<Short>,
  isNewOrDeleted: Boolean,
  highlightDiffs: Boolean
): CharSequence? {
  return LCAppUtils.getDiffString(
    diff = diff,
    isNewOrDeleted = isNewOrDeleted,
    highlightDiffColor = getHighlightDiffColor(highlightDiffs)
  ).takeIf { diff.old > 0 }
}

private fun SnapshotItemView.SnapshotItemContainerView.getHighlightDiffColor(highlightDiffs: Boolean): Int? {
  return if (highlightDiffs) {
    context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
  } else {
    null
  }
}

private fun SnapshotItemView.SnapshotItemContainerView.buildPackageSizeChangeText(
  diffSize: Long,
  oldSize: Long
): String {
  return buildString {
    if (diffSize > 0) {
      append("+")
    }
    append(diffSize.sizeToString(context))
    append(", ")
    if (diffSize > 0) {
      append("+")
    }
    val percentage = diffSize.toFloat() / oldSize
    if (abs(percentage) < 0.001f) {
      if (percentage < 0) {
        append("-")
      }
      append("<0.1%")
    } else {
      append(String.format(Locale.getDefault(), "%.1f%%", percentage * 100))
    }
  }
}

private fun SnapshotItemView.SnapshotItemContainerView.buildAbiSpanString(
  item: SnapshotAbiDisplayItem,
  tintBadge: Boolean,
  tintAbiLabels: Boolean
): SpannableString {
  val badgeRes = item.badgeRes ?: return SpannableString(item.text)
  var paddingString = "  ${item.text}"
  if (item.isMultiArch) {
    paddingString = "  $paddingString"
  }
  val spanString = SpannableString(paddingString)
  badgeRes.getDrawable(context)?.let {
    if (tintBadge) {
      if (tintAbiLabels) {
        if (badgeRes == R.drawable.ic_abi_label_64bit) {
          it.setTint(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary))
        } else {
          it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorTertiary))
        }
      } else {
        it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))
      }
    }
    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
    spanString.setSpan(CenterAlignImageSpan(it), 0, 1, ImageSpan.ALIGN_BOTTOM)
  }
  if (item.isMultiArch) {
    R.drawable.ic_multi_arch.getDrawable(context)?.let {
      it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
      spanString.setSpan(CenterAlignImageSpan(it), 2, 3, ImageSpan.ALIGN_BOTTOM)
    }
  }
  return spanString
}
