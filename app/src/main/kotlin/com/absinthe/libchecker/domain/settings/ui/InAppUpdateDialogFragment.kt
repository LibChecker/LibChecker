package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.BuildInAppUpdateDiffDataUseCase
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class InAppUpdateDialogFragment : BaseBottomSheetViewDialogFragment<InAppUpdateDialogView>() {

  private val buildInAppUpdateDiffData: BuildInAppUpdateDiffDataUseCase by inject()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val viewModel: SettingsViewModel by viewModel()
  private var getAppUpdateInfo: GetAppUpdateInfo? = null
  private var selectedChannel: AppUpdateChannel = defaultUpdateChannel()

  override fun initRootView(): InAppUpdateDialogView = InAppUpdateDialogView(
    requireContext(),
    buildSnapshotItemDisplayData,
    selectedChannel.toButtonId()
  )

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    root.toggleGroup.addOnButtonCheckedListener { toggleGroup, checkedId, isChecked ->
      if (isChecked) {
        selectedChannel = checkedId.toAppUpdateChannel() ?: return@addOnButtonCheckedListener
        doOnMainThreadIdle { toggleGroup.isEnabled = false }
        root.updateButton.isEnabled = false
        root.setItem(null)
        root.showLoading()
        viewModel.requestUpdate(selectedChannel)
      }
    }
    root.updateButton.setOnClickListener {
      val url = getAppUpdateInfo?.appForFlavor(BuildConfig.IS_FOSS)?.link
      if (url == null) {
        root.updateButton.isEnabled = false
        root.showLoading()
        viewModel.requestUpdate(selectedChannel)
      } else {
        installUpdate(url)
      }
    }
    viewModel.respStateFlow.onEach { result ->
      if (result.channel != selectedChannel) {
        return@onEach
      }

      getAppUpdateInfo = result.updateInfo
      val diffData = buildInAppUpdateDiffData(result.updateInfo)

      root.apply {
        doOnMainThreadIdle { toggleGroup.isEnabled = true }
        updateButton.isEnabled = diffData?.hasUpdate == true
        setItem(diffData?.item)
        showContent()
      }
    }.launchIn(lifecycleScope)

    root.updateButton.isEnabled = false
    viewModel.requestUpdate(selectedChannel)
  }

  private fun Int.toAppUpdateChannel(): AppUpdateChannel? {
    return when (this) {
      R.id.in_app_update_chip_stable -> AppUpdateChannel.STABLE
      R.id.in_app_update_chip_ci -> AppUpdateChannel.CI
      else -> null
    }
  }

  private fun AppUpdateChannel.toButtonId(): Int {
    return when (this) {
      AppUpdateChannel.STABLE -> R.id.in_app_update_chip_stable
      AppUpdateChannel.CI -> R.id.in_app_update_chip_ci
    }
  }

  private fun defaultUpdateChannel(): AppUpdateChannel {
    return if (BuildConfig.IS_DEV_VERSION) {
      AppUpdateChannel.CI
    } else {
      AppUpdateChannel.STABLE
    }
  }

  private fun installUpdate(url: String) {
    lifecycleScope.launch {
      root.updateButton.isEnabled = false
      root.showLoading()
      val result = viewModel.installUpdate(url)
      root.showContent()
      root.updateButton.isEnabled = buildInAppUpdateDiffData(getAppUpdateInfo)?.hasUpdate == true
      showInstallResult(result)
    }
  }

  private fun showInstallResult(result: AppUpdateInstallResult) {
    when (result) {
      AppUpdateInstallResult.Started -> {
        Toasty.showLong(requireContext(), R.string.toast_app_update_started)
      }

      AppUpdateInstallResult.Unsupported -> {
        Toasty.showLong(requireContext(), R.string.toast_app_update_unsupported)
      }

      is AppUpdateInstallResult.Failure -> {
        val message = result.message.orEmpty()
        Toasty.showLong(requireContext(), getString(R.string.toast_app_update_failed, message))
      }
    }
  }
}
