package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.DialogInterface
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceMenuAction
import com.absinthe.libchecker.domain.statistics.reference.model.buildLibReferenceMenuBottomSheetState
import com.absinthe.libchecker.domain.statistics.reference.ui.view.LibReferenceMenuBSDView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class LibReferenceMenuBSDFragment : BaseBottomSheetViewDialogFragment<LibReferenceMenuBSDView>() {

  private var previousAdvancedOptions: Int = 0
  private var currentAdvancedOptions: Int = 0

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}
  private var onOptionChanged: (option: Int, isChecked: Boolean) -> Int = { option, isChecked ->
    if (isChecked) {
      currentAdvancedOptions or option
    } else {
      currentAdvancedOptions and option.inv()
    }
  }

  override fun initRootView(): LibReferenceMenuBSDView = LibReferenceMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    render()
    dialog?.setOnDismissListener {
      onDismissCallback(previousAdvancedOptions.xor(currentAdvancedOptions))
    }
  }

  override fun onCancel(dialog: DialogInterface) {
    super.onCancel(dialog)
    runCatching {
      dismiss()
    }
  }

  fun setOnDismissListener(action: (optionsDiff: Int) -> Unit) {
    onDismissCallback = action
  }

  fun setOptionChangeListener(
    initialOptions: Int,
    onOptionChanged: (option: Int, isChecked: Boolean) -> Int
  ) {
    previousAdvancedOptions = initialOptions
    currentAdvancedOptions = initialOptions
    this.onOptionChanged = onOptionChanged
  }

  private fun render() {
    root.bind(
      state = buildLibReferenceMenuBottomSheetState(currentAdvancedOptions),
      onAction = ::handleAction
    )
  }

  private fun handleAction(action: LibReferenceMenuAction) {
    when (action) {
      is LibReferenceMenuAction.OptionChanged -> {
        currentAdvancedOptions = onOptionChanged(action.item.option, action.isChecked)
        Telemetry.recordEvent(
          Constants.Event.LIB_REF_ADVANCED_MENU_ITEM_CHANGED,
          mapOf(
            Telemetry.Param.CONTENT to getString(action.item.labelRes),
            Telemetry.Param.VALUE to action.isChecked
          )
        )
        render()
      }
    }
  }
}
