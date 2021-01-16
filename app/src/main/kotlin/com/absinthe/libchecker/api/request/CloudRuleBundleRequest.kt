package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.bean.CloudRuleInfo
import retrofit2.Call
import retrofit2.http.GET

private const val VERSION = 1

interface CloudRuleBundleRequest {
    @GET("cloud/md5/v$VERSION")
    fun requestCloudRuleBundle(): Call<CloudRuleInfo>
}