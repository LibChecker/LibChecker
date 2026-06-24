package com.absinthe.libchecker.features.applist.detail.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
import com.absinthe.libchecker.features.applist.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.setLongClickCopiedToClipboard
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppInfoPrimaryActionController(
  private val fragment: Fragment,
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val dismiss: () -> Unit
) {
  fun bind(root: AppInfoBottomSheetView, packageName: String?) {
    root.launch.setOnClickListener {
      coroutineScope.launch {
        launchApp(packageName)
      }
    }
    packageName?.let { pkg ->
      coroutineScope.launch {
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
      openSettings(packageName)
    }
  }

  private suspend fun launchApp(packageName: String?) {
    if (packageName == BuildConfig.APPLICATION_ID) {
      Toasty.showShort(fragment.requireContext(), "But why…")
      dismiss()
      return
    }

    val launchAction = viewModel.getAppLaunchAction(packageName)
    if (launchAction == null) {
      showAlternativeLaunchDialog(packageName)
      dismiss()
      return
    }

    runCatching {
      fragment.startActivity(launchAction.intent)
    }.onFailure {
      showAlternativeLaunchDialog(packageName)
    }.also {
      dismiss()
    }
  }

  private fun openSettings(packageName: String?) {
    try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData("package:$packageName".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      fragment.startActivity(intent)
      Telemetry.recordEvent(
        Constants.Event.APP_INFO_BOTTOM_SHEET,
        mapOf(
          Telemetry.Param.CONTENT to "Unknown",
          "Action" to "Setting"
        )
      )
    } catch (_: Exception) {
      fragment.context?.showToast(R.string.toast_cant_open_app)
    } finally {
      dismiss()
    }
  }

  private fun showAlternativeLaunchDialog(packageName: String?) {
    fragment.activity?.let {
      AlternativeLaunchBSDFragment().apply {
        arguments = Bundle().apply {
          putString(EXTRA_PACKAGE_NAME, packageName)
        }
        show(it.supportFragmentManager, AlternativeLaunchBSDFragment::class.java.name)
      }
    }
  }
}
