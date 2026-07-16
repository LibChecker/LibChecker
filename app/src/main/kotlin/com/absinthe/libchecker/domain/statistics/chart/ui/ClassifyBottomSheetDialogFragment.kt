package com.absinthe.libchecker.domain.statistics.chart.ui

import android.content.DialogInterface
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogAction
import com.absinthe.libchecker.domain.statistics.chart.model.ClassifyDialogState
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private var onDismissAction: (() -> Unit)? = null
  private var dialogState: ClassifyDialogState? = null
  private var isViewInitialized = false

  override fun initRootView(): ClassifyDialogView = ClassifyDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    isViewInitialized = true
    dialogState?.let(::render)
  }

  override fun onDestroyView() {
    isViewInitialized = false
    super.onDestroyView()
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    onDismissAction?.invoke()
    onDismissAction = null
  }

  fun setState(state: ClassifyDialogState) {
    dialogState = state
    if (isViewInitialized) {
      render(state)
    }
  }

  fun setOnDismiss(action: () -> Unit) {
    onDismissAction = action
  }

  private fun render(state: ClassifyDialogState) {
    root.bind(state, ::handleAction)
  }

  private fun handleAction(action: ClassifyDialogAction) {
    when (action) {
      is ClassifyDialogAction.OpenApp -> activity?.launchDetailPage(action.item)
    }
  }
}
