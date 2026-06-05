package com.absinthe.libchecker.features.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.RulesRepository
import com.absinthe.libchecker.ui.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CloudRulesDialogFragment : BaseBottomSheetViewDialogFragment<CloudRulesDialogView>() {

  private val request: CloudRuleBundleRequest = ApiManager.create()

  override fun initRootView(): CloudRulesDialogView = CloudRulesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
    root.cloudRulesContentView.updateButton.setOnClickListener {
      requestBundle()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        try {
          request.requestCloudRuleInfo()?.let {
            try {
              root.cloudRulesContentView.localVersion.version.text =
                RulesRepository.getLocalVersion(requireContext()).toString()
              root.cloudRulesContentView.remoteVersion.version.text = it.version.toString()
              if (RulesRepository.getLocalVersion(requireContext()) < it.version) {
                root.cloudRulesContentView.setUpdateButtonStatus(true)
              }
              withContext(Dispatchers.Main) {
                root.showContent()
              }
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

  private fun requestBundle() {
    val saveFile = RulesRepository.getDownloadFile(requireContext())
    DownloadUtils.download(
      ApiManager.rulesBundleUrl,
      saveFile,
      object : DownloadUtils.OnDownloadListener {
        override fun onDownloadSuccess() {
          if (RulesRepository.replaceDatabase(saveFile, requireContext())) {
            lifecycleScope.launch(Dispatchers.Main) {
              root.cloudRulesContentView.localVersion.version.text =
                root.cloudRulesContentView.remoteVersion.version.text
              root.cloudRulesContentView.setUpdateButtonStatus(false)
              runCatching {
                RulesRepository.setLocalVersion(
                  requireContext(),
                  root.cloudRulesContentView.remoteVersion.version.text.toString().toInt()
                )
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
            context?.showToast(R.string.toast_cloud_rules_update_error)
          }
        }

        override fun onDownloading(progress: Int) {
        }

        override fun onDownloadFailed() {
          context?.showToast(R.string.toast_cloud_rules_update_error)
        }
      }
    )
  }
}
