package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.domain.app.detail.ui.view.SignatureDetailBottomSheetView
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import rikka.core.util.ClipboardUtils

const val EXTRA_SIGNATURE_DETAIL = "EXTRA_SIGNATURE_DETAIL"

class SignatureDetailBSDFragment : BaseBottomSheetViewDialogFragment<SignatureDetailBottomSheetView>() {

  private val detail by unsafeLazy { arguments?.getString(EXTRA_SIGNATURE_DETAIL).orEmpty() }
  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView(): SignatureDetailBottomSheetView = SignatureDetailBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.adapter.setOnItemLongClickListener { _, _, _ ->
      ClipboardUtils.put(requireContext(), detail)
      VersionCompat.showCopiedOnClipboardToast(requireContext())
      true
    }
    lifecycleScope.launch {
      val data = viewModel.buildSignatureDetailItems(detail).toMutableList()
      root.adapter.setNewInstance(data)
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
    fun newInstance(detail: String): SignatureDetailBSDFragment {
      return SignatureDetailBSDFragment().putArguments(
        EXTRA_SIGNATURE_DETAIL to detail
      )
    }

    var isShowing = false
  }
}
