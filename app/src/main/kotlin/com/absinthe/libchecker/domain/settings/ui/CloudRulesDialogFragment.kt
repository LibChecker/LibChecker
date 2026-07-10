package com.absinthe.libchecker.domain.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.domain.rules.CloudRulesVersionInfo
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogAction
import com.absinthe.libchecker.domain.settings.model.CloudRulesDialogState
import com.absinthe.libchecker.domain.settings.model.toCloudRulesDialogState
import com.absinthe.libchecker.domain.settings.presentation.SettingsViewModel
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CloudRulesDialogFragment : BaseBottomSheetViewDialogFragment<CloudRulesDialogView>() {

  private val viewModel: SettingsViewModel by viewModel(ownerProducer = { requireParentFragment() })
  private var versionInfo: CloudRulesVersionInfo? = null

  override fun initRootView(): CloudRulesDialogView = CloudRulesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
    root.bind(CloudRulesDialogState.Loading, ::handleAction)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
          viewModel.getCloudRulesVersionInfo()?.let {
            versionInfo = it
            try {
              root.bind(it.toCloudRulesDialogState(), ::handleAction)
            } catch (e: Exception) {
              Timber.e(e)
              context?.showToast(R.string.toast_cloud_rules_update_error)
            }
          }
        } catch (t: Throwable) {
          Timber.e(t)
        }
      }
    }
  }

  private fun handleAction(action: CloudRulesDialogAction) {
    when (action) {
      CloudRulesDialogAction.Update -> requestBundle()
    }
  }

  private fun requestBundle() {
    val remoteVersion = versionInfo?.remoteVersion ?: return showUpdateErrorToast()
    val downloadRequest = viewModel.getCloudRulesDownloadRequest()
    DownloadUtils.download(
      downloadRequest.url,
      downloadRequest.destination,
      object : DownloadUtils.OnDownloadListener {
        override fun onDownloadSuccess() {
          if (viewModel.installDownloadedCloudRules(downloadRequest, remoteVersion)) {
            lifecycleScope.launch {
              root.bind(
                CloudRulesDialogState.Content(
                  localVersion = remoteVersion,
                  remoteVersion = remoteVersion,
                  updateAvailable = false
                ),
                ::handleAction
              )
              runCatching {
                context?.let {
                  val intent = SystemServices.packageManager.getLaunchIntentForPackage(
                    it.packageName
                  )!!.apply {
                    putExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, true)
                  }
                  ProcessPhoenix.triggerRebirth(it, intent)
                }
              }
            }
          } else {
            showUpdateErrorToast()
          }
        }

        override fun onDownloading(progress: Int) {
        }

        override fun onDownloadFailed() {
          showUpdateErrorToast()
        }
      }
    )
  }

  private fun showUpdateErrorToast() {
    lifecycleScope.launch {
      context?.showToast(R.string.toast_cloud_rules_update_error)
    }
  }
}
