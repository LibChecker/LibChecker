package com.absinthe.libchecker.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
            body.byteStream().use { ins ->
              val total = body.contentLength()
              FileOutputStream(file).use { fos ->
                var sum: Long = 0
                val buf = ByteArray(2048)
                var len: Int
                while (ins.read(buf).also { len = it } != -1) {
                  fos.write(buf, 0, len)
                  sum += len.toLong()
                  val progress = (sum * 1.0f / total * 100).toInt()
                  listener.onDownloading(progress)
                }
                fos.flush()
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
