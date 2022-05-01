package com.absinthe.libchecker.ui.fragment

import android.view.Menu
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.base.BaseFragment
import com.absinthe.libchecker.viewmodel.HomeViewModel
import rikka.material.app.AppBar
import rikka.widget.borderview.BorderViewDelegate

abstract class BaseListControllerFragment<T : ViewBinding> : BaseFragment<T>(), IListController {

  protected var borderDelegate: BorderViewDelegate? = null
  protected val homeViewModel: HomeViewModel by activityViewModels()
  protected var isListReady = false
  protected var allowRefreshing = true
  protected var menu: Menu? = null
  protected var lastPackageChangedTime: Long = 0

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      if (this != homeViewModel.controller) {
        homeViewModel.controller = this
        activity?.invalidateOptionsMenu()
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

  protected fun isListCanScroll(listSize: Int): Boolean {
    if (context == null) {
      return false
    }
    getSuitableLayoutManager().apply {
      if (this is LinearLayoutManager) {
        return findFirstVisibleItemPosition() > 0 || findLastVisibleItemPosition() < listSize - 1
      } else if (this is StaggeredGridLayoutManager) {
        val firstLine = IntArray(4)
        findFirstVisibleItemPositions(firstLine)
        val lastLine = IntArray(4)
        findLastVisibleItemPositions(lastLine)
        return firstLine[0] > 0 || lastLine.last() < listSize - 1
      }
    }
    return false
  }

  protected fun hasPackageChanged(): Boolean {
    homeViewModel.workerBinder?.let {
      val serverLastPackageChangedTime = it.lastPackageChangedTime
      if (lastPackageChangedTime < serverLastPackageChangedTime) {
        lastPackageChangedTime = serverLastPackageChangedTime
        return true
      }
    }
    return false
  }
}
