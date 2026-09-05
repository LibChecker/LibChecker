package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.FragmentLibReferenceBinding
import com.absinthe.libchecker.domain.app.detail.ui.dialog.LibDetailDialogFragment
import com.absinthe.libchecker.domain.app.list.ui.AdvancedMenuBSDFragment
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.home.recent.RecentVisit
import com.absinthe.libchecker.domain.home.ui.INavViewContainer
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import com.absinthe.libchecker.domain.home.ui.view.installRecentVisitDrag
import com.absinthe.libchecker.domain.statistics.chart.ui.ChartActivity
import com.absinthe.libchecker.domain.statistics.reference.TRACE_REFERENCE_RESULT_TO_FIRST_LAYOUT
import com.absinthe.libchecker.domain.statistics.reference.beginReferenceAsyncSection
import com.absinthe.libchecker.domain.statistics.reference.endReferenceAsyncSection
import com.absinthe.libchecker.domain.statistics.reference.model.LibReference
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceAction
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceListRenderState
import com.absinthe.libchecker.domain.statistics.reference.model.LibReferenceSearchLabels
import com.absinthe.libchecker.domain.statistics.reference.model.resolveReferenceIcon
import com.absinthe.libchecker.domain.statistics.reference.presentation.LibReferenceViewModel
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.provider.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.ListScreenChrome
import com.absinthe.libchecker.ui.base.shouldHandleListSearchQueryChange
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.app.EmptyListView
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
import org.koin.androidx.viewmodel.ext.android.viewModel

const val VF_LOADING = 0
const val VF_LIST = 1
private const val SEARCH_UPDATE_DELAY_MILLIS = 160L

