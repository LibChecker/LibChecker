package com.absinthe.libchecker.ui.base

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.features.home.HomeViewModel
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onResume(owner: LifecycleOwner) {
        scheduleAppbarLiftingStatus(getBorderViewDelegate()?.isShowingTopBorder == false)
      }

      override fun onStop(owner: LifecycleOwner) {
        if (this == homeViewModel.controller) {
          homeViewModel.controller = null
        }
      }
    })
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      if (this != homeViewModel.controller) {
        homeViewModel.controller = this
      }
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
