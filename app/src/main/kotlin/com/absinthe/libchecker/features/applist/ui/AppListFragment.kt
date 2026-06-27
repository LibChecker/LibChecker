package com.absinthe.libchecker.features.applist.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.domain.app.GetRandomAppIconUseCase
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.applist.ui.adapter.AppAdapter
import com.absinthe.libchecker.features.applist.ui.adapter.AppListDiffUtil
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.home.INavViewContainer
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.animator.ParticleRemoveItemAnimator
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.koin.android.ext.android.inject
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1
const val VF_INIT = 2
const val VF_REJECT = 3

class AppListFragment :
  BaseListControllerFragment<FragmentAppListBinding>(),
  SearchView.OnQueryTextListener {

  private val isFirstLaunch get() = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
  private val getRandomAppIcon: GetRandomAppIconUseCase by inject()
  private val appAdapter = AppAdapter()
  private val particleItemAnimator = ParticleRemoveItemAnimator()
  private var updateItemsJob: Job? = null
  private var delayShowNavigationJob: Job? = null
  private var advancedMenuBSDFragment: AdvancedMenuBSDFragment? = null
  private var isFirstRequestChange = true
  private var isSearchTextClearOnce = false
  private var firstScrollFlag = false
  private var hasInitializedItems = false

  private lateinit var layoutManager: RecyclerView.LayoutManager
  private lateinit var dumpAppsInfoResultLauncher: ActivityResultLauncher<String>
  private lateinit var queryAllPackagesPermissionLauncher: ActivityResultLauncher<String>

  override fun init() {
    val context = (context as? BaseActivity<*>) ?: return
    homeViewModel.onAppListViewCreated()
    appAdapter.also {
      it.setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        activity?.launchDetailPage(it.getItem(position))
      }
      it.setDiffCallback(AppListDiffUtil())
      it.setHasStableIds(true)
      it.stateView =
        EmptyListView(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        }
      it.isStateViewEnable = true
    }

    binding.apply {
      list.apply {
        adapter = appAdapter
        itemAnimator = particleItemAnimator
        borderDelegate = borderViewDelegate
        layoutManager = getSuitableLayoutManagerImpl(resources.configuration)
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            if (isResumed) {
              scheduleAppbarLiftingStatus(!top)
            }
          }
        if (itemDecorationCount == 0) {
          addItemDecoration(VerticalSpacesItemDecoration(4.dp, ratio = 0f))
        }
        setHasFixedSize(true)
        FastScrollerBuilder(this).useMd2Style().build()
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
          override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
              homeViewModel.onAppListUserScrolled()
            }
          }

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
              if (isFragmentVisible() && !isSearchTextClearOnce && canListScroll(appAdapter.data.size)) {
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
              if (isFragmentVisible() && !isSearchTextClearOnce && position < appAdapter.itemCount - 1) {
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
          appAdapter.setSpaceFooterView()
        }
      }
      rejectView.setOnClickListener {
        try {
          if (::queryAllPackagesPermissionLauncher.isInitialized && vfContainer.displayedChild == VF_REJECT) {
            queryAllPackagesPermissionLauncher.launch(Constants.GET_INSTALLED_APPS)
          }
        } catch (e: Exception) {
          Timber.e(e)
        }
      }
      initView.loadingView.setAppIconHighlightProvider { getRandomAppIcon() }
    }

    initObserver()
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    dumpAppsInfoResultLauncher =
      registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) {
        it?.let {
          homeViewModel.dumpAppsInfo(it)
        }
      }
    queryAllPackagesPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
          homeViewModel.checkPackagesPermission = false
          if (isAdded) {
            initApps()
          }
        }
      }
  }

  override fun onResume() {
    super.onResume()
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)
    if (homeViewModel.appListStatus == STATUS_START_INIT) {
      flip(VF_INIT)
      activity?.removeMenuProvider(this)
    }
    if (binding.vfContainer.displayedChild == VF_INIT) {
      binding.initView.loadingView.start()
    }
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    binding.initView.loadingView.stop()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    binding.list.layoutManager = getSuitableLayoutManagerImpl(newConfig)
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (appAdapter.highlightText != newText) {
      isSearchTextClearOnce = newText.isEmpty()
      appAdapter.highlightText = newText
      updateItems(highlightRefresh = true)

      when (val action = homeViewModel.handleAppListSearchQuery(newText)) {
        HomeViewModel.AppListSearchCommandAction.None -> Unit

        HomeViewModel.AppListSearchCommandAction.EasterEgg -> {
          context?.showToast("🥚")
          Telemetry.recordEvent(
            Constants.Event.EASTER_EGG,
            mapOf("EASTER_EGG" to "AppList Search")
          )
        }

        HomeViewModel.AppListSearchCommandAction.DebugModeEnabled -> {
          context?.showToast("DEBUG MODE")
        }

        HomeViewModel.AppListSearchCommandAction.UserModeEnabled -> {
          context?.showToast("USER MODE")
        }

        is HomeViewModel.AppListSearchCommandAction.DumpAppsInfo -> {
          runCatching {
            dumpAppsInfoResultLauncher.launch(action.fileName)
          }.onFailure {
            Timber.e(it)
            context?.showToast("Document API not working")
          }
        }
      }
    }
    return false
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.app_list_menu, menu)
    this.menu = menu

    val context = context ?: return
    val searchView = SearchView(context).apply {
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
          advancedMenuBSDFragment?.dismiss()
          advancedMenuBSDFragment = AdvancedMenuBSDFragment().apply {
            val menuState = homeViewModel.getAppListAdvancedMenuState()
            setOptionChangeListener(
              displayOptions = menuState.displayOptions,
              itemDisplayOptions = menuState.itemDisplayOptions,
              colorfulRuleIcon = menuState.colorfulRuleIcon,
              onDisplayOptionsChanged = homeViewModel::setAppListDisplayOptions,
              onItemDisplayOptionsChanged = homeViewModel::setAppListItemDisplayOptions
            )
            setOnDismissListener { advancedDiff, itemAdvancedDiff ->
              val dismissPlan = homeViewModel.onAppListAdvancedMenuDismissed(
                displayOptionsDiff = advancedDiff,
                itemDisplayOptionsDiff = itemAdvancedDiff
              )
              if (dismissPlan.shouldRefreshItems) {
                //noinspection NotifyDataSetChanged
                appAdapter.notifyDataSetChanged()
              }
              advancedMenuBSDFragment = null
            }
          }.also { bsd ->
            bsd.show(
              it.supportFragmentManager,
              AdvancedMenuBSDFragment::class.java.name
            )
          }
        }
      }
    }
    return true
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      appAdapter.setSpaceFooterView()
    }
  }

  override fun getSuitableLayoutManager() = binding.list.layoutManager

  override fun onReturnTop() {
    if (binding.list.canScrollVertically(-1)) {
      returnTopOfList()
    } else {
      flip(VF_LOADING)
      homeViewModel.requestChange()
    }
  }

  private fun initObserver() {
    homeViewModel.apply {
      effect.onEach {
        when (it) {
          is HomeViewModel.Effect.ReloadApps -> {
            Once.clearDone(OnceTag.FIRST_LAUNCH)
            doOnMainThreadIdle {
              initApps()
            }
          }

          is HomeViewModel.Effect.UpdateInitProgress -> {
            binding.initView.progressIndicator.setProgressCompat(it.progress, true)
          }

          is HomeViewModel.Effect.PackageChanged -> {
            homeViewModel.requestChange(it.packageChangeState)
          }

          is HomeViewModel.Effect.UpdateAppListStatus -> {
            Timber.d("AppList status updates to ${it.status}")
            when (it.status) {
              STATUS_START_INIT -> {
                isListReady = false
                flip(VF_INIT)
              }

              STATUS_INIT_END -> {
                if (isFirstLaunch) {
                  if (homeViewModel.isOnlySelfAppInDatabase()) {
                    Timber.d("Only the app itself")
                    flip(VF_REJECT)
                  } else {
                    Once.markDone(OnceTag.FIRST_LAUNCH)
                    Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
                  }
                }
                activity?.removeMenuProvider(this@AppListFragment)
                activity?.addMenuProvider(this@AppListFragment, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
            updateItems()
          }
        }
      }.launchIn(lifecycleScope)
      displayItemsFlow.onEach {
        if (it.isEmpty() || (isFirstLaunch && !hasInitializedItems)) {
          initApps()
        } else if (
          appListStatus != STATUS_START_INIT &&
          appListStatus != STATUS_START_REQUEST_CHANGE
        ) {
          updateItems()
          if (hasPackageChanged() || isFirstRequestChange) {
            isFirstRequestChange = false
            homeViewModel.requestChange()
          }
        }
      }.launchIn(lifecycleScope)
    }

    homeViewModel.appListDisplayOptionsChanges.onEach {
      if (isListReady) {
        updateItems()
      }
    }.launchIn(lifecycleScope)
  }

  private fun updateItems(highlightRefresh: Boolean = false) {
    updateItemsJob?.cancel()
    updateItemsJob = updateItemsImpl(highlightRefresh)
  }

  private fun updateItemsImpl(highlightRefresh: Boolean = false) = lifecycleScope.launch(Dispatchers.IO) {
    delay(250)
    Timber.d("updateItemsImpl")
    val keyword = appAdapter.highlightText
    val currentItems = withContext(Dispatchers.Main) {
      appAdapter.data.toList()
    }
    val update = homeViewModel.buildAppListUpdate(
      keyword = keyword,
      isCurrentProcess64Bit = android.os.Process.is64Bit(),
      currentItems = currentItems,
      highlightRefresh = highlightRefresh
    )
    val updatePlan = when (update) {
      HomeViewModel.AppListUpdate.OnlySelf -> {
        Timber.d("updateItemsImpl: only the app itself")
        if (homeViewModel.appListStatus == STATUS_NOT_START) {
          Once.clearDone(OnceTag.FIRST_LAUNCH)
          flip(VF_REJECT)
        }
        return@launch
      }

      is HomeViewModel.AppListUpdate.Content -> update.plan
    }

    if (!isActive) {
      return@launch
    }
    withContext(Dispatchers.Main) {
      appAdapter.apply {
        particleItemAnimator.prepareParticleRemovals(updatePlan.particleRemovalItemIds)
        setItemViewStates(updatePlan.content.itemViewStates)
        homeViewModel.onAppListUpdatePlanApplied(updatePlan)

        setDiffNewData(updatePlan.content.items.toMutableList()) {
          if (isDetached || !isBindingInitialized()) {
            return@setDiffNewData
          }
          flip(VF_LIST)
          isListReady = true

          if (highlightRefresh) {
            notifyItemRangeChanged(0, data.size)
          }

          setSpaceFooterView()
          if (updatePlan.shouldReturnTopAfterRequestChange) {
            returnTopOfList()
          }
        }
      }
    }
  }

  private fun returnTopOfList() {
    binding.list.apply {
      post {
        when (val manager = layoutManager) {
          is LinearLayoutManager -> manager.scrollToPositionWithOffset(0, 0)
          is StaggeredGridLayoutManager -> manager.scrollToPositionWithOffset(0, 0)
          else -> scrollToPosition(0)
        }
      }
    }
  }

  private fun getSuitableLayoutManagerImpl(configuration: Configuration): RecyclerView.LayoutManager {
    layoutManager = when (configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())

      Configuration.ORIENTATION_LANDSCAPE ->
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

      else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
    }
    return layoutManager
  }

  private fun flip(page: Int) = lifecycleScope.launch(Dispatchers.Main) {
    allowRefreshing = page == VF_LIST
    homeViewModel.checkPackagesPermission = page == VF_REJECT
    if (binding.vfContainer.displayedChild != page) {
      Timber.d("flip to $page")
      binding.vfContainer.displayedChild = page
    }
    if (page == VF_INIT) {
      menu?.findItem(R.id.search)?.isVisible = false
      if (isResumed) {
        binding.initView.loadingView.start()
      }
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.initView.loadingView.stop()
    }
  }

  private fun initApps() {
    hasInitializedItems = true
    flip(VF_INIT)
    activity?.let {
      it.removeMenuProvider(this)
      homeViewModel.initItems()
    }
  }
}
