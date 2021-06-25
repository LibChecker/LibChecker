package com.absinthe.libchecker.ui.fragment.snapshot

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
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
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.ui.snapshot.AlbumActivity
import com.absinthe.libchecker.utils.doOnMainThreadIdle
import com.absinthe.libchecker.view.snapshot.SnapshotDashboardView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.*
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment : BaseListControllerFragment<FragmentSnapshotBinding>(R.layout.fragment_snapshot) {

    private val viewModel by activityViewModels<SnapshotViewModel>()
    private val adapter by lazy { SnapshotAdapter(lifecycleScope) }
    private var isSnapshotDatabaseItemsReady = false
    private var isApplicationInfoItemsReady = false
    private var dropPrevious = false
    private var shouldCompare = true

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
                try {
                    binding.progressIndicator.setProgressCompat(progress, true)
                } catch (e: NullPointerException) {

                }
            }
        }
    }
    private val shootServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shootBinder = service as? IShootService
            shootBinder?.apply {
                registerOnShootOverListener(shootListener)
                computeSnapshot(dropPrevious)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shootBinder = null
        }
    }

    override fun initBinding(view: View): FragmentSnapshotBinding = FragmentSnapshotBinding.bind(view)

    override fun init() {
        setHasOptionsMenu(true)

        val dashboard = SnapshotDashboardView(ContextThemeWrapper(requireContext(), R.style.AlbumMaterialCard)).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            background = null
        }

        dashboard.setOnClickListener {
            startActivity(Intent(requireContext(), AlbumActivity::class.java))
        }

        dashboard.container.apply {

            fun changeTimeNode() {
                lifecycleScope.launch(Dispatchers.IO) {
                    val timeStampList = viewModel.repository.getTimeStamps()
                    val charList = mutableListOf<String>()
                    timeStampList.forEach { charList.add(viewModel.getFormatDateString(it.timestamp)) }

                    withContext(Dispatchers.Main) {
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.dialog_title_change_timestamp)
                            .setItems(charList.toTypedArray()) { _, which ->
                                GlobalValues.snapshotTimestamp = timeStampList[which].timestamp
                                viewModel.timestamp.value = timeStampList[which].timestamp
                                flip(VF_LOADING)
                                viewModel.compareDiff(timeStampList[which].timestamp, shouldClearDiff = true)
                            }
                            .show()
                    }
                }
            }

            tvSnapshotTimestampText.setOnClickListener {
                changeTimeNode()
            }
            arrow.setOnClickListener {
                changeTimeNode()
            }
        }

        val emptyView = SnapshotEmptyView(requireContext()).apply {
            if (GlobalValues.snapshotTimestamp == 0L) {
                text.text = getString(R.string.snapshot_no_snapshot)
            }
        }

        adapter.apply {
            headerWithEmptyEnable = true
            setEmptyView(emptyView)
            setHeaderView(dashboard)
            setDiffCallback(SnapshotDiffUtil())
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }

                val intent = Intent(requireActivity(), SnapshotDetailActivity::class.java).apply {
                    putExtras(Bundle().apply {
                        putSerializable(EXTRA_ENTITY, getItem(position))
                    })
                }
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
                        (requireActivity() as BaseActivity).appBar?.setRaised(!top)
                    }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_SETTLING) {

                        }
                    }
                })

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
            timestamp.observe(viewLifecycleOwner, {
                if (it != 0L) {
                    dashboard.container.tvSnapshotTimestampText.text = getFormatDateString(it)
                } else {
                    dashboard.container.tvSnapshotTimestampText.text = getString(R.string.snapshot_none)
                    snapshotDiffItems.value = emptyList()
                    flip(VF_LIST)
                }
            })
            allSnapshots.observe(viewLifecycleOwner, {
                if (shouldCompare) {
                    viewModel.timestamp.value = GlobalValues.snapshotTimestamp
                    isSnapshotDatabaseItemsReady = true

                    computeSnapshotAppCount(GlobalValues.snapshotTimestamp)

                    if (isApplicationInfoItemsReady) {
                        compareDiff(GlobalValues.snapshotTimestamp)
                        isSnapshotDatabaseItemsReady = false
                    }

                }
            })
            snapshotAppsCount.observe(viewLifecycleOwner, {
                if (it != null) {
                    dashboard.container.tvSnapshotAppsCountText.text = it.toString()
                }
            })
            AppItemRepository.allApplicationInfoItems.observe(viewLifecycleOwner, {
                isApplicationInfoItemsReady = true
                AppItemRepository.shouldRefreshAppList = true

                if (isSnapshotDatabaseItemsReady) {
                    compareDiff(GlobalValues.snapshotTimestamp)
                    isApplicationInfoItemsReady = false
                }
            })
            snapshotDiffItems.observe(
                viewLifecycleOwner, { list ->
                    adapter.setDiffNewData(list.sortedByDescending { it.updateTime }.toMutableList())
                    flip(VF_LIST)

                    lifecycleScope.launch(Dispatchers.IO) {
                        delay(250)

                        doOnMainThreadIdle({
                            if (this@SnapshotFragment == homeViewModel.controller
                                && !binding.list.canScrollVertically(-1)
                            ) {
                                (requireActivity() as MainActivity).showNavigationView()
                            }
                        })
                    }
                }
            )
            comparingProgressLiveData.observe(viewLifecycleOwner, {
                binding.progressIndicator.setProgressCompat(it, it != 1)
            })
        }
        homeViewModel.apply {
            packageChangedLiveData.observe(viewLifecycleOwner) {
                viewModel.compareDiff(GlobalValues.snapshotTimestamp)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppItemRepository.trackItemsChanged) {
            AppItemRepository.trackItemsChanged = false
            viewModel.compareDiff(GlobalValues.snapshotTimestamp)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter.release()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.list.layoutManager = getSuitableLayoutManager()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.snapshot_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.save) {
            fun computeNewSnapshot(dropPrevious: Boolean = false) {
                flip(VF_LOADING)
                this@SnapshotFragment.dropPrevious = dropPrevious
                shootBinder?.computeSnapshot(dropPrevious) ?: let {
                    requireContext().bindService(
                        Intent(
                            requireContext(),
                            ShootService::class.java
                        ), shootServiceConnection, Service.BIND_AUTO_CREATE
                    )
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
                AlertDialog.Builder(requireContext())
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
        if (binding.vfContainer.displayedChild == child) {
            return
        }
        if (child == VF_LOADING) {
            binding.loading.resumeAnimation()
            (requireActivity() as BaseActivity).appBar?.setRaised(false)
        } else {
            binding.loading.pauseAnimation()
            binding.list.scrollToPosition(0)
        }

        binding.vfContainer.displayedChild = child
    }

    private fun compareDiff() {
        viewModel.timestamp.value = GlobalValues.snapshotTimestamp
        isSnapshotDatabaseItemsReady = true

        viewModel.computeSnapshotAppCount(GlobalValues.snapshotTimestamp)

        if (isApplicationInfoItemsReady) {
            viewModel.compareDiff(GlobalValues.snapshotTimestamp)
            isSnapshotDatabaseItemsReady = false
        }
    }

    private fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(requireContext())
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