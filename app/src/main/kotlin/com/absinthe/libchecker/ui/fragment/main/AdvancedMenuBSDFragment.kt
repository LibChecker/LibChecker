package com.absinthe.libchecker.ui.fragment.main

import com.absinthe.libchecker.view.detail.AlternativeLaunchBSDView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView

class AdvancedMenuBSDFragment : BaseBottomSheetViewDialogFragment<AlternativeLaunchBSDView>() {

  override fun initRootView(): AlternativeLaunchBSDView =
    AlternativeLaunchBSDView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {

  }
}
