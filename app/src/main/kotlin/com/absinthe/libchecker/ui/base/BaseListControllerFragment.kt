package com.absinthe.libchecker.ui.base

import android.animation.ValueAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.ui.animator.createReturnTopAnimator
import rikka.widget.borderview.BorderRecyclerView
import rikka.widget.borderview.BorderView
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
  private var returnTopAnimator: ValueAnimator? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    activity?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
      override fun onStop(owner: LifecycleOwner) {
        listControllerHost?.clearListController(this@BaseListControllerFragment)
      }
    })
  }

  override fun onVisibilityChanged(visible: Boolean) {
    if (!visible) cancelReturnTopAnimation()
    super.onVisibilityChanged(visible)
    if (visible) {
      listControllerHost?.setListController(this)
      scheduleAppbarLiftingStatus(getBorderViewDelegate()?.isShowingTopBorder == false)
    }
  }

  protected val listControllerHost: IListControllerHost?
    get() = activity as? IListControllerHost

  override fun getBorderViewDelegate(): BorderViewDelegate? = borderDelegate

  override fun isAllowRefreshing(): Boolean = allowRefreshing

  protected fun scheduleAppbarLiftingStatus(isLifted: Boolean) {
    (activity as? IAppBarContainer)?.scheduleAppbarLiftingStatus(isLifted)
  }

  protected fun animateReturnTop(recyclerView: RecyclerView): Boolean {
    if (returnTopAnimator != null) return true
    if (!recyclerView.canScrollVertically(-1)) return false
    val appbar = activity as? IAppBarContainer
    appbar?.setAppbarReturnTopRunning(true)
    returnTopAnimator = recyclerView.createReturnTopAnimator {
      returnTopAnimator = null
      appbar?.setAppbarReturnTopRunning(false)
    }
    returnTopAnimator?.start()
    return true
  }

  protected fun cancelReturnTopAnimation() {
    returnTopAnimator?.cancel()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    cancelReturnTopAnimation()
    super.onConfigurationChanged(newConfig)
  }

  override fun onDestroyView() {
    cancelReturnTopAnimation()
    super.onDestroyView()
  }

  protected fun wireListScreenChrome(recyclerView: BorderRecyclerView) {
    recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
      override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) cancelReturnTopAnimation()
      }
    })
    borderDelegate = recyclerView.borderViewDelegate
    recyclerView.borderVisibilityChangedListener =
      BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
        if (isFragmentVisible()) {
          scheduleAppbarLiftingStatus(!top)
        }
      }
  }

  protected fun createListScreenLayoutManager(configuration: Configuration): RecyclerView.LayoutManager {
    return when (configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())

      Configuration.ORIENTATION_LANDSCAPE ->
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

      else -> error("Wrong orientation at ${javaClass.simpleName}.")
    }
  }

  protected fun onListScreenVisibilityChanged(visible: Boolean, recyclerView: RecyclerView) {
    if (visible) {
      (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(recyclerView)
    }
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
}

internal data class InitialListSearchState(
  val query: String,
  val shouldExpand: Boolean
)

internal fun initialListSearchState(
  retainedQuery: String,
  toolbarState: HomeViewModel.ToolbarSearchMenuState
): InitialListSearchState {
  return InitialListSearchState(
    query = retainedQuery,
    shouldExpand = retainedQuery.isNotEmpty() || toolbarState.isExpanded
  )
}

internal fun shouldHandleListSearchQueryChange(
  lifecycleState: Lifecycle.State
): Boolean {
  return lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
}
