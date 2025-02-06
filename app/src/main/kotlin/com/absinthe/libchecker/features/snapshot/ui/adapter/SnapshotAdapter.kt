package com.absinthe.libchecker.features.snapshot.ui.adapter

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
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.view.SnapshotItemView
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.DateUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setAlphaForAll
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import java.text.SimpleDateFormat
import java.util.Locale

const val ARROW = "→"
const val ARROW_REVERT = "←"
class SnapshotAdapter(private val cardMode: CardMode = CardMode.NORMAL) : HighlightAdapter<SnapshotDiffItem>() {

  private val formatter by unsafeLazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
  }
  private val formatterToday by unsafeLazy {
    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
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
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
      } else {
        strokeColor = Color.TRANSPARENT
        radius = 0f
      }
    }
    (holder.itemView as SnapshotItemView).container.apply {
      val packageInfo = runCatching {
        PackageUtils.getPackageInfo(item.packageName)
      }.getOrNull()

      if (packageInfo == null) {
        icon.load(R.drawable.ic_icon_blueprint)
      } else {
        icon.load(packageInfo)
      }

      if (item.deleted) {
        setAlphaForAll(0.7f)
      } else {
        setAlphaForAll(1.0f)
      }

      val isNewOrDeleted = item.deleted || item.newInstalled
      val highlightDiffColor = if ((GlobalValues.snapshotOptions and SnapshotOptions.DIFF_HIGHLIGHT) > 0) {
        context.getColorByAttr(com.google.android.material.R.attr.colorPrimary)
      } else {
        null
      }

      stateIndicator.apply {
        added = item.added && !isNewOrDeleted
        removed = item.removed && !isNewOrDeleted
        changed = item.changed && !isNewOrDeleted
        moved = item.moved && !isNewOrDeleted
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
              append(if (diffSize > 0) "+" else "")
              append(diffSize.sizeToString(context))
            }

            if (diffSize != 0L) {
              append(", $diffSizeText")
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

      val oldAbiString = PackageUtils.getAbiString(context, item.abiDiff.old.toInt(), false)
      val oldAbiSpanString: SpannableString
      var abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abiDiff.old.toInt())
      if (item.abiDiff.old.toInt() != Constants.ERROR && item.abiDiff.old.toInt() != Constants.OVERLAY && abiBadgeRes != 0) {
        var oldPaddingString = "  $oldAbiString"
        if (item.abiDiff.old / Constants.MULTI_ARCH == 1) {
          oldPaddingString = "  $oldPaddingString"
        }
        oldAbiSpanString = SpannableString(oldPaddingString)
        abiBadgeRes.getDrawable(context)?.let {
          it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          val span = CenterAlignImageSpan(it)
          oldAbiSpanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
        }
        if (item.abiDiff.old / Constants.MULTI_ARCH == 1) {
          R.drawable.ic_multi_arch.getDrawable(context)?.let {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            val span = CenterAlignImageSpan(it)
            oldAbiSpanString.setSpan(span, 2, 3, ImageSpan.ALIGN_BOTTOM)
          }
        }
      } else {
        oldAbiSpanString = SpannableString(oldAbiString)
      }
      val builder = SpannableStringBuilder(oldAbiSpanString)

      val newAbiSpanString: SpannableString
      if (item.abiDiff.new != null) {
        val newAbiString =
          PackageUtils.getAbiString(context, item.abiDiff.new.toInt(), false)
        abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abiDiff.new.toInt())
        if (item.abiDiff.new.toInt() != Constants.ERROR && item.abiDiff.new.toInt() != Constants.OVERLAY && abiBadgeRes != 0) {
          var newPaddingString = "  $newAbiString"
          if (item.abiDiff.new / Constants.MULTI_ARCH == 1) {
            newPaddingString = "  $newPaddingString"
          }
          newAbiSpanString = SpannableString(newPaddingString)
          abiBadgeRes.getDrawable(context)?.let {
            if ((GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) > 0) {
              if (abiBadgeRes == R.drawable.ic_abi_label_64bit) {
                it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorPrimary))
              } else {
                it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorTertiary))
              }
            }
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            val span = CenterAlignImageSpan(it)
            newAbiSpanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
          }
          if (item.abiDiff.new / Constants.MULTI_ARCH == 1) {
            R.drawable.ic_multi_arch.getDrawable(context)?.let {
              it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
              val span = CenterAlignImageSpan(it)
              newAbiSpanString.setSpan(span, 2, 3, ImageSpan.ALIGN_BOTTOM)
            }
          }
        } else {
          newAbiSpanString = SpannableString(newAbiString)
        }
      } else {
        newAbiSpanString = SpannableString("")
      }

      if (item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new) {
        builder.append(" $ARROW ").append(newAbiSpanString)
      }
      abiInfo.text = builder

      updateTime.isVisible = (GlobalValues.snapshotOptions and SnapshotOptions.SHOW_UPDATE_TIME) > 0 && cardMode != CardMode.GET_APP_UPDATE
      if (updateTime.isVisible) {
        val timeText = if (DateUtils.isTimestampToday(item.updateTime)) {
          formatterToday.format(item.updateTime)
        } else {
          formatter.format(item.updateTime)
        }
        updateTime.text = if (item.updateTime <= PREINSTALLED_TIMESTAMP) {
          context.getString(R.string.snapshot_preinstalled_app)
        } else {
          context.getString(R.string.format_last_updated).format(timeText)
        }
        if (OsUtils.atLeastQ() && LocalAppDataSource.apexPackageSet.contains(item.packageName)) {
          updateTime.append(", APEX")
        }
      }
    }
  }

  enum class CardMode {
    NORMAL,
    DEMO,
    GET_APP_UPDATE
  }
}
