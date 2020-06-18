package com.absinthe.libchecker.ui.fragment.snapshot

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.databinding.FragmentSnapshotBinding
import com.absinthe.libchecker.recyclerview.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ConvertUtils
import java.text.SimpleDateFormat
import java.util.*

class SnapshotFragment : Fragment() {

    private val viewModel by viewModels<SnapshotViewModel>()
    private lateinit var binding: FragmentSnapshotBinding
    private val adapter = SnapshotAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSnapshotBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.root.apply {
            setPadding(
                paddingStart,
                paddingTop + BarUtils.getStatusBarHeight(),
                paddingEnd,
                paddingBottom
            )
        }
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
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        //dy less than zero means swiping up
                        if (dy > 0 && binding.extendedFab.isShown) {
                            binding.extendedFab.hide()
                        } else if (dy < 0 && !binding.extendedFab.isShown) {
                            binding.extendedFab.show()
                        }
                    }
                })
            }
            vfContainer.apply {
                setInAnimation(activity, R.anim.anim_fade_in)
                setOutAnimation(activity, R.anim.anim_fade_out)
                displayedChild = 1
            }
        }

        adapter.apply {
            setEmptyView(R.layout.layout_snapshot_empty_view)
            setOnItemClickListener { _, view, position ->
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

        viewModel.timestamp.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            binding.dashboardLayout.tvSnapshotTimestampText.text = if (it != 0L) {
                getFormatDateString(it)
            } else {
                "None"
            }
        })
        viewModel.snapshotItems.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            binding.dashboardLayout.tvSnapshotAppsCountText.text = it.size.toString()
            viewModel.computeDiff(requireContext())
        })
        viewModel.snapshotDiffItems.observe(viewLifecycleOwner, androidx.lifecycle.Observer { list ->
            adapter.setNewInstance(list.sortedByDescending { it.updateTime }.toMutableList())
            binding.vfContainer.displayedChild = 1
            binding.extendedFab.show()
        })
    }

    private fun getFormatDateString(timestamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return simpleDateFormat.format(date)
    }
}