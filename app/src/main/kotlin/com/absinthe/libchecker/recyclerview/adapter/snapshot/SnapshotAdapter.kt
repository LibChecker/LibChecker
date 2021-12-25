package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.utils.AppIconCache
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColor
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.toColorStateList
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.absinthe.libchecker.view.snapshot.SnapshotItemView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val ARROW = "→"

class SnapshotAdapter(val lifecycleScope: LifecycleCoroutineScope) :
  BaseQuickAdapter<SnapshotDiffItem, BaseViewHolder>(0) {

  private var loadIconJob: Job? = null

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      SnapshotItemView(
        ContextThemeWrapper(
          context,
          R.style.AppListMaterialCard
        )
      ).also {
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
      icon.setTag(R.id.app_item_icon_id, item.packageName)
      lifecycleScope.launch(Dispatchers.IO) {
        try {
          val ai = PackageUtils.getPackageInfo(
            item.packageName,
            PackageManager.GET_META_DATA
          ).applicationInfo
          loadIconJob =
            AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, icon)
        } catch (e: PackageManager.NameNotFoundException) {
          val bitmap = R.drawable.ic_app_list.getDrawable(context)?.apply {
            setTint(R.color.textNormal.getColor(context))
          }?.toBitmap(40.dp, 40.dp)
          icon.post { icon.setImageBitmap(bitmap) }
        }
      }

      if (item.deleted) {
        addRedMask()
      } else {
        removeRedMask()
      }

      var isNewOrDeleted = false

      when {
        item.deleted -> {
          holder.itemView.backgroundTintList = R.color.material_red_300.toColorStateList(context)
          versionInfo.setTextColor(R.color.textNormal.getColor(context))
          targetApiInfo.setTextColor(R.color.textNormal.getColor(context))
          abiInfo.setTextColor(R.color.textNormal.getColor(context))
          isNewOrDeleted = true
        }
        item.newInstalled -> {
          holder.itemView.backgroundTintList = R.color.material_green_300.toColorStateList(context)
          versionInfo.setTextColor(R.color.textNormal.getColor(context))
          targetApiInfo.setTextColor(R.color.textNormal.getColor(context))
          abiInfo.setTextColor(R.color.textNormal.getColor(context))
          isNewOrDeleted = true
        }
        else -> {
          holder.itemView.backgroundTintList = null
          versionInfo.setTextColor(android.R.color.darker_gray.getColor(context))
          targetApiInfo.setTextColor(android.R.color.darker_gray.getColor(context))
          abiInfo.setTextColor(android.R.color.darker_gray.getColor(context))
        }
      }

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

      packageName.text = item.packageName
      versionInfo.text =
        getDiffString(item.versionNameDiff, item.versionCodeDiff, isNewOrDeleted, "%s (%s)")
      targetApiInfo.text = getDiffString(item.targetApiDiff, isNewOrDeleted, "API %s")

      val oldAbiString = PackageUtils.getAbiString(context, item.abiDiff.old.toInt(), true)
      val oldAbiSpanString: SpannableString
      var abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abiDiff.old.toInt())
      if (item.abiDiff.old.toInt() != Constants.ERROR && item.abiDiff.old.toInt() != Constants.OVERLAY && abiBadgeRes != 0) {
        oldAbiSpanString = SpannableString("  $oldAbiString")
        abiBadgeRes.getDrawable(context)?.let {
          it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          val span = CenterAlignImageSpan(it)
          oldAbiSpanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
        }
      } else {
        oldAbiSpanString = SpannableString(oldAbiString)
      }
      val builder = SpannableStringBuilder(oldAbiSpanString)

      val newAbiSpanString: SpannableString
      if (item.abiDiff.new != null) {
        val newAbiString =
          PackageUtils.getAbiString(context, item.abiDiff.new.toInt(), true)
        abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abiDiff.new.toInt())
        if (item.abiDiff.new.toInt() != Constants.ERROR && item.abiDiff.new.toInt() != Constants.OVERLAY && abiBadgeRes != 0) {
          newAbiSpanString = SpannableString("  $newAbiString")
          abiBadgeRes.getDrawable(context)?.let {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            val span = CenterAlignImageSpan(it)
            newAbiSpanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
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

  fun release() {
    if (loadIconJob?.isActive == true) {
      loadIconJob?.cancel()
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
