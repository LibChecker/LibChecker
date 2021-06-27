package com.absinthe.libchecker.ui.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.DownloadUtils
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.RuleDatabase
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.settings.CloudRulesDialogView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
            request.requestCloudRuleInfo().enqueue(object : Callback<CloudRuleInfo> {
                override fun onResponse(call: Call<CloudRuleInfo>, response: Response<CloudRuleInfo>) {
                    val body = response.body()
                    body?.let {
                        lifecycleScope.launch(Dispatchers.Main) {
                            try {
                                root.cloudRulesContentView.localVersion.version.text = GlobalValues.localRulesVersion.toString()
                                root.cloudRulesContentView.remoteVersion.version.text = it.version.toString()
                                if (GlobalValues.localRulesVersion < it.version) {
                                    root.cloudRulesContentView.updateButton.isEnabled = true
                                }
                                root.viewFlipper.displayedChild = 1
                            } catch (e: Exception) {
                                Timber.e(e)
                                context?.let {
                                    withContext(Dispatchers.Main) {
                                        Toasty.show(it, R.string.toast_cloud_rules_update_error)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<CloudRuleInfo>, t: Throwable) {
                    Timber.e(t)
                }

            })
        }
    }

    private fun requestBundle() {
        val saveFile = File(requireContext().cacheDir, Constants.RULES_DB_FILE_NAME)
        DownloadUtils.get().download(requireContext(), ApiManager.rulesBundleUrl, saveFile, object : DownloadUtils.OnDownloadListener{
            override fun onDownloadSuccess() {
                RuleDatabase.getDatabase(requireContext()).close()
                val databaseDir = File(requireContext().filesDir.parent, "databases")
                if (databaseDir.exists()) {
                    var file = File(databaseDir, Constants.RULES_DATABASE_NAME)
                    if (file.exists()) {
                        file.delete()
                    }
                    file = File(databaseDir, "${Constants.RULES_DATABASE_NAME}-shm")
                    if (file.exists()) {
                        file.delete()
                    }
                    file = File(databaseDir, "${Constants.RULES_DATABASE_NAME}-wal")
                    if (file.exists()) {
                        file.delete()
                    }
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    root.cloudRulesContentView.localVersion.version.text = root.cloudRulesContentView.remoteVersion.version.text
                    root.cloudRulesContentView.updateButton.isEnabled = false
                    try {
                        GlobalValues.localRulesVersion = root.cloudRulesContentView.remoteVersion.version.text.toString().toInt()
                    } catch (e: NumberFormatException) { }
                }
            }

            override fun onDownloading(progress: Int) {

            }

            override fun onDownloadFailed() {
                context?.let {
                    lifecycleScope.launch(Dispatchers.Main) {
                        Toasty.show(it, R.string.toast_cloud_rules_update_error)
                    }
                }
            }

        })
    }
}