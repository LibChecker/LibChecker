package com.absinthe.libchecker.domain.app.list.ui.adapter

import android.content.Context
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.ViewGroup
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.app.AppListItemViewState
import com.absinthe.libchecker.domain.app.InstalledPackageState
import com.absinthe.libchecker.domain.app.list.ui.view.AppItemView
import com.absinthe.libchecker.domain.app.stableAppListItemIdForKey
import com.absinthe.libchecker.ui.adapter.HighlightAdapter
import com.absinthe.libchecker.utils.extensions.addStrikeThroughSpan
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getColorStateListByAttr
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.setSmoothRoundCorner
import com.absinthe.libchecker.view.span.CenterAlignImageSpan
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class AppAdapter(
  private val cardMode: CardMode = CardMode.NORMAL,
  private var fallbackDisplayOptions: Int = AdvancedOptions.DEFAULT_OPTIONS
) : HighlightAdapter<LCItem>() {

  private val itemViewStateCache = mutableMapOf<String, AppListItemViewState>()

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
      val viewState = getItemViewState(item)
      val packageInfo = viewState.packageInfo
      if (item.packageName != Constants.EXAMPLE_PACKAGE) {
        icon.load(packageInfo)
      }
      setOrHighlightText(appName, item.label)
      setOrHighlightText(packageName, item.packageName)

      if (viewState.isPackageMissing && cardMode != CardMode.DEMO) {
        appName.addStrikeThroughSpan()
        packageName.addStrikeThroughSpan()
      }

      versionInfo.text = viewState.versionInfo
      setDetachedAbiBadgeLayoutEnabled(viewState.useDetachedAbiBadges)

      if (viewState.useDetachedAbiBadges) {
        if (viewState.largeAbiBadgeRes != 0) {
          val abiBadge = viewState.largeAbiBadgeRes.getDrawable(context)?.mutate()?.apply {
            setTint(context.getAbiBadgeTint(viewState.isAbiBadge64Bit, viewState.tintAbiLabels))
          }
          val multiArchBadge = if (viewState.showMultiArchBadge) {
            R.drawable.ic_abi_label_multi_arch.getDrawable(context)?.mutate()?.apply {
              setTint(context.getMultiArchBadgeTint(viewState.tintAbiLabels))
            }
          } else {
            null
          }
          setAbiBadges(abiBadge, multiArchBadge)
        } else {
          setAbiBadges(null, null)
        }
        abiInfo.text = viewState.abiInfo
      } else {
        setAbiBadges(null, null)
        abiInfo.text = context.buildInlineAbiInfo(viewState)
      }

      when (viewState.packageBadge) {
        AppListItemViewState.PackageBadge.Harmony -> setBadge(R.drawable.ic_harmony_badge)
        AppListItemViewState.PackageBadge.Frozen -> setBadge(R.drawable.ic_disabled_package)
        null -> setBadge(null)
      }
      root.setItemContentDescription(
        item.label,
        item.packageName,
        versionInfo.text,
        viewState.accessibilityAbiInfo
      )
    }
  }

  override fun getItemId(position: Int): Long {
    if (data.isEmpty() || position >= data.size) {
      return super.getItemId(position)
    }
    return stableAppListItemIdForKey(data[position].packageName)
  }

  fun setItemViewStates(itemViewStates: Map<String, AppListItemViewState>) {
    itemViewStateCache.clear()
    itemViewStateCache.putAll(itemViewStates)
  }

  fun clearItemViewStateCache() {
    itemViewStateCache.clear()
  }

  fun setFallbackDisplayOptions(options: Int) {
    fallbackDisplayOptions = options
    clearItemViewStateCache()
  }

  private fun getItemViewState(item: LCItem): AppListItemViewState {
    return itemViewStateCache.getOrPut(item.packageName) {
      AppListItemViewState.create(
        context = context,
        item = item,
        packageState = InstalledPackageState(
          packageInfo = null,
          isFrozen = item.packageName != Constants.EXAMPLE_PACKAGE
        ),
        options = fallbackDisplayOptions
      )
    }
  }

  enum class CardMode {
    NORMAL,
    DEMO
  }
}

private fun Context.getAbiBadgeTint(isAbi64Bit: Boolean, tintAbiLabels: Boolean): Int {
  if (!tintAbiLabels) {
    return getColorByAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
  }
  return getColorByAttr(
    if (isAbi64Bit) {
      androidx.appcompat.R.attr.colorPrimary
    } else {
      com.google.android.material.R.attr.colorTertiary
    }
  )
}

private fun Context.buildInlineAbiInfo(viewState: AppListItemViewState): CharSequence {
  if (viewState.abiBadgeRes == 0) {
    return viewState.abiInfo
  }

  var paddingString = "  ${viewState.abiInfo}"
  if (viewState.showMultiArchBadge) {
    paddingString = "  $paddingString"
  }
  val spanString = SpannableString(paddingString)

  viewState.abiBadgeRes.getDrawable(this)?.mutate()?.let {
    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
    it.setTint(getAbiBadgeTint(viewState.isAbiBadge64Bit, viewState.tintAbiLabels))
    spanString.setSpan(CenterAlignImageSpan(it), 0, 1, ImageSpan.ALIGN_BOTTOM)
  }
  if (viewState.showMultiArchBadge) {
    R.drawable.ic_multi_arch.getDrawable(this)?.mutate()?.let {
      it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
      it.setTint(getMultiArchBadgeTint(viewState.tintAbiLabels))
      spanString.setSpan(CenterAlignImageSpan(it), 2, 3, ImageSpan.ALIGN_BOTTOM)
    }
  }

  return spanString
}

private fun Context.getMultiArchBadgeTint(tintAbiLabels: Boolean): Int {
  return getColorByAttr(
    if (tintAbiLabels) {
      com.google.android.material.R.attr.colorSecondary
    } else {
      com.google.android.material.R.attr.colorOnSurfaceVariant
    }
  )
}
