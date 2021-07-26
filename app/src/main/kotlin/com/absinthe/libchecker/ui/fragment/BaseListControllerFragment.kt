package com.absinthe.libchecker.ui.fragment

import android.view.Menu
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.base.BaseFragment
import com.absinthe.libchecker.viewmodel.HomeViewModel
import rikka.material.app.AppBar
import rikka.widget.borderview.BorderViewDelegate

abstract class BaseListControllerFragment<T : ViewBinding> : BaseFragment<T>(), IListController {

  protected var borderDelegate: BorderViewDelegate? = null
  protected val homeViewModel by activityViewModels<HomeViewModel>()
  protected var isListReady = false
  protected var allowRefreshing = true
  protected var menu: Menu? = null

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      if (this != homeViewModel.controller) {
        homeViewModel.controller = this
        requireActivity().invalidateOptionsMenu()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    scheduleAppbarRaisingStatus()
  }

  override fun onDetach() {
    super.onDetach()
    if (this == homeViewModel.controller) {
      homeViewModel.controller = null
    }
  }

  override fun getAppBar(): AppBar? = (activity as? BaseActivity<*>)?.appBar

  override fun getBorderViewDelegate(): BorderViewDelegate? = borderDelegate

  override fun scheduleAppbarRaisingStatus() {
    getAppBar()?.setRaised(!(getBorderViewDelegate()?.isShowingTopBorder ?: true))
  }

  override fun isAllowRefreshing(): Boolean = allowRefreshing
}
