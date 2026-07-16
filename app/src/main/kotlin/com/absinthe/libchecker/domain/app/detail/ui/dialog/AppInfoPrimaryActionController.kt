package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.app.detail.action.AppInfoLaunchAction
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetAction
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.utils.Telemetry
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppInfoPrimaryActionController(
  private val fragment: Fragment,
  private val viewModel: DetailViewModel,
  private val coroutineScope: CoroutineScope,
  private val dismiss: () -> Unit
) {

  fun launch(action: AppInfoBottomSheetAction.Launch) {
    coroutineScope.launch {
      launchApp(action)
    }
  }

  fun openSettings(action: AppInfoBottomSheetAction.OpenSettings) {
    coroutineScope.launch {
      openSettingsPage(action)
    }
  }

  fun onContentLoaded(state: AppInfoBottomSheetState.Content) {
    if (state.packageName == null) {
      return
    }
    Telemetry.recordEvent(
      Constants.Event.APP_INFO_BOTTOM_SHEET,
      mapOf(Telemetry.Param.CONTENT to "Unknown", "Action" to "Launch")
    )
  }

  private suspend fun launchApp(action: AppInfoBottomSheetAction.Launch) {
    val launchAction = action.preloadedAction
      ?: viewModel.getAppInfoPrimaryActions(action.packageName).launchAction
    when (launchAction) {
      AppInfoLaunchAction.Self -> {
        Toasty.showShort(fragment.requireContext(), "But why…")
        dismiss()
      }

      AppInfoLaunchAction.Alternative -> {
        showAlternativeLaunchDialog(action.packageName)
        dismiss()
      }

      is AppInfoLaunchAction.Available -> {
        runCatching {
          fragment.startActivity(launchAction.action.intent)
        }.onFailure {
          showAlternativeLaunchDialog(action.packageName)
        }.also {
          dismiss()
        }
      }
    }
  }

  private suspend fun openSettingsPage(action: AppInfoBottomSheetAction.OpenSettings) {
    try {
      val settingsIntent = action.preloadedIntent
        ?: viewModel.getAppInfoPrimaryActions(action.packageName).settingsIntent
      fragment.startActivity(settingsIntent)
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
