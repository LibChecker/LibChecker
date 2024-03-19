package com.absinthe.libchecker.features.applist.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.features.applist.ui.view.AdvancedMenuBSDView
import com.absinthe.libchecker.features.applist.ui.view.AdvancedMenuItemView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AdvancedMenuBSDView>() {

  private val previousAdvancedOptions = GlobalValues.advancedOptions
  private val previousItemAdvancedOptions = GlobalValues.itemAdvancedOptions
  private val optionsViewMap = mutableMapOf<Int, AdvancedMenuItemView>()
  private val itemOptionsViewMap = mutableMapOf<Int, AdvancedMenuItemView>()

  private var onDismissCallback: ((advancedDiff: Int, itemAdvancedDiff: Int) -> Unit)? = null

  override fun initRootView(): AdvancedMenuBSDView = AdvancedMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    optionsViewMap[AdvancedOptions.SHOW_SYSTEM_APPS] = root.addOptionItemView(R.string.adv_show_system_apps, AdvancedOptions.SHOW_SYSTEM_APPS)
    optionsViewMap[AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS] = root.addOptionItemView(R.string.adv_show_system_framework_apps, AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS)
    optionsViewMap[AdvancedOptions.SHOW_OVERLAYS] = root.addOptionItemView(R.string.adv_show_overlays, AdvancedOptions.SHOW_OVERLAYS)
    optionsViewMap[AdvancedOptions.SHOW_64_BIT_APPS] = root.addOptionItemView(R.string.adv_show_64_bit, AdvancedOptions.SHOW_64_BIT_APPS)
    optionsViewMap[AdvancedOptions.SHOW_32_BIT_APPS] = root.addOptionItemView(R.string.adv_show_32_bit, AdvancedOptions.SHOW_32_BIT_APPS)
    optionsViewMap[AdvancedOptions.SHOW_ANDROID_VERSION] = root.addOptionItemView(R.string.adv_show_android_version, AdvancedOptions.SHOW_ANDROID_VERSION)
    optionsViewMap[AdvancedOptions.SHOW_TARGET_API] = root.addOptionItemView(R.string.adv_show_target_version, AdvancedOptions.SHOW_TARGET_API)
    optionsViewMap[AdvancedOptions.SHOW_MIN_API] = root.addOptionItemView(R.string.adv_show_min_version, AdvancedOptions.SHOW_MIN_API)
    optionsViewMap[AdvancedOptions.SHOW_COMPILE_API] = root.addOptionItemView(R.string.adv_show_compile_version, AdvancedOptions.SHOW_COMPILE_API)
    optionsViewMap[AdvancedOptions.TINT_ABI_LABEL] = root.addOptionItemView(R.string.adv_tint_abi_label, AdvancedOptions.TINT_ABI_LABEL)

    optionsViewMap[AdvancedOptions.SHOW_ANDROID_VERSION]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[AdvancedOptions.SHOW_TARGET_API]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[AdvancedOptions.SHOW_MIN_API]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[AdvancedOptions.SHOW_COMPILE_API]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[AdvancedOptions.TINT_ABI_LABEL]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }

    itemOptionsViewMap[AdvancedOptions.MARK_EXPORTED] =
      root.addOptionItemViewForItem(R.string.adv_mark_exported, AdvancedOptions.MARK_EXPORTED)
    itemOptionsViewMap[AdvancedOptions.MARK_DISABLED] =
      root.addOptionItemViewForItem(R.string.adv_mark_disabled, AdvancedOptions.MARK_DISABLED)
    itemOptionsViewMap[AdvancedOptions.SHOW_MARKED_LIB] =
      root.addOptionItemViewForItem(R.string.adv_show_marked_lib, AdvancedOptions.SHOW_MARKED_LIB)

    itemOptionsViewMap[AdvancedOptions.MARK_EXPORTED]?.setOnCheckedChangeCallback {
      root.updateItemDemoView()
    }
    itemOptionsViewMap[AdvancedOptions.MARK_DISABLED]?.setOnCheckedChangeCallback {
      root.updateItemDemoView()
    }
    itemOptionsViewMap[AdvancedOptions.SHOW_MARKED_LIB]?.setOnCheckedChangeCallback {
      root.updateItemDemoView()
    }

    dialog?.setOnDismissListener {
      onDismissCallback?.invoke(
        previousAdvancedOptions.xor(GlobalValues.advancedOptions),
        previousItemAdvancedOptions.xor(GlobalValues.itemAdvancedOptions)
      )
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    optionsViewMap.clear()
    itemOptionsViewMap.clear()
  }

  override fun onCancel(dialog: DialogInterface) {
    super.onCancel(dialog)
    runCatching {
      dismiss()
    }
  }

  fun setOnDismissListener(action: (advancedDiff: Int, itemAdvancedDiff: Int) -> Unit) {
    onDismissCallback = action
  }
}
