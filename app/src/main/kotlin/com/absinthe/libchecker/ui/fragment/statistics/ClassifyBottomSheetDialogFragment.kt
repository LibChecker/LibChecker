package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.statistics.ClassifyDialogView
import com.absinthe.libchecker.viewmodel.ChartViewModel

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private val viewModel: ChartViewModel by activityViewModels()
  private var mListener: OnDismissListener? = null

  override fun initRootView(): ClassifyDialogView =
    ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
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
