package com.absinthe.libchecker.domain.snapshot.list.ui.adapter

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.text.scale
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.SnapshotListDisplayOptions
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.SnapshotAbiDisplayItem
import com.absinthe.libchecker.domain.snapshot.display.SnapshotUpdateTimeText
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotItemView
import com.absinthe.libchecker.domain.snapshot.list.usecase.stableSnapshotDiffItemIdFor
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setAlphaForAll
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.view.span.CenterAlignImageSpan
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.util.Locale
import kotlin.math.abs

const val ARROW = "→"
const val ARROW_REVERT = "←"
class SnapshotAdapter(
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase,
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase,
  private val cardMode: CardMode = CardMode.NORMAL
) : HighlightAdapter<SnapshotDiffItem>(SnapshotDiffUtil()) {

  init {
    setHasStableIds(true)
  }

  private var displayOptions = SnapshotListDisplayOptions()
  private var packageIconSources: Map<String, SnapshotPackageIconSource> = emptyMap()
  private var apexPackageNames: Set<String> = emptySet()

  fun setDisplayOptions(displayOptions: SnapshotListDisplayOptions) {
    this.displayOptions = displayOptions
  }

  fun setPackageIconSources(packageIconSources: Map<String, SnapshotPackageIconSource>) {
    this.packageIconSources = packageIconSources
  }

  fun setApexPackageNames(apexPackageNames: Set<String>) {
    this.apexPackageNames = apexPackageNames
  }

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      SnapshotItemView(context).also {
        it.layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
    (holder.itemView as SnapshotItemView).apply {
      if (cardMode == CardMode.DEMO || cardMode == CardMode.GET_APP_UPDATE) {
        setSmoothRoundCorner(16.dp)
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
      } else {
        strokeColor = Color.TRANSPARENT
        radius = 0f
      }
    }
    (holder.itemView as SnapshotItemView).container.apply {
      when (val iconSource = packageIconSources[item.packageName]) {
        is SnapshotPackageIconSource.InstalledPackage -> icon.load(iconSource.packageInfo)

        SnapshotPackageIconSource.Fallback,
        null -> icon.load(R.drawable.ic_icon_blueprint)
      }

      if (item.deleted) {
        setAlphaForAll(0.7f)
      } else {
        setAlphaForAll(1.0f)
      }

      val isNewOrDeleted = item.deleted || item.newInstalled
      val highlightDiffColor = if (displayOptions.highlightDiffs) {
        context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
      } else {
        null
      }

      stateIndicator.apply {
        if (cardMode == CardMode.DEMO) {
          startDemoAnimation()
        } else {
          stopDemoAnimation()
          if (isNewOrDeleted) {
            added = false
            removed = false
            changed = false
            moved = false
          } else {
            setSnapshotStateCounts(item.added, item.removed, item.changed, item.moved)
          }
        }
      }

      val appNameLabel = buildSpannedString {
        if (item.isTrackItem) {
          inSpans(ImageSpan(context, R.drawable.ic_track)) {
            append(" ")
          }
        }
        append(LCAppUtils.getDiffString(item.labelDiff, isNewOrDeleted, highlightDiffColor = highlightDiffColor))
      }

      setOrHighlightText(appName, appNameLabel)

      if (isNewOrDeleted) {
        val labelDrawable = if (item.newInstalled) {
          R.drawable.ic_label_new_package.getDrawable(context)!!
        } else {
          R.drawable.ic_label_deleted_package.getDrawable(context)!!
        }
        val sb = SpannableStringBuilder(appName.text)
        val spanString = SpannableString("   ")
        val span = CenterAlignImageSpan(
          labelDrawable.also {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          }
        )
        spanString.setSpan(span, 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append(spanString)
        appName.text = sb
      }

      setOrHighlightText(packageName, item.packageName)
      versionInfo.text = LCAppUtils.getDiffString(
        diff1 = item.versionNameDiff,
        diff2 = item.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted,
        highlightDiffColor = highlightDiffColor
      )

      if (item.packageSizeDiff.old > 0L) {
        packageSizeInfo.isVisible = true
        val sizeDiff = SnapshotDiffItem.DiffNode(
          item.packageSizeDiff.old.sizeToString(context, showBytes = false),
          item.packageSizeDiff.new?.sizeToString(context, showBytes = false)
        )
        val bytesDiff = SnapshotDiffItem.DiffNode(
          item.packageSizeDiff.old,
          item.packageSizeDiff.new
        )
        val diffText = buildSpannedString {
          append(
            LCAppUtils.getDiffString(
              diff1 = sizeDiff,
              diff2 = bytesDiff,
              diff2Suffix = " Bytes",
              isNewOrDeleted = isNewOrDeleted,
              highlightDiffColor = highlightDiffColor
            )
          )

          if (item.packageSizeDiff.new != null) {
            val diffSize = item.packageSizeDiff.new - item.packageSizeDiff.old
            val diffSizeText = buildString {
              if (diffSize > 0) {
                append("+")
              }
              append(diffSize.sizeToString(context))
              append(", ")
              if (diffSize > 0) {
                append("+")
              }
              val percentage = (diffSize.toFloat() / item.packageSizeDiff.old)
              if (abs(percentage) < 0.001f) {
                if (percentage < 0) {
                  append("-")
                }
                append("<0.1%")
              } else {
                append(String.format(Locale.getDefault(), "%.1f%%", percentage * 100))
              }
            }

            if (diffSize != 0L) {
              appendLine()
              append(diffSizeText)
            }
          }
        }

        packageSizeInfo.text = diffText
      } else {
        packageSizeInfo.isGone = true
      }

      val targetDiff = LCAppUtils.getDiffString(item.targetApiDiff, isNewOrDeleted, highlightDiffColor = highlightDiffColor).takeIf { item.targetApiDiff.old > 0 }
      val minDiff = LCAppUtils.getDiffString(item.minSdkDiff, isNewOrDeleted, highlightDiffColor = highlightDiffColor).takeIf { item.minSdkDiff.old > 0 }
      val compileDiff = LCAppUtils.getDiffString(item.compileSdkDiff, isNewOrDeleted, highlightDiffColor = highlightDiffColor).takeIf { item.compileSdkDiff.old > 0 }
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

      val abiDisplayData = buildSnapshotAbiDisplayData(item.abiDiff)
      val oldAbiSpanString = buildAbiSpanString(abiDisplayData.old, tintBadge = false)
      val builder = SpannableStringBuilder(oldAbiSpanString)

      if (item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new) {
        val newAbiSpanString = abiDisplayData.new?.let {
          buildAbiSpanString(it, tintBadge = true)
        } ?: SpannableString("")
        builder.append(" $ARROW ").append(newAbiSpanString)
      }
      abiInfo.text = builder

      val updateTimeDisplayData = buildSnapshotUpdateTimeDisplayData(
        BuildSnapshotUpdateTimeDisplayDataUseCase.Request(
          updateTime = item.updateTime,
          isVisible = displayOptions.showUpdateTime && cardMode != CardMode.GET_APP_UPDATE,
          isApexPackage = item.packageName in apexPackageNames
        )
      )
      updateTime.isVisible = updateTimeDisplayData != null
      updateTimeDisplayData?.let { data ->
        updateTime.text = when (val text = data.text) {
          SnapshotUpdateTimeText.Preinstalled -> context.getString(R.string.snapshot_preinstalled_app)

          is SnapshotUpdateTimeText.LastUpdated ->
            context.getString(R.string.format_last_updated).format(text.timeText)
        }
        if (data.isApexPackage) {
          updateTime.append(", APEX")
        }
      }
      (holder.itemView as SnapshotItemView).contentDescription = buildItemDescription(
        appName.text,
        packageName.text,
        versionInfo.text,
        packageSizeInfo.text.takeIf { packageSizeInfo.isVisible },
        apisInfo.text,
        abiInfo.text,
        updateTime.text.takeIf { updateTime.isVisible },
        buildSnapshotStateDescription(context, item)
      )
    }
  }

  override fun getItemId(position: Int): Long {
    if (position !in data.indices) {
      return Long.MIN_VALUE + position
    }
    return stableSnapshotDiffItemIdFor(data[position])
  }

  private fun buildAbiSpanString(
    item: SnapshotAbiDisplayItem,
    tintBadge: Boolean
  ): SpannableString {
    val badgeRes = item.badgeRes ?: return SpannableString(item.text)
    var paddingString = "  ${item.text}"
    if (item.isMultiArch) {
      paddingString = "  $paddingString"
    }
    val spanString = SpannableString(paddingString)
    badgeRes.getDrawable(context)?.let {
      if (tintBadge) {
        if (displayOptions.tintAbiLabels) {
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
      val span = CenterAlignImageSpan(it)
      spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
    }
    if (item.isMultiArch) {
      R.drawable.ic_multi_arch.getDrawable(context)?.let {
        it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
        val span = CenterAlignImageSpan(it)
        spanString.setSpan(span, 2, 3, ImageSpan.ALIGN_BOTTOM)
      }
    }
    return spanString
  }

  enum class CardMode {
    NORMAL,
    DEMO,
    GET_APP_UPDATE
  }
}

private fun buildItemDescription(vararg parts: CharSequence?): String {
  return parts
    .mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
    .joinToString()
}

private fun buildSnapshotStateDescription(context: android.content.Context, item: SnapshotDiffItem): String {
  return listOf(
    item.added.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_added)} $it" },
    item.removed.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_removed)} $it" },
    item.changed.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_changed)} $it" },
    item.moved.takeIf { it > 0 }?.let { "${context.getString(R.string.snapshot_indicator_moved)} $it" }
  ).filterNotNull().joinToString()
}
