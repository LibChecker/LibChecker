package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.action.AppInfoLaunchAction
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.features.applist.detail.DetailViewModel
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
        val actions = viewModel.getAppInfoPrimaryActions(pkg)
        val launchAction = actions.launchAction
        if (launchAction is AppInfoLaunchAction.Available) {
          root.launch.setLongClickCopiedToClipboard(launchAction.action.launcherActivity)
        }
        Telemetry.recordEvent(
          Constants.Event.APP_INFO_BOTTOM_SHEET,
          mapOf(Telemetry.Param.CONTENT to "Unknown", "Action" to "Launch")
        )
      }
    }
    root.setting.setOnClickListener {
      coroutineScope.launch {
        openSettings(packageName)
      }
    }
  }

  private suspend fun launchApp(packageName: String?) {
    when (val launchAction = viewModel.getAppInfoPrimaryActions(packageName).launchAction) {
      AppInfoLaunchAction.Self -> {
        Toasty.showShort(fragment.requireContext(), "But why…")
        dismiss()
      }

      AppInfoLaunchAction.Alternative -> {
        showAlternativeLaunchDialog(packageName)
        dismiss()
      }

      is AppInfoLaunchAction.Available -> {
        runCatching {
          fragment.startActivity(launchAction.action.intent)
        }.onFailure {
          showAlternativeLaunchDialog(packageName)
        }.also {
          dismiss()
        }
      }
    }
  }

  private suspend fun openSettings(packageName: String?) {
    try {
      val actions = viewModel.getAppInfoPrimaryActions(packageName)
      fragment.startActivity(actions.settingsIntent)
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
