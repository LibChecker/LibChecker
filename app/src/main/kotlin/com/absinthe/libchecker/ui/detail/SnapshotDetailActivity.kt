package com.absinthe.libchecker.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import com.absinthe.libchecker.BaseActivity
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.*
import com.absinthe.libchecker.bean.DetailExtraBean
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.bean.SnapshotDetailItem
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotDetailAdapter
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.BaseSnapshotNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.ui.main.EXTRA_REF_NAME
import com.absinthe.libchecker.ui.main.EXTRA_REF_TYPE
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.unsafeLazy
import com.absinthe.libchecker.view.snapshot.SnapshotDetailDeletedView
import com.absinthe.libchecker.view.snapshot.SnapshotDetailNewInstallView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity : BaseActivity() {

    private lateinit var binding: ActivitySnapshotDetailBinding
    private lateinit var entity: SnapshotDiffItem

    private val adapter by unsafeLazy { SnapshotDetailAdapter(lifecycleScope) }
    private val viewModel by viewModels<SnapshotViewModel>()
    private val _entity by unsafeLazy { intent.getSerializableExtra(EXTRA_ENTITY) as? SnapshotDiffItem }

    override fun setViewBinding(): ViewGroup {
        binding = ActivitySnapshotDetailBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (_entity != null) {
            entity = _entity!!
            initView()
            viewModel.computeDiffDetail(this, entity)
        } else {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = entity.labelDiff.new ?: entity.labelDiff.old
        }

        binding.apply {
            list.apply {
                adapter = this@SnapshotDetailActivity.adapter
                (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                addItemDecoration(VerticalSpacesItemDecoration(4.dp))
            }

            val isNewOrDeleted = entity.deleted || entity.newInstalled

            ivAppIcon.apply {
                val appIconLoader = AppIconLoader(
                    resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
                    false,
                    this@SnapshotDetailActivity
                )
                val icon = try {
                    appIconLoader.loadIcon(
                        PackageUtils.getPackageInfo(
                            entity.packageName,
                            PackageManager.GET_META_DATA
                        ).applicationInfo
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                load(icon)
                setOnClickListener {
                    startActivity(
                        Intent(
                            this@SnapshotDetailActivity,
                            AppDetailActivity::class.java
                        ).apply {
                            putExtra(EXTRA_PACKAGE_NAME, entity.packageName)
                        })
                }
            }
            tvAppName.text = getDiffString(entity.labelDiff, isNewOrDeleted)
            tvPackageName.text = entity.packageName
            tvVersion.text = getDiffString(
                entity.versionNameDiff,
                entity.versionCodeDiff,
                isNewOrDeleted,
                "%s (%s)"
            )
            tvTargetApi.text = "API ${getDiffString(entity.targetApiDiff, isNewOrDeleted)}"
        }

        viewModel.snapshotDetailItems.observe(this, { details ->
            val titleList = mutableListOf<SnapshotTitleNode>()

            getNodeList(details.filter { it.itemType == NATIVE }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, NATIVE))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Native", this.size.toLong())
                    )
                }
            }
            getNodeList(details.filter { it.itemType == SERVICE }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, SERVICE))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Service", this.size.toLong())
                    )
                }
            }
            getNodeList(details.filter { it.itemType == ACTIVITY }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, ACTIVITY))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Activity", this.size.toLong())
                    )
                }
            }
            getNodeList(details.filter { it.itemType == RECEIVER }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, RECEIVER))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Receiver", this.size.toLong())
                    )
                }
            }
            getNodeList(details.filter { it.itemType == PROVIDER }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, PROVIDER))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Provider", this.size.toLong())
                    )
                }
            }
            getNodeList(details.filter { it.itemType == PERMISSION }).apply {
                if (isNotEmpty()) {
                    titleList.add(SnapshotTitleNode(this, PERMISSION))
                    Analytics.trackEvent(
                        Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
                        EventProperties().set("Permission", this.size.toLong())
                    )
                }
            }

            if (titleList.isNotEmpty()) {
                adapter.setList(titleList)
            }
        })

        adapter.setEmptyView(
            when {
                entity.newInstalled -> SnapshotDetailNewInstallView(this)
                entity.deleted -> SnapshotDetailDeletedView(this)
                else -> SnapshotEmptyView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.gravity = Gravity.CENTER_HORIZONTAL
                    }
                    addPaddingTop(96.dp)
                }
            }
        )
        adapter.setOnItemClickListener { _, view, position ->
            if (adapter.data[position] is SnapshotTitleNode) {
                adapter.expandOrCollapse(position)
                return@setOnItemClickListener
            }
            if (AntiShakeUtils.isInvalidClick(view)) {
                return@setOnItemClickListener
            }

            val item = (adapter.data[position] as BaseSnapshotNode).item
            if (item.diffType == REMOVED || item.itemType == PERMISSION) {
                return@setOnItemClickListener
            }

            lifecycleScope.launch {
                val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
                startActivity(
                    Intent(this@SnapshotDetailActivity, AppDetailActivity::class.java)
                        .putExtras(
                            bundleOf(
                                EXTRA_PACKAGE_NAME to entity.packageName,
                                EXTRA_REF_NAME to item.name,
                                EXTRA_REF_TYPE to item.itemType,
                                EXTRA_DETAIL_BEAN to DetailExtraBean(
                                    lcItem.isSplitApk,
                                    lcItem.isKotlinUsed,
                                    lcItem.variant
                                )
                            )
                        )
                )
            }
        }
    }

    private fun getNodeList(list: List<SnapshotDetailItem>): MutableList<BaseNode> {
        val returnList = mutableListOf<BaseNode>()

        if (list.isEmpty()) return returnList

        if (list[0].itemType == NATIVE) {
            list.forEach { returnList.add(SnapshotNativeNode(it)) }
        } else {
            list.forEach { returnList.add(SnapshotComponentNode(it)) }
        }

        return returnList
    }

    private fun <T> getDiffString(
        diff: SnapshotDiffItem.DiffNode<T>,
        isNewOrDeleted: Boolean = false,
        format: String = "%s"
    ): String {
        return if (diff.old != diff.new && !isNewOrDeleted) {
            "${String.format(format, diff.old.toString())} $ARROW ${
                String.format(
                    format,
                    diff.new.toString()
                )
            }"
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
            "${
                String.format(
                    format,
                    diff1.old.toString(),
                    diff2.old.toString()
                )
            } $ARROW ${String.format(format, diff1.new.toString(), diff2.new.toString())}"
        } else {
            String.format(format, diff1.old.toString(), diff2.old.toString())
        }
    }
}
