package com.absinthe.libchecker.features.applist.detail.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.compat.PackageManagerCompat
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isSplitsApk
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

const val MIMETYPE_APK = "application/vnd.android.package-archive"

class AppInfoBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppInfoBottomSheetView>() {

  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private val aiAdapter = AppInfoAdapter()
  private var pendingExportApkFile: File? = null
  private val illegalFilenameChars = Regex("""[\\/:*?"<>|]""")

  private val exportApkLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode != Activity.RESULT_OK) {
        pendingExportApkFile = null
        return@registerForActivityResult
      }
      val uri = result.data?.data ?: run {
        pendingExportApkFile = null
        return@registerForActivityResult
      }
      val file = pendingExportApkFile ?: run {
        pendingExportApkFile = null
        return@registerForActivityResult
      }
      val ctx = context ?: run {
        pendingExportApkFile = null
        return@registerForActivityResult
      }

      lifecycleScope.launch(Dispatchers.IO) {
        runCatching {
          ctx.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
              input.copyTo(output)
            }
          } ?: error("OutputStream is null")
        }.onSuccess {
          pendingExportApkFile = null
          Telemetry.recordEvent(
            Constants.Event.APP_INFO_BOTTOM_SHEET,
            mapOf(
              Telemetry.Param.CONTENT to "Unknown",
              "Action" to "Export APK"
            )
          )
          Timber.i("Exported APK for %s to %s", file.name, uri)
        }.onFailure { throwable ->
          pendingExportApkFile = null
          Timber.e(throwable, "Failed to export APK for %s", file.name)
          withContext(Dispatchers.Main) {
            if (isAdded) {
              Toasty.showShort(ctx, R.string.toast_cant_open_app)
            }
          }
        }
      }
    }

  override fun initRootView(): AppInfoBottomSheetView = AppInfoBottomSheetView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setPadding(24.dp, 16.dp, 24.dp, 0)
    }
    root.launch.setOnClickListener {
      try {
        if (packageName == BuildConfig.APPLICATION_ID) {
          Toasty.showShort(requireContext(), "But why…")
        } else {
          PackageUtils.startLaunchAppActivity(requireContext(), packageName)
        }
      } catch (_: Exception) {
        activity?.let {
          AlternativeLaunchBSDFragment().apply {
            arguments = bundleOf(
              EXTRA_PACKAGE_NAME to packageName
            )
            show(it.supportFragmentManager, AlternativeLaunchBSDFragment::class.java.name)
          }
        }
      } finally {
        dismiss()
      }
    }
    packageName?.let {
      root.launch.setLongClickCopiedToClipboard(PackageUtils.getLauncherActivity(it))
      Telemetry.recordEvent(
        Constants.Event.APP_INFO_BOTTOM_SHEET,
        mapOf(Telemetry.Param.CONTENT to "Unknown", "Action" to "Launch")
      )
    }
    root.setting.setOnClickListener {
      try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          .setData("package:$packageName".toUri())
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        Telemetry.recordEvent(
          Constants.Event.APP_INFO_BOTTOM_SHEET,
          mapOf(
            Telemetry.Param.CONTENT to "Unknown",
            "Action" to "Setting"
          )
        )
      } catch (_: Exception) {
        context?.showToast(R.string.toast_cant_open_app)
      } finally {
        dismiss()
      }
    }
    root.share.apply {
      setOnClickListener { view ->
        val pkg = packageName ?: run {
          Timber.e("package name is null")
          return@setOnClickListener
        }
        val ctx = view.context as Activity
        val loading = UiUtils.createLoadingDialog(ctx)
        loading.show()
        lifecycleScope.launch {
          val shareResult = runCatching {
            withContext(Dispatchers.IO) {
              val apkFile = prepareApkFile(ctx, pkg)
              val uri = FileProvider.getUriForFile(
                ctx,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                apkFile
              )
              Pair(apkFile, uri)
            }
          }
          withContext(Dispatchers.Main) {
            loading.dismiss()
          }

          shareResult.onSuccess { (file, uri) ->
            val mimeType = inferMimeType(file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = mimeType
              putExtra(Intent.EXTRA_STREAM, uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (!GlobalValues.longTapShareButtonTip) {
              Toasty.showLong(view.context, R.string.toast_long_tap_share_button_tip)
            }
            runCatching {
              startActivity(Intent.createChooser(shareIntent, null))
              Telemetry.recordEvent(
                Constants.Event.APP_INFO_BOTTOM_SHEET,
                mapOf(
                  Telemetry.Param.CONTENT to "Unknown",
                  "Action" to "Share"
                )
              )
              dismiss()
            }.onFailure { throwable ->
              Timber.e(throwable)
              Toasty.showShort(ctx, throwable.toString())
            }
          }.onFailure { throwable ->
            Timber.e(throwable)
            Toasty.showShort(ctx, throwable.toString())
          }
        }
      }
      setOnLongClickListener { view ->
        val pkg = packageName ?: run {
          Timber.e("package name is null")
          return@setOnLongClickListener true
        }

        lifecycleScope.launch {
          val ctx = view.context as Activity
          val loading = UiUtils.createLoadingDialog(ctx)
          loading.show()
          val fileResult = runCatching {
            withContext(Dispatchers.IO) {
              prepareApkFile(ctx, pkg)
            }
          }
          withContext(Dispatchers.Main) {
            loading.dismiss()
          }

          fileResult.onSuccess { file ->
            pendingExportApkFile = file
            val mimeType = inferMimeType(file)
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
              addCategory(Intent.CATEGORY_OPENABLE)
              type = mimeType
              putExtra(Intent.EXTRA_TITLE, file.name)
            }

            runCatching {
              exportApkLauncher.launch(intent)
            }.onFailure { throwable ->
              pendingExportApkFile = null
              Timber.e(throwable)
              Toasty.showShort(ctx, throwable.toString())
            }.onSuccess {
              GlobalValues.longTapShareButtonTip = true
            }
          }.onFailure { throwable ->
            Timber.e(throwable)
            Toasty.showShort(ctx, throwable.toString())
          }
        }

        true
      }
    }
    root.list.apply {
      adapter = aiAdapter
      layoutManager = StaggeredGridLayoutManager(4, StaggeredGridLayoutManager.VERTICAL)
      setHasFixedSize(true)
    }

    aiAdapter.also { adapter ->
      val list = (getShowAppInfoList() + getShowAppSourceList() + getShowMarketList())
        .distinctBy { it.pii.packageName }
      adapter.setList(list)
      adapter.setOnItemClickListener { _, _, position ->
        adapter.data[position].let {
          runCatching {
            startActivity(it.intent)
            // Telemetry.recordEvent(
            //   Constants.Event.APP_INFO_BOTTOM_SHEET,
            //   mapOf(Telemetry.Param.CONTENT to packageName.toString(), "Action" to it.pii.packageName)
            // )
          }.onFailure {
            context?.let { ctx ->
              Toasty.showShort(ctx, R.string.toast_cant_open_app)
            }
          }
        }
        dismiss()
      }
    }
  }

  private fun getShowAppInfoList(): List<AppInfoAdapter.AppInfoItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_SHOW_APP_INFO),
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
      .map {
        AppInfoAdapter.AppInfoItem(
          it.activityInfo,
          Intent(Intent.ACTION_SHOW_APP_INFO)
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowAppSourceList(): List<AppInfoAdapter.AppInfoItem> {
    val pkg = packageName ?: return emptyList()
    val source = PackageUtils.getPackageInfo(pkg).applicationInfo?.sourceDir ?: return emptyList()
    val sourcePath = runCatching { File(source) }.getOrNull() ?: return emptyList()

    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
      },
      PackageManager.MATCH_DEFAULT_ONLY
    )
      .filter { isFileManager(it.activityInfo.packageName) }
      .map {
        AppInfoAdapter.AppInfoItem(
          it.activityInfo,
          Intent(Intent.ACTION_VIEW)
            .setPackage(it.activityInfo.packageName)
            .setDataAndType(sourcePath.toUri(), MIMETYPE_APK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun getShowMarketList(): List<AppInfoAdapter.AppInfoItem> {
    return PackageManagerCompat.queryIntentActivities(
      Intent(Intent.ACTION_VIEW).also {
        it.data = "market://details?id=$packageName".toUri()
      },
      PackageManager.MATCH_DEFAULT_ONLY
    ).filter { it.activityInfo.packageName != BuildConfig.APPLICATION_ID }
      .map {
        AppInfoAdapter.AppInfoItem(
          it.activityInfo,
          Intent(Intent.ACTION_VIEW)
            .setData("market://details?id=$packageName".toUri())
            .setComponent(ComponentName(it.activityInfo.packageName, it.activityInfo.name))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
      }
  }

  private fun prepareApkFile(context: Context, pkg: String): File {
    val packageInfo = PackageUtils.getPackageInfo(pkg)
    val applicationInfo = packageInfo.applicationInfo
      ?: error("No ApplicationInfo")
    val sourceDir = applicationInfo.sourceDir
      ?: error("No sourceDir")
    val sourceFile = File(sourceDir)
    val splitFiles = PackageUtils.getSplitsSourceDir(packageInfo)
      ?.map { path -> File(path) }
      ?.filter { it.exists() }
      ?: emptyList()
    val hasSplits = packageInfo.isSplitsApk() && splitFiles.isNotEmpty()
    val targetDir = File(context.cacheDir, "shared_apk")
    if (!targetDir.exists()) {
      targetDir.mkdirs()
    }
    val targetFile = File(targetDir, buildSuggestedPackageFileName(context.packageManager, packageInfo, hasSplits))
    val latestSourceTimestamp = (listOf(sourceFile) + splitFiles).maxOf { it.lastModified() }
    val needRebuild = !targetFile.exists() ||
      targetFile.lastModified() < latestSourceTimestamp ||
      (!hasSplits && targetFile.length() != sourceFile.length()) ||
      (hasSplits && targetFile.length() == 0L)

    if (needRebuild) {
      if (targetFile.exists()) {
        targetFile.delete()
      }
      if (hasSplits) {
        buildApksArchive(targetFile, sourceFile, splitFiles)
      } else {
        sourceFile.inputStream().use { input ->
          targetFile.outputStream().use { output ->
            input.copyTo(output)
          }
        }
      }
      targetFile.setLastModified(latestSourceTimestamp)
    }

    return targetFile
  }

  private fun buildSuggestedPackageFileName(
    packageManager: PackageManager,
    packageInfo: PackageInfo,
    hasSplits: Boolean
  ): String {
    val appName = packageInfo.getAppName(packageManager).takeUnless { it.isNullOrBlank() }
      ?: packageInfo.packageName
    val versionName = packageInfo.versionName.orEmpty()
    val versionCode = packageInfo.getVersionCode()
    val raw = "${appName}_${versionName}_$versionCode"
    val sanitized = raw.replace(illegalFilenameChars, "_")
    val extension = if (hasSplits) "apks" else "apk"
    return "$sanitized.$extension"
  }

  private fun buildApksArchive(targetFile: File, baseApk: File, splits: List<File>) {
    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
    ZipOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { zos ->
      fun putFile(file: File) {
        val entry = ZipEntry(file.name)
        entry.time = file.lastModified()
        zos.putNextEntry(entry)
        file.inputStream().use { input ->
          input.copyTo(zos)
        }
        zos.closeEntry()
      }
      putFile(baseApk)
      splits.forEach { split ->
        putFile(split)
      }
    }

    if (!tempFile.renameTo(targetFile)) {
      targetFile.delete()
      tempFile.copyTo(targetFile, overwrite = true)
      tempFile.delete()
    }
  }

  private fun inferMimeType(file: File): String {
    return if (file.extension.equals("apks", ignoreCase = true)) {
      "application/octet-stream"
    } else {
      MIMETYPE_APK
    }
  }

  private fun isFileManager(packageName: String): Boolean {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType("file:///".toUri(), "*/*")
      setPackage(packageName)
    }
    val canHandleFiles = PackageManagerCompat.queryIntentActivities(
      viewIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val permissions = PackageUtils.getPermissionsList(packageName)
    val hasStoragePermission = permissions.any {
      it == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
        it == Manifest.permission.READ_EXTERNAL_STORAGE ||
        it == Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      setPackage(packageName)
    }
    val canPickFiles = PackageManagerCompat.queryIntentActivities(
      getContentIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    val openTreeIntent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      setPackage(packageName)
    }
    val canManageDirectories = PackageManagerCompat.queryIntentActivities(
      openTreeIntent,
      PackageManager.MATCH_DEFAULT_ONLY
    ).any { it.activityInfo.packageName == packageName }

    return (canHandleFiles || canPickFiles || canManageDirectories) && hasStoragePermission
  }
}
