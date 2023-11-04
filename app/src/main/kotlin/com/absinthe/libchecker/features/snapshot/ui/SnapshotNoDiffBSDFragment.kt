package com.absinthe.libchecker.features.snapshot.ui

import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.BundleCompat
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.features.snapshot.detail.ui.view.SnapshotNoDiffBSView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader

const val EXTRA_DIFF_ITEM = "EXTRA_DIFF_ITEM"

class SnapshotNoDiffBSDFragment : BaseBottomSheetViewDialogFragment<SnapshotNoDiffBSView>() {

  override fun initRootView(): SnapshotNoDiffBSView = SnapshotNoDiffBSView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    val arg = arguments ?: run {
      dismiss()
      return
    }
    BundleCompat.getSerializable<SnapshotDiffItem>(arg, EXTRA_DIFF_ITEM)?.let { item ->
      val packageInfo = runCatching { PackageUtils.getPackageInfo(item.packageName) }.getOrNull()
      root.title.apply {
        iconView.apply {
          packageInfo?.let { pi ->
            val appIconLoader = AppIconLoader(
              resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size),
              false,
              requireContext()
            )
            val icon = runCatching {
              appIconLoader.loadIcon(pi.applicationInfo)
            }.getOrNull()
            load(icon)
            setOnClickListener {
              lifecycleScope.launch {
                val lcItem = Repositories.lcRepository.getItem(item.packageName) ?: return@launch
                activity?.launchDetailPage(lcItem)
              }
            }
          } ?: run {
            setImageResource(R.drawable.ic_icon_blueprint)
          }
        }
        val isNewOrDeleted = item.deleted || item.newInstalled
        appNameView.text = LCAppUtils.getDiffString(item.labelDiff, isNewOrDeleted)
        packageNameView.text = item.packageName
        versionInfoView.text = LCAppUtils.getDiffString(
          item.versionNameDiff,
          item.versionCodeDiff,
          isNewOrDeleted,
          "%s (%s)"
        )
        targetApiView.text = LCAppUtils.getDiffString(item.targetApiDiff, isNewOrDeleted, "API %s")
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

  companion object {
    fun newInstance(snapshotDiffItem: SnapshotDiffItem): SnapshotNoDiffBSDFragment {
      return SnapshotNoDiffBSDFragment().putArguments(
        EXTRA_DIFF_ITEM to snapshotDiffItem
      )
    }
  }
}
