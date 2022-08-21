package com.absinthe.libchecker.ui.fragment.applist

import android.content.res.Configuration
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.recyclerview.adapter.AppAdapter
import com.absinthe.libchecker.recyclerview.diff.AppListDiffUtil
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.fragment.IAppBarContainer
import com.absinthe.libchecker.ui.fragment.main.AdvancedMenuBSDFragment
import com.absinthe.libchecker.ui.main.INavViewContainer
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1
const val VF_INIT = 2

class AppListFragment :
  BaseListControllerFragment<FragmentAppListBinding>(),
  SearchView.OnQueryTextListener {

  private val appAdapter = AppAdapter()
  private var isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
  private var isFirstRequestChange = true
  private var delayShowNavigationJob: Job? = null
  private var firstScrollFlag = false
  private var keyword: String = ""

  private lateinit var layoutManager: RecyclerView.LayoutManager

  override fun init() {
    appAdapter.also {
      it.setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        activity?.let { a ->
          LCAppUtils.launchDetailPage(a, it.getItem(position))
        }
      }
      it.setDiffCallback(AppListDiffUtil())
      it.setHasStableIds(true)
    }

    binding.apply {
      list.apply {
        adapter = appAdapter
        borderDelegate = borderViewDelegate
        layoutManager = getSuitableLayoutManagerImpl()
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            if (isResumed) {
              scheduleAppbarLiftingStatus(!top)
            }
          }
        setHasFixedSize(true)
        FastScrollerBuilder(this).useMd2Style().build()
        addPaddingTop(UiUtils.getStatusBarHeight())
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
              if (canListScroll(appAdapter.data.size)) {
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
              if (position < appAdapter.itemCount - 1) {
                delayShowNavigationJob = lifecycleScope.launch(Dispatchers.IO) {
                  delay(400)
                  withContext(Dispatchers.Main) {
                    (activity as? INavViewContainer)?.showNavigationView()
                  }
                }.also {
                  it.start()
                }
              }
            }
          }
        })
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
      }
    }

    initObserver()
  }

  override fun onResume() {
    super.onResume()
    if (hasPackageChanged()) {
      homeViewModel.requestChange()
    }
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)
    if (homeViewModel.appListStatus == STATUS_START_INIT) {
      flip(VF_INIT)
      activity?.removeMenuProvider(this)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    binding.list.layoutManager = getSuitableLayoutManagerImpl()
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (keyword != newText) {
      keyword = newText
      homeViewModel.dbItems.value?.let { allDatabaseItems ->
        appAdapter.highlightText = newText
        updateItems(allDatabaseItems, highlightRefresh = true)

        when {
          newText.equals("Easter Egg", true) -> {
            context?.showToast("🥚")
            Analytics.trackEvent(
              Constants.Event.EASTER_EGG,
              EventProperties().set("EASTER_EGG", "AppList Search")
            )
          }
          newText == Constants.COMMAND_DEBUG_MODE -> {
            GlobalValues.debugMode = true
            context?.showToast("DEBUG MODE")
          }
          newText == Constants.COMMAND_USER_MODE -> {
            GlobalValues.debugMode = false
            context?.showToast("USER MODE")
          }
          else -> {
          }
        }
      }
    }
    return false
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.app_list_menu, menu)
    this.menu = menu

    val searchView = SearchView(requireContext()).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@AppListFragment)
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
    when (menuItem.itemId) {
      R.id.advanced -> {
        activity?.let {
          AdvancedMenuBSDFragment().apply {
            show(it.supportFragmentManager, tag)
            setOnDismissListener {
              //noinspection NotifyDataSetChanged
              appAdapter.notifyDataSetChanged()
            }
          }
        }
      }
    }
    return true
  }

  private fun initObserver() {
    homeViewModel.apply {
      lifecycleScope.launchWhenStarted {
        effect.collect {
          when (it) {
            is HomeViewModel.Effect.ReloadApps -> {
              if (appListStatus == STATUS_NOT_START) {
                Once.clearDone(OnceTag.FIRST_LAUNCH)
                isFirstLaunch = true
                doOnMainThreadIdle {
                  initApps()
                }
              }
            }
            is HomeViewModel.Effect.UpdateInitProgress -> {
              binding.initView.progressIndicator.setProgressCompat(it.progress, true)
            }
            is HomeViewModel.Effect.PackageChanged -> {
              requestChange(true)
            }
            is HomeViewModel.Effect.UpdateAppListStatus -> {
              Timber.d("AppList status updates to ${it.status}")
              when (it.status) {
                STATUS_START_INIT -> {
                  isListReady = false
                  flip(VF_INIT)
                  (activity as? INavViewContainer)?.hideNavigationView()
                }
                STATUS_INIT_END -> {
                  if (isFirstLaunch) {
                    Once.markDone(OnceTag.FIRST_LAUNCH)
                    Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
                  }
                  (activity as? INavViewContainer)?.showNavigationView()
                  activity?.addMenuProvider(this@AppListFragment)
                }
                STATUS_START_REQUEST_CHANGE_END -> {
                  dbItems.value?.let { dbItems -> updateItems(dbItems) }
                }
                STATUS_NOT_START -> {
                  val first = HarmonyOsUtil.isHarmonyOs() &&
                    !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.HARMONY_FIRST_INIT)
                  val second = !isFirstLaunch &&
                    !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.SHOULD_RELOAD_APP_LIST)
                  if (first || second) {
                    initApps()
                    Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
                    Once.markDone(OnceTag.HARMONY_FIRST_INIT)
                  }
                }
              }
            }
            is HomeViewModel.Effect.RefreshList -> {
              homeViewModel.dbItems.value?.let { dbItems ->
                updateItems(dbItems)
              }
            }
          }
        }
      }

      dbItems.observe(viewLifecycleOwner) {
        if (it.isNullOrEmpty()) {
          initApps()
        } else if (
          appListStatus != STATUS_START_INIT &&
          appListStatus != STATUS_START_REQUEST_CHANGE
        ) {
          updateItems(it)
          if (hasPackageChanged() || isFirstRequestChange) {
            isFirstRequestChange = false
            homeViewModel.requestChange()
          }
        }
      }
    }

    GlobalValues.apply {
      isShowSystemApps.observe(viewLifecycleOwner) {
        if (isListReady) {
          updateItems(homeViewModel.dbItems.value!!)
        }
      }
      appSortModeLiveData.observe(viewLifecycleOwner) { mode ->
        if (isListReady) {
          homeViewModel.dbItems.value?.let { allDatabaseItems ->
            val list = allDatabaseItems.toMutableList()

            when (mode) {
              Constants.SORT_MODE_DEFAULT -> list.sortWith(
                compareBy(
                  { it.abi },
                  { it.label }
                )
              )
              Constants.SORT_MODE_UPDATE_TIME_DESC -> list.sortByDescending { it.lastUpdatedTime }
              Constants.SORT_MODE_TARGET_API_DESC -> list.sortByDescending { it.targetApi }
            }
            updateItems(list)
          }
        }
      }
    }
  }

  private fun updateItems(
    newItems: List<LCItem>,
    highlightRefresh: Boolean = false
  ) {
    Timber.d("updateItems")
    var filterList: MutableList<LCItem>

    filterList = if (GlobalValues.isShowSystemApps.value == false) {
      newItems.filter { !it.isSystem }.toMutableList()
    } else {
      newItems.toMutableList()
    }

    if (keyword.isNotEmpty()) {
      filterList = filterList.filter {
        it.label.contains(keyword, ignoreCase = true) ||
          it.packageName.contains(keyword, ignoreCase = true)
      }.toMutableList()

      if (HarmonyOsUtil.isHarmonyOs() && keyword.contains("Harmony", true)) {
        filterList.addAll(newItems.filter { it.variant == Constants.VARIANT_HAP })
      }
    }

    when (GlobalValues.appSortMode) {
      Constants.SORT_MODE_DEFAULT -> filterList.sortWith(compareBy({ it.abi }, { it.label }))
      Constants.SORT_MODE_UPDATE_TIME_DESC -> filterList.sortByDescending { it.lastUpdatedTime }
      Constants.SORT_MODE_TARGET_API_DESC -> filterList.sortByDescending { it.targetApi }
    }

    appAdapter.setDiffNewData(filterList) {
      flip(VF_LIST)
      isListReady = true

      if (highlightRefresh) {
        //noinspection NotifyDataSetChanged
        appAdapter.notifyDataSetChanged()
      }
    }
  }

  private fun returnTopOfList() {
    binding.list.apply {
      if (canScrollVertically(-1)) {
        smoothScrollToPosition(0)
      }
    }
  }

  override fun getSuitableLayoutManager() = binding.list.layoutManager

  private fun getSuitableLayoutManagerImpl(): RecyclerView.LayoutManager {
    layoutManager = when (resources.configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())
      Configuration.ORIENTATION_LANDSCAPE ->
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
      else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
    }
    return layoutManager
  }

  private fun flip(page: Int) {
    Timber.d("flip to $page")
    allowRefreshing = page == VF_LIST
    if (binding.vfContainer.displayedChild != page) {
      binding.vfContainer.displayedChild = page
    }
    if (page == VF_INIT) {
      menu?.findItem(R.id.search)?.isVisible = false
      binding.initView.loadingView.resumeAnimation()
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.initView.loadingView.pauseAnimation()
    }
  }

  private fun initApps() {
    flip(VF_INIT)
    activity?.removeMenuProvider(this)
    homeViewModel.initItems()
  }

  override fun onReturnTop() {
    if (binding.list.canScrollVertically(-1)) {
      returnTopOfList()
    } else {
      flip(VF_LOADING)
      homeViewModel.requestChange(true)
    }
  }
}
