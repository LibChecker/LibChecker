package com.absinthe.libchecker.ui.fragment.snapshot

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.databinding.LayoutSnapshotDashboardBinding
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.BaseFragment
import com.absinthe.libchecker.ui.main.MainActivity
import com.absinthe.libchecker.utils.AntiShakeUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import rikka.material.widget.BorderView
import java.text.SimpleDateFormat
import java.util.*

class SnapshotFragment : BaseFragment<FragmentSnapshotBinding>(R.layout.fragment_snapshot) {

    private val viewModel by activityViewModels<SnapshotViewModel>()
    private val adapter = SnapshotAdapter()

    override fun initBinding(view: View): FragmentSnapshotBinding = FragmentSnapshotBinding.bind(view)

    @SuppressLint("SetTextI18n")
    override fun init() {
        val dashboardBinding = LayoutSnapshotDashboardBinding.inflate(layoutInflater)
        binding.apply {
            extendedFab.apply {
                (layoutParams as CoordinatorLayout.LayoutParams)
                    .setMargins(
                        0,
                        0,
                        ConvertUtils.dp2px(16f),
                        ConvertUtils.dp2px(16f) + paddingBottom
                    )
                setOnClickListener {
                    vfContainer.displayedChild = 0
                    hide()
                    viewModel.computeSnapshots(requireContext())
                }
            }
            recyclerview.apply {
                adapter = this@SnapshotFragment.adapter
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        (requireActivity() as MainActivity).appBar?.setRaised(!top)
                    }
                setPadding(paddingStart, paddingTop + BarUtils.getStatusBarHeight(), paddingEnd, paddingBottom)
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
                setPadding(
                    paddingStart,
                    paddingTop,
                    paddingEnd,
                    paddingBottom + UiUtils.getNavBarHeight()
                )
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
                    requireActivity(),
                    view,
                    "app_card_container"
                )

                if (GlobalValues.isShowEntryAnimation.value!!) {
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
        }

        viewModel.apply {
            timestamp.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                if (it != 0L) {
                    dashboardBinding.tvSnapshotTimestampText.text = getFormatDateString(it)
                    binding.vfContainer.displayedChild = 0
                } else {
                    dashboardBinding.tvSnapshotTimestampText.text = "None"
                    binding.vfContainer.displayedChild = 1
                }
            })
            snapshotItems.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                dashboardBinding.tvSnapshotAppsCountText.text = it.size.toString()
                computeDiff(requireContext())
            })
            snapshotDiffItems.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer { list ->
                    adapter.setList(list.sortedByDescending { it.updateTime })
                    if (binding.vfContainer.displayedChild == 0) {
                        binding.vfContainer.displayedChild = 1
                    }
                    binding.extendedFab.show()
                })
        }
    }

    private fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }
}