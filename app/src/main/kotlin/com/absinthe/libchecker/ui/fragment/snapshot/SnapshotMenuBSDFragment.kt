package com.absinthe.libchecker.ui.fragment.snapshot

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.SnapshotOptions
import com.absinthe.libchecker.view.applist.AdvancedMenuItemView
import com.absinthe.libchecker.view.snapshot.SnapshotMenuBSDView
import com.absinthe.libchecker.view.snapshot.SnapshotMenuItemView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class SnapshotMenuBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotMenuBSDView>() {

  private val previousAdvancedOptions = GlobalValues.snapshotOptions
  private val optionsViewMap = mutableMapOf<Int, SnapshotMenuItemView>()

  private var onDismissCallback: () -> Unit = {}

  override fun initRootView(): SnapshotMenuBSDView = SnapshotMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.8).toInt()
    }
    optionsViewMap[SnapshotOptions.SHOW_UPDATE_TIME] = root.addOptionItemView(R.string.snapshot_menu_show_update_time, SnapshotOptions.SHOW_UPDATE_TIME)

    optionsViewMap[SnapshotOptions.SHOW_UPDATE_TIME]?.setOnCheckedChangeCallback {
      root.updateDemoView()
    }

    dialog?.setOnDismissListener {
      if (GlobalValues.snapshotOptions != previousAdvancedOptions) {
        onDismissCallback()
      }
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

  fun setOnDismissListener(action: () -> Unit) {
    onDismissCallback = action
  }
}
