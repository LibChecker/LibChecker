package com.absinthe.libchecker.ui.fragment.snapshot

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.SnapshotDiffItem
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libchecker.view.snapshot.SnapshotNewOrDeletedBSView
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader

const val EXTRA_DIFF_ITEM = "EXTRA_DIFF_ITEM"

class SnapshotNewOrDeletedBSDFragment :
  BaseBottomSheetViewDialogFragment<SnapshotNewOrDeletedBSView>() {

  override fun initRootView(): SnapshotNewOrDeletedBSView =
    SnapshotNewOrDeletedBSView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  @SuppressLint("SetTextI18n")
  override fun init() {
    (arguments?.getSerializable(EXTRA_DIFF_ITEM) as? SnapshotDiffItem)?.let { item ->
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
        versionInfoView.text = "${item.versionNameDiff.old} (${item.versionCodeDiff.old})"
        targetApiView.text = "API ${item.targetApiDiff.old}"
      }

      when {
        item.newInstalled -> {
          root.setMode(SnapshotNewOrDeletedBSView.Mode.New)
        }
        item.deleted -> {
          root.setMode(SnapshotNewOrDeletedBSView.Mode.Deleted)
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
    fun newInstance(snapshotDiffItem: SnapshotDiffItem): SnapshotNewOrDeletedBSDFragment {
      return SnapshotNewOrDeletedBSDFragment().putArguments(
        EXTRA_DIFF_ITEM to snapshotDiffItem
      )
    }
  }
}
