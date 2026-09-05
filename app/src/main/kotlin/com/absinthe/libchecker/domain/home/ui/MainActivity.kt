package com.absinthe.libchecker.domain.home.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.PathInterpolator
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.get
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.databinding.ActivityMainBinding
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.AppDetailActivity
import com.absinthe.libchecker.domain.home.model.HomeToolbarTitleState
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.home.presentation.RecentVisitsViewModel
import com.absinthe.libchecker.domain.home.ui.view.HomeToolbarTitleView
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitItem
import com.absinthe.libchecker.domain.home.ui.view.RecentVisitsPopup
import com.absinthe.libchecker.domain.home.ui.view.startRecentVisitDrag
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.services.WorkerService
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.ui.base.IListControllerHost
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.isKeyboardShowing
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.view.app.BlurCoordinatorLayout
import com.absinthe.libchecker.view.app.FloatingNavigationBar
import com.absinthe.libchecker.view.app.InvalidatingHideBottomViewOnScrollBehavior
import com.absinthe.libchecker.view.drawable.G2PillDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.motion.MotionUtils
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import java.util.WeakHashMap
import jonathanfinerty.once.Once
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

private const val PAGE_EXIT_DURATION = 90L
private const val PAGE_ENTER_DURATION = 160L
private const val PAGE_TRANSITION_OFFSET_DP = 20f
private const val SDR_HDR_HEADROOM = 1f
private val PAGE_EXIT_INTERPOLATOR = PathInterpolator(0.4f, 0f, 1f, 1f)
private val PAGE_ENTER_INTERPOLATOR = PathInterpolator(0f, 0f, 0.2f, 1f)

private data class RecyclerViewScreenAnchor(
  val recyclerView: RecyclerView,
  val adapterPosition: Int,
  val screenTop: Int
)

