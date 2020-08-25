package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.viewModels
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDetailItem
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.*
import com.absinthe.libchecker.constant.librarymap.NativeLibMap
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.extensions.addPaddingBottom
import com.absinthe.libchecker.extensions.finishCompat
import com.absinthe.libchecker.extensions.valueUnsafe
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotDetailAdapter
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.BaseSnapshotNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.view.dialogfragment.LibDetailDialogFragment
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.absinthe.libraries.utils.utils.UiUtils
import com.blankj.utilcode.util.AppUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity : BaseActivity() {

    private lateinit var binding: ActivitySnapshotDetailBinding
    private lateinit var entity: SnapshotDiffItem

    private val adapter = SnapshotDetailAdapter()
    private val viewModel by viewModels<SnapshotViewModel>()
    private val _entity by lazy { intent.getSerializableExtra(EXTRA_ENTITY) as? SnapshotDiffItem }

    override fun setViewBinding(): ViewGroup {
        isPaddingToolbar = true
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
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        if (GlobalValues.isShowEntryAnimation.valueUnsafe) {
            supportFinishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishCompat()
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
                addPaddingBottom(UiUtils.getNavBarHeight(contentResolver))
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
            }

            val isNewOrDeleted = entity.deleted || entity.newInstalled

            ivAppIcon.load(AppUtils.getAppIcon(entity.packageName))
            tvAppName.text = getDiffString(entity.labelDiff, isNewOrDeleted)
            tvPackageName.text = entity.packageName
            tvVersion.text = getDiffString(
                entity.versionNameDiff,
                entity.versionCodeDiff,
                isNewOrDeleted,
                "%s (%s)"
            )
            tvTargetApi.text = getDiffString(entity.targetApiDiff, isNewOrDeleted, "API %s")
        }

        viewModel.snapshotDetailItems.observe(this, { details ->
            val titleList = mutableListOf<SnapshotTitleNode>()

            getNodeList(details.filter { it.itemType == NATIVE }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, getString(R.string.ref_category_native)))
                }
            }
            getNodeList(details.filter { it.itemType == SERVICE }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, getString(R.string.ref_category_service)))
                }
            }
            getNodeList(details.filter { it.itemType == ACTIVITY }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, getString(R.string.ref_category_activity)))
                }
            }
            getNodeList(details.filter { it.itemType == RECEIVER }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, getString(R.string.ref_category_br)))
                }
            }
            getNodeList(details.filter { it.itemType == PROVIDER }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, getString(R.string.ref_category_cp)))
                }
            }

            if (titleList.isNotEmpty()) {
                adapter.setList(titleList)
            }
        })

        adapter.setEmptyView(
            when {
                entity.newInstalled -> R.layout.layout_snapshot_detail_new_install
                entity.deleted -> R.layout.layout_snapshot_detail_deleted
                else -> R.layout.layout_snapshot_empty_view
            }
        )
        adapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.chip) {
                if (AntiShakeUtils.isInvalidClick(view)) {
                    return@setOnItemChildClickListener
                }

                val item = (adapter.data[position] as BaseSnapshotNode).item
                val name = item.name
                val regexName = NativeLibMap.findRegex(name)?.regexName
                LibDetailDialogFragment.newInstance(name, item.itemType, regexName)
                    .apply {
                        show(supportFragmentManager, tag)
                    }
            }
        }
    }

    private fun getNodeList(list: List<SnapshotDetailItem>): MutableList<BaseNode> {
        val returnList = mutableListOf<BaseNode>()

        if (list.isEmpty()) return returnList

        if (list[0].itemType == NATIVE) {
            for (item in list) {
                returnList.add(SnapshotNativeNode(item))
            }
        } else {
            for (item in list) {
                returnList.add(SnapshotComponentNode(item))
            }
        }

        return returnList
    }

    private fun <T> getDiffString(
        diff: SnapshotDiffItem.DiffNode<T>,
        isNewOrDeleted: Boolean = false,
        format: String = "%s"
    ): String {
        return if (diff.old != diff.new && !isNewOrDeleted) {
            "${String.format(format, diff.old.toString())} $ARROW ${String.format(
                format,
                diff.new.toString()
            )}"
        } else {
            String.format(format, diff.old.toString())
        }
    }

    private fun getDiffString(
        diff1: SnapshotDiffItem.DiffNode<*>,
        diff2: SnapshotDiffItem.DiffNode<*>,
        isNewOrDeleted: Boolean = false,
        format: String = "%s"
    ): String {
        return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
            "${String.format(
                format,
                diff1.old.toString(),
                diff2.old.toString()
            )} $ARROW ${String.format(format, diff1.new.toString(), diff2.new.toString())}"
        } else {
            String.format(format, diff1.old.toString(), diff2.old.toString())
        }
    }
}