package com.absinthe.libchecker.domain.snapshot.detail.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import com.absinthe.libchecker.R
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.options.SnapshotOptions
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotDetailDiffTextStyle
import com.absinthe.libchecker.domain.snapshot.detail.model.SnapshotTitleDisplayData
import com.absinthe.libchecker.domain.snapshot.detail.model.buildSnapshotDetailReportHeader
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.SnapshotDetailAdapter
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailNodeClickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotDetailNodeLongClickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.SnapshotReportNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.clickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.longClickAction
import com.absinthe.libchecker.domain.snapshot.detail.ui.adapter.node.toSnapshotTitleNode
import com.absinthe.libchecker.domain.snapshot.detail.ui.model.toRenderState
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotCollapsedToolbarView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailDeletedView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotDetailNewInstallView
import com.absinthe.libchecker.domain.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.domain.snapshot.detail.usecase.BuildSnapshotTitleDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.applySystemBarsPadding
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.google.android.material.R as MaterialR
import kotlin.math.abs
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
  private val collapsedToolbarView by lazy { SnapshotCollapsedToolbarView(this) }
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
      val diffTextStyle = buildDiffTextStyle()
      initView(diffTextStyle)
      viewModel.computeDiffDetail(entity, diffTextStyle)
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

  private fun initView(diffTextStyle: SnapshotDetailDiffTextStyle) {
    addMenuProvider(this, this, Lifecycle.State.CREATED)
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setDisplayShowHomeEnabled(true)
      title = null
    }
    setupCollapsedToolbar()

    binding.apply {
      collapsingToolbar.also {
        it.setOnApplyWindowInsetsListener(null)
        it.isTitleEnabled = false
      }
      headerLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
        val totalScrollRange = appBarLayout.totalScrollRange
        val collapseFraction = if (totalScrollRange > 0) {
          abs(verticalOffset).toFloat() / totalScrollRange
        } else {
          0f
        }
        collapsedToolbarView.updateCollapseFraction(collapseFraction)
        headerLayout.isLifted = totalScrollRange > 0 && abs(verticalOffset) >= totalScrollRange
      }
      list.apply {
        adapter = this@SnapshotDetailActivity.adapter
        applySystemBarsPadding(bottom = true)
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      }

      snapshotTitle.apply {
        bindSnapshotIcon()
        setIconClickListener {
          lifecycleScope.launch {
            val lcItem = viewModel.getAppListItem(entity.packageName) ?: return@launch
            launchDetailPage(lcItem)
          }
        }
      }
      snapshotTitleDisplayData = buildSnapshotTitleDisplayData(
        BuildSnapshotTitleDisplayDataUseCase.Request(
          item = entity,
          formatSplitPackageName = true,
          diffTextStyle = diffTextStyle
        )
      )
      snapshotTitle.render(snapshotTitleDisplayData.toRenderState())
      collapsedToolbarView.bindAppName(snapshotTitleDisplayData.appName)
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
      when (val action = adapter.data[position].clickAction) {
        SnapshotDetailNodeClickAction.ToggleSection -> {
          adapter.expandOrCollapse(position)
          return@setOnItemClickListener
        }

        is SnapshotDetailNodeClickAction.OpenDetail -> {
          if (AntiShakeUtils.isInvalidClick(view)) {
            return@setOnItemClickListener
          }
          lifecycleScope.launch {
            val lcItem = viewModel.getAppListItem(entity.packageName) ?: return@launch
            launchDetailPage(
              item = lcItem,
              refName = action.target.refName,
              refType = action.target.refType
            )
          }
        }

        null -> return@setOnItemClickListener
      }
    }
    adapter.setOnItemLongClickListener { _, _, position ->
      when (val action = adapter.data[position].longClickAction(entity.packageName)) {
        is SnapshotDetailNodeLongClickAction.OpenReference -> {
          val target = action.target
          launchLibReferencePage(target.refName, target.label, target.refType, null)
          true
        }

        null -> false
      }
    }

    viewModel.snapshotDetailContentFlow.onEach { content ->
      binding.snapshotTitle.render(
        snapshotTitleDisplayData.toRenderState(summary = content.summary)
      )
      val titleList = content.sections.map { section ->
        recordDetailComponentCount(section.type, section.items.size)
        section.toSnapshotTitleNode()
      }

      adapter.isStateViewEnable = titleList.isEmpty()
      adapter.setList(titleList)
    }.launchIn(lifecycleScope)
  }

  private fun buildDiffTextStyle(): SnapshotDetailDiffTextStyle {
    return SnapshotDetailDiffTextStyle(
      highlightColor = if ((viewModel.getSnapshotOptions() and SnapshotOptions.DIFF_HIGHLIGHT) > 0) {
        getColorByAttr(androidx.appcompat.R.attr.colorPrimary)
      } else {
        null
      },
      emphasizeDiffs = (viewModel.getSnapshotOptions() and SnapshotOptions.DIFF_EMPHASIS) > 0,
      arrowColor = getColorByAttr(MaterialR.attr.colorOnSurface)
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
      binding.snapshotTitle.setIconImage(snapshotIcon)
      collapsedToolbarView.setIconImage(snapshotIcon)
      return
    }

    binding.snapshotTitle.setFallbackIcon()
    collapsedToolbarView.setFallbackIcon()
    lifecycleScope.launch {
      when (val iconSource = viewModel.getSnapshotPackageIconSources(listOf(entity.packageName))[entity.packageName]) {
        null -> {
          binding.snapshotTitle.setFallbackIcon()
          collapsedToolbarView.setFallbackIcon()
        }

        else -> {
          binding.snapshotTitle.setIconSource(iconSource)
          collapsedToolbarView.setIconSource(iconSource)
        }
      }
    }
  }

  private fun setupCollapsedToolbar() {
    if (collapsedToolbarView.parent != null) return
    binding.toolbar.addView(
      collapsedToolbarView,
      Toolbar.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      ).apply {
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
      }
    )
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
