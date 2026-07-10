package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.PermissionInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_ORIG_PERM_NAME = "EXTRA_ORIG_PERM_NAME"

class PermissionDetailDialogFragment : BaseBottomSheetViewDialogFragment<PermissionInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val origPermName by lazy {
    arguments?.getString(EXTRA_ORIG_PERM_NAME).orEmpty()
      .substringBefore(" ") // remove maxSdkVersion suffix
  }

  override fun initRootView(): PermissionInfoBottomSheetView = PermissionInfoBottomSheetView(requireContext())

  override fun init() {
    root.bind(PermissionDetailBottomSheetState.Loading(origPermName))

    lifecycleScope.launch {
      root.bind(
        PermissionDetailBottomSheetState.Content(
          viewModel.getPermissionDetail(origPermName)
        )
      )
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun show(manager: FragmentManager, tag: String?) {
    if (!isShowing) {
      isShowing = true
      super.show(manager, tag)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    isShowing = false
  }

  companion object {
    fun newInstance(origPermName: String): PermissionDetailDialogFragment {
      return PermissionDetailDialogFragment().putArguments(EXTRA_ORIG_PERM_NAME to origPermName)
    }

    var isShowing = false
  }
}
