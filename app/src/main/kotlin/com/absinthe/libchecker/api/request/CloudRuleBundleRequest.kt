package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.BuildConfig
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.CloudRuleInfo
import com.absinthe.libchecker.api.bean.RepoInfoResp
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

const val VERSION = 4

interface CloudRuleBundleRequest {
  @GET("cloud/md5/v$VERSION")
  suspend fun requestCloudRuleInfo(@Header("Referer") referer: String = BuildConfig.APPLICATION_ID): CloudRuleInfo?

  @Headers("Accept: application/vnd.github.v3+json")
  @GET(ApiManager.GITHUB_API_REPO_INFO)
  suspend fun requestRepoInfo(
    @Path("owner") owner: String,
    @Path("repo") repo: String
  ): RepoInfoResp?
}
