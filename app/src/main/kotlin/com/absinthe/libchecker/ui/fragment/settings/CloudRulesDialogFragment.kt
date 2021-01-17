package com.absinthe.libchecker.ui.fragment.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.RULES_VERSION
import com.absinthe.libchecker.databinding.LayoutCloudRuleDialogBinding
import com.absinthe.libchecker.extensions.loge
import com.absinthe.libchecker.view.BaseBottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CloudRulesDialogFragment : BaseBottomSheetDialogFragment() {

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiManager.root)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val request: CloudRuleBundleRequest = retrofit.create(CloudRuleBundleRequest::class.java)
    private lateinit var binding: LayoutCloudRuleDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutCloudRuleDialogBinding.inflate(layoutInflater)
        binding.header.tvTitle.text = getString(R.string.cloud_rules)
        binding.vfContainer.apply {
            setInAnimation(activity, R.anim.anim_fade_in)
            setOutAnimation(activity, R.anim.anim_fade_out)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenResumed {
            request.requestCloudRuleBundle().enqueue(object : Callback<CloudRuleInfo> {
                override fun onResponse(call: Call<CloudRuleInfo>, response: Response<CloudRuleInfo>) {
                    response.body()?.let {
                        binding.tvLocalRulesVersion.text = RULES_VERSION.toString()
                        binding.tvRemoteRulesVersion.text = it.version.toString()
                        if (RULES_VERSION < it.version) {
                            binding.btnUpdate.isEnabled = true
                        }
                        binding.vfContainer.displayedChild = 1
                    }
                }

                override fun onFailure(call: Call<CloudRuleInfo>, t: Throwable) {
                    loge("CloudRulesDialogFragment", t)
                }

            })
        }
    }
}