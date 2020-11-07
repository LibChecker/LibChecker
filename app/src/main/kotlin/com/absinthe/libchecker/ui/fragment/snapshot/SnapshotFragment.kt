package com.absinthe.libchecker.ui.fragment.snapshot

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.OnceTag
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.databinding.LayoutSnapshotDashboardBinding
import com.absinthe.libchecker.databinding.LayoutSnapshotEmptyViewBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.recyclerview.diff.SnapshotDiffUtil
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.BaseListControllerFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.ui.snapshot.AlbumActivity
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.blankj.utilcode.util.BarUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import jonathanfinerty.once.Once
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.material.widget.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment : BaseListControllerFragment<FragmentSnapshotBinding>(R.layout.fragment_snapshot) {

    private val viewModel by activityViewModels<SnapshotViewModel>()
    private val adapter = SnapshotAdapter()
    private var isSnapshotDatabaseItemsReady = false
    private var isApplicationInfoItemsReady = false

    override fun initBinding(view: View): FragmentSnapshotBinding = FragmentSnapshotBinding.bind(view)

    override fun init() {
        val dashboardBinding = LayoutSnapshotDashboardBinding.inflate(layoutInflater)

        dashboardBinding.apply {
            root.setOnClickListener {
                startActivity(Intent(requireContext(), AlbumActivity::class.java))
            }

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
                                viewModel.compareDiff(timeStampList[which].timestamp)
                            }
                            .show()
                    }
                }
            }

            tvSnapshotTimestampText.setOnClickListener {
                changeTimeNode()
            }
            ivArrow.setOnClickListener{
                changeTimeNode()
            }
        }

        binding.apply {
            extendedFab.apply {
                (layoutParams as ConstraintLayout.LayoutParams).setMargins(
                    0, 0, 16.dp, 70.dp + paddingBottom + UiUtils.getNavBarHeight(requireActivity().contentResolver)
                )
                setOnClickListener {
                    if (AntiShakeUtils.isInvalidClick(it)) {
                        return@setOnClickListener
                    }

                    fun computeNewSnapshot(dropPrevious: Boolean = false) {
                        flip(VF_LOADING)
                        viewModel.computeSnapshots(dropPrevious)
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
                setOnLongClickListener {
                    hide()
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_CLICK,
                        EventProperties().set("Action", "Long Click to Hide")
                    )
                    true
                }
                isVisible = false
            }
            recyclerview.apply {
                adapter = this@SnapshotFragment.adapter
                layoutManager = getSuitableLayoutManager()
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        //dy less than zero means swiping up
                        if (dy > 10 && binding.extendedFab.isShown) {
                            binding.extendedFab.hide()
                        } else if (dy < -10 && !binding.extendedFab.isShown) {
                            binding.extendedFab.show()
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
                addPaddingTop(BarUtils.getStatusBarHeight())
                addPaddingBottom(UiUtils.getNavBarHeight(requireActivity().contentResolver))
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
            loading.enableMergePathsForKitKatAndAbove(true)
        }

        adapter.apply {
            headerWithEmptyEnable = true
            val emptyViewBinding = LayoutSnapshotEmptyViewBinding.inflate(layoutInflater)
            emptyViewBinding.tvSubtitle.isGone = true
            setEmptyView(emptyViewBinding.root)
            setHeaderView(dashboardBinding.root)
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

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    requireActivity(), view, view.transitionName
                )

                if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
        }

        viewModel.apply {
            timestamp.observe(viewLifecycleOwner, {
                if (it != 0L) {
                    dashboardBinding.tvSnapshotTimestampText.text = getFormatDateString(it)
                    flip(VF_LOADING)
                } else {
                    dashboardBinding.tvSnapshotTimestampText.text = getString(R.string.snapshot_none)
                    snapshotDiffItems.value = emptyList()
                    flip(VF_LIST)
                }
            })
            snapshotItems.observe(viewLifecycleOwner, {
                viewModel.timestamp.value = GlobalValues.snapshotTimestamp
                isSnapshotDatabaseItemsReady = true

                computeSnapshotAppCount(GlobalValues.snapshotTimestamp)

                if (isApplicationInfoItemsReady) {
                    compareDiff(GlobalValues.snapshotTimestamp)
                    isSnapshotDatabaseItemsReady = false
                }

                if (!Once.beenDone(Once.THIS_APP_INSTALL, OnceTag.MIGRATION_DATABASE_4_5)) {
                    viewModel.migrateFrom4To5()
                    Once.markDone(OnceTag.MIGRATION_DATABASE_4_5)
                }
            })
            snapshotAppsCount.observe(viewLifecycleOwner, {
                if (it != null) {
                    dashboardBinding.tvSnapshotAppsCountText.text = it.toString()
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
                }
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.recyclerview.layoutManager = getSuitableLayoutManager()
    }

    private fun flip(child: Int) {
        if (binding.vfContainer.displayedChild == child) {
            return
        }
        if (child == VF_LOADING) {
            if (binding.extendedFab.isShown) {
                binding.extendedFab.hide()
            }
            binding.loading.resumeAnimation()
        } else {
            if (!binding.extendedFab.isShown) {
                binding.extendedFab.show()
            }
            binding.loading.pauseAnimation()
        }

        binding.vfContainer.displayedChild = child
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
        binding.recyclerview.apply {
            if (canScrollVertically(-1)) {
                smoothScrollToPosition(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppItemRepository.shouldRefreshAppList) {
            viewModel.compareDiff(GlobalValues.snapshotTimestamp)
        }
    }
}