class MainActivity :
  BaseActivity<ActivityMainBinding>(),
  INavViewContainer,
  IAppBarContainer,
  IListControllerHost {

  private val appViewModel: HomeViewModel by viewModel()
  private val recentVisitsViewModel: RecentVisitsViewModel by viewModel()
  private var recentVisitsPopup: RecentVisitsPopup? = null
  private val cloudRulesRepository: CloudRulesRepository by inject()
  private var listController: IListController? = null
  private val initialListTopPaddings = WeakHashMap<View, Int>()
  private var blurContainer: BlurCoordinatorLayout? = null
  private var appbarScrollTarget: RecyclerView? = null
  private val appbarLocation = IntArray(2)
  private val appbarScrollTargetLocation = IntArray(2)
  private var pendingAnchorRestoreObserver: ViewTreeObserver? = null
  private var pendingAnchorRestoreListener: ViewTreeObserver.OnPreDrawListener? = null
  private var isPageTransitionRunning = false
  private var pendingPageIndex: Int? = null
  private val appbarScrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
      updateAppbarContentUnderlap()
    }
  }

  @Suppress("DEPRECATION")
  private val navViewBehavior by lazy { InvalidatingHideBottomViewOnScrollBehavior() }
  private var navPillDrawable: G2PillDrawable? = null
  private var originalNavBackground: Drawable? = null
  private var floatingNavBarAnimator: ValueAnimator? = null
  private var floatingNavProgress: Float = 0f
  private var blurDesignEnabled = GlobalValues.isBlurDesign
  private var floatingNavEnabled = GlobalValues.isFloatingNavBar
  private var systemBarBottomInset: Int = 0
  private var originalLabelVisibilityMode: Int = NavigationBarView.LABEL_VISIBILITY_AUTO
  private val toolbarTitleView by lazy {
    HomeToolbarTitleView(this).apply {
      setHdrHeadroomChangedListener(::updateWindowHdrHeadroom)
    }
  }
  private lateinit var toolbarTitleState: HomeToolbarTitleState
  private val workerServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      if (service?.pingBinder() == true) {
        appViewModel.connectWorkerBinder(IWorkerService.Stub.asInterface(service))
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      appViewModel.disconnectWorkerBinder()
    }
  }
  private val _menuProviders = hashSetOf<MenuProvider>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureHdrWindow()

    if (intent.getBooleanExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, false)) {
      Timber.w("Reinitializing updated rule database")
      cloudRulesRepository.reinitializeRules()
    }

    initView()
    initObserver()
    bindService(
      Intent(this, WorkerService::class.java).apply {
        setPackage(packageName)
      },
      workerServiceConnection,
      BIND_AUTO_CREATE
    )
    appViewModel.clearApkCache()
    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onDestroy() {
    recentVisitsPopup?.dismissImmediately()
    recentVisitsPopup = null
    appbarScrollTarget?.removeOnScrollListener(appbarScrollListener)
    appbarScrollTarget = null
    cancelPendingAnchorRestore()
    super.onDestroy()
    unbindService(workerServiceConnection)
  }

  override fun onPause() {
    recentVisitsPopup?.dismissImmediately()
    recentVisitsPopup = null
    saveToolbarMenuState()
    super.onPause()
  }

  private fun saveToolbarMenuState() {
    if (!isBindingInitialized()) {
      return
    }
    val searchItem = binding.toolbar.menu?.findItem(R.id.search)
    val searchView = searchItem?.actionView as? SearchView
    val isExpanded = binding.toolbar.hasExpandedActionView() && searchItem?.isActionViewExpanded == true
    appViewModel.saveToolbarSearchMenuState(
      isExpanded = isExpanded,
      query = searchView?.query?.toString().orEmpty()
    )
  }

  override fun addMenuProvider(provider: MenuProvider) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider)
  }

  override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider, owner)
  }

  override fun addMenuProvider(
    provider: MenuProvider,
    owner: LifecycleOwner,
    state: Lifecycle.State
  ) {
    if (_menuProviders.contains(provider)) {
      super.removeMenuProvider(provider)
    }
    super.addMenuProvider(provider, owner, state)
  }

  override fun removeMenuProvider(provider: MenuProvider) {
    super.removeMenuProvider(provider)
    _menuProviders.remove(provider)
  }

  override fun showNavigationView() {
    // NavigationRailView 不需要隐藏，所以不需要显示
    if (binding.navView is BottomNavigationView) {
      navViewBehavior.slideUp(binding.navView as BottomNavigationView)
    }
  }

  override fun hideNavigationView() {
    // NavigationRailView 不需要隐藏
    if (binding.navView is BottomNavigationView) {
      navViewBehavior.slideDown(binding.navView as BottomNavigationView)
    }
  }

  override fun showProgressBar() {
    Timber.d("showProgressBar")
    binding.progressHorizontal.show()
  }

  override fun hideProgressBar() {
    Timber.d("hideProgressBar")
    binding.progressHorizontal.hide()
  }

  override fun setListController(controller: IListController) {
    listController = controller
  }

  override fun clearListController(controller: IListController) {
    if (listController === controller) {
      listController = null
    }
  }

  override fun isCurrentListController(controller: IListController): Boolean {
    return listController === controller
  }

  override fun scheduleAppbarLiftingStatus(isLifted: Boolean) {
    updateAppbarContentUnderlap(isLiftedHint = isLifted)
  }

  override fun setBlurDesignEnabled(enabled: Boolean) {
    blurDesignEnabled = enabled
    val scrollAnchor = captureAppbarScrollAnchor()
    val appbarInset = resolveCurrentAppbarInset()
    val contentUnderlaps = isListItemUnderAppbar()
    val container = installBlurContainer()
    val finishLayoutTransition = { progressiveBlurActive: Boolean ->
      applyHomeListTopPaddings(appbarInset)
      restoreAppbarScrollAnchorAfterLayout(scrollAnchor, progressiveBlurActive)
    }
    container?.setAppbarContentUnderlap(contentUnderlaps)
    if (enabled) {
      container?.setFloatingNavProgress(floatingNavProgress)
    }
    if (!enabled && container != null) {
      (binding.navView as? NavigationBarView)?.let { navView ->
        navView.background = if (floatingNavProgress > 0f) navPillDrawable else originalNavBackground
      }
      container.setBlurEnabled(false) transitionEnd@{
        if (blurContainer !== container) return@transitionEnd
        (binding.navView as? NavigationBarView)?.let { navView ->
          reconcileNavigationBackground(navView)
          val density = resources.displayMetrics.density
          navView.elevation = (3f + 3f * floatingNavProgress) * density
        }
        updateToolbarHdrHighlight(contentUnderlaps, animate = false)
        finishLayoutTransition(false)
      }
      return
    }
    container?.setBlurEnabled(enabled)
    updateToolbarHdrHighlight(contentUnderlaps, animate = enabled)
    finishLayoutTransition(container?.blurEnabled == true)
  }

  override fun setFloatingNavBarEnabled(enabled: Boolean) {
    floatingNavEnabled = enabled
    val navView = binding.navView as? NavigationBarView ?: return
    (navView as? FloatingNavigationBar)?.isFloating = enabled
    val targetProgress = if (enabled) 1f else 0f
    if (floatingNavProgress == targetProgress && floatingNavBarAnimator == null) return

    floatingNavBarAnimator?.cancel()
    if (enabled && !isNavigationBackgroundManagedByBlur(navView) && navView.background !== navPillDrawable) {
      navPillDrawable?.setAlpha(255)
      navView.background = navPillDrawable
    }
    val startProgress = floatingNavProgress
    val duration = MotionUtils.resolveThemeDuration(
      this,
      com.google.android.material.R.attr.motionDurationLong2,
      500
    ).toLong()
    val interpolator = MotionUtils.resolveThemeInterpolator(
      this,
      com.google.android.material.R.attr.motionEasingEmphasizedInterpolator,
      FastOutSlowInInterpolator()
    )

    var cancelled = false
    floatingNavBarAnimator = ValueAnimator.ofFloat(startProgress, targetProgress).apply {
      this.duration = duration
      this.interpolator = interpolator
      addUpdateListener { animator ->
        val progress = animator.animatedValue as Float
        applyFloatingNavProgress(navView, progress)
      }
      addListener(
        onEnd = {
          if (!cancelled) {
            floatingNavBarAnimator = null
            reconcileNavigationBackground(navView)
            navView.doOnLayout { updateAppbarContentUnderlap() }
          }
        },
        onCancel = {
          cancelled = true
          floatingNavBarAnimator = null
        }
      )
      start()
    }
  }

  private fun isNavigationBackgroundManagedByBlur(navView: NavigationBarView): Boolean = navView is BottomNavigationView && blurContainer?.blurEnabled == true

  private fun reconcileNavigationBackground(navView: NavigationBarView) {
    if (isNavigationBackgroundManagedByBlur(navView)) return
    val background = if (floatingNavProgress > 0f || floatingNavEnabled) navPillDrawable else originalNavBackground
    background?.alpha = 255
    navView.background = background
  }

  private fun initFloatingNavBar(navView: NavigationBarView?) {
    if (navView == null) return
    originalLabelVisibilityMode = navView.labelVisibilityMode
    originalNavBackground = navView.background
    navPillDrawable = G2PillDrawable(
      fillColor = getColorByAttr(com.google.android.material.R.attr.colorSurfaceContainer)
    )
    if (floatingNavEnabled) {
      navView.background = navPillDrawable
    }
    (navView as? FloatingNavigationBar)?.let {
      it.setSelectedIndex(binding.viewpager.currentItem, animate = false)
      it.isFloating = floatingNavEnabled
    }
    val targetProgress = if (floatingNavEnabled) 1f else 0f
    if (floatingNavEnabled && !navView.isLaidOut) {
      applyFloatingNavProgress(navView, 0f)
      navView.doOnLayout {
        navView.post {
          navView.doOnNextLayout { applyFloatingNavProgress(navView, targetProgress) }
        }
      }
    } else {
      applyFloatingNavProgress(navView, targetProgress)
      if (navView !is BottomNavigationView) {
        navView.doOnLayout { applyFloatingNavProgress(navView, targetProgress) }
      }
    }
  }

  private fun applyFloatingNavProgress(view: NavigationBarView, progress: Float) {
    floatingNavProgress = progress
    (view as? FloatingNavigationBar)?.setFloatingProgress(progress)

    val targetLabelVisibility = if (progress > 0.5f) {
      NavigationBarView.LABEL_VISIBILITY_UNLABELED
    } else {
      originalLabelVisibilityMode
    }
    if (view.labelVisibilityMode != targetLabelVisibility) {
      view.labelVisibilityMode = targetLabelVisibility
    }

    val maxHorizontalMargin = resources.getDimensionPixelSize(R.dimen.floating_nav_bar_margin_horizontal)
    val density = resources.displayMetrics.density
    val normalElevation = 3f * density
    val floatingElevation = 6f * density
    val lp = view.layoutParams as? ViewGroup.MarginLayoutParams

    if (view is BottomNavigationView) {
      ViewCompat.getRootWindowInsets(view)?.let {
        systemBarBottomInset = it.getInsets(WindowInsetsCompat.Type.systemBars()).bottom.coerceAtLeast(0)
      }
      val geometry = calculateFloatingNavGeometry(
        progress = progress,
        maxHorizontalMargin = maxHorizontalMargin,
        systemBarBottomInset = systemBarBottomInset,
        extraBottomSpacing = (8 * density).roundToInt()
      )
      if (lp != null) {
        lp.leftMargin = geometry.horizontalMargin
        lp.rightMargin = geometry.horizontalMargin
        lp.bottomMargin = geometry.bottomMargin
      }
      view.updatePadding(bottom = geometry.bottomPadding)
    } else if (view is NavigationRailView && lp != null) {
      val clampedProgress = progress.coerceIn(0f, 1f)
      val margin = (maxHorizontalMargin * clampedProgress).roundToInt()
      val normalWidth = resources.getDimensionPixelSize(
        com.google.android.material.R.dimen.m3expressive_nav_rail_min_width
      )
      val floatingWidth = resources.getDimensionPixelSize(R.dimen.floating_nav_rail_width)
      val parentHeight = (view.parent as? View)?.height ?: 0
      val insets = ViewCompat.getRootWindowInsets(view)?.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
      )
      val startInset = if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) insets?.right else insets?.left
      lp.marginStart = margin + (startInset ?: 0)
      lp.topMargin = margin
      lp.bottomMargin = margin
      lp.width = normalWidth + ((floatingWidth - normalWidth) * clampedProgress).roundToInt()
      val container = blurContainer ?: binding.container
      container.setPaddingRelative(
        lp.marginStart + lp.width,
        container.paddingTop,
        container.paddingEnd,
        container.paddingBottom
      )
      if (parentHeight > 0) {
        val itemHeight = floatingNavigationRailItemHeight(view)
        val itemSpacing = resources.getDimensionPixelSize(
          com.google.android.material.R.dimen.m3expressive_nav_rail_item_spacing
        )
        val floatingHeight = calculateFloatingNavigationRailHeight(
          itemCount = view.menu.size(),
          itemHeight = itemHeight,
          itemSpacing = itemSpacing,
          railWidth = floatingWidth,
          maxHeight = parentHeight - maxHorizontalMargin * 2
        )
        lp.height = parentHeight + ((floatingHeight - parentHeight) * clampedProgress).roundToInt()
      }
      view.minimumWidth = 0
      insets?.let { insets ->
        applyNavigationRailPadding(view, insets, clampedProgress)
      }
      view.layoutParams = lp
    }
    view.requestLayout()

    val outlineAttrColor = getColorByAttr(com.google.android.material.R.attr.colorOutline)
    val strokeColorInt = (outlineAttrColor and 0x00FFFFFF) or
      (((0x40 * progress).roundToInt()) shl 24)

    navPillDrawable?.setStroke(density * progress, strokeColorInt)
    navPillDrawable?.setCornerProgress(progress)

    if (!isNavigationBackgroundManagedByBlur(view)) {
      view.elevation = normalElevation + (floatingElevation - normalElevation) * progress
    }
    blurContainer?.setFloatingNavProgress(progress)
  }

  private fun installBlurContainer(): BlurCoordinatorLayout? {
    if (!OsUtils.atLeastT()) return null
    return blurContainer ?: BlurCoordinatorLayout(this).also { container ->
      replaceMainContainer(binding.container, container)
      blurContainer = container
    }
  }

  private fun replaceMainContainer(from: ViewGroup, to: ViewGroup) {
    val parent = from.parent as? ViewGroup ?: return
    val index = parent.indexOfChild(from)
    to.id = from.id
    to.layoutParams = from.layoutParams
    to.setPaddingRelative(
      from.paddingStart,
      from.paddingTop,
      from.paddingEnd,
      from.paddingBottom
    )
    while (from.childCount > 0) {
      val child = from.getChildAt(0)
      from.removeViewAt(0)
      to.addView(child)
    }
    parent.removeViewAt(index)
    parent.addView(to, index)
    to.bringChildToFront(binding.appbar)
  }

  override fun setLiftOnScrollTargetView(targetView: View) {
    binding.appbar.setLiftOnScrollTargetView(targetView)
    val recyclerView = targetView as? RecyclerView
    if (appbarScrollTarget !== recyclerView) {
      appbarScrollTarget?.removeOnScrollListener(appbarScrollListener)
      appbarScrollTarget = recyclerView
      recyclerView?.addOnScrollListener(appbarScrollListener)
    }
    targetView.doOnLayout { updateAppbarContentUnderlap() }
    updateAppbarContentUnderlap()
  }

  private fun updateAppbarContentUnderlap(isLiftedHint: Boolean = false) {
    val contentUnderlaps = isLiftedHint || isListItemUnderAppbar()
    blurContainer?.setAppbarContentUnderlap(contentUnderlaps)
    if (blurContainer?.blurEnabled != true) {
      binding.appbar.isLifted = contentUnderlaps
    }
    updateToolbarHdrHighlight(contentUnderlaps)
  }

  private fun updateToolbarHdrHighlight(
    contentUnderlaps: Boolean,
    animate: Boolean = true
  ) {
    toolbarTitleView.setHdrHighlightEnabled(
      enabled = shouldEnableToolbarHdrHighlight(
        blurEnabled = blurContainer?.blurEnabled == true,
        contentUnderlaps = contentUnderlaps,
        darkModeEnabled = resources.configuration.uiMode and
          Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
        pageTransitionRunning = isPageTransitionRunning
      ),
      animate = animate
    )
  }

  private fun captureAppbarScrollAnchor(): RecyclerViewScreenAnchor? {
    val recyclerView = appbarScrollTarget ?: return null
    val anchorView = recyclerView.findTopmostChild() ?: return null
    val adapterPosition = recyclerView.getChildAdapterPosition(anchorView)
    if (adapterPosition == RecyclerView.NO_POSITION) return null
    val recyclerLocation = IntArray(2)
    recyclerView.getLocationOnScreen(recyclerLocation)
    val anchorScreenTop = recyclerLocation[1] + anchorView.top
    return RecyclerViewScreenAnchor(
      recyclerView = recyclerView,
      adapterPosition = adapterPosition,
      screenTop = anchorScreenTop
    )
  }

  private fun restoreAppbarScrollAnchorAfterLayout(
    anchor: RecyclerViewScreenAnchor?,
    progressiveBlurActive: Boolean
  ) {
    cancelPendingAnchorRestore()
    if (anchor == null) return
    val recyclerView = anchor.recyclerView
    val observer = recyclerView.viewTreeObserver
    val rootLocation = IntArray(2)
    binding.root.getLocationOnScreen(rootLocation)
    binding.appbar.getLocationOnScreen(appbarLocation)
    val expectedRecyclerScreenTop = expectedAppbarScrollTargetScreenTop(
      rootScreenTop = rootLocation[1],
      appbarScreenTop = appbarLocation[1],
      appbarHeight = binding.appbar.height,
      progressiveBlurActive = progressiveBlurActive
    )
    var remainingSettleAttempts = MAX_APPBAR_LAYOUT_SETTLE_PREDRAWS
    val listener = object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        val recyclerLocation = IntArray(2)
        recyclerView.getLocationOnScreen(recyclerLocation)
        if (
          recyclerLocation[1] != expectedRecyclerScreenTop &&
          remainingSettleAttempts > 0
        ) {
          remainingSettleAttempts--
          binding.viewpager.requestLayout()
          recyclerView.postInvalidateOnAnimation()
          return false
        }
        cancelPendingAnchorRestore()
        val anchorView = recyclerView.layoutManager
          ?.findViewByPosition(anchor.adapterPosition)
          ?: return true
        val currentScreenTop = recyclerLocation[1] + anchorView.top
        val correction = recyclerAnchorScrollCorrection(
          previousScreenTop = anchor.screenTop,
          currentScreenTop = currentScreenTop
        )
        if (correction != 0) {
          recyclerView.scrollBy(0, correction)
        }
        updateAppbarContentUnderlap()
        return true
      }
    }
    pendingAnchorRestoreObserver = observer
    pendingAnchorRestoreListener = listener
    observer.addOnPreDrawListener(listener)
  }

  private fun cancelPendingAnchorRestore() {
    val observer = pendingAnchorRestoreObserver
    val listener = pendingAnchorRestoreListener
    if (observer?.isAlive == true && listener != null) {
      observer.removeOnPreDrawListener(listener)
    }
    pendingAnchorRestoreObserver = null
    pendingAnchorRestoreListener = null
  }

  private fun isListItemUnderAppbar(): Boolean {
    val recyclerView = appbarScrollTarget ?: return false
    if (recyclerView.canScrollVertically(-1)) return true
    val firstListItem = recyclerView.findTopmostChild() ?: return false
    binding.appbar.getLocationOnScreen(appbarLocation)
    recyclerView.getLocationOnScreen(appbarScrollTargetLocation)
    val appbarBottom = appbarLocation[1] + binding.appbar.height
    return isListItemUnderAppbar(
      appbarBottom = appbarBottom,
      firstListItemTop = appbarScrollTargetLocation[1] + firstListItem.top
    )
  }

  override fun prepareAppbarContentInset(targetView: View) {
    val initialPaddingTop = initialListTopPaddings.getOrPut(targetView) {
      targetView.paddingTop
    }
    applyHomeListTopPadding(
      targetView = targetView,
      initialPaddingTop = initialPaddingTop,
      appbarBottom = resolveCurrentAppbarInset()
    )
  }

  private fun applyHomeListTopPaddings(appbarBottom: Int) {
    initialListTopPaddings.entries.toList().forEach { (targetView, initialPaddingTop) ->
      applyHomeListTopPadding(
        targetView = targetView,
        initialPaddingTop = initialPaddingTop,
        appbarBottom = appbarBottom
      )
    }
  }

  private fun applyHomeListTopPadding(
    targetView: View,
    initialPaddingTop: Int,
    appbarBottom: Int
  ) {
    val progressiveBlurActive = blurContainer?.blurEnabled == true
    val targetPaddingTop = calculateHomeListTopPadding(
      initialPaddingTop = initialPaddingTop,
      appbarBottom = appbarBottom,
      progressiveBlurActive = progressiveBlurActive
    )
    if (targetView.paddingTop == targetPaddingTop) return
    targetView.updatePadding(top = targetPaddingTop)
  }

  private fun resolveActionBarHeight(): Int {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, typedValue, true)) {
      return 0
    }
    return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
  }

  private fun resolveCurrentAppbarInset(): Int {
    return resolveHomeListAppbarInset(
      appbarBottom = binding.appbar.bottom,
      actionBarHeight = resolveActionBarHeight(),
      systemBarTopInset = ViewCompat.getRootWindowInsets(binding.root)
        ?.getInsets(WindowInsetsCompat.Type.systemBars())
        ?.top ?: 0
    )
  }

  private fun initView() {
    val navView = binding.navView as NavigationBarView
    val floatingNavView = navView as? FloatingNavigationBar
    binding.appbar.addOnLayoutChangeListener { appbar, _, _, _, _, _, _, _, _ ->
      applyHomeListTopPaddings(appbar.bottom)
    }
    setSupportActionBar(binding.toolbar)
    binding.toolbar.isBackInvokedCallbackEnabled = false
    setupToolbarTitle()

    binding.apply {
      (container as ViewGroup).bringChildToFront(binding.appbar)
      viewpager.apply {
        adapter = object : FragmentStateAdapter(this@MainActivity) {
          override fun getItemCount(): Int {
            return HomeDestination.pageCount
          }

          override fun createFragment(position: Int): Fragment {
            return HomeDestination.requirePageIndex(position).createFragment()
          }
        }

        // 当ViewPager切换页面时，改变底部导航栏的状态
        registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
          override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            navView.menu.findItem(HomeDestination.requirePageIndex(position).navigationItemId).isChecked = true
            navView.post { bindRecentVisitsShortcuts(navView) }
            appViewModel.clearMenuState()

            val fragment = supportFragmentManager.findFragmentByTag("f$position") as? BaseFragment<*>
            fragment?.onVisibilityChanged(true)
            (0 until HomeDestination.pageCount).forEach { index ->
              if (index != position) {
                (supportFragmentManager.findFragmentByTag("f$index") as? BaseFragment<*>)?.onVisibilityChanged(false)
              }
            }
            floatingNavView?.setSelectedIndex(position, animate = true)
          }
        })

        // 禁止左右滑动
        isUserInputEnabled = false
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
          /* Do nothing */
          windowInsets
        }
      }

      navView.apply {
        if (this is BottomNavigationView) {
          (layoutParams as CoordinatorLayout.LayoutParams).also {
            it.behavior = navViewBehavior
          }
        }
        requestLayout()
        // 当 ViewPager 切换页面时，改变 ViewPager 的显示
        setOnItemSelectedListener {
          fun performClickNavigationItem(index: Int) {
            if (isPageTransitionRunning) {
              floatingNavView?.setSelectedIndex(index, animate = true)
              pendingPageIndex = index
              return
            }
            if (binding.viewpager.currentItem != index) {
              if (!binding.viewpager.isFakeDragging) {
                floatingNavView?.setSelectedIndex(index, animate = true)
                navigateToPage(index)
              }
            } else {
              val clickFlag =
                binding.viewpager.getTag(R.id.viewpager_tab_click) as? Boolean == true
              if (!clickFlag) {
                binding.viewpager.setTag(R.id.viewpager_tab_click, true)

                lifecycleScope.launch {
                  delay(200)
                  binding.viewpager.setTag(R.id.viewpager_tab_click, false)
                }
              } else if (listController?.isAllowRefreshing() == true) {
                listController?.onReturnTop()
              }
            }
          }

          HomeDestination.fromNavigationItemId(it.itemId)?.let { destination ->
            performClickNavigationItem(destination.pageIndex)
            true
          } ?: false
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        if (this is BottomNavigationView) {
          fixBottomNavigationViewInsets(this)
        } else if (this is NavigationRailView) {
          fixNavigationRailInsets(this)
        }
      }
    }

    bindRecentVisitsShortcuts(navView)
    navView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
      bindRecentVisitsShortcuts(navView)
    }
    recentVisitsViewModel.items.onEach { lists ->
      recentVisitsPopup?.takeIf { it.isShowing }?.let { popup ->
        popup.updateItems(if (popup.libraries) lists?.libraries else lists?.apps)
      }
    }.launchIn(lifecycleScope)

    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = { !isKeyboardShowing() && binding.toolbar.hasExpandedActionView() },
      handler = { binding.toolbar.collapseActionView() }
    )
    // Apply floating nav bar config before blur design replaces the background with transparency.
    initFloatingNavBar(navView)
    // Apply blur config last so it wins over the behavior/background setup above.
    setBlurDesignEnabled(GlobalValues.isBlurDesign)
  }

  private fun bindRecentVisitsShortcuts(navView: NavigationBarView) {
    for (index in 0 until navView.menu.size()) {
      val id = navView.menu.getItem(index).itemId
      val tab = navView.findViewById<View>(id) ?: continue
      TooltipCompat.setTooltipText(tab, null)
      if (id == R.id.navigation_app_list || id == R.id.navigation_classify) {
        val libraries = id == R.id.navigation_classify
        tab.setOnLongClickListener {
          (navView as? FloatingNavigationBar)?.suppressDragUntilRelease()
          showRecentVisits(tab, libraries)
          true
        }
        ViewCompat.replaceAccessibilityAction(
          tab,
          AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK,
          getString(if (libraries) R.string.recent_libraries else R.string.recent_apps)
        ) { _, _ ->
          showRecentVisits(tab, libraries)
          true
        }
      }
    }
  }

  private fun showRecentVisits(anchor: View, libraries: Boolean) {
    if (recentVisitsPopup?.isShowing == true) return
    recentVisitsPopup = createRecentVisitsPopup(anchor, libraries).also {
      val lists = recentVisitsViewModel.items.value
      it.show(if (libraries) lists?.libraries else lists?.apps)
    }
  }

  private fun createRecentVisitsPopup(anchor: View, libraries: Boolean) = RecentVisitsPopup(
    binding.root,
    binding.navView,
    anchor,
    libraries,
    ::openRecentVisit,
    recentVisitsViewModel::pin,
    { recentVisitsViewModel.remove(it.visit) }
  )

  fun pinListItem(source: View, touch: PointF?, item: RecentVisitItem): Boolean {
    if (recentVisitsPopup?.isShowing == true) return false
    val libraries = item.visit.isLibrary
    val anchor = binding.navView.findViewById<View>(if (libraries) R.id.navigation_classify else R.id.navigation_app_list) ?: return false
    if (binding.navView is BottomNavigationView) {
      navViewBehavior.slideUp(binding.navView as BottomNavigationView, false)
    }
    val popup = createRecentVisitsPopup(anchor, libraries)
    if (touch != null) {
      val token = popup.prepareDrag(item, source, touch)
      if (!startRecentVisitDrag(source, item, token, popup::finishDrag) { x, y -> popup.updateDragLocation(source, x, y) }) return false
    } else {
      // TalkBack's labelled action offers the same operation without a spatial gesture.
      recentVisitsViewModel.pin(item)
    }
    recentVisitsPopup = popup
    val lists = recentVisitsViewModel.items.value
    popup.show(if (libraries) lists?.libraries else lists?.apps)
    return true
  }

  private fun openRecentVisit(item: RecentVisitItem) {
    val visit = item.visit
    if (visit.type != null) {
      launchLibReferencePage(visit.name, visit.label ?: item.label, visit.type, visit.referredList?.toTypedArray())
    } else if (item.app != null) {
      launchDetailPage(item.app)
    } else {
      startActivity(Intent(this, AppDetailActivity::class.java).putExtra(EXTRA_PACKAGE_NAME, visit.name))
    }
  }

  private fun navigateToPage(index: Int) {
    val viewPager = binding.viewpager
    isPageTransitionRunning = true
    updateAppbarContentUnderlap()
    val direction = if (index > viewPager.currentItem) 1f else -1f
    val offset = PAGE_TRANSITION_OFFSET_DP * resources.displayMetrics.density
    viewPager.animate()
      .alpha(0f)
      .translationX(-direction * offset)
      .setDuration(PAGE_EXIT_DURATION)
      .setInterpolator(PAGE_EXIT_INTERPOLATOR)
      .setUpdateListener { blurContainer?.invalidate() }
      .withEndAction {
        viewPager.setCurrentItem(index, false)
        viewPager.translationX = direction * offset
        blurContainer?.invalidate()
        viewPager.animate()
          .alpha(1f)
          .translationX(0f)
          .setDuration(PAGE_ENTER_DURATION)
          .setInterpolator(PAGE_ENTER_INTERPOLATOR)
          .setUpdateListener { blurContainer?.invalidate() }
          .withEndAction(::finishPageTransition)
          .start()
      }
      .start()
  }

  private fun finishPageTransition() {
    isPageTransitionRunning = false
    blurContainer?.invalidate()
    val nextPageIndex = pendingPageIndex
    pendingPageIndex = null
    if (nextPageIndex != null && nextPageIndex != binding.viewpager.currentItem) {
      (binding.navView as? FloatingNavigationBar)?.setSelectedIndex(nextPageIndex, animate = true)
      navigateToPage(nextPageIndex)
      return
    }
    updateAppbarContentUnderlap()
  }

  private fun setupToolbarTitle() {
    supportActionBar?.title = null
    binding.toolbar.title = null
    renderToolbarTitle(HomeToolbarTitleState(title = LCAppUtils.buildAppTitle(this)))
    if (toolbarTitleView.parent == null) {
      binding.toolbar.addView(
        toolbarTitleView,
        Toolbar.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
          gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
      )
    }
  }

  private fun renderToolbarTitle(state: HomeToolbarTitleState) {
    toolbarTitleState = state
    toolbarTitleView.bind(state)
  }

  override fun onResume() {
    super.onResume()
    recentVisitsViewModel.refresh()
    if (appViewModel.shouldCheckPackagesPermissionOnResume()) {
      val granted =
        ContextCompat.checkSelfPermission(
          this,
          Constants.GET_INSTALLED_APPS
        ) == PackageManager.PERMISSION_GRANTED
      if (granted) {
        appViewModel.onPackagesPermissionResult(isGranted = true)
        appViewModel.initItems()
      }
    }
  }

  /**
   * Opt the window into HDR while starting at SDR headroom. The title's
   * transition updates the requested headroom only while its HDR highlight is
   * active, so an otherwise SDR page does not keep an HDR layer alive.
   * `Window.setDesiredHdrHeadroom` only exists on API 35+, so below that the
   * title falls back to plain SDR rendering.
   */
  private fun configureHdrWindow() {
    if (!OsUtils.atLeastV()) {
      return
    }
    window.colorMode = ActivityInfo.COLOR_MODE_HDR
    window.desiredHdrHeadroom = SDR_HDR_HEADROOM
  }

  private fun updateWindowHdrHeadroom(headroom: Float) {
    if (!OsUtils.atLeastV()) {
      return
    }
    window.desiredHdrHeadroom = headroom
  }

  /**
   * 覆盖掉 BottomNavigationView 内部的 OnApplyWindowInsetsListener 并避免其被软键盘顶起来
   * @see BottomNavigationView.applyWindowInsets
   */
  private fun fixBottomNavigationViewInsets(view: BottomNavigationView) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
      // 这里不直接使用 windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
      // 因为它的结果可能受到 insets 传播链上层某环节的影响，出现了错误的 navigationBarsInsets
      // 使用 WindowInsetsCompat.Type.systemBars() 以适配如 HyperOS Freeform 之类的奇怪的东西
      val navigationBarsInsets =
        ViewCompat.getRootWindowInsets(view)!!.getInsets(WindowInsetsCompat.Type.systemBars())
      systemBarBottomInset = navigationBarsInsets.bottom
      applyFloatingNavProgress(view, floatingNavProgress)
      windowInsets
    }
  }

  private fun fixNavigationRailInsets(view: NavigationRailView) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
      applyFloatingNavProgress(view, floatingNavProgress)
      windowInsets
    }
    ViewCompat.requestApplyInsets(view)
  }

  private fun applyNavigationRailPadding(
    view: NavigationRailView,
    insets: androidx.core.graphics.Insets,
    progress: Float
  ) {
    val floatingProgress = progress.coerceIn(0f, 1f)
    val attachedProgress = 1f - floatingProgress
    val contentMarginTop = resources.getDimensionPixelSize(
      com.google.android.material.R.dimen.m3expressive_nav_rail_content_margin_top
    )
    val floatingWidth = resources.getDimensionPixelSize(R.dimen.floating_nav_rail_width)
    val floatingContentPadding = ((floatingWidth - floatingNavigationRailItemHeight(view)) / 2).coerceAtLeast(0)
    view.updatePadding(
      top = (
        (insets.top + contentMarginTop) * attachedProgress +
          floatingContentPadding * floatingProgress
        ).roundToInt(),
      bottom = (
        insets.bottom * attachedProgress +
          floatingContentPadding * floatingProgress
        ).roundToInt()
    )
  }

  private fun floatingNavigationRailItemHeight(view: NavigationRailView): Int {
    val minimumHeight = resources.getDimensionPixelSize(
      com.google.android.material.R.dimen.m3expressive_nav_rail_item_min_height
    )
    return maxOf(minimumHeight, view.itemActiveIndicatorWidth + view.itemPaddingTop + view.itemPaddingBottom)
  }

  private fun handleIntent(intent: Intent) {
    HomeDestination.fromLaunchAction(intent.action)?.let {
      (binding.navView as? FloatingNavigationBar)?.setSelectedIndex(it.pageIndex, animate = false)
      binding.viewpager.setCurrentItem(it.pageIndex, false)
    }
    Telemetry.recordEvent(
      Constants.Event.LAUNCH_ACTION,
      mapOf(Telemetry.Param.VALUE to intent.action.toString())
    )
  }

  private fun initObserver() {
    appViewModel.apply {
      if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.FIRST_LAUNCH)) {
        initItems()
      }

      effect.onEach {
        when (it) {
          is HomeViewModel.Effect.ReloadApps -> {
            binding.viewpager.setCurrentItem(HomeDestination.APP_LIST.pageIndex, true)
          }

          is HomeViewModel.Effect.UpdateAppListStatus -> {
            if (it.status == STATUS_START_INIT) {
              doOnMainThreadIdle {
                hideNavigationView()
              }
            } else if (it.status == STATUS_INIT_END) {
              doOnMainThreadIdle {
                showNavigationView()
              }
            }
          }

          else -> {}
        }
      }.launchIn(lifecycleScope)

      toolbarLoading.onEach {
        renderToolbarTitle(toolbarTitleState.withLoading(it))
      }.launchIn(lifecycleScope)
    }
    appViewModel.packageChanges.onEach {
      Timber.d("MainActivity received package change: $it")
      appViewModel.packageChanged(it)
    }.launchIn(lifecycleScope)
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val result = super.onPrepareOptionsMenu(menu)
    restoreToolbarMenuState()
    return result
  }

  private fun restoreToolbarMenuState() {
    if (!isBindingInitialized()) {
      return
    }
    // Only restore state if menu exists and has search item
    binding.toolbar.post {
      val searchItem = binding.toolbar.menu?.findItem(R.id.search)
      val searchMenuState = appViewModel.getToolbarSearchMenuState()
      if (searchMenuState.isExpanded) {
        val searchView = searchItem?.actionView as? SearchView
        searchView?.let {
          if (!searchItem.isActionViewExpanded) {
            searchItem.expandActionView()
          }
        }
      }
    }
  }
}

