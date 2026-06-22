package com.absinthe.libchecker.features.applist.detail.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

/**
 * <pre>
 * author : Absinthe
 * time : 2020/10/25
 * </pre>
 */

class AppInfoBottomSheetDialogFragment : BaseBottomSheetViewDialogFragment<AppInfoBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private val aiAdapter = AppInfoAdapter()
  private var pendingExportApkFile: File? = null

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
    maxPeekHeightPercentage = 0.67f
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
            arguments = Bundle().apply {
              putString(EXTRA_PACKAGE_NAME, packageName)
            }
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
              val shareFile = viewModel.prepareAppPackageShareFile(ctx.cacheDir, pkg)
              val uri = FileProvider.getUriForFile(
                ctx,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                shareFile.file
              )
              Pair(shareFile, uri)
            }
          }
          withContext(Dispatchers.Main) {
            loading.dismiss()
          }

          shareResult.onSuccess { (shareFile, uri) ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = shareFile.mimeType
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
              viewModel.prepareAppPackageShareFile(ctx.cacheDir, pkg)
            }
          }
          withContext(Dispatchers.Main) {
            loading.dismiss()
          }

          fileResult.onSuccess { shareFile ->
            pendingExportApkFile = shareFile.file
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
              addCategory(Intent.CATEGORY_OPENABLE)
              type = shareFile.mimeType
              putExtra(Intent.EXTRA_TITLE, shareFile.file.name)
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
      layoutManager = GridLayoutManager(context, 4)
      setHasFixedSize(true)
    }

    aiAdapter.also { adapter ->
      adapter.setOnItemClickListener { _, _, position ->
        adapter.data[position].let {
          runCatching {
            startActivity(it.intent)
          }.onFailure {
            context?.let { ctx ->
              Toasty.showShort(ctx, R.string.toast_cant_open_app)
            }
          }
        }
        dismiss()
      }
    }
    packageName?.let { pkg ->
      lifecycleScope.launch {
        aiAdapter.setList(viewModel.getAppInfoActions(pkg))
      }
    }
  }
}
