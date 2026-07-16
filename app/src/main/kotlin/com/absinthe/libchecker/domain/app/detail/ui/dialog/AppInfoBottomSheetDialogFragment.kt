package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInfoActionItem
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetAction
import com.absinthe.libchecker.domain.app.detail.model.AppInfoBottomSheetState
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInfoBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
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

  private val exportApkLauncher: ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      shareController.onExportResult(result.resultCode, result.data?.data)
    }
  private val shareControllerDelegate: Lazy<AppInfoPackageShareController> = lazy {
    AppInfoPackageShareController(
      fragment = this,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      exportDocument = { intent -> exportApkLauncher.launch(intent) },
      dismiss = ::dismiss
    )
  }
  private val shareController: AppInfoPackageShareController by shareControllerDelegate
  private val primaryActionController: AppInfoPrimaryActionController by lazy {
    AppInfoPrimaryActionController(
      fragment = this,
      viewModel = viewModel,
      coroutineScope = lifecycleScope,
      dismiss = ::dismiss
    )
  }

  override fun initRootView(): AppInfoBottomSheetView {
    return AppInfoBottomSheetView(requireContext())
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun onDestroyView() {
    if (shareControllerDelegate.isInitialized()) {
      shareController.clear()
    }
    super.onDestroyView()
  }

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    root.bind(
      AppInfoBottomSheetState.Loading(packageName),
      ::handleAction
    )
    lifecycleScope.launch {
      val state = viewModel.getAppInfoBottomSheetState(packageName)
      root.bind(state, ::handleAction)
      primaryActionController.onContentLoaded(state)
    }
  }

  private fun handleAction(action: AppInfoBottomSheetAction) {
    when (action) {
      is AppInfoBottomSheetAction.Launch -> primaryActionController.launch(action)
      is AppInfoBottomSheetAction.OpenSettings -> primaryActionController.openSettings(action)
      is AppInfoBottomSheetAction.Share -> shareController.sharePackage(activity, action.packageName)
      is AppInfoBottomSheetAction.Export -> shareController.exportPackage(activity, action.packageName)
      is AppInfoBottomSheetAction.OpenExternal -> openAction(action.item)
    }
  }

  private fun openAction(item: AppInfoActionItem) {
    runCatching {
      startActivity(item.intent)
    }.onFailure {
      context?.let { ctx ->
        Toasty.showShort(ctx, R.string.toast_cant_open_app)
      }
    }
    dismiss()
  }
}