internal data class FloatingNavGeometry(
  val horizontalMargin: Int,
  val bottomMargin: Int,
  val bottomPadding: Int
)

internal fun calculateFloatingNavGeometry(
  progress: Float,
  maxHorizontalMargin: Int,
  systemBarBottomInset: Int,
  extraBottomSpacing: Int
): FloatingNavGeometry {
  val clampedProgress = progress.coerceIn(0f, 1f)
  val hMargin = (maxHorizontalMargin * clampedProgress).roundToInt()
  val inset = systemBarBottomInset.coerceAtLeast(0)
  val bMargin = ((maxOf(inset, maxHorizontalMargin) + extraBottomSpacing) * clampedProgress).roundToInt()
  val bPadding = inset - (inset * clampedProgress).roundToInt()
  return FloatingNavGeometry(
    horizontalMargin = hMargin,
    bottomMargin = bMargin,
    bottomPadding = bPadding
  )
}

internal fun calculateFloatingNavigationRailHeight(
  itemCount: Int,
  itemHeight: Int,
  itemSpacing: Int,
  railWidth: Int,
  maxHeight: Int
): Int {
  val count = itemCount.coerceAtLeast(0)
  val endPadding = ((railWidth - itemHeight) / 2).coerceAtLeast(0)
  val contentHeight = itemHeight * count + itemSpacing * (count - 1).coerceAtLeast(0) + endPadding * 2
  return minOf(contentHeight, maxHeight.coerceAtLeast(0))
}

