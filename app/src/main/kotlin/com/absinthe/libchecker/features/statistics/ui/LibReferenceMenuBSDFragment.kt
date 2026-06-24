package com.absinthe.libchecker.features.statistics.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.statistics.LibReferenceSettingsRepository
import com.absinthe.libchecker.features.statistics.ui.view.LibReferenceMenuBSDView
import com.absinthe.libchecker.features.statistics.ui.view.LibReferenceMenuItemView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import org.koin.android.ext.android.inject

class LibReferenceMenuBSDFragment : BaseBottomSheetViewDialogFragment<LibReferenceMenuBSDView>() {

  private val libReferenceSettingsRepository: LibReferenceSettingsRepository by inject()
  private val optionsViewMap = mutableMapOf<Int, LibReferenceMenuItemView>()
  private var previousAdvancedOptions: Int = 0

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}

  override fun initRootView(): LibReferenceMenuBSDView = LibReferenceMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    previousAdvancedOptions = libReferenceSettingsRepository.options
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
      onDismissCallback(previousAdvancedOptions.xor(libReferenceSettingsRepository.options))
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

  private fun addOptionItemView(labelRes: Int, option: Int) {
    optionsViewMap[option] =
      root.addOptionItemView(labelRes, option, previousAdvancedOptions).apply {
        setOnCheckedChangeCallback { isChecked ->
          updateOption(labelRes, option, isChecked)
        }
      }
  }

  private fun updateOption(labelRes: Int, option: Int, isChecked: Boolean) {
    val newOptions = if (isChecked) {
      libReferenceSettingsRepository.options or option
    } else {
      libReferenceSettingsRepository.options and option.inv()
    }
    libReferenceSettingsRepository.options = newOptions
    Telemetry.recordEvent(
      Constants.Event.LIB_REF_ADVANCED_MENU_ITEM_CHANGED,
      mapOf(Telemetry.Param.CONTENT to getString(labelRes), Telemetry.Param.VALUE to isChecked)
    )
  }
}
