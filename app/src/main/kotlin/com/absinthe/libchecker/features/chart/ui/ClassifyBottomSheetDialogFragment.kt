package com.absinthe.libchecker.features.chart.ui

import android.content.DialogInterface
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private var onDismissAction: (() -> Unit)? = null

  override fun initRootView(): ClassifyDialogView =
    ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.post {
      maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    onDismissAction?.invoke()
    onDismissAction = null
  }

  fun setTitle(title: String) {
    root.getHeaderView().title.text = title
  }

  fun setAndroidVersionLabel(triple: Triple<Int, String, Int?>?) {
    root.addAndroidVersionView(triple)
  }

  fun setList(list: List<LCItem>) {
    root.adapter.setList(list)
  }

  fun setOnDismiss(action: () -> Unit) {
    onDismissAction = action
  }
}
