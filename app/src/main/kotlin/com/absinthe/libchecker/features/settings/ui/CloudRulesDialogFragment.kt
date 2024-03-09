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
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.absinthe.rulesbundle.Repositories as RulesBundleRepo
import com.absinthe.rulesbundle.RuleDatabase
import com.jakewharton.processphoenix.ProcessPhoenix
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.core.os.FileUtils
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
                RulesBundleRepo.getLocalRulesVersion(requireContext()).toString()
              root.cloudRulesContentView.remoteVersion.version.text = it.version.toString()
              if (RulesBundleRepo.getLocalRulesVersion(requireContext()) < it.version) {
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
    val saveFile = File(requireContext().cacheDir, Constants.RULES_DB_FILE_NAME)
    DownloadUtils.download(
      ApiManager.rulesBundleUrl,
      saveFile,
      object : DownloadUtils.OnDownloadListener {
        override fun onDownloadSuccess() {
          RuleDatabase.getDatabase(requireContext()).close()
          Repositories.deleteRulesDatabase()

          val databaseDir = requireContext().getDatabasePath(Constants.RULES_DATABASE_NAME).parent
          FileUtils.copy(saveFile, File(databaseDir, Constants.RULES_DATABASE_NAME))

          if (File(databaseDir, Constants.RULES_DATABASE_NAME).md5() == saveFile.md5()) {
            lifecycleScope.launch(Dispatchers.Main) {
              root.cloudRulesContentView.localVersion.version.text =
                root.cloudRulesContentView.remoteVersion.version.text
              root.cloudRulesContentView.setUpdateButtonStatus(false)
              runCatching {
                RulesBundleRepo.setLocalRulesVersion(
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
