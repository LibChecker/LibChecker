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
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogAction
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogContent
import com.absinthe.libchecker.domain.settings.model.InAppUpdateDialogState
import com.absinthe.libchecker.domain.settings.model.selectChannel
import com.absinthe.libchecker.domain.settings.model.showContent
import com.absinthe.libchecker.domain.settings.model.showInstallProgress
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
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
  private var dialogState = InAppUpdateDialogState(
    selectedChannel = defaultUpdateChannel(),
    content = InAppUpdateDialogContent.Loading(),
    isChannelSelectionEnabled = true,
    isUpdateEnabled = false
  )

  override fun initRootView(): InAppUpdateDialogView = InAppUpdateDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
    root.bind(dialogState, ::handleAction)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.respStateFlow.onEach { result ->
      if (result.channel != dialogState.selectedChannel) {
        return@onEach
      }

      getAppUpdateInfo = result.updateInfo
      val diffData = buildInAppUpdateDiffData(result.updateInfo)

      render(
        dialogState.showContent(
          item = diffData?.item?.let(::buildUpdateDisplayData),
          hasUpdate = diffData?.hasUpdate == true
        )
      )
    }.launchIn(lifecycleScope)

    viewModel.requestUpdate(dialogState.selectedChannel)
  }

  private fun handleAction(action: InAppUpdateDialogAction) {
    when (action) {
      is InAppUpdateDialogAction.SelectChannel -> {
        render(dialogState.selectChannel(action.channel))
        viewModel.requestUpdate(action.channel)
      }

      InAppUpdateDialogAction.Update -> {
        val url = getAppUpdateInfo?.appForFlavor(BuildConfig.IS_FOSS)?.link
        if (url == null) {
          render(dialogState.showInstallProgress())
          viewModel.requestUpdate(dialogState.selectedChannel)
        } else {
          installUpdate(url)
        }
      }
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
      render(dialogState.showInstallProgress())
      val result = viewModel.installUpdate(url)
      val diffData = buildInAppUpdateDiffData(getAppUpdateInfo)
      render(
        dialogState.showContent(
          item = diffData?.item?.let(::buildUpdateDisplayData),
          hasUpdate = diffData?.hasUpdate == true
        )
      )
      showInstallResult(result)
    }
  }

  private fun render(state: InAppUpdateDialogState) {
    dialogState = state
    root.bind(state, ::handleAction)
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

  private fun buildUpdateDisplayData(item: SnapshotDiffItem): SnapshotItemDisplayData {
    return buildSnapshotItemDisplayData(
      BuildSnapshotItemDisplayDataUseCase.Request(
        item = item,
        cardPresentation = SnapshotItemCardPresentation.Rounded,
        iconSource = null,
        showUpdateTime = false,
        isApexPackage = false,
        animateStateIndicator = false,
        tintChangedAbiBadge = false,
        highlightDiffColor = null,
        highlightText = ""
      )
    )
  }
}
