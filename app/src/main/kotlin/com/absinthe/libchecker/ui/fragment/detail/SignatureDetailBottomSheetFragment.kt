package com.absinthe.libchecker.ui.fragment.detail

import android.content.DialogInterface
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.bean.SignatureDetailItem
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.detail.SignatureDetailBottomSheetView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.util.ClipboardUtils

const val EXTRA_SIGNATURE_NAME = "EXTRA_SIGNATURE_NAME"
const val EXTRA_SIGNATURE_DETAIL = "EXTRA_SIGNATURE_DETAIL"

class SignatureDetailBottomSheetDetailFragment :
  BaseBottomSheetViewDialogFragment<SignatureDetailBottomSheetView>() {

  private val detail by unsafeLazy { arguments?.getString(EXTRA_SIGNATURE_DETAIL).orEmpty() }

  override fun initRootView(): SignatureDetailBottomSheetView =
    SignatureDetailBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
    root.adapter.setOnItemLongClickListener { _, _, _ ->
      ClipboardUtils.put(requireContext(), detail)
      VersionCompat.showCopiedOnClipboardToast(requireContext())
      true
    }
    lifecycleScope.launch {
      val data = withContext(Dispatchers.IO) {
        detail.lines().map {
          val values = it.split(":", limit = 2)
          SignatureDetailItem(
            values.getOrNull(0).orEmpty(),
            values.getOrNull(1).orEmpty()
          )
        }.toMutableList()
      }
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
    fun newInstance(
      libName: String, detail: String
    ): SignatureDetailBottomSheetDetailFragment {
      return SignatureDetailBottomSheetDetailFragment().putArguments(
        EXTRA_SIGNATURE_NAME to libName,
        EXTRA_SIGNATURE_DETAIL to detail
      )
    }

    var isShowing = false
  }
}
