package com.absinthe.libchecker.api

import java.util.Locale
import okhttp3.Interceptor
import okhttp3.Response

class AndroidDevelopersInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val originalUrl = originalRequest.url
    if (originalUrl.host != "android.com" || !"CN".equals(Locale.getDefault().country)) return chain.proceed(originalRequest)

    val newUrl = originalUrl.newBuilder()
      .host("android.google.cn")
      .build()
    val newRequest = originalRequest.newBuilder()
      .url(newUrl)
      .build()
    return chain.proceed(newRequest)
  }
}
