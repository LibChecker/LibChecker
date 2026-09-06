package com.absinthe.libchecker.domain.app.detail.ui.dialog

import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.ComponentMenuView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ComponentMenuDialogFragment : BaseBottomSheetViewDialogFragment<ComponentMenuView>() {
  private val viewModel: DetailViewModel by activityViewModel()

  override fun initRootView() = ComponentMenuView(requireContext())

  override fun init() {
    maxPeekHeightPercentage = 0.8f
    render()
  }

  private fun render() {
    root.bind(viewModel.getComponentMenuState(requireContext().packageName)) { item, checked ->
      viewModel.setComponentDisplayOption(item.option, checked)
      Telemetry.recordEvent(
        Constants.Event.APP_LIST_ADVANCED_MENU_ITEM_CHANGED,
        mapOf(
          Telemetry.Param.CONTENT to getString(item.labelRes),
          Telemetry.Param.VALUE to checked
        )
      )
      render()
    }
  }
}
