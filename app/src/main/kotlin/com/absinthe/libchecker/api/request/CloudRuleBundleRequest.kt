package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.bean.CloudRuleInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

private const val VERSION = 2

interface CloudRuleBundleRequest {
    @GET("cloud/md5/v$VERSION")
    fun requestCloudRuleInfo(): Call<CloudRuleInfo>

    @GET("cloud/rules/v$VERSION/rules.lcr.{bundleCount}")
    fun requestRulesBundle(@Path("bundleCount") bundleCount: Int): Call<ResponseBody>
}