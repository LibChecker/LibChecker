package com.absinthe.libchecker.features.statistics.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.features.statistics.ui.view.LibReferenceMenuBSDView
import com.absinthe.libchecker.features.statistics.ui.view.LibReferenceMenuItemView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class LibReferenceMenuBSDFragment : BaseBottomSheetViewDialogFragment<LibReferenceMenuBSDView>() {

  private val optionsViewMap = mutableMapOf<Int, LibReferenceMenuItemView>()
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
    addOptionItemView(R.string.ref_category_native, LibReferenceOptions.NATIVE_LIBS)
    addOptionItemView(R.string.ref_category_service, LibReferenceOptions.SERVICES)
    addOptionItemView(R.string.ref_category_activity, LibReferenceOptions.ACTIVITIES)
    addOptionItemView(R.string.ref_category_br, LibReferenceOptions.RECEIVERS)
    addOptionItemView(R.string.ref_category_cp, LibReferenceOptions.PROVIDERS)
    addOptionItemView(R.string.ref_category_action, LibReferenceOptions.ACTION)
    addOptionItemView(R.string.ref_category_perm, LibReferenceOptions.PERMISSIONS)
    addOptionItemView(R.string.ref_category_metadata, LibReferenceOptions.METADATA)
    addOptionItemView(R.string.ref_category_package, LibReferenceOptions.PACKAGES)
    addOptionItemView(R.string.ref_category_shared_uid, LibReferenceOptions.SHARED_UID)
    addOptionItemView(R.string.ref_category_only_not_marked, LibReferenceOptions.ONLY_NOT_MARKED)

    dialog?.setOnDismissListener {
      onDismissCallback(previousAdvancedOptions.xor(currentAdvancedOptions))
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    optionsViewMap.clear()
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

  private fun addOptionItemView(labelRes: Int, option: Int) {
    optionsViewMap[option] =
      root.addOptionItemView(labelRes, option, previousAdvancedOptions).apply {
        setOnCheckedChangeCallback { isChecked ->
          updateOption(labelRes, option, isChecked)
        }
      }
  }

  private fun updateOption(labelRes: Int, option: Int, isChecked: Boolean) {
    currentAdvancedOptions = onOptionChanged(option, isChecked)
    Telemetry.recordEvent(
      Constants.Event.LIB_REF_ADVANCED_MENU_ITEM_CHANGED,
      mapOf(Telemetry.Param.CONTENT to getString(labelRes), Telemetry.Param.VALUE to isChecked)
    )
  }
}
