package com.absinthe.libchecker.domain.snapshot.list.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotMenuBSDView
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotMenuItemView
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.supportIECUnit
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SnapshotMenuBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotMenuBSDView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val optionsViewMap = mutableMapOf<Int, SnapshotMenuItemView>()
  private var previousAdvancedOptions: Int = 0

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}

  override fun initRootView(): SnapshotMenuBSDView = SnapshotMenuBSDView(
    requireContext(),
    buildSnapshotItemDisplayData
  )

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    previousAdvancedOptions = viewModel.getSnapshotOptions()
    addOptionItemView(R.string.snapshot_menu_show_update_time, SnapshotOptions.SHOW_UPDATE_TIME)
    addOptionItemView(R.string.snapshot_menu_hide_no_component_changes, SnapshotOptions.HIDE_NO_COMPONENT_CHANGES)
    addOptionItemView(R.string.snapshot_menu_diff_highlight, SnapshotOptions.DIFF_HIGHLIGHT)
    if (supportIECUnit) {
      addOptionItemView(R.string.snapshot_menu_use_iec_units, SnapshotOptions.USE_IEC_UNITS)
    }

    dialog?.setOnDismissListener {
      onDismissCallback(viewModel.getSnapshotOptionsDiff(previousAdvancedOptions))
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
          root.updateDemoView()
        }
      }
  }

  private fun updateOption(labelRes: Int, option: Int, isChecked: Boolean) {
    viewModel.setSnapshotOption(option, isChecked)
    Telemetry.recordEvent(
      Constants.Event.SNAPSHOT_ADVANCED_MENU_ITEM_CHANGED,
      mapOf(Telemetry.Param.CONTENT to getString(labelRes), Telemetry.Param.VALUE to isChecked)
    )
  }
}
