package com.absinthe.libchecker.domain.app.list.ui

import android.content.DialogInterface
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuAction
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuBottomSheetState
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuBSDView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AdvancedMenuBSDView>() {

  private var previousAdvancedOptions = 0
  private var previousItemAdvancedOptions = 0
  private var currentAdvancedOptions = 0
  private var currentItemAdvancedOptions = 0

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

  override fun initRootView(): AdvancedMenuBSDView = AdvancedMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    render()
    dialog?.setOnDismissListener {
      onDismissCallback?.invoke(
        previousAdvancedOptions.xor(currentAdvancedOptions),
        previousItemAdvancedOptions.xor(currentItemAdvancedOptions)
      )
    }
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

  private fun render() {
    root.bind(
      state = buildAdvancedMenuBottomSheetState(
        displayOptions = currentAdvancedOptions,
        itemDisplayOptions = currentItemAdvancedOptions,
        colorfulRuleIcon = currentColorfulRuleIcon,
        rulePackageName = requireContext().packageName
      ),
      onAction = ::handleAction
    )
  }

  private fun handleAction(action: AdvancedMenuAction) {
    when (action) {
      is AdvancedMenuAction.SortChanged -> {
        setAdvancedOptions(action.displayOptions)
      }

      is AdvancedMenuAction.DisplayOptionChanged -> {
        setAdvancedOptions(
          currentAdvancedOptions.withOption(action.item.option, action.isChecked)
        )
      }

      is AdvancedMenuAction.ItemDisplayOptionChanged -> {
        currentItemAdvancedOptions = currentItemAdvancedOptions.withOption(
          action.item.option,
          action.isChecked
        )
        currentItemAdvancedOptions = onItemDisplayOptionsChanged(currentItemAdvancedOptions)
        Telemetry.recordEvent(
          Constants.Event.APP_LIST_ADVANCED_MENU_ITEM_CHANGED,
          mapOf(
            Telemetry.Param.CONTENT to getString(action.item.labelRes),
            Telemetry.Param.VALUE to action.isChecked
          )
        )
      }
    }
    render()
  }

  private fun setAdvancedOptions(options: Int) {
    currentAdvancedOptions = onDisplayOptionsChanged(options)
  }
}

private fun Int.withOption(option: Int, isEnabled: Boolean): Int {
  return if (isEnabled) {
    this or option
  } else {
    this and option.inv()
  }
}
