package com.absinthe.libchecker.api

import java.util.Locale
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

class AndroidDevelopersInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val originalUrl = originalRequest.url
    Timber.d("originalRequest.url: %s", originalUrl)
    if (originalUrl.host != "android.com" || !Locale.CHINA.equals(Locale.getDefault())) return chain.proceed(originalRequest)

    val newUrl = originalUrl.newBuilder()
      .host("android.google.cn")
      .build()
    val newRequest = originalRequest.newBuilder()
      .url(newUrl)
      .build()
    Timber.d("newRequest.url: %s", newRequest.url)
    return chain.proceed(newRequest)
  }
}
