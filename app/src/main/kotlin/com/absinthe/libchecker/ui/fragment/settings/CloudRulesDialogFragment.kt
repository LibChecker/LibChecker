package com.absinthe.libchecker.ui.fragment.settings

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.database.entity.RuleEntity
import com.absinthe.libchecker.extensions.addPaddingTop
import com.absinthe.libchecker.extensions.dp
import com.absinthe.libchecker.protocol.CloudRulesBundle
import com.absinthe.libchecker.ui.fragment.BaseBottomSheetViewDialogFragment
import com.absinthe.libchecker.utils.Toasty
import com.absinthe.libchecker.view.app.BottomSheetHeaderView
import com.absinthe.libchecker.view.settings.CloudRulesDialogView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class CloudRulesDialogFragment : BaseBottomSheetViewDialogFragment<CloudRulesDialogView>() {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiManager.root)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val request: CloudRuleBundleRequest = retrofit.create(CloudRuleBundleRequest::class.java)
    private var bundlesCount: Int = 1

    override fun initRootView(): CloudRulesDialogView = CloudRulesDialogView(requireContext())
    override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

    override fun init() {
        root.addPaddingTop(16.dp)
        root.cloudRulesContentView.updateButton.setOnClickListener {
            for (i in 1..bundlesCount) {
                requestBundle(i)
            }
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
                                bundlesCount = it.bundles
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

    private fun requestBundle(bundleCount: Int) {
        request.requestRulesBundle(bundleCount).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                response.body()?.let { rb ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val builder = CloudRulesBundle.parseFrom(rb.byteStream())

                        try {
                            Timber.d("version = ${builder?.version}")
                            Timber.d("count = ${builder?.count}")

                            val rulesList = mutableListOf<RuleEntity>()
                            builder.rulesList.cloudRulesList.forEach { rule ->
                                rule?.let {
                                    rulesList.add(RuleEntity(it.name, it.label, it.type, it.iconIndex, it.isRegexRule, it.regexName))
                                }
                            }
                            LibCheckerApp.repository.insertRules(rulesList)
                            withContext(Dispatchers.Main) {
                                root.cloudRulesContentView.localVersion.version.text = builder.version.toString()
                                root.cloudRulesContentView.updateButton.isEnabled = false
                                GlobalValues.localRulesVersion = builder.version
                            }
                        } catch (e: Throwable) {
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

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Timber.e(t)
            }

        })
    }
}