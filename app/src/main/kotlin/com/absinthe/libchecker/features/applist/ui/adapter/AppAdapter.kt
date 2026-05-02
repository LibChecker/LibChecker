package com.absinthe.libchecker.features.applist.ui.adapter

import android.content.Context
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
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
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
        setSmoothRoundCorner(20.dp)
        strokeColor = context.getColorByAttr(com.google.android.material.R.attr.colorOutlineVariant)
        setCardBackgroundColor(context.getColorStateListByAttr(com.google.android.material.R.attr.colorSurfaceContainerHigh))
      } else {
        radius = 0f
      }
    }
    root.container.apply {
      val packageInfo = if (item.packageName != Constants.EXAMPLE_PACKAGE) {
        val packageInfo = runCatching {
          PackageUtils.getPackageInfo(item.packageName, needAchieve = false)
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
      val abi = item.abi.toInt()
      val useDetachedAbiBadges = shouldUseDetachedAbiBadges()
      setDetachedAbiBadgeLayoutEnabled(useDetachedAbiBadges)

      if (useDetachedAbiBadges) {
        val abiBadgeRes = PackageUtils.getLargeAbiBadgeResource(abi)
        if (abi != Constants.OVERLAY && abi != Constants.ERROR && abiBadgeRes != 0) {
          val abiBadge = abiBadgeRes.getDrawable(context)?.mutate()?.apply {
            setTint(context.getAbiBadgeTint(abi % Constants.MULTI_ARCH))
          }
          val multiArchBadge = if (abi / Constants.MULTI_ARCH == 1) {
            R.drawable.ic_abi_label_multi_arch.getDrawable(context)?.mutate()?.apply {
              setTint(context.getMultiArchBadgeTint())
            }
          } else {
            null
          }
          setAbiBadges(abiBadge, multiArchBadge)
        } else {
          setAbiBadges(null, null)
        }
        abiInfo.text = str
      } else {
        setAbiBadges(null, null)
        abiInfo.text = context.buildInlineAbiInfo(abi, str)
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
    return data[position].packageName.hashCode().toLong()
  }

  override fun getItemViewType(position: Int): Int {
    if (data.isEmpty() || position >= data.size) {
      return super.getItemViewType(position)
    }
    return data[position].packageName.hashCode()
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }
}

private fun shouldUseDetachedAbiBadges(): Boolean {
  return listOf(
    AdvancedOptions.SHOW_ANDROID_VERSION,
    AdvancedOptions.SHOW_TARGET_API,
    AdvancedOptions.SHOW_MIN_API,
    AdvancedOptions.SHOW_COMPILE_API
  ).count { (GlobalValues.advancedOptions and it) > 0 } >= 4
}

private fun Context.getAbiBadgeTint(abi: Int): Int {
  if ((GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) == 0) {
    return getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
  }
  return getColorByAttr(
    if (PackageUtils.isAbi64Bit(abi)) {
      androidx.appcompat.R.attr.colorPrimary
    } else {
      com.google.android.material.R.attr.colorTertiary
    }
  )
}

private fun Context.buildInlineAbiInfo(abi: Int, text: CharSequence): CharSequence {
  val abiBadgeRes = PackageUtils.getAbiBadgeResource(abi)
  if (abi == Constants.OVERLAY || abi == Constants.ERROR || abiBadgeRes == 0) {
    return text
  }

  var paddingString = "  $text"
  if (abi / Constants.MULTI_ARCH == 1) {
    paddingString = "  $paddingString"
  }
  val spanString = SpannableString(paddingString)

  abiBadgeRes.getDrawable(this)?.mutate()?.let {
    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
    it.setTint(getAbiBadgeTint(abi % Constants.MULTI_ARCH))
    spanString.setSpan(CenterAlignImageSpan(it), 0, 1, ImageSpan.ALIGN_BOTTOM)
  }
  if (abi / Constants.MULTI_ARCH == 1) {
    R.drawable.ic_multi_arch.getDrawable(this)?.mutate()?.let {
      it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
      it.setTint(getMultiArchBadgeTint())
      spanString.setSpan(CenterAlignImageSpan(it), 2, 3, ImageSpan.ALIGN_BOTTOM)
    }
  }

  return spanString
}

private fun Context.getMultiArchBadgeTint(): Int {
  return getColorByAttr(
    if ((GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) > 0) {
      com.google.android.material.R.attr.colorSecondary
    } else {
      com.google.android.material.R.attr.colorOnSurfaceVariant
    }
  )
}
