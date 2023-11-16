package com.absinthe.libchecker.api

import com.absinthe.libchecker.api.request.VERSION
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.JsonUtil
import com.absinthe.libchecker.utils.extensions.unsafeLazy
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val BRANCH_MASTER = "master"
private const val WORKING_BRANCH = BRANCH_MASTER

object ApiManager {

  private const val GITHUB_ROOT_URL =
    "https://raw.githubusercontent.com/LibChecker/LibChecker-Rules/$WORKING_BRANCH/"
  private const val GITLAB_ROOT_URL =
    "https://gitlab.com/zhaobozhen/LibChecker-Rules/-/raw/$WORKING_BRANCH/"

  const val GITHUB_NEW_ISSUE_URL =
    "https://github.com/LibChecker/LibChecker-Rules/issues/new?labels=&template=library-name.md&title=%5BNew+Rule%5D"

  const val GITHUB_API_REPO_INFO = "https://api.github.com/repos/%s/%s"

  const val ANDROID_VERSION_DISTRIBUTION_HOST = "https://dl.google.com/"
  const val ANDROID_VERSION_DISTRIBUTION_PATH = "android/studio/metadata/distributions.json"

  private val root
    get() = when (GlobalValues.repo) {
      Constants.REPO_GITHUB -> GITHUB_ROOT_URL
      Constants.REPO_GITLAB -> GITLAB_ROOT_URL
      else -> GITHUB_ROOT_URL
    }

  val rulesBundleUrl = "${root}cloud/rules/v$VERSION/rules.db"

  @PublishedApi
  internal val retrofit by unsafeLazy {
    val okHttpClient = OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .addInterceptor(BaseUrlInterceptor())
      .build()
    Retrofit.Builder()
      .addConverterFactory(MoshiConverterFactory.create(JsonUtil.moshi))
      .client(okHttpClient)
      .baseUrl(root)
      .build()
  }

  inline fun <reified T : Any> create(): T = retrofit.create(T::class.java)
}
