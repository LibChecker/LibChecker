package com.absinthe.libchecker.data.statistics

import com.absinthe.libchecker.domain.statistics.chart.model.StatisticRemoteManifest
import com.absinthe.libchecker.utils.JsonUtil
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

interface OfficialStatisticRemoteSource {
  suspend fun getManifest(): StatisticRemoteManifest

  suspend fun downloadBundle(destination: File, maximumBytes: Long)
}

class HttpOfficialStatisticRemoteSource(
  private val client: OkHttpClient,
  private val manifestUrl: String,
  private val bundleUrl: String
) : OfficialStatisticRemoteSource {

  override suspend fun getManifest(): StatisticRemoteManifest = withContext(Dispatchers.IO) {
    val request = Request.Builder()
      .url(manifestUrl.withCacheBuster())
      .cacheControl(CacheControl.FORCE_NETWORK)
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("Chart manifest request failed: HTTP ${response.code}")
      }
      val body = response.body
      if (body.contentLength() > MAX_MANIFEST_BYTES) {
        throw IOException("Chart manifest is too large")
      }
      val bytes = body.bytes()
      if (bytes.size > MAX_MANIFEST_BYTES) {
        throw IOException("Chart manifest is too large")
      }
      checkNotNull(
        JsonUtil.moshi.adapter(StatisticRemoteManifest::class.java)
          .fromJson(bytes.toString(Charsets.UTF_8))
      ) {
        "Chart manifest is empty"
      }
    }
  }

  override suspend fun downloadBundle(
    destination: File,
    maximumBytes: Long
  ) = withContext(Dispatchers.IO) {
    val request = Request.Builder()
      .url(bundleUrl.withCacheBuster())
      .cacheControl(CacheControl.FORCE_NETWORK)
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IOException("Chart bundle request failed: HTTP ${response.code}")
      }
      if (response.body.contentLength() > maximumBytes) {
        throw IOException("Chart bundle is larger than its manifest")
      }
      check(destination.parentFile?.ensureDirectoryExists() == true) {
        "Unable to create chart download directory"
      }
      destination.outputStream().buffered().use { output ->
        response.body.byteStream().use { input ->
          val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
          var total = 0L
          while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maximumBytes) {
              throw IOException("Chart bundle is larger than its manifest")
            }
            output.write(buffer, 0, count)
          }
        }
      }
    }
  }

  private companion object {
    const val MAX_MANIFEST_BYTES = 32 * 1024
  }
}

internal fun File.ensureDirectoryExists(): Boolean = isDirectory || mkdirs()

private fun String.withCacheBuster(): HttpUrl = toHttpUrl().newBuilder()
  .addQueryParameter("refresh", System.currentTimeMillis().toString())
  .build()
