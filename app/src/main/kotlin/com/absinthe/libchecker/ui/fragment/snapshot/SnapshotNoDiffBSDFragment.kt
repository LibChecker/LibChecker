package com.absinthe.libchecker.ui.fragment.snapshot

import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.compat.BundleCompat
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.recyclerview.adapter.snapshot.ARROW
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.view.snapshot.SnapshotNoDiffBSView
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
                activity?.let { activity ->
                  LCAppUtils.launchDetailPage(activity, lcItem)
                }
              }
            }
          } ?: run {
            setImageResource(R.drawable.ic_icon_blueprint)
          }
        }
        appNameView.text = item.labelDiff.old
        packageNameView.text = item.packageName
        versionInfoView.text = getDiffString(
          item.versionNameDiff,
          item.versionCodeDiff,
          item.deleted || item.newInstalled,
          "%s (%s)"
        )
        targetApiView.text = String.format("API %s", item.targetApiDiff.old)
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

  private fun getDiffString(
    diff1: SnapshotDiffItem.DiffNode<*>,
    diff2: SnapshotDiffItem.DiffNode<*>,
    isNewOrDeleted: Boolean = false,
    format: String = "%s"
  ): String {
    return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
      "${format.format(diff1.old, diff2.old)} $ARROW ${format.format(diff1.new, diff2.new)}"
    } else {
      format.format(diff1.old, diff2.old)
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
