package com.absinthe.libchecker.features.applist.ui.adapter

import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ViewGroup
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.features.applist.ui.view.AppItemView
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.addStrikeThroughSpan
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter(private val cardMode: CardMode = CardMode.NORMAL) : HighlightAdapter<LCItem>() {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      AppItemView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: LCItem) {
    val root = holder.itemView as AppItemView
    root.apply {
      if (cardMode == CardMode.DEMO) {
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
        setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
      } else {
        radius = 0f
      }
    }
    root.container.apply {
      val packageInfo = if (item.packageName != Constants.EXAMPLE_PACKAGE) {
        val packageInfo = runCatching {
          PackageUtils.getPackageInfo(item.packageName)
        }.getOrNull()
        icon.load(packageInfo)
        packageInfo
      } else {
        null
      }
      setOrHighlightText(appName, item.label)
      setOrHighlightText(packageName, item.packageName)

      if (packageInfo == null && cardMode != CardMode.DEMO) {
        appName.addStrikeThroughSpan()
        packageName.addStrikeThroughSpan()
      }

      versionInfo.text = PackageUtils.getVersionString(item.versionName, item.versionCode)

      val str = StringBuilder()
        .append(PackageUtils.getAbiString(context, item.abi.toInt(), false))
        .append(PackageUtils.getBuildVersionsInfo(packageInfo, item.packageName))
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
          if ((GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) > 0) {
            if (abiBadgeRes == R.drawable.ic_abi_label_64bit) {
              it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorPrimary))
            } else {
              it.setTint(context.getColorByAttr(com.google.android.material.R.attr.colorTertiary))
            }
          }
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
        item.packageName == Constants.EXAMPLE_PACKAGE -> {
          setBadge(null)
        }

        item.variant == Constants.VARIANT_HAP -> {
          setBadge(R.drawable.ic_harmony_badge)
        }

        FreezeUtils.isAppFrozen(item.packageName) -> {
          setBadge(R.drawable.ic_disabled_package)
        }

        else -> {
          setBadge(null)
        }
      }
    }
  }

  override fun getItemId(position: Int): Long {
    if (data.isEmpty() || position >= data.size) {
      return super.getItemId(position)
    }
    return data[position].hashCode().toLong()
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }
}