class LibReferenceFragment :
  BaseListControllerFragment<FragmentLibReferenceBinding>(),
  SearchView.OnQueryTextListener {

  private val libReferenceViewModel: LibReferenceViewModel by viewModel()
  private var listRenderState = LibReferenceListRenderState()
  private val refAdapter by lazy {
    listRenderState = listRenderState.copy(
      colorfulRuleIcon = libReferenceViewModel.colorfulRuleIcon
    )
    LibReferenceAdapter { action ->
      when (action) {
        is LibReferenceAction.DetailIconClicked -> showLibReferenceDetail(action.reference)
      }
    }.apply {
      bind(listRenderState)
    }
  }
  private var searchUpdateJob: Job? = null
  private var advancedMenuBSDFragment: LibReferenceMenuBSDFragment? = null
  private var isSearchTextClearOnce = false
  private var resetScrollbarNavigationReveal: (() -> Unit)? = null
  private var hasReportedFirstListLayout = false
  private var resultToFirstLayoutTraceActive = false
  private var prewarmIndex = 0
  private val resultToFirstLayoutTraceCookie = System.identityHashCode(this)
  private val prewarmViewTypes = IntArray(8) { LIB_REFERENCE_PROVIDER } +
    IntArray(4) { MULTIPLE_APPS_ICON_PROVIDER }
  private val startLoadingAnimation = Runnable {
    if (isResumed && binding.vfContainer.displayedChild == VF_LOADING) {
      binding.loadingView.loadingView.start()
    }
  }
  private val prewarmNextViewHolder = object : Runnable {
    override fun run() {
      if (prewarmIndex >= prewarmViewTypes.size || binding.vfContainer.displayedChild != VF_LOADING) {
        return
      }
      val viewType = prewarmViewTypes[prewarmIndex++]
      runCatching {
        val holder = refAdapter.createViewHolder(binding.list, viewType)
        binding.list.recycledViewPool.putRecycledView(holder)
      }
      binding.list.postOnAnimation(this)
    }
  }

  override fun init() {
    val context = (context as? BaseActivity<*>) ?: return

    binding.apply {
      list.apply {
        adapter = refAdapter
        installRecentVisitDrag(this) { row, position, touch ->
          val item = refAdapter.data.getOrNull(position) as? LibReference ?: return@installRecentVisitDrag false
          val label = item.rule?.label?.takeIf(String::isNotBlank) ?: item.resolvedLabel ?: item.libName
          val iconRes = resolveReferenceIcon(item.libName, item.type, item.rule)
          val icon = ContextCompat.getDrawable(context, iconRes) ?: return@installRecentVisitDrag false
          val visit = RecentVisit(item.libName, item.type, label, item.referredList.toList())
          (activity as? MainActivity)?.pinListItem(
            row,
            touch,
            RecentVisitItem(visit, label, icon, tintIcon = item.rule?.isSimpleColorIcon == true || iconRes == R.drawable.ic_question)
          ) == true
        }
        layoutManager = LinearLayoutManager(context)
        wireListScreenChrome(this)
        FastScrollerBuilder(this).useMd2Style().build()
        setRecycledViewPool(
          recycledViewPool.apply {
            setMaxRecycledViews(LIB_REFERENCE_PROVIDER, 8)
            setMaxRecycledViews(MULTIPLE_APPS_ICON_PROVIDER, 4)
          }
        )
        postOnAnimation(prewarmNextViewHolder)
        resetScrollbarNavigationReveal =
          ListScreenChrome.installScrollbarNavigationReveal(
            recyclerView = this,
            coroutineScope = lifecycleScope,
            isFragmentVisible = ::isFragmentVisible,
            isSearchTextClearOnce = { isSearchTextClearOnce },
            clearSearchTextFlag = { isSearchTextClearOnce = false },
            revealNavigation = { (activity as? INavViewContainer)?.showNavigationView() }
          )
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
        setOnDisplayedChildChangedListener {
          refAdapter.setSpaceFooterView()
        }
      }
      loadingView.loadingView.setRuleIconHighlightProvider()
    }

    refAdapter.apply {
      animationEnable = true
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
      viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
          launch {
            loadingState.collect {
              binding.loadingView.bind(it)
            }
          }
          launch {
            libReference.collect { references ->
              if (references == null) {
                return@collect
              }
              val searchResult = libReferenceViewModel.onReferenceListChanged(references)
              updateListRenderState { it.copy(highlightText = searchResult.query) }
              beginFirstListLayoutTrace()
              refAdapter.setDiffNewData(searchResult.references) {
                if (isDetached || libReferenceViewModel.libReference.value !== references) {
                  return@setDiffNewData
                }
                scheduleFirstListPresentation()
                flip(VF_LIST)
                refAdapter.setSpaceFooterView()
                isListReady = true
              }
            }
          }
        }
      }
      showSystemAppsChanges.onEach {
        applyReferenceWork(
          onShowSystemAppsChanged(isVisible = isFragmentVisible())
        )
      }.launchIn(lifecycleScope)
      colorfulRuleIconChanges.onEach { enabled ->
        if (updateListRenderState { it.copy(colorfulRuleIcon = enabled) }) {
          // noinspection NotifyDataSetChanged
          refAdapter.notifyDataSetChanged()
        }
      }.launchIn(lifecycleScope)
      thresholdChanges.onEach { threshold ->
        applyReferenceWork(
          onThresholdChanged(
            threshold = threshold,
            isVisible = isFragmentVisible()
          )
        )
      }.launchIn(lifecycleScope)
    }
  }

  override fun onResume() {
    super.onResume()
    if (binding.vfContainer.displayedChild == VF_LOADING) {
      scheduleLoadingAnimation()
    }
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    (activity as? INavViewContainer)?.hideProgressBar()
    binding.loadingView.loadingView.removeCallbacks(startLoadingAnimation)
    binding.loadingView.loadingView.stop()
  }

  override fun onDestroyView() {
    binding.list.removeCallbacks(prewarmNextViewHolder)
    binding.loadingView.loadingView.removeCallbacks(startLoadingAnimation)
    finishFirstListLayoutTrace(reportFullyDrawn = false)
    super.onDestroyView()
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.lib_ref_menu, menu)
    this.menu = menu

    val context = (context as? BaseActivity<*>) ?: return
    ListScreenChrome.installSearchMenuItem(
      menuItem = menu.findItem(R.id.search),
      context = context,
      queryHint = getText(R.string.search_hint),
      retainedQuery = libReferenceViewModel.getSearchQuery(),
      toolbarState = homeViewModel.getToolbarSearchMenuState(),
      listener = this,
      isListReady = isListReady
    )
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    val context = (context as? BaseActivity<*>) ?: return false
    if (menuItem.itemId == R.id.filter) {
      advancedMenuBSDFragment?.dismiss()
      advancedMenuBSDFragment = LibReferenceMenuBSDFragment().apply {
        setOptionChangeListener(
          initialOptions = libReferenceViewModel.getLibReferenceOptions(),
          colorfulRuleIcon = libReferenceViewModel.colorfulRuleIcon,
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
      LibDetailDialogFragment.newInstance(
        libName = request.name,
        type = request.type,
        regexName = request.regexName,
        enableLibraryInsight = false
      )
        .show(context.supportFragmentManager, LibDetailDialogFragment::class.java.name)
    }
  }

  override fun onQueryTextChange(newText: String): Boolean {
    if (!shouldHandleListSearchQueryChange(viewLifecycleOwner.lifecycle.currentState)) {
      return false
    }
    val shouldSyncHighlight = listRenderState.highlightText != newText
    val searchChange = libReferenceViewModel.onSearchQueryChanged(
      query = newText,
      labels = LibReferenceSearchLabels(
        notMarkedLabel = getString(R.string.not_marked_lib),
        permissionFallbackLabel = getString(R.string.ref_category_perm),
        metadataLabel = getString(R.string.ref_category_metadata),
        packageLabel = getString(R.string.ref_category_package)
      )
    )
    if (searchChange.shouldRefreshItems || shouldSyncHighlight) {
      isSearchTextClearOnce = newText.isEmpty()
      updateListRenderState { it.copy(highlightText = newText) }

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
          val searchResult = libReferenceViewModel.buildCurrentSearchResult() ?: return@launch
          updateListRenderState { it.copy(highlightText = searchResult.query) }

          if (!isActive) {
            return@launch
          }
          refAdapter.setList(searchResult.references)
          doOnMainThreadIdle {
            refAdapter.setSpaceFooterView()
          }

          if (searchChange.shouldRefreshItems && searchResult.shouldShowEasterEgg) {
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

  private fun updateListRenderState(
    transform: (LibReferenceListRenderState) -> LibReferenceListRenderState
  ): Boolean {
    val currentAdapter = refAdapter
    val state = transform(listRenderState)
    if (state == listRenderState) {
      return false
    }
    listRenderState = state
    currentAdapter.bind(state)
    return true
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    onListScreenVisibilityChanged(visible, binding.list)
    if (visible) {
      refAdapter.setSpaceFooterView()
      applyReferenceWork(
        libReferenceViewModel.onReferencePageVisible(hasDisplayedReferences = refAdapter.data.isNotEmpty())
      )
    } else {
      resetScrollbarNavigationReveal?.invoke()
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
        scheduleLoadingAnimation()
      }
    } else {
      menu?.findItem(R.id.search)?.isVisible = true
      binding.loadingView.loadingView.stop()
      binding.list.scrollToPosition(0)
    }

    binding.vfContainer.displayedChild = child
  }

  private fun scheduleLoadingAnimation() {
    binding.loadingView.loadingView.removeCallbacks(startLoadingAnimation)
    binding.loadingView.loadingView.postOnAnimation(startLoadingAnimation)
  }

  private fun beginFirstListLayoutTrace() {
    if (hasReportedFirstListLayout || resultToFirstLayoutTraceActive) {
      return
    }
    resultToFirstLayoutTraceActive = true
    beginReferenceAsyncSection(
      TRACE_REFERENCE_RESULT_TO_FIRST_LAYOUT,
      resultToFirstLayoutTraceCookie
    )
  }

  private fun scheduleFirstListPresentation() {
    binding.list.doOnNextLayout {
      binding.list.postOnAnimation {
        finishFirstListLayoutTrace(reportFullyDrawn = true)
      }
    }
  }

  private fun finishFirstListLayoutTrace(reportFullyDrawn: Boolean) {
    if (!resultToFirstLayoutTraceActive) {
      return
    }
    resultToFirstLayoutTraceActive = false
    endReferenceAsyncSection(
      TRACE_REFERENCE_RESULT_TO_FIRST_LAYOUT,
      resultToFirstLayoutTraceCookie
    )
    if (reportFullyDrawn && !hasReportedFirstListLayout) {
      hasReportedFirstListLayout = true
      activity?.reportFullyDrawn()
    }
  }
}
