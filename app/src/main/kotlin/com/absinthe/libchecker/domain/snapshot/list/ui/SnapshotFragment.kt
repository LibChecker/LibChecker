package com.absinthe.libchecker.domain.snapshot.list.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.domain.app.list.GetRandomAppIconUseCase
import com.absinthe.libchecker.domain.home.presentation.HomeViewModel
import com.absinthe.libchecker.domain.home.ui.INavViewContainer
import com.absinthe.libchecker.domain.snapshot.album.ui.AlbumActivity
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.domain.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotCapturePlan
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotDashboardAction
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotSystemPropDisplayData
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.domain.snapshot.list.ui.view.SnapshotDashboardView
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotDashboardDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.timenode.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.services.ShootService
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.animator.ParticleRemoveItemAnimator
import com.absinthe.libchecker.ui.base.BaseActivity
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.BaseListControllerFragment
import com.absinthe.libchecker.ui.base.IAppBarContainer
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment :
  BaseListControllerFragment<FragmentSnapshotBinding>(),
  SearchView.OnQueryTextListener {

  private val viewModel: SnapshotViewModel by activityViewModel()
  private val getRandomAppIcon: GetRandomAppIconUseCase by inject()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val buildSnapshotDashboardDisplayData: BuildSnapshotDashboardDisplayDataUseCase by inject()
  private val adapter by lazy(LazyThreadSafetyMode.NONE) {
    SnapshotAdapter(buildSnapshotItemDisplayData)
  }
  private val particleItemAnimator = ParticleRemoveItemAnimator()

  private val shootListener = object : OnShootListener.Stub() {
    override fun onShootFinished(timestamp: Long) {
      lifecycleScope.launch(Dispatchers.Main) {
        viewModel.onSnapshotCaptureFinished(timestamp)
      }
    }

    override fun onProgressUpdated(progress: Int) {
      lifecycleScope.launch(Dispatchers.Main) {
        flip(VF_LOADING)
        runCatching {
          binding.progressIndicator.setProgressCompat(progress, true)
        }
      }
    }
  }
  private val shootServiceController by lazy(LazyThreadSafetyMode.NONE) {
    SnapshotShootServiceController(shootListener)
  }
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
  private var advancedMenuBSDFragment: SnapshotMenuBSDFragment? = null

  private lateinit var layoutManager: RecyclerView.LayoutManager

  override fun init() {
    val context = (this.context as? BaseActivity<*>) ?: return
    viewModel.onSnapshotServiceStateObserved(ShootService.isComputing)

    val dashboard =
      SnapshotDashboardView(ContextThemeWrapper(context, R.style.AlbumMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          it.setMargins(8.dp, 2.dp, 8.dp, 2.dp)
        }
      }

    var dashboardTimestampText: CharSequence = ""
    var dashboardAppsCountText: CharSequence = ""
    var dashboardSystemProps: List<SnapshotSystemPropDisplayData> = emptyList()
    fun changeTimeNode() {
      lifecycleScope.launch(Dispatchers.IO) {
        val timeStampList = viewModel.getTimeStamps()
        withContext(Dispatchers.Main) {
          TimeNodeBottomSheetDialogFragment.newInstance(ArrayList(timeStampList))
            .apply {
              setOnItemClickListener { position ->
                val item = timeStampList[position]
                viewModel.refreshSnapshotTimestamp(item.timestamp, shouldClearDiff = true)
                flip(VF_LOADING)
                dismiss()
              }
            }
            .show(
              context.supportFragmentManager,
              TimeNodeBottomSheetDialogFragment::class.java.name
            )
        }
      }
    }
    fun handleDashboardAction(action: SnapshotDashboardAction) {
      when (action) {
        SnapshotDashboardAction.OpenAlbum -> {
          startActivity(Intent(context, AlbumActivity::class.java))
        }

        SnapshotDashboardAction.ChangeTimestamp -> changeTimeNode()
      }
    }
    fun renderDashboard() {
      dashboard.bind(
        data = buildSnapshotDashboardDisplayData(
          BuildSnapshotDashboardDisplayDataUseCase.Request(
            timestampText = dashboardTimestampText,
            appsCountText = dashboardAppsCountText,
            systemProps = dashboardSystemProps
          )
        ),
        onAction = ::handleDashboardAction
      )
    }
    renderDashboard()

    val emptyView = SnapshotEmptyView(context).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER_HORIZONTAL
      }
      addPaddingTop(96.dp)
      if (viewModel.selectedSnapshotTimestamp == 0L) {
        text.text = getString(R.string.snapshot_no_snapshot)
      }
      updateContentDescription()
    }

    adapter.apply {
      dashboard.also {
        setHeaderView(it)
      }
      stateView = emptyView
      isStateViewEnable = true
      setOnItemClickListener { _, view, position ->
        if (AntiShakeUtils.isInvalidClick(view)) {
          return@setOnItemClickListener
        }

        val item = getItem(position)
        if (item.deleted || item.newInstalled || item.isNothingChanged()) {
          SnapshotNoDiffBSDFragment.newInstance(item).also {
            it.show(context.supportFragmentManager, SnapshotNoDiffBSDFragment::class.java.name)
          }
        } else {
          val intent = Intent(context, SnapshotDetailActivity::class.java)
            .putExtras(
              Bundle().apply {
                putSerializable(EXTRA_ENTITY, item)
              }
            )
          startActivity(intent)
        }
      }
    }

    binding.apply {
      list.apply {
        adapter = this@SnapshotFragment.adapter
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
        scrollToPosition(0)
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
        setOnDisplayedChildChangedListener {
          adapter.setSpaceFooterView()
        }
      }
      loading.setAppIconHighlightProvider { getRandomAppIcon() }
    }

    viewModel.apply {
      allSnapshots.onEach {
        if (viewModel.shouldAutoCompareSnapshot()) {
          viewModel.refreshSelectedSnapshot()
        }
      }.launchIn(lifecycleScope)
      snapshotDiffItemsUpdates.onEach {
        updateItems()

        lifecycleScope.launch(Dispatchers.IO) {
          delay(250)

          doOnMainThreadIdle {
            if (listControllerHost?.isCurrentListController(this@SnapshotFragment) == true &&
              !binding.list.canScrollVertically(-1)
            ) {
              (activity as? INavViewContainer)?.showNavigationView()
            }
          }
        }
      }.launchIn(lifecycleScope)
    }
    homeViewModel.effect.onEach {
      when (it) {
        is HomeViewModel.Effect.PackageChanged -> {
          if (allowRefreshing) {
            viewModel.handlePackageChanged(it.packageChangeState)
          }
        }

        else -> {}
      }
    }.launchIn(lifecycleScope)
    viewModel.effect.onEach {
      when (it) {
        is SnapshotViewModel.Effect.DashboardCountChange -> {
          dashboardAppsCountText = buildSnapshotDashboardDisplayData.formatAppsCount(
            snapshotCount = it.snapshotCount,
            appCount = it.appCount
          )
          renderDashboard()
        }

        is SnapshotViewModel.Effect.TimeStampChange -> {
          if (it.timestamp != 0L) {
            dashboardTimestampText = viewModel.getFormatDateString(it.timestamp)
            renderDashboard()
            updateSystemProps(it.timestamp) { props ->
              dashboardSystemProps = props
              renderDashboard()
            }
          } else {
            dashboardTimestampText = buildSnapshotDashboardDisplayData.noSnapshotTimestampText()
            dashboardSystemProps = emptyList()
            renderDashboard()
            viewModel.clearSnapshotDiffItems()
            flip(VF_LIST)
          }
        }

        is SnapshotViewModel.Effect.ComparingProgressChange -> {
          binding.progressIndicator.setProgressCompat(it.progress, it.progress != 1)
        }
      }
    }.launchIn(lifecycleScope)

    viewModel.changeTimeStamp(viewModel.selectedSnapshotTimestamp)
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (OsUtils.atLeastT()) {
      requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
          Timber.d("Request post notification: $isGranted")
        }
    }
  }

  override fun onResume() {
    super.onResume()
    val context = context ?: return

    if (!shootServiceController.isStarted && isFragmentVisible()) {
      shootServiceController.startAndBind(context)
    }

    if (viewModel.consumeTrackItemsChanged()) {
      flip(VF_LOADING)
      viewModel.compareDiff(viewModel.selectedSnapshotTimestamp)
    }

    if (viewModel.currentTimeStamp != viewModel.selectedSnapshotTimestamp) {
      flip(VF_LOADING)
      viewModel.refreshSelectedSnapshot(shouldClearDiff = true)
    }

    if (viewModel.shouldAutoCompareSnapshot()) {
      if (!viewModel.isComparingActive()) {
        flip(VF_LIST)
      } else {
        flip(VF_LOADING)
      }
    }

    if (hasPackageChanged()) {
      viewModel.compareDiff(viewModel.selectedSnapshotTimestamp)
    }
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)

    if (binding.vfContainer.displayedChild == VF_LOADING) {
      binding.loading.start()
    }
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
    binding.loading.stop()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    context?.applicationContext?.let {
      shootServiceController.release(it)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    binding.list.layoutManager = getSuitableLayoutManagerImpl(newConfig)
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.snapshot_menu, menu)
    this.menu = menu.apply {
      findItem(R.id.save)?.isVisible = binding.vfContainer.displayedChild == VF_LIST
    }
    val context = context ?: return
    val searchView = SearchView(context).apply {
      setIconifiedByDefault(false)
      setOnQueryTextListener(this@SnapshotFragment)
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
    val context = (this.context as? BaseActivity<*>) ?: return false
    if (menuItem.itemId == R.id.save) {
      if (viewModel.isComparingActive() || shootServiceController.isShooting) {
        return false
      }
      fun computeNewSnapshot(dropPrevious: Boolean = false) {
        flip(VF_LOADING)
        (context as INavViewContainer).showNavigationView()
        shootServiceController.start(context)
        if (!shootServiceController.computeSnapshot(dropPrevious)) {
          Toasty.showShort(context, "Snapshot service error")
        }
        viewModel.onSnapshotCaptureStarted()

        if (OsUtils.atLeastT()) {
          if (ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
          ) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
              requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          }
        }

        Telemetry.recordEvent(
          Constants.Event.SNAPSHOT_CLICK,
          mapOf("Action" to "Click to Save")
        )
      }

      when (val plan = viewModel.buildSnapshotCapturePlan()) {
        is SnapshotCapturePlan.Capture -> computeNewSnapshot(plan.dropPrevious)

        is SnapshotCapturePlan.ConfirmKeepPrevious -> {
          BaseAlertDialogBuilder(context)
            .setTitle(R.string.dialog_title_keep_previous_snapshot)
            .setMessage(R.string.dialog_message_keep_previous_snapshot)
            .setView(buildSnapshotSchemeTipView(context, plan.bridgeUri))
            .setPositiveButton(R.string.btn_keep) { _, _ ->
              computeNewSnapshot(false)
            }
            .setNegativeButton(R.string.btn_drop) { _, _ ->
              computeNewSnapshot(true)
            }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
        }

        SnapshotCapturePlan.NoAction -> Unit
      }
    } else if (menuItem.itemId == R.id.advanced) {
      activity?.let {
        advancedMenuBSDFragment?.dismiss()
        advancedMenuBSDFragment = SnapshotMenuBSDFragment().apply {
          setOnDismissListener { optionsDiff ->
            if (optionsDiff > 0) {
              updateItems(highlightRefresh = true)
            }
            advancedMenuBSDFragment = null
          }
        }
        advancedMenuBSDFragment?.show(
          it.supportFragmentManager,
          SnapshotMenuBSDFragment::class.java.name
        )
      }
    }
    return true
  }

  private fun buildSnapshotSchemeTipView(context: Context, scheme: String): TextView {
    return TextView(context).also {
      it.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      it.setPadding(24.dp, 0, 24.dp, 0)
      it.text = HtmlCompat.fromHtml(getString(R.string.snapshot_scheme_tip, scheme), 0)
      it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      it.setLongClickCopiedToClipboard(scheme)
    }
  }

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      adapter.setSpaceFooterView()
    }
  }

  private fun flip(child: Int) {
    if (isDetached || !isBindingInitialized()) {
      return
    }
    allowRefreshing = child == VF_LIST
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      if (isResumed) {
        binding.loading.start()
      }
      menu?.findItem(R.id.save)?.isVisible = false
      menu?.findItem(R.id.search)?.isVisible = false
    } else {
      binding.loading.stop()
      binding.list.scrollToPosition(0)
      menu?.findItem(R.id.save)?.isVisible = true
      menu?.findItem(R.id.search)?.isVisible = true
    }

    binding.vfContainer.displayedChild = child
  }

  override fun getSuitableLayoutManager() = binding.list.layoutManager

  private fun getSuitableLayoutManagerImpl(configuration: Configuration): RecyclerView.LayoutManager {
    layoutManager = when (configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())

      Configuration.ORIENTATION_LANDSCAPE ->
        StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

      else -> throw IllegalStateException("Wrong orientation at SnapshotFragment.")
    }
    return layoutManager
  }

  override fun onReturnTop() {
    val context = context ?: return
    if (binding.list.canScrollVertically(-1)) {
      binding.list.smoothScrollToPosition(0)
    } else {
      flip(VF_LOADING)
      viewModel.compareDiff(viewModel.selectedSnapshotTimestamp)
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String?): Boolean {
    val keyword = newText.orEmpty()
    if (viewModel.updateSnapshotSearchKeyword(keyword)) {
      updateItems(highlightRefresh = true)
    }
    return false
  }

  private fun updateItems(highlightRefresh: Boolean = false) = lifecycleScope.launch(Dispatchers.Main) {
    val updatePlan = viewModel.buildSnapshotListUpdatePlan(
      currentItems = adapter.data,
      highlightRefresh = highlightRefresh
    )
    particleItemAnimator.prepareParticleRemovals(updatePlan.particleRemovalItemIds)

    adapter.bind(updatePlan.renderState)
    adapter.setDiffNewData(updatePlan.items.toMutableList()) {
      if (isDetached) {
        return@setDiffNewData
      }
      isListReady = true
      flip(VF_LIST)
      adapter.setSpaceFooterView()

      if (highlightRefresh) {
        // noinspection NotifyDataSetChanged
        adapter.notifyDataSetChanged()
      }
    }
  }

  private fun updateSystemProps(
    timestamp: Long,
    onSystemPropsReady: (List<SnapshotSystemPropDisplayData>) -> Unit
  ) {
    lifecycleScope.launch(Dispatchers.IO) {
      val displayedSystemProps = viewModel.getSystemPropDisplayData(timestamp)
      launch(Dispatchers.Main) {
        onSystemPropsReady(displayedSystemProps)
      }
    }
  }
}
