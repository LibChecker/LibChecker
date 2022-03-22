package com.absinthe.libchecker.ui.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.settings.CloudRulesDialogView
import com.absinthe.rulesbundle.RuleDatabase
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

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
    lifecycleScope.launchWhenResumed {
      try {
        request.requestCloudRuleInfo()?.let {
          try {
            root.cloudRulesContentView.localVersion.version.text =
              GlobalValues.localRulesVersion.toString()
            root.cloudRulesContentView.remoteVersion.version.text =
              it.version.toString()
            if (GlobalValues.localRulesVersion < it.version) {
              root.cloudRulesContentView.setUpdateButtonStatus(true)
            }
            root.viewFlipper.displayedChild = 1
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

  private fun requestBundle() {
    val saveFile = File(requireContext().cacheDir, Constants.RULES_DB_FILE_NAME)
    DownloadUtils.download(
      ApiManager.rulesBundleUrl,
      saveFile,
      object : DownloadUtils.OnDownloadListener {
        override fun onDownloadSuccess() {
          RuleDatabase.getDatabase(requireContext()).close()
          Repositories.deleteRulesDatabase()

          lifecycleScope.launch(Dispatchers.Main) {
            root.cloudRulesContentView.localVersion.version.text =
              root.cloudRulesContentView.remoteVersion.version.text
            root.cloudRulesContentView.setUpdateButtonStatus(false)
            try {
              GlobalValues.localRulesVersion =
                root.cloudRulesContentView.remoteVersion.version.text.toString()
                  .toInt()
              context?.let {
                ProcessPhoenix.triggerRebirth(it)
              }
            } catch (e: NumberFormatException) {
            }
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
