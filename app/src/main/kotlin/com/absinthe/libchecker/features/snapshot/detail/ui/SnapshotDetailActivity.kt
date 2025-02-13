package com.absinthe.libchecker.features.snapshot.detail.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.core.view.descendants
import androidx.core.view.isVisible
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
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.features.applist.detail.AppBarStateChangeListener
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.detail.bean.ADDED
import com.absinthe.libchecker.features.snapshot.detail.bean.CHANGED
import com.absinthe.libchecker.features.snapshot.detail.bean.MOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.REMOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDetailItem
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.SnapshotDetailAdapter
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.BaseSnapshotNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotComponentNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotNativeNode
import com.absinthe.libchecker.features.snapshot.detail.ui.adapter.node.SnapshotTitleNode
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailDeletedView
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotDetailNewInstallView
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotEmptyView
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.ui.adapter.VerticalSpacesItemDecoration
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.launchLibReferencePage
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import rikka.core.util.ClipboardUtils

const val EXTRA_ENTITY = "EXTRA_ENTITY"
const val EXTRA_ICON = "EXTRA_ICON"

class SnapshotDetailActivity :
  CheckPackageOnResumingActivity<ActivitySnapshotDetailBinding>(),
  MenuProvider {

  private lateinit var entity: SnapshotDiffItem

  private val adapter = SnapshotDetailAdapter()
  private val viewModel: SnapshotViewModel by viewModels()
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (_entity != null) {
      entity = _entity!!
      initView()
      viewModel.computeDiffDetail(this, entity)
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
    addMenuProvider(this, this, Lifecycle.State.STARTED)
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
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        addItemDecoration(VerticalSpacesItemDecoration(4.dp))
      }

      val isNewOrDeleted = entity.deleted || entity.newInstalled

      snapshotTitle.iconView.apply {
        val appIconLoader = AppIconLoader(
          resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
          false,
          this@SnapshotDetailActivity
        )
        runCatching {
          val icon = _icon?.takeIf { entity.packageName.contains("/") } ?: appIconLoader.loadIcon(
            PackageUtils.getPackageInfo(entity.packageName).applicationInfo!!
          )
          load(icon)
        }
        setOnClickListener {
          lifecycleScope.launch {
            val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
            launchDetailPage(lcItem)
          }
        }
      }
      snapshotTitle.appNameView.text = LCAppUtils.getDiffString(entity.labelDiff, isNewOrDeleted)

      val pkgSplits = entity.packageName.split("/")
      val first = pkgSplits[0]
      val second = pkgSplits.getOrNull(1)
      snapshotTitle.packageNameView.text = if (second != null && second != first) "$first $ARROW $second" else first
      snapshotTitle.versionInfoView.text = LCAppUtils.getDiffString(
        diff1 = entity.versionNameDiff,
        diff2 = entity.versionCodeDiff,
        isNewOrDeleted = isNewOrDeleted
      )

      snapshotTitle.setApisText(entity, isNewOrDeleted)
      snapshotTitle.setPackageSizeText(entity, isNewOrDeleted)
    }

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
      if (item.diffType == REMOVED) {
        return@setOnItemClickListener
      }

      lifecycleScope.launch {
        val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
        launchDetailPage(
          item = lcItem,
          refName = item.name,
          refType = item.itemType
        )
      }
    }
    adapter.setOnItemLongClickListener { _, view, position ->
      val item = (adapter.data[position] as? BaseSnapshotNode)?.item ?: return@setOnItemLongClickListener false
      // if (item.diffType == REMOVED) {
      //   return@setOnItemLongClickListener false
      // }
      if (isComponentType(item.itemType) && item.name.startsWith(entity.packageName)) {
        return@setOnItemLongClickListener false
      }
      val label = ((view as? ViewGroup)?.descendants?.find { it is Chip } as? Chip)?.text?.toString()
      launchLibReferencePage(item.name, label, item.itemType, null)
      true
    }

    viewModel.snapshotDetailItemsFlow.onEach { details ->
      val titleList = mutableListOf<SnapshotTitleNode>()

      getNodeList(details.filter { it.itemType == NATIVE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, NATIVE))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Native" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == SERVICE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, SERVICE))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Service" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == ACTIVITY }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, ACTIVITY))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Activity" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == RECEIVER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, RECEIVER))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Receiver" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == PROVIDER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PROVIDER))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Provider" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == PERMISSION }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PERMISSION))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Permission" to this.size.toLong())
          )
        }
      }
      getNodeList(details.filter { it.itemType == METADATA }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, METADATA))
          Telemetry.recordEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            mapOf("Metadata" to this.size.toLong())
          )
        }
      }

      if (titleList.isNotEmpty()) {
        adapter.setList(titleList)
      }
    }.launchIn(lifecycleScope)
  }

  private fun getNodeList(list: List<SnapshotDetailItem>): MutableList<BaseNode> {
    val returnList = mutableListOf<BaseNode>()

    if (list.isEmpty()) return returnList

    when (list[0].itemType) {
      NATIVE, METADATA -> list.forEach { returnList.add(SnapshotNativeNode(it)) }
      else -> list.forEach { returnList.add(SnapshotComponentNode(it)) }
    }

    return returnList
  }

  private fun generateReport() {
    val sb = StringBuilder()
    sb.append(binding.snapshotTitle.appNameView.text).appendLine()
      .append(binding.snapshotTitle.packageNameView.text).appendLine()
      .append(binding.snapshotTitle.versionInfoView.text).appendLine()
      .append(binding.snapshotTitle.apisView.text).appendLine()

    if (binding.snapshotTitle.packageSizeView.isVisible) {
      sb.append(binding.snapshotTitle.packageSizeView.text).appendLine()
    }

    sb.appendLine()

    adapter.data.forEach {
      when (it) {
        is SnapshotTitleNode -> {
          sb.append("[${getComponentName(it.type)}]").appendLine()
        }

        is SnapshotComponentNode -> {
          sb.append(getDiffTypeLabel(it.item.diffType))
            .append(" ")
            .append(it.item.title)
            .appendLine()
        }

        is SnapshotNativeNode -> {
          sb.append(getDiffTypeLabel(it.item.diffType))
            .append(" ")
            .append(it.item.title)
            .appendLine()
            .append("\t")
            .append(it.item.extra)
            .appendLine()
        }
      }
    }
    ClipboardUtils.put(this, sb.toString())
    VersionCompat.showCopiedOnClipboardToast(this)
  }

  private fun getComponentName(@LibType type: Int): String {
    val titleRes = when (type) {
      NATIVE -> R.string.ref_category_native
      SERVICE -> R.string.ref_category_service
      ACTIVITY -> R.string.ref_category_activity
      RECEIVER -> R.string.ref_category_br
      PROVIDER -> R.string.ref_category_cp
      PERMISSION -> R.string.ref_category_perm
      METADATA -> R.string.ref_category_metadata
      else -> android.R.string.untitled
    }
    return getString(titleRes)
  }

  private fun getDiffTypeLabel(diffType: Int): String {
    return when (diffType) {
      ADDED -> "🟢+"
      REMOVED -> "🔴-"
      CHANGED -> "🟡~"
      MOVED -> "🔵<->"
      else -> throw IllegalArgumentException("wrong diff type")
    }
  }
}
