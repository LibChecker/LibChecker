package com.absinthe.libchecker.ui.fragment.detail

import android.view.ViewGroup
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.detail.XmlBottomSheetView

const val EXTRA_TEXT = "text"

class XmlBSDFragment : BaseBottomSheetViewDialogFragment<XmlBottomSheetView>() {

  private val text by lazy { arguments?.getCharSequence(EXTRA_TEXT) }

  override fun initRootView(): XmlBottomSheetView = XmlBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(24.dp, 16.dp, 24.dp, 0)
      post {
        maxPeekSize = ((dialog?.window?.decorView?.height ?: 0) * 0.67).toInt()
      }
    }
    root.setText(text)
  }
}
