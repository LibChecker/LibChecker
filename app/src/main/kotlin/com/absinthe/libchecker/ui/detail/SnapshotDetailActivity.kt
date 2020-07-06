package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.recyclerview.adapter.ARROW
import com.absinthe.libchecker.recyclerview.adapter.SnapshotDetailAdapter
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.BarUtils
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity : BaseActivity() {

    private lateinit var binding: ActivitySnapshotDetailBinding
    private lateinit var entity: SnapshotDiffItem

    private val adapter = SnapshotDetailAdapter()
    private val viewModel by viewModels<SnapshotViewModel>()
    private val _entity by lazy { intent.getSerializableExtra(EXTRA_ENTITY) as SnapshotDiffItem? }

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding(): View {
        binding = ActivitySnapshotDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTransition()
        super.onCreate(savedInstanceState)

        if (_entity != null) {
            entity = _entity!!
            initView()
            viewModel.computeDiffDetail(entity)
        } else {
            supportFinishAfterTransition()
        }
    }

    override fun onBackPressed() {
        supportFinishAfterTransition()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFinishAfterTransition()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setRootPadding()
    }

    private fun initTransition() {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementEnterTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
            sharedElementReturnTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 250L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = "app_card_container"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        setRootPadding()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = entity.labelDiff.new ?: entity.labelDiff.old
        }

        binding.apply {
            rvList.apply {
                adapter = this@SnapshotDetailActivity.adapter
                setPadding(paddingStart, paddingTop, paddingEnd, paddingBottom + UiUtils.getNavBarHeight())
            }

            val isNewOrDeleted = entity.deleted || entity.newInstalled

            ivAppIcon.setImageDrawable(AppUtils.getAppIcon(entity.packageName))
            tvAppName.text = getDiffString(entity.labelDiff, isNewOrDeleted)
            tvPackageName.text = entity.packageName
            tvVersion.text = getDiffString(entity.versionNameDiff, entity.versionCodeDiff, isNewOrDeleted, "%s (%s)")
            tvTargetApi.text = getDiffString(entity.targetApiDiff, isNewOrDeleted, "API %s")
        }

        viewModel.snapshotDetailItems.observe(this, Observer {
            adapter.setList(it)
        })

        adapter.setEmptyView(
            when {
                entity.newInstalled -> R.layout.layout_snapshot_detail_new_install
                entity.deleted -> R.layout.layout_snapshot_detail_deleted
                else -> R.layout.layout_snapshot_empty_view
            }
        )
    }

    private fun setRootPadding() {
        val isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        binding.root.apply {
            fitsSystemWindows = isLandScape
            setPadding(0, if (isLandScape) 0 else BarUtils.getStatusBarHeight(), 0, 0)
        }
    }

    private fun <T> getDiffString(diff: SnapshotDiffItem.DiffNode<T>, isNewOrDeleted: Boolean = false, format: String = "%s"): String {
        return if (diff.old != diff.new && !isNewOrDeleted) {
            "${String.format(format, diff.old.toString())} $ARROW ${String.format(format, diff.new.toString())}"
        } else {
            String.format(format, diff.old.toString())
        }
    }

    private fun getDiffString(diff1: SnapshotDiffItem.DiffNode<*>, diff2: SnapshotDiffItem.DiffNode<*>, isNewOrDeleted: Boolean = false, format: String = "%s"): String {
        return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
            "${String.format(format, diff1.old.toString(), diff2.old.toString())} $ARROW ${String.format(format, diff1.new.toString(), diff2.new.toString())}"
        } else {
            String.format(format, diff1.old.toString(), diff2.old.toString())
        }
    }
}