package com.absinthe.libchecker.utils

import java.io.File
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import okio.source

object DownloadUtils {
  private val client by lazy { OkHttpClient() }

  /**
   * @param url      Download URL
   * @param file     File
   * @param listener Download callback
   */
  fun download(url: String, file: File, listener: OnDownloadListener) {
    val request: Request = Request.Builder()
      .url(url)
      .build()
    client.newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        listener.onDownloadFailed()
      }

      @Throws(IOException::class)
      override fun onResponse(call: Call, response: Response) {
        if (file.exists()) {
          file.delete()
        }
        file.createNewFile()
        runCatching {
          response.body?.let { body ->
            body.byteStream().source().buffer().use { input ->
              file.sink().buffer().use { output ->
                output.writeAll(input)
                listener.onDownloadSuccess()
              }
            }
          } ?: run {
            listener.onDownloadFailed()
          }
        }.onFailure {
          listener.onDownloadFailed()
        }
      }
    })
  }

  interface OnDownloadListener {
    fun onDownloadSuccess()
    fun onDownloading(progress: Int)
    fun onDownloadFailed()
  }
}
