package com.absinthe.libchecker.ui.detail

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
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
import com.absinthe.libchecker.bean.ADDED
import com.absinthe.libchecker.bean.CHANGED
import com.absinthe.libchecker.bean.MOVED
import com.absinthe.libchecker.bean.REMOVED
import com.absinthe.libchecker.bean.SnapshotDetailItem
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.compat.IntentCompat
import com.absinthe.libchecker.compat.VersionCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.databinding.ActivitySnapshotDetailBinding
import com.absinthe.libchecker.recyclerview.VerticalSpacesItemDecoration
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.recyclerview.adapter.snapshot.SnapshotDetailAdapter
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.BaseSnapshotNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotComponentNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotNativeNode
import com.absinthe.libchecker.recyclerview.adapter.snapshot.node.SnapshotTitleNode
import com.absinthe.libchecker.ui.app.CheckPackageOnResumingActivity
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import com.absinthe.libchecker.view.detail.AppBarStateChangeListener
import com.absinthe.libchecker.view.snapshot.SnapshotDetailDeletedView
import com.absinthe.libchecker.view.snapshot.SnapshotDetailNewInstallView
import com.absinthe.libchecker.view.snapshot.SnapshotEmptyView
import com.absinthe.libchecker.viewmodel.SnapshotViewModel
import com.absinthe.libraries.utils.utils.AntiShakeUtils
import com.chad.library.adapter.base.entity.node.BaseNode
import com.google.android.material.appbar.AppBarLayout
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.analytics.EventProperties
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import rikka.core.util.ClipboardUtils

const val EXTRA_ENTITY = "EXTRA_ENTITY"

class SnapshotDetailActivity :
  CheckPackageOnResumingActivity<ActivitySnapshotDetailBinding>(),
  MenuProvider {

  private lateinit var entity: SnapshotDiffItem

  private val adapter = SnapshotDetailAdapter()
  private val viewModel: SnapshotViewModel by viewModels()
  private val _entity by unsafeLazy {
    IntentCompat.getSerializableExtra<SnapshotDiffItem>(
      intent,
      EXTRA_ENTITY,
    )
  }

  override fun requirePackageName() = entity.packageName

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
    addMenuProvider(this)
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
          this@SnapshotDetailActivity,
        )
        runCatching {
          val icon = appIconLoader.loadIcon(
            PackageUtils.getPackageInfo(
              entity.packageName,
              PackageManager.GET_META_DATA,
            ).applicationInfo,
          )
          load(icon)
        }
        setOnClickListener {
          lifecycleScope.launch {
            val lcItem = Repositories.lcRepository.getItem(entity.packageName) ?: return@launch
            LCAppUtils.launchDetailPage(this@SnapshotDetailActivity, lcItem)
          }
        }
      }
      snapshotTitle.appNameView.text = getDiffString(entity.labelDiff, isNewOrDeleted)
      snapshotTitle.packageNameView.text = entity.packageName
      snapshotTitle.versionInfoView.text = getDiffString(
        entity.versionNameDiff,
        entity.versionCodeDiff,
        isNewOrDeleted,
        "%s (%s)",
      )
      snapshotTitle.targetApiView.text =
        String.format("API %s", getDiffString(entity.targetApiDiff, isNewOrDeleted))

      if (entity.packageSizeDiff.old > 0L) {
        snapshotTitle.packageSizeView.isVisible = true
        val sizeDiff = SnapshotDiffItem.DiffNode(
          entity.packageSizeDiff.old.sizeToString(this@SnapshotDetailActivity),
          entity.packageSizeDiff.new?.sizeToString(this@SnapshotDetailActivity),
        )
        snapshotTitle.packageSizeView.text = getDiffString(sizeDiff, isNewOrDeleted)
      } else {
        snapshotTitle.packageSizeView.isVisible = false
      }
    }

    viewModel.snapshotDetailItems.observe(this) { details ->
      val titleList = mutableListOf<SnapshotTitleNode>()

      getNodeList(details.filter { it.itemType == NATIVE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, NATIVE))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Native", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == SERVICE }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, SERVICE))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Service", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == ACTIVITY }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, ACTIVITY))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Activity", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == RECEIVER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, RECEIVER))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Receiver", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == PROVIDER }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PROVIDER))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Provider", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == PERMISSION }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, PERMISSION))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Permission", this.size.toLong()),
          )
        }
      }
      getNodeList(details.filter { it.itemType == METADATA }).apply {
        if (isNotEmpty()) {
          titleList.add(SnapshotTitleNode(this, METADATA))
          Analytics.trackEvent(
            Constants.Event.SNAPSHOT_DETAIL_COMPONENT_COUNT,
            EventProperties().set("Metadata", this.size.toLong()),
          )
        }
      }

      if (titleList.isNotEmpty()) {
        adapter.setList(titleList)
      }
    }

    adapter.setEmptyView(
      when {
        entity.newInstalled -> SnapshotDetailNewInstallView(this)
        entity.deleted -> SnapshotDetailDeletedView(this)
        else -> SnapshotEmptyView(this).apply {
          layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
          ).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
          }
          addPaddingTop(96.dp)
        }
      },
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
        LCAppUtils.launchDetailPage(
          this@SnapshotDetailActivity,
          item = lcItem,
          refName = item.name,
          refType = item.itemType,
        )
      }
    }
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

  private fun <T> getDiffString(
    diff: SnapshotDiffItem.DiffNode<T>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s",
  ): String {
    return if (diff.old != diff.new && !isNewOrDeleted) {
      "${format.format(diff.old)} $ARROW ${format.format(diff.new)}"
    } else {
      format.format(diff.old)
    }
  }

  private fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff2: SnapshotDiffItem.DiffNode<*>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s",
  ): String {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      "${format.format(diff1.old, diff2.old)} $ARROW ${format.format(diff1.new, diff2.new)}"
    } else {
      format.format(diff1.old, diff2.old)
    }
  }

  private fun generateReport() {
    val sb = StringBuilder()
    sb.append(binding.snapshotTitle.appNameView.text).appendLine()
      .append(binding.snapshotTitle.packageNameView.text).appendLine()
      .append(binding.snapshotTitle.versionInfoView.text).appendLine()
      .append(binding.snapshotTitle.targetApiView.text).appendLine()

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
