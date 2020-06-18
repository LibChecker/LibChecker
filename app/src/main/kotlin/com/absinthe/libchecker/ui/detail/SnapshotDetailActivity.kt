package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
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
import com.absinthe.libchecker.recyclerview.ARROW
import com.absinthe.libchecker.recyclerview.SnapshotDetailAdapter
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.blankj.utilcode.util.AppUtils
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity : BaseActivity() {

    private lateinit var binding: ActivitySnapshotDetailBinding
    private val adapter = SnapshotDetailAdapter()
    private val entity by lazy { intent.getSerializableExtra(EXTRA_ENTITY) as SnapshotDiffItem? }
    private val viewModel by viewModels<SnapshotViewModel>()

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding() {
        binding = ActivitySnapshotDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setRoot() {
        root = binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        initTransition()
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun onStart() {
        super.onStart()
        initData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
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
                duration = 300L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = "app_card_container"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = entity!!.labelDiff.new ?: entity!!.labelDiff.old
        }

        binding.apply {
            rvList.apply {
                adapter = this@SnapshotDetailActivity.adapter
            }
            ivAppIcon.setImageDrawable(AppUtils.getAppIcon(entity!!.packageName))

            if (entity!!.labelDiff.new != null && entity!!.labelDiff.new != entity!!.labelDiff.old) {
                tvAppName.text = "${entity!!.labelDiff.old} $ARROW ${entity!!.labelDiff.new}"
            } else {
                tvAppName.text = entity!!.labelDiff.old
            }

            tvPackageName.text = entity!!.packageName

            val isNewOrDeleted = entity!!.deleted || entity!!.newInstalled

            if ((entity!!.versionNameDiff.old != entity!!.versionNameDiff.new || entity!!.versionCodeDiff.old != entity!!.versionCodeDiff.new) && !isNewOrDeleted) {
                tvVersion.text =
                    "${entity!!.versionNameDiff.old} (${entity!!.versionCodeDiff.old}) $ARROW ${entity!!.versionNameDiff.new ?: entity!!.versionNameDiff.old} (${entity!!.versionCodeDiff.new ?: entity!!.versionCodeDiff.old})"
            } else {
                tvVersion.text = "${entity!!.versionNameDiff.old} (${entity!!.versionCodeDiff.old})"
            }

            if (entity!!.targetApiDiff.old != entity!!.targetApiDiff.new && !isNewOrDeleted) {
                tvTargetApi.text =
                    "API ${entity!!.targetApiDiff.old} $ARROW API ${entity!!.targetApiDiff.new}"
            } else {
                tvTargetApi.text = "API ${entity!!.targetApiDiff.old}"
            }
        }
        viewModel.snapshotDetailItems.observe(this, Observer {
            adapter.setNewInstance(it.toMutableList())
        })
        adapter.setEmptyView(
            when {
                entity!!.newInstalled -> R.layout.layout_snapshot_detail_new_install
                entity!!.deleted -> R.layout.layout_snapshot_detail_deleted
                else -> R.layout.layout_snapshot_empty_view
            }
        )
    }

    private fun initData() {
        if (entity == null) {
            finish()
        } else {
            viewModel.computeDiffDetail(entity!!)
        }
    }
}