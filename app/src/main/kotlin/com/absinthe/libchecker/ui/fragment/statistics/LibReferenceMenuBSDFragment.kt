package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibReferenceOptions
import com.absinthe.libchecker.view.statistics.LibReferenceMenuBSDView
import com.absinthe.libchecker.view.statistics.LibReferenceMenuItemView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class LibReferenceMenuBSDFragment : BaseBottomSheetViewDialogFragment<LibReferenceMenuBSDView>() {

  private val previousAdvancedOptions = GlobalValues.libReferenceOptions
  private val optionsViewMap = mutableMapOf<Int, LibReferenceMenuItemView>()

  private var onDismissCallback: () -> Unit = {}

  override fun initRootView(): LibReferenceMenuBSDView = LibReferenceMenuBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.8).toInt()
    }
    optionsViewMap[LibReferenceOptions.NATIVE_LIBS] = root.addOptionItemView(R.string.ref_category_native, LibReferenceOptions.NATIVE_LIBS)
    optionsViewMap[LibReferenceOptions.SERVICES] = root.addOptionItemView(R.string.ref_category_service, LibReferenceOptions.SERVICES)
    optionsViewMap[LibReferenceOptions.ACTIVITIES] = root.addOptionItemView(R.string.ref_category_activity, LibReferenceOptions.ACTIVITIES)
    optionsViewMap[LibReferenceOptions.RECEIVERS] = root.addOptionItemView(R.string.ref_category_br, LibReferenceOptions.RECEIVERS)
    optionsViewMap[LibReferenceOptions.PROVIDERS] = root.addOptionItemView(R.string.ref_category_cp, LibReferenceOptions.PROVIDERS)
    optionsViewMap[LibReferenceOptions.PERMISSIONS] = root.addOptionItemView(R.string.ref_category_perm, LibReferenceOptions.PERMISSIONS)
    optionsViewMap[LibReferenceOptions.METADATA] = root.addOptionItemView(R.string.ref_category_metadata, LibReferenceOptions.METADATA)
    optionsViewMap[LibReferenceOptions.PACKAGES] = root.addOptionItemView(R.string.ref_category_package, LibReferenceOptions.PACKAGES)
    optionsViewMap[LibReferenceOptions.SHARED_UID] = root.addOptionItemView(R.string.ref_category_shared_uid, LibReferenceOptions.SHARED_UID)
    optionsViewMap[LibReferenceOptions.ONLY_NOT_MARKED] = root.addOptionItemView(R.string.ref_category_only_not_marked, LibReferenceOptions.ONLY_NOT_MARKED)

    dialog?.setOnDismissListener {
      if (GlobalValues.libReferenceOptions != previousAdvancedOptions) {
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
