package com.absinthe.libchecker.ui.fragment.snapshot

import android.app.ActivityOptions
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.AppItemRepository
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.databinding.LayoutSnapshotDashboardBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.AntiShakeUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.blankj.utilcode.util.BarUtils
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import rikka.material.widget.BorderView
import java.text.SimpleDateFormat
import java.util.*

const val VF_LOADING = 0
const val VF_LIST = 1

class SnapshotFragment : BaseFragment<FragmentSnapshotBinding>(R.layout.fragment_snapshot) {

    private val viewModel by activityViewModels<SnapshotViewModel>()
    private val adapter = SnapshotAdapter()
    private var isSnapshotDatabaseItemsReady = false
    private var isApplicationInfoItemsReady = false

    override fun initBinding(view: View): FragmentSnapshotBinding =
        FragmentSnapshotBinding.bind(view)

    override fun init() {
        val dashboardBinding = LayoutSnapshotDashboardBinding.inflate(layoutInflater)

        binding.apply {
            extendedFab.apply {
                (layoutParams as ConstraintLayout.LayoutParams).setMargins(
                    0, 0, 16.dp, 70.dp + paddingBottom + UiUtils.getNavBarHeight()
                )
                setOnClickListener {
                    if (AntiShakeUtils.isInvalidClick(it)) {
                        return@setOnClickListener
                    }

                    flip(VF_LOADING)
                    viewModel.computeSnapshots()
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_CLICK,
                        EventProperties().set("Action", "Click to Save")
                    )
                }
                setOnLongClickListener {
                    hide()
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_CLICK,
                        EventProperties().set("Action", "Long Click to Hide")
                    )
                    true
                }
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
                addPaddingBottom(UiUtils.getNavBarHeight())
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
            }
        }

        adapter.apply {
            headerWithEmptyEnable = true
            setEmptyView(R.layout.layout_snapshot_empty_view)
            setHeaderView(dashboardBinding.root)
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

                if (GlobalValues.isShowEntryAnimation.value!!) {
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
                    flip(VF_LIST)
                }
            })
            snapshotItems.observe(viewLifecycleOwner, {
                isSnapshotDatabaseItemsReady = true
                dashboardBinding.tvSnapshotAppsCountText.text = it.size.toString()

                if (isApplicationInfoItemsReady) {
                    computeDiff()
                }
            })
            AppItemRepository.allApplicationInfoItems.observe(viewLifecycleOwner, {
                isApplicationInfoItemsReady = true

                if (isSnapshotDatabaseItemsReady) {
                    computeDiff()
                }
            })
            snapshotDiffItems.observe(
                viewLifecycleOwner, { list ->
                    adapter.setList(list.sortedByDescending { it.updateTime })
                    flip(VF_LIST)
                }
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.recyclerview.layoutManager = getSuitableLayoutManager()
    }

    private fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }

    private fun flip(child: Int) {
        if (binding.vfContainer.displayedChild == child) {
            return
        }
        if (child == VF_LOADING) {
            if (binding.extendedFab.isShown) {
                binding.extendedFab.hide()
            }
        } else {
            if (!binding.extendedFab.isShown) {
                binding.extendedFab.show()
            }
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
}