package com.absinthe.libchecker.features.chart.ui

import android.content.DialogInterface
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class ClassifyBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<ClassifyDialogView>() {

  private var onDismissAction: (() -> Unit)? = null
  private var _title: String? = null
  private var _androidVersionTriple: Triple<Int, String, Int?>? = null
  private var _list: List<LCItem>? = null

  override fun initRootView(): ClassifyDialogView = ClassifyDialogView(requireContext(), lifecycleScope)

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.post {
      _title?.let { setTitle(it) }
      _androidVersionTriple?.let { setAndroidVersionLabel(it) }
      _list?.let { setList(it) }
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    onDismissAction?.invoke()
    onDismissAction = null
  }

  fun setTitle(title: String) {
    _title = title
    runCatching {
      root.getHeaderView().title.text = title
    }
  }

  fun setAndroidVersionLabel(triple: Triple<Int, String, Int?>?) {
    _androidVersionTriple = triple
    runCatching {
      root.addAndroidVersionView(triple)
    }
  }

  fun setList(list: List<LCItem>) {
    _list = list
    runCatching {
      root.adapter.setList(list)
    }
  }

  fun setOnDismiss(action: () -> Unit) {
    onDismissAction = action
  }
}
