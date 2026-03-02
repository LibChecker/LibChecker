package com.absinthe.libchecker.features.snapshot.ui

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.features.snapshot.ui.view.SnapshotMenuBSDView
import com.absinthe.libchecker.features.snapshot.ui.view.SnapshotMenuItemView
import com.absinthe.libchecker.utils.extensions.supportIECUnit
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class SnapshotMenuBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotMenuBSDView>() {

  private val previousAdvancedOptions = GlobalValues.snapshotOptions
  private val optionsViewMap = mutableMapOf<Int, SnapshotMenuItemView>()

  private var onDismissCallback: (optionsDiff: Int) -> Unit = {}

  override fun initRootView(): SnapshotMenuBSDView = SnapshotMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    optionsViewMap[SnapshotOptions.SHOW_UPDATE_TIME] = root.addOptionItemView(R.string.snapshot_menu_show_update_time, SnapshotOptions.SHOW_UPDATE_TIME)
    optionsViewMap[SnapshotOptions.HIDE_NO_COMPONENT_CHANGES] = root.addOptionItemView(R.string.snapshot_menu_hide_no_component_changes, SnapshotOptions.HIDE_NO_COMPONENT_CHANGES)
    optionsViewMap[SnapshotOptions.DIFF_HIGHLIGHT] = root.addOptionItemView(R.string.snapshot_menu_diff_highlight, SnapshotOptions.DIFF_HIGHLIGHT)
    if (supportIECUnit) {
      optionsViewMap[SnapshotOptions.USE_IEC_UNITS] =
        root.addOptionItemView(R.string.snapshot_menu_use_iec_units, SnapshotOptions.USE_IEC_UNITS)
    }

    optionsViewMap[SnapshotOptions.SHOW_UPDATE_TIME]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[SnapshotOptions.HIDE_NO_COMPONENT_CHANGES]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[SnapshotOptions.DIFF_HIGHLIGHT]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }
    optionsViewMap[SnapshotOptions.USE_IEC_UNITS]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }

    dialog?.setOnDismissListener {
      onDismissCallback(previousAdvancedOptions.xor(GlobalValues.snapshotOptions))
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
}
