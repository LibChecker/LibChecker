package com.absinthe.libchecker.api.request

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GitHubContributorResp
import com.absinthe.libchecker.api.bean.RepoInfoResp
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface CloudRuleBundleRequest {
  @GET
  suspend fun requestV5Manifest(@Url url: String = ApiManager.rulesV5Root + "manifest.json"): com.absinthe.libchecker.domain.rules.RuleBundleManifest?

  @Headers(
    "Accept: application/vnd.github+json",
    "X-GitHub-Api-Version: 2022-11-28",
    "User-Agent: LibChecker"
  )
  @GET(ApiManager.GITHUB_API_REPO_INFO)
  suspend fun requestRepoInfo(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Header("Authorization") authorization: String? = null
  ): RepoInfoResp?

  @Headers(
    "Accept: application/vnd.github+json",
    "X-GitHub-Api-Version: 2022-11-28",
    "User-Agent: LibChecker"
  )
  @GET(ApiManager.GITHUB_API_REPO_CONTRIBUTORS)
  suspend fun requestContributors(
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Header("Authorization") authorization: String? = null,
    @Query("per_page") perPage: Int = 100,
    @Query("page") page: Int,
    @Query("anon") includeAnonymous: Int = 1
  ): List<GitHubContributorResp>
}
