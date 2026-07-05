package com.absinthe.libchecker.data.app

import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.LibDetailBean
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.api.request.LibDetailRequest
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.domain.app.repository.LibraryDetailRepository
import retrofit2.HttpException

object RemoteLibraryDetailRepository : LibraryDetailRepository {
  private val libDetailRequest: LibDetailRequest = ApiManager.create()
  private val cloudRuleBundleRequest: CloudRuleBundleRequest = ApiManager.create()

  override suspend fun requestLibraryDetail(categoryDir: String, libPath: String): LibDetailBean {
    return libDetailRequest.requestLibDetail(categoryDir, libPath)
  }

  override suspend fun getRepoPushedAt(owner: String, repo: String): String? {
    val authorization = GlobalValues.githubApiAuthorizationHeaderFor(ApiManager.GITHUB_API_REPO_INFO)
    return runCatching {
      cloudRuleBundleRequest.requestRepoInfo(
        owner = owner,
        repo = repo,
        authorization = authorization
      )?.pushedAt.also {
        GlobalValues.isGitHubReachable = true
      }
    }.onFailure {
      if (it is HttpException && authorization == null) {
        GlobalValues.isGitHubReachable = false
      }
    }.getOrNull()
  }
}
