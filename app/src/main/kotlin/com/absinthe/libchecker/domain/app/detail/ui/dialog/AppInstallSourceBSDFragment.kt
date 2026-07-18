package com.absinthe.libchecker.domain.app.detail.ui.dialog

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceRequesterAccess
import com.absinthe.libchecker.domain.app.detail.navigation.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel
import com.absinthe.libchecker.domain.app.detail.presentation.DetailViewModel.AppInstallSourceDetailsResult
import com.absinthe.libchecker.domain.app.detail.ui.view.AppInstallSourceBottomSheetView
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.ShizukuManager
import com.absinthe.libchecker.utils.ShizukuManager.Availability
import com.absinthe.libchecker.utils.extensions.launchDetailPage
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

@RequiresApi(Build.VERSION_CODES.R)
class AppInstallSourceBSDFragment : BaseBottomSheetViewDialogFragment<AppInstallSourceBottomSheetView>() {

  private val viewModel: DetailViewModel by activityViewModel()
  private val packageName by lazy { arguments?.getString(EXTRA_PACKAGE_NAME) }
  private var binderReceivedHandle: ShizukuManager.ListenerHandle? = null
  private var permissionResultHandle: ShizukuManager.ListenerHandle? = null

  override fun initRootView(): AppInstallSourceBottomSheetView {
    return AppInstallSourceBottomSheetView(requireContext())
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    maxPeekHeightPercentage = 0.67f
    if (packageName == null) {
      return
    }
    collectAppInstallSourceDetailsResults()
    registerBinderReceivedRefresh()
  }

  override fun onResume() {
    super.onResume()
    loadAppInstallSourceDisplay()
  }

  private fun collectAppInstallSourceDetailsResults() {
    lifecycleScope.launch {
      viewModel.appInstallSourceDetailsResults.collect(::handleAppInstallSourceDetailsResult)
    }
  }

  private fun handleAppInstallSourceDetailsResult(loadResult: AppInstallSourceDetailsResult) {
    if (loadResult.packageName != packageName) {
      return
    }
    loadResult.display?.let {
      root.bind(it, ::handleAction)
    }
  }

  private fun handleAction(action: AppInstallSourceAction) {
    when (action) {
      is AppInstallSourceAction.OpenApp -> {
        val hostActivity = activity ?: return
        dismiss()
        hostActivity.launchDetailPage(action.item)
      }

      AppInstallSourceAction.RequestShizukuPermission -> {
        registerPermissionResultRefresh()
        ShizukuManager.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
      }
    }
  }

  override fun onDestroyView() {
    binderReceivedHandle?.close()
    permissionResultHandle?.close()
    binderReceivedHandle = null
    permissionResultHandle = null
    super.onDestroyView()
  }

  private fun registerBinderReceivedRefresh() {
    binderReceivedHandle?.close()
    binderReceivedHandle = ShizukuManager.registerBinderReceivedListener {
      refreshShizukuDependentViews()
    }
  }

  private fun registerPermissionResultRefresh() {
    permissionResultHandle?.close()
    permissionResultHandle = ShizukuManager.registerRequestPermissionResultListener { _, _ ->
      permissionResultHandle?.close()
      permissionResultHandle = null
      refreshShizukuDependentViews()
    }
  }

  private fun refreshShizukuDependentViews() {
    if (context == null) {
      return
    }
    loadAppInstallSourceDisplay()
  }

  private fun loadAppInstallSourceDisplay() {
    val packageName = packageName ?: return
    viewModel.loadAppInstallSourceDetails(
      packageName = packageName,
      requesterAccess = ShizukuManager.getAvailability().toRequesterAccess()
    )
  }

  companion object {
    private const val SHIZUKU_PERMISSION_REQUEST_CODE = 0
  }
}

private fun Availability.toRequesterAccess(): AppInstallSourceRequesterAccess {
  return when (this) {
    Availability.Available -> AppInstallSourceRequesterAccess.Available
    Availability.NotRunning -> AppInstallSourceRequesterAccess.ShizukuNotRunning
    Availability.LowVersion -> AppInstallSourceRequesterAccess.ShizukuLowVersion
    Availability.PermissionDenied -> AppInstallSourceRequesterAccess.ShizukuPermissionDenied
  }
}
