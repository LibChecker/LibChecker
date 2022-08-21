package com.absinthe.libchecker.ui.fragment.main

import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.AdvancedMenuItem
import com.absinthe.libchecker.constant.AdvancedOptions
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.view.applist.AdvancedMenuBSDView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AdvancedMenuBSDView>() {

  private val previousAdvancedOptions = GlobalValues.advancedOptions
  private var onDismissCallback: () -> Unit = {}

  override fun initRootView(): AdvancedMenuBSDView = AdvancedMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val list = listOf(
      AdvancedMenuItem(
        R.string.adv_show_system_apps,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_SYSTEM_APPS) > 0,
        AdvancedOptions.SHOW_SYSTEM_APPS
      ),
      AdvancedMenuItem(
        R.string.adv_show_overlays,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_OVERLAYS) > 0,
        AdvancedOptions.SHOW_OVERLAYS
      ),
      AdvancedMenuItem(
        R.string.adv_show_64_bit,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_64_BIT_APPS) > 0,
        AdvancedOptions.SHOW_64_BIT_APPS
      ),
      AdvancedMenuItem(
        R.string.adv_show_32_bit,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_32_BIT_APPS) > 0,
        AdvancedOptions.SHOW_32_BIT_APPS
      ),
      AdvancedMenuItem(
        R.string.adv_sort_by_name,
        (GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_NAME) > 0,
        AdvancedOptions.SORT_BY_NAME
      ),
      AdvancedMenuItem(
        R.string.adv_sort_by_time,
        (GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_UPDATE_TIME) > 0,
        AdvancedOptions.SORT_BY_UPDATE_TIME
      ),
      AdvancedMenuItem(
        R.string.adv_sort_by_target_version,
        (GlobalValues.advancedOptions and AdvancedOptions.SORT_BY_TARGET_API) > 0,
        AdvancedOptions.SORT_BY_TARGET_API
      ),
      AdvancedMenuItem(
        R.string.adv_show_android_version,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_ANDROID_VERSION) > 0,
        AdvancedOptions.SHOW_ANDROID_VERSION
      ),
      AdvancedMenuItem(
        R.string.adv_show_target_version,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_TARGET_API) > 0,
        AdvancedOptions.SHOW_TARGET_API
      ),
      AdvancedMenuItem(
        R.string.adv_show_min_version,
        (GlobalValues.advancedOptions and AdvancedOptions.SHOW_MIN_API) > 0,
        AdvancedOptions.SHOW_MIN_API
      ),
      AdvancedMenuItem(
        R.string.adv_tint_abi_label,
        (GlobalValues.advancedOptions and AdvancedOptions.TINT_ABI_LABEL) > 0,
        AdvancedOptions.TINT_ABI_LABEL
      ),
    )
    root.adapter.setList(list)

    dialog?.setOnDismissListener {
      if (GlobalValues.advancedOptions != previousAdvancedOptions) {
        onDismissCallback()
      }
    }
  }

  fun setOnDismissListener(action: () -> Unit) {
    onDismissCallback = action
  }
}
