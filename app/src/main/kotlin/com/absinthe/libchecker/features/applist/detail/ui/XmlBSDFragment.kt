package com.absinthe.libchecker.features.applist.detail.ui

import android.view.ViewGroup
import com.absinthe.libchecker.features.applist.detail.ui.view.XmlBottomSheetView
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

const val EXTRA_TEXT = "text"

class XmlBSDFragment : BaseBottomSheetViewDialogFragment<XmlBottomSheetView>() {

  private val text by lazy { arguments?.getCharSequence(EXTRA_TEXT) }

  override fun initRootView(): XmlBottomSheetView = XmlBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(24.dp, 16.dp, 24.dp, 0)
      setText(text)
    }
  }
}
