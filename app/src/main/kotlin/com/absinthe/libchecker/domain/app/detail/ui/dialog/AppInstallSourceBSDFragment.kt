package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.Intent
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.domain.app.detail.RelatedAppDisplayData
import com.absinthe.libchecker.domain.app.detail.action.AppInstallSourceDetails
import com.absinthe.libchecker.domain.app.detail.action.AppInstalledTimeDisplayData
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppInstallSourceDetailsResult
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppLaunchActionResult
import com.absinthe.libchecker.domain.app.detail.ui.binder.RelatedAppItemBinder
import com.absinthe.libchecker.domain.app.detail.ui.view.AppDexoptItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInstallSourceItemView
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInstallTimeItemView
import com.absinthe.libchecker.domain.app.model.AppInstallSource
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.ShizukuManager
import com.absinthe.libchecker.utils.ShizukuManager.Availability
import com.absinthe.libchecker.utils.extensions.DexFileOptimizationInfo
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@RequiresApi(Build.VERSION_CODES.R)
class AppInstallSourceBSDFragment : BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private val relatedAppItemBinder = RelatedAppItemBinder()
  private var binderReceivedHandle: ShizukuManager.ListenerHandle? = null
  private var permissionResultHandle: ShizukuManager.ListenerHandle? = null

  override fun initRootView(): AppInstallSourceBottomSheetView = AppInstallSourceBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    val packageName = packageName ?: return
    collectAppInstallSourceDetailsResults()
    collectAppLaunchActionResults()
    viewModel.loadAppInstallSourceDetails(packageName)
  }

  private fun collectAppInstallSourceDetailsResults() {
    lifecycleScope.launch {
      viewModel.appInstallSourceDetailsResults.collect(::handleAppInstallSourceDetailsResult)
    }
  }

  private fun handleAppInstallSourceDetailsResult(loadResult: AppInstallSourceDetailsResult) {
    if (loadResult.packageName != packageName) {
      return
    }
    val details = loadResult.details ?: return
    bindAppInstallSourceDetails(loadResult, details)
  }

  private fun bindAppInstallSourceDetails(
    loadResult: AppInstallSourceDetailsResult,
    details: AppInstallSourceDetails
  ) {
    bindAppInstallSourceItems(
      installSource = details.installSource,
      originatingApp = loadResult.originatingApp,
      installingApp = loadResult.installingApp
    )
    initAppInstalledTimeItemView(
      item = root.installedTimeView,
      installedTime = details.installedTime
    )
    initDexoptItemView(root.dexoptView, details.dexoptInfo)
  }

  private fun bindAppInstallSourceItems(
    installSource: AppInstallSource?,
    originatingApp: RelatedAppDisplayData?,
    installingApp: RelatedAppDisplayData?
  ) {
    installSource?.let {
      initOriginatingItemView(root.originatingView, it.originatingPackageName, originatingApp)
      initAppInstallSourceItemView(root.installingView, it.installingPackageName, installingApp)
    } ?: run {
      root.originatingView.isGone = true
      root.installingView.isGone = true
    }
  }

  override fun onDestroyView() {
    binderReceivedHandle?.close()
    permissionResultHandle?.close()
    binderReceivedHandle = null
    permissionResultHandle = null
    super.onDestroyView()
  }

  private fun initOriginatingItemView(
    item: AppInstallSourceItemView,
    originatingPackageName: String?,
    data: RelatedAppDisplayData?
  ) {
    if (context == null) {
      item.isGone = true
      return
    }

    when (val availability = ShizukuManager.getAvailability()) {
      Availability.Available -> initAppInstallSourceItemView(item, originatingPackageName, data)

      else -> initShizukuPromptItemView(
        item = item,
        availability = availability,
        usage = getString(R.string.lib_detail_app_install_source_shizuku_usage),
        permissionDetail = getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted_detail)
      )
    }
  }

  private fun initDexoptItemView(
    item: AppDexoptItemView,
    dexoptInfo: DexFileOptimizationInfo?
  ) {
    if (context == null || dexoptInfo == null) {
      item.isGone = true
      return
    }

    item.isGone = false
    item.contentView.setInfo(dexoptInfo)
    (item.contentView.parent as? View)?.setLongClickCopiedToClipboard(item.contentView.getAllContentText())
  }

  private fun initShizukuPromptItemView(
    item: AppInstallSourceItemView,
    availability: Availability,
    usage: String,
    permissionDetail: String
  ) {
    item.isGone = false
    item.packageView.setOnClickListener(null)
    item.packageView.container.apply {
      abiInfo.isVisible = false
      setBadge(null)
      icon.load(com.absinthe.lc.rulesbundle.R.drawable.ic_lib_shizuku)
      packageName.text = usage
    }

    when (availability) {
      Availability.NotInstalled -> {
        item.packageView.container.appName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_uninstalled)
        item.packageView.container.versionInfo.text =
          getString(R.string.lib_detail_app_install_source_shizuku_uninstalled_detail)
        item.packageView.setOnClickListener {
          openShizukuReleasePage()
        }
      }

      Availability.NotRunning -> {
        item.packageView.container.appName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_not_running)
        item.packageView.container.versionInfo.text =
          getString(R.string.lib_detail_app_install_source_shizuku_not_running_detail)
        item.packageView.setOnClickListener {
          registerBinderReceivedRefresh()
          viewModel.loadAppLaunchAction(Constants.PackageNames.SHIZUKU)
        }
      }

      Availability.LowVersion -> {
        item.packageView.container.appName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_low_version)
        item.packageView.container.versionInfo.text =
          getString(R.string.lib_detail_app_install_source_shizuku_low_version_detail)
        item.packageView.setOnClickListener {
          openShizukuReleasePage()
        }
      }

      Availability.PermissionDenied -> {
        item.packageView.container.appName.text =
          getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted)
        item.packageView.container.versionInfo.text = permissionDetail
        item.packageView.setOnClickListener {
          registerPermissionResultRefresh()
          ShizukuManager.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
      }

      Availability.Available -> {
        item.isGone = true
      }
    }

    if (!item.isGone) {
      item.packageView.setItemContentDescription(
        item.titleView.text,
        item.packageView.container.appName.text,
        item.packageView.container.packageName.text,
        item.packageView.container.versionInfo.text
      )
    }
  }

  private fun initAppInstallSourceItemView(
    item: AppInstallSourceItemView,
    packageName: String?,
    data: RelatedAppDisplayData?
  ) {
    if (context == null) {
      item.isGone = true
      return
    }

    item.isGone = false
    if (packageName == null) {
      item.packageView.container.also {
        it.icon.load(R.drawable.ic_icon_blueprint)
        it.appName.text =
          getString(R.string.lib_detail_app_install_source_empty)
        it.packageName.text =
          getString(R.string.lib_detail_app_install_source_empty_detail)
        it.setVersionInfo("                                                                            ")
        it.setAbiInfo("                                            ")
        it.abiInfo.isVisible = true
        it.setBadge(null)
      }
      item.packageView.setItemContentDescription(
        item.titleView.text,
        item.packageView.container.appName.text,
        item.packageView.container.packageName.text
      )
      item.packageView.setOnClickListener(null)
      return
    }

    if (data == null) {
      item.isGone = true
      return
    }

    bindAppInstallSourceItemView(
      item = item,
      data = data
    )
  }

  private fun bindAppInstallSourceItemView(
    item: AppInstallSourceItemView,
    data: RelatedAppDisplayData
  ) {
    relatedAppItemBinder.bind(
      appItemView = item.packageView,
      title = item.titleView.text,
      data = data
    ) {
      activity?.finish()
      dismiss()
      activity?.launchDetailPage(data.item)
    }
  }

  private fun initAppInstalledTimeItemView(
    item: AppInstallTimeItemView,
    installedTime: AppInstalledTimeDisplayData?
  ) {
    if (context == null || installedTime == null) {
      item.isGone = true
      return
    }

    item.contentView.apply {
      firstInstalledView.libSize.text = installedTime.firstInstalledTime
      lastUpdatedView.libSize.text = installedTime.lastUpdatedTime
      (parent as? View)?.setLongClickCopiedToClipboard(item.contentView.getAllContentText())
    }
  }

  private fun registerBinderReceivedRefresh() {
    binderReceivedHandle?.close()
    binderReceivedHandle = ShizukuManager.registerBinderReceivedListener {
      binderReceivedHandle?.close()
      binderReceivedHandle = null
      refreshShizukuDependentViews()
    }
  }

  private fun registerPermissionResultRefresh() {
    permissionResultHandle?.close()
    permissionResultHandle = ShizukuManager.registerRequestPermissionResultListener { _, _ ->
      permissionResultHandle?.close()
      permissionResultHandle = null
      refreshShizukuDependentViews()
    }
  }

  private fun refreshShizukuDependentViews() {
    if (context == null) {
      return
    }

    val packageName = packageName ?: return
    viewModel.loadAppInstallSourceDetails(packageName)
  }

  private fun collectAppLaunchActionResults() {
    lifecycleScope.launch {
      viewModel.appLaunchActionResults.collect(::handleAppLaunchActionResult)
    }
  }

  private fun handleAppLaunchActionResult(loadResult: AppLaunchActionResult) {
    if (loadResult.packageName == Constants.PackageNames.SHIZUKU) {
      loadResult.action?.let {
        startActivity(it.intent)
      }
    }
  }

  private fun openShizukuReleasePage() {
    startActivity(
      Intent(Intent.ACTION_VIEW).apply {
        data = URLManager.SHIZUKU_APP_GITHUB_RELEASE_PAGE.toUri()
      }
    )
  }

  companion object {
    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 0
  }
}
