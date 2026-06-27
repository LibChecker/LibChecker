package com.absinthe.libchecker.features.statistics.ui

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
import com.absinthe.libchecker.constant.options.AdvancedOptions
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.domain.app.AppListSettingsRepository
import com.absinthe.libchecker.features.applist.detail.ui.LibDetailDialogFragment
import com.absinthe.libchecker.features.applist.detail.ui.view.EmptyListView
import com.absinthe.libchecker.features.applist.ui.AdvancedMenuBSDFragment
import com.absinthe.libchecker.features.chart.ui.ChartActivity
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.home.INavViewContainer
import com.absinthe.libchecker.features.statistics.LibReferenceViewModel
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.features.statistics.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.features.statistics.ui.adapter.RefListDiffUtil
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.utils.AntiShakeUtils
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
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1
private const val SEARCH_UPDATE_DELAY_MILLIS = 160L

class LibReferenceFragment :
  BaseListControllerFragment<FragmentLibReferenceBinding>(),
  SearchView.OnQueryTextListener {

  private val appListSettingsRepository: AppListSettingsRepository by inject()
  private val libReferenceViewModel: LibReferenceViewModel by viewModel()
  private val refAdapter by lazy {
    LibReferenceAdapter(
      initialColorfulRuleIcon = appListSettingsRepository.colorfulRuleIcon,
      onDetailIconClick = ::showLibReferenceDetail
    )
  }
  private var delayShowNavigationJob: Job? = null
  private var searchUpdateJob: Job? = null
  private var advancedMenuBSDFragment: LibReferenceMenuBSDFragment? = null
  private var firstScrollFlag = false
  private var isSearchTextClearOnce = false

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
      loadingView.loadingView.setRuleIconHighlightProvider(withCircleBackground = true)
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
        activity?.launchLibReferencePage(
          item.libName,
          item.rule?.label,
          item.type,
          item.referredList.toTypedArray()
        )
      }
      stateView =
        EmptyListView(context).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
          )
        }
      isStateViewEnable = true
    }

    homeViewModel.apply {
      effect.onEach {
        when (it) {
          is HomeViewModel.Effect.PackageChanged -> {
            requestComputeRef(false)
          }

          else -> {}
        }
      }.launchIn(lifecycleScope)
    }
    libReferenceViewModel.apply {
      progress.onEach {
        binding.loadingView.progressIndicator.setProgressCompat(
          it,
          it > 0
        )
      }.launchIn(lifecycleScope)
      libReference.onEach { references ->
        if (references == null) {
          return@onEach
        }
        refAdapter.setList(references)

        flip(VF_LIST)
        refAdapter.setSpaceFooterView()
        isListReady = true
      }.launchIn(lifecycleScope)
    }
    appListSettingsRepository.displayOptionsChanges.onEach { options ->
      if (options and AdvancedOptions.SHOW_SYSTEM_APPS > 0) {
        requestComputeRef(true)
      }
    }.launchIn(lifecycleScope)
    appListSettingsRepository.colorfulRuleIconChanges.onEach { enabled ->
      refAdapter.updateColorfulRuleIcon(enabled)
    }.launchIn(lifecycleScope)
    libReferenceViewModel.thresholdChanges.onEach { threshold ->
      applyReferenceWork(
        libReferenceViewModel.onThresholdChanged(
          threshold = threshold,
          isVisible = isFragmentVisible()
        )
      )
    }.launchIn(lifecycleScope)
  }

  override fun onResume() {
    super.onResume()
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)
    if (binding.vfContainer.displayedChild == VF_LOADING) {
      binding.loadingView.loadingView.start()
    }
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    (activity as? INavViewContainer)?.hideProgressBar()
    binding.loadingView.loadingView.stop()
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
        setOptionChangeListener(
          initialOptions = libReferenceViewModel.getLibReferenceOptions(),
          onOptionChanged = libReferenceViewModel::setLibReferenceOption
        )
        setOnDismissListener { optionsDiff ->
          if (optionsDiff > 0) {
            refreshList()
          }
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
    requestComputeRef(true)
    Telemetry.recordEvent(
      Constants.Event.LIB_REFERENCE_FILTER_TYPE,
      mapOf(
        Telemetry.Param.CONTENT_TYPE to
          libReferenceViewModel.getLibReferenceOptionsString()
      )
    )
  }

  private fun requestComputeRef(needShowLoading: Boolean) {
    applyReferenceWork(
      libReferenceViewModel.requestComputeReference(
        isVisible = isFragmentVisible(),
        needShowLoading = needShowLoading
      )
    )
  }

  private fun applyReferenceWork(plan: LibReferenceViewModel.ReferenceWorkPlan?) {
    plan ?: return
    isListReady = false
    if (plan.shouldShowLoading) {
      flip(VF_LOADING)
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  private fun showLibReferenceDetail(ref: LibReference) {
    val context = (context as? BaseActivity<*>) ?: return
    lifecycleScope.launch {
      val request = libReferenceViewModel.buildDetailDialogRequest(ref.libName, ref.type) ?: return@launch
      context.findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()
      LibDetailDialogFragment.newInstance(request.name, request.type, request.regexName)
        .show(context.supportFragmentManager, LibDetailDialogFragment::class.java.name)
    }
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (LibReferenceAdapter.highlightText != newText) {
      isSearchTextClearOnce = newText.isEmpty()
      LibReferenceAdapter.highlightText = newText

      searchUpdateJob?.cancel()
      searchUpdateJob = lifecycleScope.launch {
        var progressBarShown = false
        try {
          if (newText.isNotEmpty()) {
            delay(SEARCH_UPDATE_DELAY_MILLIS)
          }
          if (isFragmentVisible()) {
            (activity as? INavViewContainer)?.showProgressBar()
            progressBarShown = true
          }
          val searchResult = libReferenceViewModel.buildSearchResult(newText) ?: return@launch
          LibReferenceAdapter.highlightText = newText

          if (!isActive) {
            return@launch
          }
          refAdapter.setList(searchResult.references)
          doOnMainThreadIdle {
            refAdapter.setSpaceFooterView()
          }

          if (searchResult.shouldShowEasterEgg) {
            context?.showToast("🥚")
            Telemetry.recordEvent(
              Constants.Event.EASTER_EGG,
              mapOf("EASTER_EGG" to "Lib Reference Search")
            )
          }
        } finally {
          if (progressBarShown) {
            (activity as? INavViewContainer)?.hideProgressBar()
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
      applyReferenceWork(
        libReferenceViewModel.onReferencePageVisible(hasDisplayedReferences = refAdapter.data.isNotEmpty())
      )
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
      if (isResumed) {
        binding.loadingView.loadingView.start()
      }
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.loadingView.loadingView.stop()
      binding.list.scrollToPosition(0)
    }

    binding.vfContainer.displayedChild = child
  }
}
