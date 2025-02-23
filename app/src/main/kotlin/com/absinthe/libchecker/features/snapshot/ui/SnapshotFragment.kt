package com.absinthe.libchecker.features.snapshot.ui

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
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
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.LCUris
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.features.album.ui.AlbumActivity
import com.absinthe.libchecker.features.home.HomeViewModel
import com.absinthe.libchecker.features.home.INavViewContainer
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.features.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotAdapter
import com.absinthe.libchecker.features.snapshot.ui.adapter.SnapshotDiffUtil
import com.absinthe.libchecker.features.snapshot.ui.view.SnapshotDashboardView
import com.absinthe.libchecker.services.IShootService
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.services.ShootService
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
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
import com.absinthe.libchecker.utils.extensions.getDimensionByAttr
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.extensions.setSpaceFooterView
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.widget.borderview.BorderView
import timber.log.Timber

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment :
  BaseListControllerFragment<FragmentSnapshotBinding>(),
  SearchView.OnQueryTextListener {

  private val viewModel: SnapshotViewModel by activityViewModels()
  private val adapter = SnapshotAdapter()
  private var isSnapshotDatabaseItemsReady = false
  private var dropPrevious = false
  private var shouldCompare = true and ShootService.isComputing.not()
  private var shootServiceStarted = false
  private var keyword: String = ""
  private var currentTimeStamp = GlobalValues.snapshotTimestamp
  private var items = emptyList<SnapshotDiffItem>()

  private var shootBinder: IShootService? = null
  private val shootListener = object : OnShootListener.Stub() {
    override fun onShootFinished(timestamp: Long) {
      lifecycleScope.launch(Dispatchers.Main) {
        viewModel.changeTimeStamp(timestamp)
        compareDiff()
        shouldCompare = true
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
  private val shootServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
      shootServiceStarted = true
      if (shootBinder == null && service?.pingBinder() == true) {
        shootBinder = IShootService.Stub.asInterface(service).also {
          it.registerOnShootOverListener(shootListener)
        }
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      shootServiceStarted = false
      shootBinder = null
    }
  }
  private val packageQueue by lazy { LinkedBlockingQueue<Pair<String?, String?>>() }
  private var dequeuePackagesJob: Job? = null
  private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
  private var advancedMenuBSDFragment: SnapshotMenuBSDFragment? = null

  private lateinit var layoutManager: RecyclerView.LayoutManager

  override fun init() {
    val context = (this.context as? BaseActivity<*>) ?: return

    val dashboard =
      SnapshotDashboardView(ContextThemeWrapper(context, R.style.AlbumMaterialCard)).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
          it.setMargins(8.dp, 0, 8.dp, 0)
        }
      }

    dashboard.setOnClickListener {
      startActivity(Intent(context, AlbumActivity::class.java))
    }

    dashboard.container.apply {
      fun changeTimeNode() {
        lifecycleScope.launch(Dispatchers.IO) {
          val timeStampList = viewModel.repository.getTimeStamps()
          val dialog = TimeNodeBottomSheetDialogFragment.newInstance(ArrayList(timeStampList))
            .apply {
              setOnItemClickListener { position ->
                val item = timeStampList[position]
                GlobalValues.snapshotTimestamp = item.timestamp
                lifecycleScope.launch(Dispatchers.Main) {
                  viewModel.changeTimeStamp(item.timestamp)
                  flip(VF_LOADING)
                  dismiss()
                }
                viewModel.compareDiff(item.timestamp, shouldClearDiff = true)
              }
            }
          dialog.show(
            context.supportFragmentManager,
            TimeNodeBottomSheetDialogFragment::class.java.name
          )
        }
      }

      tvSnapshotTimestampText.setOnClickListener {
        if (AntiShakeUtils.isInvalidClick(it)) {
          return@setOnClickListener
        }
        changeTimeNode()
      }
      arrow.setOnClickListener {
        if (AntiShakeUtils.isInvalidClick(it)) {
          return@setOnClickListener
        }
        changeTimeNode()
      }
    }

    val emptyView = SnapshotEmptyView(context).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).also {
        it.gravity = Gravity.CENTER_HORIZONTAL
      }
      addPaddingTop(96.dp)
      if (GlobalValues.snapshotTimestamp == 0L) {
        text.text = getString(R.string.snapshot_no_snapshot)
      }
    }

    adapter.apply {
      headerWithEmptyEnable = true
      dashboard.also {
        setHeaderView(it)
        (it.parent as? ViewGroup)?.apply {
          clipToPadding = false
          clipChildren = false
        }
      }
      setEmptyView(emptyView)
      setDiffCallback(SnapshotDiffUtil())
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
            .putExtras(bundleOf(EXTRA_ENTITY to item))
          startActivity(intent)
        }
      }
    }

    binding.apply {
      list.apply {
        adapter = this@SnapshotFragment.adapter
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
    }

    viewModel.apply {
      allSnapshots.onEach {
        if (shouldCompare) {
          compareDiff()
        }
      }.launchIn(lifecycleScope)
      snapshotDiffItemsFlow.onEach {
        items = it
        updateItems(it)

        lifecycleScope.launch(Dispatchers.IO) {
          delay(250)

          doOnMainThreadIdle {
            if (this@SnapshotFragment == homeViewModel.controller &&
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
            packageQueue.offer(it.packageName to it.action)
            dequeuePackages()
            viewModel.getDashboardCount(GlobalValues.snapshotTimestamp, true)
          }
        }

        else -> {}
      }
    }.launchIn(lifecycleScope)
    viewModel.effect.onEach {
      when (it) {
        is SnapshotViewModel.Effect.DashboardCountChange -> {
          dashboard.container.tvSnapshotAppsCountText.text =
            String.format(Locale.getDefault(), "%d / %d", it.snapshotCount, it.appCount)
        }

        is SnapshotViewModel.Effect.TimeStampChange -> {
          currentTimeStamp = it.timestamp
          if (it.timestamp != 0L) {
            dashboard.container.tvSnapshotTimestampText.text = viewModel.getFormatDateString(it.timestamp)
          } else {
            dashboard.container.tvSnapshotTimestampText.text = getString(R.string.snapshot_none)
            viewModel.snapshotDiffItemsFlow.emit(emptyList())
            flip(VF_LIST)
          }
        }

        is SnapshotViewModel.Effect.DiffItemChange -> {
          val newItems = adapter.data.toMutableList()
          newItems.removeIf { item -> item.packageName == it.item.packageName }
          newItems.add(it.item)
          items = newItems
          updateItems(newItems)
        }

        is SnapshotViewModel.Effect.ComparingProgressChange -> {
          binding.progressIndicator.setProgressCompat(it.progress, it.progress != 1)
        }

        else -> {}
      }
    }.launchIn(lifecycleScope)

    viewModel.changeTimeStamp(GlobalValues.snapshotTimestamp)
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

    if (!shootServiceStarted && isFragmentVisible()) {
      context?.applicationContext?.also {
        val intent = Intent(it, ShootService::class.java).apply {
          setPackage(it.packageName)
        }
        it.startService(intent)
        it.bindService(
          intent,
          shootServiceConnection,
          Service.BIND_AUTO_CREATE
        )
        shootServiceStarted = true
      }
    }

    if (GlobalValues.trackItemsChanged) {
      GlobalValues.trackItemsChanged = false
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    }

    if (currentTimeStamp != GlobalValues.snapshotTimestamp) {
      viewModel.changeTimeStamp(GlobalValues.snapshotTimestamp)
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp, shouldClearDiff = true)
    }

    if (shouldCompare) {
      if (!viewModel.isComparingActive()) {
        flip(VF_LIST)
      } else {
        flip(VF_LOADING)
      }
    }

    if (hasPackageChanged()) {
      viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    }
    (activity as? IAppBarContainer)?.setLiftOnScrollTargetView(binding.list)
  }

  override fun onPause() {
    super.onPause()
    advancedMenuBSDFragment?.dismiss()
    advancedMenuBSDFragment = null
  }

  override fun onDestroyView() {
    super.onDestroyView()
    shootBinder?.let {
      context?.applicationContext?.let { ctx ->
        it.unregisterOnShootOverListener(shootListener)
        if (ShootService.isComputing.not()) {
          runCatching {
            ctx.unbindService(shootServiceConnection)
            ctx.stopService(
              Intent(
                ctx,
                ShootService::class.java
              )
            )
          }
        }
      }
      shootBinder = null
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
      if (viewModel.isComparingActive() || shootBinder?.isShooting == true) {
        return false
      }
      fun computeNewSnapshot(dropPrevious: Boolean = false) {
        flip(VF_LOADING)
        (context as INavViewContainer).showNavigationView()
        this@SnapshotFragment.dropPrevious = dropPrevious
        ContextCompat.startForegroundService(
          context,
          Intent(context, ShootService::class.java)
        )
        shootBinder?.computeSnapshot(dropPrevious) ?: run {
          Timber.w("shoot binder is null")
          Toasty.showShort(context, "Snapshot service error")
        }
        shouldCompare = false

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

      if (GlobalValues.snapshotTimestamp == 0L) {
        computeNewSnapshot()
      } else {
        val scheme = Uri.Builder().scheme(LCUris.SCHEME)
          .authority(LCUris.Bridge.AUTHORITY)
          .appendQueryParameter(LCUris.Bridge.PARAM_ACTION, LCUris.Bridge.ACTION_SHOOT)
          .appendQueryParameter(
            LCUris.Bridge.PARAM_AUTHORITY,
            GlobalValues.generateAuthKey().toString()
          )
          .appendQueryParameter(LCUris.Bridge.PARAM_DROP_PREVIOUS, false.toString())
          .build()
          .toString()
        val tipView = TextView(context).also {
          it.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
          )
          val paddingHorizontal =
            context.getDimensionByAttr(com.google.android.material.R.attr.dialogPreferredPadding)
              .toInt()
          it.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
          it.text =
            HtmlCompat.fromHtml(getString(R.string.snapshot_scheme_tip, scheme), 0)
          it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
          it.setLongClickCopiedToClipboard(scheme)
        }
        when (GlobalValues.snapshotKeep) {
          Constants.SNAPSHOT_DEFAULT -> {
            BaseAlertDialogBuilder(context)
              .setTitle(R.string.dialog_title_keep_previous_snapshot)
              .setMessage(R.string.dialog_message_keep_previous_snapshot)
              .setView(tipView)
              .setPositiveButton(R.string.btn_keep) { _, _ ->
                computeNewSnapshot(false)
              }
              .setNegativeButton(R.string.btn_drop) { _, _ ->
                computeNewSnapshot(true)
              }
              .setNeutralButton(android.R.string.cancel, null)
              .show()
          }

          Constants.SNAPSHOT_KEEP -> computeNewSnapshot(false)

          Constants.SNAPSHOT_DISCARD -> computeNewSnapshot(true)
        }
      }
    } else if (menuItem.itemId == R.id.advanced) {
      activity?.let {
        advancedMenuBSDFragment?.dismiss()
        advancedMenuBSDFragment = SnapshotMenuBSDFragment().apply {
          setOnDismissListener { optionsDiff ->
            if (optionsDiff > 0) {
              updateItems(adapter.data, true)
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

  override fun onVisibilityChanged(visible: Boolean) {
    super.onVisibilityChanged(visible)
    if (visible) {
      adapter.setSpaceFooterView()
    }
  }

  private fun flip(child: Int) {
    if (isDetached) {
      return
    }
    allowRefreshing = child == VF_LIST
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      binding.loading.resumeAnimation()
      menu?.findItem(R.id.save)?.isVisible = false
      menu?.findItem(R.id.search)?.isVisible = false
    } else {
      binding.loading.pauseAnimation()
      binding.list.scrollToPosition(0)
      menu?.findItem(R.id.save)?.isVisible = true
      menu?.findItem(R.id.search)?.isVisible = true
    }

    binding.vfContainer.displayedChild = child
  }

  private fun compareDiff() {
    viewModel.changeTimeStamp(GlobalValues.snapshotTimestamp)
    isSnapshotDatabaseItemsReady = true

    viewModel.getDashboardCount(GlobalValues.snapshotTimestamp, true)
    viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    isSnapshotDatabaseItemsReady = false
  }

  private fun dequeuePackages() {
    if (dequeuePackagesJob?.isActive == true) {
      return
    }
    dequeuePackagesJob = lifecycleScope.launch(Dispatchers.IO) {
      while (isActive) {
        packageQueue.take()?.first?.let {
          Timber.d("Dequeue package: $it")
          viewModel.compareItemDiff(GlobalValues.snapshotTimestamp, it)
        }
      }
    }
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
    if (binding.list.canScrollVertically(-1)) {
      binding.list.smoothScrollToPosition(0)
    } else {
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    return false
  }

  override fun onQueryTextChange(newText: String?): Boolean {
    if (keyword != newText) {
      keyword = newText.orEmpty()
      adapter.highlightText = keyword
      val list = if (keyword.isEmpty()) {
        items
      } else {
        items.asSequence()
          .filter {
            it.packageName.contains(keyword, ignoreCase = true) ||
              it.labelDiff.old.contains(keyword, ignoreCase = true) ||
              it.labelDiff.new?.contains(keyword, ignoreCase = true) == true
          }.toList()
      }
      updateItems(list, true)
    }
    return false
  }

  private fun updateItems(list: List<SnapshotDiffItem>, highlightRefresh: Boolean = false) = lifecycleScope.launch(Dispatchers.Main) {
    val filterList = list.toMutableList()
    if (GlobalValues.snapshotOptions.and(SnapshotOptions.HIDE_NO_COMPONENT_CHANGES) != 0) {
      filterList.removeAll { it.isNothingChanged() }
    }
    adapter.setDiffNewData(
      filterList.sortedByDescending { it.updateTime }
        .toMutableList()
    ) {
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
}
