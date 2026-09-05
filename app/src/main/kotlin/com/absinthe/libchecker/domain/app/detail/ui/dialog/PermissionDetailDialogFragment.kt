package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.model.PermissionDetailBottomSheetState
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.PermissionInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

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
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
      val state = try {
        PermissionDetailBottomSheetState.Content(
          viewModel.getPermissionDetail(origPermName)
        )
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        Timber.e(exception)
        PermissionDetailBottomSheetState.Unavailable(
          origPermName,
          notFound = exception is PackageManager.NameNotFoundException
        )
      }
      root.bind(state)
    }
  }

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
