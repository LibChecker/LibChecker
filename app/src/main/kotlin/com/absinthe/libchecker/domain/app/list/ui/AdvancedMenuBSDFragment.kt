package com.absinthe.libchecker.domain.app.list.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuBSDView
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuItemView
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuSection
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AdvancedMenuBSDView>() {

  private var previousAdvancedOptions = 0
  private var previousItemAdvancedOptions = 0
  private var currentAdvancedOptions = 0
  private var currentItemAdvancedOptions = 0
  private val optionsViewMap = mutableMapOf<Int, AdvancedMenuItemView>()
  private val itemOptionsViewMap = mutableMapOf<Int, AdvancedMenuItemView>()

  private var onDismissCallback: ((advancedDiff: Int, itemAdvancedDiff: Int) -> Unit)? = null
  private var currentColorfulRuleIcon = true
  private var onDisplayOptionsChanged: (Int) -> Int = {
    currentAdvancedOptions = it
    it
  }
  private var onItemDisplayOptionsChanged: (Int) -> Int = {
    currentItemAdvancedOptions = it
    it
  }

  override fun initRootView(): AdvancedMenuBSDView {
    return AdvancedMenuBSDView(
      context = requireContext(),
      displayOptions = currentAdvancedOptions,
      itemDisplayOptions = currentItemAdvancedOptions,
      colorfulRuleIcon = currentColorfulRuleIcon,
      onSortOptionsChanged = ::setAdvancedOptions
    )
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    addAdvancedOption(R.string.adv_show_system_apps, AdvancedOptions.SHOW_SYSTEM_APPS)
    addAdvancedOption(R.string.adv_show_system_framework_apps, AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS)
    addAdvancedOption(R.string.adv_show_overlays, AdvancedOptions.SHOW_OVERLAYS)
    addAdvancedOption(R.string.adv_show_64_bit, AdvancedOptions.SHOW_64_BIT_APPS)
    addAdvancedOption(R.string.adv_show_32_bit, AdvancedOptions.SHOW_32_BIT_APPS)
    addAdvancedOption(R.string.adv_show_android_version, AdvancedOptions.SHOW_ANDROID_VERSION, AdvancedMenuSection.View, refreshDemo = true)
    addAdvancedOption(R.string.adv_show_target_version, AdvancedOptions.SHOW_TARGET_API, AdvancedMenuSection.View, refreshDemo = true)
    addAdvancedOption(R.string.adv_show_min_version, AdvancedOptions.SHOW_MIN_API, AdvancedMenuSection.View, refreshDemo = true)
    addAdvancedOption(R.string.adv_show_compile_version, AdvancedOptions.SHOW_COMPILE_API, AdvancedMenuSection.View, refreshDemo = true)
    addAdvancedOption(R.string.adv_tint_abi_label, AdvancedOptions.TINT_ABI_LABEL, AdvancedMenuSection.View, refreshDemo = true)

    addItemOption(R.string.adv_mark_exported, AdvancedOptions.MARK_EXPORTED)
    addItemOption(R.string.adv_mark_disabled, AdvancedOptions.MARK_DISABLED)
    addItemOption(R.string.adv_show_marked_lib, AdvancedOptions.SHOW_MARKED_LIB)

    dialog?.setOnDismissListener {
      onDismissCallback?.invoke(
        previousAdvancedOptions.xor(currentAdvancedOptions),
        previousItemAdvancedOptions.xor(currentItemAdvancedOptions)
      )
    }
  }

  private fun addAdvancedOption(
    labelRes: Int,
    option: Int,
    section: AdvancedMenuSection = AdvancedMenuSection.Filter,
    refreshDemo: Boolean = false
  ) {
    optionsViewMap[option] = root.addOptionItemView(
      labelRes = labelRes,
      isChecked = currentAdvancedOptions.hasOption(option),
      section = section
    ) { isChecked ->
      setAdvancedOptions(currentAdvancedOptions.withOption(option, isChecked))
      if (refreshDemo) {
        root.updateDemoView(currentAdvancedOptions)
      }
    }
  }

  private fun addItemOption(labelRes: Int, option: Int) {
    itemOptionsViewMap[option] = root.addOptionItemViewForItem(
      labelRes = labelRes,
      isChecked = currentItemAdvancedOptions.hasOption(option)
    ) { isChecked ->
      currentItemAdvancedOptions = currentItemAdvancedOptions.withOption(option, isChecked)
      currentItemAdvancedOptions = onItemDisplayOptionsChanged(currentItemAdvancedOptions)
      root.updateItemDemoView(currentItemAdvancedOptions)
      Telemetry.recordEvent(
        Constants.Event.APP_LIST_ADVANCED_MENU_ITEM_CHANGED,
        mapOf(Telemetry.Param.CONTENT to getString(labelRes), Telemetry.Param.VALUE to isChecked)
      )
    }
  }

  private fun setAdvancedOptions(options: Int) {
    currentAdvancedOptions = onDisplayOptionsChanged(options)
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

  fun setOptionChangeListener(
    displayOptions: Int,
    itemDisplayOptions: Int,
    colorfulRuleIcon: Boolean,
    onDisplayOptionsChanged: (Int) -> Int,
    onItemDisplayOptionsChanged: (Int) -> Int
  ) {
    previousAdvancedOptions = displayOptions
    previousItemAdvancedOptions = itemDisplayOptions
    currentAdvancedOptions = displayOptions
    currentItemAdvancedOptions = itemDisplayOptions
    currentColorfulRuleIcon = colorfulRuleIcon
    this.onDisplayOptionsChanged = onDisplayOptionsChanged
    this.onItemDisplayOptionsChanged = onItemDisplayOptionsChanged
  }
}

private fun Int.hasOption(option: Int): Boolean = (this and option) > 0

private fun Int.withOption(option: Int, isEnabled: Boolean): Int {
  return if (isEnabled) {
    this or option
  } else {
    this and option.inv()
  }
}
