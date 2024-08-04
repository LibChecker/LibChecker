package com.absinthe.libchecker.features.settings.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.settings.SettingsViewModel
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getCompileSdkVersion
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InAppUpdateDialogFragment : BaseBottomSheetViewDialogFragment<InAppUpdateDialogView>() {

  private val viewModel by viewModels<SettingsViewModel>()
  private var getAppUpdateInfo: GetAppUpdateInfo? = null

  override fun initRootView(): InAppUpdateDialogView = InAppUpdateDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    root.toggleGroup.addOnButtonCheckedListener { toggleGroup, checkedId, isChecked ->
      if (isChecked) {
        doOnMainThreadIdle { toggleGroup.isEnabled = false }
        root.updateButton.isEnabled = false
        root.showLoading()
        viewModel.requestUpdate(checkedId == com.absinthe.libchecker.R.id.in_app_update_chip_stable)
      }
    }
    root.updateButton.setOnClickListener {
      getAppUpdateInfo?.app?.link?.let { url ->
        viewModel.downloadApk(url)
        Toasty.showLong(requireContext(), com.absinthe.libchecker.R.string.toast_downloading_app)
      }
    }
    viewModel.respStateFlow.onEach {
      getAppUpdateInfo = it
      val localVersionCode = PackageUtils.getVersionCode(BuildConfig.APPLICATION_ID)
      val info = it.takeIf { (it?.app?.versionCode ?: 0) > localVersionCode }

      root.apply {
        doOnMainThreadIdle { toggleGroup.isEnabled = true }
        updateButton.isEnabled = info != null
        setItem(provideSnapshotDiffItem(info))
        showContent()
      }
    }.launchIn(lifecycleScope)

    root.updateButton.isEnabled = false
    viewModel.requestUpdate(true)
  }

  private fun provideSnapshotDiffItem(getAppUpdateInfo: GetAppUpdateInfo?): SnapshotDiffItem {
    val pi = PackageUtils.getPackageInfo(BuildConfig.APPLICATION_ID)
    val ai = pi.applicationInfo!!
    return SnapshotDiffItem(
      packageName = pi.packageName,
      updateTime = System.currentTimeMillis(),
      labelDiff = SnapshotDiffItem.DiffNode(ai.loadLabel(SystemServices.packageManager).toString()),
      versionNameDiff = SnapshotDiffItem.DiffNode(
        pi.versionName.toString(),
        if (getAppUpdateInfo == null) pi.versionName.toString() else getAppUpdateInfo.app.version
      ),
      versionCodeDiff = SnapshotDiffItem.DiffNode(
        pi.getVersionCode(),
        if (getAppUpdateInfo == null) pi.getVersionCode() else getAppUpdateInfo.app.versionCode.toLong()
      ),
      abiDiff = SnapshotDiffItem.DiffNode(PackageUtils.getAbi(pi).toShort()),
      targetApiDiff = SnapshotDiffItem.DiffNode(
        ai.targetSdkVersion.toShort(),
        if (getAppUpdateInfo == null) ai.targetSdkVersion.toShort() else getAppUpdateInfo.app.extra.target.toShort()
      ),
      compileSdkDiff = SnapshotDiffItem.DiffNode(
        pi.getCompileSdkVersion().toShort(),
        if (getAppUpdateInfo == null) {
          pi.getCompileSdkVersion().toShort()
        } else {
          getAppUpdateInfo.app.extra.compile.toShort()
        }
      ),
      minSdkDiff = SnapshotDiffItem.DiffNode(
        ai.minSdkVersion.toShort(),
        if (getAppUpdateInfo == null) ai.minSdkVersion.toShort() else getAppUpdateInfo.app.extra.min.toShort()
      ),
      packageSizeDiff = SnapshotDiffItem.DiffNode(
        pi.getPackageSize(includeSplits = false),
        if (getAppUpdateInfo == null) pi.getPackageSize(includeSplits = false) else getAppUpdateInfo.app.extra.packageSize.toLong()
      ),
      nativeLibsDiff = SnapshotDiffItem.DiffNode(""),
      servicesDiff = SnapshotDiffItem.DiffNode(""),
      activitiesDiff = SnapshotDiffItem.DiffNode(""),
      receiversDiff = SnapshotDiffItem.DiffNode(""),
      providersDiff = SnapshotDiffItem.DiffNode(""),
      permissionsDiff = SnapshotDiffItem.DiffNode(""),
      metadataDiff = SnapshotDiffItem.DiffNode("")
    )
  }
}
