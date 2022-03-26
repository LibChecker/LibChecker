package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import retrofit2.http.GET
import retrofit2.http.Header

const val VERSION = 3

interface CloudRuleBundleRequest {
  @GET("cloud/md5/v$VERSION")
  suspend fun requestCloudRuleInfo(@Header("Referer") referer: String = BuildConfig.APPLICATION_ID): CloudRuleInfo?
}
