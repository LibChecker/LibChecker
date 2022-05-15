package com.absinthe.libchecker.recyclerview.adapter

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.AppIconCache
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.coroutines.Job
import timber.log.Timber

class AppAdapter(val lifecycleScope: LifecycleCoroutineScope) : HighlightAdapter<LCItem>() {

  private var loadIconJob: Job? = null

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      AppItemView(ContextThemeWrapper(context, R.style.AppListMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(margin, margin, margin, margin)
        }
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: LCItem) {
    (holder.itemView as AppItemView).container.apply {
      icon.setTag(R.id.app_item_icon_id, item.packageName)
      runCatching {
        val ai = PackageUtils.getPackageInfo(item.packageName).applicationInfo
        loadIconJob =
          AppIconCache.loadIconBitmapAsync(context, ai, ai.uid / 100000, icon)
      }.onFailure {
        Timber.e(it)
      }

      setOrHighlightText(appName, item.label)
      setOrHighlightText(packageName, item.packageName)

      versionInfo.text = PackageUtils.getVersionString(item.versionName, item.versionCode)

      val str = StringBuilder()
        .append(PackageUtils.getAbiString(context, item.abi.toInt(), false))
        .append(", ")
        .append(PackageUtils.getTargetApiString(item.targetApi))
      val spanString: SpannableString
      val abiBadgeRes = PackageUtils.getAbiBadgeResource(item.abi.toInt())

      if (item.abi.toInt() != Constants.OVERLAY && item.abi.toInt() != Constants.ERROR && abiBadgeRes != 0) {
        var paddingString = "  $str"
        if (item.abi / Constants.MULTI_ARCH == 1) {
          paddingString = "  $paddingString"
        }
        spanString = SpannableString(paddingString)

        abiBadgeRes.getDrawable(context)?.let {
          it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          val span = CenterAlignImageSpan(it)
          spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
        }
        if (item.abi / Constants.MULTI_ARCH == 1) {
          R.drawable.ic_multi_arch.getDrawable(context)?.let {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
            val span = CenterAlignImageSpan(it)
            spanString.setSpan(span, 2, 3, ImageSpan.ALIGN_BOTTOM)
          }
        }
        abiInfo.text = spanString
      } else {
        abiInfo.text = str
      }

      when {
        item.variant == Constants.VARIANT_HAP -> {
          setBadge(R.drawable.ic_harmony_badge)
        }
        FreezeUtils.isAppFrozen(item.packageName) -> {
          setBadge(R.drawable.ic_uninstalled_package)
        }
        else -> {
          setBadge(null)
        }
      }
    }
  }

  override fun getItemId(position: Int): Long {
    return data[position].hashCode().toLong()
  }

  fun release() {
    if (loadIconJob?.isActive == true) {
      loadIconJob?.cancel()
    }
  }
}
