package com.absinthe.libchecker.domain.app.list.ui

import android.content.DialogInterface
import com.absinthe.libchecker.constant.options.withOption
import com.absinthe.libchecker.domain.app.list.model.AdvancedMenuAction
import com.absinthe.libchecker.domain.app.list.model.buildAdvancedMenuBottomSheetState
import com.absinthe.libchecker.domain.app.list.ui.view.AdvancedMenuBSDView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AdvancedMenuBSDView>() {

  private var previousAdvancedOptions = 0
  private var currentAdvancedOptions = 0

  private var onDismissCallback: ((advancedDiff: Int) -> Unit)? = null
  private var onDisplayOptionsChanged: (Int) -> Int = { options ->
    options
  }

  override fun initRootView(): AdvancedMenuBSDView = AdvancedMenuBSDView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    render()
    dialog?.setOnDismissListener {
      onDismissCallback?.invoke(
        previousAdvancedOptions.xor(currentAdvancedOptions)
      )
    }
  }

  override fun onCancel(dialog: DialogInterface) {
    super.onCancel(dialog)
    runCatching {
      dismiss()
    }
  }

  fun setOnDismissListener(action: (advancedDiff: Int) -> Unit) {
    onDismissCallback = action
  }

  fun setOptionChangeListener(
    displayOptions: Int,
    onDisplayOptionsChanged: (Int) -> Int
  ) {
    previousAdvancedOptions = displayOptions
    currentAdvancedOptions = displayOptions
    this.onDisplayOptionsChanged = onDisplayOptionsChanged
  }

  private fun render() {
    root.bind(
      state = buildAdvancedMenuBottomSheetState(
        displayOptions = currentAdvancedOptions
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
    }
    render()
  }

  private fun setAdvancedOptions(options: Int) {
    currentAdvancedOptions = onDisplayOptionsChanged(options)
  }
}
