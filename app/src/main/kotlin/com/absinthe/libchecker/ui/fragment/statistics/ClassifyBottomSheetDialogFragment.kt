package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.libchecker.view.statistics.ClassifyDialogView
import com.absinthe.libchecker.viewmodel.ChartViewModel

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private val viewModel: ChartViewModel by activityViewModels()
  private var mListener: OnDismissListener? = null

  override fun initRootView(): ClassifyDialogView =
    ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
    viewModel.dialogTitle.observe(viewLifecycleOwner) {
      getHeaderView().title.text = it
    }
    viewModel.filteredList.observe(viewLifecycleOwner) {
      root.adapter.setList(it)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    mListener?.onDismiss()
    mListener = null
  }

  fun setOnDismissListener(listener: OnDismissListener) {
    mListener = listener
  }

  interface OnDismissListener {
    fun onDismiss()
  }
}
