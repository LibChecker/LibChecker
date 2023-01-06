package com.absinthe.libchecker.ui.fragment.statistics

import android.content.DialogInterface
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.view.statistics.ClassifyDialogView
import com.absinthe.libchecker.viewmodel.ChartViewModel
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private val viewModel: ChartViewModel by activityViewModels()
  private var onDismissAction: (() -> Unit)? = null

  override fun initRootView(): ClassifyDialogView =
    ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
    viewModel.filteredList.observe(viewLifecycleOwner) {
      root.adapter.setList(it)
      if (it.isNotEmpty()) {
        getHeaderView().title.text = viewModel.dialogTitle.value
        root.addAndroidVersionView(viewModel.androidVersion.value)
      }
    }
    viewModel.dialogTitle.observe(viewLifecycleOwner) {
      if (viewModel.filteredList.value?.isNotEmpty() == true) {
        getHeaderView().title.text = it
      }
    }
    viewModel.androidVersion.observe(viewLifecycleOwner) {
      if (viewModel.filteredList.value?.isNotEmpty() == true) {
        root.addAndroidVersionView(it)
      }
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    onDismissAction?.invoke()
    onDismissAction = null
  }

  fun setOnDismiss(action: () -> Unit) {
    onDismissAction = action
  }
}
