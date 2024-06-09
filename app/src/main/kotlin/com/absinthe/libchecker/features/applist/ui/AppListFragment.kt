package com.absinthe.libchecker.features.applist.ui

import android.content.res.Configuration
import android.graphics.Color
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
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
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.databinding.FragmentAppListBinding
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.applist.ui.adapter.AppAdapter
import com.absinthe.libchecker.features.applist.ui.adapter.AppListDiffUtil
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.home.INavViewContainer
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.isPreinstalled
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
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
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1
const val VF_INIT = 2

class AppListFragment :
  BaseListControllerFragment<FragmentAppListBinding>(),
  SearchView.OnQueryTextListener {

  private val appAdapter = AppAdapter()
  private var updateItemsJob: Job? = null
  private var delayShowNavigationJob: Job? = null
  private var advancedMenuBSDFragment: AdvancedMenuBSDFragment? = null
  private var isFirstLaunch = !Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)
  private var isFirstRequestChange = true
  private var isSearchTextClearOnce = false
  private var firstScrollFlag = false

  private lateinit var layoutManager: RecyclerView.LayoutManager

  override fun init() {
    val context = (context as? BaseActivity<*>) ?: return
    appAdapter.also {
      it.setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        activity?.launchDetailPage(it.getItem(position))
      }
      it.setDiffCallback(AppListDiffUtil())
      it.setHasStableIds(true)
      it.setEmptyView(
        EmptyListView(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        }
      )
    }

    binding.apply {
      list.apply {
        adapter = appAdapter
        borderDelegate = borderViewDelegate
        layoutManager = getSuitableLayoutManagerImpl(resources.configuration)
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            if (isResumed) {
              scheduleAppbarLiftingStatus(!top)
            }
          }
        setHasFixedSize(true)
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

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
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
            setOnDismissListener { advancedDiff, itemAdvancedDiff ->
              if (advancedDiff > 0) {
                lifecycleScope.launch {
                  GlobalValues.preferencesFlow.emit(Constants.PREF_ADVANCED_OPTIONS to advancedDiff)
                }
              }

              if (advancedDiff > 0 || itemAdvancedDiff > 0) {
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

  private fun initObserver() {
    homeViewModel.apply {
      effect.onEach {
        when (it) {
          is HomeViewModel.Effect.ReloadApps -> {
            Once.clearDone(OnceTag.FIRST_LAUNCH)
            isFirstLaunch = true
            doOnMainThreadIdle {
              initApps()
            }
          }

          is HomeViewModel.Effect.UpdateInitProgress -> {
            binding.initView.progressIndicator.setProgressCompat(it.progress, true)
          }

          is HomeViewModel.Effect.PackageChanged -> {
            requestChange()
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
                  Once.markDone(OnceTag.FIRST_LAUNCH)
                  Once.markDone(OnceTag.SHOULD_RELOAD_APP_LIST)
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

          else -> {}
        }
      }.launchIn(lifecycleScope)
      dbItemsFlow.onEach {
        if (it.isEmpty()) {
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

    GlobalValues.preferencesFlow.onEach {
      if (it.first == Constants.PREF_ADVANCED_OPTIONS) {
        if (isListReady) {
          updateItems()
        }
      }
    }.launchIn(lifecycleScope)
  }

  private fun updateItems(highlightRefresh: Boolean = false) {
    updateItemsJob?.cancel()
    updateItemsJob = updateItemsImpl(highlightRefresh)
  }

  private fun updateItemsImpl(highlightRefresh: Boolean = false) =
    lifecycleScope.launch(Dispatchers.IO) {
      Timber.d("updateItems")
      var filterList: MutableList<LCItem> = Repositories.lcRepository.getLCItems().toMutableList()

      val isNonNativeLibApp64Bit = android.os.Process.is64Bit()
      val options = GlobalValues.advancedOptions
      if ((options and AdvancedOptions.SHOW_SYSTEM_APPS) == 0) {
        filterList = filterList.filter { !it.isSystem }.toMutableList()
      }
      if ((options and AdvancedOptions.SHOW_SYSTEM_FRAMEWORK_APPS) == 0) {
        filterList = filterList.filter {
          (!it.packageName.startsWith("com.android.") && it.packageName != "android") ||
            runCatching {
              PackageUtils.getPackageInfo(it.packageName).isPreinstalled()
            }.getOrDefault(false).not()
        }.toMutableList()
      }
      if ((options and AdvancedOptions.SHOW_OVERLAYS) == 0) {
        filterList = filterList.filter { it.abi.toInt() != Constants.OVERLAY }.toMutableList()
      }
      if ((options and AdvancedOptions.SHOW_64_BIT_APPS) == 0) {
        filterList = filterList.filter {
          val trueAbi = it.abi.mod(Constants.MULTI_ARCH)
          it.abi.toInt() == Constants.OVERLAY || trueAbi == Constants.X86 || trueAbi == Constants.ARMV7 || trueAbi == Constants.ARMV5 || (trueAbi == Constants.NO_LIBS && !isNonNativeLibApp64Bit)
        }.toMutableList()
      }
      if ((options and AdvancedOptions.SHOW_32_BIT_APPS) == 0) {
        filterList = filterList.filter {
          val trueAbi = it.abi.mod(Constants.MULTI_ARCH)
          it.abi.toInt() == Constants.OVERLAY || trueAbi == Constants.X86_64 || trueAbi == Constants.ARMV8 || (trueAbi == Constants.NO_LIBS && isNonNativeLibApp64Bit)
        }.toMutableList()
      }

      val keyword = appAdapter.highlightText
      if (keyword.isNotEmpty()) {
        filterList = filterList.filter {
          it.label.contains(keyword, ignoreCase = true) ||
            it.packageName.contains(keyword, ignoreCase = true)
        }.toMutableList()

        if (HarmonyOsUtil.isHarmonyOs() && keyword.contains("Harmony", true)) {
          filterList = filterList.filter { it.variant == Constants.VARIANT_HAP }.toMutableList()
        }
      }

      if ((options and AdvancedOptions.SORT_BY_NAME) > 0) {
        filterList.sortWith(compareBy({ it.abi }, { it.label }))
      } else if ((options and AdvancedOptions.SORT_BY_UPDATE_TIME) > 0) {
        filterList.sortByDescending { it.lastUpdatedTime }
      } else if ((options and AdvancedOptions.SORT_BY_TARGET_API) > 0) {
        filterList.sortByDescending { it.targetApi }
      }

      if (!isActive) {
        return@launch
      }
      withContext(Dispatchers.Main) {
        appAdapter.apply {
          setDiffNewData(filterList) {
            if (isDetached || !isBindingInitialized()) {
              return@setDiffNewData
            }
            flip(VF_LIST)
            isListReady = true

            if (highlightRefresh) {
              notifyItemRangeChanged(0, data.size)
            }

            setSpaceFooterView()
          }
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

  private fun getSuitableLayoutManagerImpl(configuration: Configuration): RecyclerView.LayoutManager {
    layoutManager = when (configuration.orientation) {
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
      homeViewModel.requestChange()
    }
  }
}
