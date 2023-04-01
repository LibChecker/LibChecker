package com.absinthe.libchecker.ui.fragment

import android.view.Menu
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.viewmodel.HomeViewModel
import rikka.widget.borderview.BorderViewDelegate

abstract class BaseListControllerFragment<T : ViewBinding> :
  BaseFragment<T>(),
  IListController,
  MenuProvider {

  protected var borderDelegate: BorderViewDelegate? = null
  protected val homeViewModel: HomeViewModel by activityViewModels()
  protected var isListReady = false
  protected var allowRefreshing = true
  protected var menu: Menu? = null

  private var lastPackageChangedTime: Long = 0

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      if (this != homeViewModel.controller) {
        homeViewModel.controller = this
      }
      (activity as? IAppBarContainer)?.let { container ->
        if (container.currentMenuProvider != this) {
          container.currentMenuProvider?.let { current ->
            activity?.removeMenuProvider(current)
          }
          activity?.removeMenuProvider(this)
          activity?.addMenuProvider(this)
          container.currentMenuProvider = this
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    scheduleAppbarLiftingStatus(!(getBorderViewDelegate()?.isShowingTopBorder ?: true))
  }

  override fun onDetach() {
    super.onDetach()
    if (this == homeViewModel.controller) {
      homeViewModel.controller = null
    }
  }

  override fun getBorderViewDelegate(): BorderViewDelegate? = borderDelegate

  override fun isAllowRefreshing(): Boolean = allowRefreshing

  protected fun scheduleAppbarLiftingStatus(isLifted: Boolean) {
    (activity as? IAppBarContainer)?.scheduleAppbarLiftingStatus(isLifted)
  }

  protected fun canListScroll(listSize: Int): Boolean {
    if (context == null) {
      return false
    }
    getSuitableLayoutManager().apply {
      if (this is LinearLayoutManager) {
        if (findFirstVisibleItemPosition() == 0 && findLastVisibleItemPosition() == listSize - 1) {
          return false
        }
        return findFirstVisibleItemPosition() > 0 || findLastVisibleItemPosition() < listSize - 1
      } else if (this is StaggeredGridLayoutManager) {
        val firstLine = IntArray(4)
        findFirstVisibleItemPositions(firstLine)
        val lastLine = IntArray(4)
        findLastVisibleItemPositions(lastLine)
        if (firstLine[0] == 0 && lastLine.last() == listSize - 1) {
          return false
        }
        return firstLine[0] > 0 || lastLine.last() < listSize - 1
      }
    }
    return false
  }

  protected fun hasPackageChanged(): Boolean {
    homeViewModel.workerBinder?.let {
      val serverLastPackageChangedTime =
        runCatching { it.lastPackageChangedTime }.getOrElse { return false }
      if (lastPackageChangedTime < serverLastPackageChangedTime) {
        lastPackageChangedTime = serverLastPackageChangedTime
        return true
      }
    }
    return false
  }
}
