package com.absinthe.libchecker.features.snapshot.ui

import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.SnapshotPackageIconSource
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.SnapshotViewModel
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotNoDiffBSView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import org.koin.androidx.viewmodel.ext.android.activityViewModel

const val EXTRA_DIFF_ITEM = "EXTRA_DIFF_ITEM"

class SnapshotNoDiffBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotNoDiffBSView>() {

  private val viewModel: SnapshotViewModel by activityViewModel()

  override fun initRootView(): SnapshotNoDiffBSView = SnapshotNoDiffBSView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val arg = arguments ?: run {
      dismiss()
      return
    }
    BundleCompat.getSerializable(arg, EXTRA_DIFF_ITEM, SnapshotDiffItem::class.java)?.let { item ->
      root.title.apply {
        bindIcon(item)
        val isNewOrDeleted = item.deleted || item.newInstalled
        appNameView.text = LCAppUtils.getDiffString(item.labelDiff, isNewOrDeleted)
        iconView.contentDescription = appNameView.text
        packageNameView.text = item.packageName
        versionInfoView.text = LCAppUtils.getDiffString(
          diff1 = item.versionNameDiff,
          diff2 = item.versionCodeDiff,
          isNewOrDeleted = isNewOrDeleted
        )
        setApisText(item, isNewOrDeleted)
        setPackageSizeText(item, isNewOrDeleted)
      }

      when {
        item.newInstalled -> {
          root.setMode(SnapshotNoDiffBSView.Mode.New)
        }

        item.deleted -> {
          root.setMode(SnapshotNoDiffBSView.Mode.Deleted)
        }

        item.isNothingChanged() -> {
          root.setMode(SnapshotNoDiffBSView.Mode.NothingChanged)
        }

        else -> {
          dismiss()
        }
      }
    } ?: run {
      dismiss()
    }
  }

  private fun bindIcon(item: SnapshotDiffItem) {
    root.title.iconView.apply {
      setImageResource(R.drawable.ic_icon_blueprint)
      setOnClickListener(null)
    }
    lifecycleScope.launch {
      when (val iconSource = viewModel.getSnapshotPackageIconSources(listOf(item.packageName))[item.packageName]) {
        is SnapshotPackageIconSource.InstalledPackage -> {
          val ctx = context ?: return@launch
          val appIconLoader = AppIconLoader(
            resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
            false,
            ctx
          )
          val icon = iconSource.packageInfo.applicationInfo?.let { applicationInfo ->
            runCatching {
              appIconLoader.loadIcon(applicationInfo)
            }.getOrNull()
          }
          root.title.iconView.load(icon)
          root.title.iconView.setOnClickListener {
            lifecycleScope.launch {
              val lcItem = viewModel.getAppListItem(item.packageName) ?: return@launch
              activity?.launchDetailPage(lcItem)
            }
          }
        }

        SnapshotPackageIconSource.Fallback,
        null -> root.title.iconView.setImageResource(R.drawable.ic_icon_blueprint)
      }
    }
  }

  companion object {
    fun newInstance(snapshotDiffItem: SnapshotDiffItem): SnapshotNoDiffBSDFragment {
      return SnapshotNoDiffBSDFragment().putArguments(
        EXTRA_DIFF_ITEM to snapshotDiffItem
      )
    }
  }
}
