package com.absinthe.libchecker.domain.snapshot.detail.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.isComponentType
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.domain.app.detail.ui.AppBarStateChangeListener
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailSection
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportHeader
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailAdapter
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.BaseSnapshotNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotComponentNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailCountNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotReportNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailDeletedView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailNewInstallView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import rikka.core.util.ClipboardUtils

const val EXTRA_ENTITY = "EXTRA_ENTITY"
const val EXTRA_ICON = "EXTRA_ICON"

class SnapshotDetailActivity :
  CheckPackageOnResumingActivity<ActivitySnapshotDetailBinding>(),
  MenuProvider {

  private lateinit var entity: SnapshotDiffItem
  private lateinit var snapshotTitleDisplayData: SnapshotTitleDisplayData

  private val adapter by lazy { SnapshotDetailAdapter() }
  private val viewModel: SnapshotViewModel by viewModel()
  private val buildSnapshotTitleDisplayData: BuildSnapshotTitleDisplayDataUseCase by inject()
  private val _entity by unsafeLazy {
    IntentCompat.getSerializableExtra<SnapshotDiffItem>(
      intent,
      EXTRA_ENTITY
    )
  }
  private val _icon by unsafeLazy {
    IntentCompat.getParcelableExtra<Bitmap>(
      intent,
      EXTRA_ICON
    )
  }

  override fun requirePackageName() = entity.packageName.takeIf { it.contains("/").not() }

  override fun onApplyContentWindowInsets() {
    binding.root.applySystemBarsPadding(left = true, top = true, right = true)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (_entity != null) {
      entity = _entity!!
      initView()
      viewModel.computeDiffDetail(entity)
    } else {
      finish()
    }
  }

  override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
    menuInflater.inflate(R.menu.snapshot_detail_menu, menu)
  }

  override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
    if (menuItem.itemId == android.R.id.home) {
      finish()
    } else if (menuItem.itemId == R.id.report_generate) {
      generateReport()
    }
    return true
  }

  private fun initView() {
    addMenuProvider(this, this, Lifecycle.State.CREATED)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
      title = null
    }

    binding.apply {
      collapsingToolbar.also {
        it.setOnApplyWindowInsetsListener(null)
        it.title = entity.labelDiff.new ?: entity.labelDiff.old
      }
      headerLayout.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
        override fun onStateChanged(appBarLayout: AppBarLayout, state: State) {
          collapsingToolbar.isTitleEnabled = state == State.COLLAPSED
          headerLayout.isLifted = state == State.COLLAPSED
        }
      })
      list.apply {
        adapter = this@SnapshotDetailActivity.adapter
        applySystemBarsPadding(bottom = true)
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        addItemDecoration(VerticalSpacesItemDecoration(4.dp))
      }

      snapshotTitle.iconView.apply {
        bindSnapshotIcon()
        setOnClickListener {
          lifecycleScope.launch {
            val lcItem = viewModel.getAppListItem(entity.packageName) ?: return@launch
            launchDetailPage(lcItem)
          }
        }
      }
      snapshotTitleDisplayData = buildSnapshotTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = entity,
          formatSplitPackageName = true
        )
      )
      snapshotTitle.render(snapshotTitleDisplayData)
    }

    adapter.stateView =
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
    adapter.isStateViewEnable = entity.newInstalled || entity.deleted
    adapter.setOnItemClickListener { _, view, position ->
      if (adapter.data[position] is SnapshotTitleNode) {
        adapter.expandOrCollapse(position)
        return@setOnItemClickListener
      }
      if (AntiShakeUtils.isInvalidClick(view)) {
        return@setOnItemClickListener
      }

      val item = (adapter.data[position] as BaseSnapshotNode).item
      if (item.diffType == REMOVED) {
        return@setOnItemClickListener
      }

      lifecycleScope.launch {
        val lcItem = viewModel.getAppListItem(entity.packageName) ?: return@launch
        launchDetailPage(
          item = lcItem,
          refName = item.name,
          refType = item.itemType
        )
      }
    }
    adapter.setOnItemLongClickListener { _, _, position ->
      val node = adapter.data[position] as? BaseSnapshotNode ?: return@setOnItemLongClickListener false
      val item = node.item
      // if (item.diffType == REMOVED) {
      //   return@setOnItemLongClickListener false
      // }
      if (isComponentType(item.itemType) && item.name.startsWith(entity.packageName)) {
        return@setOnItemLongClickListener false
      }
      launchLibReferencePage(item.name, node.referenceLabel, item.itemType, null)
      true
    }

    viewModel.snapshotDetailSectionsFlow.onEach { sections ->
      val titleList = sections.map(::buildSnapshotTitleNode)

      adapter.isStateViewEnable = titleList.isEmpty()
      adapter.setList(titleList)
    }.launchIn(lifecycleScope)
  }

  private fun buildSnapshotTitleNode(section: SnapshotDetailSection): SnapshotTitleNode {
    val nodes = section.items.mapTo(mutableListOf<BaseNode>()) { item ->
      when (section.type) {
        NATIVE, METADATA -> SnapshotNativeNode(item)
        else -> SnapshotComponentNode(item)
      }
    }
    recordDetailComponentCount(section.type, nodes.size)
    return SnapshotTitleNode(
      childNode = nodes,
      type = section.type,
      title = section.title,
      reportText = section.reportText,
      expandedDescription = section.expandedDescription,
      collapsedDescription = section.collapsedDescription,
      counts = section.statusCounts.map {
        SnapshotDetailCountNode(
          count = it.count,
          countText = it.countText,
          status = it.status
        )
      }
    )
  }

  private fun recordDetailComponentCount(@LibType type: Int, count: Int) {
    Telemetry.recordEvent(
      Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
      mapOf(getTelemetryComponentName(type) to count.toLong())
    )
  }

  private fun getTelemetryComponentName(@LibType type: Int): String {
    return when (type) {
      NATIVE -> "Native"
      SERVICE -> "Service"
      ACTIVITY -> "Activity"
      RECEIVER -> "Receiver"
      PROVIDER -> "Provider"
      PERMISSION -> "Permission"
      METADATA -> "Metadata"
      else -> "Unknown"
    }
  }

  private fun bindSnapshotIcon() {
    val snapshotIcon = _icon?.takeIf { entity.packageName.contains("/") }
    if (snapshotIcon != null) {
      binding.snapshotTitle.iconView.load(snapshotIcon)
      return
    }

    binding.snapshotTitle.iconView.setImageResource(R.drawable.ic_icon_blueprint)
    lifecycleScope.launch {
      when (val iconSource = viewModel.getSnapshotPackageIconSources(listOf(entity.packageName))[entity.packageName]) {
        is SnapshotPackageIconSource.InstalledPackage -> {
          val appIconLoader = AppIconLoader(
            resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
            false,
            this@SnapshotDetailActivity
          )
          val icon = iconSource.packageInfo.applicationInfo?.let { applicationInfo ->
            runCatching {
              appIconLoader.loadIcon(applicationInfo)
            }.getOrNull()
          }
          binding.snapshotTitle.iconView.load(icon)
        }

        SnapshotPackageIconSource.Fallback,
        null -> binding.snapshotTitle.iconView.setImageResource(R.drawable.ic_icon_blueprint)
      }
    }
  }

  private fun generateReport() {
    val sb = StringBuilder()
    sb.append(buildSnapshotDetailReportHeader(snapshotTitleDisplayData))
    sb.appendLine()

    adapter.data.forEach { node ->
      if (node is SnapshotReportNode) {
        sb.append(node.reportText)
      }
    }
    ClipboardUtils.put(this, sb.toString())
    VersionCompat.showCopiedOnClipboardToast(this)
  }
}
