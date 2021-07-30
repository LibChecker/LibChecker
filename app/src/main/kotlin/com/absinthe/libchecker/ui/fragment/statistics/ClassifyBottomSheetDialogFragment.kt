package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.statistics.ClassifyDialogView
import com.absinthe.libchecker.viewmodel.ChartViewModel

const val EXTRA_TITLE = "EXTRA_TITLE"

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private val viewModel: ChartViewModel by activityViewModels()
  private val dialogTitle by lazy { arguments?.getString(EXTRA_TITLE).orEmpty() }
  private var mListener: OnDismissListener? = null

  override fun initRootView(): ClassifyDialogView =
    ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    getHeaderView().title.text = dialogTitle
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
