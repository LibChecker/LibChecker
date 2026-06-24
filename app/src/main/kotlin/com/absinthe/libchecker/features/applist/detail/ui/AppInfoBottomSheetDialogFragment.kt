package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.adapter.AppInfoAdapter
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

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

  private val exportApkLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      shareController.onExportResult(result.resultCode, result.data?.data)
    }
  private val shareController: AppInfoPackageShareController by lazy {
    AppInfoPackageShareController(
      fragment = this,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      exportDocument = { intent -> exportApkLauncher.launch(intent) },
      dismiss = ::dismiss
    )
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
      lifecycleScope.launch {
        if (packageName == BuildConfig.APPLICATION_ID) {
          Toasty.showShort(requireContext(), "But why…")
          dismiss()
          return@launch
        }

        val launchAction = viewModel.getAppLaunchAction(packageName)
        if (launchAction == null) {
          showAlternativeLaunchDialog()
          dismiss()
          return@launch
        }

        runCatching {
          startActivity(launchAction.intent)
        }.onFailure {
          showAlternativeLaunchDialog()
        }.also {
          dismiss()
        }
      }
    }
    packageName?.let { pkg ->
      lifecycleScope.launch {
        viewModel.getAppLaunchAction(pkg)?.launcherActivity?.let {
          root.launch.setLongClickCopiedToClipboard(it)
        }
        Telemetry.recordEvent(
          Constants.Event.APP_INFO_BOTTOM_SHEET,
          mapOf(Telemetry.Param.CONTENT to "Unknown", "Action" to "Launch")
        )
      }
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
        shareController.sharePackage(view, packageName)
      }
      setOnLongClickListener { view ->
        shareController.exportPackage(view, packageName)
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

  private fun showAlternativeLaunchDialog() {
    activity?.let {
      AlternativeLaunchBSDFragment().apply {
        arguments = Bundle().apply {
          putString(EXTRA_PACKAGE_NAME, packageName)
        }
        show(it.supportFragmentManager, AlternativeLaunchBSDFragment::class.java.name)
      }
    }
  }
}