internal fun calculateHomeListTopPadding(
  initialPaddingTop: Int,
  appbarBottom: Int,
  progressiveBlurActive: Boolean
): Int {
  return initialPaddingTop + if (progressiveBlurActive) appbarBottom.coerceAtLeast(0) else 0
}

internal fun resolveHomeListAppbarInset(
  appbarBottom: Int,
  actionBarHeight: Int,
  systemBarTopInset: Int
): Int {
  return if (appbarBottom > 0) {
    appbarBottom
  } else {
    actionBarHeight.coerceAtLeast(0) + systemBarTopInset.coerceAtLeast(0)
  }
}

internal fun isListItemUnderAppbar(appbarBottom: Int, firstListItemTop: Int?): Boolean {
  return appbarBottom > 0 && firstListItemTop != null && firstListItemTop < appbarBottom
}

internal fun shouldEnableToolbarHdrHighlight(
  blurEnabled: Boolean,
  contentUnderlaps: Boolean,
  darkModeEnabled: Boolean,
  pageTransitionRunning: Boolean = false
): Boolean = blurEnabled && contentUnderlaps && darkModeEnabled && !pageTransitionRunning

internal fun recyclerAnchorScrollCorrection(
  previousScreenTop: Int,
  currentScreenTop: Int
): Int = currentScreenTop - previousScreenTop

internal fun expectedAppbarScrollTargetScreenTop(
  rootScreenTop: Int,
  appbarScreenTop: Int,
  appbarHeight: Int,
  progressiveBlurActive: Boolean
): Int {
  return if (progressiveBlurActive) {
    rootScreenTop
  } else {
    appbarScreenTop + appbarHeight.coerceAtLeast(0)
  }
}

private const val MAX_APPBAR_LAYOUT_SETTLE_PREDRAWS = 4

private fun RecyclerView.findTopmostChild(): View? {
  var topmostChild: View? = null
  for (index in 0 until childCount) {
    val child = getChildAt(index)
    if (topmostChild == null || child.top < topmostChild.top) {
      topmostChild = child
    }
  }
  return topmostChild
}
