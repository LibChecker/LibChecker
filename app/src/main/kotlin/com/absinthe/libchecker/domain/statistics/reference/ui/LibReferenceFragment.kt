package com.absinthe.libchecker.domain.statistics.reference.ui

import android.content.Intent
import android.graphics.Rect
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LIB_REFERENCE_PROVIDER
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.LibReferenceAdapter
import com.absinthe.libchecker.domain.statistics.reference.ui.adapter.MULTIPLE_APPS_ICON_PROVIDER
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.ListScreenChrome
import com.absinthe.libchecker.ui.base.shouldHandleListSearchQueryChange
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
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
private const val VF_TREEMAP = 2
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
  private var isTreemap: Boolean
    get() = libReferenceViewModel.treemapEnabled
    set(value) {
      libReferenceViewModel.treemapEnabled = value
      (activity as? MainActivity)?.updateStatisticsIcon(value)
    }
  private var displayedReferences = emptyList<LibReference>()
  private val insetBounds = Rect()
  private val contentLocation = IntArray(2)
  private val chromeLocation = IntArray(2)
  private var animatedTreemapInsets: WindowInsetsCompat? = null
  private val treemapInsetsListener = ViewTreeObserver.OnPreDrawListener {
    if (isResumed && isTreemap && isBindingInitialized()) {
      updateTreemapInsets()
      if (!libReferenceViewModel.pinchHintShown && advancedMenuBSDFragment?.dialog?.isShowing != true &&
        binding.treemap.showPinchHint()
      ) {
        libReferenceViewModel.pinchHintShown = true
      }
    }
    true
  }
  private var treemapInsetsObserver: ViewTreeObserver? = null
  private val treemapAttachListener = object : View.OnAttachStateChangeListener {
    override fun onViewAttachedToWindow(v: View) {
      removeTreemapInsetsListener()
      treemapInsetsObserver = v.viewTreeObserver.also { it.addOnPreDrawListener(treemapInsetsListener) }
    }

    override fun onViewDetachedFromWindow(v: View) {
      removeTreemapInsetsListener()
    }
  }

  private fun removeTreemapInsetsListener() {
    treemapInsetsObserver?.takeIf { it.isAlive }?.removeOnPreDrawListener(treemapInsetsListener)
    treemapInsetsObserver = null
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
          val item = refAdapter.data.getOrNull(position) ?: return@installRecentVisitDrag false
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
      treemap.colorfulRuleIcon = libReferenceViewModel.colorfulRuleIcon
      treemap.onReferenceClick = ::openReferenceApps
      (activity as? MainActivity)?.observeKeyboardInsets(
        viewLifecycleOwner,
        frame = { insets ->
          animatedTreemapInsets = insets
          if (isResumed && isTreemap) updateTreemapInsets()
        },
        end = {
          if (isResumed && isTreemap) {
            updateTreemapInsets()
            val currentRoot = root
            currentRoot.doOnPreDraw {
              if (isBindingInitialized() && binding.root === currentRoot && isResumed && isTreemap) treemap.reflowForViewport()
            }
            currentRoot.postInvalidateOnAnimation()
          }
        }
      )
      root.addOnAttachStateChangeListener(treemapAttachListener)
      if (root.isAttachedToWindow) treemapAttachListener.onViewAttachedToWindow(root)
    }

    refAdapter.apply {
      animationEnable = true
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }
        openReferenceApps(refAdapter.data[position])
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
                displayedReferences = searchResult.references
                isListReady = true
                flip(VF_LIST)
                scheduleFirstListPresentation()
                refAdapter.setSpaceFooterView()
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
        binding.treemap.colorfulRuleIcon = enabled
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

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    (activity as? INavViewContainer)?.hideProgressBar()
  }

  override fun onDestroyView() {
    animatedTreemapInsets = null
    binding.root.removeOnAttachStateChangeListener(treemapAttachListener)
    removeTreemapInsetsListener()
    searchUpdateJob?.cancel()
    binding.list.removeCallbacks(prewarmNextViewHolder)
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
        setDisplayModeListener(isTreemap) { enabled ->
          isTreemap = enabled
          if (isListReady) flip(VF_LIST)
        }
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
          displayedReferences = searchResult.references
          refAdapter.setList(searchResult.references)
          if (isTreemap && isListReady) renderTreemap(animate = true)
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
    if (visible && isTreemap) {
      (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.treemap)
      (activity as? INavViewContainer)?.showNavigationView()
    } else {
      onListScreenVisibilityChanged(visible, binding.list)
    }
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
    if (!isTreemap) animateReturnTop(binding.list)
  }

  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager? = if (isTreemap) null else binding.list.layoutManager

  private fun openReferenceApps(item: LibReference) {
    activity?.findViewById<View>(androidx.appcompat.R.id.search_src_text)?.clearFocus()
    activity?.launchLibReferencePage(
      item.libName,
      item.rule?.label,
      item.type,
      item.referredList.toTypedArray()
    )
  }

  private fun renderTreemap(animate: Boolean = false) {
    binding.treemap.isVisible = true
    binding.treemapEmpty.isVisible = displayedReferences.isEmpty()
    binding.treemap.submitReferences(displayedReferences, animate)
  }

  private fun flip(child: Int) {
    val target = if (child == VF_LIST && isTreemap) VF_TREEMAP else child
    allowRefreshing = target == VF_LIST
    if (target == VF_TREEMAP) {
      renderTreemap()
      cancelReturnTopAnimation()
      if (isFragmentVisible()) {
        (activity as? INavViewContainer)?.showNavigationView()
        (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.treemap)
      }
    } else if (target == VF_LIST && isFragmentVisible()) {
      onListScreenVisibilityChanged(true, binding.list)
    }
    menu?.findItem(R.id.search)?.isVisible = child != VF_LOADING
    if (binding.vfContainer.displayedChild == target) return
    if (target == VF_LIST && binding.vfContainer.displayedChild == VF_LOADING) {
      binding.list.scrollToPosition(0)
    }
    binding.vfContainer.displayedChild = target
  }

  private fun updateTreemapInsets() {
    val container = binding.treemapContainer
    if (container.width == 0 || container.height == 0) return
    val decor = activity?.window?.decorView as? ViewGroup ?: return

    // Insets follow layout bounds, not the page/nav translation used during tab transitions.
    fun layoutLocation(view: View, result: IntArray) {
      insetBounds.setEmpty()
      decor.offsetDescendantRectToMyCoords(view, insetBounds)
      result[0] = insetBounds.left
      result[1] = insetBounds.top
    }
    val insets = (animatedTreemapInsets ?: ViewCompat.getRootWindowInsets(container))
      ?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
    layoutLocation(container, contentLocation)
    layoutLocation(decor, chromeLocation)
    var left = ((insets?.left ?: 0) + chromeLocation[0] - contentLocation[0]).coerceAtLeast(0)
    var top = ((insets?.top ?: 0) + chromeLocation[1] - contentLocation[1]).coerceAtLeast(0)
    var right = (contentLocation[0] + container.width - chromeLocation[0] - decor.width + (insets?.right ?: 0)).coerceAtLeast(0)
    var bottom = (contentLocation[1] + container.height - chromeLocation[1] - decor.height + (insets?.bottom ?: 0)).coerceAtLeast(0)
    activity?.findViewById<View>(R.id.appbar)?.takeIf { it.isShown }?.let { appbar ->
      layoutLocation(appbar, chromeLocation)
      top = maxOf(top, chromeLocation[1] + appbar.height - contentLocation[1])
    }
    activity?.findViewById<View>(R.id.nav_view)?.takeIf { it.isShown }?.let { nav ->
      layoutLocation(nav, chromeLocation)
      if (nav.width > nav.height) {
        chromeLocation[1] += (activity as? MainActivity)?.keyboardNavigationOffset?.toInt() ?: 0
        bottom = maxOf(bottom, contentLocation[1] + container.height - chromeLocation[1])
      } else if (chromeLocation[0] < contentLocation[0] + container.width / 2) {
        left = maxOf(left, chromeLocation[0] + nav.width - contentLocation[0])
      } else {
        right = maxOf(right, contentLocation[0] + container.width - chromeLocation[0])
      }
    }
    val gap = 3.dp
    // Cells already inset their painted bounds by half the 3dp gutter.
    val horizontalGap = (resources.getDimension(R.dimen.main_list_horizontal_padding) - 1.5f * resources.displayMetrics.density).toInt().coerceAtLeast(0)
    left = (left + horizontalGap).coerceIn(0, container.width)
    right = (right + horizontalGap).coerceIn(0, container.width - left)
    top = (top + gap).coerceIn(0, container.height)
    bottom = (bottom + horizontalGap).coerceIn(0, container.height - top)
    if (container.paddingLeft != left || container.paddingTop != top || container.paddingRight != right || container.paddingBottom != bottom) {
      container.updatePadding(left = left, top = top, right = right, bottom = bottom)
    }
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
    val content = if (isTreemap) binding.treemapContainer else binding.list
    content.doOnNextLayout {
      content.postOnAnimation {
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
