package com.absinthe.libchecker.recyclerview.adapter

import android.content.pm.PackageInfo
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.AdvancedOptions
import com.absinthe.libchecker.constant.AndroidVersions
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.addStrikeThroughSpan
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDimensionPixelSize
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.view.applist.AppItemView
import com.absinthe.libchecker.view.detail.CenterAlignImageSpan
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter(private val cardMode: CardMode = CardMode.NORMAL) : HighlightAdapter<LCItem>() {

  override fun onCreateDefViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
    return createBaseViewHolder(
      AppItemView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          val margin = context.getDimensionPixelSize(R.dimen.main_card_margin)
          it.setMargins(0, margin, 0, margin)
        }
      }
    )
  }

  override fun convert(holder: BaseViewHolder, item: LCItem) {
    if (cardMode == CardMode.DEMO) {
      (holder.itemView as AppItemView).apply {
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutline)
        setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSecondaryContainer))
      }
    }
    (holder.itemView as AppItemView).container.apply {
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
        .append(getBuildVersionsInfo(packageInfo, item.packageName))
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

  private fun getBuildVersionsInfo(packageInfo: PackageInfo?, packageName: String): CharSequence {
    if (packageInfo == null && packageName != Constants.EXAMPLE_PACKAGE) {
      return ""
    }
    val showAndroidVersion =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0
    val showTarget =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0
    val showMin =
      (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0
    val target = packageInfo?.applicationInfo?.targetSdkVersion ?: Build.VERSION.SDK_INT
    val min = packageInfo?.applicationInfo?.minSdkVersion ?: Build.VERSION.SDK_INT

    return buildSpannedString {
      if (showTarget) {
        append(", ")
        scale(0.8f) {
          append("Target: ")
        }
        append(target.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[target]})")
        }
      }

      if (showMin) {
        if (showTarget) {
          append(", ")
        }
        scale(0.8f) {
          append(" Min: ")
        }
        append(min.toString())
        if (showAndroidVersion) {
          append(" (${AndroidVersions.simpleVersions[min]})")
        }
      }
    }
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }
}
