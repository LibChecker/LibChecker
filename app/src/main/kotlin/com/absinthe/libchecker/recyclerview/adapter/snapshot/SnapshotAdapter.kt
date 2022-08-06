package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.annotation.SuppressLint
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setAlphaForAll
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.view.snapshot.SnapshotItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

const val ARROW = "→"

class SnapshotAdapter : BaseQuickAdapter<SnapshotDiffItem, BaseViewHolder>(0) {

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

  @SuppressLint("SetTextI18n")
  override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
    (holder.itemView as SnapshotItemView).container.apply {
      val packageInfo = runCatching {
        AppItemRepository.allPackageInfoMap[item.packageName]
          ?: PackageUtils.getPackageInfo(item.packageName)
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

      stateIndicator.apply {
        added = item.added && !isNewOrDeleted
        removed = item.removed && !isNewOrDeleted
        changed = item.changed && !isNewOrDeleted
        moved = item.moved && !isNewOrDeleted
      }

      if (item.isTrackItem) {
        val imageSpan = ImageSpan(context, R.drawable.ic_track)
        val spannable = SpannableString(" ${getDiffString(item.labelDiff, isNewOrDeleted)}")
        spannable.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        appName.text = spannable
      } else {
        appName.text = getDiffString(item.labelDiff, isNewOrDeleted)
      }

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

      packageName.text = item.packageName
      versionInfo.text =
        getDiffString(item.versionNameDiff, item.versionCodeDiff, isNewOrDeleted, "%s (%s)")

      if (item.packageSizeDiff.old > 0L) {
        packageSizeInfo.isVisible = true
        val sizeDiff = SnapshotDiffItem.DiffNode(
          item.packageSizeDiff.old.sizeToString(context),
          item.packageSizeDiff.new?.sizeToString(context)
        )
        packageSizeInfo.text = getDiffString(sizeDiff, isNewOrDeleted)
      } else {
        packageSizeInfo.isGone = true
      }

      targetApiInfo.text = getDiffString(item.targetApiDiff, isNewOrDeleted, "API %s")

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
    }
  }

  private fun <T> getDiffString(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s"
  ): String {
    return if (diff.old != diff.new && !isNewOrDeleted) {
      "${format.format(diff.old)} $ARROW ${format.format(diff.new)}"
    } else {
      format.format(diff.old)
    }
  }

  private fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff2: SnapshotDiffItem.DiffNode<*>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s"
  ): String {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      "${format.format(diff1.old, diff2.old)} $ARROW ${format.format(diff1.new, diff2.new)}"
    } else {
      format.format(diff1.old, diff2.old)
    }
  }
}
