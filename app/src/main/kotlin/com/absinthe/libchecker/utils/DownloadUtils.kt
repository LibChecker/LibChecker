package com.absinthe.libchecker.utils

import com.absinthe.libchecker.compat.DnsCompat
import java.io.File
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.io.IOUtils

object DownloadUtils {
  private val client by lazy {
    OkHttpClient.Builder()
      .apply {
        // Enables Encrypted Client Hello where the platform and network security config allow it.
        DnsCompat.echCapableDns?.let(::dns)
      }
      .build()
  }

  /**
   * @param url      Download URL
   * @param file     File
   * @param listener Download callback
   */
  fun download(url: String, file: File, listener: OnDownloadListener, maximumBytes: Long = 32L * 1024 * 1024) {
    val request: Request = Request.Builder()
      .url(url)
      .build()
    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        listener.onDownloadFailed()
      }

      @Throws(IOException::class)
      override fun onResponse(call: Call, response: Response) {
        val success = runCatching {
          response.use {
            check(it.isSuccessful) { "Download failed: HTTP ${it.code}" }
            check(it.body.contentLength() <= maximumBytes) { "Download is too large" }
            file.parentFile?.mkdirs()
            it.body.byteStream().use { input ->
              file.outputStream().use { output ->
                val limit = maximumBytes.coerceAtLeast(0)
                val count = IOUtils.copyLarge(input, output, 0, limit)
                check(count < limit || input.read() == -1) { "Download is too large" }
                output.fd.sync()
              }
            }
          }
        }.isSuccess
        if (success) {
          listener.onDownloadSuccess()
        } else {
          file.delete()
          listener.onDownloadFailed()
        }
      }
    })
  }

  interface OnDownloadListener {
    fun onDownloadSuccess()
    fun onDownloadFailed()
  }
}
