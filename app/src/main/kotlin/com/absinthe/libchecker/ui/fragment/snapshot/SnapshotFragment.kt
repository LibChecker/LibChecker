package com.absinthe.libchecker.ui.fragment.snapshot

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.base.BaseActivity
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.recyclerview.diff.SnapshotDiffUtil
import com.absinthe.libchecker.services.IShootService
import com.absinthe.libchecker.services.OnShootListener
import com.absinthe.libchecker.services.ShootService
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.main.INavViewContainer
import com.absinthe.libchecker.ui.snapshot.AlbumActivity
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.addPaddingBottom
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.snapshot.SnapshotDashboardView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.HomeViewModel
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.widget.borderview.BorderView
import timber.log.Timber
import java.util.LinkedList
import java.util.Queue

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment : BaseListControllerFragment<FragmentSnapshotBinding>() {

  private val viewModel: SnapshotViewModel by activityViewModels()
  private val adapter by unsafeLazy { SnapshotAdapter(lifecycleScope) }
  private var isSnapshotDatabaseItemsReady = false
  private var dropPrevious = false
  private var shouldCompare = true and ShootService.isComputing.not()
  private var hasAddedListBottomPadding = false

  private var shootBinder: IShootService? = null
  private val shootListener = object : OnShootListener.Stub() {
    override fun onShootFinished(timestamp: Long) {
      lifecycleScope.launch(Dispatchers.Main) {
        viewModel.timestamp.value = timestamp
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
      if (shootBinder == null) {
        shootBinder = IShootService.Stub.asInterface(service).also {
          it.registerOnShootOverListener(shootListener)
        }
      }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      shootBinder = null
    }
  }
  private val packageQueue: Queue<Pair<String?, String?>> by lazy { LinkedList() }

  override fun init() {
    setHasOptionsMenu(true)

    val context = (this.context as? BaseActivity<*>) ?: return
    context.applicationContext.also {
      val intent = Intent(it, ShootService::class.java).apply {
        setPackage(it.packageName)
      }
      it.bindService(
        intent,
        shootServiceConnection, Service.BIND_AUTO_CREATE
      )
    }

    val dashboard = SnapshotDashboardView(
      ContextThemeWrapper(context, R.style.AlbumMaterialCard)
    ).apply {
      layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      if (!GlobalValues.md3Theme) {
        background = null
      }
    }

    dashboard.setOnClickListener {
      startActivity(Intent(context, AlbumActivity::class.java))
    }

    dashboard.container.apply {

      fun changeTimeNode() {
        lifecycleScope.launch(Dispatchers.IO) {
          val timeStampList = viewModel.repository.getTimeStamps()
          val dialog = TimeNodeBottomSheetDialogFragment
            .newInstance(ArrayList(timeStampList))
            .apply {
              setOnItemClickListener { position ->
                val item = timeStampList[position]
                GlobalValues.snapshotTimestamp = item.timestamp
                lifecycleScope.launch(Dispatchers.Main) {
                  viewModel.timestamp.value = item.timestamp
                  flip(VF_LOADING)
                  dismiss()
                }
                viewModel.compareDiff(item.timestamp, shouldClearDiff = true)
              }
            }
          dialog.show(context.supportFragmentManager, dialog.tag)
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

        val intent = Intent(context, SnapshotDetailActivity::class.java)
          .putExtras(bundleOf(EXTRA_ENTITY to getItem(position)))
        startActivity(intent)
      }
    }

    binding.apply {
      list.apply {
        adapter = this@SnapshotFragment.adapter
        borderDelegate = borderViewDelegate
        layoutManager = getSuitableLayoutManager()
        borderVisibilityChangedListener =
          BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
            context.appBar?.setRaised(!top)
          }

        if (itemDecorationCount == 0) {
          addItemDecoration(
            HorizontalSpacesItemDecoration(
              resources.getDimension(R.dimen.normal_padding).toInt() / 2
            )
          )
        }
        scrollToPosition(0)
      }
      vfContainer.apply {
        setInAnimation(activity, R.anim.anim_fade_in)
        setOutAnimation(activity, R.anim.anim_fade_out)
      }
    }

    viewModel.apply {
      timestamp.observe(viewLifecycleOwner) {
        if (it != 0L) {
          dashboard.container.tvSnapshotTimestampText.text = getFormatDateString(it)
        } else {
          dashboard.container.tvSnapshotTimestampText.text =
            getString(R.string.snapshot_none)
          snapshotDiffItems.value = emptyList()
          flip(VF_LIST)
        }
      }
      allSnapshots.onEach {
        if (shouldCompare) {
          compareDiff()
        }
      }.launchIn(lifecycleScope)
      snapshotAppsCount.observe(viewLifecycleOwner) {
        if (it != null) {
          dashboard.container.tvSnapshotAppsCountText.text = it.toString()
        }
      }
      snapshotDiffItems.observe(viewLifecycleOwner) { list ->
        adapter.setDiffNewData(
          list.sortedByDescending { it.updateTime }
            .toMutableList()
        ) {
          if (!isListCanScroll(adapter.data.size)) {
            if (!hasAddedListBottomPadding) {
              binding.list.addPaddingBottom(20.dp)
              hasAddedListBottomPadding = true
            }
          } else {
            if (hasAddedListBottomPadding) {
              binding.list.addPaddingBottom((-20).dp)
              hasAddedListBottomPadding = false
            }
          }
        }
        flip(VF_LIST)

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
      }
      comparingProgressLiveData.observe(viewLifecycleOwner) {
        binding.progressIndicator.setProgressCompat(it, it != 1)
      }
    }
    homeViewModel.apply {
      lifecycleScope.launchWhenStarted {
        effect.collect {
          when (it) {
            is HomeViewModel.Effect.PackageChanged -> {
              if (allowRefreshing) {
                packageQueue.offer(it.packageName to it.action)
                dequeuePackages()
              }
            }
            else -> {}
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    menu?.findItem(R.id.save)?.isVisible = binding.vfContainer.displayedChild == VF_LIST
    if (AppItemRepository.trackItemsChanged) {
      AppItemRepository.trackItemsChanged = false
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    }
    if (viewModel.timestamp.value != GlobalValues.snapshotTimestamp) {
      viewModel.timestamp.value = GlobalValues.snapshotTimestamp
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp, shouldClearDiff = true)
    }
    if (!viewModel.isComparingActive()) {
      flip(VF_LIST)
    } else {
      flip(VF_LOADING)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    adapter.release()
    shootBinder?.let {
      context?.applicationContext?.let { ctx ->
        it.unregisterOnShootOverListener(shootListener)
        ctx.unbindService(shootServiceConnection)
        if (ShootService.isComputing.not()) {
          runCatching {
            ctx.stopService(
              Intent(
                ctx, ShootService::class.java
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
    binding.list.layoutManager = getSuitableLayoutManager()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.snapshot_menu, menu)
    this.menu = menu

    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val context = (this.context as? BaseActivity<*>) ?: return false
    if (item.itemId == R.id.save) {
      if (viewModel.isComparingActive() || shootBinder?.isShooting == true) {
        return false
      }
      fun computeNewSnapshot(dropPrevious: Boolean = false) {
        flip(VF_LOADING)
        this@SnapshotFragment.dropPrevious = dropPrevious
        ContextCompat.startForegroundService(
          context, Intent(context, ShootService::class.java)
        )
        shootBinder?.computeSnapshot(dropPrevious) ?: run {
          Timber.w("shoot binder is null")
          Toasty.showShort(context, "Snapshot service error")
        }
        shouldCompare = false
        Analytics.trackEvent(
          Constants.Event.SNAPSHOT_CLICK,
          EventProperties().set("Action", "Click to Save")
        )
      }

      if (GlobalValues.snapshotTimestamp == 0L) {
        computeNewSnapshot()
      } else {
        MaterialAlertDialogBuilder(context)
          .setTitle(R.string.dialog_title_keep_previous_snapshot)
          .setMessage(R.string.dialog_message_keep_previous_snapshot)
          .setPositiveButton(R.string.btn_keep) { _, _ ->
            computeNewSnapshot(false)
          }
          .setNegativeButton(R.string.btn_drop) { _, _ ->
            computeNewSnapshot(true)
          }
          .setNeutralButton(android.R.string.cancel, null)
          .show()
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun flip(child: Int) {
    val context = (this.context as? BaseActivity<*>) ?: return
    allowRefreshing = child == VF_LIST
    if (binding.vfContainer.displayedChild == child) {
      return
    }
    if (child == VF_LOADING) {
      binding.loading.resumeAnimation()
      context.appBar?.setRaised(false)
      menu?.findItem(R.id.save)?.isVisible = false
    } else {
      binding.loading.pauseAnimation()
      binding.list.scrollToPosition(0)
      menu?.findItem(R.id.save)?.isVisible = true
    }

    binding.vfContainer.displayedChild = child
  }

  private fun compareDiff() {
    viewModel.timestamp.value = GlobalValues.snapshotTimestamp
    isSnapshotDatabaseItemsReady = true

    viewModel.computeSnapshotAppCount(GlobalValues.snapshotTimestamp)

    viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    isSnapshotDatabaseItemsReady = false
  }

  private fun dequeuePackages() = lifecycleScope.launch(Dispatchers.IO) {
    while (packageQueue.isNotEmpty()) {
      packageQueue.poll()?.first?.let {
        withContext(Dispatchers.Main) {
          flip(VF_LOADING)
        }
        viewModel.compareItemDiff(GlobalValues.snapshotTimestamp, it)
      }
    }
  }

  override fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
    return when (resources.configuration.orientation) {
      Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(context)
      Configuration.ORIENTATION_LANDSCAPE -> StaggeredGridLayoutManager(
        2,
        StaggeredGridLayoutManager.VERTICAL
      )
      else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
    }
  }

  override fun onReturnTop() {
    if (binding.list.canScrollVertically(-1)) {
      binding.list.smoothScrollToPosition(0)
    } else {
      flip(VF_LOADING)
      viewModel.compareDiff(GlobalValues.snapshotTimestamp)
    }
  }
}
