package com.absinthe.libchecker.ui.album

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.databinding.ActivityComparisonBinding
import com.absinthe.libchecker.databinding.LayoutComparisonDashboardBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.recyclerview.HorizontalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotAdapter
import com.absinthe.libchecker.ui.detail.EXTRA_ENTITY
import com.absinthe.libchecker.ui.detail.SnapshotDetailActivity
import com.absinthe.libchecker.ui.fragment.snapshot.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.utils.unsafeLazy
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import kotlinx.coroutines.launch
import rikka.widget.borderview.BorderView

const val VF_LOADING = 0
const val VF_LIST = 1

class ComparisonActivity : BaseActivity() {

    private lateinit var binding: ActivityComparisonBinding
    private val viewModel by viewModels<SnapshotViewModel>()
    private val adapter by unsafeLazy { SnapshotAdapter(lifecycleScope) }
    private var leftTimeStamp = 0L
    private var rightTimeStamp = 0L

    override fun setViewBinding(): ViewGroup {
        binding = ActivityComparisonBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.release()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        setAppBar(binding.appbar, binding.toolbar)
        (binding.root as ViewGroup).bringChildToFront(binding.appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dashboardBinding = LayoutComparisonDashboardBinding.inflate(layoutInflater)

        dashboardBinding.apply {
            infoLeft.horizontalGravity = Gravity.START
            infoLeft.setOnClickListener {
                lifecycleScope.launch {
                    val timeStampList = viewModel.repository.getTimeStamps()
                    val dialog = TimeNodeBottomSheetDialogFragment
                        .newInstance(ArrayList(timeStampList))
                        .apply {
                            setOnItemClickListener { position ->
                                val item = timeStampList[position]
                                leftTimeStamp = item.timestamp
                                infoLeft.tvSnapshotTimestampText.text =
                                    viewModel.getFormatDateString(leftTimeStamp)
                                lifecycleScope.launch {
                                    val count =
                                        viewModel.repository.getSnapshots(leftTimeStamp).size
                                    infoLeft.tvSnapshotAppsCountText.text = count.toString()
                                }
                                dismiss()
                            }
                        }
                    dialog.show(supportFragmentManager, dialog.tag)
                }
            }
            infoRight.horizontalGravity = Gravity.END
            infoRight.setOnClickListener {
                lifecycleScope.launch {
                    val timeStampList = viewModel.repository.getTimeStamps()
                    val dialog = TimeNodeBottomSheetDialogFragment
                        .newInstance(ArrayList(timeStampList))
                        .apply {
                            setOnItemClickListener { position ->
                                val item = timeStampList[position]
                                rightTimeStamp = item.timestamp
                                infoRight.tvSnapshotTimestampText.text =
                                    viewModel.getFormatDateString(rightTimeStamp)
                                lifecycleScope.launch {
                                    val count =
                                        viewModel.repository.getSnapshots(rightTimeStamp).size

                                    infoRight.tvSnapshotAppsCountText.text = count.toString()
                                }
                                dismiss()
                            }
                        }
                    dialog.show(supportFragmentManager, dialog.tag)
                }
            }
        }

        binding.apply {
            extendedFab.apply {
                post {
                    (layoutParams as ViewGroup.MarginLayoutParams).setMargins(
                        0,
                        0,
                        16.dp,
                        paddingBottom + (window.decorView.rootWindowInsets?.systemWindowInsetBottom
                            ?: 0)
                    )
                }
                setOnClickListener {
                    if (AntiShakeUtils.isInvalidClick(it)) {
                        return@setOnClickListener
                    }
                    if (leftTimeStamp == rightTimeStamp || leftTimeStamp == 0L || rightTimeStamp == 0L) {
                        return@setOnClickListener
                    }
                    viewModel.compareDiff(
                        leftTimeStamp.coerceAtMost(rightTimeStamp),
                        leftTimeStamp.coerceAtLeast(rightTimeStamp)
                    )
                    flip(VF_LOADING)
                }
                setOnLongClickListener {
                    hide()
                    true
                }
            }
            recyclerview.apply {
                adapter = this@ComparisonActivity.adapter
                layoutManager = getSuitableLayoutManager()
                borderVisibilityChangedListener =
                    BorderView.OnBorderVisibilityChangedListener { top: Boolean, _: Boolean, _: Boolean, _: Boolean ->
                        appBar?.setRaised(!top)
                    }

                if (itemDecorationCount == 0) {
                    addItemDecoration(
                        HorizontalSpacesItemDecoration(
                            resources.getDimension(R.dimen.normal_padding).toInt() / 2
                        )
                    )
                }
            }
            vfContainer.apply {
                setInAnimation(this@ComparisonActivity, R.anim.anim_fade_in)
                setOutAnimation(this@ComparisonActivity, R.anim.anim_fade_out)
                displayedChild = VF_LIST
            }
        }

        adapter.apply {
            headerWithEmptyEnable = true
            val emptyView = SnapshotEmptyView(this@ComparisonActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.CENTER_HORIZONTAL
                }
                addPaddingTop(96.dp)
            }
            setEmptyView(emptyView)
            setHeaderView(dashboardBinding.root)
            setOnItemClickListener { _, view, position ->
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemClickListener
                }

                val intent = Intent(this@ComparisonActivity, SnapshotDetailActivity::class.java)
                    .putExtras(
                        bundleOf(
                            EXTRA_ENTITY to getItem(position)
                        )
                    )

                startActivity(intent)
            }
        }

        viewModel.snapshotDiffItems.observe(this, { list ->
            adapter.setList(list.sortedByDescending { it.updateTime })
            flip(VF_LIST)
        })
    }

    private fun getSuitableLayoutManager(): RecyclerView.LayoutManager {
        return when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> LinearLayoutManager(this)
            Configuration.ORIENTATION_LANDSCAPE -> StaggeredGridLayoutManager(
                2,
                StaggeredGridLayoutManager.VERTICAL
            )
            else -> throw IllegalStateException("Wrong orientation at AppListFragment.")
        }
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
}
