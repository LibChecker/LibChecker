package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Build
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import coil.load
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.URLManager
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.features.applist.detail.ui.view.AppDexoptItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallSourceItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInstallTimeItemView
import com.absinthe.libchecker.features.applist.detail.ui.view.CenterAlignImageSpan
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.FreezeUtils
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.ShizukuManager
import com.absinthe.libchecker.utils.ShizukuManager.Availability
import com.absinthe.libchecker.utils.extensions.PREINSTALLED_TIMESTAMP
import com.absinthe.libchecker.utils.extensions.getDexFileOptimizationInfo
import com.absinthe.libchecker.utils.extensions.getDrawable
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.text.SimpleDateFormat
import kotlinx.coroutines.runBlocking

@RequiresApi(Build.VERSION_CODES.R)
class AppInstallSourceBSDFragment : BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private var packageInfo: PackageInfo? = null
  private var binderReceivedHandle: ShizukuManager.ListenerHandle? = null
  private var permissionResultHandle: ShizukuManager.ListenerHandle? = null

  override fun initRootView(): AppInstallSourceBottomSheetView = AppInstallSourceBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    val packageName = packageName ?: return
    val pi = runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull() ?: return
    packageInfo = pi

    PackageUtils.getInstallSourceInfo(packageName)?.let { info ->
      initOriginatingItemView(root.originatingView, info.originatingPackageName)
      initAppInstallSourceItemView(root.installingView, info.installingPackageName)
    } ?: run {
      root.originatingView.isGone = true
      root.installingView.isGone = true
    }
    initAppInstalledTimeItemView(root.installedTimeView, pi)
    initDexoptItemView(root.dexoptView, pi)
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
    originatingPackageName: String?
  ) {
    if (context == null) {
      item.isGone = true
      return
    }

    when (val availability = ShizukuManager.getAvailability()) {
      Availability.Available -> initAppInstallSourceItemView(item, originatingPackageName)

      else -> initShizukuPromptItemView(
        item = item,
        availability = availability,
        usage = getString(R.string.lib_detail_app_install_source_shizuku_usage),
        permissionDetail = getString(R.string.lib_detail_app_install_source_shizuku_permission_not_granted_detail)
      )
    }
  }

  private fun initDexoptItemView(item: AppDexoptItemView, pi: PackageInfo) {
    if (context == null) {
      item.isGone = true
      return
    }

    val dexoptInfo = pi.getDexFileOptimizationInfo()
    if (dexoptInfo == null) {
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
          PackageUtils.startLaunchAppActivity(requireContext(), Constants.PackageNames.SHIZUKU)
          registerBinderReceivedRefresh()
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
    packageName: String?
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

    val targetLCItem = runBlocking { Repositories.lcRepository.getItem(packageName) } ?: run {
      item.isGone = true
      return
    }

    val pi = runCatching {
      PackageUtils.getPackageInfo(packageName)
    }.getOrNull()
    item.packageView.container.also {
      it.icon.load(pi)
      it.appName.text = targetLCItem.label
      it.packageName.text = packageName
      it.versionInfo.text =
        PackageUtils.getVersionString(targetLCItem.versionName, targetLCItem.versionCode)
    }

    val str = StringBuilder()
      .append(PackageUtils.getAbiString(requireContext(), targetLCItem.abi.toInt(), true))
      .append(PackageUtils.getBuildVersionsInfo(pi, packageName))
      .toString()
    val spanString: SpannableString
    val abiBadgeRes = PackageUtils.getAbiBadgeResource(targetLCItem.abi.toInt())

    if (targetLCItem.abi.toInt() != Constants.OVERLAY && targetLCItem.abi.toInt() != Constants.ERROR && abiBadgeRes != 0) {
      spanString = SpannableString("  $str")
      abiBadgeRes.getDrawable(requireContext())?.let { drawable ->
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val span = CenterAlignImageSpan(drawable)
        spanString.setSpan(span, 0, 1, ImageSpan.ALIGN_BOTTOM)
      }
      item.packageView.container.abiInfo.text = spanString
    } else {
      item.packageView.container.abiInfo.text = str
    }
    item.packageView.container.abiInfo.isVisible = true
    item.packageView.setItemContentDescription(
      item.titleView.text,
      targetLCItem.label,
      packageName,
      item.packageView.container.versionInfo.text,
      str
    )

    if (targetLCItem.variant == Constants.VARIANT_HAP) {
      item.packageView.container.setBadge(R.drawable.ic_harmony_badge)
    } else {
      item.packageView.container.setBadge(null)
    }

    item.packageView.setOnClickListener {
      activity?.finish()
      dismiss()
      activity?.launchDetailPage(targetLCItem)
    }
  }

  private fun initAppInstalledTimeItemView(item: AppInstallTimeItemView, pi: PackageInfo) {
    if (context == null || FreezeUtils.isAppFrozen(pi.packageName)) {
      item.isGone = true
      return
    }

    val firstInstalledTime = pi.firstInstallTime
    val lastUpdatedTime = pi.lastUpdateTime
    item.contentView.apply {
      if (firstInstalledTime <= PREINSTALLED_TIMESTAMP) {
        firstInstalledView.libSize.text = getString(R.string.snapshot_preinstalled_app)
      } else {
        firstInstalledView.libSize.text =
          SimpleDateFormat.getDateTimeInstance().format(firstInstalledTime)
      }
      if (lastUpdatedTime <= PREINSTALLED_TIMESTAMP) {
        lastUpdatedView.libSize.text = getString(R.string.snapshot_preinstalled_app)
      } else {
        lastUpdatedView.libSize.text =
          SimpleDateFormat.getDateTimeInstance().format(lastUpdatedTime)
      }
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
    PackageUtils.getInstallSourceInfo(packageName)?.let {
      initOriginatingItemView(root.originatingView, it.originatingPackageName)
      initAppInstallSourceItemView(root.installingView, it.installingPackageName)
    } ?: run {
      root.originatingView.isGone = true
      root.installingView.isGone = true
    }

    val pi = packageInfo ?: runCatching {
      PackageUtils.getPackageInfo(packageName)
    }.getOrNull()?.also {
      packageInfo = it
    } ?: return
    initDexoptItemView(root.dexoptView, pi)
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
