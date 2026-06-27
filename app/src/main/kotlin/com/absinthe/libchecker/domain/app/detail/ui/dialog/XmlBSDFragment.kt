package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.view.ViewGroup
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_TEXT
import com.absinthe.libchecker.domain.app.detail.ui.view.XmlBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

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
