package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.domain.app.update.BuildInAppUpdateDiffDataUseCase
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.domain.snapshot.BuildSnapshotAbiDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.display.BuildSnapshotUpdateTimeDisplayDataUseCase
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.doOnMainThreadIdle
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class InAppUpdateDialogFragment : BaseBottomSheetViewDialogFragment<InAppUpdateDialogView>() {

  private val buildInAppUpdateDiffData: BuildInAppUpdateDiffDataUseCase by inject()
  private val buildSnapshotAbiDisplayData: BuildSnapshotAbiDisplayDataUseCase by inject()
  private val buildSnapshotUpdateTimeDisplayData: BuildSnapshotUpdateTimeDisplayDataUseCase by inject()
  private val viewModel: SettingsViewModel by viewModel()
  private var getAppUpdateInfo: GetAppUpdateInfo? = null

  override fun initRootView(): InAppUpdateDialogView = InAppUpdateDialogView(
    requireContext(),
    buildSnapshotAbiDisplayData,
    buildSnapshotUpdateTimeDisplayData
  )

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    root.toggleGroup.addOnButtonCheckedListener { toggleGroup, checkedId, isChecked ->
      if (isChecked) {
        doOnMainThreadIdle { toggleGroup.isEnabled = false }
        root.updateButton.isEnabled = false
        root.showLoading()
        viewModel.requestUpdate(checkedId == com.absinthe.libchecker.R.id.in_app_update_chip_stable)
      }
    }
    root.updateButton.setOnClickListener {
      getAppUpdateInfo?.app?.link?.let { url ->
        viewModel.downloadApk(url)
        Toasty.showLong(requireContext(), com.absinthe.libchecker.R.string.toast_downloading_app)
      }
    }
    viewModel.respStateFlow.onEach {
      getAppUpdateInfo = it
      val diffData = buildInAppUpdateDiffData(it)

      root.apply {
        doOnMainThreadIdle { toggleGroup.isEnabled = true }
        updateButton.isEnabled = diffData?.hasUpdate == true
        diffData?.let { data -> setItem(data.item) }
        showContent()
      }
    }.launchIn(lifecycleScope)

    root.updateButton.isEnabled = false
    viewModel.requestUpdate(true)
  }
}
