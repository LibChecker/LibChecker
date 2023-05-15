package com.absinthe.libchecker.ui.fragment.statistics

import android.content.Intent
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LibReferenceOptions
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.model.LibReference
import com.absinthe.libchecker.recyclerview.adapter.statistics.LibReferenceAdapter
import com.absinthe.libchecker.recyclerview.diff.RefListDiffUtil
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.fragment.IAppBarContainer
import com.absinthe.libchecker.ui.fragment.applist.AdvancedMenuBSDFragment
import com.absinthe.libchecker.ui.main.ChartActivity
import com.absinthe.libchecker.ui.main.EXTRA_REF_LIST
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.ui.main.INavViewContainer
import com.absinthe.libchecker.ui.main.LibReferenceActivity
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.detail.EmptyListView
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class LibReferenceFragment :
  BaseListControllerFragment<FragmentLibReferenceBinding>(),
  SearchView.OnQueryTextListener {

  private val refAdapter = LibReferenceAdapter()
  private var delayShowNavigationJob: Job? = null
  private var firstScrollFlag = false
  private var isSearchTextClearOnce = false
  private var keyword: String = ""
  private var searchUpdateJob: Job? = null
  private var advancedMenuBSDFragment: LibReferenceMenuBSDFragment? = null

  override fun init() {
    val context = (context as? BaseActivity<*>) ?: return

    binding.apply {
      list.apply {
        adapter = refAdapter
        layoutManager = LinearLayoutManager(context)
        borderDelegate = borderViewDelegate
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            if (isResumed) {
              scheduleAppbarLiftingStatus(!top)
            }
          }
        FastScrollerBuilder(this).useMd2Style().build()
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
          override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dx == 0 && dy == 0) {
              // scrolled by dragging scrolling bar
              if (!firstScrollFlag) {
                firstScrollFlag = true
                return
              }
              if (delayShowNavigationJob?.isActive == true) {
                delayShowNavigationJob?.cancel()
                delayShowNavigationJob = null
              }
              if (isFragmentVisible() && !isSearchTextClearOnce && canListScroll(refAdapter.data.size)) {
                (activity as? INavViewContainer)?.hideNavigationView()
              }

              val position = when (layoutManager) {
                is LinearLayoutManager -> {
                  (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                }
                is StaggeredGridLayoutManager -> {
                  val counts = IntArray(4)
                  (layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(counts)
                  counts[0]
                }
                else -> {
                  0
                }
              }
              if (isFragmentVisible() && !isSearchTextClearOnce && position < refAdapter.itemCount - 1) {
                delayShowNavigationJob = lifecycleScope.launch(Dispatchers.IO) {
                  delay(400)
                  withContext(Dispatchers.Main) {
                    (activity as? INavViewContainer)?.showNavigationView()
                  }
                }
              }
              isSearchTextClearOnce = false
            }
          }
        })
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
        setOnDisplayedChildChangedListener {
          refAdapter.setSpaceFooterView()
        }
      }
    }

    refAdapter.apply {
      animationEnable = true
      setDiffCallback(RefListDiffUtil())
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        context.findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()

        val item = refAdapter.data[position] as? LibReference ?: return@setOnItemClickListener
        val intent = Intent(context, LibReferenceActivity::class.java)
          .putExtra(EXTRA_REF_NAME, item.libName)
          .putExtra(EXTRA_REF_TYPE, item.type)
          .putExtra(EXTRA_REF_LIST, item.referredList.toTypedArray())
        startActivity(intent)
      }
      setEmptyView(
        EmptyListView(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        }
      )
    }

    homeViewModel.apply {
      effect.onEach {
        when (it) {
          is HomeViewModel.Effect.PackageChanged -> {
            computeRef(false)
          }

          is HomeViewModel.Effect.UpdateLibRefProgress -> {
            binding.loadingView.progressIndicator.setProgressCompat(
              it.progress,
              it.progress > 0
            )
          }

          else -> {}
        }
      }.launchIn(lifecycleScope)
      libReference.onEach {
        if (it == null) {
          return@onEach
        }
        refAdapter.setList(it)

        flip(VF_LIST)
        refAdapter.setSpaceFooterView()
        isListReady = true
      }.launchIn(lifecycleScope)
    }
    GlobalValues.isShowSystemApps.observe(viewLifecycleOwner) {
      if (homeViewModel.libRefSystemApps == null || homeViewModel.libRefSystemApps != it) {
        computeRef(true)
        homeViewModel.libRefSystemApps = it
      }
    }
    GlobalValues.libReferenceThresholdLiveData.observe(viewLifecycleOwner) {
      if (it < homeViewModel.savedThreshold) {
        matchRules(true)
        homeViewModel.savedThreshold = it
      } else {
        homeViewModel.refreshRef()
      }
    }
    GlobalValues.isColorfulIcon.observe(viewLifecycleOwner) {
      // noinspection NotifyDataSetChanged
      refAdapter.notifyDataSetChanged()
    }

    lifecycleScope.launch {
      if (refAdapter.data.isEmpty()) {
        computeRef(true)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    (activity as? INavViewContainer)?.hideProgressBar()
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.lib_ref_menu, menu)
    this.menu = menu

    val context = (context as? BaseActivity<*>) ?: return
    val searchView = SearchView(context).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@LibReferenceFragment)
      queryHint = getText(R.string.search_hint)
      isQueryRefinementEnabled = true

      findViewById<View>(androidx.appcompat.R.id.search_plate).apply {
        setBackgroundColor(Color.TRANSPARENT)
      }
    }

    menu.findItem(R.id.search).apply {
      setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
      actionView = searchView

      if (!isListReady) {
        isVisible = false
      }
    }
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    val context = (context as? BaseActivity<*>) ?: return false
    if (menuItem.itemId == R.id.filter) {
      advancedMenuBSDFragment?.dismiss()
      advancedMenuBSDFragment = LibReferenceMenuBSDFragment().apply {
        setOnDismissListener {
          GlobalValues.libReferenceOptionsLiveData.postValue(GlobalValues.libReferenceOptions)
          refreshList()
          advancedMenuBSDFragment = null
        }
      }
      advancedMenuBSDFragment?.show(context.supportFragmentManager, AdvancedMenuBSDFragment::class.java.name)
    } else if (menuItem.itemId == R.id.chart) {
      startActivity(Intent(context, ChartActivity::class.java))
    }
    return true
  }

  private fun refreshList() {
    computeRef(true)
    Analytics.trackEvent(
      Constants.Event.LIB_REFERENCE_FILTER_TYPE,
      EventProperties().set(
        "Type",
        LibReferenceOptions.getOptionsString(GlobalValues.libReferenceOptions)
      )
    )
  }

  private fun computeRef(needShowLoading: Boolean) {
    isListReady = false
    if (needShowLoading) {
      flip(VF_LOADING)
    }
    homeViewModel.computeLibReference()
  }

  private fun matchRules(needShowLoading: Boolean) {
    isListReady = false
    if (needShowLoading) {
      flip(VF_LOADING)
    }
    homeViewModel.cancelMatchingJob()
    homeViewModel.matchingRules()
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (keyword != newText) {
      isSearchTextClearOnce = newText.isEmpty()
      keyword = newText

      searchUpdateJob?.cancel()
      searchUpdateJob = lifecycleScope.launch(Dispatchers.IO) {
        homeViewModel.savedRefList?.let { list ->
          val filter = list.filter {
            it.libName.contains(newText, ignoreCase = true) || it.chip?.name?.contains(
              newText,
              ignoreCase = true
            ) ?: false
          }
          LibReferenceAdapter.highlightText = newText

          withContext(Dispatchers.Main) {
            if (isFragmentVisible()) {
              (activity as? INavViewContainer)?.showProgressBar()
            }
            refAdapter.setDiffNewData(filter.toMutableList()) {
              doOnMainThreadIdle {
                //noinspection NotifyDataSetChanged
                refAdapter.notifyDataSetChanged()
              }
            }
            binding.list.post {
              (activity as? INavViewContainer)?.hideProgressBar()
            }
          }

          if (newText.equals("Easter Egg", true)) {
            context?.showToast("🥚")
            Analytics.trackEvent(
              Constants.Event.EASTER_EGG,
              EventProperties().set("EASTER_EGG", "Lib Reference Search")
            )
          }
        }
      }
    }
    return false
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      refAdapter.setSpaceFooterView()
    }
  }

  override fun onReturnTop() {
    binding.list.apply {
      if (canScrollVertically(-1)) {
        smoothScrollToPosition(0)
      }
    }
  }

  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = binding.list.layoutManager

  private fun flip(child: Int) {
    allowRefreshing = child == VF_LIST
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      menu?.findItem(R.id.search)?.isVisible = false
      binding.loadingView.loadingView.resumeAnimation()
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.loadingView.loadingView.pauseAnimation()
      binding.list.scrollToPosition(0)
    }

    binding.vfContainer.displayedChild = child
  }
}
