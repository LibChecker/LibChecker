package com.absinthe.libchecker.api

import com.absinthe.libchecker.api.request.OWNER
import com.absinthe.libchecker.api.request.REPO
import com.absinthe.libchecker.api.request.REPO_INFO
import okhttp3.Interceptor
import okhttp3.Request
import timber.log.Timber

const val HEADER_BASE_URL = "Base-Url"

class BaseUrlInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    val originalRequest = chain.request()
    val builder: Request.Builder = originalRequest.newBuilder()
    val headers: List<String> = originalRequest.headers(HEADER_BASE_URL)
    if (headers.isNotEmpty()) {
      if (REPO_INFO == headers[0]) {
        val owner =
          originalRequest.headers(OWNER).getOrNull(0) ?: return chain.proceed(builder.build())
        val repo =
          originalRequest.headers(REPO).getOrNull(0) ?: return chain.proceed(builder.build())
        Timber.d("BaseUrlInterceptor: %s/%s", owner, repo)
        builder.removeHeader(HEADER_BASE_URL)
        builder.removeHeader(OWNER)
        builder.removeHeader(REPO)
        builder.url(String.format(ApiManager.GITHUB_API_REPO_INFO, owner, repo))
      }
    }
    return chain.proceed(builder.build())
  }
}
